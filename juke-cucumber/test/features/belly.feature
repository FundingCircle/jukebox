Feature: Belly

  Scenario: a few cukes
    Given I have 42 cukes in my belly
    When I wait 1 hour
    Then my belly should growl

  Scenario: support data tables
    Given I have this table
      | col1 | col2 |
      | 1    | "2"  |
    Then the datafied table should be ({:col1 1, :col2 "2"})
