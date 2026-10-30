import type {HtmlTagDescriptor, IndexHtmlTransformResult} from "vite";
import {describe, expect, it} from "vitest";

import {liteRedirect} from "./lite-redirect";

const html = "<head><title>TorMap</title></head><body><noscript>You need to enable JavaScript to run this app.</noscript></body>"

function transform(liteUrl: string | undefined, input = html): IndexHtmlTransformResult {
    const hook = liteRedirect(liteUrl).transformIndexHtml
    if (typeof hook !== "function") throw new Error("Expected a transformIndexHtml function")
    return hook.call({} as never, input, {} as never) as IndexHtmlTransformResult
}

describe("liteRedirect", () => {
    it("leaves index.html unchanged while the feature is off", () => {
        expect(transform(undefined)).toBe(html)
    })

    it("adds a refresh to TorMap Lite and a visible link", () => {
        const result = transform("https://api.tormap.org/lite/") as {html: string, tags: HtmlTagDescriptor[]}
        expect(result.html).toContain('<a href="https://api.tormap.org/lite/">TorMap Lite</a>')
        expect(result.html).not.toContain("You need to enable JavaScript")
        expect(result.tags).toEqual([expect.objectContaining({
            tag: "noscript",
            injectTo: "head",
            children: [
                {tag: "meta", attrs: {"http-equiv": "refresh", content: "0; url=https://api.tormap.org/lite/"}},
                {tag: "style", children: "#splash { display: none; }"},
            ],
        })])
    })

    it("rejects URLs that are not plain", () => {
        expect(() => liteRedirect('https://example.org/"><script>')).toThrow("Invalid TorMap Lite URL")
        expect(() => liteRedirect("javascript:alert(1)")).toThrow("Invalid TorMap Lite URL")
    })

    it("fails the build if the message to replace is missing", () => {
        expect(() => transform("https://api.tormap.org/lite/", "<body></body>")).toThrow("no default <noscript>")
    })
})
