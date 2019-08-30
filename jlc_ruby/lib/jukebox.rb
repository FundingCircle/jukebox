# frozen_string_literal: true

require 'logger'

# Defines the Jukebox DSL
module Jukebox
  class PendingError < StandardError; end
  class UndefinedError < StandardError; end

  @@steps = {}
  @@world = {}

  class <<self
    def log
      @log ||= Logger.new(STDOUT)
      @log.level = Logger::WARN
      @log
    end

    def steps
      @@steps
    end

    def load_glue(file)
      glue = File.open(file)
      # @@world.instance_eval glue.read, file
      instance_eval glue.read, file
    end

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

  def Given(step, symbol = nil, &proc)
    proc_or_sym = symbol || proc
    raise UndefinedError unless proc_or_sym

    p "REGISTERING STEP: #{step}: #{proc_or_sym}"
    @@steps[step] = Proc.new { |board, *args|
      # @@world.instance_eval &proc_or_sym
      Jukebox.instance_eval &proc_or_sym
      board
    }
  end


  def failed?
    false
  end

  def Before(*tag_expressions, &proc)
    raise UndefinedError unless proc

    p "BEFORE: #{tag_expressions} #{proc}"

    # @@world.instance_eval &proc_or_sym
    Jukebox.instance_eval &proc
  end

  def World(s)
    p "WORLD: #{s}"
    Jukebox.extend s
  end

  alias When Given
  alias Then Given
  alias And Given
  alias After Before

  # Mark a step implementation as pending
  def pending
    raise PendingError
  end

  module_function :step, :And, :Given, :When, :Then, :After, :Before, :failed?, :World
end
