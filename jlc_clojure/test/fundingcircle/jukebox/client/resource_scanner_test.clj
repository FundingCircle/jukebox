(ns fundingcircle.jukebox.client.resource-scanner-test
  "Tests of the resource scanner feature."
  (:require [clojure.test :refer [deftest is]]
            [fundingcircle.jukebox.client.resource-scanner :as resource-scanner]
            [fundingcircle.jukebox.client.step-scanner :as scanner]
            [fundingcircle.jukebox.client.step-registry :as step-registry]))

(deftest inventory-test
  (scanner/load-step-definitions! ["test"])
  (let [entry-points (vals @step-registry/callbacks)]
    (is (= {'example.belly/i-have-cukes-in-my-belly
            #{{:scene/resources [:kafka/topic "topic-name"]}
              {:scene/resources [:kafka/topic "topic-name-a"]}
              {:scene/resources [:kafka/topic "topic-name-b"]}
              {:scene/resources [:kafka/topic "topic-name-c"]}}
            'example.belly/before-step
            #{{:scene/resources [:kafka/topic "topic-name-d"]}}}
           (resource-scanner/inventory entry-points [:scene/resources :resources])))))
