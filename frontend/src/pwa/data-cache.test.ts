import {afterEach, describe, expect, it, vi} from "vitest";

import {fakeServiceWorker} from "../test/fake-service-worker";
import {requestDataCache} from "./data-cache";

describe("requestDataCache", () => {
    afterEach(() => {
        vi.restoreAllMocks()
    })

    it("answers with the state from the service worker", async () => {
        const {requests} = fakeServiceWorker({enabled: true, dates: ["2024-01-01"]})

        expect(await requestDataCache({type: "GET_DATA_CACHE"})).toEqual({enabled: true, dates: ["2024-01-01"]})
        expect(requests).toEqual([{type: "GET_DATA_CACHE"}])
    })

    it("answers undefined without a service worker", async () => {
        expect(await requestDataCache({type: "GET_DATA_CACHE"}, null)).toBeUndefined()
    })

    it("answers undefined if the service worker does not answer", async () => {
        const silent = {postMessage: () => undefined} as unknown as ServiceWorker
        expect(await requestDataCache({type: "GET_DATA_CACHE"}, silent, 10)).toBeUndefined()
    })
})
