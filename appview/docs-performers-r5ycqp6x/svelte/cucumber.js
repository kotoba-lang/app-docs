// Cucumber configuration for BDD E2E testing
export default {
	default: {
		require: [
			"e2e/features/step_definitions/**/*.ts",
			"e2e/features/support/**/*.ts",
		],
		format: [
			"@cucumber/pretty-formatter",
			"json:e2e/reports/cucumber-report.json",
			"html:e2e/reports/cucumber-report.html",
		],
		formatOptions: {
			colorsEnabled: true,
		},
		publishQuiet: true,
		worldParameters: {
			baseURL: process.env.DOCS_BASE_URL || "https://docs-systems.etzhayyim.com",
		},
	},
};
