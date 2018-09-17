(defproject fundingcircle.jukebox "0.1.0"
  :description "Bundle of useful Funding Circle test tooling."
  :url "https://github.com/fundingcircle/jukebox/tree/master/jukebox"
  :source-paths ["src"]
  :profiles {:dev {:source-paths ["src"]}}
  :dependencies [[avro-schemas/avro-schemas "5.4472.0"]
                 [camel-snake-kebab "0.4.0"]
                 [clj-http "3.9.0"]
                 [clojure.java-time "0.3.2"]
                 [com.cemerick/url "0.1.1"]
                 [com.jcabi/jcabi-log "0.18"]
                 [com.taoensso/encore "2.96.0"]
                 [fundingcircle/jackdaw-client "0.3.20"]
                 [fundingcircle/jackdaw-serdes "0.3.20"]
                 [fundingcircle/juke "0.1.1"]
                 [fundingcircle/juke-cucumber "0.1.1"]
                 [fundingcircle/topic.metadata "0.294.0"]
                 [io.confluent/kafka-avro-serializer "3.2.1"]
                 [io.forward/yaml "1.0.9"]
                 [lynxeyes/dotenv "1.0.2"]
                 [org.clojars.matthiasmargush/etaoin "0.2.8-SNAPSHOT"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.1"]]
  :repositories {"confluent" {:url "https://packages.confluent.io/maven/"}
                 "snapshots" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-snapshot"
                              :username [:gpg :env/artifactory_user]
                              :password [:gpg :env/artifactory_password]
                              :sign-releases false}
                 "snapshots-local" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-snapshot-local"
                                    :username [:gpg :env/artifactory_user]
                                    :password [:gpg :env/artifactory_password]
                                    :sign-releases false}
                 "releases" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-release"
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
