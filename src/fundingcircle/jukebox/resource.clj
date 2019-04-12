(ns fundingcircle.jukebox.resource
  "Library for creating an inventory of resources needed to run tests."
  (:require [fundingcircle.jukebox :as jukebox]
            [fundingcircle.jukebox.backend.cucumber :as cucumber]
            [yagni.graph :as graph]
            [yagni.jvm :as jvm]
            [yagni.namespace :as namesp]
            [yagni.namespace.form :as form])
  (:import cucumber.runtime.FeaturePathFeatureSupplier
           cucumber.runtime.io.MultiLoader
           cucumber.runtime.model.FeatureLoader))

(defn surrounding-step?
  "Checks whether a var is a surrounding step fn (:scene/before, :scene/after,
  :scene/before-step, or :scene/after-step)."
  [v]
  (let [step? (->> (meta v)
                   (keys)
                   (some #{:scene/before
                           :scene/after
                           :scene/before-step
                           :scene/after-step}))]
    (when step? v)))

(defn- flattened-call-graph
  "Given a step definition, returns the set of all functions that are
  called by it, transitively."
  ([step call-graphs]
   (flattened-call-graph step call-graphs (atom #{})))
  ([step call-graphs visited]
   (when-not (@visited step)
     (swap! visited conj step)
     (let [called-fns (->> (call-graphs step)
                           (remove #{(find-ns 'clojure.core)
                                     (find-ns 'clojure.spec.alpha)}))]
       (when (seq called-fns)
         (apply concat
                called-fns
                (map #(flattened-call-graph % call-graphs visited) called-fns)))))))

(defn- ->var
  "Ensures that `s` is a var."
  [s]
  (if (symbol? s)
    (resolve s)
    s))

(defn- ->sym
  "Ensures that `v` is a symbol."
  [v]
  (if (symbol? v)
    v
    (symbol (str v))))

(defn- resources
  "If the meta on `v` contains any of the `keys`, returns the meta."
  [v keys]
  (let [m (meta (->var v))]
    (when-not (empty? (select-keys m keys))
      m)))

(defn ignore-exceptions
  [f]
  (fn [& args]
    (try
      (apply f args)
      (catch Throwable _))))

;; The `potemkin` library attempts to "fix" various clojure problems
;; in ways that break the `macro-expand-1` call that `yagni`
;; makes. (Code that this is scanning can use potemkin.) Ignoring
;; namespaces that are built with potemkin (or are unparsable for
;; other reasons) seems fine for the moment, for the purpose of the
;; feature this code is providing.
(alter-var-root #'form/count-vars-in-ns ignore-exceptions)

(defn- call-graph
  "Returns the call graph of every var in each of the namespaces."
  [namespaces entry-points]
  ;; This is the same as `yagni.core/construct-reference-graph`, but
  ;; with a couple of tweaks:
  ;;  - Instead of acting on a directory, it acts on a list of namespaces
  ;;  - `count-vars-in-ns` is overridden (above) to ignore unparsable namespaces
  (let [namespaces (->> (map ->sym namespaces)
                        (remove #{'clojure.core}))
        graph (atom (namesp/named-vars-map namespaces))
        generator-fns (jvm/find-generator-fns graph)
        entry-points (map ->var entry-points)]
    (jvm/extend-generators! graph generator-fns)
    (form/count-vars graph namespaces)
    (graph/prune-findable-nodes! graph entry-points (atom #{}))
    (jvm/compress-generators! graph generator-fns)
    @graph))

(defn- manifest
  "Returns a fn that obtains the inventory 'manifest' for a given var."
  [call-graph keys]
  (fn [[f]]
    (when (jukebox/scene-related? (->var f))
      (let [fcg (flattened-call-graph f call-graph)]
        (let [r (resources f keys)]
          [f (into (if r #{r} #{})
                   (keep #(resources % keys) fcg))])))))
(defn parse-tags
  "Parses the `--tags` command line option."
  [cli-args]
  (cucumber.runtime.RuntimeOptions. cli-args))

(defn- pickle-steps
  "Returns the list of `gherkin.pickle.PickleStep`s in the features
  matching `tags`."
  [tags]
  (let [tags (or tags (cucumber.runtime.RuntimeOptions. []))
        filters (cucumber.runtime.filter.Filters. tags)]
   (->>
    (-> (Thread/currentThread)
        (.getContextClassLoader)
        (MultiLoader.)
        (FeatureLoader.)
        (FeaturePathFeatureSupplier. tags)
        (.get))
    (mapcat #(.getPickles %))
    (filter #(.matchesFilters filters %))
    (mapcat #(.getSteps (.pickle %))))))

(defn- step-definition-for-pickle-step
  "Returns the step-definition that matches the `pickle-step."
  [step-definitions pickle-step]
  (:step-fn (first (filter #(.matchedArguments % pickle-step) step-definitions))))

(defn- step-definitions
  "Returns the step-definitions (clojure fn) matching `tags`."
  [tags]
  (let [definitions (cucumber/->definitions)
        hooks (jukebox/hooks)]
    (jukebox/register (cucumber/->CucumberJukeBackend definitions) hooks)

    (let [step-definitions
          (map
           (fn [{:keys [pattern step-fn]}]
             (cucumber/->JukeStepDefinition pattern step-fn (meta step-fn)))
           (:steps @definitions))]

      (->> (pickle-steps tags)
           (map #(step-definition-for-pickle-step step-definitions %))
           (concat (filter surrounding-step? hooks))
           (map ->sym)
           (into #{})))))

(defn inventory
  "Returns a map of entry points to inventory manifest for features matching `tags` (see `parse-tags`)."
  [keys tags]
  (let [entry-points (step-definitions tags)
        cg (call-graph (all-ns) entry-points)
        entry-points (into #{} (map symbol entry-points))]
    (->> (keep (manifest cg keys) cg)
         (map (fn [[k v]]
                [k (into #{} (map #(select-keys % keys) v))]))
         (filter (fn [[k v]]
                   (and (entry-points (->sym (resolve k)))
                        (not= #{} v))))
         (into {}))))
