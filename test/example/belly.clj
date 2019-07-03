(ns example.belly)

(defn- helper-fn-a
  "A test helper fn."
  {:scene/resources [:kafka/topic "topic-name-a"]}
  [])

(defn- helper-fn-b
  "A test helper fn."
  {:scene/resources [:kafka/topic "topic-name-b"]}
  [])

(defn- helper-fn-c
  "A test helper fn."
  {:scene/resources [:kafka/topic "topic-name-c"]}
  [])

(defn- helper-fn-d
  "A test helper fn."
  {:scene/resources [:kafka/topic "topic-name-a"]}
  []
  (helper-fn-c))

(defn- helper-fn-e
  "A test helper fn."
  []
  '(helper-fn-e))

(defn ^:scene/before before-step
  "Called before a scenario."
  {:scene/resources [:kafka/topic "topic-name-d"]}
  [board scenario]
  board)

(defn test-flavor-generator
  "What flavor of cake?"
  [board _number-of-cukes]
  (assoc board :flavor "Chocolate"))

(defn test-layers-generator
  "Generate a test order."
  [{:keys [flavor]  :as board} number-of-cukes]
  (assoc board :layers number-of-cukes))

(defn i-have-cukes-in-my-belly
  {:scene/step      "I have {int} cukes in my belly"
   :scene/resources [:kafka/topic "topic-name"]
   :accord          {:receives test-flavor-generator
                     :provides test-layers-generator}}
  [board number-of-cukes]
  (throw (ex-info "This is mocked out with :accord meta." {}))
  (helper-fn-a)
  (helper-fn-b)
  (helper-fn-d)
  (helper-fn-e)
  board)

(defn i-wait-hour
  {:scene/step "I wait {int} hour"}
  [board number-of-hours]
  board)

(defn my-belly-should-growl
  {:scene/step "my belly should growl"}
  [board]
  board)

(defn i-have-this-table
  {:scene/step "I have this table"}
  [board data-table]
  (assoc board :table data-table))

(defn the-datafied-table-should-be-foo-col-bar
  {:scene/step "the datafied table should be"}
  [board datafied-table]
  (assert (= (:table board)
             (read-string datafied-table)))
  board)
