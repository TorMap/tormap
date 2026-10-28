/**
 * The app shell cache of the service worker: every file of one build, stored before the worker activates so the app
 * starts without network access. Kept free of service worker globals, so tests can run it against any CacheStorage.
 */

/**
 * A file of the build. The revision is the SHA-256 hex digest of its content, written by the Vite plugin.
 */
export interface PrecacheEntry {
    url: string
    revision: string
}

export const precachePrefix = "tormap-precache-"

/**
 * One cache per build, so a failed install never touches the cache that the active worker serves from.
 * The name is derived from all revisions and changes whenever any file changes.
 */
export async function precacheName(entries: PrecacheEntry[]): Promise<string> {
    const revisions = entries.map(entry => `${entry.url} ${entry.revision}`).sort().join("\n")
    return precachePrefix + (await sha256Hex(new TextEncoder().encode(revisions))).slice(0, 16)
}

/**
 * The cache key of an entry. The revision in the key lets a new build reuse unchanged files from an older cache.
 */
export function precacheKey(entry: PrecacheEntry): string {
    return `${entry.url}?__precache=${entry.revision}`
}

export async function sha256Hex(data: BufferSource): Promise<string> {
    const digest = await crypto.subtle.digest("SHA-256", data)
    return Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, "0")).join("")
}

/**
 * Stores every entry in the cache `name`. Files that an older build already cached with the same revision are copied,
 * the rest are downloaded. A download whose content does not match its revision (e.g. a newer deployment or an
 * index.html served for a missing file) fails the whole install, and the incomplete cache is deleted.
 */
export async function installPrecache(
    cacheStorage: CacheStorage,
    name: string,
    entries: PrecacheEntry[],
    fetchFile: (url: string) => Promise<Response>,
): Promise<void> {
    const cache = await cacheStorage.open(name)
    try {
        await Promise.all(entries.map(async entry => {
            const key = precacheKey(entry)
            if (await cache.match(key)) return
            const cached = await cacheStorage.match(key)
            if (cached) {
                await cache.put(key, cached)
                return
            }
            const response = await fetchFile(entry.url)
            if (!response.ok) {
                throw new Error(`Precaching ${entry.url} failed with status ${response.status}`)
            }
            const body = await response.arrayBuffer()
            if (await sha256Hex(body) !== entry.revision) {
                throw new Error(`Precaching ${entry.url} failed: the content does not match the build`)
            }
            await cache.put(key, new Response(body, {status: response.status, headers: response.headers}))
        }))
    } catch (error) {
        await cacheStorage.delete(name)
        throw error
    }
}

/**
 * Deletes the app shell caches of other builds. Caches of other features are kept.
 */
export async function deleteOtherPrecaches(cacheStorage: CacheStorage, name: string): Promise<void> {
    const names = await cacheStorage.keys()
    await Promise.all(names
        .filter(other => other.startsWith(precachePrefix) && other !== name)
        .map(other => cacheStorage.delete(other)))
}

/**
 * Looks up a file of the build by its path, e.g. `/assets/index-abc.js`.
 */
export async function matchPrecache(
    cacheStorage: CacheStorage,
    name: string,
    keysByUrl: Map<string, string>,
    url: string,
): Promise<Response | undefined> {
    const key = keysByUrl.get(url)
    if (key === undefined) return undefined
    const cache = await cacheStorage.open(name)
    return cache.match(key)
}
