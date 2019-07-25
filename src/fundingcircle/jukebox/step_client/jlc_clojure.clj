(ns fundingcircle.jukebox.step-client.jlc-clojure
  "Clojure-language client for jukebox."
  (:require [aleph.http :as http]
            [cheshire.core :as json]
            [cheshire.parse :as parse]
            [clojure.tools.logging :as log]
            [fundingcircle.jukebox :as jukebox :refer [JukeBackend]]
            [manifold.stream :as s])
  (:import java.util.UUID))

(defn logf
  "Log a messagate to stdout."
  [fmt & args]
  (.printf (System/out) (str "JLC (clojure): " fmt "\n") (into-array Object args)))

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
  (logf "Received request to execute step: %s" {:step step :board board :args args})
  (if-let [f (get-in @definitions [:steps step])]
    (try
      (assoc message
             :action "result"
             :board (apply f board args))
      (catch Throwable e
        (assoc message
               :action "result"
               :error (.getMessage e)
               :trace (mapv str (.getStackTrace e))
               :board board)))
    (assoc message
           :action "error"
           :code "TODO"
           :message (format "Don't know how to handle step: %s" step)
           :board nil)))

(defonce ws (atom nil))

(defn stop
  "Stop the jukebox language client."
  []
  (when @ws
    (logf "Stopping")
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
    (logf "timed out")
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
      (logf "Scanning for step definitions")
      (scan-steps glue-paths)

      (logf "Connecting")
      (reset! ws {:sock @(http/websocket-client (format "ws://localhost:%s/jukebox" port))
                  :clientid (str (UUID/randomUUID))})

      (logf "Listening for step instructions...")
      (s/consume #'handle-coordinator-message (:sock @ws))

      (logf "Sending step inventory")
      (send! {"action" "register"
              "clientid" (:clientid @ws)
              "language" "clojure"
              "version" "1"
              "steps" (into [] (keys (:steps @definitions)))}))))
