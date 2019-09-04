# frozen_string_literal: true

require 'logger'
require 'securerandom'

# Defines the Jukebox DSL
module Jukebox
  class PendingError < StandardError;
  end
  class UndefinedError < StandardError;
  end

  @definitions = []
  @callbacks = {}

  class << self
    attr_reader :definitions, :callbacks

    def log
      @log ||= Logger.new(STDOUT)
      @log.level = Logger::WARN
      @log
    end

    # def load_glue(file)
    #   glue = File.open(file)
    #   instance_eval glue.read, file
    # end

    def run_callback(id:, board:, args:, **)
      callback = Jukebox.callbacks[id]
      p "Running callback #{id}: #{callback} #{board} #{args}"
      raise UndefinedError, "ID: #{id}: #{board}: #{args}" unless callback

      callback.call(board, *args)
    end

  end

  def step(*triggers, **opts, &proc)
    raise UndefinedError unless proc

    triggers.map! { |t| t.is_a?(Symbol) ? t.to_s : t.inspect[1..-2] }
    opts[:tags] = [opts[:tags]] if opts[:tags].is_a?(String)

    id = SecureRandom.uuid
    Jukebox.definitions << {
      id: id,
      triggers: triggers,
      opts: opts
    }
    Jukebox.callbacks[id] = proc
  end

  # Mark a step implementation as pending
  def pending!
    raise PendingError
  end

  module_function :step, :pending!
end
