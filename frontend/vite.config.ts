import {defineConfig, loadEnv} from 'vite'
import viteReact from "@vitejs/plugin-react";

import {discoveryFiles} from "./plugins/discovery-files";
import {liteLink} from "./plugins/lite-link";
import {securityTxtFile} from "./plugins/security-txt";
import {serviceWorker} from "./plugins/service-worker";

// https://vitejs.dev/config/
export default defineConfig(({mode}) => {
    const env = loadEnv(mode, process.cwd())
    // The public URL of this frontend, used for canonical and social media links, robots.txt, sitemap.xml and llms.txt
    const siteUrl = env.VITE_SITE_URL?.replace(/\/+$/, "")
    if (!siteUrl) throw new Error("VITE_SITE_URL is not set")
    const liteUrl = env.VITE_LITE_ENABLED === "true" && env.VITE_BACKEND_API_URL
        ? `${env.VITE_BACKEND_API_URL.replace(/\/+$/, "")}/lite/`
        : undefined
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
            securityTxtFile(),
            // Off unless VITE_LITE_ENABLED is "true" (the default in .env), for backends that do not serve /lite/
            liteLink(liteUrl),
            discoveryFiles(siteUrl, liteUrl),
        ],
        define: {
            APP_VERSION: JSON.stringify(process.env.npm_package_version),
        }
    }
})
