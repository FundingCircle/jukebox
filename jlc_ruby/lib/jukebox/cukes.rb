# frozen_string_literal: true

require_relative '../jukebox'

module Jukebox
  # Cucumber-compatibility layer
  module Cukes
    def Given(trigger, symbol = nil, &block)
      proc_or_sym = symbol || block
      raise UndefinedError unless proc_or_sym

      Jukebox.step(trigger) do |board, *args|
        Cukes.instance_eval { proc_or_sym.call(*args) }
        board
      end
    end

    def BeforeStep(tag_expressions = '', &block)
      Jukebox.step(:before_step, tags: tag_expressions, &block)
    end

    def AfterStep(tag_expressions = '', &block)
      Jukebox.step(:after_step, tags: tag_expressions, &block)
    end

    def Before(tag_expressions = '', &block)
      Jukebox.step(:before, tags: tag_expressions, &block)
    end

    def After(tag_expressions = '', &block)
      Jukebox.step(:after, tags: tag_expressions, &block)
    end

    def World(mixin)
      Jukebox.extend mixin
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

    class << Cukes
      # Scan for cucumber-style step definitions
      def load_step_definitions!(glue_paths)
        @logger.debug("Loading ruby step definitions in glue paths: #{glue_paths}")
        glue_paths.each do |path|
          @logger.debug("Scanning glue path w/step_definitions convention: #{path}")
          $LOAD_PATH.unshift "./#{path}/support"
          Dir["#{path}/support/**/*.rb"].sort.each do |file|
            @logger.debug("Loading GLUE file: #{file}")
            require file
          end
          Dir["#{path}/step_definitions/**/*.rb"].sort.each do |file|
            @logger.debug("Loading GLUE file: #{file}")
            File.open(file) do |glue|
              instance_eval glue.read, file
            end
          end
        end
      end
    end
  end
end