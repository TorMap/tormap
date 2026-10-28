/**
 * Messages from the app to the service worker. Only types are shared: a value imported by both would become a chunk
 * of its own, which the worker cannot load.
 */
export type ServiceWorkerMessage =
    // Activates a waiting worker after the user agreed to reload
    | "SKIP_WAITING"
    // Requests that answer with the DataCacheState on the port passed with the message
    | DataCacheRequest

export type DataCacheRequest =
    | { type: "GET_DATA_CACHE" }
    | { type: "SET_DATA_CACHING", enabled: boolean }
    | { type: "CLEAR_DATA_CACHE" }

/**
 * What the relay data cache holds
 */
export interface DataCacheState {
    /** Whether viewed days are kept for offline use */
    enabled: boolean
    /** Days (YYYY-MM-DD) with cached data, sorted */
    dates: string[]
}

/**
 * Header of responses that the worker answered from the relay data cache, with the time the data was downloaded
 */
export type CachedAtHeader = "X-TorMap-Cached-At"
