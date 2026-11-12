import type {Plugin} from "vite";

const plainUrl = /^https?:\/\/[^\s"'<>]+$/

/** robots.txt: every crawler may read the whole site, including AI crawlers. */
export function robotsTxt(siteUrl: string): string {
    return [
        "User-agent: *",
        "Allow: /",
        "",
        `Sitemap: ${siteUrl}/sitemap.xml`,
        "",
    ].join("\n")
}

/** sitemap.xml: the app has a single page, its views live in the URL hash, which crawlers ignore. */
export function sitemapXml(siteUrl: string): string {
    return [
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
        `  <url><loc>${siteUrl}/</loc></url>`,
        "</urlset>",
        "",
    ].join("\n")
}

/**
 * llms.txt (https://llmstxt.org): a short Markdown summary with links for language models and agents.
 * @param liteUrl - The URL of TorMap Lite, or undefined if the backend does not serve it
 */
export function llmsTxt(siteUrl: string, liteUrl: string | undefined): string {
    return [
        "# TorMap",
        "",
        "> TorMap shows where the public relays of the Tor network are hosted, on a world map and for any day since " +
        "October 2007. It is free and open source (GPL-3.0).",
        "",
        "Relays can be grouped by family, autonomous system (AS), country or coordinates, filtered by type (exit, " +
        "guard, other) and relay flags, and searched by nickname or fingerprint. Countries can be colored by the " +
        "estimated number of Tor users. The data comes from the Tor Project's CollecTor archive and Tor Metrics, with " +
        "IP geolocation and AS data by DB-IP, and is updated every day.",
        "",
        "## Links",
        "",
        `- [TorMap](${siteUrl}/): the interactive map (needs JavaScript)`,
        ...(liteUrl ? [`- [TorMap Lite](${liteUrl}): most of the map's features as plain HTML pages that work without JavaScript`] : []),
        "- [Source code and README](https://github.com/TorMap/tormap): features, self-hosting with Docker, data sources",
        "- [Tor Metrics](https://metrics.torproject.org/): the Tor Project's own statistics and relay search",
        "",
    ].join("\n")
}

/**
 * Adds robots.txt, sitemap.xml and llms.txt to the build, with absolute URLs of this deployment.
 * @param siteUrl - The public URL of the frontend, without trailing slash
 * @param liteUrl - The URL of TorMap Lite, or undefined if the backend does not serve it
 */
export function discoveryFiles(siteUrl: string, liteUrl: string | undefined): Plugin {
    // Build configuration, but it ends up in files that crawlers parse, so only plain URLs are accepted
    for (const url of [siteUrl, liteUrl]) {
        if (url !== undefined && !plainUrl.test(url)) throw new Error(`Invalid URL: ${url}`)
    }
    return {
        name: "tormap:discovery-files",
        apply: "build",
        generateBundle() {
            this.emitFile({type: "asset", fileName: "robots.txt", source: robotsTxt(siteUrl)})
            this.emitFile({type: "asset", fileName: "sitemap.xml", source: sitemapXml(siteUrl)})
            this.emitFile({type: "asset", fileName: "llms.txt", source: llmsTxt(siteUrl, liteUrl)})
        },
    }
}
