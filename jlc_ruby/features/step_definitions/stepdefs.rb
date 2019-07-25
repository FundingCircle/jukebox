# frozen_string_literal: true

require_relative '../../lib/jukebox'

module Tests
  extend Jukebox

  Step('today is Sunday') do |board|
    # Write code here that turns the phrase above into concrete actions
    board
  end

  Step('I ask whether it\'s Friday yet') do |board|
    # Write code here that turns the phrase above into concrete actions
    board
  end

  Step('I should be told {string}') do |board, string|
    # Write code here that turns the phrase above into concrete actions
    board
  end

  Step('I wait {int} hour') do |board, hours|
    # Write code here that turns the phrase above into concrete actions
    p "Inside step - I wait {int} hour: board: #{board}, hours: #{hours}"
    board
  end

  Step('my belly should growl') do |board|
    # Write code here that turns the phrase above into concrete actions
    board
  end
end
