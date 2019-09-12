(ns fundingcircle.jukebox.client
  "A jukebox language client for clojure."
  (:require [aleph.http :as http]
            [manifold.stream :as s]
            [cheshire.parse :as parse]
            [cheshire.core :as json]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [fundingcircle.jukebox.client.step-registry :as registry]
            [fundingcircle.jukebox.client.step-scanner :as scanner]
            [clojure.string :as str])
  (:import (java.util UUID))
  (:gen-class))

(defonce ws (atom nil))
(defonce local-board (atom {}))

(defn- ->transmittable
  "Removes non-json-able entries from the map, stashing non-serializable values in `local-board`."
  ([m] (->transmittable m []))
  ([m ks]
   (let [local-board (volatile! nil)
         board (->transmittable m ks local-board)]
     {:board board :local-board @local-board}))
  ([m ks local-board]
   (into {}
    (for [[k v] m]
      (cond
        (map? v)
        [k (->transmittable v (conj ks k) local-board)]

        ;; TODO: vectors

        :else
        (try
          (json/generate-string v)
          [k v]
          (catch Throwable _
            (log/warnf "Note: Board entry can't be transmitted across languages: %s" [ks v])
            (vswap! local-board assoc (conj ks k) v)
            nil)))))))

(defn- transmittable->
  "Parse json & merge with local board"
  [s]
  (reduce (fn [m [ks v]] (assoc-in m ks v))
          (binding [parse/*use-bigdecimals?* true]
            (json/parse-string s true))
          @local-board))

(defn send!
  "Send a message to the jukebox coordinator."
  [message]
  (log/debugf "Sending message to coordinator: %s" message)
  (let [jsonable (->transmittable message)]
    (reset! local-board (:local-board jsonable))
    (s/put! @ws (json/generate-string (:board jsonable)))))

(defn error
  "Create an error response message."
  [message e]
  (log/debugf "Step threw exception: %s" e)
  (log/debugf "%s" (.getStackTrace e))
  (assoc message
    :action "error"
    :error (.getMessage e)
    :trace (mapv (fn [t] {:class-name (.getClassName t)
                          :file-name (.getFileName t)
                          :line-number (.getLineNumber t)
                          :method-name (.getMethodName t)})
                 (.getStackTrace e))))

(defn run
  "Runs a step or hook."
  [message]
  (try
    (assoc message
      :action "result"
      :board (registry/run message))
    (catch Throwable e
      (error message e))))

(defn handle-coordinator-message
  "Handle messages from the coordinator"
  [message]
  (try
    (log/debugf "Coordinator message: %s" message)
    (let [message (transmittable-> message)]
      (try
        (case (:action message)
          "run" (send! (run message))
          (throw (ex-info (format "Unknown action: %s" message) {})))
        (catch Throwable e (error message e))))             ;; Error handling message
    (catch Throwable e                                      ;; Error parsing message
      (send! (error {} e)))))

(def ^:private template (str
                          "  (defn {2}\n"
                          "    \"Returns an updated context (`board`).\"\n"
                          "    '{':scene/step \"{1}\"'}'\n"
                          "    [{3}]\n"
                          "    ;; {4}\n"
                          "    (throw (cucumber.api.PendingException.))\n"
                          "    board) ;; Return the board\n"))

(defn client-info
  "Client details for this jukebox client."
  ([] (client-info (str (UUID/randomUUID))))
  ([client-id]
   {"action" "register"
    "client-id" client-id
    "language" "clojure"
    "version" "1"
    "definitions" @registry/definitions
    "snippet" {"argument-joiner" " "
               "escape-pattern" ["\"" "\\\""]
               "template" template}}))

(defn start
  "Start this jukebox language client."
  [_client-config port glue-paths]
  (scanner/load-step-definitions! glue-paths)
  (reset! ws @(http/websocket-client (format "ws://localhost:%s/jukebox" port)))

  (s/consume handle-coordinator-message @ws)
  (send! (client-info)))

(def ^:private cli-options
  "Command line options."
  [["-p" "--port PORT" "Port number"]
   ["-h" "--help" "Prints this help"]])

(defn- banner
  "Print the command line banner."
  [summary]
  (println "Usage: jlc_clojure [options] <glue paths>.\n%s")
  (println summary))

(defn -main
  ""
  [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)
      (banner summary)

      errors
      (println (str/join \newline errors))

      :else (start nil (:port options) arguments))))
