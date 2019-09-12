module FridayStepHelper
  def is_it_friday(day); end
end

World FridayStepHelper

Given('today is Sunday') do
  @today = 'Sunday'
end

When("I ask whether it's Friday yet") do
  @actual_answer = is_it_friday(@today)
end

Then('I should be told {string}') do |expected_answer|
  expect(@actual_answer).to eq(expected_answer)
end
