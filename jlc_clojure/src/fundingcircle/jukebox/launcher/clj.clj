(ns fundingcircle.jukebox.launcher.clj
  "Clojure-language client for jukebox.

Here's an example feature:
```
Feature: Belly

  Scenario: a few cukes
     Given I have 42 cukes in my belly
     When I wait 2 hours
     Then my belly should growl
```

Clojure functions can be mapped to each step by tagging it with `:scene/step`:
```
(defn i-have-cukes-in-my-belly
  \"Returns an updated `board`.\"
  {:scene/step \"I have {int} cukes in my belly\"}
  [board cukes]
  ;; Write code here that turns the phrase above into concrete actions
  (throw (cucumber.api.PendingException.)))

(defn i-wait-hours
  \"Returns an updated `board`.\"
  {:scene/step \"I wait {int} hours\"}
  [board hours]
  ;; Write code here that turns the phrase above into concrete actions
  (throw (cucumber.api.PendingException.)))

(defn my-belly-should-growl
  \"Returns an updated `board`.\"
  {:scene/step \"my belly should growl\"}
  [board]
  ;; Write code here that turns the phrase above into concrete actions
  (throw (cucumber.api.PendingException.)))
```

Functions with multiple arities can also be tagged. (Clojure allows
metadata to be placed after the function body. This example uses
that style.)
```
(defn i-wait-hours
  \"Returns an updated `board`.\"
  ([board]
   ;; Write code here that turns the phrase above into concrete actions
   (throw (cucumber.api.PendingException.)))
  ([board hours]
   ;; Write code here that turns the phrase above into concrete actions
   (throw (cucumber.api.PendingException.)))

  {:scene/steps [\"It felt like forever\"
                 \"I wait {int} hours\"]})
```

Functions can be registered to run before or after a scenario by
tagging them with `:scene/before` or `:scene/after` (or both).  A
list of tags can also be provided.
```
(defn ^:scene/before setup
  \"Initializes systems under test.\"
  {:scene/tags [\"tag-a\" \"tag-b\"]}
  [board scenario])

(defn ^:scene/after teardown
  \"Tears down the test system.\"
  [board scenario])
```

A function can be registered to be run before or after each step by
tagging it with `:scene/before-step` or `:scene/after-step`:
```clojure
(defn ^:scene/before-step before-step
  \"Runs before each scenario step.\"
  [board])

(defn ^:scene/after-step after-step
  \"Runs after each scenario step.\"
  [board])
 ```"
  (:require [fundingcircle.jukebox.launcher :refer [launch]]
            [fundingcircle.jukebox.client :as client]))

(defmethod launch "jlc-clj-embedded"
  [client-config port glue-paths]
  (future
    (client/start client-config port glue-paths)))
