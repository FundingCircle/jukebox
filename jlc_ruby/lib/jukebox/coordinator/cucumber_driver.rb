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
                       StepScanner.new(@step_registry, self, true)
                ])
        @missing_steps
      end

      # Runs the steps for all features.
      def execute_steps
        configuration = ::Cucumber::Configuration.default
        receiver = configuration.event_bus
        # compile(documents(@feature_paths), receiver, [::Cucumber::Core::Test::TagFilter.new(['@jukebox']), StepRunner.new(@step_registry)]) do |events|
        execute(documents(@feature_paths), [::Cucumber::Core::Test::TagFilter.new(['@jukebox']), StepRunner.new(@step_registry)]) do |events|
          events.on(:test_case_started) { @step_registry.run_hooks(:before) }
          events.on(:test_step_started) { |event| @step_registry.run_hooks(:before_step); puts "Starting step: #{event.test_step}" }
          events.on(:test_step_finished) { |event|
            puts "STEP FINISHED: #{event.result == Test::Result::Failed}"
            case event.result
            when Test::Result::Failed then puts "** Failed **"
            when Test::Result::Passed then puts "** Passed **"
            else puts "Unknown result"
            end
            @step_registry.run_hooks(:after_step)
          }
          events.on(:test_case_finished) { @step_registry.run_hooks(:after) }
        end
      rescue => e
        puts "execute_steps rescue: #{e}"
        raise e
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
      attr_accessor :error

      # def initialize(step_registry)
      #   @step_registry = step_registry
      #   # @receiver = receiver
      # end

      def test_case(test_case)
        test_steps = test_case.test_steps.map do |step|
          run(step)
        end

        puts "-> test_case: #{receiver}"
        test_case.with_steps(test_steps).describe_to(receiver)
      end

      def run(step)
        step.with_action do # TODO: add location
          puts "-> Running step: #{step.text} (#{error})"
          puts "-> Skipping step: #{step.text}" if error
          step_registry.run_step(step.text) unless error
          # ::Cucumber::Core::Test::Result::Passed.new(0)
        rescue StepError => e
          error = e
          puts "-> with_action exception: #{error}: "
          # ::Cucumber::Core::Test::Result::Failed.new(0, e)
          raise e
        end
      # rescue Jukebox::Coordinator::StepError => e
      #   step.with_action { raise e }
      end
    end

    # Implements a Cucumber Filter that will scan for step definitions.
    class StepScanner < Cucumber::Core::Filter.new(:step_registry, :cucumber_backend, :collect_missing)
      # attr_reader :cucumber_backend

      # def initialize(step_registry, cucumber_backend, collect_missing = false, receiver = nil)
      #   @step_registry = step_registry
      #   @cucumber_backend = cucumber_backend
      #   @collect_missing = collect_missing
      #   @receiver = receiver
      # end

      def test_case(test_case)
        test_steps = test_case.test_steps.map do |step|
          activate(step)
        end

        test_case.with_steps(test_steps).describe_to(receiver)
      end

      def activate(step)
        step.with_action(step.location) do
          callback, _args = @step_registry.find_callback(step.text)

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
