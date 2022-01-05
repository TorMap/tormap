import {RelayFlag} from "../types/relay";
import {ReactJSXElement} from "@emotion/react/types/jsx-namespace";

export interface RelayLocationDto {
    lat: number
    long: number
    country: string
    flags?: RelayFlag[] | null
    detailsId?: number | null
    familyId?: number | null
}

export interface RelayDetailsDto {
    id: number
    month: string
    day: string
    address: string
    autonomousSystemName: string
    autonomousSystemNumber: string
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
    cachesExtraInfo: boolean
    isHiddenServiceDir: boolean
    linkProtocolVersions: string
    circuitProtocolVersions: string
    tunnelledDirServer: boolean
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
