(ns fundingcircle.jukebox.step-client.jlc-clojure
  "Clojure-language client for jukebox."
  (:require [aleph.http :as http]
            [cheshire.core :as json]
            [cheshire.parse :as parse]
            [clojure.tools.logging :as log]
            [fundingcircle.jukebox :as jukebox :refer [JukeBackend]]
            [manifold.stream :as s])
  (:import java.util.UUID))

(defn ->definitions
  "Return an atom that will be used to track step definitions as they are loaded."
  []
  (atom {:steps {} :before {} :after {} :before-step [] :after-step []}))

(defonce definitions
  (->definitions))

(deftype JLCJukeBackend [definitions]
  JukeBackend
  (register-step [_ pattern step-fn]
    (swap! definitions update :steps assoc pattern step-fn))

  (register-before-scene-hook [_ tags hook-fn]
    (swap! definitions update :before assoc tags hook-fn))

  (register-after-scene-hook [_ tags hook-fn]
    (swap! definitions update :after assoc tags hook-fn))

  (register-before-step-hook [_ hook-fn]
    (swap! definitions update :before-step conj {:hook-fn hook-fn}))

  (register-after-step-hook [_ hook-fn]
    (swap! definitions update :after-step conj {:hook-fn hook-fn})))

(def jlc-backend
  "A jukebox clojure backend."
  (->JLCJukeBackend definitions))

(defn scan-steps
  "Scan for step definitions."
  [glue-paths]
  (log/debugf "Glue paths: %s" glue-paths)
  (if (= 0 (count glue-paths))
    (jukebox/register jlc-backend (jukebox/hooks))
    (doseq [glue-path glue-paths]
      (jukebox/register jlc-backend (jukebox/hooks glue-path)))))

(defn execute-step
  "Execute a step."
  [{:keys [step board args] :as message}]
  (log/debugf "Received request to execute step: %s" {:step step :args args :board board })
  (if-let [f (get-in @definitions [:steps step])]
    (try
      (log/debugf "Running step: %s" {:step step :f f :args args :board board})
      (assoc message
             :action "result"
             :board (apply f board args))
      (catch Throwable e
        (log/debugf "Step threw exception: %s" {:step step :f f :args args :e e :board board})
        (assoc message
               :action "result"
               :error (.getMessage e)
               :trace (mapv (fn [t] {:class-name (.getClassName t)
                                     :file-name (.getFileName t)
                                     :line-number (.getLineNumber t)
                                     :method-name (.getMethodName t)})
                            (.getStackTrace e))
               :board board)))
    (do
      (log/errorf "Don't know how to handle step: %s" {:step step :f nil :args args :board board})
      (assoc message
             :action "error"
             :code "TODO"
             :message (format "Don't know how to handle step: %s" step)
             :board nil))))

(defonce ws (atom nil))

(defn stop
  "Stop the jukebox language client."
  []
  (when @ws
    (log/debugf "Stopping")
    (.close (:sock @ws))
    (reset! ws nil)))

(defn send!
  ""
  [message]
  (s/put! (:sock @ws) (json/generate-string message)))

(defn handle-coordinator-message
  ""
  [message]
  (if (= :timeout message)
    (log/errorf "timed out")
    (let [message (binding [parse/*use-bigdecimals?* true]
                    (json/parse-string message true))]
      (case (:action message)
        "step" (send! (execute-step message))
        "stop" (stop)
        (log/errorf "Don't know how to handle message: %s" message)))))

(defn client
  "Start the clojure step language client."
  [port glue-paths]
  (if @ws
    (stop)
    (do
      (log/debugf "Scanning for step definitions")
      (scan-steps glue-paths)

      (log/debugf "Connecting")
      (reset! ws {:sock @(http/websocket-client (format "ws://localhost:%s/jukebox" port))
                  :clientid (str (UUID/randomUUID))})

      (log/debugf "Listening for step instructions...")
      (s/consume #'handle-coordinator-message (:sock @ws))

      (log/debugf "Sending step inventory")
      (send! {"action" "register"
              "clientid" (:clientid @ws)
              "language" "clojure"
              "version" "1"
              "steps" (into [] (keys (:steps @definitions)))}))))
