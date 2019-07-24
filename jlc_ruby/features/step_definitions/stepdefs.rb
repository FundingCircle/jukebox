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
end
