(defproject fundingcircle.end-to-end "0.1.7"
  :description "End to end jukebox tests"
  :url "https://github.com/fundingcircle/jukebox/tree/master/end-to-end"
  :aliases {"cucumber" ["run" "-m" "fundingcircle.end-to-end.cucumber"
                        "--plugin" "json:cucumber/cucumber.json"
                        "--plugin" "pretty"
                        "features"]
            "snippets" ["run" "-m" "fundingcircle.end-to-end.cucumber"
                        "--glue" ""
                        "features"]}
  :profiles {:dev {:source-paths ["src" "troubleshooting" "doc"]}
             :uberjar {:aot [fundingcircle.end-to-end.cucumber]}}
  :uberjar-name "end-to-end.jar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[fundingcircle/jukebox "0.1.7-SNAPSHOT"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.1"]]
  :pedantic? :warn
  :repositories {"confluent" {:url "https://packages.confluent.io/maven/"}
                 "snapshots" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-snapshot"
                              :username [:gpg :env/artifactory_user]
                              :password [:gpg :env/artifactory_password]
                              :sign-releases false}
                 "snapshots-local" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-snapshot-local"
                                    :username [:gpg :env/artifactory_user]
                                    :password [:gpg :env/artifactory_password]
                                    :sign-releases false}
                 "releases" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-release-local"
                             :username [:gpg :env/artifactory_user]
                             :password [:gpg :env/artifactory_password]
                             :sign-releases false}
                 "releases-local" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-release-local"
                                   :username [:gpg :env/artifactory_user]
                                   :password [:gpg :env/artifactory_password]
                                   :sign-releases false}
                 "central" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-release"
                            :username [:gpg :env/artifactory_user]
                            :password [:gpg :env/artifactory_password]
                            :sign-releases false}
                 "clojars" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-release"
                            :username [:gpg :env/artifactory_user]
                            :password [:gpg :env/artifactory_password]
                            :sign-releases false}})
