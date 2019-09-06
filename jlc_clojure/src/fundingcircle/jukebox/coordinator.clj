(ns fundingcircle.jukebox.coordinator
  "Coordinate with remote step executors."
  (:require [aleph.http :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [fundingcircle.jukebox.launcher :as step-client]
            [fundingcircle.jukebox.launcher.auto :as auto]
            [manifold.deferred :as d]
            [manifold.stream :as s])
  (:import (java.io Closeable)))

;; Jukebox language client launchers
(require 'fundingcircle.jukebox.launcher.clj)
(require 'fundingcircle.jukebox.launcher.cli)

(defonce client-count (atom 0))
(defonce client-ids (atom #{}))
(defonce callbacks (atom {}))
(defonce registration-completed (atom nil))
(defonce result-received (atom nil))
(defonce definitions (atom []))
(defonce snippets (atom []))
(defonce ws (atom nil))

(defn send!
  "Send a message to the language client over the web socket."
  [client-id message]
  (log/debugf "Sending message to %s: %s" client-id message)
  (s/put! (get @ws client-id) (json/generate-string message)))

(defn- stack-trace-element
  "Deserialize a stack trace."
  [{:keys [class-name method-name file-name line-number]}]
  (StackTraceElement. class-name method-name file-name line-number))

(defn drive-step
  "Find a jukebox language client that knows about `step`, and ask for it to be run."
  [id board args]
  (if-let [client-id (get @callbacks id)]
    (do
      (log/debugf "Sending request to client %s to execute step: %s: %s" client-id id args)
      (reset! result-received (d/deferred))
      (send! client-id {"action" "run"
                        "id" id
                        "board" board
                        "args" args})
      (let [result @@result-received]
        (log/debugf "Step result: %s" result)
        (when (:error result)
          (let [e (RuntimeException. (str "Step exception: " (:error result)))]
            (.setStackTrace e (into-array StackTraceElement (mapv stack-trace-element (:trace result))))
            (throw e)))
        (:board result)))
    (do
      (log/errorf "No client knows how to handle step: %s" id)
      board)))

(defn drive-hook
  "Run hooks on all language clients."
  [id board scenario]
  (let [client-id (get @callbacks id)]
    (log/debugf "Sending request to client %s to execute hook: %s: %s" client-id id {:board board :scenario scenario})

    (reset! result-received (d/deferred))
    (send! client-id {"action" "run"
                      "id" id
                      "board" board
                      "args" [scenario]})
    (let [result @@result-received]
      (log/debugf "Hook result: %s" result)
      (when (:error result)
        (let [e (RuntimeException. (str "Step exception: " (:error result)))]
          (.setStackTrace e (into-array StackTraceElement (mapv stack-trace-element (:trace result))))
          (throw e)))
      (:board result))))

(defn register-client-steps
  "Register steps that a jukebox language client knows how to handle."
  [{:keys [client-id language] :as message}]
  (swap! definitions into (:definitions message))
  (swap! callbacks merge
         (->> (map (fn [{:keys [id]}] [id client-id]) (:definitions message))
              (into {})))
  (swap! snippets conj (assoc (:snippet message) :language language))
  {:status 202})

(defonce server (atom nil))

(defn stop
  "Stop step coordinator."
  []
  (when @ws
    (log/debugf "Stopping jukebox language clients")
    (doseq [client-id (keys @ws)]
      (send! client-id {"action" "stop"}))
    (reset! ws nil))

  (when @server
    (log/debugf "Stopping")
    (.close ^Closeable @server)
    (reset! server nil)))

(defn handle-client-message
  ""
  [message]
  (if (= :timeout message)
    (log/errorf "timed out")
    (let [message (json/parse-string message true)]
      (case (:action message)
        "result" (d/success! @result-received message)
        "error" (let [e (RuntimeException. (str "Exception: " (:message message)))]
                  (.setStackTrace e (into-array StackTraceElement (mapv stack-trace-element (:trace message))))
                  (stop)
                  (.printStackTrace e)
                  (System/exit 1)                           ;; TODO: something better
                  #_(throw e))
        (do
          (log/errorf "Don't know how to handle message: %s" message)
          (stop))))))

(defn jlc-connected
  "Called when a jukebox language client initially connects."
  [req]
  (if-let [socket (try
                    @(http/websocket-connection req)
                    (catch Exception e
                      (.printStackTrace e)
                      nil))]
    (do
      (if-let [{:keys [client-id] :as message} (json/parse-string @(s/take! socket) true)]
        (do
          (log/debugf "Established ws connection with language client: %s" client-id)
          (swap! ws assoc client-id socket)
          (register-client-steps message)
          (swap! client-count dec)
          (swap! client-ids conj client-id)
          (log/debugf "Remaining # of clients to register steps: %s" @client-count)
          (when (= 0 @client-count)
            (log/debugf "All clients have registered steps: %s" @definitions)
            (d/success! @registration-completed {:definitions @definitions :snippets @snippets})))
        (log/errorf "Didn't get registration message"))
      (s/consume #'handle-client-message socket)
      {:status 202})

    (do
      (log/errorf "Failed to establish client websocket connection")
      {:status 400
       :headers {"content-type" "application/text"}
       :body "Expected a websocket request."})))

(defroutes step-coordinator
           (GET "/jukebox" [] jlc-connected)
           (GET "/status" [] {:status :ok})
           (route/not-found "No such route."))

(defn- language-client-configs
  "Load language client configs from a json file named `.jukebox` on the classpath."
  []
  (log/debugf "cwd: %s" (System/getProperty "user.dir"))
  (let [project-configs   (into {} (-> (try (slurp ".jukebox") (catch Exception _ "{}"))
                                       (json/parse-string true)))
        language-clients  (into (:language-clients project-configs)
                                [{:language "clojure" :launcher "jlc-clj-embedded"}
                                 {:language "ruby" :launcher "jlc-cli" :cmd ["bundle" "exec" "jlc_ruby"]}])
        project-languages (into #{} (:languages project-configs))]
    (when (:languages project-configs)
      (filter #(project-languages (:language %)) language-clients))))

(defn start
  "Start step coordinator."
  [glue-paths]
  (when-not @server
    (let [client-configs   (or (language-client-configs) (auto/detect))
          steps-registered (d/deferred)
          s                (http/start-server #'step-coordinator {:port 0})
          port             (aleph.netty/port s)]
      (log/debugf "Started on port %s" port)
      (log/debugf "")
      (reset! server s)
      (reset! client-count (count client-configs))
      (reset! registration-completed steps-registered)
      (doseq [client-config client-configs]
        (log/infof "Spinning up jukebox language client: %s" client-config)
        (step-client/launch client-config port glue-paths))
      steps-registered)))

(defn restart
  "Restart the step coordinator."
  [glue-paths]
  (stop)
  (start glue-paths))
