import {AxiosAdapter, AxiosError, AxiosResponse} from "axios";

import {backend} from "../util/util";

/*
 Replaces the transport of the app's axios instance. Requests still pass through axios (interceptors and transforms),
 but nothing leaves the browser. Routes are registered per test; unmocked requests are recorded and fail the test.
 */

type Path = string | RegExp

export interface MockRequest {
    method: string
    url: string
    /** Parsed JSON body, if any */
    data: unknown
    /** Capture groups when the route was registered with a RegExp */
    match: RegExpMatchArray | null
}

class Reply {
    constructor(readonly status: number, readonly data: unknown) {
    }
}

class NetworkFailure {
}

/**
 * Answer with a specific status, e.g. `reply(500, ...)`. Handlers returning plain data answer with 200.
 */
export const reply = (status: number, data?: unknown) => new Reply(status, data)

/**
 * Fail like a lost connection: the app sees an error without a response.
 */
export const networkError = () => new NetworkFailure()

type Handler = (request: MockRequest) => unknown | Promise<unknown>

// Overloads, so that inline handler functions get a typed request
interface Responder {
    (path: Path, handler: Handler): void
    (path: Path, data: unknown): void
}

interface Route {
    method: string
    path: Path
    handler: Handler
}

/**
 * A promise that is settled from the outside, to control the order in which requests complete.
 */
export const deferred = <T>() => {
    let resolve!: (value: T) => void
    const promise = new Promise<T>(res => {
        resolve = res
    })
    return {promise, resolve}
}

let routes: Route[] = []
let requests: MockRequest[] = []
let unhandled: string[] = []

const originalAdapter = backend.defaults.adapter

const parseBody = (data: unknown): unknown => {
    if (typeof data !== "string") return data
    try {
        return JSON.parse(data)
    } catch {
        return data
    }
}

const adapter: AxiosAdapter = async (config) => {
    const method = (config.method ?? "get").toUpperCase()
    const url = config.url ?? ""
    let match: RegExpMatchArray | null = null
    const route = routes.find(candidate => {
        if (candidate.method !== method) return false
        if (typeof candidate.path === "string") return candidate.path === url
        match = url.match(candidate.path)
        return match !== null
    })
    const request: MockRequest = {method, url, data: parseBody(config.data), match}
    requests.push(request)

    if (!route) {
        unhandled.push(`${method} ${url}`)
        throw new AxiosError(`No mock registered for ${method} ${url}`, AxiosError.ERR_NETWORK, config)
    }

    let result = await route.handler(request)
    if (result instanceof NetworkFailure) {
        throw new AxiosError("Network Error", AxiosError.ERR_NETWORK, config)
    }
    if (!(result instanceof Reply)) result = new Reply(200, result)
    const {status, data} = result as Reply

    const response: AxiosResponse = {data, status, statusText: String(status), headers: {}, config, request: {}}
    const valid = config.validateStatus ? config.validateStatus(status) : status >= 200 && status < 300
    if (valid) return response
    throw new AxiosError(
        `Request failed with status code ${status}`,
        status >= 500 ? AxiosError.ERR_BAD_RESPONSE : AxiosError.ERR_BAD_REQUEST,
        config,
        response.request,
        response,
    )
}

const register = (method: string, path: Path, handler: Handler | unknown) => {
    const routeHandler: Handler = typeof handler === "function" ? handler as Handler : () => handler
    // Later registrations win, so a test can override a default
    routes.unshift({method, path, handler: routeHandler})
}

export const mockBackend = {
    /**
     * Routes the app's axios instance to the registered handlers. Called once from the test setup.
     */
    install() {
        backend.defaults.adapter = adapter
    },

    /**
     * Restores the real transport. Only needed by tests of the setup itself.
     */
    uninstall() {
        backend.defaults.adapter = originalAdapter
    },

    /**
     * Answers GET requests with the given data, or with the result of a handler function.
     */
    onGet: ((path: Path, response: Handler | unknown) => register("GET", path, response)) as Responder,

    /**
     * Answers POST requests with the given data, or with the result of a handler function.
     */
    onPost: ((path: Path, response: Handler | unknown) => register("POST", path, response)) as Responder,

    /**
     * All requests made since the last reset, in order.
     */
    get requests(): readonly MockRequest[] {
        return requests
    },

    /**
     * URLs of requests made since the last reset, e.g. `GET /relay/location/days`.
     */
    get requested(): string[] {
        return requests.map(request => `${request.method} ${request.url}`)
    },

    /**
     * Requests that had no matching route.
     */
    get unhandled(): readonly string[] {
        return unhandled
    },

    reset() {
        routes = []
        requests = []
        unhandled = []
    },
}
