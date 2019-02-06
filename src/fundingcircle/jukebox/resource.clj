(ns fundingcircle.jukebox.resource
  "Library for creating an inventory of resources needed to run tests."
  (:require [fundingcircle.jukebox :as jukebox]
            [yagni.graph :as graph]
            [yagni.jvm :as jvm]
            [yagni.namespace :as namesp]
            [yagni.namespace.form :as form]))

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

(defn- count-vars-in-ns
  "Calls `yagni.namespace.form/count-vars-in-ns`, ignoring unparsable
  namespaces."
  [graph ns]
  ;; The `potemkin` library attempts to "fix" various clojure problems
  ;; in ways that break the `macro-expand-1` call that `yagni`
  ;; makes. Ignoring namespaces that are built with potemkin (or are
  ;; unparsable for other reasons) seems fine for the moment, for the
  ;; purposes of the feature this code is providing.
  (try
    (form/count-vars-in-ns graph ns)
    (catch Throwable _)))

(defn- call-graph
  "Returns the call graph of every var in each of the namespaces."
  [namespaces]
  ;; This is the same as `yagni.core/construct-reference-graph`, but
  ;; with a couple of tweaks:
  ;;  - Instead of acting on a directory, it acts on a list of namespaces
  ;;  - `count-vars-in-ns` is overridden (above) to ignore unparsable namespaces
  (let [namespaces (->> (map ->sym namespaces)
                        (remove #{'clojure.core}))
        graph (atom (namesp/named-vars-map namespaces))
        generator-fns (jvm/find-generator-fns graph)]
    (jvm/extend-generators! graph generator-fns)
    ;; (form/count-vars graph namespaces)
    (doall (map (partial count-vars-in-ns graph) namespaces))
    (graph/prune-findable-nodes! graph nil (atom #{}))
    (jvm/compress-generators! graph generator-fns)
    @graph))

(defn- manifest
  "Returns a fn that obtains the inventory 'manifest' for a given var."
  [call-graph keys]
  (fn [[f]]
    (when (jukebox/scene-related? (->var f))
      (when-let [fcg (flattened-call-graph f call-graph)]
        (let [r (resources f keys)]
          [f (into (if r #{r} #{})
                   (keep #(resources % keys) fcg))])))))

(defn inventory
  "Returns a map of entry points to inventory manifest.

  Scans the paths for fn's with meta containing `keys` as entry
  points. Then scans each's entrypoint's call graph transitively,
  looking for fns also tagged with `keys` in their 'meta'."
  [keys]
  (let [cg (call-graph (all-ns))]
    (->> (keep (manifest cg keys) cg)
         (into {}))))
