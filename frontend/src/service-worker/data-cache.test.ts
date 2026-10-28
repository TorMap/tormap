import {afterEach, describe, expect, it, vi} from "vitest";

import {cachedAtHeader, DataCache, dataCacheName, DataCacheLimits, DataRoute, matchDataRoute} from "./data-cache";

// Runs in Chromium, against the browser's real Cache Storage
const api = "https://api.tormap.org"
const now = new Date("2026-10-28T12:00:00Z")
const day = 24 * 60 * 60 * 1000
const oldDate: DataRoute = {group: "date", date: "2024-01-01", strategy: "stale-while-revalidate"}
const recentDate: DataRoute = {group: "date", date: "2026-10-27", strategy: "network-first"}

const json = (body: unknown, init: ResponseInit = {}) =>
    new Response(JSON.stringify(body), {headers: {"Content-Type": "application/json"}, ...init})

const limits: DataCacheLimits = {dates: 2, dateMaxAgeMs: 60 * day, details: 2, detailsMaxAgeMs: 7 * day, networkTimeoutMs: 50}

function setup() {
    let time = now.getTime()
    const clock = {
        now: () => time,
        advance: (ms: number) => {
            time += ms
        },
    }
    const cache = new DataCache(caches, `${location.origin}/__tormap-data-index`, clock.now, limits)
    const background: Promise<unknown>[] = []
    const handle = (path: string, route: DataRoute, fetchData: (request: Request) => Promise<Response>) =>
        cache.handle(new Request(api + path), route, fetchData, promise => background.push(promise))
    const settle = () => Promise.all(background)
    return {cache, clock, handle, settle}
}

const offline = () => Promise.reject(new TypeError("Failed to fetch"))

describe("matchDataRoute", () => {
    it.each([
        ["/relay/location/days", {group: "list", strategy: "network-first"}],
        ["/relay/statistics/daily-counts", {group: "list", strategy: "network-first"}],
        ["/relay/location/day/2024-01-01", {group: "date", date: "2024-01-01", strategy: "stale-while-revalidate"}],
        ["/relay/country-usage/day/2024-01-01", {group: "date", date: "2024-01-01", strategy: "stale-while-revalidate"}],
        // The backend reprocesses the current and the previous month
        ["/relay/location/day/2026-09-01", {group: "date", date: "2026-09-01", strategy: "network-first"}],
        ["/relay/location/day/2026-10-28", {group: "date", date: "2026-10-28", strategy: "network-first"}],
        ["/relay/details/relay/42", {group: "details", strategy: "network-first"}],
        ["/relay/details/autonomous-systems/2024-01", {group: "details", strategy: "network-first"}],
    ])("caches %s", (path, route) => {
        expect(matchDataRoute(api + path, api + "/", now)).toEqual(route)
    })

    it.each([
        "https://evil.example/relay/location/days",
        `${api}/relay/location/days?cache-buster=1`,
        `${api}/relay/location/day/2024-01-01#x`,
        `${api}/relay/location/day/latest`,
        `${api}/relay/details/family/identifiers`,
        `${api}/relay/details/autonomous-systems/latest`,
        `${api}/actuator/health`,
    ])("leaves %s alone", url => {
        expect(matchDataRoute(url, api, now)).toBeUndefined()
    })
})

