import {createHash} from "node:crypto";
import {readFileSync} from "node:fs";
import path from "node:path";

import type {Plugin, ResolvedConfig} from "vite";

interface Options {
    /** The worker's source, built to /sw.js */
    entry: string
    /** Files of the public directory that the app shell needs, besides everything in the build */
    publicFiles: string[]
}

const workerFileName = "sw.js"
const placeholder = "self.__TORMAP_PRECACHE"

const sha256Hex = (content: string | Uint8Array) => createHash("sha256").update(content).digest("hex")

/**
 * Builds the service worker and writes the list of files to precache into it. The worker gets a fixed name, so its
 * scope is the whole site and the browser finds a new version on its next update check. Only used for builds: the dev
 * server registers no worker.
 */
export function serviceWorker(options: Options): Plugin {
    let config: ResolvedConfig
    return {
        name: "tormap:service-worker",
        apply: "build",
        configResolved(resolvedConfig) {
            config = resolvedConfig
        },
        buildStart() {
            this.emitFile({type: "chunk", id: options.entry, fileName: workerFileName})
        },
        generateBundle: {
            // After Vite wrote index.html and finished all chunks, so the revisions match the files on disk
            order: "post",
            handler(_outputOptions, bundle) {
                const worker = bundle[workerFileName]
                if (worker?.type !== "chunk") {
                    this.error(`${workerFileName} is missing from the build`)
                }
                // Browsers load the worker as a classic script, which cannot import or export
                if (worker.imports.length > 0 || worker.dynamicImports.length > 0 || worker.exports.length > 0) {
                    this.error(`${workerFileName} must not share modules with the app: imports ${worker.imports.join(", ")}`)
                }
                if (worker.code.split(placeholder).length !== 2) {
                    this.error(`${workerFileName} must contain ${placeholder} exactly once`)
                }

                const entries = Object.values(bundle)
                    .filter(file => file.fileName !== workerFileName && !file.fileName.endsWith(".map"))
                    .map(file => ({
                        url: config.base + file.fileName,
                        revision: sha256Hex(file.type === "chunk" ? file.code : file.source),
                    }))
                options.publicFiles.forEach(fileName => entries.push({
                    url: config.base + fileName,
                    revision: sha256Hex(readFileSync(path.join(config.publicDir, fileName))),
                }))
                entries.sort((a, b) => a.url.localeCompare(b.url))

                worker.code = worker.code.replace(placeholder, JSON.stringify(entries))
            },
        },
    }
}
