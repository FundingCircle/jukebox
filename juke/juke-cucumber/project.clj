(defproject juke-cucumber "0.1.0-SNAPSHOT"
  :description "Cucumber backend for juke."
  :url "https://github.com/fundingcircle/blackbox"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aot [fundingcircle.juke.backend.cucumber]
  :profiles {:dev {:aliases {"cucumber" ["run" "-m" "cucumber.api.cli.Main"
                                         "--glue" "test/example"
                                         "--plugin" "json:cucumber.json"
                                         "test/features"]}}}
  :dependencies [[fundingcircle/juke "0.1.0-SNAPSHOT"]
                 [io.cucumber/cucumber-core "3.0.2"]
                 [io.cucumber/cucumber-junit "3.0.2"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.1"]]
:repositories [["clojars"
                  "https://clojars.org/repo/"]
                 ["snapshots"
                  {:url "https://fundingcircle.artifactoryonline.com/fundingcircle/libs-snapshot-local"
                   :username [:gpg :env/artifactory_user]
                   :password [:gpg :env/artifactory_password]}]
                 ["releases"
                  {:url "https://fundingcircle.artifactoryonline.com/fundingcircle/libs-release-local"
                   :username [:gpg :env/artifactory_user]
                   :password [:gpg :env/artifactory_password]}]])
