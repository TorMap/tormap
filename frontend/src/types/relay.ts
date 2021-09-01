// noinspection JSUnusedGlobalSymbols

export interface RelayView {
    id: string
    firstSeen: string
    lastSeen: string
    lat: number
    long: number
    flags?: RelayFlag[]
}

// Order of flags must be kept in sync with backend
export enum RelayFlag {
    Valid,
    Named,
    Unnamed,
    Running,
    Stable,
    Exit,
    Fast,
    Guard,
    Authority,
    V2Dir,
    HSDir,
    NoEdConsensus,
    StaleDesc,
    Sybil,
    BadExit,
}

export const RelayFlags: RelayFlag[] = [
    RelayFlag.Valid,
    RelayFlag.Named,
    RelayFlag.Unnamed,
    RelayFlag.Running,
    RelayFlag.Stable,
    RelayFlag.Exit,
    RelayFlag.Fast,
    RelayFlag.Guard,
    RelayFlag.Authority,
    RelayFlag.V2Dir,
    RelayFlag.HSDir,
    RelayFlag.NoEdConsensus,
    RelayFlag.StaleDesc,
    RelayFlag.Sybil,
    RelayFlag.BadExit,
]

export const RelayFlagLabel: Record<RelayFlag, string> = {
    [RelayFlag.Valid]: "Valid",
    [RelayFlag.Named]: "Named",
    [RelayFlag.Unnamed]: "Unnamed",
    [RelayFlag.Running]: "Running",
    [RelayFlag.Stable]: "Stable",
    [RelayFlag.Exit]: "Exit",
    [RelayFlag.Fast]: "Fast",
    [RelayFlag.Guard]: "Guard",
    [RelayFlag.Authority]: "Authority",
    [RelayFlag.V2Dir]: "V2Dir",
    [RelayFlag.HSDir]: "HSDir",
    [RelayFlag.NoEdConsensus]: "NoEdConsensus",
    [RelayFlag.StaleDesc]: "StaleDesc",
    [RelayFlag.Sybil]: "Sybil",
    [RelayFlag.BadExit]: "BadExit",
}

export enum RelayType {
    Exit,
    Guard,
    Other,
}

export const RelayTypeLabel: Record<RelayType, string> = {
    [RelayType.Exit]: "Exit",
    [RelayType.Guard]: "Guard",
    [RelayType.Other]: "Other",
}

export interface Relay {
    fingerprint: string
    id: number
    nickname: string
    as?: string
    as_name?: string
    or_addresses: Array<string>
    exit_addresses?: Array<string>
    verified_host_names?: Array<string>
    exit_policy?: Array<string>
    exit_policy_summary?: ExitPolicySummary
    exit_policy_v6_summary?: ExitPolicySummary
    effective_family?: Array<string>
    alleged_family?: Array<string>
    indirect_family?: Array<string>
    unverified_host_names?: Array<string>
    unreachable_or_addresses?: Array<string>
    dir_address?: string
    last_changed_address_or_port: string
    consensus_weight: number
    flags?: Array<string>
    first_seen: string
    running: boolean
    last_seen: string
    last_restarted?: string
    bandwidth_rate?: number
    bandwidth_burst?: number
    observed_bandwidth?: number
    advertised_bandwidth?: number
    platform?: string
    version?: string
    version_status?: string
    recommended_version?: boolean
    measured?: boolean
    contact?: string
    consensus_weight_fraction?: number
    guard_probability?: number
    middle_probability?: number
    exit_probability?: number
    hibernating?: boolean
    country?: string
    country_name?: string
    region_name?: string
    city_name?: string
    latitude?: number
    longitude?: number
}

export interface ExitPolicySummary {
    id: number
    reject?: Array<String>
    accept?: Array<String>
}
