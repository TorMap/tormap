/**
 * TorMap's service worker. It serves the app shell from the precache, so installed or returning visitors start the app
 * without waiting for the network. Data requests to the backend and map tiles are not handled here and go to the
 * network as before.
 *
 * The Vite plugin in plugins/service-worker.ts builds this file to /sw.js and replaces `self.__TORMAP_PRECACHE` with
 * the files of the build. It must not import anything that the app imports too, so the output stays a single script.
 */
import type {ServiceWorkerMessage} from "./messages";
import {deleteOtherPrecaches, installPrecache, matchPrecache, PrecacheEntry, precacheKey, precacheName} from "./precache";

declare const self: ServiceWorkerGlobalScope & { __TORMAP_PRECACHE: PrecacheEntry[] }

const entries = self.__TORMAP_PRECACHE
const keysByUrl = new Map(entries.map(entry => [entry.url, precacheKey(entry)]))
const cacheName = precacheName(entries)

self.addEventListener("install", event => {
    // `no-cache` revalidates with the server, so an outdated copy in the HTTP cache is not stored
    event.waitUntil(cacheName.then(name =>
        installPrecache(caches, name, entries, url => fetch(url, {cache: "no-cache"}))
    ))
})

self.addEventListener("activate", event => {
    event.waitUntil(cacheName
        .then(name => deleteOtherPrecaches(caches, name))
        // Control pages opened before the first install, without a reload
        .then(() => self.clients.claim()))
})

self.addEventListener("message", event => {
    // Browsers only deliver messages from pages of this origin, the check makes that explicit
    if (event.origin === self.location.origin && event.data === ("SKIP_WAITING" satisfies ServiceWorkerMessage)) {
        void self.skipWaiting()
    }
})

/**
 * Navigations to app routes get the precached index.html. Paths with a file extension, like /robots.txt, are files.
 */
function isAppNavigation(request: Request, url: URL): boolean {
    return request.mode === "navigate" && !/\.[a-z0-9]+$/i.test(url.pathname.replace(/\/index\.html$/, "/"))
}

self.addEventListener("fetch", event => {
    const request = event.request
    if (request.method !== "GET") return
    const url = new URL(request.url)
    if (url.origin !== self.location.origin) return

    const path = isAppNavigation(request, url) ? "/index.html" : url.pathname
    if (!keysByUrl.has(path)) return

    event.respondWith(cacheName
        .then(name => matchPrecache(caches, name, keysByUrl, path))
        .then(cached => cached ?? fetch(request)))
})
