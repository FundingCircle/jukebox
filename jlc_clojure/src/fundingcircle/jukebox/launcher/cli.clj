(ns fundingcircle.jukebox.launcher.cli
  "A command-line based jukebox language client."
  (:require [me.raynes.conch.low-level :as sh]
            [clojure.tools.logging :as log]
            [fundingcircle.jukebox.launcher :refer [launch]]))

(defmethod launch "jlc-cli"
  [{:keys [cmd env dir]} port glue-paths]
  (log/debugf "Starting '%s' on port %s - glue paths: %s" cmd port glue-paths)
  (let [args      (concat cmd glue-paths ["--port" (str port) :env env :dir dir])
        p         (apply sh/proc args)
        exit-code (future (sh/exit-code p))]
    (future (sh/stream-to p :err (System/err)))
    (future (sh/stream-to p :out (System/out)))
    (when (not= 0 @exit-code)
      (throw (ex-info (format "Jukebox language client exited with non-zero status: %s" @exit-code) {})))))
