(ns fundingcircle.jukebox.client.resource-scanner-test
  "Tests of the resource scanner feature."
  (:require [clojure.test :refer [deftest is]]
            [fundingcircle.jukebox.client.resource-scanner :as resource-scanner]
            [fundingcircle.jukebox.client.step-scanner :as scanner]
            [fundingcircle.jukebox.client.step-registry :as step-registry]))

(deftest inventory-test
  (scanner/load-step-definitions! ["test"])
  (is (= #{"foo"
           :kafka/topic-a
           :kafka/topic-b
           :kafka/topic-c
           :kafka/topic-d
           :kafka/topic-e
           :kafka/topic-f}
         (resource-scanner/inventory [#"example\.belly"]))))
