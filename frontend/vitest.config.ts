import {playwright} from "@vitest/browser-playwright";
import viteReact from "@vitejs/plugin-react-swc";
import {defineConfig} from "vitest/config";

/*
 Two projects:
 - unit: pure logic in plain Node (*.unit.test.ts), no DOM.
 - components: everything that needs a DOM or Leaflet, run in real headless Chromium (*.test.ts, *.test.tsx).
 vite.config.ts is intentionally not reused, so the type checker plugin does not run alongside tests.
 */
export default defineConfig({
    plugins: [viteReact()],
    define: {
        APP_VERSION: JSON.stringify("test"),
    },
    test: {
        coverage: {
            provider: "v8",
            include: ["src/**/*.{ts,tsx}"],
            exclude: [
                "src/**/*.test.{ts,tsx}",
                "src/**/*.d.ts",
                "src/test/**",
                "src/index.tsx",
                "src/types/**/*.d.ts",
                "src/resources/**",
                "src/fonts/**",
            ],
            reporter: ["text-summary", "html", "lcov"],
        },
        projects: [
            {
                extends: true,
                test: {
                    name: "unit",
                    environment: "node",
                    include: ["src/**/*.unit.test.ts"],
                },
            },
            {
                extends: true,
                test: {
                    name: "components",
                    include: ["src/**/*.test.{ts,tsx}"],
                    exclude: ["src/**/*.unit.test.ts"],
                    setupFiles: ["./src/test/setup.ts"],
                    browser: {
                        enabled: true,
                        headless: true,
                        provider: playwright(),
                        instances: [{browser: "chromium"}],
                    },
                },
            },
        ],
    },
});
