// Cucumber World object for Playwright integration
import { setWorldConstructor, World } from "@cucumber/cucumber";
import type { Browser, BrowserContext, Page } from "@playwright/test";
import { chromium, firefox, webkit } from "@playwright/test";
import path from "node:path";
import fs from "node:fs";

export interface CucumberWorldParameters {
	baseURL?: string;
	storageState?: string;
}

export class CucumberWorld extends World {
	public browser!: Browser;
	public context!: BrowserContext;
	public page!: Page;
	public baseURL: string;
	public storageState?: string;

	constructor(options: any) {
		super(options);
		this.baseURL = process.env.DOCS_BASE_URL || options.parameters?.baseURL || "http://localhost:3000";
		this.storageState = options.parameters?.storageState;
	}

	async initBrowser(browserName: "chromium" | "firefox" | "webkit" = "chromium") {
		const browserMap = {
			chromium,
			firefox,
			webkit,
		};

		this.browser = await browserMap[browserName].launch({
			headless: process.env.CI === "true" || process.env.HEADLESS === "true",
		});

		const contextOptions: Parameters<typeof this.browser.newContext>[0] = {
			baseURL: this.baseURL,
			viewport: { width: 1280, height: 720 },
		};

		if (this.storageState) {
			const storageStatePath = path.resolve(process.cwd(), this.storageState);
			try {
				if (fs.existsSync(storageStatePath)) {
					contextOptions.storageState = storageStatePath;
				} else {
					console.warn(`[CucumberWorld] Storage state file not found: ${storageStatePath}`);
				}
			} catch (err) {
				console.warn(`[CucumberWorld] Could not load storage state: ${err}`);
			}
		}

		this.context = await this.browser.newContext(contextOptions);
		this.page = await this.context.newPage();
	}

	async closeBrowser() {
		if (this.page) {
			await this.page.close();
		}
		if (this.context) {
			await this.context.close();
		}
		if (this.browser) {
			await this.browser.close();
		}
	}
}

setWorldConstructor(CucumberWorld);





