# frozen_string_literal: true

require 'logger'

# Defines the Jukebox DSL
module Jukebox
  class PendingError < StandardError; end
  class UndefinedError < StandardError; end

  @@steps = {}

  class <<self
    def log
      @log ||= Logger.new(STDOUT)
      @log.level = Logger::ERROR
      @log
    end

    # attr_accessor :steps

    def run_step(step, board, args)
      @@steps ||= {}
      proc_or_sym = @@steps[step]
      Jukebox.log.debug("Running step: #{step} with board: #{board} and args: #{args}: #{proc_or_sym}")
      raise UndefinedError unless proc_or_sym

      @@steps[step].call(board, *args)
    end

    def unimplemented_step
      raise PendingError
    end
  end

  # Registers a step definition
  def step(step, symbol = nil, &proc)
    proc_or_sym = symbol || proc
    raise UndefinedError unless proc_or_sym

    @@steps[step] = proc_or_sym
  end

  # Mark a step implementation as pending
  def pending
    raise PendingError
  end


  module_function :step
end
