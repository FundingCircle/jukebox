(ns example.belly)

(defn i-have-cukes-in-my-belly
  {:scene/step "I have {int} cukes in my belly"}
  [board number-of-cukes]
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
  {:scene/step "the datafied table should be (.*)"}
  [board datafied-table]
  (assert (= (:table board)
             (read-string datafied-table)))
  board)
