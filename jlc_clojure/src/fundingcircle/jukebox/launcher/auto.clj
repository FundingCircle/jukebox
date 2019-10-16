(ns fundingcircle.jukebox.launcher.auto
  "Auto detect languages for the current project."
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

(require 'fundingcircle.jukebox.launcher.cli)
(require 'fundingcircle.jukebox.launcher.clj)

(defn detect
  "Detects what languages the current project uses, and return launch configs."
  ([] (detect {}))
  ([{:keys [clojure? ruby?]}]
   (let [clojure?         (or clojure?
                              (.exists (io/file "deps.edn"))
                              (.exists (io/file "project.clj")))
         ruby?            (or ruby?
                              (.exists (io/file "Gemfile")))
         language_clients (cond-> []
                                  clojure? (conj {:language "clojure" :launcher "jlc-clj-embedded"})
                                  ruby? (conj {:language "ruby" :launcher "jlc-cli" :cmd ["bundle" "exec" "jlc_ruby"]}))]
     (if (seq language_clients)
       (log/debugf "Detected project languages: %s" (mapv :language language_clients))
       (log/errorf "No project languages auto-detected"))
     language_clients)))
