(ns fundingcircle.jukebox.resource-test
  "Test resource inventory libary."
  (:require [fundingcircle.jukebox.resource :as resource]
            [clojure.test :refer [deftest is]]))

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

(defn- helper-fn-a
  "A test helper fn."
  {:scene/resource-type :kafka-topic
   :scene/resource-caps {:topic-name "topic-name-a"}}
  [])

(defn- helper-fn-b
  "A test helper fn."
  {:scene/resource-type :kafka-topic
   :scene/resource-caps {:topic-name "topic-name-b"}}
  [])

(defn- helper-fn-c
  "A test helper fn."
  {:scene/resource-type :kafka-topic
   :scene/resource-caps {:topic-name "topic-name-c"}}
  [])

(defn- helper-fn-d
  "A test helper fn."
  {:scene/resource-type :kafka-topic
   :scene/resource-caps {:topic-name "topic-name-a"}}
  []
  (helper-fn-c))

(defn- helper-fn-e
  "A test helper fn."
  []
  (helper-fn-e))

(defn entry-point
  "A test entry point."
  {:scene/step "test entry point"
   :scene/resource-type :kafka-topic
   :scene/resource-caps {:topic-name "topic-name"}}
  [_]
  (helper-fn-a)
  (helper-fn-b)
  (helper-fn-d)
  (helper-fn-e))

(deftest inventory-test
  (is (= #{{:scene/resource-type :kafka-topic
            :scene/resource-caps {:topic-name "topic-name"}}
           {:scene/resource-type :kafka-topic
            :scene/resource-caps {:topic-name "topic-name-c"}}
           {:scene/resource-type :kafka-topic
            :scene/resource-caps {:topic-name "topic-name-a"}}
           {:scene/resource-type :kafka-topic
            :scene/resource-caps {:topic-name "topic-name-b"}}}
       (`entry-point
        (resource/inventory ["test"])))))
