import {defineConfig} from 'vite'
import viteChecker from 'vite-plugin-checker'
import viteReact from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig({
    build: {
        outDir: 'build',
    },
    plugins: [
        viteReact(),
        viteChecker({ typescript: true }),
    ],
    define: {
        APP_VERSION: JSON.stringify(process.env.npm_package_version),
    }
})