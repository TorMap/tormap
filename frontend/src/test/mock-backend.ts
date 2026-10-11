import {backendApiUrl} from "../config";
import {backend, BackendTransport} from "../util/util";

/*
 Replaces the transport of the app's backend client. Requests still pass through the client (URL, headers, JSON parsing
 and status handling), but nothing leaves the browser. Routes are registered per test; unmocked requests are recorded and fail the test.
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

const originalTransport = backend.transport

const parseBody = (body: RequestInit["body"]): unknown => {
    if (typeof body !== "string") return body ?? undefined
    try {
        return JSON.parse(body)
    } catch {
        return body
    }
}

// Like fetch, a lost connection rejects with a TypeError instead of resolving with a response
const connectionFailure = () => new TypeError("Failed to fetch")

const transport: BackendTransport = async (fullUrl, init) => {
    const method = (init.method ?? "GET").toUpperCase()
    const base = (backendApiUrl ?? "").replace(/\/+$/, "")
    const url = fullUrl.startsWith(base) ? fullUrl.slice(base.length) : fullUrl
    let match: RegExpMatchArray | null = null
    const route = routes.find(candidate => {
        if (candidate.method !== method) return false
        if (typeof candidate.path === "string") return candidate.path === url
        match = url.match(candidate.path)
        return match !== null
    })
    const request: MockRequest = {method, url, data: parseBody(init.body), match}
    requests.push(request)

    if (!route) {
        unhandled.push(`${method} ${url}`)
        throw connectionFailure()
    }

    let result = await route.handler(request)
    if (result instanceof NetworkFailure) throw connectionFailure()
    if (!(result instanceof Reply)) result = new Reply(200, result)
    const {status, data} = result as Reply
    return new Response(data === undefined ? null : JSON.stringify(data), {
        status,
        headers: {"Content-Type": "application/json"},
    })
}

const register = (method: string, path: Path, handler: Handler | unknown) => {
    const routeHandler: Handler = typeof handler === "function" ? handler as Handler : () => handler
    // Later registrations win, so a test can override a default
    routes.unshift({method, path, handler: routeHandler})
}

export const mockBackend = {
    /**
     * Routes the app's backend client to the registered handlers. Called once from the test setup.
     */
    install() {
        backend.transport = transport
    },

    /**
     * Restores the real transport. Only needed by tests of the setup itself.
     */
    uninstall() {
        backend.transport = originalTransport
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
