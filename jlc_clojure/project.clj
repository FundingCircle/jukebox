(defproject fundingcircle/jukebox "1.0.5-SNAPSHOT"
  :description "A clojure BDD library that integrates with cucumber."
  :url "https://github.com/fundingcircle/jukebox/"
  :license {:name "BSD 3-clause"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[cheshire "5.8.1"]
                 [clojure-msgpack "1.2.1"]
                 [io.cucumber/cucumber-core "4.7.1"]
                 [io.cucumber/cucumber-junit "4.7.1"]
                 [me.raynes/conch "0.8.0"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.4.500"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [venantius/yagni "0.1.7"]]
  :profiles {:dev {:aliases {"cucumber" ["run" "-m" "cucumber.api.cli.Main"
                                         "--glue" "test/example"
                                         "--plugin" "json:cucumber.json"
                                         "--plugin" "pretty"
                                         "test/features"]
                             "inventory" ["run" "-m" "fundingcircle.jukebox.alias.inventory" "--glue" "test/example"]}
                   :source-paths ["src" "junit"]
                   :dependencies [[ch.qos.logback/logback-classic "1.2.3"]
                                  [net.mikera/cljunit "0.7.0"]]
                   :resource-paths ["test"]}}
  :aot [fundingcircle.jukebox.backend.cucumber])
