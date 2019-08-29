(ns fundingcircle.jukebox.step-client.jlc-jruby
  "An in-process jruby clojure language client."
  (:require [clojure.tools.logging :as log]
            [fundingcircle.jukebox.step-client :refer [launch]]
            [clojure.java.io :as io]))

(defmethod launch "jruby-embedded"
  [{:keys [language] :as client-config} port glue-paths]
  (future
    (try
      (log/debugf "Starting embedded script: '%s' on port %s - glue paths: %s" language port glue-paths)
      ;; TODO: jruby
      #_(JarMain/main (into-array String (into ["--port" (str port)] glue-paths)))
      (catch Exception e
        (.printStackTrace e)))))
