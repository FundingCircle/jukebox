(ns fundingcircle.jukebox.accord)

(defmulti provide
  "docstring here"
  (fn [board fixture-key step-args] fixture-key))

(defmethod provide :default
  [board fixture-key step-args]
  ::not-implemented)

