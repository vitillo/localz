(ns clj.localz.core
  (:require [chord.http-kit :refer [with-channel]]
            [org.httpkit.server :refer :all :exclude [with-channel]]
            [compojure.core :refer[defroutes GET]]
            [compojure.route :as route]
            [clojure.core.async :refer [<! >! put! close! go]]
            [clojure.edn :as edn]
            [clj.localz.location :as loc]))

(def clients (atom {}))

(defn broadcast [{:keys [location] :as message}]
  (println "Broadcasting message: " message)
  (let [message (pr-str message)
        sector-id (loc/location-sector-id location)
        sector-clients (get @clients sector-id)]
    (go
      (doseq [client sector-clients]
        (>! (key client) message)))))

(defn store-sector-id [store ws-ch {:keys [location] :as message}]
  (when (empty? @store)
    (let [sector-id (loc/location-sector-id location)]
      (swap! clients assoc-in [sector-id ws-ch] true)
      (reset! store sector-id))))

(defn eval-messages [ws-ch]
  (let [sector-id (atom {})]
    (go
      (loop []
        (if-let [{:keys [message]} (<! ws-ch)]
          (let [message (edn/read-string message) ]
            (println "Message received:" message)
            (store-sector-id sector-id ws-ch message)
            (<! (broadcast message))
            (recur))
          (do
            (swap dissoc-in [@sector-id ws-ch])
            (println "DISCONNECTED")))))))

(defn chatroom [req]
  (with-channel req ws-ch
    (eval-messages ws-ch)))

(defroutes routes
  (GET "/chat" [] chatroom)
  (route/resources "/")
  (route/not-found "Page not found"))

(defonce server (run-server #'routes {:port 8080}))
