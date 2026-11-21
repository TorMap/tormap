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
        ["/v1/days", {group: "list", strategy: "network-first"}],
        ["/v1/statistics/daily-counts", {group: "list", strategy: "network-first"}],
        ["/v1/days/2024-01-01/relays", {group: "date", date: "2024-01-01", strategy: "stale-while-revalidate"}],
        ["/v1/country-usage/2024-01-01", {group: "date", date: "2024-01-01", strategy: "stale-while-revalidate"}],
        // The backend reprocesses the current and the previous month
        ["/v1/days/2026-09-01/relays", {group: "date", date: "2026-09-01", strategy: "network-first"}],
        ["/v1/days/2026-10-28/relays", {group: "date", date: "2026-10-28", strategy: "network-first"}],
        ["/v1/relays/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA?month=2024-01", {group: "details", strategy: "network-first"}],
        ["/v1/autonomous-systems/2024-01", {group: "details", strategy: "network-first"}],
        ["/v1/families?month=2024-01", {group: "details", strategy: "network-first"}],
    ])("caches %s", (path, route) => {
        expect(matchDataRoute(api + path, api + "/", now)).toEqual(route)
    })

    it.each([
        "https://evil.example/v1/days",
        `${api}/v1/days?cache-buster=1`,
        `${api}/v1/days/2024-01-01/relays#x`,
        `${api}/v1/days/latest/relays`,
        `${api}/v1/relays/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA`,
        `${api}/v1/relays/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA?month=2024-01&x=1`,
        `${api}/v1/families?month=latest`,
        `${api}/v1/families/7`,
        `${api}/v1/autonomous-systems/latest`,
        // The API before v1 is not cached anymore
        `${api}/relay/location/day` + "s",
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
        const response = await handle("/v1/days", {group: "list", strategy: "network-first"}, async () => json(["2024-01-01"]))

        expect(await response.json()).toEqual(["2024-01-01"])
        expect(response.headers.get(cachedAtHeader)).toBeNull()

        const cached = await handle("/v1/days", {group: "list", strategy: "network-first"}, offline)
        expect(await cached.json()).toEqual(["2024-01-01"])
        expect(cached.headers.get(cachedAtHeader)).toBe(now.toISOString())
    })

    it("answers from the cache when the network is too slow, and updates the cache in the background", async () => {
        const {handle, clock, settle} = setup()
        await handle("/v1/days/2026-10-27/relays", recentDate, async () => json(["first"]))
        clock.advance(1000)

        let answer: (response: Response) => void = () => undefined
        const slow = new Promise<Response>(resolve => {
            answer = resolve
        })
        const response = await handle("/v1/days/2026-10-27/relays", recentDate, () => slow)
        expect(await response.json()).toEqual(["first"])

        answer(json(["second"]))
        await settle()
        const updated = await handle("/v1/days/2026-10-27/relays", recentDate, offline)
        expect(await updated.json()).toEqual(["second"])
        expect(updated.headers.get(cachedAtHeader)).toBe(new Date(now.getTime() + 1000).toISOString())
    })

    it("prefers the cached copy over an error answer", async () => {
        const {handle} = setup()
        await handle("/v1/days", {group: "list", strategy: "network-first"}, async () => json(["2024-01-01"]))

        const response = await handle("/v1/days", {group: "list", strategy: "network-first"}, async () => json({}, {status: 503}))
        expect(await response.json()).toEqual(["2024-01-01"])
    })

    it("passes the network error on without a cached copy", async () => {
        const {handle} = setup()
        await expect(handle("/v1/days", {group: "list", strategy: "network-first"}, offline)).rejects.toThrow("Failed to fetch")
    })

    it("answers older days from the cache at once and refreshes them for the next time", async () => {
        const {handle, settle} = setup()
        await handle("/v1/days/2024-01-01/relays", oldDate, async () => json(["first"]))
        const fetchData = vi.fn(async () => json(["second"]))

        const response = await handle("/v1/days/2024-01-01/relays", oldDate, fetchData)
        expect(await response.json()).toEqual(["first"])
        await settle()
        expect(fetchData).toHaveBeenCalledTimes(1)
        expect(await (await handle("/v1/days/2024-01-01/relays", oldDate, offline)).json()).toEqual(["second"])
    })

    it.each([
        ["an error", json({error: true}, {status: 500})],
        ["a non JSON answer", new Response("<html></html>", {headers: {"Content-Type": "text/html"}})],
    ])("does not keep %s", async (_name, answer) => {
        const {handle, cache} = setup()
        await handle("/v1/days/2024-01-01/relays", oldDate, async () => answer)
        expect((await cache.state()).dates).toEqual([])
    })

    it("keeps the most recently viewed days, with snapshot and country usage together", async () => {
        const {handle, cache, clock} = setup()
        const view = async (date: string) => {
            const route: DataRoute = {...oldDate, date}
            await handle(`/v1/days/${date}/relays`, route, async () => json([date]))
            await handle(`/v1/country-usage/${date}`, route, async () => json([date]))
            clock.advance(1000)
        }
        await view("2024-01-01")
        await view("2024-01-02")
        // Viewing the first day again makes the second one the least recently viewed
        await handle("/v1/days/2024-01-01/relays", oldDate, offline)
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
            await handle(`/v1/relays/${String(id).padStart(40, "0")}?month=2024-01`, details, async () => json({id}))
            clock.advance(1000)
        }
        await expect(handle(`/v1/relays/${"1".padStart(40, "0")}?month=2024-01`, details, offline)).rejects.toThrow()
        expect(await (await handle(`/v1/relays/${"3".padStart(40, "0")}?month=2024-01`, details, offline)).json()).toEqual({id: 3})
    })

    it("drops days that were downloaded too long ago", async () => {
        const {handle, clock} = setup()
        await handle("/v1/days/2024-01-01/relays", oldDate, async () => json(["old"]))
        clock.advance(61 * day)

        await expect(handle("/v1/days/2024-01-01/relays", oldDate, offline)).rejects.toThrow()
    })

    it("deletes everything and stops caching when turned off", async () => {
        const {handle, cache} = setup()
        await handle("/v1/days/2024-01-01/relays", oldDate, async () => json(["day"]))

        expect(await cache.setEnabled(false)).toEqual({enabled: false, dates: []})
        await handle("/v1/days/2024-01-02/relays", {...oldDate, date: "2024-01-02"}, async () => json(["day"]))
        await expect(handle("/v1/days/2024-01-01/relays", oldDate, offline)).rejects.toThrow()
        expect((await cache.state()).dates).toEqual([])

        expect(await cache.setEnabled(true)).toEqual({enabled: true, dates: []})
    })

    it("deletes the saved days on request", async () => {
        const {handle, cache} = setup()
        await handle("/v1/days/2024-01-01/relays", oldDate, async () => json(["day"]))
        expect((await cache.state()).dates).toEqual(["2024-01-01"])

        expect(await cache.clear()).toEqual({enabled: true, dates: []})
    })

    it("starts over without leftovers when the index is broken", async () => {
        const {handle} = setup()
        await handle("/v1/days/2024-01-01/relays", oldDate, async () => json(["day"]))
        const stored = await caches.open(dataCacheName)
        await stored.put(`${location.origin}/__tormap-data-index`, new Response("{broken"))

        const {cache} = setup()
        expect(await cache.state()).toEqual({enabled: true, dates: []})
        expect(await stored.match(`${api}/v1/days/2024-01-01/relays`)).toBeUndefined()
    })
})
