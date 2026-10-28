import {createHash} from "node:crypto";
import {mkdtempSync, writeFileSync} from "node:fs";
import {tmpdir} from "node:os";
import path from "node:path";

import {describe, expect, it, vi} from "vitest";

import {serviceWorker} from "./service-worker";

const sha256Hex = (content: string) => createHash("sha256").update(content).digest("hex")

interface FakeChunk {
    type: "chunk"
    fileName: string
    code: string
    imports: string[]
    dynamicImports: string[]
    exports: string[]
}

const chunk = (fileName: string, code: string, options: Partial<FakeChunk> = {}): FakeChunk =>
    ({type: "chunk", fileName, code, imports: [], dynamicImports: [], exports: [], ...options})

function setup(bundle: Record<string, unknown>) {
    const publicDir = mkdtempSync(path.join(tmpdir(), "tormap-public-"))
    writeFileSync(path.join(publicDir, "manifest.json"), "{}")
    const plugin = serviceWorker({entry: "src/service-worker/sw.ts", publicFiles: ["manifest.json"]})
    const context = {
        emitFile: vi.fn(),
        error: vi.fn((message: string) => {
            throw new Error(message)
        }),
    }
    ;(plugin.configResolved as (config: object) => void)({base: "/", publicDir})
    ;(plugin.buildStart as (this: object) => void).call(context)
    const generateBundle = () => (plugin.generateBundle as { handler: (this: object, options: object, bundle: object) => void })
        .handler.call(context, {}, bundle)
    return {context, generateBundle}
}

describe("serviceWorker plugin", () => {
    it("emits the worker under a fixed name", () => {
        const {context} = setup({})
        expect(context.emitFile).toHaveBeenCalledWith({type: "chunk", id: "src/service-worker/sw.ts", fileName: "sw.js"})
    })

    it("writes every file of the build and the listed public files into the worker", () => {
        const worker = chunk("sw.js", "const entries=self.__TORMAP_PRECACHE;")
        const {generateBundle} = setup({
            "sw.js": worker,
            "index.html": {type: "asset", fileName: "index.html", source: "<html></html>"},
            "assets/index-abc.js": chunk("assets/index-abc.js", "app()"),
            "assets/index-abc.js.map": {type: "asset", fileName: "assets/index-abc.js.map", source: "{}"},
        })
        generateBundle()

        const entries = JSON.parse(worker.code.slice("const entries=".length, -1))
        expect(entries).toEqual([
            {url: "/assets/index-abc.js", revision: sha256Hex("app()")},
            {url: "/index.html", revision: sha256Hex("<html></html>")},
            {url: "/manifest.json", revision: sha256Hex("{}")},
        ])
    })

    it("refuses a worker that shares code with the app", () => {
        const {generateBundle} = setup({
            "sw.js": chunk("sw.js", "self.__TORMAP_PRECACHE", {imports: ["assets/shared.js"]}),
        })
        expect(generateBundle).toThrow("must not share modules")
    })

    it("refuses a worker without the placeholder", () => {
        const {generateBundle} = setup({"sw.js": chunk("sw.js", "")})
        expect(generateBundle).toThrow("exactly once")
    })
})
