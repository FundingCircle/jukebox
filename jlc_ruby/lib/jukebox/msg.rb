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

module Jukebox
  # Coordinator/client messages.
  module Msg

    # Wrapper for msgpacked uuids
    class UUID < String
      def to_msgpack_ext
        to_s.to_msgpack
      end

      def self.from_msgpack_ext(data)
        UUID.new(MessagePack.unpack(data))
      end
    end

    EXTENSIONS = { 0x00 => Symbol,
                   0x01 => Set,
                   0x02 => UUID }.freeze

    EXTENSIONS.each do |type, klass|
      MessagePack::DefaultFactory.register_type(type, klass)
    end

    # Removes non-transmittable entries from the map, stashing them
    # in `local-board`. (Doesn't scan arrays)
    def self.to_transmittable(board, key_path = [], local_board = {}, &block)
      board ||= self
      board.each do |key, value|
        if value.is_a?(Hash)
          key_path << key
          board[key], local_board = to_transmittable(value, key_path, local_board, &block)
        else
          unless transmittable?(value, &block)
            # TODO: Print a warning
            local_board.dig_in(*key_path << key, value)
            [board.except!(key), local_board]
          end
        end
      end

      [board, local_board]
    end

    def self.unpacker(io)
      input_stream = ::MessagePack::Unpacker.new(io)
      EXTENSIONS.each do |type, klass|
        input_stream.register_type(type, klass, :from_msgpack_ext)
      end
      input_stream
    end

    def self.packer(io)
      output_stream = ::MessagePack::Packer.new(io)
      EXTENSIONS.each do |type, klass|
        output_stream.register_type(type, klass, :to_msgpack_ext)
      end
      output_stream
    end

    def self.transmittable?(value, &block)
      block.call(value)
      true
    rescue StandardError
      false
    end
  end

  class Client #:nodoc:
    # Sends a message to the coordinator.
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
