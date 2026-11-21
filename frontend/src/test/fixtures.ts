import {defaultSettings} from "../config";
import {FamilyV1Dto, RelayDetailsDto, RelayFamilyIdentifier, RelayIdentifierDto, RelayLocationDto, RelayV1Dto} from "../dto/relay";
import {Settings} from "../types/settings";

let nextRelay = 1

/**
 * A fingerprint (40 hexadecimal characters) made from a number, e.g. for the n-th relay of a test
 */
export const fingerprintOf = (n: number) => n.toString(16).toUpperCase().padStart(40, "0")

/**
 * Builds a relay at 0,0 in DE. Override only what a test cares about.
 */
export const makeRelay = (overrides: Partial<RelayLocationDto> = {}): RelayLocationDto => ({
    lat: 0,
    long: 0,
    country: "DE",
    nickname: "relay",
    flags: [],
    fingerprint: fingerprintOf(nextRelay++),
    hasDetails: true,
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
    relayTypeColors: {...defaultSettings.relayTypeColors},
    relaysMustHaveFlag: {...defaultSettings.relaysMustHaveFlag},
    ...overrides,
})

/**
 * Builds the details of one relay. Override only what a test cares about.
 */
export const makeRelayDetails = (overrides: Partial<RelayDetailsDto> = {}): RelayDetailsDto => ({
    month: "2024-01",
    day: "2024-01-02",
    address: "192.0.2.1",
    autonomousSystemName: "Example AS",
    autonomousSystemNumber: 64500,
    allowSingleHopExits: false,
    nickname: "relay",
    bandwidthRate: 2_000_000,
    bandwidthBurst: 4_000_000,
    bandwidthObserved: 1_000_000,
    platform: "Tor 0.4.8 on Linux",
    protocols: "Cons=1-2",
    fingerprint: "0123456789ABCDEF0123456789ABCDEF01234567",
    hibernating: false,
    uptime: 7200,
    contact: "operator@example.org",
    familyEntries: "",
    familyKeys: [],
    familyId: null,
    cachesExtraInfo: true,
    hiddenServiceDir: true,
    linkProtocolVersions: "1-5",
    circuitProtocolVersions: "1-2",
    tunnelledDirServer: true,
    confirmedFamilyMembers: [],
    verifiedHostNames: [],
    unverifiedHostNames: [],
    ...overrides,
})

export const makeRelayIdentifier = (overrides: Partial<RelayIdentifierDto> = {}): RelayIdentifierDto => ({
    fingerprint: "0123456789ABCDEF0123456789ABCDEF01234567",
    nickname: "relay",
    ...overrides,
})

export const makeFamilyIdentifier = (overrides: Partial<RelayFamilyIdentifier> = {}): RelayFamilyIdentifier => ({
    id: 1,
    memberCount: 2,
    nicknames: "alpha, bravo",
    autonomousSystems: "AS64500",
    ...overrides,
})

/**
 * Builds a family as `/v1/families?month=` returns it
 */
export const makeFamily = (overrides: Partial<FamilyV1Dto> = {}): FamilyV1Dto => ({
    id: 1,
    members: [
        {fingerprint: fingerprintOf(0xA), nickname: "alpha"},
        {fingerprint: fingerprintOf(0xB), nickname: "bravo"},
    ],
    autonomousSystems: [{number: 64500, name: "AS64500"}],
    ...overrides,
})

/**
 * Builds `count` relays of a day as `/v1/days/{day}/relays` returns them
 */
export const makeApiRelays = (count: number, overrides: Partial<RelayV1Dto> = {}): RelayV1Dto[] =>
    Array.from({length: count}, () => ({
        fingerprint: fingerprintOf(nextRelay++),
        nickname: "relay",
        country: "DE",
        lat: 0,
        long: 0,
        flags: [],
        consensusWeight: null,
        autonomousSystemNumber: null,
        familyId: null,
        ...overrides,
    }))
