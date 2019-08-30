# frozen_string_literal: true

require 'logger'
require 'securerandom'

# Defines the Jukebox DSL
module Jukebox
  class PendingError < StandardError; end
  class UndefinedError < StandardError; end

  @@steps = {}
  @@hook_registry = {before: [], after: []}
  @@hook_index = {}
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

    def hook_registry
      @@hook_registry
    end

    def load_glue(file)
      glue = File.open(file)
      instance_eval glue.read, file
    end

    def run_step(step, board, args)
      @@steps ||= {}
      proc_or_sym = @@steps[step]
      Jukebox.log.debug("Running step: #{step} with board: #{board} and args: #{args}: #{proc_or_sym}")
      raise UndefinedError unless proc_or_sym

      @@steps[step].call(board, *args)
    end

    def run_hook(board, hook_id)
      Jukebox.instance_eval do
        @@hook_index[hook_id].call
        board
      end
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

    step = step.inspect[1..-2]
    p "REGISTERING STEP: #{step}: #{proc_or_sym}"
    @@steps[step] = Proc.new { |board, *args|
      Jukebox.instance_eval { proc_or_sym.call(*args) }
      board
    }
  end


  def failed?
    false
  end

  def Before(*tag_expressions, &before_proc)
    raise UndefinedError unless before_proc

    hook_id = SecureRandom.uuid
    @@hook_registry[:before] << {
      id: hook_id,
      tags: tag_expressions
    }

    @@hook_index[hook_id] = before_proc
  end

  def After(*tag_expressions, &after_proc)
    raise UndefinedError unless after_proc

    hook_id = SecureRandom.uuid
    @@hook_registry[:after] << {
      id: hook_id,
      tags: tag_expressions
    }

    @@hook_index[hook_id] = after_proc
  end

  def World(s)
    p "WORLD: #{s}"
    Jukebox.extend s
  end

  alias When Given
  alias Then Given
  alias And Given

  # Mark a step implementation as pending
  def pending
    raise PendingError
  end

  module_function :step, :And, :Given, :When, :Then, :After, :Before, :failed?, :World
end
