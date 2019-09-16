(ns fundingcircle.jukebox.msg
  "Coordinator/client messages."
  (:refer-clojure :exclude [send])
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [msgpack.core :as msg]
            [msgpack.macros :refer [extend-msgpack]])
  (:import (java.util UUID)))

(defn- keyword->str
  "Convert keyword to string with namespace preserved.
  Example: :A/A => \"A/A\""
  [k]
  (subs (str k) 1))

(extend-msgpack
  clojure.lang.Keyword
  0
  [k] (msg/pack (keyword->str k))
  [bytes] (some-> bytes
                  (msg/unpack)
                  (str/replace #"_" "-")
                  (keyword)))

(extend-msgpack
  clojure.lang.IPersistentSet
  1
  [s] (msg/pack (seq s))
  [bytes] (set (msg/unpack bytes)))

(extend-msgpack
  UUID
  2
  [uuid] (msg/pack (str uuid))
  [bytes] (UUID/fromString (msg/unpack bytes)))

(defonce local-board (atom {}))

(defn- transmittable->
  "Merge with local board"
  [m]
  (into {}
        (reduce (fn [m [ks v]] (assoc-in m ks v))
                m
                @local-board)))

(defn- ->transmittable
  "Removes non-transmittable entries from the map, stashing non-serializable values in `local-board`."
  ([m] (->transmittable m []))
  ([m ks]
   (let [local-board (volatile! nil)
         board       (->transmittable m ks local-board)]
     {:board board :local-board @local-board}))
  ([m ks local-board]
   (into {}
         (for [[k v] m]
           (cond
             (map? v)
             [k (->transmittable v (conj ks k) local-board)]
             :else
             (try
               (msg/pack v)
               [k v]
               (catch Throwable _
                 (log/warnf "Note: Board entry can't be transmitted across languages: %s" [(conj ks k) v])
                 (vswap! local-board assoc (conj ks k) v)
                 nil)))))))

(defn send
  "Sends a message to a client."
  [{:keys [out]} message]
  (let [transmittable (->transmittable message)]
    (reset! local-board (:local-board transmittable))
    (msg/pack-stream (:board transmittable) out)))

(defn recv
  "Gets the next message from the client."
  [{:keys [in]}]
  (transmittable-> (msg/unpack-stream in)))

(defn messages
  "Lazy sequence of messages from the client."
  [client]
  (lazy-seq (cons (recv client) (messages client))))
