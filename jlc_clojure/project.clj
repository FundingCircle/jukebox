(defproject fundingcircle/jukebox "1.0.5-SNAPSHOT"
  :description "A clojure BDD library that integrates with cucumber."
  :url "https://github.com/fundingcircle/jukebox/"
  :license {:name "BSD 3-clause"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[aleph "0.4.7-alpha5"]
                 [camel-snake-kebab "0.4.0"]
                 [cheshire "5.8.1"]
                 [compojure "1.6.1"]
                 [io.cucumber/cucumber-core "4.7.1"]
                 [io.cucumber/cucumber-junit "4.7.1"]
                 [me.raynes/conch "0.8.0"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [venantius/yagni "0.1.7"]]
  :profiles {:dev {:aliases {"cucumber" ["run" "-m" "cucumber.api.cli.Main"
                                         "--glue" "test/example"
                                         "--plugin" "json:cucumber.json"
                                         "--plugin" "pretty"
                                         "test/features"]}
                   :source-paths ["src" "junit"]
                   :dependencies [[ch.qos.logback/logback-classic "1.2.3"]
                                  [net.mikera/cljunit "0.7.0"]]
                   :resource-paths ["test"]}
             :kaocha {:dependencies [[lambdaisland/kaocha "0.0-541"]
                                     [lambdaisland/kaocha-junit-xml "0.0-70"]]}}
  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner" "--plugin" "kaocha.plugin/junit-xml" "--junit-xml-file" "junit.xml"]}
  :aot [fundingcircle.jukebox.backend.cucumber])
