package etzhayyim

project: #Project & {
	nanoid: "dvf92zap"
	name:   "docs-app-\(nanoid)"
	type:   "app"
	app: {
		domain:     "docs.apps.etzhayyim.ai"
		framework:  "svelte"
		output_dir: "build"
	}
	build: {
		steps: [
			{ name: "Install", cmd: "pnpm install" },
			{ name: "Build", cmd: "pnpm run build" }
		]
	}
	deploy: {
		type:      "static"
		namespace: "default"
	}
}
