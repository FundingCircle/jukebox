# frozen_string_literal: true

require 'rspec'
require 'jukebox'

describe Jukebox do
  include Jukebox
  context 'an undefined step is run' do
    it 'raises a Jukebox::UndefinedError' do
      expect { Jukebox.run_step('undefined step', {}, nil) }
        .to raise_error(Jukebox::UndefinedError)
    end
  end

  context 'a step is registered with no definition' do
    it 'raises a Jukebox::UndefinedError' do
      expect { step('undefined step') }.to raise_error(Jukebox::UndefinedError)
    end
  end

  context 'Pending step definitions' do
    it 'raises a PendingError' do
      step 'pending step' do
        pending
      end

      expect { Jukebox.run_step('pending step', {}, nil) }
        .to raise_error(Jukebox::PendingError)
    end
  end

  context 'Defined step' do
    it 'runs succesfully' do
      step 'defined step' do |board, arg1, arg2|
        board[:success] = true
        board[:arg1] = arg1
        board[:arg2] = arg2
        board
      end

      expect(Jukebox.run_step('defined step', {}, [1, 2]))
        .to eq(success: true, arg1: 1, arg2: 2)
    end
  end
end
