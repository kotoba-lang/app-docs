// Advanced feature step definitions for Docs Service
import { When, Then } from "@cucumber/cucumber";
import type { CucumberWorld } from "../support/world";
import { expect } from "@playwright/test";

Then("I should see {string} as resolved", async function (this: CucumberWorld, text: string) {
	const locator = this.page.locator(".comment-item.resolved", { hasText: text }).first();
	await locator.waitFor({ state: "visible", timeout: 5000 });
	const isVisible = await locator.isVisible();
	if (!isVisible) {
		throw new Error(`Expected comment "${text}" to be resolved and visible`);
	}
});

When("I select {string} from permission", async function (this: CucumberWorld, permission: string) {
	const select = this.page.locator("select").first();
	await select.selectOption({ label: permission });
});

Then("I should see {string} in the access list", async function (this: CucumberWorld, email: string) {
	const locator = this.page.locator(".share-item", { hasText: email }).first();
	await locator.waitFor({ state: "visible", timeout: 5000 });
	const isVisible = await locator.isVisible();
	if (!isVisible) {
		throw new Error(`Expected "${email}" to be in the access list`);
	}
});

Then("I should see {string} in the version list", async function (this: CucumberWorld, versionText: string) {
	const locator = this.page.locator(".version-num", { hasText: versionText }).first();
	await locator.waitFor({ state: "visible", timeout: 5000 });
	const isVisible = await locator.isVisible();
	if (!isVisible) {
		throw new Error(`Expected version "${versionText}" to be in the version list`);
	}
});

Then("I should see a new paragraph block", async function (this: CucumberWorld) {
	const blocks = this.page.locator(".block-content.block-paragraph");
	const count = await blocks.count();
	if (count === 0) {
		throw new Error("No paragraph blocks found");
	}
});
