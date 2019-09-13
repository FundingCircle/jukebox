(ns fundingcircle.jukebox.client-test
  "Clojure jukebox language client tests."
  (:require [clojure.test :refer [deftest is testing]]
            [fundingcircle.jukebox.client :as client]
            [fundingcircle.jukebox.client.step-scanner :as step-scanner]
            [fundingcircle.jukebox.client.step-registry :as step-registry])
  (:import (java.util UUID)))

(require 'example.belly)
(require 'glue-paths.jukebox.step-definitions.is-it-friday-yet)

(deftest client-info-test
  (testing "the clojure client info should include step definitions and a template snippet"
    (let [step-registry (-> (step-registry/create)
                            (step-scanner/load-step-definitions ["test/glue-paths/jukebox"]))
          client-id     (str (UUID/randomUUID))]
      (is (= (client/client-info step-registry client-id)
             {"action" "register"
              "client-id" client-id
              "definitions" []
              "language" "clojure"
              "resources"
              #{"foo"
                :kafka/topic-a
                :kafka/topic-b
                :kafka/topic-c
                :kafka/topic-d
                :kafka/topic-e
                :kafka/topic-f}
              "snippet" {"argument-joiner" " "
                         "escape-pattern" ["\""
                                           "\\\""]
                         "template" (str
                                      "  (defn {2}\n"
                                      "    \"Returns an updated context (`board`).\"\n"
                                      "    '{':scene/step \"{1}\"'}'\n"
                                      "    [{3}]\n"
                                      "    ;; {4}\n"
                                      "    (throw (cucumber.api.PendingException.))\n"
                                      "    board) ;; Return the board\n")}})))))

(deftest run-step
  (testing "when a step fails, an error message payload is created"
    (let [
          trigger       (str (UUID/randomUUID))
          test-callback (fn [_board _arg1] (assert false))
          step-registry (-> (step-registry/create)
                            (step-scanner/load-step-definitions ["test/glue-paths/jukebox"]) ;; TODO
                            (step-registry/add {:triggers [trigger] :tags "@foo" :callback test-callback})
                            )
          ]

      (let [definition (step-registry/find-trigger step-registry trigger)
            result     (client/run step-registry {:id (:id definition)
                                                  :board {:a 1 :arg1 2}
                                                  :args [2]})]
        (is (= (dissoc result :trace)
               {:action "error"
                :args [2]
                :board {:a 1 :arg1 2}
                :error "Assert failed: false"
                :id (:id definition)}))))))
