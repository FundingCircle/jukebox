(defproject fundingcircle/juke "0.1.8"
  :description "A simple library that hooks clojure into BDD frameworks such
as cucumber."
  :url "https://github.com/fundingcircle/jukebox/tree/master/juke"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/tools.namespace "0.2.11"]]
  :pedantic? :warn
  :repositories {"snapshots" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-snapshot-local"
                              :username [:gpg :env/artifactory_user]
                              :password [:gpg :env/artifactory_password]
                              :sign-releases false}

                 "releases" {:url "https://fundingcircle.jfrog.io/fundingcircle/libs-release-local"
                             :username [:gpg :env/artifactory_user]
                             :password [:gpg :env/artifactory_password]
                             :sign-releases false}})
