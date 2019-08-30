(ns fundingcircle.jukebox.step-coordinator
  "Coordinate with remote step executors."
  (:require [aleph.http :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [fundingcircle.jukebox.step-client :as step-client]
            [clojure.java.io :as io]))

;; Jukebox language client launchers
(require 'fundingcircle.jukebox.step-client.jlc-clojure)
(require 'fundingcircle.jukebox.step-client.jlc-cli)
(require 'fundingcircle.jukebox.step-client.jlc-jruby)

(defonce client-count (atom 0))
(defonce client-ids (atom #{}))
(defonce registration-completed (atom nil))
(defonce result-received (atom nil))
(defonce jlc-step-registry (atom nil))
(defonce ws (atom nil))

(defn send!
  "Send a message to the language client over the web socket."
  [clientid message]
  (s/put! (get @ws clientid) (json/generate-string message)))

(defn- stack-trace-element
  "Deserialize a stack trace."
  [{:keys [class-name method-name file-name line-number]}]
  (StackTraceElement. class-name method-name file-name line-number))

(defn drive-step
  "Find a jukebox language client that knows about `step`, and ask for it to be run."
  [step board args]
  (if-let [clientid (get-in @jlc-step-registry [:steps step])]
    (do
      (log/debugf "Sending request to client %s to execute step: %s: %s" clientid step args)
      (reset! result-received (d/deferred))
      (send! clientid {"action" "step"
                       "step" step
                       "args" args
                       "board" board})
      (let [result @@result-received]
        (log/debugf "Step result: %s" result)
        (when (:error result)
          (let [e (RuntimeException. (str "Step exception: " (:error result)))]
            (.setStackTrace e (into-array StackTraceElement (mapv stack-trace-element (:trace result))))
            (throw e)))
        (:board result)))
    (log/errorf "No client knows how to handle step: %s" step)))

#_(defn drive-hook
  "Run hooks on all language clients."
  [hook-id board scenario]
  (doseq [client-id @client-ids]
    (reset! result-received (d/deferred))
    (send! client-id {"action" "hook"
                      "hook_id" hook-id
                      "board" board
                      "scenario" scenario})
    (let [result @@result-received]
        (log/debugf "%s result: %s" action result)
        (when (:error result)
          (let [e (RuntimeException. (str "Step exception: " (:error result)))]
            (.setStackTrace e (into-array StackTraceElement (mapv stack-trace-element (:trace result))))
            (throw e)))
        (:board result))))

(defn register-client-steps
  "Register steps that a jukebox language client knows how to handle."
  [{:keys [clientid language steps hooks]}]
  (log/debugf "Registering %s client (%s) steps: %s" language clientid steps)
  (log/debugf "Step Registry BEFORE: %s" @jlc-step-registry)
  (swap! jlc-step-registry
         update :steps
         merge
         (->> (map (fn [step] [step clientid]) steps)
              (into {})))
  (log/debugf "Client (%s) hooks: %s" clientid hooks)
  (log/debugf "Current registry hooks: %s" (:hooks-registry @jlc-step-registry))
  (swap! jlc-step-registry update :hooks-registry conj hooks)
  (swap! jlc-step-registry
         update :hooks-index
         merge
         (->> (map (fn [{:keys [id]}] [id clientid])
                   (concat (:before hooks) (:after hooks)))
              (into {})))
  (log/debugf "Step Registry AFTER: %s" @jlc-step-registry)
  {:status 202})

(defn hook-fn
  "Returns a hook fn that will dispatch to the registered hook by ID when called."
  [hook-id]
  (fn [board scenario]
    (if-let [client-id (get-in @jlc-step-registry [:hooks-index hook-id])]
      (do
        (log/debugf "Sending request to client %s to execute hook: %s" client-id {:hook-id hook-id :board board :scenario scenario})
        (reset! result-received (d/deferred))
        (send! client-id {"action" "hook"
                          "hook-id" hook-id
                          "board" board
                          "scenario" nil #_scenario})
        (let [result @@result-received]
          (log/debugf "Hook result: %s" result)
          (when (:error result)
            (let [e (RuntimeException. (str "Hook exception: " (:error result)))]
              (.setStackTrace e (into-array StackTraceElement (mapv stack-trace-element (:trace result))))
              (throw e)))
          (:board result)))
      (log/errorf "No client knows how to handle hook: %s" hook-id))))

(defn handle-client-message
  ""
  [message]
  (if (= :timeout message)
    (log/errorf "timed out")
    (let [message (json/parse-string message true)]
      (case (:action message)
        "result" (d/success! @result-received message)
        (log/errorf "Don't know how to handle message: %s" message)))))

(defn jlc-connected
  "Called when a jukebox language client initially connects."
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
          (swap! client-ids conj clientid)
          (log/debugf "Remaining # of clients to register steps: %s" @client-count)
          (when (= 0 @client-count)
            (log/debugf "All clients have registered steps: %s" @jlc-step-registry)
            (d/success! @registration-completed @jlc-step-registry)))
        (log/errorf "Didn't get registration message"))
      (s/consume #'handle-client-message socket)
      {:status 202})

    (do
      (log/errorf "Failed to establish client websocket connection")
      {:status 400
       :headers { "content-type" "application/text" }
       :body "Expected a websocket request."})))

(defroutes step-coordinator
  (GET "/jukebox" [] jlc-connected)
  (GET "/status" []  {:status :ok})
  (route/not-found "No such route."))

(defonce server (atom nil))

(defn- language-client-configs
  "Load language client configs from a json file named `.jukebox` on the classpath."
  []

  (log/debugf "cwd: %s" (System/getProperty "user.dir"))
  (into
    (-> (try (slurp ".jukebox") (catch Exception _ "[]"))
        (json/parse-string true)
        :language-clients)
    ;; spin up clojure & jruby as default
    [#_{:language "ruby" :launcher "jruby-embedded"}
     #_{:language "ruby" :launcher "jlc-cli" :cmd ["ruby" "-I" "../../jlc_ruby/lib" "jlc_ruby"]}
     #_{:language "ruby" :launcher "jlc-cli" :cmd ["test/bin/jlc_ruby"] :dir "."}
     {:language "clojure" :launcher "clojure-embedded"}]))

(defn start
  "Start step coordinator."
  [glue-paths]
  (when-not @server
    (let [client-configs (language-client-configs)
          steps-registered (d/deferred)
          s (http/start-server #'step-coordinator {:port 0})
          port (aleph.netty/port s)]
      (log/debugf "Started on port %s" port)
      (reset! server s)
      (reset! client-count (count client-configs))
      (reset! registration-completed steps-registered)
      (doseq [client-config client-configs]
        (log/infof "Spinning up jukebox language client: %s" client-config)
        (step-client/launch client-config port glue-paths))
      steps-registered)))

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
  [glue-paths]
  (stop)
  (start glue-paths))
