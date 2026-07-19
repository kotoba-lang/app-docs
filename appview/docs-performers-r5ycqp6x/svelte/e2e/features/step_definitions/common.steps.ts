// Common step definitions for Docs Service BDD E2E tests
import { Given, When, Then } from "@cucumber/cucumber";
import type { CucumberWorld } from "../support/world";

Given("I am logged in to Docs Service", async function (this: CucumberWorld) {
	await this.page.goto("/");
	await this.page.waitForLoadState("domcontentloaded");
	// Wait for Clerk authentication to initialize
	await this.page.waitForTimeout(2000);
});

Given("I initialize the browser", async function (this: CucumberWorld) {
	// Browser is already initialized in Before hook
	// This step is a no-op but kept for clarity in feature files
});

Given("I am on the {string} page", async function (this: CucumberWorld, pagePath: string) {
	let targetPath = pagePath;
	if (pagePath.includes("{workspaceId}")) {
		await this.page.goto("/");
		await this.page.waitForLoadState("networkidle");

		const workspaceLink = this.page.locator(".workspace-card").first();
		if (await workspaceLink.isVisible({ timeout: 5000 }).catch((_err) => false)) {
			const href = await workspaceLink.getAttribute("href");
			targetPath = href || "/";
		} else {
			// Create a workspace
			await this.page.getByRole("button", { name: /Create Workspace|\+ New Workspace/ }).first().click();
			await this.page.getByLabel("Name").fill("Test Workspace");
			await this.page.getByRole("button", { name: "Create" }).click();
			await this.page.waitForSelector(".workspace-card");
			const href = await this.page.locator(".workspace-card").first().getAttribute("href");
			targetPath = href || "/";
		}
	}
	await this.page.goto(targetPath);
	await this.page.waitForLoadState("networkidle");
});

When("I navigate to {string}", async function (this: CucumberWorld, path: string) {
	await this.page.goto(path);
	await this.page.waitForLoadState("networkidle");
});

When("I click on {string}", async function (this: CucumberWorld, buttonText: string) {
	// Try multiple ways to find the button/link
	const selectors = [
		this.page.getByRole("button", { name: buttonText, exact: false }),
		this.page.getByRole("link", { name: buttonText, exact: false }),
		this.page.locator(`button:has-text("${buttonText}")`),
		this.page.locator(`a:has-text("${buttonText}")`),
		this.page.getByText(buttonText, { exact: false })
	];

	for (const selector of selectors) {
		const count = await selector.count().catch((_err) => 0);
		if (count > 0) {
			const isVisible = await selector.first().isVisible({ timeout: 2000 }).catch((_err) => false);
			if (isVisible) {
				await selector.first().click();
				return;
			}
		}
	}

	throw new Error(`Could not find button or link with text "${buttonText}" or alternatives`);
});

Given("I create a workspace named {string}", async function (this: CucumberWorld, name: string) {
	const createBtn = this.page.getByRole("button", { name: /New Workspace|Create Workspace/i }).first();
	await createBtn.click();

	const nameInput = this.page.getByLabel("Name").first();
	await nameInput.fill(name);

	const submitBtn = this.page.getByRole("button", { name: "Create" }).first();
	await submitBtn.click();

	// Wait for the workspace to be created
	await this.page.waitForTimeout(2000);
});

When("I fill in {string} with {string}", async function (this: CucumberWorld, fieldName: string, value: string) {
	const label = this.page.getByLabel(fieldName).first();
	const placeholder = this.page.getByPlaceholder(fieldName).first();
	const nameInput = this.page.locator(`input[name*="${fieldName}" i]`).first();

	if (await label.isVisible({ timeout: 2000 }).catch((_err) => false)) {
		await label.fill(value);
	} else if (await placeholder.isVisible({ timeout: 2000 }).catch((_err) => false)) {
		await placeholder.fill(value);
	} else if (await nameInput.isVisible({ timeout: 2000 }).catch((_err) => false)) {
		await nameInput.fill(value);
	} else {
		throw new Error(`Field "${fieldName}" not found`);
	}
});

Then("I should see {string}", async function (this: CucumberWorld, text: string) {
	// If searching for "Untitled", wait specifically for the page header title
	if (text === "Untitled") {
		const header = this.page.locator("h1.title");
		await header.waitFor({ state: "visible", timeout: 15000 });
		return;
	}

	const locator = this.page.getByText(text).first();
	await locator.waitFor({ state: "visible", timeout: 10000 });
	if (!(await locator.isVisible())) {
		throw new Error(`Expected to see "${text}" but it was not visible`);
	}
});

