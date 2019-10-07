# frozen_string_literal: true

require 'rspec/expectations'
require 'jukebox'

module Jukebox
  # Cucumber-compatibility layer
  module Cukes
    @logger = Logger.new(STDOUT)
    @logger.level = Logger::INFO
    $stdout.sync = true

    include RSpec::Matchers

    # rubocop:disable Naming/MethodName

    def Given(trigger, &block)
      Cukes.step(trigger, &block)
    end

    def BeforeStep(tag_expressions = '', &block)
      Cukes.step(:before_step, tags: tag_expressions, &block)
    end

    def AfterStep(tag_expressions = '', &block)
      Cukes.step(:after_step, tags: tag_expressions, &block)
    end

    def Before(tag_expressions = '', &block)
      Cukes.step(:before, tags: tag_expressions, &block)
    end

    def After(tag_expressions = '', &block)
      Cukes.step(:after, tags: tag_expressions, &block)
    end

    def World(mixin)
      Cukes.extend mixin
    end

    # rubocop:enable Naming/MethodName

    # Mark a step implementation as pending
    def pending
      raise PendingError
    end

    def failed?
      false
    end

    alias When Given
    alias Then Given
    alias And Given

    class << self
      def load_support_files(path)
        @logger.debug("Loading support files from: #{path}")
        $LOAD_PATH.unshift "./#{path}/support"
        env_rb = "./#{path}/support/env.rb"
        load env_rb if File.file?(env_rb)
        Dir["./#{path}/support/**/*.rb"].sort.each do |file|
          load file
        end
      end

      def load_step_definitions(path)
        @logger.debug("Loading step definition files from: #{path}")
        Dir["./#{path}/step_definitions/**/*.rb"].sort.each do |file|
          @logger.debug("Loading step definitions file: #{file}")
          load file
        end
      end

      # Scan for cucumber-style step definitions
      def scan(glue_paths)
        glue_paths.each do |path|
          load_support_files(path)
          load_step_definitions(path)
        end
      end

      def step(*triggers, **opts, &block)
        Jukebox.step(*triggers, **opts) do |board, *args|
          if block.arity == args.size + 1
            Cukes.instance_exec(board, *args, &block)
          else
            puts "Cukes running block: #{block}"
            Cukes.instance_exec(*args, &block)
            board
          end
        end
      end
    end
  end
end

extend Jukebox::Cukes # rubocop:disable Style/MixinUsage
