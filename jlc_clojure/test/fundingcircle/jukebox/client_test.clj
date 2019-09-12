(ns fundingcircle.jukebox.client-test
  "Clojure jukebox language client tests."
  (:require [clojure.test :refer [deftest is testing]]
            [fundingcircle.jukebox.client :as client]
            [fundingcircle.jukebox.client.step-scanner :as step-scanner]
            [fundingcircle.jukebox.client.step-registry :as step-registry])
  (:import (java.util UUID)))

(deftest client-info-test
  (testing "the clojure client info should include step definitions and a template snippet"
    (step-registry/clear)
    (step-scanner/load-step-definitions! ["test/glue-paths/jukebox"])
    (let [client-id (str (UUID/randomUUID))]
      (is (= (client/client-info client-id)
             {"action" "register"
              "client-id" client-id
              "definitions" []
              "language" "clojure"
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
                                      "    board) ;; Return the board\n")}
              "version" "1"})))))

(deftest run-step
  (testing "when a step fails, an error message payload is created"
    (let [trigger       (str (UUID/randomUUID))
          test-callback (fn [board arg1] (assert false))]
      (step-registry/clear)
      (step-registry/add {:triggers [trigger] :tags "@foo" :callback test-callback})

      (let [definition (step-registry/find-trigger trigger)
            result     (client/run {:id (:id definition)
                                    :board {:a 1 :arg1 2}
                                    :args [2]})]
        (is (= (dissoc result :trace)
               {:action "error"
                :args [2]
                :board {:a 1 :arg1 2}
                :error "Assert failed: false"
                :id (:id definition)}))))))
