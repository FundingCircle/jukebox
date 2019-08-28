# frozen_string_literal: true

require 'jukebox'
require 'watir'
require 'webdrivers'

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
    p "Inside step - I wait {int} hour: board: #{board}, hours:A #{hours}"
    browser = Watir::Browser.new
    browser.goto( 'https://duckduckgo.com')
    browser.text_field(id: 'search_form_input_homepage').set 'Funding Circle'
    browser.send_keys :enter
    sleep 3
    browser.close
    board
  end

  step 'my belly should growl' do |board|
    # Write code here that turns the phrase above into concrete actions
    board
  end
end
