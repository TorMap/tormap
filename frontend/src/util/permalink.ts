export interface Permalink {
    lat?: number
    lng?: number
    zoom?: number
    day?: string
}

const dayPattern = /^\d{4}-\d{2}-\d{2}$/
const minZoom = 2
const maxZoom = 18

function parseNumber(value: string | null, min: number, max: number): number | undefined {
    if (value === null || value.trim() === "") return undefined
    const parsed = Number(value)
    return Number.isFinite(parsed) && parsed >= min && parsed <= max ? parsed : undefined
}

/**
 * Parses a location hash like `#lat=30&lng=0&zoom=3&day=2024-05-01`.
 * The hash is untrusted input, so every value is validated and invalid values are dropped.
 * Latitude and longitude are only returned as a pair.
 * @param hash - The location hash, with or without the leading `#`
 */
export function parsePermalink(hash: string): Permalink {
    const params = new URLSearchParams(hash.startsWith("#") ? hash.slice(1) : hash)
    const lat = parseNumber(params.get("lat"), -90, 90)
    const lng = parseNumber(params.get("lng"), -180, 180)
    const zoom = parseNumber(params.get("zoom"), minZoom, maxZoom)
    const day = params.get("day")
    const result: Permalink = {}
    if (lat !== undefined && lng !== undefined) {
        result.lat = lat
        result.lng = lng
    }
    if (zoom !== undefined) result.zoom = Math.round(zoom)
    if (day !== null && dayPattern.test(day) && !Number.isNaN(Date.parse(day))) result.day = day
    return result
}

/**
 * Formats a permalink as location hash (without the leading `#`).
 */
export function formatPermalink(permalink: Permalink): string {
    const params = new URLSearchParams()
    if (permalink.lat !== undefined && permalink.lng !== undefined) {
        params.set("lat", permalink.lat.toFixed(3))
        params.set("lng", permalink.lng.toFixed(3))
    }
    if (permalink.zoom !== undefined) params.set("zoom", String(permalink.zoom))
    if (permalink.day !== undefined) params.set("day", permalink.day)
    return params.toString()
}

/**
 * Reads the permalink from the current URL. Returns an empty permalink if the location is not available.
 */
export function readPermalink(): Permalink {
    try {
        return parsePermalink(window.location.hash)
    } catch {
        return {}
    }
}

/**
 * Merges the given values into the permalink of the current URL without adding a history entry.
 */
export function writePermalink(update: Permalink): void {
    try {
        const hash = formatPermalink({...readPermalink(), ...update})
        window.history.replaceState(window.history.state, "", `${window.location.pathname}${window.location.search}#${hash}`)
    } catch {
        // The permalink is a convenience, so a restricted history API must not break the app
    }
}
