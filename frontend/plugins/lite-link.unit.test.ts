import {describe, expect, it} from "vitest";

import {liteLink} from "./lite-link";

const html = "<head><title>TorMap</title></head><body><noscript>You need to enable JavaScript to run this app.</noscript></body>"

function transform(liteUrl: string | undefined, input = html): unknown {
    const hook = liteLink(liteUrl).transformIndexHtml
    if (typeof hook !== "function") throw new Error("Expected a transformIndexHtml function")
    return hook.call({} as never, input, {} as never)
}

describe("liteLink", () => {
    it("leaves index.html unchanged while the feature is off", () => {
        expect(transform(undefined)).toBe(html)
    })

    it("links to TorMap Lite without redirecting", () => {
        const result = transform("https://api.tormap.org/lite/")
        expect(typeof result).toBe("string")
        expect(result).toContain('<noscript><p>This map needs JavaScript. <a href="https://api.tormap.org/lite/">TorMap Lite</a> works without it.</p></noscript>')
        expect(result).not.toContain("You need to enable JavaScript")
        expect(result).not.toContain("refresh")
    })

    it("rejects URLs that are not plain", () => {
        expect(() => liteLink('https://example.org/"><script>')).toThrow("Invalid TorMap Lite URL")
        expect(() => liteLink("javascript:alert(1)")).toThrow("Invalid TorMap Lite URL")
    })

    it("fails the build if the message to replace is missing", () => {
        expect(() => transform("https://api.tormap.org/lite/", "<body></body>")).toThrow("no default <noscript>")
    })
})
