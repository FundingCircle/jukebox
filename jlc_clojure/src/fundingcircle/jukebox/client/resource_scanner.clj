(ns fundingcircle.jukebox.client.resource-scanner
  "Library for creating an inventory of resources needed to run tests."
  (:require [fundingcircle.jukebox.client.step-scanner :as scanner]
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
  [v meta-keys]
  (let [m (meta (->var v))]
    (when-not (empty? (select-keys m meta-keys))
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

(def ^:private
  blacklisted-namespaces
 [#"clojure.*" #"manifold.*"])

(defn- blacklisted-namespace?
  "Predicate matches blacklisted namespaces."
  [n]
  (let [n (str n)]
    (some #(re-matches % n) blacklisted-namespaces)))

(defn- call-graph
  "Returns the call graph of every var in each of the namespaces."
  [namespaces entry-points]
  ;; This is the same as `yagni.core/construct-reference-graph`, but
  ;; with a couple of tweaks:
  ;;  - Instead of acting on a directory, it acts on a list of namespaces
  ;;  - `count-vars-in-ns` is overridden (above) to ignore unparsable namespaces
  (let [namespaces    (->> (map ->sym namespaces)
                           (remove blacklisted-namespace?))
        graph         (atom (namesp/named-vars-map namespaces))
        generator-fns (jvm/find-generator-fns graph)
        entry-points  (map ->var entry-points)]
    (jvm/extend-generators! graph generator-fns)
    (form/count-vars graph namespaces)
    (graph/prune-findable-nodes! graph entry-points (atom #{}))
    (jvm/compress-generators! graph generator-fns)
    @graph))

(defn- manifest
  "Returns a fn that obtains the inventory 'manifest' for a given var."
  [call-graph keys]
  (fn [[f]]
    (when (scanner/scene-related? (->var f))
      (let [fcg (flattened-call-graph f call-graph)]
        (let [r (resources f keys)]
          [f (into (if r #{r} #{})
                   (keep #(resources % keys) fcg))])))))

(defn inventory
  "Returns a map of entry points to inventory manifest for features matching `tags` (see `parse-tags`)."
  [entry-points tags]
  (let [call-graph (call-graph (all-ns) entry-points)]
    (->> (keep (manifest call-graph tags) call-graph)
         (map (fn [[k v]]
                [k (into #{} (map #(select-keys % tags) v))]))
         (filter (comp seq second) #_(fn [[_ v]] (not= #{} v)))
         (into {}))))
