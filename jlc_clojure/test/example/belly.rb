# frozen_string_literal: true

require 'jukebox'

# Example tests using jukebox
module ExampleTests
  extend Jukebox

  step 'today is Sunday' do |board|
    board
  end

  step 'I ask whether it\'s Friday yet' do |board|
    # Write code here that turns the phrase above into concrete actions
    board
  end

  step 'I should be told {string}' do |board, string|
    # Write code here that turns the phrase above into concrete actions
    board
  end

  step 'I wait {int} hour' do |board, hours|
    # Write code here that turns the phrase above into concrete actions
    board
  end

  step 'my belly should growl' do |board|
    # Write code here that turns the phrase above into concrete actions
    board
  end

  Before do
    p "Before scenario from ruby"
  end
end
