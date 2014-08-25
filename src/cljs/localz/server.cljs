(ns cljs.localz.server
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer  [<! >! put! close! chan]]
            [cljs.reader :as reader]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def server-channel (atom nil))

(defn eval-messages [history]
  (let [ch @server-channel]
    (go
      (loop []
        (if-let [{:keys [message error]} (<! ch)]
          (if-not error
            (do
              (om/transact! history :history #(conj % (reader/read-string message)))
              (recur))
            (println "An error occured" error))
          nil)))))

(defn send-message [state owner e]
  (let [node (om/get-node owner "chat-input")
        payload (.-value node)
        is-enter (= 13 (.-which e))
        ch @server-channel]
    (when (and (> (count payload) 0) is-enter)
      (go
        (let [message (-> @state (assoc :payload payload :type :message) pr-str)]
         (>! ch message)
         (set! (.-value node) ""))))))

(defn connect-to-server [state]
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:8080/chat"))]
      (if-not error
        (do
          (reset! server-channel ws-channel)
          (eval-messages state))
        (println "Error:" error)))))

