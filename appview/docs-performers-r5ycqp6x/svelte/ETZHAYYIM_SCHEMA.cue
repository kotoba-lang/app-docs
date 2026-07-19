package etzhayyim

#BuildStep: {
	name:  string
	cmd:   string
	image: string | *""
}

#Deployment: {
	type:      "kustomize" | "helm" | "raw" | "static"
	path:      string | *"./manifests/overlays/production"
	namespace: string | *"default"
}

#AppConfig: {
	domain:     string
	framework:  "svelte" | "next" | "static"
	output_dir: string | *"build"
}

#Project: {
	nanoid: string
	name:   string
	type:   "service" | "system" | "app" | "personType"
	app?:   #AppConfig
	build: {
		steps: [...#BuildStep]
		output_image?: string
	}
	deploy: #Deployment
}
