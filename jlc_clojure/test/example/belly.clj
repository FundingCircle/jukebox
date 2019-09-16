(ns example.belly
  (:require [fundingcircle.jukebox :refer [step]]))

(defn- helper-fn-a
  "A test helper fn."
  {:scene/resources [:kafka/topic-a]}
  [])

(defn- helper-fn-b
  "A test helper fn."
  {:scene/resources [:kafka/topic-b]}
  [])

(defn- helper-fn-c
  "A test helper fn."
  {:scene/resources [:kafka/topic-c]}
  [])

(defn- helper-fn-d
  "A test helper fn."
  {:scene/resources [:kafka/topic-d]}
  []
  (helper-fn-c))

(defn- helper-fn-e
  "A test helper fn."
  []
  '(helper-fn-e))

(defn ^:scene/before before-step
  "Called before a scenario."
  {:scene/resources [:kafka/topic-d :kafka/topic-f]
   :scene/tags "@bat"}
  [board _scenario]
  board)

(defn i-have-cukes-in-my-belly
    {:scene/step "I have {int} cukes in my belly"
     :scene/resources [:kafka/topic-e]}
    [board number-of-cukes]
    (helper-fn-a)
    (helper-fn-b)
    (helper-fn-d)
    (helper-fn-e)
    board)

(step "I have this table"
  [board data-table]
  (assoc board :table data-table))

(defn ^:scene/after to-run-after-scenario
  {:scene/tags "@qux"}
  [board _scenario]
  board)

(step :before {:tags "@foo or @bar and @clj"}
  [board scenario]
  board)

(defn the-datafied-table-should-be-foo-col-bar
  {:scene/step "the datafied table should be"}
  [board datafied-table]
  (assert (= (:table board)
             (read-string datafied-table)))
  board)
