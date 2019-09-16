# frozen_string_literal: true

require 'logger'

# Scan the code
module Jukebox
  class Client
    # Scan paths for ruby step definitions.
    module StepScanner
      @logger = Logger.new(STDOUT)
      @logger.level = Logger::WARN
      @cuke_keywords = Set[:After,
                           :AfterStep,
                           :And,
                           :Before,
                           :BeforeStep,
                           :Given,
                           :Then,
                           :When,
                           :World]

      class << self
        # Scan for step definitions.
        def scan(glue_paths)
          @logger.debug("Glue paths: #{glue_paths}")

          glue_paths.each do |path|
            Dir["./#{path}/**/*.rb"].each do |file|
              require file
            end
          end
        rescue NoMethodError => e
          raise e unless @cuke_keywords.include?(e.name)

          enable_cucumber_compatibility(glue_paths)
        end

        def enable_cucumber_compatibility(glue_paths)
          @logger.info('Switching to cucumber compatibility mode')
          require_relative '../cukes'
          Jukebox::Cukes.scan(glue_paths)
        end
      end
    end
  end
end
