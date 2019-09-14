Feature: Belly

  @success
  Scenario: a few cukes
    Given I have 42 cukes in my belly
    When I wait 1 hour
    Then my belly should growl

  @success
  Scenario: support data tables
    Given I have this table
      | col1 | col2 |
      | 1    | "2"  |
      | 1.2  | yeah |
    Then the datafied table should be
    """
    ({:col1 1, :col2 "2"} {:col1 1.2, :col2 "yeah"})
    """

  @failure-in-ruby
  Scenario: A failing scenario (ruby)
    Given a ruby step that fails
    Then the scenario should end with an error

  @failure-in-clojure
  Scenario: A failing scenario (clojure)
    Given a clojure step that fails
    Then the scenario should end with an error
