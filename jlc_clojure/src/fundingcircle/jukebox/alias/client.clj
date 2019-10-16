(ns fundingcircle.jukebox.alias.client
  "Entry point for the jukebox language client for clojure."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [fundingcircle.jukebox.client :as client]))

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
  [glue_paths]
  (or (seq glue_paths)
      (:paths (slurp-deps))
      ["src" "test" "src/main/clojure" "src/test/clojure"]))


(def ^:private cli-options
  "Command line options."
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x65536) "Invalid port number"]]
   ["-h" "--help" "Prints this help"]])

(defn- banner
  "Print the command line banner."
  [summary]
  (println "Usage: jlc_clojure [options] <glue paths>.\n%s")
  (println summary))

(defn -main
  "Launch the clojure jukebox language client from the command line."
  [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)
      (banner summary)

      (not (:port options))
      (banner summary)

      errors
      (do
        (binding [*out* *err*] (println (str/join \newline errors)))
        (System/exit 1))

      :else (try
              (client/start nil (:port options) (glue-paths arguments))
              (catch Throwable e
                (prn e)
                (.printStackTrace e))))))
