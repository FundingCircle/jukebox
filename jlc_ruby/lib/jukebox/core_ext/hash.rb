# frozen_string_literal: true

require 'active_support/core_ext'

class Hash #:nodoc:
  def dig_in(*key_path, value)
    b = self
    key = key_path.pop
    key_path.each do |k|
      b[k] = {} unless b.key?(k)
      b = b[k]
    end
    b[key] = value
    self
  end
end
