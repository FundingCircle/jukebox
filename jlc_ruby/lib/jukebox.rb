# frozen_string_literal: true

module Jukebox
  class <<self
    attr_accessor :steps

    def run_step(step, board, args)
      proc_or_sym = Jukebox.steps[step]
      p "Running step: #{step} with board: #{board} and args: #{args}: #{proc_or_sym}"
      Jukebox.steps[step].call(board, *args)
    end
  end

  def Step(step, symbol = nil, &proc)
    proc_or_sym = symbol || proc
    Jukebox.steps ||= {}
    Jukebox.steps[step] = proc_or_sym
  end
end
