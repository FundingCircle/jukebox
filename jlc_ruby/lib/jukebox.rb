# frozen_string_literal: true

module Jukebox
  class <<self
    attr_accessor :steps
  end

  def Step(step, symbol = nil, &proc)
    proc_or_sym = symbol || proc
    Jukebox.steps ||= {}
    Jukebox.steps[step] = proc_or_sym
  end
end
