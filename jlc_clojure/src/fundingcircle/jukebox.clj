(ns fundingcircle.jukebox
  "Defines the Jukebox DSL."
  (:require [fundingcircle.jukebox.client.step-registry :as registry]))

(def ^:private trigger?
  ""
  (some-fn keyword? string?))

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
      board)
  "
  {:style/indent 1}
  [& triggers-opts-args-body]
  (let [triggers  (take-while trigger? triggers-opts-args-body)
        opts_body (drop-while trigger? triggers-opts-args-body)
        opts      (first (take-while map? opts_body))
        args_body (drop-while map? opts_body)
        args      (first (take-while vector? args_body))
        body      (drop-while vector? args_body)]
    `(registry/add {:triggers ~(vec triggers)
                    :opts {:scene/tags ~(:tags opts)}
                    :callback (fn ~args ~@body)})))
