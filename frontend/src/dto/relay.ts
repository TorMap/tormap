import {RelayFlag} from "../types/relay";

/**
 * A relay of a day as API v1 returns it (`/v1/days/{day}/relays`, see api/openapi-v1.json)
 */
export interface RelayV1Dto {
    fingerprint: string
    /**
     * Missing if the relay has no details (server descriptor) in that month
     */
    nickname?: string | null
    country: string
    lat: number
    long: number
    flags: string[]
    consensusWeight?: number | null
    autonomousSystemNumber?: number | null
    familyId?: number | null
}

/**
 * A relay of the selected day, as the map uses it. Built from a [RelayV1Dto] by `toRelayLocation`.
 */
export interface RelayLocationDto {
    lat: number
    long: number
    country: string
    flags?: RelayFlag[] | null
    /**
     * The identity of the relay in Tor. Its details are requested by fingerprint and month.
     */
    fingerprint: string
    /**
     * Whether the relay has details in the month of the day. Relays without details can not be shown in the dialog.
     */
    hasDetails: boolean
    familyId?: number | null
    /**
     * Empty if the relay has no details
     */
    nickname: string

    /**
     * The weight clients use for path selection. Not a measured bandwidth. Missing until the day got reprocessed.
     */
    consensusWeight?: number | null

    /**
     * The number of the autonomous system the relay is located in. Missing for unknown systems.
     */
    asNumber?: number | null
}

export interface AutonomousSystemDto {
    number: number
    name?: string | null
}

export interface CountryUsageDto {
    /**
     * ISO 3166-1 alpha-2 code in upper case
     */
    country: string
    users: number
    lower?: number | null
    upper?: number | null

    /**
     * The percentage of the network that contributed to the estimate of the day. Low values are less reliable.
     */
    frac: number
}

export interface DailyRelayCountDto {
    day: string
    total: number
    exit: number
    guard: number
    other: number
}

/**
 * The details of a relay in a month, as API v1 returns them (`/v1/relays/{fingerprint}?month=`)
 */
export interface RelayDetailsDto {
    month: string
    day: string
    address: string
    autonomousSystemName: string
    autonomousSystemNumber?: number | null
    allowSingleHopExits: boolean
    nickname: string
    bandwidthRate: number
    bandwidthBurst: number
    bandwidthObserved: number
    platform: string
    protocols: string
    fingerprint: string
    hibernating: boolean
    uptime: number
    contact: string
    familyEntries: string
    familyKeys: string[]
    familyId?: number | null
    cachesExtraInfo: boolean
    hiddenServiceDir: boolean
    linkProtocolVersions: string
    circuitProtocolVersions: string
    tunnelledDirServer: boolean
    confirmedFamilyMembers: RelayIdentifierDto[]
    verifiedHostNames: string[]
    unverifiedHostNames: string[]
}

export interface RelayIdentifierDto {
    fingerprint: string
    nickname: string
}

/**
 * A family of a month, as API v1 returns it (`/v1/families?month=`)
 */
export interface FamilyV1Dto {
    id: number
    members: RelayIdentifierDto[]
    autonomousSystems: AutonomousSystemDto[]
}

/**
 * A family as the family dialog shows it. Built from a [FamilyV1Dto] by `toFamilyIdentifier`.
 */
export interface RelayFamilyIdentifier {
    id: number
    memberCount: number
    nicknames: string
    autonomousSystems?: string
}
