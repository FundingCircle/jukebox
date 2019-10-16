# frozen_string_literal: true

require 'jukebox'

# Example tests using jukebox
module ExampleTests
  extend Jukebox

  step 'today is Sunday' do
    |board|
    puts '-> today is Sunday'
    board
  end

  step 'I ask whether it\'s Friday yet' do
    |board|
    puts '-> I ask whether it\'s Friday yet'
    # Write code here that turns the phrase above into concrete actions
    board
  end

  step 'I should be told {string}', {resources: ["kafka/topic-h"]} do
    |board, _string|
    puts '-> I should be told {string}'
    # Write code here that turns the phrase above into concrete actions
    board
  end

  step 'I have {int} cukes in my belly' do
    |board, cukes|
    p "-> I HAVE #{cukes} CUKES IN MY BELLY"
    board
  end

  step 'I wait {int} hour' do
    |board, _hours|
    puts '-> I wait {int} hour'
    # Write code here that turns the phrase above into concrete actions
    board
  end

  step 'my belly should growl' do
    |board|
    puts '-> my belly should growl'
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
  # Before do |board|
  #   board
  # end

  step 'a ruby step that fails' do |board|
    puts '=> a ruby step that fails'
    pending! # Write code here that turns the phrase above into concrete actions
    board # return the updated board
  end

  # TODO: Remove
  # step 'I have {float} cukes in my belly' do
  # |board|
  #   puts '=> I have {float} cukes in my belly'
  #   pending! # Write code here that turns the phrase above into concrete actions
  #   board # Return the updated board
  # end
  #
  # step 'I have this table' do
  #   |board|
  #   pending! # Write code here that turns the phrase above into concrete actions
  #   board # Return the updated board
  # end

  step 'the scenario should end with an error' do
    |board|
    # pending! # Write code here that turns the phrase above into concrete actions
    board # Return the updated board
  end

  # step 'a clojure step that fails' do
  #   |board|
  #   pending! # Write code here that turns the phrase above into concrete actions
  #   board # Return the updated board
  # end
  # step 'the datafied table should be' do
  # |board|
  #   pending! # Write code here that turns the phrase above into concrete actions
  #   board # Return the updated board
  # end
end
