# frozen_string_literal: true

require 'logger'
require 'securerandom'

module Jukebox
  module Client
    # Step registry.
    module StepRegistry
      @definitions = []
      @callbacks = {}

      class << self
        attr_reader :definitions, :callbacks

        # Add a step or hook to the step registry.
        def add(*triggers, **opts, &block)
          raise UndefinedError unless block

          triggers.map! { |t| t.is_a?(Symbol) ? t.to_s : t.inspect[1..-2] }
          opts[:tags] = [opts[:tags]] if opts[:tags].is_a?(String)

          id = SecureRandom.uuid
          @definitions << {
            id: id,
            triggers: triggers,
            opts: opts
          }
          @callbacks[id] = block
        end

        # Run a step or a hook
        def run(id:, board:, args:, **message)
          callback = Jukebox::Client::StepRegistry.callbacks[id]
          raise UndefinedError, "Undefined callback: #{message}" unless callback

          callback.call(board, *args)
        end
      end
    end
  end
end
