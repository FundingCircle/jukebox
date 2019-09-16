# frozen_string_literal: true

require 'rspec/expectations'
require 'jukebox'

module Jukebox
  # Cucumber-compatibility layer
  module Cukes
    include RSpec::Matchers

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

    # Evaluation context for step definitions
    class World
      extend Jukebox::Cukes
    end

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
            World.instance_eval glue.read, file
          end
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
            Cukes.instance_exec(*args, &block)
            board
          end
        end
      end
    end
  end
end
