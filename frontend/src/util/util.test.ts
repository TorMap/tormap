import {renderHook} from "vitest-browser-react";
import {afterEach, describe, expect, it, vi} from "vitest";

import {backendApiUrl} from "../config";
import {backend, nameOfFactory, useDebounce} from "./util";

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
    it("targets the configured backend and sends JSON", () => {
        expect(backend.defaults.baseURL).toBe(backendApiUrl)
        expect(backend.defaults.headers["Content-type"]).toBe("application/json")
    })
})
