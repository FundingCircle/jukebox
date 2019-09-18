# frozen_string_literal: true

require 'active_support'
require 'active_support/core_ext'
require 'jukebox/client/step_registry'
require 'jukebox/client/step_scanner'
require 'jukebox/core_ext/hash'
require 'jukebox/msg'
require 'logger'
require 'msgpack'
require 'optparse'
require 'securerandom'
require 'socket'

module Jukebox
  # A jukebox language client for ruby.
  class Client
    @local_board = {}
    @logger = Logger.new(STDOUT)
    @logger.level = Logger::INFO
    $stdout.sync = true

    class << self
      # Returns an error response message.
      def error(message, exception)
        message.merge!(
          action: :error,
          message: exception.message,
          trace: exception.backtrace_locations&.map do |location|
            @logger.debug("Backtrace class_name: #{location.label.class}")
            { class_name: '' + location.label,
              file_name: '' + location.path,
              line_number: location.lineno,
              method_name: '' + location.label }
          end
        )
        @logger.debug("Sending error: #{message}: #{exception}")
        message
      end

      # Runs a step or hook, returning a result or error response.
      def run(message)
        @logger.debug("Running: #{message}")
        board = StepRegistry.instance.run(message)
        message.merge!(action: :result, board: board)
        message
      rescue Exception => e
        pp e
        error(message, e)
      end

      def template
        "# Ruby:\n" \
        "step ''{1}'' do\n" \
        "  |{3}|\n" \
        "  pending! # {4}\n" \
        "  board # Return the updated board\n" \
        "end\n" \
      end

      # Start this jukebox language client.
      def start(_client_options, port, glue_paths)
        client = Client.new(glue_paths).connect(port)
        @logger.debug("Connected: #{client}")
        Thread.new { client.handle_coordinator_messages }.join
      end
    end

    # Creates a ruby jukebox language client.
    def initialize(glue_paths, client_id = nil)
      @client_id = client_id || SecureRandom.uuid

      Jukebox::Client::StepScanner.scan(glue_paths)
      @step_registry = nil
      @definitions = Jukebox::Client::StepRegistry.instance.definitions
    end

    # Client details for this jukebox client
    def client_info
      { action: :register,
        client_id: @client_id,
        language: 'ruby',
        definitions: @definitions,
        snippet: {
          argument_joiner: ', ',
          escape_pattern: %w['\'' '\\\''],
          template: Client.template
        } }
    end

    # Connects to the jukebox coordinator and registers known step definitions.
    def connect(port)
      @socket = TCPSocket.open('localhost', port)
      send(client_info)
      self
    end

    # Handles messages from the coordinator
    def handle_coordinator_messages
      messages.each do |message|
        case message[:action]
        when :run then send(Client.run(message))
        else raise "Unknown action: #{message[:action]}"
        end
      rescue Exception => e
        send(Client.error(message, e))
      end
    end
  end
end
