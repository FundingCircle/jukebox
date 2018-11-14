(defproject fundingcircle/jukebox "0.1.7-SNAPSHOT"
  :description "Bundle of useful Funding Circle test tooling."
  :url "https://github.com/fundingcircle/jukebox/tree/master/jukebox"
  :source-paths ["src"]
  :profiles {:dev {:source-paths ["src"]}}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[fundingcircle/juke "0.1.7-SNAPSHOT"]
                 [fundingcircle/juke-cucumber "0.1.7-SNAPSHOT"]
                 [avro-schemas "5.4472.0" :exclusions [log4j org.slf4j/slf4j-log4j12]]
                 [camel-snake-kebab "0.4.0"]
                 [clj-http "3.9.0"]
                 [clojure.java-time "0.3.2"]
                 [com.amazonaws/aws-java-sdk-athena "1.11.155"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.155"]
                 [com.cemerick/url "0.1.1"]
                 [com.jcabi/jcabi-log "0.18"]
                 [com.taoensso/encore "2.96.0"]
                 [fundingcircle/topic.metadata "0.294.0" :exclusions [environ]]
                 [fundingcircle/jackdaw-serdes "0.3.20" :exclusions [
                                                                     io.confluent/kafka-avro-serializer
                                                                     io.confluent/common-config
                                                                     org.apache.avro/avro
                                                                     environ
                                                                     com.fasterxml.jackson.core/jackson-databind
                                                                     com.thoughtworks.paranamer/paranamer
                                                                     org.slf4j/slf4j-api
                                                                     org.apache.commons/commons-compress
                                                                     com.fasterxml.jackson.core/jackson-core]]
                 [fundingcircle/topic.metadata "0.294.0"]
                 [io.confluent/kafka-avro-serializer "3.2.1" :exclusions [log4j org.slf4j/slf4j-log4j12]]
                 [io.forward/yaml "1.0.9"]
                 [lynxeyes/dotenv "1.0.2"]
                 [org.clojars.matthiasmargush/etaoin "0.2.8-SNAPSHOT" :exclusions [
                                                                                   com.fasterxml.jackson.dataformat/jackson-dataformat-cbor]]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.postgresql/postgresql "42.1.4"]]
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
