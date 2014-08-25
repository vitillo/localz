(ns cljs.localz.core
  (:require [cljs.core.async :refer  [<! >! put! close! chan]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros  [html]]
            [cljs.core.match]
            [cljs.localz.facebook :as fb]
            [cljs.localz.server :as server])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs.core.match.macros :refer [match]]))

(enable-console-print!)

(defn input-widget [state owner]
  (reify
    om/IRender
    (render [_]
      (html [:div.footer.navbar-fixed-bottom {:style {:padding-bottom "10px"}}
             [:div.container
              [:div.row
               (html/label {:class "col-sm-2"} "input" (str (:name state) ": "))
               [:div.col-sm-10
                (html/text-field {:on-key-up (partial server/send-message state owner)
                                  :ref "chat-input"
                                  :style {:width "90%"}
                                  :autocomplete "off"
                                  :autocorrection "off"
                                  :autocapitalize "off"}
                                 "input")]]]]))))

(defn history-widget [history owner]
  (reify
    om/IWillUpdate
    (will-update [_ _ _]
      (let [node (om/get-node owner "history")]
        (set! (.-shouldScrollBottom node)
              (= (+ (.-scrollTop node) (.-offsetHeight node)) (.-scrollHeight node)))))
    om/IDidUpdate
    (did-update [_ _ _]
      (let [node (om/get-node owner "history")]
        (when (.-shouldScrollBottom node)
          (set! (.-scrollTop node) (.-scrollHeight node)))))
    om/IRender
    (render [_]
      (html [:div {:style {:overflow "auto"
                           :height "calc(100vh - 155px)"}
                   :ref "history"}
             [:table.table.table-striped.table-condensed
              (for [message history]
                (let [{:keys [name url payload type]} message]
                  [:tr
                   (if (= type :message)
                     [:td {:style {:padding "5px"}}
                      (html/image url)
                      [:span (str " " name ": " payload)]]
                     [:td {:style {:padding "5px"}}
                      [:span (str name " " payload)]])
                   ]))]]))))

(defn header-widget [_]
  (om/component
    (html [:div.page-header.text-center {:style {:height "50px"}}
           [:h1 "localz "
            [:small "the stupid geolocalized chat"]]])))

(defn get-location []
  (let [ch (chan)
        success
        (fn [position]
          (let [latitude (aget position "coords" "latitude")
                longitude (aget position "coords" "longitude")]
            (go (>! ch {:latitude latitude, :longitude longitude}))))

        error (fn [e] (go (>! ch {})))]
    (.getCurrentPosition js/navigator.geolocation success error)
    ch))

(defn login [state]
  (go
    (om/update! state :error ["Logging in..."])
    (let [status (<! (fb/get-login-status))
          location (<! (get-location))]
      (match [status location]
        ["connected" {:latitude _, :longitude _}]
        (let [_ (server/connect-to-server state)
              name (.-first_name (<! (fb/api-call "/me")))
              url (aget (<! (fb/api-call "/me/picture" #js {:height 15, :width 15})) "data" "url")
              credentials {:name name, :url url, :location location}]
          (om/transact! state #(assoc % :page ["chat"], :credentials credentials, :error [])))

        ["not_authorized" _]
        (om/update! state :error ["Please authorize this app on Facebook."])

        ["unknown" _]
        (om/update! state :error ["Please login with Facebook it proceed."])

        [_ ({} :only [])]
        (om/update! state :error ["Please enable geolocalization"])))))

(defn login-widget [state]
  (reify
    om/IWillMount
    (will-mount [_]
      (fb/init)
      (login state))

    om/IRender
    (render [_]
      (html [:div.text-center
             (html/link-to {:on-click #(when-not (= ["Logging in..."] (:error @state))
                                         (go
                                           (<! (fb/login))
                                           (<! (login state))))}
                           "#"
                           (html/image {:style {:width "100px"}} "images/login.png"))]))))

(defn chat-widget [state]
  (om/component
    (html [:div
           (om/build history-widget (:history state))
           (om/build input-widget (:credentials state))])))

(defn error-widget [[error]]
  (reify
    om/IRender
    (render [_]
      (when error
        (html [:div.text-center.text-danger error])))))

(defn layout [state]
  (reify
    om/IRender
    (render [_]
      (let [page (get-in state [:page 0])]
       (html [:div.container {:style {:height "100%"}}
              (om/build header-widget nil)
              (om/build error-widget (:error state))
              (if (and (= page "chat")
                       (empty? (:error state)))
                (om/build chat-widget (select-keys state [:history :credentials]))
                (om/build login-widget state))])))))

(def app-state (atom {:page ["login"]
                      :history []
                      :credentials {}
                      :error []}))

(om/root layout app-state {:target (.getElementById js/document "app")})
