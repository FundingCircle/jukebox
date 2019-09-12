(ns glue-paths.jukebox.step-definitions.is-it-friday-yet
  (:require [fundingcircle.jukebox :refer [step]]))

(defn is-it-friday?
  [day]
  (= day "Friday"))

(step "today is Sunday"
  [board]
  (assoc board :today "Sunday"))

(step "I ask whether it's Friday yet"
  [board]
  (assoc board :actual-answer (is-it-friday? (:today board))))

(step " I should be told {string}"
  [board expected-answer]
  (assert (= (:actual-answer board) expected-answer)))
