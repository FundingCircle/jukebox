(defproject fundingcircle/jukebox "1.0.4"
  :description "A clojure BDD library that integrates with cucumber."
  :url "https://github.com/fundingcircle/jukebox/"
  :license {:name "BSD 3-clause"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[io.cucumber/cucumber-core "4.2.2"]
                 [io.cucumber/cucumber-junit "4.2.2"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [venantius/yagni "0.1.7"]]
  :profiles {:dev {:aliases {"cucumber" ["run" "-m" "cucumber.api.cli.Main"
                                         "--glue" "test/example"
                                         "--plugin" "json:cucumber.json"
                                         "--plugin" "pretty"
                                         "test/features"]}
                   :dependencies [[ch.qos.logback/logback-classic "1.2.3"]]}}
  :aot [fundingcircle.jukebox.backend.cucumber])
