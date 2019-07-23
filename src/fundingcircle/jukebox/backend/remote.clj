(ns fundingcircle.jukebox.backend.remote
  "Coordinate with remote step executors."
  (:require [aleph.http :as http]
            [aleph.tcp :as tcp]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [manifold.deferred :as d]
            [manifold.stream :as s]))

(defonce jlc-step-registry (atom nil))

(defn register-client-steps
  "Register steps that a jukebox language client knows how to handle."
  [{:strs [clientid steps]}]
  (println "Registring client steps")
  (swap! jlc-step-registry
         merge
         (-> (map (fn [step] [step clientid]) steps)
             (into {}))))

(defonce ws (atom nil))
(defn step-request
  [req]
  (if-let [socket (try
                    @(http/websocket-connection req)
                    (catch Exception e
                      (.printStackTrace e)
                      nil))]
    (do
      (reset! ws socket)
      (loop [message @(s/take! @ws)]
        (case (get message "action")
          "register" (register-client-steps message))
        (recur @(s/take! @ws))))

    {:status 400
     :headers { "content-type" "application/text" }
     :body "Expected a websocket request."}))

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
  (start)
  [server ws]

  (stop)
  )
