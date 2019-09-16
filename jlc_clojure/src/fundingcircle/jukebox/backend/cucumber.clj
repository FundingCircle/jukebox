(ns fundingcircle.jukebox.backend.cucumber
  "Cucumber backend for jukebox."
  (:gen-class
    :name cucumber.runtime.JukeCucumberRuntimeBackend
    :constructors {[cucumber.runtime.io.ResourceLoader io.cucumber.stepexpression.TypeRegistry] []}
    :init init
    :implements [cucumber.runtime.Backend])
  (:require [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [fundingcircle.jukebox.coordinator :as step-coordinator]
            [clojure.string :as string])
  (:import [cucumber.runtime.snippets FunctionNameGenerator SnippetGenerator Concatenator Snippet]
           io.cucumber.cucumberexpressions.ParameterTypeRegistry
           [io.cucumber.stepexpression ExpressionArgumentMatcher StepExpressionFactory TypeRegistry]
           java.util.Locale
           (io.cucumber.datatable DataTable)
           (cucumber.runner Glue)
           (cucumber.runtime.filter TagPredicate)
           (java.lang.reflect Type)
           (cucumber.runtime StepDefinition HookDefinition)))

(def world
  "Used to track and provide state between steps."
  (atom nil))

(defn update-world
  "Checks whether the `world` object appears to have been dropped, and
  prints an error if so."
  [f]
  (fn [world & args]
    (let [new-world (apply f world args)]
      (when-not (::world? new-world)
        (throw (ex-info "The scenario step context appears to have been dropped. (Step implementations are expected to return an updated context.)" {:old-board world :new-board new-world})))
      new-world)))

(defn- location
  "Returns a string repr of the file and line number of the var."
  [v]
  (let [{:keys [file line]} (meta v)]
    (str file ":" line)))

(defn read-cuke-str
  "Using the clojure reader is often a good way to interpret literal values
   in feature files. This function makes some cucumber-specific adjustments
   to basic reader behavior. This is particularly appropriate when reading a
   table, for example: reading | \"1\" | 1 | we should interpret 1 as an int
   and \"1\" as a string."
  [string]
  (cond
    (re-matches #"^\d+" string) (edn/read-string string)
    (re-matches #"^:.*|\d+(\.\d+)" string) (edn/read-string string)
    (re-matches #"^\s*nil\s*$" string) nil
    :else (string/replace string #"\"" "")))

(defn table->rows
  "Reads a cucumber table of the form
     | key-1 | key-2 | ... | key-n |
     | val-1 | val-2 | ... | val-n |
   For example, given:
     | id | name    | created-at    |
     | 55 | \"foo\" | 1293884100000 |
     | 56 | \"bar\" | 1293884100000 |
   It evaluates to the clojure literal:
     [{:id 55, :name \"foo\", :created-at 1293884100000}
      {:id 56, :name \"bar\", :created-at 1293884100000}]"
  [data]
  (let [rows        (map seq (.asLists data))
        header-keys (map keyword (first rows))
        row->hash   (fn [row] (apply hash-map
                                     (interleave header-keys
                                                 (map read-cuke-str row))))]
    (mapv row->hash (next rows))))

(defmulti process-arg class)

(defmethod process-arg DataTable [arg] (table->rows arg))

(defmethod process-arg :default [arg] arg)

(defrecord JukeStepDefinition [id pattern step-fn]
  StepDefinition
  (matchedArguments [_ step]
    (.argumentsFrom
      (ExpressionArgumentMatcher.
        (.createExpression (StepExpressionFactory. (TypeRegistry. (Locale/getDefault)))
                           pattern)) step (make-array Type 0)))

  (getLocation [_ _detail]
    (location step-fn))

  (getParameterCount [_] nil)

  (execute [_ args]
    (swap! world assoc :scene/step pattern)
    (swap! world (update-world (fn [world]
                                 (step-coordinator/drive-step
                                   id
                                   (assoc world :scene/step pattern)
                                   (mapv process-arg args))))))

  (isDefinedAt [_ stack-trace-element]
    (let [{:keys [file line]} (meta step-fn)]
      (and (= (.getLineNumber stack-trace-element) line)
           (= (.getFileName stack-trace-element) file))))

  (getPattern [_] (str pattern))

  (isScenarioScoped [_] false))

(deftype JukeHookDefinition [tag-predicate id]
  HookDefinition
  (getLocation [_ _detail?]
    ;; TODO: location of hook
    (location id))

  (execute [_ scenario]
    (swap! world (update-world
                   (fn [world]
                     (step-coordinator/drive-step
                       id
                       world
                       [{:status (str (.getStatus scenario))
                         :failed? (.isFailed scenario)
                         :name (.getName scenario)
                         :id (.getId scenario)
                         :uri (.getUri scenario)
                         :lines (into [] (.getLines scenario))}])))))
  (matches [_ tags]
    (.apply tag-predicate tags))

  (getOrder [_] 0)

  (isScenarioScoped [_] false))

(deftype ClojureFnName []
  Concatenator
  (concatenate [_ words]
    (str/lower-case
      (str/join "-" words))))

(def snippets (atom nil))

(deftype JukeCucumberSnippet [step]
  Snippet
  ;; From the javadoc for Snippet (https://github.com/cucumber/cucumber-jvm/blob/v3.0.2/core/src/main/java/cucumber/runtime/snippets/Snippet.java):
  ;; /**
  ;; * @return a {@link java.text.MessageFormat} template used to generate a snippet. The template can access the
  ;; * following variables:
  ;; * <p/>
  ;; * <ul>
  ;; * <li>{0} : Step Keyword</li>
  ;; * <li>{1} : Value of {@link #escapePattern(String)}</li>
  ;; * <li>{2} : Function name</li>
  ;; * <li>{3} : Value of {@link #arguments(Map)}</li>
  ;; * <li>{4} : Regexp hint comment</li>
  ;; * <li>{5} : value of {@link #tableHint()} if the step has a table</li>
  ;; * </ul>
  ;; */
  (template [_]
    (reduce (fn [t {:keys [template language]}]
              (str t
                   "\n  ```" language "\n"
                   template
                   "  ```\n"))
            "" @snippets))
  (tableHint [_] nil)
  (arguments [_ args]
    (str/join ", " (cons "board" (keys args))))
  (escapePattern [_ pattern]
    (str/replace (str pattern) "\"" "\\\"")))

(defn -init
  [_resource-loader _type-registry]
  [[] nil])

(defn -loadGlue [_ ^Glue glue glue-paths]
  (let [glue-paths (mapv #(if (= java.net.URI (class %)) (.getSchemeSpecificPart %) %) glue-paths)
        setup      @(step-coordinator/restart glue-paths)]
    (reset! snippets (:snippets setup))
    (doseq [{:keys [id triggers opts]} (:definitions setup)]
      (doseq [trigger triggers]
        (try
          (case trigger
            :before (.addBeforeHook glue (->JukeHookDefinition (TagPredicate. (:tags opts)) id))
            :after (.addAfterHook glue (->JukeHookDefinition (TagPredicate. (:tags opts)) id))
            :before-step (.addBeforeStepHook glue (->JukeHookDefinition (TagPredicate. (:tags opts)) id))
            :after-step (.addAfterStepHook glue (->JukeHookDefinition (TagPredicate. (:tags opts)) id))
            (.addStepDefinition glue (->JukeStepDefinition id trigger step-coordinator/drive-step)))
          (catch cucumber.runtime.DuplicateStepDefinitionException _
            (log/errorf "Duplicate step definition: %s" {:trigger trigger :tags (:tags opts) :id id :glue glue})))))))

(defn -buildWorld [_]
  (reset! world {::world? true}))

(defn -disposeWorld [_]
  (reset! world {::world? true}))

(defn -getSnippet
  [_ step keyword _]
  (.getSnippet (SnippetGenerator. (->JukeCucumberSnippet step)
                                  (ParameterTypeRegistry. (Locale/getDefault)))
               step
               keyword
               (FunctionNameGenerator. (->ClojureFnName))))
