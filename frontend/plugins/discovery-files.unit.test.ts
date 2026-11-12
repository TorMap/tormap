import {describe, expect, it} from "vitest";

import {discoveryFiles, llmsTxt, robotsTxt, sitemapXml} from "./discovery-files";

describe("discoveryFiles", () => {
    it("points robots.txt to the sitemap", () => {
        expect(robotsTxt("https://tormap.org")).toBe(
            "User-agent: *\nAllow: /\n\nSitemap: https://tormap.org/sitemap.xml\n",
        )
    })

    it("lists the start page in the sitemap", () => {
        expect(sitemapXml("https://tormap.org")).toContain("<url><loc>https://tormap.org/</loc></url>")
    })

    it("links TorMap Lite in llms.txt only if it is enabled", () => {
        expect(llmsTxt("https://tormap.org", "https://api.tormap.org/lite/"))
            .toContain("- [TorMap Lite](https://api.tormap.org/lite/):")
        expect(llmsTxt("https://tormap.org", undefined)).not.toContain("TorMap Lite")
    })

    it("starts llms.txt with a title and a summary", () => {
        expect(llmsTxt("https://tormap.org", undefined)).toMatch(/^# TorMap\n\n> /)
    })

    it("rejects URLs that are not plain", () => {
        expect(() => discoveryFiles("https://tormap.org\"><x", undefined)).toThrow()
        expect(() => discoveryFiles("https://tormap.org", "javascript:alert(1)")).toThrow()
    })
})
