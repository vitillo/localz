(ns cljs.localz.facebook
  (:require [cljs.core.async :refer [<! >! put! close! chan]])
  (:require-macros [cljs.core.async.macros :refer  [go]]))

(defn api-call
  ([method]
   (let [channel (chan)]
     (js/FB.api method #(go (>! channel %)))
     channel))

  ([method params]
   (let [channel (chan)]
     (js/FB.api method params #(go (>! channel %)))
     channel)))

(defn init []
  (js/FB.init #js {:appId "276475512541725"
                   :cookie true
                   :xfbml true
                   :version "v2.1"}))

(defn login []
  (let [ch (chan)
        status-change-callback #(go (>! ch (.-status %)))]
    (js/FB.login status-change-callback)
    ch))

(defn get-login-status []
  (let [ch (chan)
        status-change-callback #(go (>! ch (.-status %)))]
    (js/FB.getLoginStatus status-change-callback)
    ch))

(defn fetch-personal-data []
  (let [ch (chan)]
    (go
      (let [name (.-first_name (<! (api-call "/me")))
            url (aget (<! (api-call "/me/picture" #js {:height 15, :width 15})) "data" "url")]
        (>! ch {:name name, :url url})
        (close! ch)))
    ch))

