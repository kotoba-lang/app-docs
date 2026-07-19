Feature: Block Management
  As a user
  I want to manage blocks on a page
  So that I can create and edit content

  Background:
    Given I initialize the browser
    And I am logged in to Docs Service

  Scenario: Add a new block to a page
    Given I am on the "/" page
    And I create a workspace named "Block Test Workspace"
    And I click on "New Page"
    Then I should see "Untitled"
    When I click on "+ Add a block"
    Then I should see a new paragraph block
