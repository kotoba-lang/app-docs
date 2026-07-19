Feature: Block and Page Editing
  As a user
  I want to edit pages and blocks seamlessly
  So that I can maintain my documents efficiently

  Background:
    Given I initialize the browser
    And I am logged in to Docs Service

  Scenario: Edit page title
    Given I am on the "/" page
    And I create a workspace named "Edit Test Workspace"
    And I click on "New Page"
    Then I should see "Untitled"
    When I click on the title "Untitled"
    And I fill in the title with "My New Title"
    And I press "Enter"
    Then I should see "My New Title"
    And the page title should be saved as "My New Title"

  Scenario: Edit block content and save
    Given I am on a new page in workspace "Block Edit Workspace"
    When I type "Hello World" into the first block
    And I wait for 2 seconds
    And I reload the page
    Then I should see "Hello World" in the first block

  Scenario: Clean up empty blocks
    Given I am on a new page in workspace "Cleanup Workspace"
    When I click on "+ Add a block"
    And I click on "+ Add a block"
    Then I should see multiple blocks
    When I type "Some content" into the second block
    And I delete the first empty block
    Then the first block should contain "Some content"

  Scenario: Verify no unnecessary icons in body
    Given I am on a new page in workspace "Icon Test Workspace"
    Then I should not see a large document icon above the title
