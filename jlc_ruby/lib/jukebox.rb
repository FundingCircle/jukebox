# frozen_string_literal: true

require 'logger'

# Defines the Jukebox DSL
module Jukebox
  class PendingError < StandardError
  end

  class UndefinedError < StandardError
  end

  class << self
    def log
      @log ||= Logger.new(STDOUT)
      @log.level = Logger::WARN
      @log
    end
  end

  def step(*triggers, **opts, &block)
    Jukebox::Client::StepRegistry.add(*triggers, **opts, &block)
  end

  # Mark a step implementation as pending
  def pending!
    raise PendingError
  end

  module_function :step, :pending!
end
