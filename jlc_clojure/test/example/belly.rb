# frozen_string_literal: true

require 'jukebox'

# Example tests using jukebox
module ExampleTests
  extend Jukebox

  step 'today is Sunday' do
    |board|
    board
  end

  step 'I ask whether it\'s Friday yet' do
    |board|
    # Write code here that turns the phrase above into concrete actions
    board
  end

  step 'I should be told {string}', {resources: ["kafka/topic-h"]} do
    |board, _string|
    # Write code here that turns the phrase above into concrete actions
    board
  end

  step 'I wait {int} hour' do
    |board, _hours|
    # Write code here that turns the phrase above into concrete actions
    board
  end

  step 'my belly should growl' do
    |board|
    # Write code here that turns the phrase above into concrete actions
    board
  end

  step :before, :after_step, tags: "@tag1 or @tag2 and @rb" do
    |board|
    board
  end

  step :before, :after do
    |board|
    board
  end

  step :before do
    |board|
    board
  end

  require 'jukebox/cukes'
  extend Jukebox::Cukes
  Before do |board|
    board
  end

  step 'a ruby step that fails' do |board|
    pending! # Write code here that turns the phrase above into concrete actions
    board # return the updated board
  end
end
