import {RelayFlag} from "./relay";

export interface GeoRelayView {
    lat: number
    long: number
    country: string
    flags?: RelayFlag[]
    detailsId: string
    familyId: number
}

export interface NodeDetails {
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

export interface NodeDetailsInfo {
    name: string
    value: string | number | undefined
}

export interface NodeIdentifier {
    id: string,
    fingerprint: string
    nickname: string
}