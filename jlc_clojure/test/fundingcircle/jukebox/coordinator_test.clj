(ns fundingcircle.jukebox.coordinator-test
  (:require [clojure.test :refer [deftest is testing]]
            [fundingcircle.jukebox.coordinator :as coordinator]
            [clojure.tools.logging :as log]))

(deftest start-test
  @(coordinator/restart ["test/example"])
  (is (= #{{:triggers ["before"], :opts #:scene{:tags ["@foo or @bar"]}}
           {:triggers ["the datafied table should be"], :opts #:scene{:tags nil}}
           {:triggers ["I have this table"], :opts #:scene{:tags nil}}
           {:triggers ["before"], :opts #:scene{:tags nil}}
           {:triggers ["after"], :opts #:scene{:tags ["@baz"]}}
           {:triggers ["I have {int} cukes in my belly"], :opts #:scene{:tags nil}}
           {:triggers ["today is Sunday"], :opts {}}
           {:triggers ["I ask whether it's Friday yet"], :opts {}}
           {:triggers ["I should be told {string}"], :opts {:scene/resources ["kafka/topic-h"]}}
           {:triggers ["I wait {int} hour"], :opts {}}
           {:triggers ["my belly should growl"], :opts {}}
           {:triggers ["before" "after_step"], :opts #:scene{:tags ["@tag1 or @tag2"]}}
           {:triggers ["before" "after"], :opts {}}
           {:triggers ["before"], :opts {}}
           {:triggers ["before"], :opts #:scene{:tags [""]}}
           {:triggers ["a ruby step that fails"], :opts {}}}
         (into #{}  (map #(dissoc % :id) @coordinator/definitions)))))
