(ns fundingcircle.juke
  "Integrates instrumented clojure functions with juke backends.

   Here's an example feature:
   ```
   Feature: Belly

     Scenario: a few cukes
        Given I have 42 cukes in my belly
        When I wait 2 hours
        Then my belly should growl
   ```

   Clojure functions can be mapped to each step by tagging it with `:scene/step`:
   ```
   (defn i-have-cukes-in-my-belly
     \"Returns an updated `ctx`.\"
     {:scene/step \"I have {int} cukes in my belly\"}
     [ctx cukes]
     ;; Write code here that turns the phrase above into concrete actions
     (throw (cucumber.api.PendingException.)))

   (defn i-wait-hours
     \"Returns an updated `ctx`.\"
     {:scene/step \"I wait {int} hours\"}
     [ctx hours]
     ;; Write code here that turns the phrase above into concrete actions
     (throw (cucumber.api.PendingException.)))

   (defn my-belly-should-growl
     \"Returns an updated `ctx`.\"
     {:scene/step \"my belly should growl\"}
     [ctx]
     ;; Write code here that turns the phrase above into concrete actions
     (throw (cucumber.api.PendingException.)))
   ```

   Functions with multiple arities can also be tagged. (Clojure allows
  metadata to be placed after the function body. This example uses
  that style.)
   ```
   (defn i-wait-hours
     \"Returns an updated `ctx`.\"
     ([ctx]
      ;; Write code here that turns the phrase above into concrete actions
      (throw (cucumber.api.PendingException.)))
     ([ctx hours]
      ;; Write code here that turns the phrase above into concrete actions
      (throw (cucumber.api.PendingException.)))

     {:scene/steps [\"It felt like forever\"
                    \"I wait {int} hours\"]})
   ```

   Functions can be registered to run before or after a scenario by
  tagging them with `:scene/before` or `:scene/after` (or both).  A
  list of tags can also be provided.
   ```
   (defn ^:scene/before setup
     \"Initializes systems under test.\"
     {:scene/tags [\"tag-a\" \"tag-b\"]}
     [ctx])

   (defn ^:scene/after teardown
     \"Tears down the test system.\"
     [ctx])
   ```"
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.find :as find]))

(defn- scene-related?
  "Checks whether a var is tagged with any scene related metadata."
  [v]
  (->> (meta v)
       (keys)
       (some #{:scene/step :scene/steps :scene/before :scene/after})))

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

(defmethod find-hooks String
  [dir]
  (mapcat find-hooks (require-namespaces-in-dir (io/file dir))))

(defn hooks
  "Finds steps and hooks in `ns`, or in all namespaces if `ns` is not
  specified."
  ([] (mapcat hooks (all-ns)))
  ([source]
   (log/infof "Loading hooks from source: %s" source)
   (find-hooks source)))

(defprotocol JukeBackend
  "Implement this to integrate with a bdd backend like cucumber, ."
  (register-step
    [_ pattern step-fn]
    "Called to register a step function.")

  (register-before-hook
    [_ tags hook-fn]
    "Called to register a before hook.")

  (register-after-hook
    [_ tags hook-fn]
    "Called to register an after hook."))

(defn register
  "Registers the given step/hook functions with the backend."
  [backend hook-fns]
  (doseq [hook-fn hook-fns]
    (let [m (meta hook-fn)]
      (when (:scene/step m)
        (log/infof "Registering step: %s" hook-fn)
        (register-step backend (:scene/step m) hook-fn))

      (when (:scene/steps m)
        (doseq [step (:scene/steps m)]
          (log/infof "Registering step: %s" hook-fn)
          (register-step backend (:scene/step m) hook-fn)))

      (when (:scene/before m)
        (log/infof "Registering before hook: %s" hook-fn)
        (register-before-hook backend (:scene/tags m) hook-fn))

      (when (:scene/after m)
        (log/infof "Registering after hook: %s" hook-fn)
        (register-after-hook backend (:scene/tags m) hook-fn)))))
