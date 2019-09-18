# frozen_string_literal: true

require 'active_support/core_ext'
require 'logger'
require 'singleton'
require 'securerandom'
require 'set'

module Jukebox
  class Client
    # Step registry.
    class StepRegistry
      include Singleton
      attr_reader :definitions, :callbacks

      def initialize
        @definitions = []
        @callbacks = {}
        @resources = Set[]
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

      # Run a step or a hook
      def run(id:, board:, args:, **message)
        callback = @callbacks[id]
        raise UndefinedError, "Undefined callback: #{message}" unless callback

        callback.call(board, *args)
      end

      # Finds a step's definition
      def find_definition(trigger)
        @definitions.find { |d| d[:triggers].include? trigger }
      end

      # Normalizes a step's options
      def self.cleanup_opts(opts)
        opts['scene/tags'.to_sym] = [opts[:tags]] if opts[:tags].is_a?(String)
        opts['scene/resources'.to_sym] = opts[:resources] if opts[:resources]
        opts.except!(:tags, :resources)
      end
    end
  end
end
