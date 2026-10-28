import {vi} from "vitest";

import type {DataCacheRequest, DataCacheState} from "../service-worker/messages";

/**
 * Lets a fake service worker control the page, which answers relay data cache requests like the real one.
 * Restore with vi.restoreAllMocks().
 */
export function fakeServiceWorker(initial: DataCacheState) {
    let state = initial
    const requests: DataCacheRequest[] = []
    const controller = {
        postMessage: (request: DataCacheRequest, [port]: MessagePort[]) => {
            requests.push(request)
            if (request.type === "SET_DATA_CACHING") state = {enabled: request.enabled, dates: request.enabled ? state.dates : []}
            if (request.type === "CLEAR_DATA_CACHE") state = {...state, dates: []}
            port.postMessage(state)
        },
    }
    vi.spyOn(ServiceWorkerContainer.prototype, "controller", "get").mockReturnValue(controller as unknown as ServiceWorker)
    return {requests}
}
