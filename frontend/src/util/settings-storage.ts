import {Settings, topRelayCountOptions} from "../types/settings";

export const settingsStorageKey = "tormap.settings.v1"

const hexColorPattern = /^#[0-9a-f]{6}$/

type Stored = Record<string, unknown>

const isObject = (value: unknown): value is Stored => typeof value === "object" && value !== null && !Array.isArray(value)

/**
 * Takes each entry of a stored record, whose key exists in the defaults and whose value passes the check
 */
function mergeRecord<K extends PropertyKey, V>(defaults: Record<K, V>, stored: unknown, isValid: (value: unknown) => value is V): Record<K, V> {
    const result = {...defaults}
    if (!isObject(stored)) return result
    for (const key of Object.keys(defaults) as K[]) {
        const value = stored[String(key)]
        if (isValid(value)) result[key] = value
    }
    return result
}

const isBoolean = (value: unknown): value is boolean => typeof value === "boolean"
const isHexColor = (value: unknown): value is string => typeof value === "string" && hexColorPattern.test(value)

/**
 * Merges settings stored as JSON into the defaults. Unknown keys and invalid values are ignored,
 * so outdated or tampered storage can not break the app.
 * @param json - The stored settings, null if none are stored
 * @param defaults - The settings to fall back to
 */
export function parseStoredSettings(json: string | null, defaults: Settings): Settings {
    if (!json) return defaults
    let stored: unknown
    try {
        stored = JSON.parse(json)
    } catch {
        return defaults
    }
    if (!isObject(stored)) return defaults

    const booleanSetting = (key: "sortCountry" | "sortFamily" | "aggregateCoordinates" | "heatMap" | "sizeByWeight" | "showCountryUsage") =>
        isBoolean(stored[key]) ? stored[key] : defaults[key]

    return {
        sortCountry: booleanSetting("sortCountry"),
        selectedCountry: typeof stored.selectedCountry === "string" ? stored.selectedCountry : defaults.selectedCountry,
        sortFamily: booleanSetting("sortFamily"),
        // Family IDs differ between dates, so a selected family is never stored
        selectedFamily: defaults.selectedFamily,
        aggregateCoordinates: booleanSetting("aggregateCoordinates"),
        heatMap: booleanSetting("heatMap"),
        sizeByWeight: booleanSetting("sizeByWeight"),
        showCountryUsage: booleanSetting("showCountryUsage"),
        topRelayCount: topRelayCountOptions.includes(stored.topRelayCount as number)
            ? stored.topRelayCount as number
            : defaults.topRelayCount,
        showRelayTypes: mergeRecord(defaults.showRelayTypes, stored.showRelayTypes, isBoolean),
        relayTypeColors: mergeRecord(defaults.relayTypeColors, stored.relayTypeColors, isHexColor),
        relaysMustHaveFlag: mergeRecord(defaults.relaysMustHaveFlag, stored.relaysMustHaveFlag, isBoolean),
    }
}

/**
 * Loads the settings from the browser's local storage, falls back to the defaults if storage is unavailable
 */
export function loadSettings(defaults: Settings): Settings {
    try {
        return parseStoredSettings(window.localStorage.getItem(settingsStorageKey), defaults)
    } catch {
        return defaults
    }
}

/**
 * Saves the settings to the browser's local storage. Fails silently, e.g. in private mode or when storage is full.
 */
export function saveSettings(settings: Settings) {
    try {
        window.localStorage.setItem(settingsStorageKey, JSON.stringify({...settings, selectedFamily: undefined}))
    } catch {
        // Settings are then only kept until the page is reloaded
    }
}
