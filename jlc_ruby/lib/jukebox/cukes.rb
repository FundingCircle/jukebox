# frozen_string_literal: true

require 'rspec/expectations'
require_relative '../jukebox'

module Jukebox
  # Cucumber-compatibility layer
  module Cukes
    include RSpec::Matchers

    def Given(trigger, symbol = nil, &block)
      proc_or_sym = symbol || block
      raise UndefinedError unless proc_or_sym

      Jukebox.step(trigger) do |board, *args|
        Cukes.instance_exec *args, &proc_or_sym
        board
      end
    end

    def BeforeStep(tag_expressions = '', &block)
      Jukebox.step(:before_step, tags: tag_expressions) do |board, *args|
        Cukes.instance_exec *args, &block
        board
      end
    end

    def AfterStep(tag_expressions = '', &block)
      Jukebox.step(:after_step, tags: tag_expressions) do |board, *args|
        Cukes.instance_exec *args, &block
        board
      end
    end

    def Before(tag_expressions = '', &block)
      Jukebox.step(:before, tags: tag_expressions) do |board, *args|
        Cukes.instance_exec *args, &block
        board
      end
    end

    def After(tag_expressions = '', &block)
      Jukebox.step(:after, tags: tag_expressions) do |board, *args|
        Cukes.instance_exec *args, &block
        board
      end
    end

    def World(mixin)
      Cukes.extend mixin
    end

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
        $LOAD_PATH.unshift "./#{path}/support"
        Dir["./#{path}/support/**/*.rb"].sort.each do |file|
          load file
        end
      end

      def load_step_definitions(path)
        Dir["./#{path}/step_definitions/**/*.rb"].sort.each do |file|
          File.open(file) do |glue|
            Cukes::instance_eval glue.read, file
          end
        end
      end

      # Scan for cucumber-style step definitions
      def load_step_definitions!(glue_paths)
        glue_paths.each do |path|
          load_support_files(path)
          load_step_definitions(path)
        end
      end
    end

    # module_function :And, :Given, :When, :Then, :After, :Before, :failed?, :World

  end
end