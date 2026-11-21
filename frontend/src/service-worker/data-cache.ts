/**
 * The relay data cache of the service worker: recently viewed days and the lists the app needs to start, so they open
 * without network access. Only GET requests to known backend paths without query strings are cached, which bounds the
 * possible keys, and every group has a limit on entries and age. Kept free of service worker globals for tests.
 */
import type {CachedAtHeader, DataCacheState} from "./messages";

export const dataCacheName = "tormap-data-v1"

/**
 * Set on responses served from the cache, with the time the data was downloaded (ISO 8601)
 */
export const cachedAtHeader: CachedAtHeader = "X-TorMap-Cached-At"

export type DataGroup = "list" | "date" | "details"
export type Strategy = "network-first" | "stale-while-revalidate"

export interface DataRoute {
    group: DataGroup
    /** The day of a `date` route (YYYY-MM-DD), its snapshot and country usage are kept and dropped together */
    date?: string
    strategy: Strategy
}

export interface DataCacheLimits {
    /** Days kept, the least recently viewed is dropped first */
    dates: number
    dateMaxAgeMs: number
    details: number
    detailsMaxAgeMs: number
    /** Network-first waits this long before it answers from the cache */
    networkTimeoutMs: number
}

const day = 24 * 60 * 60 * 1000

export const defaultDataCacheLimits: DataCacheLimits = {
    dates: 20,
    dateMaxAgeMs: 60 * day,
    details: 100,
    detailsMaxAgeMs: 7 * day,
    networkTimeoutMs: 3000,
}

interface IndexEntry {
    group: DataGroup
    date?: string
    cachedAt: number
    usedAt: number
}

interface DataIndex {
    enabled: boolean
    entries: Record<string, IndexEntry>
}

const datePathPattern = /^\/relay\/(?:location|country-usage)\/day\/(\d{4}-\d{2}-\d{2})$/
const detailsPathPattern = /^\/relay\/details\/(?:relay\/\d+|autonomous-systems\/\d{4}-\d{2})$/
const listPaths = ["/relay/location/days", "/relay/statistics/daily-counts"]

/**
 * Whether a day is in the current or previous month. The backend reprocesses whole months when new descriptors
 * arrive, so these days may still change and are always requested from the network first.
 */
function isRecentDate(date: string, now: Date): boolean {
    const previousMonth = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - 1, 1))
    return date >= previousMonth.toISOString().slice(0, 7)
}

/**
 * Finds how a request to the backend at `apiBase` (e.g. `https://api.tormap.org`) is cached, undefined if it is not.
 */
