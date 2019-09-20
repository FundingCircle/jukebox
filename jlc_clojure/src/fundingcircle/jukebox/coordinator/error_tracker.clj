(ns fundingcircle.jukebox.coordinator.error-tracker
  (:require [fundingcircle.jukebox.coordinator.registration-tracker :as registration-tracker]))

(defonce error (atom nil))

(defn reset
  "Reset the error tracker."
  []
  (reset! error nil))

(defmacro with-error-tracking
  "Runs `body` with error tracking."
  [& body]
  `(do
     (when @error (throw @error))
     (try
       ~@body
       (catch Throwable e#
         (.printStackTrace e#)
         (reset! error e#)
         (registration-tracker/error! e#)
         (throw e#)))))

(defmacro with-future-error-tracking
  "Runs `body` in a `future` with error tracking."
  [& body]
  `(future
     (with-error-tracking
       ~@body)))
