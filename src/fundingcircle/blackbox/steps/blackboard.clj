(ns fundingcircle.blackbox.steps.blackboard
  "Manage set up and tear down of the 'blackboard' (context) object
  for tests."
  (:require [clojure.tools.logging :as log]
            [fundingcircle.blackbox.blackboard :as blackboard]))

(defn ^:scene/before setup
  "Called when a scenario starts to initialize the 'blackboard'."
  [ctx scenario]
  (log/info "Initializing blackboard for scenario")
  (merge ctx (blackboard/create)))

(defn ^:scene/after teardown
  "Called when a scenario finishes to tear down the 'blackboard'."
  [blackboard scenario]
  (log/info "Tearing down blackboard for scenario"))
