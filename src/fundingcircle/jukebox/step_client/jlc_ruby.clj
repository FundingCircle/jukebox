(ns fundingcircle.jukebox.step-client.jlc-ruby
  "Ruby-language client for jukebox."
  (:require [me.raynes.conch.low-level :as sh]
            [clojure.java.io :as io]))

(defn logf
  "Log a messagate to stdout."
  [fmt & args]
  (.printf (System/out) (str "JLC (ruby): " fmt "\n") (into-array Object args)))

(defn bashc
  [cmd & args]
  (let [p (apply sh/proc "bash" "-c" cmd args)
        out (future (sh/stream-to p :out (System/out)))
        err (future (sh/stream-to p :err (System/err)))
        exit (future (sh/exit-code p))]
    {:out out :err err :exit exit}))

(defn client
  "Start the clojure step language client."
  [port glue-paths]
  (logf "... Starting jlc_ruby on port %s" port)
  (bashc (str "jlc_ruby --port " port)))
