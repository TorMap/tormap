// noinspection JSUnusedGlobalSymbols

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

