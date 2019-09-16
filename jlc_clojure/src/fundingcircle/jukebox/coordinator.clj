(ns fundingcircle.jukebox.coordinator
  "Coordinate with remote step executors."
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [fundingcircle.jukebox.launcher :as step-client]
            [fundingcircle.jukebox.launcher.auto :as auto]
            [msgpack.core :as msg]
            [clojure.string :as str])
  (:import (java.io Closeable DataOutputStream DataInputStream)
           (java.net ServerSocket)))

(require 'fundingcircle.jukebox.msg)

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
  (msg/pack-stream message (get-in @ws [client-id :out])))

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
      (reset! result-received (promise))
      (send! client-id {:action :run
                        :id id
                        :board board
                        :args args})
      (let [result @@result-received]
        (log/debugf "Result received: %s" result)
        (case (:action result)
          :result (:board result)
          :error (let [e (RuntimeException. (str "Step exception: " (:error result)))]
                   (.setStackTrace e (into-array StackTraceElement (mapv stack-trace-element (:trace result))))
                   (throw e))
          (do
            (log/errorf "Don't know how to handle step message: %s" result)
            (stop)))))
    (do
      (log/errorf "No client knows how to handle step: %s" id)
      board)))

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
    :result (deliver @result-received message)
    :error (deliver @result-received message)
    (do
      (log/errorf "Don't know how to handle client message: %s" message)
      (stop))))

(defn- language-client-configs
  "Load language client configs from a json file named `.jukebox` on the classpath."
  []
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
    (let [socket (.accept server-socket)
          in     (DataInputStream. (.getInputStream socket))
          out    (DataOutputStream. (.getOutputStream socket))]
      (future
        (try
          (let [message-data (msg/unpack-stream in)
                {:keys [client-id] :as message} message-data]
            (do
              (register-client-steps message)
              (swap! ws assoc client-id {:socket socket :out out})
              (swap! client-count dec)
              (swap! client-ids conj client-id)
              (when (= 0 @client-count)
                (deliver @registration-completed {:definitions @definitions :snippets @snippets}))
              (loop [message (msg/unpack-stream in)]
                (handle-client-message message)
                (recur (msg/unpack-stream in)))))
          (catch Throwable e
            (.printStackTrace e)))))))

(defn start
  "Starts the step coordinator."
  [glue-paths]
  (when-not @server
    (let [client-configs   (or (language-client-configs) (auto/detect))
          steps-registered (promise)
          s                (ServerSocket. 0)
          port             (.getLocalPort s)]
      (reset! server s)
      (reset! client-count (count client-configs))
      (reset! registration-completed steps-registered)
      (future (handle-client-connections s))

      (log/infof "Spinning up jukebox language clients: %s" (str/join ", " (map :language client-configs)))
      (doseq [client-config client-configs]
        (step-client/launch client-config port glue-paths))
      steps-registered)))

(defn restart
  "Restart the step coordinator."
  [glue-paths]
  (stop)
  (start glue-paths))

