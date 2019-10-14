# frozen_string_literal: true

require 'rspec'
require 'jukebox/step_registry'
require 'jukebox/client/step_scanner'

describe Jukebox::Client::StepScanner do
  StepRegistry = Jukebox::StepRegistry
  StepScanner = Jukebox::Client::StepScanner

  context 'a glue path is scanned for step definitions' do
    before :all do
      StepScanner.scan(['spec/glue_paths/jukebox'])
    end

    it 'registers the step definitions' do
      expected_steps = ['today is Sunday',
                        "I ask whether it's Friday yet",
                        'I should be told {string}']

      expected_steps.each do |step|
        expect(StepRegistry.instance.find_definition(step)).to_not be_nil
      end
    end
  end

  context 'a glue path contains cucumber-style definitions' do
    before :all do
      StepScanner.scan(['spec/glue_paths/cucumber_compat'])
    end

    it 'registers the step definitions' do
      expected_steps = ['today is Sunday',
                        "I ask whether it's Friday yet",
                        'I should be told {string}']

      expected_steps.each do |step|
        expect(StepRegistry.instance.find_definition(step)).to_not be_nil
      end
    end
  end
end
