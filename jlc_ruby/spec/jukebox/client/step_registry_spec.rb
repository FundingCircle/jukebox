# frozen_string_literal: true

require 'rspec'
require 'jukebox/client/step_registry'

StepRegistry = Jukebox::Client::StepRegistry

describe Jukebox::Client::StepRegistry do
  context 'a step definition is added to the step registry' do
    before :all do
      @trigger = SecureRandom.uuid
      @test_callback = proc { |board, arg1| board.merge(arg1: arg1) }
      StepRegistry.instance.add @trigger, tags: '@foo', &@test_callback

      @definition = StepRegistry.instance.find_trigger(@trigger)
      @callback = StepRegistry.instance.callbacks[@definition[:id]]
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
      board = StepRegistry.instance.run(id: @definition[:id],
                                        board: { a: 1 },
                                        args: [2])
      expect(board).to eq(a: 1, arg1: 2)
    end
  end
end
