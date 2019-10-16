(ns fundingcircle.jukebox.step-registry
  "Step registry."
  (:refer-clojure :exclude [merge])
  (:import (java.util UUID)))

(defn create
  "Creates a step registry."
  []
  {:snippets {} :definitions [] :callbacks {} :resources []})

(defn definitions
  "Gets the step definitions that have been loaded."
  [step-registry]
  (:definitions step-registry))

(defn callbacks
  "Gets the step callbacks that have been found."
  [step-registry]
  (:callbacks step-registry))

(defn merge
  "Merge step registry."
  [step-registry language snippet definitions callbacks resources]
  (-> step-registry
      (update :snippets assoc language snippet)
      (update :definitions into definitions)
      (update :callbacks clojure.core/merge callbacks)
      (update :resources into resources)))

(defn register-step
  "Registers a step or hook definition."
  [step-registry {:keys [triggers opts callback]}]
  (let [id   (str (UUID/randomUUID))
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
    (apply callback board args)))


(defn find-definition
  "Finds the step definition for the trigger."
  [step-registry trigger]
  (first
    (filter
      #(some (fn [t] (= trigger t)) (:triggers %))
      (:definitions step-registry))))
