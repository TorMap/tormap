// noinspection JSUnusedGlobalSymbols

export interface NodeDetails {
    id: number
    month: string,
    day: string,
    address: string
    allowSingleHopExits: Boolean
    nickname: string
    bandwidthRate: number
    bandwidthBurst: number
    bandwidthObserved: number
    platform: string
    protocols: string
    fingerprint: string
    isHibernating: Boolean
    uptime: number
    contact: string
    familyEntries: string
    cachesExtraInfo: Boolean
    isHiddenServiceDir: Boolean
    linkProtocolVersions: string
    circuitProtocolVersions: string
    tunnelledDirServer: Boolean
}
