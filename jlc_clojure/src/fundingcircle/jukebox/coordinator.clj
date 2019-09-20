(ns fundingcircle.jukebox.coordinator
  "Coordinate with remote step executors."
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [fundingcircle.jukebox.coordinator.error-tracker :as error-tracker :refer [with-error-tracking with-future-error-tracking]]
            [fundingcircle.jukebox.coordinator.registration-tracker :as registration-tracker]
            [fundingcircle.jukebox.launcher :as step-client]
            [fundingcircle.jukebox.launcher.auto :as auto]
            [msgpack.core :as msg])
  (:import (java.io DataOutputStream DataInputStream)
           (java.net ServerSocket)))

(require 'fundingcircle.jukebox.msg)

(defonce server-socket (atom nil))
(defonce clients (atom nil))

(defn send!
  "Send a message to a language client."
  [client-id message]
  (msg/pack-stream message (get-in @clients [client-id :out])))

(defn recv!
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
  (reset! clients nil)
  (error-tracker/reset))

(defn drive-step
  "Find a jukebox language client that knows about `step`, and ask for it to be run."
  [step-registry id board args]
  (with-error-tracking
    (let [client-id (get (:callbacks step-registry) id)]
      (assert client-id)

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
          (throw (ex-info "Unknown message from client" {:message result})))))))

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

(defn handle-client-registrations
  "Handles client connections."
  []
  (try
    (while (not (.isClosed @server-socket))
      (let [socket (.accept @server-socket)
            in     (DataInputStream. (.getInputStream socket))
            out    (DataOutputStream. (.getOutputStream socket))
            {:keys [client-id] :as message} (msg/unpack-stream in)]
        (swap! clients assoc client-id {:socket socket :out out :in in})
        (registration-tracker/register! message)))
    (catch java.net.SocketException _
      (log/debugf "Socket closed"))))

(defn start
  "Starts the step coordinator."
  [glue-paths]
  (let [client-configs (or (language-client-configs) (auto/detect {:clojure? true}))]
    (reset! server-socket (ServerSocket. 0))
    (registration-tracker/init client-configs)
    (with-future-error-tracking
      (handle-client-registrations))

    (doseq [client-config client-configs]
      (with-future-error-tracking
        (step-client/launch client-config (.getLocalPort @server-socket) glue-paths)))

    (registration-tracker/step-registry)))

(defn restart
  "Restart the step coordinator."
  [glue-paths]
  (stop)
  (start glue-paths))

