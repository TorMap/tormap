import {defineConfig} from 'vite'
import reactRefresh from '@vitejs/plugin-react-refresh'
import checker from 'vite-plugin-checker'

// https://vitejs.dev/config/
export default defineConfig({
    build: {
        outDir: 'build',
    },
    plugins: [
        reactRefresh(),
        checker({ typescript: true }),
    ],
    define: {
        APP_VERSION: JSON.stringify(process.env.npm_package_version),
    }
})
