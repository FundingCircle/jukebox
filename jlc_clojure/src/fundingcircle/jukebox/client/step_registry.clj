(ns fundingcircle.jukebox.client.step-registry
  "Step registry."
  (:require [clojure.tools.logging :as log])
  (:import (java.util UUID)))

(defn create
  "Creates a step registry."
  []
  {:definitions [] :callbacks {}})

(defn definitions
  "Gets the step definitions that have been loaded."
  [step-registry]
  (:definitions step-registry))

(defn callbacks
  "Gets the step callbacks that have been found."
  [step-registry]
  (:callbacks step-registry))

(defn entry-points
  "The list of entry point functions."
  [step-registry]
  (vals (callbacks step-registry)))

(defn add
  "Add a step or hook to the step registry."
  [step-registry {:keys [triggers opts callback]}]
  (let [id (str (UUID/randomUUID))
        tags (:scene/tags opts)
        tags (if (string? tags) [tags] tags)
        opts (dissoc (assoc opts :scene/tags tags) :tags)]
    (-> step-registry
        (update :callbacks assoc id callback)
        (update :definitions conj {:id id :triggers triggers :opts opts}))))

(defn run
  "Run a step or hook."
  [step-registry {:keys [id board args] :as message}]
  (let [callback (get-in step-registry [:callbacks id])]
    (when-not callback (throw (ex-info "Undefined callback" {:message message})))
    (log/debugf "Running step %s: %s" id {:board board :args args})
    (apply callback board args)))


(defn find-trigger
  "Finds the step definition for the trigger."
  [step-registry trigger]
  (first (filter #(some (fn [t] (= trigger t)) (:triggers %)) (:definitions step-registry))))
