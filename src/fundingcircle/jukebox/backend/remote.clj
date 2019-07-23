(ns fundingcircle.jukebox.backend.remote
  "Coordinate with remote step executors."
  (:require [aleph.http :as http]
            [aleph.tcp :as tcp]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [manifold.deferred :as d]
            [manifold.stream :as s]))

(def non-websocket-error
  {:status 400
   :headers { "content-type" "application/text" }
   :body "Expected a websocket request."})

(defmulti coord
  "Handle coordination messages on the server side."
  (fn [message] (get message "action")))

(defmethod coord :default
  [message]
  (println "Got message w/unknown action: %s" message)
  message)

(defonce sock (atom nil))

(defn step-request
  [req]
  (if-let [socket (try
                    @(http/websocket-connection req)
                    (catch Exception e
                      (.printStackTrace e)
                      nil))]
    (do
      (println "Resetting sock:" socket)
      (reset! sock socket)

      (s/connect socket socket))
    non-websocket-error))

(defroutes step-coordinator
  (GET "/jukebox" [] step-request)
  (GET "/status" []  {:status :ok})
  (route/not-found "No such route."))

(defonce server nil)

(defn start
  "Start step coordinator."
  []
  (when-not server
    (alter-var-root #'server (fn [_]
                               (println "creating server")
                               (http/start-server #'step-coordinator {:port 9453})))))

(defn stop
  "Stop step coordinator."
  []
  (when server
    (.close @server)
    (alter-var-root #'server (constantly nil))))

(comment
  server
  @sock

  (s/put! @sock "10")
  @(s/take! @sock)

  (start)
  (stop)
  )
