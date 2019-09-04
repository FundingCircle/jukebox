(ns fundingcircle.jukebox.resource-test
  "Test resource inventory library."
  (:require [clojure.test :refer [deftest is testing]]
    ;[fundingcircle.jukebox.resource :as resource]
            ))

;; These helper functions set up a call graph for the test below to cover these scenarios:
;; * The inventory should include resources on transitive functions
;; * The inventory should de-duplicate resources
;; * Recursive calls don't result in a stack overflow
;;
;; entry-point
;;   --> helper-fn-a
;;   --> helper-fn-b
;;   --> helper-fn-d
;;     --> helper-fn-c (transitive scenario)
;;   --> helper-fn-e
;;     --> helper-fn-e (recursive scenario)

(require 'example.belly) ;; be sure the instrumented fns are loaded

#_(deftest inventory-test
  (testing "resource inventory"
    (let [inventory (resource/inventory [:scene/resources]
                                        (resource/parse-tags ["test/features"]))]
      (is (= {'example.belly/i-have-cukes-in-my-belly
              #{{:scene/resources [:kafka/topic "topic-name"]}
                {:scene/resources [:kafka/topic "topic-name-a"]}
                {:scene/resources [:kafka/topic "topic-name-b"]}
                {:scene/resources [:kafka/topic "topic-name-c"]}}
              'example.belly/before-step
              #{{:scene/resources [:kafka/topic "topic-name-d"]}}}
             inventory)))))

