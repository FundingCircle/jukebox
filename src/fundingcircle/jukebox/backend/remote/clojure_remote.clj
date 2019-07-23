(ns fundingcircle.jukebox.backend.remote.clojure-remote
  "Client for clojure steps."
  (:require [aleph.http :as http]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [manifold.stream :as s]))

(defonce sock (atom nil))

(defn start
  "Start the clojure step language client."
  []
  (when-not @sock
    (println "Connecting")
    (reset! sock @(http/websocket-client "ws://localhost:9453/jukebox"))
    (println "Connected"))) ;=> ("0" "1" "2" "3" "4" "5" "6" "7" "8" "9")

(defn stop
  "Stop step language client."
  []
  (when @sock
    (.close @sock)
    (alter-var-root #'sock (constantly nil))))

(comment
  (client)
  sock
  (def r (s/take! @sock))
  @r

  (s/put! @sock "11")

  )
