// Checks the service worker of the production build in Chromium: the app shell starts offline after one visit, and a
// new version is offered instead of being activated. Run after `yarn build`: `yarn smoke:pwa`.
// The backend and map tiles are answered by the test, so no network access is needed.
import {spawn} from "node:child_process";
import {appendFileSync, cpSync, mkdtempSync, rmSync} from "node:fs";
import {tmpdir} from "node:os";
import path from "node:path";

import {chromium} from "playwright";

const port = 4175
const origin = `http://127.0.0.1:${port}`
const updateMessage = "A new version of TorMap is available."
const offlineMessage = "You are offline. The map loads once you are back online."

// A copy of the build, so the update check can change sw.js without touching build/
const root = mkdtempSync(path.join(tmpdir(), "tormap-pwa-"))
cpSync("build", root, {recursive: true})

const server = spawn("yarn", ["vite", "preview", "--outDir", root, "--host", "127.0.0.1", "--port", String(port), "--strictPort"], {
    stdio: "ignore",
})
const browser = await chromium.launch()

async function waitForServer() {
    for (let attempt = 0; attempt < 60; attempt++) {
        try {
            if ((await fetch(origin)).ok) return
        } catch {
            // Not started yet
        }
        await new Promise(resolve => setTimeout(resolve, 500))
    }
    throw new Error(`Preview server did not start at ${origin}`)
}

function step(name) {
    console.log(`pwa smoke: ${name}`)
}

try {
    await waitForServer()
    const context = await browser.newContext()
    const answerBackend = route => {
        const pathname = new URL(route.request().url()).pathname
        const body = pathname === "/relay/location/days" ? ["2024-01-01"] : []
        return route.fulfill({json: body, headers: {"Access-Control-Allow-Origin": "*"}})
    }
    await context.route("https://api.tormap.org/**", answerBackend)
    await context.route("https://basemaps.cartocdn.com/**", route => route.abort())
    const page = await context.newPage()

    step("first visit installs the worker")
    await page.goto(origin)
    await page.waitForFunction(() => navigator.serviceWorker.controller !== null, null, {timeout: 30000})
    const cacheNames = await page.evaluate(() => caches.keys())
    if (!cacheNames.some(name => name.startsWith("tormap-precache-"))) {
        throw new Error(`No app shell cache, found: ${cacheNames.join(", ")}`)
    }

    step("the app starts offline")
    await context.unroute("https://api.tormap.org/**")
    await context.setOffline(true)
    await page.reload()
    await page.locator(".leaflet-container").waitFor()
    await page.getByText(offlineMessage).waitFor()

    step("a new version is offered, not activated")
    await context.setOffline(false)
    await context.route("https://api.tormap.org/**", answerBackend)
    appendFileSync(path.join(root, "sw.js"), "\n// next version\n")
    await page.reload()
    await page.getByText(updateMessage).waitFor({timeout: 30000})

    step("reloading activates it")
    await Promise.all([
        page.waitForEvent("framenavigated"),
        page.getByRole("button", {name: "Reload"}).click(),
    ])
    await page.locator(".leaflet-container").waitFor()
    const waiting = await page.evaluate(async () => (await navigator.serviceWorker.getRegistration())?.waiting ?? null)
    if (waiting !== null) throw new Error("The new version is still waiting after the reload")

    step("passed")
} finally {
    await browser.close()
    server.kill()
    rmSync(root, {recursive: true, force: true})
}
