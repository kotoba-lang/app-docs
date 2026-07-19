Feature: Advanced Docs Features
  As a user
  I want to use comments, sharing, and version history
  So that I can collaborate and manage document versions

  Background:
    Given I initialize the browser
    And I am logged in to Docs Service
    And I am on the "/" page
    And I create a workspace named "Collaboration Workspace"
    And I click on "New Page"

  Scenario: Add and resolve a comment
    Given I should see "Untitled"
    When I click on "Show Comments"
    And I fill in "Add a comment..." with "This is a test comment"
    And I click on "Send"
    Then I should see "This is a test comment"
    When I click on "Resolve"
    Then I should see "This is a test comment" as resolved

  Scenario: Share a page with another user
    Given I should see "Untitled"
    When I click on "Share"
    And I fill in "Email or user ID" with "collaborator@example.com"
    And I select "Can edit" from permission
    And I click on "Invite"
    Then I should see "collaborator@example.com" in the access list

  Scenario: View version history
    Given I should see "Untitled"
    When I click on the title "Untitled"
    And I fill in the title with "Version 1"
    And I press "Enter"
    And I wait for 2 seconds
    And I click on "Show History"
    Then I should see "v1" in the version list
    And I should see "Edited by"
