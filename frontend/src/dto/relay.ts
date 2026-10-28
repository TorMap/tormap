import {RelayFlag} from "../types/relay";

export interface RelayLocationDto {
    lat: number
    long: number
    country: string
    flags?: RelayFlag[] | null
    detailsId?: number | null
    familyId?: number | null
    nickname: string

    /**
     * The weight clients use for path selection. Not a measured bandwidth. Missing until the day got reprocessed.
     */
    consensusWeight?: number | null

    /**
     * The number of the autonomous system the relay is located in. Missing for unknown systems and older responses.
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

export interface RelayDetailsDto {
    id: number
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
    isHibernating: boolean
    uptime: number
    contact: string
    familyEntries: string
    familyId?: number | null
    cachesExtraInfo: boolean
    isHiddenServiceDir: boolean
    linkProtocolVersions: string
    circuitProtocolVersions: string
    tunnelledDirServer: boolean
    confirmedFamilyMembers: RelayIdentifierDto[]
    verifiedHostNames: string[]
    unverifiedHostNames: string[]
}

export interface RelayIdentifierDto {
    id: number
    fingerprint: string
    nickname: string
}

export interface RelayFamilyIdentifier {
    id: number
    memberCount: number
    fingerprints: string
    nicknames: string
    autonomousSystems?: string
}
