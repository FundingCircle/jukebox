(ns fundingcircle.jukebox.coordinator
  "Coordinate with remote step executors."
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [fundingcircle.jukebox.launcher :as step-client]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [fundingcircle.jukebox.launcher.auto :as auto]
            [manifold.deferred :as d]
            [msgpack.core :as msg])
  (:import (java.io Closeable DataOutputStream DataInputStream)
           (java.net ServerSocket)))

(require 'msgpack.clojure-extensions)

;; Jukebox language client launchers
(require 'fundingcircle.jukebox.launcher.clj)

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
  (try
    (log/debugf "Sending message to client %s: %s: %s" client-id message (get-in @ws [client-id :out]))
    (msg/pack-stream message (get-in @ws [client-id :out]))
    (catch Throwable e
      (log/debugf "Failed to send message: %s" message)
      (throw e))))

(defn- stack-trace-element
  "Deserialize a stack trace."
  [{:keys [class-name method-name file-name line-number]}]
  (StackTraceElement. class-name method-name file-name line-number))

(defonce server (atom nil))

(defn stop
  "Stop step coordinator."
  []
  (when @server
    (.close ^Closeable @server))
  (reset! ws nil)
  (reset! server nil)
  (reset! callbacks {})
  (reset! definitions [])
  (reset! snippets [])
  (reset! result-received nil)
  (reset! client-ids #{})
  (reset! client-count 0))

(defn drive-step
  "Find a jukebox language client that knows about `step`, and ask for it to be run."
  [id board args]
  (if-let [client-id (get @callbacks id)]
    (do
      (reset! result-received (d/deferred))
      (send! client-id {"action" "run"
                        "id" id
                        "board" board
                        "args" args})
      (let [result @@result-received]
        (log/debugf "Result received: %s" result)
        (case (:action result)
          :result (:board result)
          :error (let [e (RuntimeException. (str "Step exception: " (get result "error")))]
                    (.setStackTrace e (into-array StackTraceElement (mapv stack-trace-element (:trace result))))
                    (throw e))
          (do
            (log/errorf "Don't know how to handle message: %s" result)
            (stop)))))
    (do
      (log/errorf "No client knows how to handle step: %s" id)
      board)))

(defn drive-hook
  "Run hooks on all language clients."
  [id board scenario]
  (let [client-id (get @callbacks id)]
    (reset! result-received (d/deferred))
    (send! client-id {"action" "run"
                      "id" id
                      "board" board
                      "args" [scenario]})
    (let [result @@result-received]
      (log/debugf "Hook Result received: %s" result)
      (log/debugf "Hook board received: %s" (:board result))
      (case (:action result)
        :result (:board result)
        :error (let [e (RuntimeException. (str "Step exception: " (get result "error")))]
                  (.setStackTrace e (into-array StackTraceElement (mapv stack-trace-element (:trace result))))
                  (throw e))
        (do
          (log/errorf "Don't know how to handle message: %s" result)
          (stop))))))

(defn register-client-steps
  "Register steps that a jukebox language client knows how to handle."
  [{:keys [client-id language] :as message}]
  (log/debugf "Registering client steps (%s, %s): %s\n" language client-id message)
  (swap! definitions into (:definitions message))
  (swap! callbacks merge
         (->> (map (fn [{:keys [id]}] [id client-id]) (:definitions message))
              (into {})))
  (swap! snippets conj (assoc (:snippet message) :language language))
  {:status 202})

(defn handle-client-message
  ""
  [message]
  (log/debugf "Handling message from client: %s" message)
  (case (:action message)
    "result" (d/success! @result-received message)
    "error" (d/success! @result-received message)
    (do
      (log/errorf "Don't know how to handle message: %s" message)
      (stop))))

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

(defn handle-client-connections
  "Handles client connections."
  [server-socket]
  (while (not (.isClosed server-socket))
    (log/debug "Waiting for new client connections")
    (let [socket (.accept server-socket)
          in     (DataInputStream. (.getInputStream socket))
          out    (DataOutputStream. (.getOutputStream socket))]
      (future
        (try
          (log/debug "Waiting for first client message")
          (let [{:keys [client-id] :as message} (msg/unpack-stream in)]
            (do
              (log/debugf "Established ws connection with language client: %s" client-id)
              (register-client-steps message)
              (swap! ws assoc client-id {:socket socket :out out})
              (swap! client-count dec)
              (swap! client-ids conj client-id)
              (log/debugf "Client count: %s" @client-count)
              (when (= 0 @client-count)
                (d/success! @registration-completed {:definitions @definitions :snippets @snippets}))
              (log/debug "Consuming messages from client")
              (loop [message (transform-keys keyword (msg/unpack-stream in))]
                  (handle-client-message message)
                  (recur (transform-keys keyword (msg/unpack-stream in)))))
            (log/errorf "Didn't get registration message"))
          (catch Throwable e
            (.printStackTrace e)))))))

(defn start
  "Starts the step coordinator."
  [glue-paths]
  (when-not @server
    (log/debug "Starting coordinator")
    (let [client-configs   (or (language-client-configs) (auto/detect))
          steps-registered (d/deferred)
          s                (ServerSocket. 0)
          port             (.getLocalPort s)]
      (log/debugf "Started on port %s" port)
      (future (handle-client-connections s))
      (reset! server s)
      (reset! client-count (count client-configs))
      (reset! registration-completed steps-registered)
      (doseq [client-config client-configs]
        (log/infof "Spinning up jukebox language client: %s" client-config)
        (step-client/launch client-config port glue-paths))
      (log/debug "Waiting for steps to be registered")
      steps-registered)))

(defn restart
  "Restart the step coordinator."
  [glue-paths]
  (stop)
  (start glue-paths))

