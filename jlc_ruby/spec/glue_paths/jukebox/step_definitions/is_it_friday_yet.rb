# frozen_string_literal: true

require 'jukebox'

module IsItFriday
  extend Jukebox

  def is_it_friday(day)
    day == 'Friday'
  end

  step 'today is Sunday' do |board|
    board.merge(today: 'Sunday')
  end

  step 'I ask whether it\'s Friday yet' do |board|
    board.merge(actual_answer: is_it_friday(board[:today]))
  end

  step 'I should be told {string}' do |board, expected_answer|
    expect(board[:actual_answer]).to eq(expected_answer)
  end
end
