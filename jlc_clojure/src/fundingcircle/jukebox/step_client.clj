(ns fundingcircle.jukebox.step-client
  "Dispatcher for jukebox language clients.")

(defmulti launch
  "Launch a language client"
  (fn [client-config port glue-paths] (:launcher client-config)))
