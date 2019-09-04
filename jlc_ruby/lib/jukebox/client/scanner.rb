#!/usr/bin/env ruby
# frozen_string_literal: true

require 'eventmachine'
require 'faye/websocket'
require 'logger'
require 'json'
require 'optparse'
require 'securerandom'

# Scan the code
module Jukebox
  module Client
    # Scan paths for ruby step definitions.
    module Scanner
      @logger = Logger.new(STDOUT)
      @logger.level = Logger::DEBUG

      class << self
        # Scan for step definitions.
        def load_step_definitions!(glue_paths)
          @logger.debug("Glue paths: #{glue_paths}")
          glue_paths.each do |path|
            Dir["./#{path}/**/*.rb"].each do |file|
              require file
            end
          end
        end
      end
    end
  end
end