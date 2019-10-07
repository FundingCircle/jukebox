# frozen_string_literal: true

require 'active_support'
require 'active_support/core_ext'
require 'logger'
require 'json'
require 'jukebox/coordinator/cucumber_driver'
require 'jukebox/step_registry'
require 'jukebox/msg'
require 'shellwords'
require 'singleton'
require 'socket'

module Jukebox #:nodoc:
  # Coordinate with remote step executors.
  class Coordinator
    include Singleton

    @logger = Logger.new(STDOUT)
    @logger.level = Logger::DEBUG
    $stdout.sync = true

    # Loads language client configs from a json file named '.jukebox'.
    def self.language_client_configs
      return nil unless File.exist?('.jukebox')

      File.read('.jukebox') do |file|
        project_configs = JSON.parse(file)
        language_clients = project_configs['language-clients']
                             .merge([{ 'language' => 'ruby', 'launcher' => 'jlc_cli', 'cmd' => 'bundle exec jlc_ruby' },
                                     { 'language' => 'clojure', 'launcher' => 'jlc_cli', 'cmd' => 'clojure -A:jukebox/client' }])
        project_languages = Set.new(project_configs['languages'])
        language_clients.select { |language_client| project_languages.include?(language_client['language']) } if project_configs['languages']
      end
    end

    def self.auto_detect(clojure: false, ruby: false)
      clojure ||= File.exist?('deps.edn') || File.exist?('project.clj')
      ruby ||= File.exist?('Gemfile')
      language_clients = []
      language_clients << { 'language' => 'clojure', 'launcher' => 'jlc_cli', 'cmd' => 'clojure -A:jukebox/client' } if clojure
      language_clients << { 'language' => 'ruby', 'launcher' => 'jlc_cli', 'cmd' => 'bundle exec jlc_ruby' } if ruby
      language_clients
    end

    def initialize
      super() # Initialize Concurrent::Async
      @clients = {}
      @step_registry = Jukebox::StepRegistry.instance
      # @snippets = []
    end

    class StepError < StandardError
      attr_reader :client_backtrace

      def initialize(msg = nil, client_backtrace = nil)
        puts "STEP ERROR TRACE: #{client_backtrace}"
        # puts "STEP ERROR MSG: #{msg}"
        @client_backtrace = client_backtrace
        super(msg)
      end
    end

    def register_client(messenger, language:, client_id:, definitions:, snippet:, **_)
      @clients[client_id] = messenger

      definitions.each do |id:, **definition|
        callback = proc do |board, *args|
          @logger.debug("Forwarding step to jukebox language client (#{language}) #{client_id}")
          messenger.send(action: :run,
                         id: id,
                         board: board,
                         args: args)
          message = messenger.recv
          case message[:action]
          when :result then
            message[:board]
          when :error then
            puts "TRACE: #{message[:trace]}"

            # message[:trace].each do |entry|
            #   puts entry
            # end
            # puts message[:trace].join '\n'
            # TODO: Fill in stack trace
            # raise StepError.new("hello", nil)

            raise StepError.new(message[:message], message[:trace])
          else
            raise "Unknown message from client: #{message}"
          end
        end

        definition[:triggers].each do |trigger|
          @step_registry.register_callback(trigger, callback)
        end

        @step_registry.snippets[language] = snippet
      end
    end

    # Accept incoming client connections and register steps.
    def register_clients(server_socket, client_configs, glue_paths)
      port = server_socket.connect_address.ip_port
      client_configs.each do |client_config|
        Thread.new { run_client(client_config, port, glue_paths) }
        client_socket = server_socket.accept
        messenger = Jukebox::Messenger.new(client_socket)
        client_registration = messenger.recv
        client_registration[:callbacks] = register_client(messenger, **client_registration)
      end

      @step_registry
    rescue StandardError => e
      puts e
      puts e.backtrace
      raise e
    end

    def run_client(client_config, port, glue_paths)
      cmd = client_config['cmd']
      system("#{cmd} --port #{port} #{glue_paths.shelljoin}")
    end

    # Starts the step coordinator
    def start(feature_paths, glue_paths)
      @logger = Logger.new(STDOUT)
      @logger.level = Logger::DEBUG

      client_configs = Coordinator.language_client_configs || Coordinator.auto_detect
      @server_socket = TCPServer.new('0.0.0.0', 0)

      register_clients(@server_socket, client_configs, glue_paths)

      cucumber = CucumberDriver.new(feature_paths, @step_registry)
      missing_steps = cucumber.scan
      if missing_steps
        missing_steps.each do |snippet|
          @step_registry.snippets.each do |language, snippet_template|
            puts snippet.render(snippet_template)
          end
        end
      else
        cucumber.execute_steps
      end
    end

    def stop
      if @server_socket
        @server_socket.close
        @server_socket = nil
      end

      @clients = {}
    end
  end
end
