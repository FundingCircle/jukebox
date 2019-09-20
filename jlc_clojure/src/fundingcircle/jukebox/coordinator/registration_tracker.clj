(ns fundingcircle.jukebox.coordinator.registration-tracker
  "Client registration tracking."
  (:require [clojure.tools.logging :as log]))

(defonce tracker (atom nil))

(defn init
  "Creates a new client registration tracker."
  [client-configs]
  (reset! tracker {:definitions []
                   :callbacks {}
                   :snippets {}
                   :pending-count (count client-configs)
                   :result (promise)}))

(defn error!
  "Call to register an error that occurred on another thread."
  [e]
  (deliver (:result @tracker) e))

(defn register!
  "Register a client."
  [{:keys [client-id definitions language snippet]}]
  (swap! tracker
         #(-> %
              (update :definitions into definitions)
              (update :snippets assoc :language language :snippet snippet)
              (update :pending-count dec)
              (update :callbacks merge
                      (->> (map (fn [{:keys [id]}] [id client-id]) definitions)
                           (into {})))))
  (log/debugf "Registered client: %s" language)
  (when (= 0 (:pending-count @tracker))
    (deliver (:result @tracker)
             (dissoc @tracker :pending-count :finished))))

(defn step-registry
  "Blocks until all clients have registered, then returns the registered steps."
  []
  (let [result @(:result @tracker)]
    (when (instance? Throwable result) (throw result))
    result))
