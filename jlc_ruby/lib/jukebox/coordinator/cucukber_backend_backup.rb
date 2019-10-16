# frozen_string_literal: true

require 'singleton'
require 'gherkin/token_scanner'
require 'gherkin/token_matcher'
require 'gherkin/parser'
require 'gherkin/dialect'

module Jukebox
  class Coordinator
    class CucumberDriver
      include Singleton

      @logger = Logger.new(STDOUT)
      @logger.level = Logger::DEBUG
      $stdout.sync = true

      def initialize
        @board = {}
        @language = 'en'
      end

      def register_definitions(step_registry)
        step_registry.definitions.each do |id:, triggers:, **_|
          triggers.each do |trigger|
            Cucumber::Glue::Dsl.register_rb_step_definition(trigger, proc { |*args|
              step_registry.run(id: id, board: @board, args: args)
            })
          end
        end
      end

      def parse(text)
        dialect = ::Gherkin::Dialect.for(@language)
        token_matcher = ::Gherkin::TokenMatcher.new(@language)
        token_scanner = ::Gherkin::TokenScanner.new(feature_header(dialect) + text)
        parser = ::Gherkin::Parser.new
        gherkin_document = parser.parse(token_scanner, token_matcher)

        # @builder.steps(gherkin_document[:feature][:children][0][:steps])
        p gherkin_document
      end

      def feature_header(dialect)
        %(#{dialect.feature_keywords[0]}:
            #{dialect.scenario_keywords[0]}:
         )
      end
    end

    # Invokes a series of steps +steps_text+. Example:
    #
    #   invoke(%Q{
    #     Given I have 8 cukes in my belly
    #     Then I should not be thirsty
    #   })
    def invoke_dynamic_steps(steps_text, iso_code, _location)
      parser = Cucumber::Gherkin::StepsParser.new(StepInvoker.new(self), iso_code)
      parser.parse(steps_text)
    end

    def parse(gherkin_documents, compiler, event_bus)
      parser = Core::Gherkin::Parser.new(compiler, event_bus)
      gherkin_documents.each do |document|
        parser.document document
      end
      parser.done
      self
    end

  end
end
