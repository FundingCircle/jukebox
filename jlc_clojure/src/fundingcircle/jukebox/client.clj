(ns fundingcircle.jukebox.client
  "A jukebox language client for clojure."
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [fundingcircle.jukebox.client.resource-scanner :as resource-scanner]
            [fundingcircle.jukebox.step-registry :as step-registry]
            [fundingcircle.jukebox.client.step-scanner :as step-scanner]
            [fundingcircle.jukebox.msg :as msg]
            [clojure.tools.logging :as log])
  (:import (java.util UUID)
           (java.net Socket)
           (java.io DataOutputStream DataInputStream))
  (:gen-class))

(defn error
  "Returns an error response message."
  [message e]
  (assoc message
    :action :error
    :error (.getMessage e)
    :trace (mapv (fn [t] {:class-name (.getClassName t)
                          :file-name (.getFileName t)
                          :line-number (.getLineNumber t)
                          :method-name (.getMethodName t)})
                 (.getStackTrace e))))

(defn run
  "Runs a step or hook, returning a result or error response."
  [step-registry message]
  (try
    (assoc message
      :action :result
      :board (step-registry/run step-registry message))
    (catch Throwable e
      (error message e))))

(def ^:private template
  (str
    ";; Clojure:\n"
    "(step \"{1}\"\n"
    "  [{3}]\n"
    "  (pending!) ;; {4}\n"
    "  board) ;; Return the updated board\n"))

(defn create
  "Creates a clojure jukebox language client."
  ([glue-paths] (create glue-paths (str (UUID/randomUUID))))
  ([glue-paths client-id]
   (let [step-registry (-> (step-registry/create)
                           (step-scanner/scan glue-paths))]
     {:client-id client-id
      :step-registry step-registry})))

(defn client-info
  "Client details for this jukebox client."
  [{:keys [step-registry client-id]}]
  {:action :register
   :client-id client-id
   :language "clojure"
   :definitions (step-registry/definitions step-registry)
   :resources (resource-scanner/inventory step-registry)
   :snippet {:argument-joiner " "
             :escape-pattern ["\"" "\\\""]
             :template template}})

(defn connect
  "Connects to the jukebox coordinator and registers known step definitions."
  [client port]
  ;(println "xClojure: Connecting to" port)
  (let [socket (Socket. "localhost" port)
        in     (DataInputStream. (.getInputStream socket))
        out    (DataOutputStream. (.getOutputStream socket))
        client (assoc client
                 :socket socket
                 :in in
                 :out out)]
    (msg/send client (client-info client))
    client))

(defn handle-coordinator-messages
  "Handles messages from the coordinator"
  [{:keys [step-registry] :as client}]
  (try
    (doseq [message (msg/messages client)]
      (try
        (case (:action message)
          :run (msg/send client (run step-registry message))
          (throw (ex-info (format "Unknown action: %s" message) {})))
        (catch Throwable e (error message e))))
    (catch java.io.EOFException _
      (log/debug "Connection closed."))))

(defn start
  "Start this jukebox language client."
  [_client-config port glue-paths]
  (handle-coordinator-messages (connect (create glue-paths) port)))

