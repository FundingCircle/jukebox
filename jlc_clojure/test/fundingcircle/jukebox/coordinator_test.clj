(ns fundingcircle.jukebox.coordinator-test
  (:require [clojure.test :refer [deftest is testing]]
            [fundingcircle.jukebox.coordinator :as coordinator]))

(deftest start-test
  @(coordinator/restart ["test/example"])
  (is (= #{{:triggers ["my belly should growl"], :opts {}}
           {:triggers [:before :after-step], :opts #:scene{:tags ["@tag1 or @tag2 and @rb"]}}
           {:triggers [:after], :opts #:scene{:tags ["@qux"]}}
           {:triggers ["a ruby step that fails"], :opts {}}
           {:triggers ["today is Sunday"], :opts {}}
           {:triggers ["I wait {int} hour"], :opts {}}
           {:triggers [:before :after], :opts {}}
           {:triggers [:before], :opts #:scene{:tags ["@foo or @bar and @clj"]}}
           {:triggers ["the datafied table should be"], :opts #:scene{:tags nil}}
           {:triggers ["I have {int} cukes in my belly"], :opts #:scene{:tags nil}}
           {:triggers [:before], :opts #:scene{:tags [""]}}
           {:triggers ["I should be told {string}"], :opts #:scene{:resources ["kafka/topic-h"]}}
           {:triggers ["I ask whether it's Friday yet"], :opts {}}
           {:triggers ["I have this table"], :opts #:scene{:tags nil}}
           {:triggers [:before], :opts {}}
           {:triggers [:before], :opts #:scene{:tags ["@bat"]}}}
         (into #{}  (map #(dissoc % :id) @coordinator/definitions)))))
