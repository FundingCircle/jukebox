(ns fundingcircle.jukebox.launcher.auto
  "Auto detect languages for the current project."
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

(require 'fundingcircle.jukebox.launcher.cli)
(require 'fundingcircle.jukebox.launcher.clj)

(defn detect
  "Detect what languages the current project uses, and return launch configs."
  []
  (let [clojure?  (or (.exists (io/file "deps.edn"))
                      (.exists (io/file "project.clj")))
        ruby?     (.exists (io/file "Gemfile"))
        languages (cond-> []
                          clojure? (conj {:language "clojure" :launcher "jlc-clj-embedded"})
                          ruby? (conj {:language "ruby" :launcher "jlc-cli" :cmd ["bundle" "exec" "jlc_ruby"]}))]
    (if (seq languages)
      (log/debugf "Detected project languages: %s" (mapv :language languages))
      (log/errorf "No project languages detected"))
    languages))
