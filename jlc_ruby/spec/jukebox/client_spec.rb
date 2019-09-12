# frozen_string_literal: true

require 'rspec'
require 'active_support/core_ext'
require 'jukebox/client'

describe Jukebox::Client do
  context 'jukebox client info' do
    it 'should include step definitions and template snippet' do
      Jukebox::Client::StepRegistry.clear
      Jukebox::Client::StepScanner.load_step_definitions!(['spec/glue_paths/jukebox'])
      client_id = SecureRandom.uuid

      expect(Jukebox::Client.client_info(client_id))
        .to eq(action: 'register',
               client_id: client_id,
               language: 'ruby',
               version: '1',
               definitions: Jukebox::Client::StepRegistry.definitions,
               snippet: {
                 argument_joiner: ', ',
                 escape_pattern: %w['\'' '\\\''],
                 template: <<~END_TEMPLATE
                   require 'jukebox'
                   module 'MyTests'
                     extend Jukebox

                     step ''{1}'' do |{3}|
                       pending! # {4}
                       board # return the updated board
                     end
                   end
                 END_TEMPLATE
               })
    end
  end

  context 'reading a message payload' do
    subject do
      local_board = { baz: 'bat' }
      Jukebox::Client.read_message_data(
        JSON[{ 'action': 'run', 'board': { 'foo': 'bar' } }],
        local_board)
    end

    it { is_expected.to eq(action: 'run', board: { foo: 'bar', baz: 'bat' }) }
  end

  context 'handling a request to run a step' do
    subject do
      @foo = Unserializable.new
      Jukebox::Client.write_message_data(action: 'result', board: { foo: @foo,
                                                                    bar: 'baz' })
    end

    it 'writes a result message' do
      expect(subject).to eq('{"action":"result","board":{"bar":"baz"}}')
      expect(Jukebox::Client.instance_variable_get(:@local_board))
        .to eq(foo: @foo)
    end
  end

  context 'running a step fails' do
    subject do
      raise 'step failure test'
    rescue StandardError => e
      Jukebox::Client.error({ foo: :bar }, e)
    end

    it 'an error message payload is created' do
      expect(subject.except(:trace)).to eq(action: 'error',
                                           foo: :bar,
                                           message: 'step failure test')
    end
  end

  context 'executing a step' do
    subject do
      @trigger = SecureRandom.uuid
      @test_callback = proc { |board, arg1| board.merge(arg1: arg1)}
      Jukebox::Client::StepRegistry.clear
      Jukebox::Client::StepRegistry.add @trigger, tags: '@foo', &@test_callback

      @definition = Jukebox::Client::StepRegistry.find_trigger(@trigger)
      # @callback = Jukebox::Client::StepRegistry.callbacks[@definition[:id]]
      Jukebox::Client.run(id: @definition[:id],
                          board: { a: 1 },
                          args: [2])
    end

    it 'produces a result message' do
      expect(subject).to eq(action: 'result',
                            id: @definition[:id],
                            board: { a: 1, arg1: 2 },
                            args: [2])
    end
  end
end
