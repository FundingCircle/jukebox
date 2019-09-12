# frozen_string_literal: true

require 'rspec'
require 'jukebox/client/step_registry'
require 'jukebox/client/step_scanner'

step_registry = Jukebox::Client::StepRegistry
scanner = Jukebox::Client::StepScanner

describe Jukebox::Client::StepScanner do
  context 'a glue path is scanned for step definitions' do
    before :all do
      step_registry.clear
      scanner.load_step_definitions!(['spec/glue_paths/jukebox'])
    end

    it 'registers the step definitions' do
      expected_steps = ['today is Sunday',
                        "I ask whether it's Friday yet",
                        'I should be told {string}']

      expected_steps.each do |step|
        expect(step_registry.find_trigger(step)).to_not be_nil
      end
    end
  end

  context 'a glue path contains cucumber-style definitions' do
    before :all do
      step_registry.clear
      scanner.load_step_definitions!(['spec/glue_paths/cucumber_compat'])
    end

    it 'registers the step definitions' do
      expected_steps = ['today is Sunday',
                        "I ask whether it's Friday yet",
                        'I should be told {string}']

      expected_steps.each do |step|
        expect(step_registry.find_trigger(step)).to_not be_nil
      end
    end
  end
end
