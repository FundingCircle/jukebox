# frozen_string_literal: true

require 'active_support/core_ext'
require 'logger'
require 'securerandom'
require 'set'

module Jukebox
  module Client
    # Step registry.
    module StepRegistry
      @definitions = []
      @callbacks = {}
      @resources = Set[]

      class << self
        attr_reader :definitions, :callbacks

        def cleanup_opts(opts)
          opts['scene/tags'] = [opts[:tags]] if opts[:tags].is_a?(String)
          opts['scene/resources'] = opts[:resources] if opts[:resources]
          opts.except!(:tags, :resources)
        end

        # Add a step or hook to the step registry.
        def add(*triggers, **opts, &block)
          raise UndefinedError unless block

          triggers.map! { |t| t.is_a?(Symbol) ? t.to_s : t.inspect[1..-2] }
          id = SecureRandom.uuid
          @definitions << {
            id: id,
            triggers: triggers,
            opts: cleanup_opts(opts)
          }
          @callbacks[id] = block
          @resources += opts[:resources] if opts[:resources]
        end

        # Run a step or a hook
        def run(id:, board:, args:, **message)
          callback = Jukebox::Client::StepRegistry.callbacks[id]
          raise UndefinedError, "Undefined callback: #{message}" unless callback

          callback.call(board, *args)
        end

        def find_trigger(trigger)
          @definitions.find { |d| d[:triggers].include? trigger }
        end

        def clear
          @definitions = []
          @callbacks = {}
        end
      end
    end
  end
end
