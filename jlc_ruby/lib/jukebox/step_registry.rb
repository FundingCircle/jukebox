# frozen_string_literal: true

require 'active_support'
require 'active_support/core_ext'
require 'cucumber/cucumber_expressions/cucumber_expression'
require 'cucumber/cucumber_expressions/regular_expression'
require 'cucumber/cucumber_expressions/parameter_type_registry'
require 'logger'
require 'singleton'
require 'securerandom'
require 'set'

module Jukebox
  # Step registry.
  class StepRegistry
    include Singleton
    attr_reader :definitions, :callbacks, :snippets

    def initialize
      @snippets = {}
      @definitions = []
      @callbacks = {}
      @resources = Set[]
      @hooks = { before: [], after: [], before_step: [], after_step: [] }
      @board = { 'fundingcircle.jukebox.backend.cucumber' => true }
    end

    def valid_board?
      @board['fundingcircle.jukebox.backend.cucumber']
    end

    # Add a step or hook to the step registry.
    def add(*triggers, **opts, &block)
      raise UndefinedError unless block

      triggers.map! { |t| t.is_a?(Symbol) ? t : t.inspect[1..-2] }
      id = SecureRandom.uuid
      @definitions << {
        id: id,
        triggers: triggers,
        opts: StepRegistry.cleanup_opts(opts)
      }
      @callbacks[id] = block
      @resources += opts[:resources] if opts[:resources]
    end

    def register_callback(trigger, callback)
      if @hooks.key?(trigger)
        @hooks[trigger] << callback
      elsif trigger.is_a?(String)
        if trigger.starts_with?('^')
          expr = ::Cucumber::CucumberExpressions::RegularExpression.new(
            trigger,
            ::Cucumber::CucumberExpressions::ParameterTypeRegistry.new
          )
          @callbacks[expr] = callback
        else
          expr = ::Cucumber::CucumberExpressions::CucumberExpression.new(
            trigger,
            ::Cucumber::CucumberExpressions::ParameterTypeRegistry.new
          )
          @callbacks[expr] = callback
        end
      else
        raise "Don't know how to register callback with trigger: #{trigger}"
      end
    end

    # Runs a step
    def run(id:, board:, args:, **message)
      callback = @callbacks[id]
      raise UndefinedError, "Undefined callback: #{message}" unless callback

      # puts "Client running step: #{callback}: #{board} / #{args}"
      callback.call(board, *args)
    end

    def run_hooks(trigger)
      raise "Unknown hook type: #{trigger}" unless @hooks.key?(trigger)

      @hooks[trigger].each do |callback|
        scenario = {} # TODO: fill in scenario details
        board = callback.call(@board, scenario)
        raise "Dropped board: Before: '#{@board}' /  After: #{board}" unless valid_board?

        @board = board
      end
    end

    def run_step(trigger)
      callback, args = find_callback(trigger)
      raise "Don't know how to run step: #{trigger}" unless callback

      board = callback.call(@board, *args)
      raise "Dropped board: Before: '#{@board}' /  After: #{board}" unless valid_board?

      @board = board
    rescue => e
      puts "run_step exception: #{e}"
      raise e
    end

    # Finds a step's definition
    def find_definition(trigger)
      @definitions.find { |d| d[:triggers].include? trigger }
    end

    # Find a callback
    def find_callback(step_text)
      step_text = step_text.to_s
      _, callback = @callbacks.find { |expr, _|
        # puts "Finding callback '#{step_text}' / '#{expr}' #{expr.match(step_text)}"
        expr.match(step_text)
      }
      callback
    end

    # Normalizes a step's options
    def self.cleanup_opts(opts)
      opts['scene/tags'.to_sym] = [opts[:tags]] if opts[:tags].is_a?(String)
      opts['scene/resources'.to_sym] = opts[:resources] if opts[:resources]
      opts.except!(:tags, :resources)
    end
  end
end
