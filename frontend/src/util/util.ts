import {useEffect, useState} from "react";

import {backendApiUrl} from "../config";

/**
 * new React-Hook for handling
 * @param value Value to debounce
 * @param delay Time without changes to update value
 */
export function useDebounce<T>(value: T, delay: number): T {
    // State and setters for debounced value
    const [debouncedValue, setDebouncedValue] = useState<T>(value);
    useEffect(
        () => {
            if (value === undefined) return undefined
            // Update debounced value after delay
            const handler = setTimeout(() => {
                setDebouncedValue(value);
            }, delay);
            // Cancel the timeout if value changes (also on delay change or unmount)
            // This is how we prevent debounced value from updating if value is changed ...
            // .. within the delay period. Timeout gets cleared and restarted.
            return () => {
                clearTimeout(handler);
            };
        },
        [value, delay] // Only re-call effect if value or delay changes
    );
    return debouncedValue;
}

/**
 * Helper to check if object contains key
 */
export const nameOfFactory = <T>() => (name: keyof T) => name;

export interface BackendResponse<T> {
    data: T
    status: number
}

/**
 * Rejection of a backend request. `status` and `data` are undefined if no response arrived (e.g. connection lost).
 */
export class BackendError extends Error {
    constructor(message: string, readonly status?: number, readonly data?: unknown, options?: ErrorOptions) {
        super(message, options)
        this.name = "BackendError"
    }
}

export type BackendTransport = (url: string, init: RequestInit) => Promise<Response>

/**
 * Parses a JSON body, falling back to the raw text (and to undefined for an empty body).
 */
const parseBody = (text: string): unknown => {
    if (text === "") return undefined
    try {
        return JSON.parse(text)
    } catch {
        return text
    }
}

const request = async <T>(method: string, path: string, body?: unknown): Promise<BackendResponse<T>> => {
    const url = `${(backendApiUrl ?? "").replace(/\/+$/, "")}${path}`
    // Only send Content-Type with a body: on GET requests it would force a CORS preflight
    const init: RequestInit = body === undefined
        ? {method, headers: {Accept: "application/json"}}
        : {method, headers: {Accept: "application/json", "Content-Type": "application/json"}, body: JSON.stringify(body)}
    let response: Response
    try {
        response = await backend.transport(url, init)
    } catch (error) {
        throw new BackendError(`Network error for ${method} ${path}`, undefined, undefined, {cause: error})
    }
    const data = parseBody(await response.text())
    if (!response.ok) {
        throw new BackendError(`Request failed with status code ${response.status}`, response.status, data)
    }
    return {data: data as T, status: response.status}
}

export const backend = {
    /**
     * Sends the HTTP request. Tests replace it to answer requests without network access.
     */
    transport: ((url, init) => fetch(url, init)) as BackendTransport,
    get: <T>(path: string) => request<T>("GET", path),
    post: <T>(path: string, body: unknown) => request<T>("POST", path, body),
}