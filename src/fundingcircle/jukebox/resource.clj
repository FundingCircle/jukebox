(ns fundingcircle.jukebox.resource
  "Library for creating an inventory of resources needed to run tests."
  (:require [fundingcircle.jukebox :as jukebox]
            [yagni.core :as yagni]))

(defn- flattened-call-graph
  "Given a step definition, returns the set of all functions that are
  called by it, transitively."
  [step call-graphs]
  (let [called-fns (call-graphs step)]
    (when (seq called-fns)
      (apply concat
             called-fns
             (map #(flattened-call-graph % call-graphs) called-fns)))))

(defn- resources
  "Returns the resources a fn is tagged with."
  [v]
  (when (symbol? v)
    (let [r (-> (resolve v)
                (meta)
                (select-keys [:scene/resource-type :scene/resource-caps]))]
      (when-not (empty? r)
        r))))

(defn inventory
  "Scans the paths for step definitions, and generates a map of step
  definitions to resource inventory."
  [source-paths]
  (let [cg  @(yagni/construct-reference-graph source-paths)]
    (->> cg
         (keep (fn [[f _]]
                 (if-let [fcg (flattened-call-graph f cg)]
                   [f fcg])))
         (map (fn [[f calls]] [f (into #{(resources f)}
                                       (keep resources calls))]))
         (into {}))))
