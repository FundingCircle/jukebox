(ns fundingcircle.jukebox.client.clojure-lang
  "Client for clojure steps."
  (:require [aleph.http :as http]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
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
  [{:strs [step board args] :as message}]
  (if-let [f (get-in definitions :steps step)]
    (assoc message
           "action" "result"
           "board" (f board args))
    (assoc message
           "action" "error"
           "code" "TODO"
           "message" (format "Don't know how to handle step: %s" step)
           "board" nil)))

(defonce ws (atom nil))

(defn stop
  "Stop the jukebox language client."
  []
  (when @ws
    (.close (:sock @ws))
    (alter-var-root #'ws (constantly nil))))

(defn start
  "Start the clojure step language client."
  [port glue-paths]
  (when-not @ws
    (println "Scanning for step definitions")
    (scan-steps glue-paths)

    (println "Connecting")
    (reset! ws {:sock @(http/websocket-client (format "ws://localhost:%s/jukebox" port))
                :clientid (UUID/randomUUID)})

    (println "Sending step inventory")
    (let [m {"action" "register"
             "clientid" (:clientid @ws)
             "language" "clojure"
             "version" "1"
             "steps" (into [] (keys (:steps @definitions)))}]
      (println "Sending %s" m)
      (s/put! (:sock @ws) {"action" "register"
                           "clientid" (:clientid @ws)
                           "language" "clojure"
                           "version" "1"
                           "steps" (into [] (keys (:steps @definitions)))}))

    (println "Listening for instructions...")
    #_(loop [message @(s/take! (:sock @ws))]
      (case (get message "action")
        "step" (s/put! (:sock @ws) (execute-step message))
        "stop" (stop)
        (log/errorf "Don't know how to handle message: %s" message))
      (recur @(s/take! (:sock @ws))))))

(comment
  (start 9453 ["test/example"])
  (stop)

  sock
  (def r (s/take! @sock))
  @r


  )
