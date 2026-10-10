import {defaultSettings} from "../config";
import {RelayDetailsDto, RelayFamilyIdentifier, RelayIdentifierDto, RelayLocationDto} from "../dto/relay";
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
    relayTypeColors: {...defaultSettings.relayTypeColors},
    relaysMustHaveFlag: {...defaultSettings.relaysMustHaveFlag},
    ...overrides,
})

/**
 * Builds the details of one relay. Override only what a test cares about.
 */
export const makeRelayDetails = (overrides: Partial<RelayDetailsDto> = {}): RelayDetailsDto => ({
    id: 1,
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
    isHibernating: false,
    uptime: 7200,
    contact: "operator@example.org",
    familyEntries: "",
    familyId: null,
    cachesExtraInfo: true,
    isHiddenServiceDir: true,
    linkProtocolVersions: "1-5",
    circuitProtocolVersions: "1-2",
    tunnelledDirServer: true,
    confirmedFamilyMembers: [],
    verifiedHostNames: [],
    unverifiedHostNames: [],
    ...overrides,
})

export const makeRelayIdentifier = (overrides: Partial<RelayIdentifierDto> = {}): RelayIdentifierDto => ({
    id: 1,
    fingerprint: "0123456789ABCDEF0123456789ABCDEF01234567",
    nickname: "relay",
    ...overrides,
})

export const makeFamilyIdentifier = (overrides: Partial<RelayFamilyIdentifier> = {}): RelayFamilyIdentifier => ({
    id: 1,
    memberCount: 2,
    fingerprints: "AAAA,BBBB",
    nicknames: "alpha, bravo",
    autonomousSystems: "AS64500",
    ...overrides,
})
