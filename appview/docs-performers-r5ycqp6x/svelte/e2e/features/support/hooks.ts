// Cucumber hooks for test lifecycle management
import { Before, After, BeforeAll, AfterAll, setDefaultTimeout } from "@cucumber/cucumber";
import type { CucumberWorld } from "./world";

// Set default timeout to 30 seconds
setDefaultTimeout(30 * 1000);

BeforeAll(async function () {
	console.log("[Cucumber] Starting Docs Service BDD E2E test suite");
});

Before(async function (this: CucumberWorld) {
	// Initialize browser before each scenario
	await this.initBrowser("chromium");
});

After(async function (this: CucumberWorld) {
	// Clean up browser after each scenario
	await this.closeBrowser();
});

AfterAll(async function () {
	console.log("[Cucumber] Completed Docs Service BDD E2E test suite");
});





