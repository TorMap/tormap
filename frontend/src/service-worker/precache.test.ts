import {afterEach, describe, expect, it, vi} from "vitest";

import {
    deleteOtherPrecaches,
    installPrecache,
    matchPrecache,
    PrecacheEntry,
    precacheKey,
    precacheName,
    sha256Hex
} from "./precache";

// Runs in Chromium, against the browser's real Cache Storage
const files: Record<string, string> = {
    "/index.html": "<html>v1</html>",
    "/assets/index-abc.js": "console.log(1)",
}

async function entriesFor(contents: Record<string, string>): Promise<PrecacheEntry[]> {
    return Promise.all(Object.entries(contents).map(async ([url, content]) =>
        ({url, revision: await sha256Hex(new TextEncoder().encode(content))})))
}

function serve(contents: Record<string, string>) {
    return vi.fn(async (url: string) => url in contents
        ? new Response(contents[url], {headers: {"Content-Type": "text/plain"}})
        : new Response("<html>fallback</html>", {status: 404}))
}

describe("precache", () => {
    afterEach(async () => {
        for (const name of await caches.keys()) await caches.delete(name)
    })

    it("stores every file and serves it by path", async () => {
        const entries = await entriesFor(files)
        const name = await precacheName(entries)
        const fetchFile = serve(files)
        await installPrecache(caches, name, entries, fetchFile)

        const keysByUrl = new Map(entries.map(entry => [entry.url, precacheKey(entry)]))
        const cached = await matchPrecache(caches, name, keysByUrl, "/assets/index-abc.js")
        expect(await cached?.text()).toBe("console.log(1)")
        expect(cached?.headers.get("Content-Type")).toBe("text/plain")
        expect(await matchPrecache(caches, name, keysByUrl, "/unknown.js")).toBeUndefined()
        expect(fetchFile).toHaveBeenCalledTimes(2)
    })

    it("names the cache after the content of the build", async () => {
        const name = await precacheName(await entriesFor(files))
        expect(name).toMatch(/^tormap-precache-[0-9a-f]{16}$/)
        expect(await precacheName(await entriesFor({...files}))).toBe(name)
        expect(await precacheName(await entriesFor({...files, "/index.html": "<html>v2</html>"}))).not.toBe(name)
    })

    it("copies unchanged files from an older build instead of downloading them", async () => {
        const oldEntries = await entriesFor(files)
        await installPrecache(caches, await precacheName(oldEntries), oldEntries, serve(files))

        const newFiles = {...files, "/index.html": "<html>v2</html>"}
        const newEntries = await entriesFor(newFiles)
        const fetchFile = serve(newFiles)
        await installPrecache(caches, await precacheName(newEntries), newEntries, fetchFile)

        expect(fetchFile.mock.calls.map(call => call[0])).toEqual(["/index.html"])
    })

    it.each([
        ["is missing", {"/index.html": files["/index.html"]}],
        ["has other content, e.g. from a newer deployment", {...files, "/assets/index-abc.js": "console.log(2)"}],
    ])("fails and leaves no cache behind if a file %s", async (_name, served) => {
        const entries = await entriesFor(files)
        const name = await precacheName(entries)

        await expect(installPrecache(caches, name, entries, serve(served))).rejects.toThrow("/assets/index-abc.js")
        expect(await caches.has(name)).toBe(false)
    })

    it("deletes only the app shell caches of other builds", async () => {
        await caches.open("tormap-precache-old")
        await caches.open("tormap-precache-current")
        await caches.open("other-feature")

        await deleteOtherPrecaches(caches, "tormap-precache-current")

        expect((await caches.keys()).sort()).toEqual(["other-feature", "tormap-precache-current"])
    })
})
