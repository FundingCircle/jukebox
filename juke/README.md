# juke - Clojure BDD Testing Library

This is a simple library that hooks clojure into BDD frameworks such
as cucumber.

Here's an example feature.
```
Feature: Belly

  Scenario: a few cukes
    Given I have 42 cukes in my belly
    When I wait 2 hours
    Then my belly should growl
```

Clojure functions can be mapped to each step by tagging it with `:scene/step`:
```clojure
(defn i-have-cukes-in-my-belly
  "Returns an updated `ctx`."
  {:scene/step "I have {int} cukes in my belly"}
  [ctx cukes]
  ;; Write code here that turns the phrase above into concrete actions
  (throw (cucumber.api.PendingException.)))

(defn i-wait-hours
  "Returns an updated `ctx`."
  {:scene/step "I wait {int} hours"}
  [ctx hours]
  ;; Write code here that turns the phrase above into concrete actions
  (throw (cucumber.api.PendingException.)))

(defn my-belly-should-growl
  "Returns an updated `ctx`."
  {:scene/step "my belly should growl"}
  [ctx]
  ;; Write code here that turns the phrase above into concrete actions
  (throw (cucumber.api.PendingException.)))
```

Functions with multiple arities can also be tagged. (Clojure allows metadata to be placed after the function body. This example uses that style.)
```clojure
(defn i-wait-hours
  "Returns an updated `ctx`."
  ([ctx]
   ;; Write code here that turns the phrase above into concrete actions
   (throw (cucumber.api.PendingException.)))
  ([ctx hours]
   ;; Write code here that turns the phrase above into concrete actions
   (throw (cucumber.api.PendingException.)))

  {:scene/steps ["It felt like forever"
                 "I wait {int} hours"]})
```

Functions can be registered to run before or after a scenario by
tagging them with `:scene/before` or `:scene/after` (or both).
A list of tags can also be provided.
```clojure
(defn ^:scene/before setup
  "Initializes systems under test."
  {:scene/tags ["tag-a" "tag-b"]}
  [ctx])

(defn ^:scene/after teardown
  "Tears down the test system."
  [ctx])
```
