import type {Plugin} from "vite";

const defaultNoscript = "<noscript>You need to enable JavaScript to run this app.</noscript>"

/**
 * Sends browsers without JavaScript (e.g. Tor Browser at the "Safest" security level) to TorMap Lite, which the
 * backend serves. Behind a feature flag, because the backend has to be deployed with Lite first.
 * @param liteUrl - The URL of TorMap Lite, or undefined to leave index.html unchanged
 */
export function liteRedirect(liteUrl: string | undefined): Plugin {
    // Build configuration, but it ends up in attributes, so only plain URLs are accepted
    if (liteUrl !== undefined && !/^https?:\/\/[^\s"'<>]+$/.test(liteUrl)) {
        throw new Error(`Invalid TorMap Lite URL: ${liteUrl}`)
    }
    return {
        name: "tormap:lite-redirect",
        transformIndexHtml(html) {
            if (liteUrl === undefined) return html
            if (!html.includes(defaultNoscript)) {
                throw new Error("index.html has no default <noscript> message to replace")
            }
            return {
                html: html.replace(
                    defaultNoscript,
                    `<noscript><p>This map needs JavaScript. <a href="${liteUrl}">TorMap Lite</a> works without it.</p></noscript>`,
                ),
                tags: [{
                    tag: "noscript",
                    // At the end of the head, after the splash style, so that hiding the splash screen wins and the
                    // link stays visible if the browser blocks the refresh
                    injectTo: "head",
                    children: [
                        {tag: "meta", attrs: {"http-equiv": "refresh", content: `0; url=${liteUrl}`}},
                        {tag: "style", children: "#splash { display: none; }"},
                    ],
                }],
            }
        },
    }
}
