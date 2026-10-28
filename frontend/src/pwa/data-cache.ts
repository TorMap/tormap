import {useEffect, useState} from "react";

import type {DataCacheRequest, DataCacheState} from "../service-worker/messages";

/**
 * Sends a request to the service worker that controls this page and waits for its answer. Undefined without such a
 * worker (dev server, Tor Browser, first visit) or if it does not answer in time.
 */
export function requestDataCache(
    request: DataCacheRequest,
    controller: ServiceWorker | null | undefined = navigator.serviceWorker?.controller,
    timeoutMs = 3000,
): Promise<DataCacheState | undefined> {
    if (!controller) return Promise.resolve(undefined)
    return new Promise(resolve => {
        const channel = new MessageChannel()
        const timer = setTimeout(() => resolve(undefined), timeoutMs)
        channel.port1.onmessage = event => {
            clearTimeout(timer)
            resolve(event.data as DataCacheState)
        }
        controller.postMessage(request, [channel.port2])
    })
}

/**
 * The state of the offline relay data, reloaded whenever `reloadKey` changes. Undefined while unknown or without a
 * service worker.
 */
export function useDataCacheState(reloadKey: unknown): [DataCacheState | undefined, (state: DataCacheState | undefined) => void] {
    const [state, setState] = useState<DataCacheState>()
    useEffect(() => {
        let active = true
        void requestDataCache({type: "GET_DATA_CACHE"}).then(answer => {
            if (active) setState(answer)
        })
        return () => {
            active = false
        }
    }, [reloadKey])
    return [state, setState]
}
