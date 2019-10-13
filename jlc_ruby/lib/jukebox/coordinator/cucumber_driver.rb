# frozen_string_literal: true

require 'cucumber/configuration'
require 'cucumber/core'
require 'cucumber/core/filter'
require 'cucumber/core/test/filters/tag_filter'
require 'cucumber/core/test/result'
require 'cucumber/glue/snippet'
require 'jukebox/coordinator/snippet'
require 'logger'

module Jukebox
  class Coordinator

    # Integrate with the cucumber driver.
    class CucumberDriver
      include Cucumber::Core

      # Initialize with a list of paths to search for features and a step registry instance.
      def initialize(feature_paths, step_registry)
        @feature_paths = feature_paths
        @step_registry = step_registry
      end

      # List of missing step snippets.
      def missing_steps
        @missing_steps ||= []
      end

      # Scans for step definitions.
      def scan
        # execute(documents(@feature_paths), [StepScanner.new(@step_registry, self)])
        execute(documents(@feature_paths), [StepScanner.new(@step_registry, self)])
        execute(documents(@feature_paths),
                [::Cucumber::Core::Test::TagFilter.new(['@jukebox']),
                 StepScanner.new(@step_registry, self, true)])
        @missing_steps
      end

      # Runs the steps for all features.
      def execute_steps
        # noinspection RubyArgCount
        execute(documents(@feature_paths), [::Cucumber::Core::Test::TagFilter.new(['@jukebox']), StepRunner.new(@step_registry)]) do |events|
          events.on(:test_case_started) { @step_registry.run_hooks(:before) }
          events.on(:test_step_started) { @step_registry.run_hooks(:before_step) }
          events.on(:test_step_finished) do |event|
            handle_error(event.result.exception) if event.result.is_a?(Test::Result::Failed)
            @step_registry.run_hooks(:after_step)
          end
          events.on(:test_case_finished) { @step_registry.run_hooks(:after) }
        end
      end

      def handle_error(error)
        error = error.exception if error.is_a?(::Jukebox::Coordinator::StepError)
        puts error.backtrace.join("\n")
      end

      # Finds '.feature' files
      def documents(feature_paths)
        feature_paths.flat_map do |feature_path|
          Dir["#{feature_path}/**/*.feature"].map do |feature_file|
            ::Cucumber::Core::Gherkin::Document.new(feature_file, IO.read(feature_file))
          end
        end
      end
    end

    # Implements a Cucumber Filter that will execute each step definition and hook in the right order.
    class StepRunner < Cucumber::Core::Filter.new(:step_registry)
      attr_reader :receiver, :step_registry

      def initialize(step_registry, receiver = nil)
        @step_registry = step_registry
        @receiver = receiver
      end

      def test_case(test_case)
        test_steps = test_case.test_steps.map do |step|
          run(step)
        end

        test_case.with_steps(test_steps).describe_to(receiver)
      end

      def run(step)
        step.with_action do # TODO: add location
          step_registry.run_step(step.text)
        end
      end
    end

    # Implements a Cucumber Filter that will scan for step definitions.
    class StepScanner < Cucumber::Core::Filter.new(:step_registry, :cucumber_backend, :collect_missing)
      attr_reader :collect_missing, :cucumber_backend, :step_registry

      def test_case(test_case)
        test_steps = test_case.test_steps.map do |step|
          activate(step)
        end

        test_case.with_steps(test_steps).describe_to(receiver)
      end

      def activate(step)
        step.with_action(step.location) do
          callback, _args = step_registry.find_callback(step.text)

          if collect_missing && callback.nil?
            cucumber_expression_generator = ::Cucumber::CucumberExpressions::CucumberExpressionGenerator.new(::Cucumber::CucumberExpressions::ParameterTypeRegistry.new)
            expr_gen = cucumber_expression_generator.generate_expression(step.text).source
            cucumber_backend.missing_steps << Snippet.new(expr_gen)
          end
        end
      end
    end
  end
end
