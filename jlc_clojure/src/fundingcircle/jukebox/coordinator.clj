(ns fundingcircle.jukebox.coordinator
  "Coordinate with remote step executors."
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [fundingcircle.jukebox.step-registry :as step-registry]
            [fundingcircle.jukebox.launcher :as step-client]
            [fundingcircle.jukebox.launcher.auto :as auto]
            [msgpack.core :as msg])
  (:import (java.io DataOutputStream DataInputStream)
           (java.net ServerSocket)))

(require 'fundingcircle.jukebox.msg)

(defonce server-socket (atom nil))
(defonce clients (atom nil))
(defonce step-registry (atom (step-registry/create)))

(defn- send!
  "Send a message to a language client."
  [client-id message]
  (msg/pack-stream message (get-in @clients [client-id :out])))

(defn- recv!
  "Receive a message from a language client."
  [client-id]
  (msg/unpack-stream (get-in @clients [client-id :in])))

(defn- stack-trace-element
  "Deserialize a stack trace."
  [{:keys [class-name method-name file-name line-number] :as e}]
  (StackTraceElement. class-name method-name file-name line-number))

(defn stop
  "Stop step coordinator."
  []
  (swap! server-socket #(when % (.close %)))
  (doseq [])
  (reset! clients nil))

(defn drive-step
  "Find a jukebox language client that knows about `step`, and ask for it to be run."
  [step-registry id board args]
  (let [forwarder (get-in step-registry [:callbacks id])]
    (assert forwarder)
    (apply forwarder board args)))

(defn step-forwarder
  "Forwards a request to execute a step to the right jukebox language client."
  [id language client-id]
  (fn run-step [board & args]

    (send! client-id {:action :run
                      :id id
                      :board board
                      :args args})

    (let [result (recv! client-id)]
      (case (:action result)
        :result (:board result)
        :error (let [e (RuntimeException. (str "Step exception: " (:error result)))]
                 (.setStackTrace e (into-array StackTraceElement (mapv stack-trace-element (:trace result))))
                 (throw e))
        (throw (ex-info "Unknown message from client" {:message result :language language}))))))

(defn forwarder-by-id
  "Returns a [step-id step-forwarder] pair."
  [language client-id]
  (fn [{:keys [id]}]
    [id (step-forwarder id language client-id)]))

(defn- language-client-configs
  "Loads language client configs from a json file named '.jukebox'."
  []
  (let [project-configs   (into {} (-> (try (slurp ".jukebox") (catch Exception _ "{}"))
                                       (json/parse-string true)))
        language-clients  (into (:language-clients project-configs)
                                [{:language "clojure" :launcher "jlc-clj-embedded"}
                                 {:language "ruby" :launcher "jlc-cli" :cmd ["bundle" "exec" "jlc_ruby"]}])
        project-languages (into #{} (:languages project-configs))]
    (when (:languages project-configs)
      (filter #(project-languages (:language %)) language-clients))))

(defn register-clients
  "Accept incoming client connections and register steps."
  [client-configs]
  (dotimes [_ (count client-configs)]
    (let [socket    (.accept @server-socket)
          in        (DataInputStream. (.getInputStream socket))
          out       (DataOutputStream. (.getOutputStream socket))
          {:keys [client-id definitions language resources snippet]} (msg/unpack-stream in)
          callbacks (into {} (map (forwarder-by-id language client-id) definitions))]
      (swap! clients assoc client-id {:socket socket :out out :in in})
      (swap! step-registry step-registry/merge language snippet definitions callbacks resources)))
  @step-registry)

(defn start
  "Starts the step coordinator."
  [glue-paths]
  (reset! server-socket (ServerSocket. 0))
  (.setSoTimeout @server-socket 30000)
  (let [client-configs       (or (language-client-configs) (auto/detect {:clojure? true}))
        client-registrations (future (register-clients client-configs))]
    (println "Clients; " client-configs)
    (doseq [client-config client-configs]
      (future
        (try
          (step-client/launch client-config (.getLocalPort @server-socket) glue-paths)
          (catch Throwable e
            (log/errorf e "Error while launching jukebox language client")))))

    @client-registrations))

(defn restart
  "Restart the step coordinator."
  [glue-paths]
  (stop)
  (start glue-paths))
