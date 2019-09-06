# Jukebox - Polyglot Update

This update makes jukebox work with steps defined in multiple languages. Each step can be defined in a supported language (currently clojure or ruby). When a scenario is run, each step will be run in whichever language it's defined with. 

## Syntax
Here are some example step definition snippets:

    ჻ clojure -A:jukebox/snippets test/features
    UUU
    Undefined scenarios:
    test/features/belly.feature:3 # a few cukes
    
    1 Scenarios (1 undefined)
    3 Steps (3 undefined)
    0m3.145s
    
    
    You can implement missing steps with the snippets below:
    
      ```clojure
      (defn i-have-cukes-in-my-belly
        "Returns an updated context (`board`)."
        {:scene/step "I have {int} cukes in my belly"}
        [board, int1]
        ;; Write code here that turns the phrase above into concrete actions
        (throw (cucumber.api.PendingException.))
        board) ;; Return the board
      ```
    
      ```ruby
      require jukebox
      module MyTests
        extend Jukebox
        
        step 'I have {int} cukes in my belly' do |board, int1|
          pending! # Write code here that turns the phrase above into concrete actions
          board # return the updated board
        end
      end
      ```

## How it works (architecture)
1. When jukebox starts up, it starts a coordinator thread. The coordinator will launch jukebox language clients, one for each language. The coordinator sits between cucumber (which will parse feature files written in gherkin) and the language clients, dispatching requests (from cucumber) to run a step to the appropriate language client. 
2. In the default configuration, the coordinator will determine what languages are active. Currently, clojure is detected by the presence of a `deps.edn` or `project.clj` file in the current directory. Ruby is detected with the presense of a `Gemfile`. This can also be defined explicitly in a `.jukebox` file (see below).
3. A `jukebox language client` is launched for each language. In the default configuration, the clojure client is launched in memory. The ruby client is launched by running `bundle exec jcl_ruby`. This can explicitly configured in a `.jukebox` file.
4. In the current iteration, communication between the language clients and coordinator happens via a JSON-formatted protocol over a websocket connection. When the coordinator starts up, it creates a websocket server on a random port. When it launches each language client, the port is provided.
5. At launch, a language client will scan the feature paths for step definitions, creating an internal registry. It will then connect to the jukebox coordinator via a websocket client, and provide it's step definition inventory to the coordinator.
6. As the features are executed by cucumber, the coordinator will look up which client knows how to handle a step, and dispatch the request to the right client.

## Configuration
A jukebox configuration can be defined explicitly if needed in a project by creating a `.jukebox` file with the following (JSON):
    ```json
    {"languages": ["ruby", "clojure"]}
    ``` 

In addition, the launcher details can be configured if needed by adding the "language-clients" configuration. These are the defaults:
    ```json
    {"languages": ["ruby", "clojure"]
     "language-clients": [{:language "clojure" :launcher "jlc-clj-embedded"}
                          {:language "ruby" :launcher "jlc-cli" :cmd ["bundle" "exec" "jlc_ruby"]}]}
    ```

## Ruby Details
### Defining Step Definitions & Hooks
Step definitions can be defined by requiring 'jukebox' and using `step`:

      require jukebox
      
      module MyTests
        extend Jukebox # Mixin `Jukebox.step` so it can be used as `step`
        
        step 'I have {int} cukes in my belly' do |board, int1|
          pending! # Write code here that turns the phrase above into concrete actions
          board # return the updated board
        end
        
        step :before do |board scenario|
          pending! # Write code here that runs before each scenario
          board # return the updated board
        end
        
        step :before {:tags "@user and @admin"} do |board|
            pending! # Write code here that will run before each scenario that matches the tag expression
            board # return the updated board
        end
        
        step :after do |board scenario|
          pending! # Write code here that runs after each scenario
          board # return the updated board
        end

        step :after_step do |board scenario|
          pending! # Write code here that runs before each step
          board # return the updated board
        end
        
        step :before_step do |board scenario|
          pending! # Write code here that runs after each step
          board # return the updated board
        end
      end

### Cucumber Compatibility
If a step is defined in a cucumber style (`When`, `Then`, etc), then the Ruby jukebox language client will switch to Cucumber compatibility mode. This mode replicates / requires the code to be laid out in the cucumber conventions. In compatibility mode, the `board` is not provided to the step definition, unless the arity supports it.

## Clojure Details
### Defining steps with metadata tags
Functions can be tagged as step definitions using function meta:

    ```clojure
    (defn i-have-cukes-in-my-belly
      "Returns an updated context (`board`)."
      {:scene/step "I have {int} cukes in my belly"}
      [board, int1]
      ;; Write code here that turns the phrase above into concrete actions
      (throw (cucumber.api.PendingException.))
      board) ;; Return the board
    ```
      
Functions can be tagged as hooks with the metadata keys: `:step/before`, `:step/after`, `:step/before-step`, or `:step/after-step`:
    ```clojure
    (defn ^:scene/before webdriver-initialize
      "Initialize a webdriver."
      [board scenario]
      (assoc board :web-driver (web/driver)))
    ```
    
### Defining steps with the `step` macro
Steps can now alternatively be defined with the `step` macro that works like the Ruby version:

    ```clojure
    (ns example.belly
      (:require [fundingcircle.jukebox :refer [step]]))
      
    (step "I have {int} cukes in my belly"
      [board int1]
      board) ;; return the updated board
     
    (step :before ;; Run before every scenario
      [board scenario]
      board)
       
    (step :before-step {:tags "@user and @admin"} ;; Run before the scenarios with the matching tags
      [board scenario]
      board)
       
    (step :after ;; Run after each scenario
      [board scenario]
      board)
       
    (step :after-step ;; Run after each step
      [board scenario]
      board)       
    ```
