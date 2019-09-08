(ns acceptance
  (:require [clojure.test :refer [deftest is]])
  (:import (cucumber.api.cli Main)))

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

(deftest cukes
  (is (thrown?  RuntimeException (run-scenarios "@failure-in-ruby")))
  (is (thrown? RuntimeException (run-scenarios "@failure-in-clojure"))))
