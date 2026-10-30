import {defineConfig, loadEnv} from 'vite'
import viteReact from "@vitejs/plugin-react";

import {liteRedirect} from "./plugins/lite-redirect";
import {serviceWorker} from "./plugins/service-worker";

// https://vitejs.dev/config/
export default defineConfig(({mode}) => {
    const env = loadEnv(mode, process.cwd())
    return {
        build: {
            outDir: 'build',
        },
        plugins: [
            viteReact(),
            serviceWorker({
                entry: "src/service-worker/sw.ts",
                // The icons that the manifest and index.html reference. Images of dialogs are loaded when shown.
                publicFiles: [
                    "manifest.json",
                    "favicon.ico",
                    "favicon-16x16.png",
                    "favicon-32x32.png",
                    "apple-touch-icon.png",
                    "android-chrome-192x192.png",
                    "android-chrome-512x512.png",
                    "maskable-icon-512x512.png",
                ],
            }),
            // Off unless VITE_LITE_ENABLED is "true", because the backend has to serve /lite/ first
            liteRedirect(env.VITE_LITE_ENABLED === "true" && env.VITE_BACKEND_API_URL
                ? `${env.VITE_BACKEND_API_URL.replace(/\/+$/, "")}/lite/`
                : undefined),
        ],
        define: {
            APP_VERSION: JSON.stringify(process.env.npm_package_version),
        }
    }
})