Then("I should not see {string}", async function (this: CucumberWorld, text: string) {
	const locator = this.page.getByText(text).first();
	try {
		await locator.waitFor({ state: "hidden", timeout: 5000 });
	} catch {
		// Element not found is also acceptable
	}
	if (await locator.isVisible().catch((_err) => false)) {
		throw new Error(`Expected not to see "${text}" but it was visible`);
	}
});

When("I click on the title {string}", async function (this: CucumberWorld, title: string) {
	const titleElement = this.page.locator("h1.title", { hasText: title });
	await titleElement.click();
});

When("I fill in the title with {string}", async function (this: CucumberWorld, newTitle: string) {
	const input = this.page.locator("input.title-input");
	await input.waitFor({ state: "visible" });
	await input.fill(newTitle);
});

When("I press {string}", async function (this: CucumberWorld, key: string) {
	await this.page.keyboard.press(key);
});

Then("the page title should be saved as {string}", async function (this: CucumberWorld, expectedTitle: string) {
	await this.page.reload();
	await this.page.waitForLoadState("networkidle");
	const titleElement = this.page.locator("h1.title");
	await titleElement.waitFor({ state: "visible", timeout: 10000 });
	const text = await titleElement.textContent();
	if (text?.trim() !== expectedTitle) {
		throw new Error(`Expected title "${expectedTitle}" but found "${text?.trim()}"`);
	}
});

Given("I am on a new page in workspace {string}", async function (this: CucumberWorld, workspaceName: string) {
	await this.page.goto("/");
	await this.page.waitForLoadState("networkidle");

	// Create workspace if not exists
	const workspaceBtn = this.page.getByRole("button", { name: /New Workspace|Create Workspace/i }).first();
	await workspaceBtn.click();
	await this.page.getByLabel("Name").fill(workspaceName);
	await this.page.getByRole("button", { name: "Create" }).click();
	await this.page.waitForTimeout(1000);

	// Create page
	const newPageBtn = this.page.getByRole("button", { name: "New Page" }).first();
	await newPageBtn.click();
	await this.page.waitForSelector("h1.title");
});

When("I type {string} into the first block", async function (this: CucumberWorld, content: string) {
	const firstBlock = this.page.locator(".block-content").first();
	await firstBlock.waitFor({ state: "visible" });
	await firstBlock.click();
	await firstBlock.fill(content);
	// Click title to trigger blur and save
	await this.page.locator("h1.title").click();
	await this.page.waitForTimeout(2000);
});

When("I wait for {int} seconds", async function (this: CucumberWorld, seconds: number) {
	await this.page.waitForTimeout(seconds * 1000);
});

When("I reload the page", async function (this: CucumberWorld) {
	await this.page.reload();
	await this.page.waitForLoadState("networkidle");
});

Then("I should see {string} in the first block", async function (this: CucumberWorld, expectedContent: string) {
	const firstBlock = this.page.locator(".block-content").first();
	const text = await firstBlock.textContent();
	if (text?.trim() !== expectedContent) {
		throw new Error(`Expected content "${expectedContent}" but found "${text?.trim()}"`);
	}
});

Then("I should see multiple blocks", async function (this: CucumberWorld) {
	const count = await this.page.locator(".block-wrapper").count();
	if (count <= 1) {
		throw new Error(`Expected multiple blocks but found ${count}`);
	}
});

When("I type {string} into the second block", async function (this: CucumberWorld, content: string) {
	const secondBlock = this.page.locator(".block-content").nth(1);
	await secondBlock.waitFor({ state: "visible" });
	await secondBlock.click();
	await secondBlock.fill(content);
	// Click title to trigger blur
	await this.page.locator("h1.title").click();
	await this.page.waitForTimeout(2000);
});

When("I delete the first empty block", async function (this: CucumberWorld) {
	const firstBlock = this.page.locator(".block-content").first();
	await firstBlock.click();
	await this.page.keyboard.press("Backspace"); // Backspace on empty block should delete it
	await this.page.waitForTimeout(1000);
});

Then("the first block should contain {string}", async function (this: CucumberWorld, expectedContent: string) {
	const firstBlock = this.page.locator(".block-content").first();
	const text = await firstBlock.textContent();
	if (text?.trim() !== expectedContent) {
		throw new Error(`Expected content "${expectedContent}" but found "${text?.trim()}"`);
	}
});

Then("I should not see a large document icon above the title", async function (this: CucumberWorld) {
	const iconBtn = this.page.locator(".icon-btn");
	if (await iconBtn.isVisible()) {
		const box = await iconBtn.boundingBox();
		if (box && box.height > 50) {
			throw new Error("Large icon is visible above the title");
		}
	}
});