export function matchDataRoute(url: string, apiBase: string, now: Date): DataRoute | undefined {
    const base = apiBase.replace(/\/+$/, "")
    if (!url.startsWith(base + "/")) return undefined
    const path = url.slice(base.length)
    // Query strings and fragments would allow unbounded keys
    if (/[?#]/.test(path)) return undefined
    if (listPaths.includes(path)) return {group: "list", strategy: "network-first"}
    const date = datePathPattern.exec(path)?.[1]
    if (date) return {group: "date", date, strategy: isRecentDate(date, now) ? "network-first" : "stale-while-revalidate"}
    if (detailsPathPattern.test(path)) return {group: "details", strategy: "network-first"}
    return undefined
}

/**
 * Only complete JSON answers are kept, never errors or what a proxy answers instead
 */
function isCacheable(response: Response): boolean {
    return response.status === 200 && (response.headers.get("Content-Type") ?? "").includes("application/json")
}

export class DataCache {
    // Index changes run one after another, so concurrent requests do not overwrite each other's entries
    private queue: Promise<unknown> = Promise.resolve()

    constructor(
        private readonly cacheStorage: CacheStorage,
        /** A URL of the worker's origin, under which the index of the cache is stored */
        private readonly indexUrl: string,
        private readonly now: () => number = Date.now,
        private readonly limits: DataCacheLimits = defaultDataCacheLimits,
    ) {
    }

    /**
     * Answers a request of the route. Work that continues after the answer, like updating the cache in the background,
     * is passed to `waitUntil`, so the worker is kept alive for it.
     */
    async handle(
        request: Request,
        route: DataRoute,
        fetchData: (request: Request) => Promise<Response>,
        waitUntil: (promise: Promise<unknown>) => void,
    ): Promise<Response> {
        if (!(await this.state()).enabled) return fetchData(request)

        const url = request.url
        const network = fetchData(request).then(async response => {
            if (isCacheable(response)) await this.store(url, route, response.clone())
            return response
        })
        // Settled in every case, so a failed background update is not reported as an unhandled rejection
        const networkSettled = network.then(() => undefined, () => undefined)

        if (route.strategy === "stale-while-revalidate") {
            const cached = await this.lookup(url)
            if (cached) {
                waitUntil(networkSettled)
                return cached
            }
            return network
        }

        let timer: ReturnType<typeof setTimeout> | undefined
        const timeout = new Promise<undefined>(resolve => {
            timer = setTimeout(() => resolve(undefined), this.limits.networkTimeoutMs)
        })
        const first = await Promise.race([network.catch(() => undefined), timeout])
        clearTimeout(timer)
        if (first?.ok) return first
        // A slow network, a failed request or an error answer, e.g. while the backend is being upgraded
        const cached = await this.lookup(url)
        if (cached) {
            waitUntil(networkSettled)
            return cached
        }
        // Waits for a slow network, or passes its answer or error on
        return network
    }

    async state(): Promise<DataCacheState> {
        return this.withIndex(async index => ({
            enabled: index.enabled,
            dates: [...new Set(Object.values(index.entries).flatMap(entry => entry.date ? [entry.date] : []))].sort(),
        }), false)
    }

    /**
     * Turning caching off deletes everything cached
     */
    async setEnabled(enabled: boolean): Promise<DataCacheState> {
        await this.withIndex(async index => {
            if (!enabled) await this.deleteEntries(index, Object.keys(index.entries))
            index.enabled = enabled
        })
        return this.state()
    }

    async clear(): Promise<DataCacheState> {
        await this.withIndex(index => this.deleteEntries(index, Object.keys(index.entries)))
        return this.state()
    }

    private async lookup(url: string): Promise<Response | undefined> {
        return this.withIndex(async (index, cache) => {
            const entry = index.entries[url]
            if (!entry) return undefined
            const cached = await cache.match(url)
            if (!cached || this.isExpired(entry)) {
                await this.deleteEntries(index, [url])
                return undefined
            }
            entry.usedAt = this.now()
            const headers = new Headers(cached.headers)
            headers.set(cachedAtHeader, new Date(entry.cachedAt).toISOString())
            return new Response(cached.body, {status: cached.status, statusText: cached.statusText, headers})
        })
    }

    private async store(url: string, route: DataRoute, response: Response) {
        const body = await response.arrayBuffer()
        await this.withIndex(async (index, cache) => {
            // Caching may have been turned off while the request was running
            if (!index.enabled) return
            await cache.put(url, new Response(body, {headers: {"Content-Type": response.headers.get("Content-Type") ?? "application/json"}}))
            const now = this.now()
            index.entries[url] = {group: route.group, date: route.date, cachedAt: now, usedAt: now}
            await this.evict(index)
        })
    }

    private isExpired(entry: IndexEntry): boolean {
        const age = this.now() - entry.cachedAt
        if (entry.group === "date") return age > this.limits.dateMaxAgeMs
        if (entry.group === "details") return age > this.limits.detailsMaxAgeMs
        return false
    }

    private async evict(index: DataIndex) {
        const entries = Object.entries(index.entries)
        const evicted = new Set(entries.filter(([, entry]) => this.isExpired(entry)).map(([url]) => url))

        // A day counts as viewed when its snapshot or its country usage was last used
        const lastUsedByDate = new Map<string, number>()
        entries.forEach(([, entry]) => {
            if (entry.date) lastUsedByDate.set(entry.date, Math.max(entry.usedAt, lastUsedByDate.get(entry.date) ?? 0))
        })
        const keptDates = new Set([...lastUsedByDate].sort((a, b) => b[1] - a[1]).slice(0, this.limits.dates).map(([date]) => date))
        entries.forEach(([url, entry]) => {
            if (entry.date && !keptDates.has(entry.date)) evicted.add(url)
        })

        entries.filter(([, entry]) => entry.group === "details")
            .sort((a, b) => b[1].usedAt - a[1].usedAt)
            .slice(this.limits.details)
            .forEach(([url]) => evicted.add(url))

        await this.deleteEntries(index, [...evicted])
    }

    private async deleteEntries(index: DataIndex, urls: string[]) {
        const cache = await this.cacheStorage.open(dataCacheName)
        await Promise.all(urls.map(url => cache.delete(url)))
        urls.forEach(url => delete index.entries[url])
    }

    /**
     * Runs `update` with the index and saves the index afterwards, unless `save` is false
     */
    private withIndex<T>(update: (index: DataIndex, cache: Cache) => Promise<T>, save = true): Promise<T> {
        const run = this.queue.then(async () => {
            const cache = await this.cacheStorage.open(dataCacheName)
            const index = await this.readIndex(cache)
            const result = await update(index, cache)
            if (save) await cache.put(this.indexUrl, new Response(JSON.stringify(index), {headers: {"Content-Type": "application/json"}}))
            return result
        })
        this.queue = run.catch(() => undefined)
        return run
    }

    private async readIndex(cache: Cache): Promise<DataIndex> {
        const stored = await cache.match(this.indexUrl)
        if (!stored) return {enabled: true, entries: {}}
        try {
            const index = await stored.json() as DataIndex
            if (typeof index?.enabled === "boolean" && typeof index.entries === "object" && index.entries !== null) return index
        } catch {
            // Handled below
        }
        // Without a valid index, the cached responses could never be evicted, so they are deleted
        await Promise.all((await cache.keys()).map(request => cache.delete(request)))
        return {enabled: true, entries: {}}
    }
}
