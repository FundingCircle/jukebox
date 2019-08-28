(defproject fundingcircle/jukebox "1.0.5-SNAPSHOT"
  :description "A clojure BDD library that integrates with cucumber."
  :url "https://github.com/fundingcircle/jukebox/"
  :license {:name "BSD 3-clause"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[aleph "0.4.7-alpha5"]
                 [cheshire "5.8.1"]
                 [compojure "1.6.1"]
                 [io.cucumber/cucumber-core "4.2.2"]
                 [io.cucumber/cucumber-junit "4.2.2"]
                 [me.raynes/conch "0.8.0"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [venantius/yagni "0.1.7"]]
  :resource-paths ["../jlc_ruby/jlc_ruby.jar"]
  :profiles {:dev {:aliases {"cucumber" ["run" "-m" "cucumber.api.cli.Main"
                                         "--glue" "test/example"
                                         "--plugin" "json:cucumber.json"
                                         "--plugin" "pretty"
                                         "test/features"]}
                   :dependencies [[ch.qos.logback/logback-classic "1.2.3"]]
                   :resource-paths ["../jlc_ruby/jlc_ruby.jar" "test/test.jar"]}}
  :aot [fundingcircle.jukebox.backend.cucumber])