describe("DataCache", () => {
    afterEach(async () => {
        for (const name of await caches.keys()) await caches.delete(name)
    })

    it("answers network-first routes from the network and keeps a copy", async () => {
        const {handle} = setup()
        const response = await handle("/relay/location/days", {group: "list", strategy: "network-first"}, async () => json(["2024-01-01"]))

        expect(await response.json()).toEqual(["2024-01-01"])
        expect(response.headers.get(cachedAtHeader)).toBeNull()

        const cached = await handle("/relay/location/days", {group: "list", strategy: "network-first"}, offline)
        expect(await cached.json()).toEqual(["2024-01-01"])
        expect(cached.headers.get(cachedAtHeader)).toBe(now.toISOString())
    })

    it("answers from the cache when the network is too slow, and updates the cache in the background", async () => {
        const {handle, clock, settle} = setup()
        await handle("/relay/location/day/2026-10-27", recentDate, async () => json(["first"]))
        clock.advance(1000)

        let answer: (response: Response) => void = () => undefined
        const slow = new Promise<Response>(resolve => {
            answer = resolve
        })
        const response = await handle("/relay/location/day/2026-10-27", recentDate, () => slow)
        expect(await response.json()).toEqual(["first"])

        answer(json(["second"]))
        await settle()
        const updated = await handle("/relay/location/day/2026-10-27", recentDate, offline)
        expect(await updated.json()).toEqual(["second"])
        expect(updated.headers.get(cachedAtHeader)).toBe(new Date(now.getTime() + 1000).toISOString())
    })

    it("prefers the cached copy over an error answer", async () => {
        const {handle} = setup()
        await handle("/relay/location/days", {group: "list", strategy: "network-first"}, async () => json(["2024-01-01"]))

        const response = await handle("/relay/location/days", {group: "list", strategy: "network-first"}, async () => json({}, {status: 503}))
        expect(await response.json()).toEqual(["2024-01-01"])
    })

    it("passes the network error on without a cached copy", async () => {
        const {handle} = setup()
        await expect(handle("/relay/location/days", {group: "list", strategy: "network-first"}, offline)).rejects.toThrow("Failed to fetch")
    })

    it("answers older days from the cache at once and refreshes them for the next time", async () => {
        const {handle, settle} = setup()
        await handle("/relay/location/day/2024-01-01", oldDate, async () => json(["first"]))
        const fetchData = vi.fn(async () => json(["second"]))

        const response = await handle("/relay/location/day/2024-01-01", oldDate, fetchData)
        expect(await response.json()).toEqual(["first"])
        await settle()
        expect(fetchData).toHaveBeenCalledTimes(1)
        expect(await (await handle("/relay/location/day/2024-01-01", oldDate, offline)).json()).toEqual(["second"])
    })

    it.each([
        ["an error", json({error: true}, {status: 500})],
        ["a non JSON answer", new Response("<html></html>", {headers: {"Content-Type": "text/html"}})],
    ])("does not keep %s", async (_name, answer) => {
        const {handle, cache} = setup()
        await handle("/relay/location/day/2024-01-01", oldDate, async () => answer)
        expect((await cache.state()).dates).toEqual([])
    })

    it("keeps the most recently viewed days, with snapshot and country usage together", async () => {
        const {handle, cache, clock} = setup()
        const view = async (date: string) => {
            const route: DataRoute = {...oldDate, date}
            await handle(`/relay/location/day/${date}`, route, async () => json([date]))
            await handle(`/relay/country-usage/day/${date}`, route, async () => json([date]))
            clock.advance(1000)
        }
        await view("2024-01-01")
        await view("2024-01-02")
        // Viewing the first day again makes the second one the least recently viewed
        await handle("/relay/location/day/2024-01-01", oldDate, offline)
        clock.advance(1000)
        await view("2024-01-03")

        expect((await cache.state()).dates).toEqual(["2024-01-01", "2024-01-03"])
        const stored = await (await caches.open(dataCacheName)).keys()
        expect(stored.map(request => new URL(request.url).pathname).filter(path => path.includes("2024-01-02"))).toEqual([])
    })

    it("keeps a limited number of relay details", async () => {
        const {handle, clock} = setup()
        const details: DataRoute = {group: "details", strategy: "network-first"}
        for (const id of [1, 2, 3]) {
            await handle(`/relay/details/relay/${id}`, details, async () => json({id}))
            clock.advance(1000)
        }
        await expect(handle("/relay/details/relay/1", details, offline)).rejects.toThrow()
        expect(await (await handle("/relay/details/relay/3", details, offline)).json()).toEqual({id: 3})
    })

    it("drops days that were downloaded too long ago", async () => {
        const {handle, clock} = setup()
        await handle("/relay/location/day/2024-01-01", oldDate, async () => json(["old"]))
        clock.advance(61 * day)

        await expect(handle("/relay/location/day/2024-01-01", oldDate, offline)).rejects.toThrow()
    })

    it("deletes everything and stops caching when turned off", async () => {
        const {handle, cache} = setup()
        await handle("/relay/location/day/2024-01-01", oldDate, async () => json(["day"]))

        expect(await cache.setEnabled(false)).toEqual({enabled: false, dates: []})
        await handle("/relay/location/day/2024-01-02", {...oldDate, date: "2024-01-02"}, async () => json(["day"]))
        await expect(handle("/relay/location/day/2024-01-01", oldDate, offline)).rejects.toThrow()
        expect((await cache.state()).dates).toEqual([])

        expect(await cache.setEnabled(true)).toEqual({enabled: true, dates: []})
    })

    it("deletes the saved days on request", async () => {
        const {handle, cache} = setup()
        await handle("/relay/location/day/2024-01-01", oldDate, async () => json(["day"]))
        expect((await cache.state()).dates).toEqual(["2024-01-01"])

        expect(await cache.clear()).toEqual({enabled: true, dates: []})
    })

    it("starts over without leftovers when the index is broken", async () => {
        const {handle} = setup()
        await handle("/relay/location/day/2024-01-01", oldDate, async () => json(["day"]))
        const stored = await caches.open(dataCacheName)
        await stored.put(`${location.origin}/__tormap-data-index`, new Response("{broken"))

        const {cache} = setup()
        expect(await cache.state()).toEqual({enabled: true, dates: []})
        expect(await stored.match(`${api}/relay/location/day/2024-01-01`)).toBeUndefined()
    })
})
