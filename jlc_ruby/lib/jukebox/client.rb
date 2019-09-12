# frozen_string_literal: true

require 'eventmachine'
require 'faye/websocket'
require 'logger'
require 'jukebox/client/step_scanner'
require 'jukebox/client/step_registry'
require 'json'
require 'optparse'
require 'securerandom'
require 'active_support'
require 'active_support/core_ext'
require 'jukebox/core_ext/hash'

module Jukebox
  # A jukebox language client for ruby.
  module Client
    class UnknownAction < StandardError
    end

    @ws = nil
    @local_board = {}
    @logger = Logger.new(STDOUT)
    @logger.level = Logger::DEBUG

    class << self
      # Create an error response message.
      def error(message, exception)
        message.merge(
          action: 'error',
          message: exception.message,
          trace: exception.backtrace_locations&.map do |location|
            { class_name: location.label,
              file_name: location.path,
              line_number: location.lineno,
              method_name: location.label }
          end
        )
      end

      # Run a step or hook
      def run(message)
        message.merge(action: 'result', board: StepRegistry.run(message))
      rescue Exception => e
        error(message, e)
      end

      def read_message_data(message_data, local_board)
        message = JSON.parse(message_data).deep_symbolize_keys
        message[:board] = message[:board]&.deep_merge(local_board)
        message
      end

      def write_message_data(message_data)
        board = message_data[:board] || {}
        board, @local_board = board.transmittable_as(&JSON.method(:generate))
        message_data[:board] = board
        message_data.deep_transform_keys! { |k| k.to_s.dasherize }
        JSON[message_data]
      end

      # Handle messages from the coordinator
      def handle_coordinator_message(message)
        message_data = read_message_data(message.data, @local_board)

        case message_data[:action]
        when 'run' then @ws.send write_message_data(run(message_data))
        else raise UnknownAction, "Unknown action: #{message_data[:action]}"
        end
      rescue Exception => e
        @ws.send write_message_data(error(message.data, e))
      end

      def template
        <<~END_TEMPLATE
          require 'jukebox'
          module 'MyTests'
            extend Jukebox

            step ''{1}'' do |{3}|
              pending! # {4}
              board # return the updated board
            end
          end
        END_TEMPLATE
      end

      # Client details for this jukebox client
      def client_info(client_id = nil)
        { action: 'register',
          client_id: client_id || SecureRandom.uuid,
          language: 'ruby',
          version: '1',
          definitions: Jukebox::Client::StepRegistry.definitions,
          snippet: {
            argument_joiner: ', ',
            escape_pattern: %w['\'' '\\\''],
            template: template
          } }
      end

      # Start this jukebox language client.
      def start(_client_options, port, glue_paths)
        Jukebox::Client::StepScanner.load_step_definitions!(glue_paths)

        EM.run do
          @ws = Faye::WebSocket::Client.new("ws://localhost:#{port}/jukebox")
          @ws.on :message, method(:handle_coordinator_message)
          @ws.send write_message_data(client_info)
        end
      end
    end
  end
end
