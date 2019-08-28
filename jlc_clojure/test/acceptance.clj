(ns acceptance
  (:require [clojure.test :refer [deftest is]])
  (:import (cucumber.api.cli Main)))

(deftest cukes
  (is (zero? (Main/run
               (into-array ["--glue" "test/example"
                            "--plugin" "json:cucumber.json"
                            "test/features"])
               (.getContextClassLoader (Thread/currentThread))))))
