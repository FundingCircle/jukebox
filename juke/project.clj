(defproject fundingcircle/juke "0.1.1"
  :description "A simple library that hooks clojure into BDD frameworks such
as cucumber."
  :url "https://github.com/fundingcircle/blackbox"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/tools.namespace "0.2.11"]]
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
