(ns fundingcircle.jukebox.client.step-scanner
  "Scan paths for clojure step definitions."
  (:require [clojure.tools.namespace.find :as find]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [fundingcircle.jukebox.client.step-registry :as step-registry]))

(defn scene-related?
  "Checks whether a var is tagged with any scene related metadata."
  [v]
  (->> (meta v)
       (keys)
       (some #{:scene/step
               :scene/steps
               :scene/before
               :scene/after
               :scene/before-step
               :scene/after-step})))

(defn- require-namespaces-in-dir
  "Scan namespaces in a dir and require them."
  [dir]
  (for [ns (find/find-namespaces-in-dir dir)]
    (do
      (require ns)
      (find-ns ns))))

(defmulti find-hooks
  "Find steps and hooks."
  (fn [source] (type source)))

(defmethod find-hooks clojure.lang.Namespace
  [ns]
  (->> ns ns-interns vals (filter scene-related?)))

(defmethod find-hooks java.io.File
  [dir]
  (mapcat find-hooks (require-namespaces-in-dir dir)))

(defmethod find-hooks java.net.URI
  [^java.net.URI uri]
  (mapcat find-hooks (require-namespaces-in-dir (io/file (.getSchemeSpecificPart uri)))))

(defmethod find-hooks String
  [dir]
  (mapcat find-hooks (require-namespaces-in-dir (io/file dir))))

(defn hooks
  "Finds steps and hooks in `ns`, or in all namespaces if `ns` is not
  specified."
  ([] (mapcat hooks (all-ns)))
  ([source]
   (log/debugf "Loading hooks from source: %s" source)
   (find-hooks source)))

(defn register-callbacks
  "Registers the given step/hook functions with the backend."
  [step-registry callback-fns]
  (reduce (fn [step-registry callback]
            (let [m        (meta callback)
                  hooks    (map name (keys (select-keys m [:scene/before :scene/after :scene/before-step :scene/after-step])))
                  triggers (into (filter identity (conj (:scene/steps m) (:scene/step m))) hooks)
                  opts     (select-keys m [:scene/tags])]
              (step-registry/add step-registry {:triggers triggers
                                                :opts opts
                                                :callback callback})))
          step-registry callback-fns))

(defn load-step-definitions
  "Scan for step definitions."
  [step-registry glue-paths]
  (log/debugf "Glue paths: %s" glue-paths)
  (if (= 0 (count glue-paths))
    (register-callbacks step-registry (hooks))
    (reduce (fn [step-registry glue-path]
              (register-callbacks step-registry (hooks glue-path)))
            step-registry glue-paths)))
