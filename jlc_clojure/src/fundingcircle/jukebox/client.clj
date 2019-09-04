(ns fundingcircle.jukebox.client
  "A jukebox language client for clojure."
  (:require [aleph.http :as http]
            [manifold.stream :as s]
            [cheshire.parse :as parse]
            [cheshire.core :as json]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [fundingcircle.jukebox.client.step-registry :as registry]
            [fundingcircle.jukebox.client.scanner :as scanner]
            [clojure.string :as str])
  (:import (java.util UUID)
           (java.io Closeable))
  (:gen-class))

(defonce ws (atom nil))

(defn send!
  "Send a message to the jukebox coordinator."
  [message]
  (s/put! @ws (json/generate-string message)))

(defn stop
  "Stop the clojure jukebox language client."
  []
  (when @ws
    (.close ^Closeable @ws)
    (reset! ws nil)))

(defn error
  "Create an error response message."
  [message e]
  (assoc message
    :action "result"
    :error (.getMessage e)
    :trace (mapv (fn [t] {:class-name (.getClassName t)
                          :file-name (.getFileName t)
                          :line-number (.getLineNumber t)
                          :method-name (.getMethodName t)})
                 (.getStackTrace e))))

(defn handle-coordinator-message
  "Handle messages from the coordinator"
  [message]
  (try
    (log/debugf "Coordinator message: %s" message)
    (let [message (binding [parse/*use-bigdecimals?* true]
                    (json/parse-string message true))]
      (case (:action message)
        "run" (send! (assoc message
                       :action "result"
                       :board (registry/run message)))
        "stop" (stop)
        (throw (ex-info (format "Unknown action: %s" message) {}))))
    (catch Exception e
      (log/debugf "Callback threw exception: %s" message)
      (send! (error message e)))))

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
  []
  {"action" "register"
   "client-id" (str (UUID/randomUUID))
   "language" "clojure"
   "version" "1"
   "definitions" @registry/definitions
   "snippet" {"argument-joiner" " "
              "escape-pattern" ["\"" "\\\""]
              "template" template}})

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