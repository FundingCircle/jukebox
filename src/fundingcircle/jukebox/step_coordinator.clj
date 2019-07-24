(ns fundingcircle.jukebox.step-coordinator
  "Coordinate with remote step executors."
  (:require [aleph.http :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [fundingcircle.jukebox.step-client.clojure-lang :as clojure-lang]
            [manifold.deferred :as d]
            [manifold.stream :as s]))

(defn logf
  "Log a messagate to stdout."
  [fmt & args]
  (apply printf (str "JLS: " fmt "\n") args))

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
      (logf "Sending request to execute step: %s: %s" step args)
      (send! clientid {"action" "step"
                       "step" step
                       "args" args
                       "board" board})
      (reset! result-received (d/deferred))
      @result-received)
    (logf "No client knows how to handle step: %s" step)))

(defn register-client-steps
  "Register steps that a jukebox language client knows how to handle."
  [{:keys [clientid steps]}]
  (logf "Registering client steps: %s" steps)
  (swap! jlc-step-registry
         merge
         (->> (map (fn [step] [step clientid]) steps)
              (into {})))
  {:status 202})

(defn handle-client-message
  ""
  [message]
  (if (= :timeout message)
    (logf "timed out")
    (let [message (json/parse-string message true)]
      (case (:action message)
        "response" (logf "received step result: %s" message)
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
          (logf "Established ws connection with language client: %s" clientid)
          (swap! ws assoc clientid socket)
          (register-client-steps message)
          (swap! client-count dec)
          (when (= 0 @client-count)
            (d/success! @registration-completed (keys @jlc-step-registry))))
        (logf "Didn't get registration message"))
      (s/consume #'handle-client-message socket)
      {:status 202})

    (do
      (logf "Failed to establish client websocket connection")
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
      (logf "Started on port %s" port)
      (reset! server s)
      (reset! client-count (count clients))
      (reset! registration-completed steps-registered)
      (doseq [client clients]
        (logf "Spinning up jukebox language client: %s" client)
        (client port glue-paths))
      (logf "Started")
      steps-registered))
  )

(defn stop
  "Stop step coordinator."
  []
  (when @ws
    (logf "Stopping jukebox language clients")
    (doseq [clientid (keys @ws)]
      (send! clientid {"action" "stop"}))
    (reset! ws nil))

  (when @server
    (logf "Stopping")
    (.close @server)
    (reset! server nil)))

(defn restart
  "Restart the step coordinator."
  [glue-paths clients]
  (stop)
  (start glue-paths clients))

(comment
  (restart ["test/example"] [#'clojure-lang/client])
  (restart [] [])

  )
