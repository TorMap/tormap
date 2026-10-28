import {defineConfig} from 'vite'
import viteReact from "@vitejs/plugin-react";

import {serviceWorker} from "./plugins/service-worker";

// https://vitejs.dev/config/
export default defineConfig({
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
    ],
    define: {
        APP_VERSION: JSON.stringify(process.env.npm_package_version),
    }
})
