# frozen_string_literal: true

require 'logger'

# Defines the Jukebox DSL
module Jukebox
  class <<self
    def log
      @log ||= Logger.new(STDOUT)
      @log.level = Logger::ERROR
      @log
    end

    attr_writer :steps

    def steps
      @steps || {}
    end

    def run_step(step, board, args)
      proc_or_sym = Jukebox.steps[step]
      Jukebox.log.debug("Running step: #{step} with board: #{board} and args: #{args}: #{proc_or_sym}")
      Jukebox.steps[step].call(board, *args)
    end
  end

  # Registers a step definition
  #
  #   Step()
  def Step(step, symbol = nil, &proc)
    proc_or_sym = symbol || proc
    Jukebox.steps ||= {}
    Jukebox.steps[step] = proc_or_sym
  end
end
