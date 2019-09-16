(ns fundingcircle.jukebox.client.resource-scanner-test
  "Tests of the resource scanner feature."
  (:require [clojure.test :refer [deftest is]]
            [fundingcircle.jukebox.client.resource-scanner :as resource-scanner]
            [fundingcircle.jukebox.client.step-scanner :as step-scanner]
            [fundingcircle.jukebox.client.step-registry :as step-registry]))

(require 'example.belly)
(require 'glue-paths.jukebox.step-definitions.is-it-friday-yet)

(deftest inventory-test
  (let [step-registry (-> (step-registry/create)
                          (step-scanner/scan ["test"]))]
    (is (= #{:kafka/topic-a
             :kafka/topic-b
             :kafka/topic-c
             :kafka/topic-d
             :kafka/topic-e
             :kafka/topic-f}
           (resource-scanner/inventory step-registry
                                       [#"example\.belly.*"])))))
