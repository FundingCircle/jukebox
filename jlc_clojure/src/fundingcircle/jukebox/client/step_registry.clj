(ns fundingcircle.jukebox.client.step-registry
  "Step registry."
  (:import (java.util UUID)))

(defonce definitions (atom []))
(defonce callbacks (atom {}))

(defn add
  "Add a step or hook to the step registry."
  [{:keys [triggers opts callback]}]
  (let [id (str (UUID/randomUUID))]
    (swap! callbacks assoc id callback)
    (swap! definitions conj {:id id
                             :triggers triggers
                             :opts opts})))

(defn run
  "Run a step or hook."
  [{:keys [id board args] :as message}]
  (let [callback (get @callbacks id)]
    (when-not callback (throw (ex-info "Undefined callback" {:message message})))
    (apply callback board args)))