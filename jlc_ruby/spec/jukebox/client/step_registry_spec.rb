# frozen_string_literal: true

require 'rspec'
require 'jukebox/client/step_registry'

describe Jukebox::Client::StepRegistry do
  context 'a step definition is added to the step registry' do
    step_registry = Jukebox::Client::StepRegistry

    before :all do
      @trigger = SecureRandom.uuid
      @test_callback = proc { |board, arg1| board.merge(arg1: arg1) }
      step_registry.instance.add @trigger, tags: '@foo', &@test_callback

      @definition = step_registry.instance.find_definition(@trigger)
      @callback = step_registry.instance.callbacks[@definition[:id]]
    end

    it 'saves the step definition' do
      expect(@definition).to_not be_nil
      expect(@definition[:opts]).to eq('scene/tags': ['@foo'])
      expect(@definition[:id]).to_not be_nil
      expect(@definition[:triggers]).to eq([@trigger])
    end

    it 'registers the callback' do
      expect(@callback).to_not be_nil
      expect(@callback).to eq(@test_callback)
    end

    it 'runs the step definition' do
      board = step_registry.instance.run(id: @definition[:id],
                                         board: { a: 1 },
                                         args: [2])
      expect(board).to eq(a: 1, arg1: 2)
    end
  end
end
