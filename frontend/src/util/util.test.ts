import {renderHook} from "vitest-browser-react";
import {afterEach, describe, expect, it, vi} from "vitest";

import {backendApiUrl} from "../config";
import {backend, BackendError, BackendTransport, nameOfFactory, useDebounce} from "./util";

describe("useDebounce", () => {
    afterEach(() => {
        vi.useRealTimers()
    })

    const render = (initial: string | undefined, delay = 200) =>
        renderHook((value?: string | undefined) => useDebounce(value, delay), {initialProps: initial})

    it("returns the initial value immediately", async () => {
        const {result} = await render("a")
        expect(result.current).toBe("a")
    })

    it("updates after the delay", async () => {
        vi.useFakeTimers()
        const {result, rerender, act} = await render("a")
        await rerender("b")
        expect(result.current).toBe("a")
        await act(() => vi.advanceTimersByTime(199))
        expect(result.current).toBe("a")
        await act(() => vi.advanceTimersByTime(1))
        expect(result.current).toBe("b")
    })

    it("restarts the delay when the value changes again", async () => {
        vi.useFakeTimers()
        const {result, rerender, act} = await render("a")
        await rerender("b")
        await act(() => vi.advanceTimersByTime(150))
        await rerender("c")
        await act(() => vi.advanceTimersByTime(150))
        expect(result.current).toBe("a")
        await act(() => vi.advanceTimersByTime(50))
        expect(result.current).toBe("c")
    })

    it("ignores undefined values", async () => {
        vi.useFakeTimers()
        const {result, rerender, act} = await render("a")
        await rerender(undefined)
        await act(() => vi.advanceTimersByTime(1000))
        expect(result.current).toBe("a")
    })
})

describe("nameOfFactory", () => {
    it("returns the given key name", () => {
        const nameOf = nameOfFactory<{ title: string }>()
        expect(nameOf("title")).toBe("title")
    })
})

describe("backend", () => {
    // The test setup installs the mock transport again before each test
    const useTransport = (respond: () => Response | Promise<Response>) => {
        const transport = vi.fn<BackendTransport>(async () => respond())
        backend.transport = transport
        return transport
    }

    it("sends GET requests to the configured backend without a Content-Type", async () => {
        const transport = useTransport(() => Response.json(["2024-01-01"]))
        const response = await backend.get<string[]>("/relay/location/days")

        expect(response).toEqual({data: ["2024-01-01"], status: 200})
        const [url, init] = transport.mock.calls[0]
        expect(url).toBe(`${backendApiUrl}/relay/location/days`)
        expect(init).toEqual({method: "GET", headers: {Accept: "application/json"}})
    })

    it("sends POST bodies as JSON", async () => {
        const transport = useTransport(() => Response.json([]))
        await backend.post("/relay/details/relay/identifiers", [1, 2])

        const [, init] = transport.mock.calls[0]
        expect(init.method).toBe("POST")
        expect(init.headers).toEqual({Accept: "application/json", "Content-Type": "application/json"})
        expect(init.body).toBe("[1,2]")
    })

    it("returns non-JSON bodies as text", async () => {
        useTransport(() => new Response("plain"))
        expect((await backend.get("/text")).data).toBe("plain")
    })

    it("rejects error statuses with status and body", async () => {
        useTransport(() => Response.json({message: "boom"}, {status: 503}))
        const error = await backend.get("/broken").catch(e => e)

        expect(error).toBeInstanceOf(BackendError)
        expect(error).toMatchObject({status: 503, data: {message: "boom"}})
    })

    it("rejects connection failures without a status", async () => {
        const cause = new TypeError("Failed to fetch")
        useTransport(() => Promise.reject(cause))
        const error = await backend.get("/offline").catch(e => e)

        expect(error).toBeInstanceOf(BackendError)
        expect(error.status).toBeUndefined()
        expect(error.cause).toBe(cause)
    })
})
