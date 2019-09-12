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

  # Removes non-transmittable entries from the map, stashing them
  # in `local-board`. (Doesn't scan arrays)
  def transmittable_as(board = nil, key_path = [], local_board = {}, &block)
    board ||= self
    board.each do |key, value|
      if value.is_a?(Hash)
        key_path << key
        board[key], local_board = transmittable_as(value,
                                                   key_path,
                                                   local_board,
                                                   &block)
      else
        begin
          block.call(value)
        rescue StandardError
          local_board.dig_in(*key_path << key, value)
          [board.except!(key), local_board]
        end
      end
    end

    [board, local_board]
  end
end
