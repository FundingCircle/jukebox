(ns fundingcircle.jukebox.step-client.jlc-cli
  "A command-line based jukebox language client."
  (:require [me.raynes.conch.low-level :as sh]
            [clojure.tools.logging :as log]
            [fundingcircle.jukebox.step-client :refer [launch]]))

(defmethod launch "jlc-cli"
  [{:keys [cmd env dir]} port glue-paths]
  (log/debugf "Starting '%s' on port %s - glue paths: %s" cmd port glue-paths)
  (let [args (concat cmd glue-paths ["--port" (str port) :env env :dir dir])
        p (apply sh/proc args)
        out (future (sh/stream-to p :out (System/out)))
        err (future (sh/stream-to p :err (System/err)))
        exit (future (sh/exit-code p))]
    {:out out :err err :exit exit}))

