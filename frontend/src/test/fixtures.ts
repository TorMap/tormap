import {defaultSettings} from "../config";
import {RelayLocationDto} from "../dto/relay";
import {Settings} from "../types/settings";

let nextDetailsId = 1

/**
 * Builds a relay at 0,0 in DE. Override only what a test cares about.
 */
export const makeRelay = (overrides: Partial<RelayLocationDto> = {}): RelayLocationDto => ({
    lat: 0,
    long: 0,
    country: "DE",
    nickname: "relay",
    flags: [],
    detailsId: nextDetailsId++,
    familyId: null,
    ...overrides,
})

/**
 * Builds `count` relays that share the given overrides (e.g. the same coordinates).
 */
export const makeRelays = (count: number, overrides: Partial<RelayLocationDto> = {}): RelayLocationDto[] =>
    Array.from({length: count}, () => makeRelay(overrides))

/**
 * Default app settings with overrides. Nested records are copied, so tests can not leak state into each other.
 */
export const makeSettings = (overrides: Partial<Settings> = {}): Settings => ({
    ...defaultSettings,
    showRelayTypes: {...defaultSettings.showRelayTypes},
    relaysMustHaveFlag: {...defaultSettings.relaysMustHaveFlag},
    ...overrides,
})
