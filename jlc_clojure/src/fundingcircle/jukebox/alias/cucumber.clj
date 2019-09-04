(ns fundingcircle.jukebox.alias.cucumber
  "Entry point for cucumber command."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

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
  (if-not (some #{"-g" "--glue"} args)
    (->>
     (interleave
      (repeat "--glue")
      (or (:paths (slurp-deps))
          ["src" "test" "src/main/clojure" "src/test/clojure"]))
     (into []))))

(defn -main [& args]
  (cucumber.api.cli.Main/main
   (into-array String (concat (glue-paths args) args))))
