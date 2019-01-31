 (ns fundingcircle.jukebox
   "Integrates instrumented clojure functions with jukebox backends.

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
     \"Returns an updated `board`.\"
     {:scene/step \"I have {int} cukes in my belly\"}
     [board cukes]
     ;; Write code here that turns the phrase above into concrete actions
     (throw (cucumber.api.PendingException.)))

   (defn i-wait-hours
     \"Returns an updated `board`.\"
     {:scene/step \"I wait {int} hours\"}
     [board hours]
     ;; Write code here that turns the phrase above into concrete actions
     (throw (cucumber.api.PendingException.)))

   (defn my-belly-should-growl
     \"Returns an updated `board`.\"
     {:scene/step \"my belly should growl\"}
     [board]
     ;; Write code here that turns the phrase above into concrete actions
     (throw (cucumber.api.PendingException.)))
   ```

   Functions with multiple arities can also be tagged. (Clojure allows
   metadata to be placed after the function body. This example uses
   that style.)
   ```
   (defn i-wait-hours
     \"Returns an updated `board`.\"
     ([board]
      ;; Write code here that turns the phrase above into concrete actions
      (throw (cucumber.api.PendingException.)))
     ([board hours]
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
     [board scenario])

   (defn ^:scene/after teardown
     \"Tears down the test system.\"
     [board scenario])
   ```

   A function can be registered to be run before or after each step by
   tagging it with `:scene/before-step` or `:scene/after-step`:
   ```clojure
   (defn ^:scene/before-step before-step
     \"Runs before each scenario step.\"
     [board])

   (defn ^:scene/after-step after-step
     \"Runs after each scenario step.\"
     [board])
    ```"
   (:require [clojure.java.io :as io]
             [clojure.tools.logging :as log]
             [clojure.tools.namespace.find :as find]))

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

(defprotocol JukeBackend
  "Implement this to integrate with a bdd backend like cucumber, ."
  (register-step
    [_ pattern step-fn]
    "Called to register a step function.")

  (register-before-scene-hook
    [_ tags hook-fn]
    "Called to register a :scene/before hook.")

  (register-after-scene-hook
    [_ tags hook-fn]
    "Called to register a :scene/after hook.")

  (register-before-step-hook
    [_ hook-fn]
    "Called to register a :scene/before-step hook.")

  (register-after-step-hook
    [_ hook-fn]
    "Called to register a :scene/after-step hook."))

(defn register
  "Registers the given step/hook functions with the backend."
  [backend hook-fns]
  (doseq [hook-fn hook-fns]
    (let [m (meta hook-fn)]
      (when (:scene/step m)
        (log/debugf "Registering step: %s" hook-fn)
        (register-step backend (:scene/step m) hook-fn))

      (when (:scene/steps m)
        (doseq [step (:scene/steps m)]
          (log/debugf "Registering step: %s" hook-fn)
          (register-step backend (:scene/step m) hook-fn)))

      (when (:scene/before m)
        (log/debugf "Registering :scene/before hook: %s" hook-fn)
        (register-before-scene-hook backend (:scene/tags m) hook-fn))

      (when (:scene/after m)
        (log/debugf "Registering :scene/after hook: %s" hook-fn)
        (register-after-scene-hook backend (:scene/tags m) hook-fn))

      (when (:scene/before-step m)
        (log/debugf "Registering :scene/before-step hook: %s", hook-fn)
        (register-before-step-hook backend hook-fn))

      (when (:scene/after-step m)
        (log/debugf "Registering :scene/after-step hook: %s", hook-fn)
        (register-after-step-hook backend hook-fn)))))
