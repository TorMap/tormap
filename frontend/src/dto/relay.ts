import {RelayFlag} from "../types/relay";

export interface RelayLocationDto {
    lat: number
    long: number
    country: string
    flags?: RelayFlag[] | null
    detailsId?: number | null
    familyId?: number | null
    nickname: string
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
