(ns fundingcircle.jukebox.alias.inventory
  "Entry point for generating an inventory of steps tagged with`:scene/resources`."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [fundingcircle.jukebox.coordinator :as coordinator]))

(defn- slurp-deps
  "Loads `deps.edn` in the current working directory."
  []
  (let [deps (io/file "deps.edn")]
    (when (.exists deps)
      (edn/read-string (slurp deps)))))

(defn- glue-paths
  "Returns the list of glue paths.

  - If paths are provided in args as `-g` or `--glue` options, those are used
  - Otherwise, `:paths` in `deps.edn`
  - Otherwise, `[\"src\", \"test\", \"src/main/clojure\", \"src/test/clojure\"]`"
  [args]
  (if (some #{"-g" "--glue"} args)
    (mapv second (partition 2 args))
    (->>
      (or (:paths (slurp-deps))
          ["src" "test" "src/main/clojure" "src/test/clojure"])
      (into []))))

(defn -main [& args]
  (let [{:keys [resources]} (coordinator/start (glue-paths args))]
    (coordinator/stop)
    (doseq [resource resources]
      (println resource))
    (System/exit 0)))                                       ;; TODO: Fix
