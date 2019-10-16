# frozen_string_literal: true

require 'rspec'
require 'active_support/core_ext'
require 'jukebox/client'
require 'jukebox/step_registry'

describe Jukebox::Client do
  step_registry = Jukebox::StepRegistry

  context 'jukebox client info' do
    it 'should include step definitions and template snippet' do
      client_id = SecureRandom.uuid

      expect(Jukebox::Client.new(['spec/glue_paths/jukebox'], client_id).client_info)
        .to eq(action: :register,
               client_id: client_id,
               language: 'ruby',
               definitions: step_registry.instance.definitions,
               snippet: {
                 argument_joiner: ', ',
                 escape_pattern: %w['\'' '\\\''],
                 template: "# Ruby:\n" \
                           "step ''{1}'' do\n" \
                           "  |{3}|\n" \
                           "  pending! # {4}\n" \
                           "  board # Return the updated board\n" \
                           "end\n" \

               })
    end
  end

  context 'running a step fails' do
    subject do
      raise 'step failure test'
    rescue StandardError => e
      Jukebox::Client.error({ foo: :bar }, e)
    end

    it 'an error message payload is created' do
      expect(subject.except(:trace)).to eq(action: :error,
                                           foo: :bar,
                                           message: 'step failure test')
    end
  end

  context 'executing a step' do
    subject do
      @trigger = SecureRandom.uuid
      @test_callback = proc { |board, arg1| board.merge(arg1: arg1) }
      step_registry.instance.add @trigger, tags: '@foo', &@test_callback

      @definition = step_registry.instance.find_definition(@trigger)
      Jukebox::Client.run(id: @definition[:id],
                          board: { a: 1 },
                          args: [2])
    end

    it 'produces a result message' do
      expect(subject).to eq(action: :result,
                            id: @definition[:id],
                            board: { a: 1, arg1: 2 },
                            args: [2])
    end
  end
end
