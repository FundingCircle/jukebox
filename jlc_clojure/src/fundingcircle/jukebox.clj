(ns fundingcircle.jukebox
  "Defines the Jukebox DSL."
  (:require [fundingcircle.jukebox.step-registry :as step-registry]
            [clojure.string :as str])
  (:import (java.util UUID)
           (cucumber.api PendingException)))

(def ^:private trigger?
  "Checks whether a value is a string or keyword."
  (some-fn keyword? string?))

(defn- step-fn-name
  "Returns a name for a step."
  [trigger]
  (symbol
    (if (keyword? trigger)
      (str (name trigger) "-" (UUID/randomUUID))
      (str/lower-case
        (str/join "-" (remove str/blank? (str/split trigger #"\W")))))))

(defmacro step
  "Defines a step.

  Examples:
    ;; Define a step
    (step \"I have {int} cukes in my belly\"
      [board number-of-cukes]
      board)

    ;; Run before scenarios with tags:
    (step :before {:tags \"@foo or @bar\"}
      [board scenario]
      board)"
  {:style/indent 1}
  [& triggers-opts-args-body]
  (let [triggers  (take-while trigger? triggers-opts-args-body)
        opts_body (drop-while trigger? triggers-opts-args-body)
        opts      (first (take-while map? opts_body))
        args_body (drop-while map? opts_body)
        args      (first (take-while vector? args_body))
        body      (drop-while vector? args_body)]
    `(do
       ~@(for [trigger triggers]
           `(defn ~(step-fn-name trigger)
              ~(cond-> (assoc opts :scene/step trigger) (:tags opts)
                       (assoc :scene/tags (:tags opts)))
              ~args
              ~@body)))))

(defn pending!
  "Marks a step definition as pending."
  []
  (throw (PendingException.)))
