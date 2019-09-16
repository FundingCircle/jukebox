(ns fundingcircle.jukebox.client.step-registry-test
  "Tests of the step registry."
  (:require [clojure.test :refer [deftest is testing]]
            [fundingcircle.jukebox.client.step-registry :as step-registry])
  (:import (java.util UUID)))

(deftest registry-test
  (let [trigger      (str (UUID/randomUUID))
        test-callback (fn test-callback [board arg1] (assoc board :arg1 arg1))
        step-registry (-> (step-registry/create)
                          (step-registry/add {:triggers [trigger]
                                              :opts {:scene/tags "@foo"}
                                              :callback test-callback}))
        definition (step-registry/find-definition step-registry trigger)
        callback   (get (step-registry/callbacks step-registry) (:id definition))]
    (testing "it saves the step definition"
      (is definition)
      (is (= {:scene/tags ["@foo"]} (:opts definition)))
      (is (uuid? (UUID/fromString (:id definition))))
      (is (= [trigger] (:triggers definition))))

    (testing "it registers the callback"
      (is callback)
      (is (= test-callback callback)))

    (testing "it runs the step definition"
      (is (= {:a 1 :arg1 2}
             (step-registry/run step-registry {:id (:id definition)
                                               :board {:a 1}
                                               :args [2]}))))))
