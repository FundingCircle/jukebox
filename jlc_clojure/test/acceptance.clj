(ns acceptance
  (:require [clojure.test :refer [deftest is]])
  (:import (io.cucumber.core.cli Main)))

(defn run-scenarios
  "Runs the tagged scenarios."
  [tags]
  (Main/run
    (into-array ["--glue" "test/example"
                 "--plugin" "json:cucumber.json"
                 "--tags" tags
                 "test/features"])
    (.getContextClassLoader (Thread/currentThread))))

(deftest cukes
  (is (zero? (run-scenarios "@success"))))

#_(deftest cukes
  (is (not (zero? (run-scenarios "@failure-in-ruby"))))
  #_(is (not (zero? (run-scenarios "@failure-in-clojure")))))
