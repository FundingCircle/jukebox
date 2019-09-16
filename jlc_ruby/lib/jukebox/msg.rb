# frozen_string_literal: true

require 'msgpack'
require 'set'

class Symbol #:nodoc:
  def to_msgpack_ext
    to_s.to_msgpack
  end

  def self.from_msgpack_ext(data)
    MessagePack.unpack(data).to_sym
  end
end

class Set #:nodoc:
  def to_msgpack_ext
    to_a.to_msgpack
  end

  def self.from_msgpack_ext(data)
    Set.new(MessagePack.unpack(data))
  end
end

MessagePack::DefaultFactory.register_type(0x00, Symbol)
MessagePack::DefaultFactory.register_type(0x01, Set)

module Jukebox
  # Coordinator/client messages.
  module Msg
    # Removes non-transmittable entries from the map, stashing them
    # in `local-board`. (Doesn't scan arrays)
    def self.to_transmittable(board, key_path = [], local_board = {}, &block)
      board ||= self
      board.each do |key, value|
        if value.is_a?(Hash)
          key_path << key
          board[key], local_board = to_transmittable(value,
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

    def self.unpacker(io)
      input_stream = ::MessagePack::Unpacker.new(io)
      input_stream.register_type(0x00, Symbol, :from_msgpack_ext)
      input_stream
    end

    def self.packer(io)
      output_stream = ::MessagePack::Packer.new(io)
      output_stream.register_type(0x00, Symbol, :to_msgpack_ext)
      output_stream
    end

  end

  class Client #:nodoc:
    def send(message)
      board = message[:board] || {}
      board, @local_board = Jukebox::Msg.to_transmittable(board, &MessagePack.method(:pack))
      message[:board] = board
      @socket.write(MessagePack.pack(message))
    end

    def messages
      Messages.new(@socket)
    end

    # Sequence of messages from the coordinator
    class Messages
      def initialize(socket)
        @socket = socket
      end

      def each
        Jukebox::Msg.unpacker(@socket).each do |message|
          message[:board] = message[:board]&.deep_merge(@local_board) if @local_board
          yield message
        end
      end
    end
  end
end
