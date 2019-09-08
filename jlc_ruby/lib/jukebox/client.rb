# frozen_string_literal: true

require 'eventmachine'
require 'faye/websocket'
require 'logger'
require_relative '../../lib/jukebox/client/scanner'
require_relative '../../lib/jukebox/client/step_registry'
require 'json'
require 'optparse'
require 'securerandom'
require 'active_support'
require 'active_support/core_ext'

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
      def assoc_in(hash, key_path, value)
        b = hash
        key_path.each do |key|
          b[key] |= {}
          b = b[key]
        end
        b[key] = value
        hash
      end

      # Removes non-serializable entries from the map, stashing them
      # in `local-board`.
      def to_jsonifiable(board, key_path = [], local_board = {})
        return board, local_board if board.nil?

        board.each do |key, value|
          # TODO: arrays
          if value.is_a?(Hash)
            board[key] = to_jsonifiable(value, key_path << key, local_board)
          else
            begin
              JSON.generate(value)
            rescue StandardError
              @logger.warn("Note: Board entry can't be transmitted across languages: '#{key}'': '#{value}'")
              assoc_in(local_board, key_path, value)
              board.except!(key)
            end
          end
        end

        [board, local_board]
      end

      def from_jsonifiable(board)
        board&.deep_merge(@local_board)
      end

      # Send a message to the jukebox coordinator.
      def send!(message)
        message[:board], @local_board = to_jsonifiable(message[:board])
        message.deep_transform_keys! { |k| k.to_s.dasherize }
        @ws.send(JSON[message])
      end

      # Stop the ruby jukebox language client.
      def stop!
        return unless @ws

        @ws.close
        @ws = nil
      end

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
      end

      # Handle messages from the coordinator
      def handle_coordinator_message(message)
        message = JSON.parse(message.data)
                      .deep_transform_keys! { |k| k.underscore.to_sym }
        message[:board] = from_jsonifiable(message[:board])

        case message[:action]
        when 'run' then send!(run(message))
        when 'stop' then stop!
        else raise UnknownAction, "Unknown action: #{message[:action]}"
        end
      rescue Exception => e
        send!(error(message, e))
      end

      def template
        "  require 'jukebox'\n" \
        "  module 'MyTests'\n" \
        "    extend Jukebox\n" \
        "    \n" \
        "    step ''{1}'' do |{3}|\n" \
        "      pending! # {4}\n" \
        "      board # return the updated board\n" \
        "    end\n" \
        "  end\n"
      end

      # Client details for this jukebox client
      def client_info
        { action: 'register',
          client_id: SecureRandom.uuid,
          language: 'ruby',
          version: '1',
          definitions: Jukebox.definitions,
          snippet: {
            argument_joiner: ', ',
            escape_pattern: %w['\'' '\\\''],
            template: template
          } }
      end

      # Start this jukebox language client.
      def start(_client_options, port, glue_paths)
        Jukebox::Client::Scanner.load_step_definitions!(glue_paths)

        EM.run do
          @ws = Faye::WebSocket::Client.new("ws://localhost:#{port}/jukebox")
          @ws.on :message, method(:handle_coordinator_message)
          send!(client_info)
        end
      end
    end
  end
end
