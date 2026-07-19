Feature: Docs Management
  As a user
  I want to manage documents
  So that I can create, edit, and organize documents

  Background:
    Given I initialize the browser
    And I am logged in to Docs Service

  Scenario: View workspace list
    Given I am on the "/" page
    Then I should see "Workspaces"

  Scenario: Create a new workspace
    Given I am on the "/" page
    When I click on "New Workspace"
    And I fill in "Name" with "BDD Workspace"
    And I click on "Create"
    Then I should see "Workspace created successfully"

  Scenario: Create a new page
    Given I am on the "/" page
    And I create a workspace named "Workspace for Page"
    When I click on "New Page"
    Then I should see "Untitled"
