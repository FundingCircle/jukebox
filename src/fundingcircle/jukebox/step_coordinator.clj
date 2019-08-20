(ns fundingcircle.jukebox.step-coordinator
  "Coordinate with remote step executors."
  (:require [aleph.http :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [fundingcircle.jukebox.step-client.jlc-clojure :as jlc-clojure]
            [manifold.deferred :as d]
            [manifold.stream :as s]))

(defonce client-count (atom 0))
(defonce registration-completed (atom nil))
(defonce result-received (atom nil))
(defonce jlc-step-registry (atom nil))
(defonce ws (atom nil))

(defn send!
  "Send a message to the language client over the web socket."
  [clientid message]
  (s/put! (get @ws clientid) (json/generate-string message)))

(defn drive-step
  ""
  [step board args]
  (if-let [clientid (get @jlc-step-registry step)]
    (do
      (log/debugf "Sending request to client %s to execute step: %s: %s" clientid step args)
      (send! clientid {"action" "step"
                       "step" step
                       "args" args
                       "board" board})
      (reset! result-received (d/deferred))
      @result-received)
    (log/errorf "No client knows how to handle step: %s" step)))

(defn register-client-steps
  "Register steps that a jukebox language client knows how to handle."
  [{:keys [clientid language steps]}]
  (log/debugf "Registering %s client (%s) steps: %s" language clientid steps)
  (swap! jlc-step-registry
         merge
         (->> (map (fn [step] [step clientid]) steps)
              (into {})))
  {:status 202})

(defn handle-client-message
  ""
  [message]
  (if (= :timeout message)
    (log/errorf "timed out")
    (let [message (json/parse-string message true)]
      (case (:action message)
        "response" (log/debugf "received step result: %s" message)
        "result" (d/success! @result-received (:board message))
        (log/errorf "Don't know how to handle message: %s" message)))))

(defn step-request
  [req]
  (if-let [socket (try
                    @(http/websocket-connection req)
                    (catch Exception e
                      (.printStackTrace e)
                      nil))]
    (do
      (if-let [{:keys [clientid] :as message} (json/parse-string @(s/take! socket) true)]
        (do
          (log/debugf "Established ws connection with language client: %s" clientid)
          (swap! ws assoc clientid socket)
          (register-client-steps message)
          (swap! client-count dec)
          (log/debugf "Remaining # of clients to register steps: %s" @client-count)
          (when (= 0 @client-count)
            (log/debugf "All clients have registered steps: %s" @jlc-step-registry)
            (d/success! @registration-completed (keys @jlc-step-registry))))
        (log/errorf "Didn't get registration message"))
      (s/consume #'handle-client-message socket)
      {:status 202})

    (do
      (log/errorf "Failed to establish client websocket connection")
      {:status 400
       :headers { "content-type" "application/text" }
       :body "Expected a websocket request."})))

(defroutes step-coordinator
  (GET "/jukebox" [] step-request)
  (GET "/status" []  {:status :ok})
  (route/not-found "No such route."))

(defonce server (atom nil))

(defn start
  "Start step coordinator."
  [glue-paths clients]
  (when-not @server
    (let [steps-registered (d/deferred)
          s (http/start-server #'step-coordinator {:port 0})
          port (aleph.netty/port s)]
      (log/debugf "Started on port %s" port)
      (reset! server s)
      (reset! client-count (count clients))
      (reset! registration-completed steps-registered)
      (doseq [client clients]
        (log/infof "Spinning up jukebox language client: %s" client)
        (client port glue-paths))
      ;(log/debugf "Started")
      steps-registered))
  )

(defn stop
  "Stop step coordinator."
  []
  (when @ws
    (log/debugf "Stopping jukebox language clients")
    (doseq [clientid (keys @ws)]
      (send! clientid {"action" "stop"}))
    (reset! ws nil))

  (when @server
    (log/debugf "Stopping")
    (.close @server)
    (reset! server nil)))

(defn restart
  "Restart the step coordinator."
  [glue-paths clients]
  (stop)
  (start glue-paths clients))

(comment
  (restart ["test/example"] [#'jlc-client/client])
  (restart [] [])

  )
