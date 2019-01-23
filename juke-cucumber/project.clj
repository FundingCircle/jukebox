(defproject fundingcircle/juke-cucumber "0.1.10"
  :description "Cucumber backend for juke."
  :url "https://github.com/fundingcircle/jukebox/tree/master/juke-cucumber"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aot [fundingcircle.juke.backend.cucumber]
  :profiles {:dev {:aliases {"cucumber" ["run" "-m" "cucumber.api.cli.Main"
                                         "--glue" "test/example"
                                         "--plugin" "json:cucumber.json"
                                         "--plugin" "pretty"
                                         "test/features"]}}}
  :dependencies [[fundingcircle/juke "0.1.10"]
                 [io.cucumber/cucumber-core "4.2.0"]
                 [io.cucumber/cucumber-junit "4.2.0"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.1"]]
  :pedantic? :warn
  :repositories {"snapshots" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-snapshot-local"
                               :username [:gpg :env/artifactory_user]
                               :password [:gpg :env/artifactory_password]
                               :sign-releases false}

                  "releases" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-release-local"
                              :username [:gpg :env/artifactory_user]
                              :password [:gpg :env/artifactory_password]
                              :sign-releases false}})