// Order of flags must be kept in sync with backend
import {RelayDetailsDto, RelayIdentifierDto, RelayLocationDto} from "../dto/relay";

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

export const RelayTypeTooltip: Record<RelayType, string> = {
    [RelayType.Exit]: "Shows relays who have an 'Exit' flag",
    [RelayType.Guard]: "Shows relays who have a 'Guard' but no 'Exit' flag",
    [RelayType.Other]: "Shows relays who have neither a 'Guard' nor 'Exit' flag",
}

export interface RelayMatch extends RelayIdentifierDto, RelayLocationDto {
    relayType: RelayType
}

export interface RelayDetailsMatch extends RelayDetailsDto, RelayLocationDto {
    relayType: RelayType
}