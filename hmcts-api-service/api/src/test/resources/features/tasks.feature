Feature: Manage tasks
  Scenario: Create and list tasks
    When I create a task titled "Pay fine"
    Then listing tasks returns at least one task
