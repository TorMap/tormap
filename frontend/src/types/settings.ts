import {RelayFlag, RelayType} from "./relay";

export interface Settings {
    // Grouping settings
    sortCountry: boolean
    selectedCountry: string | undefined
    sortFamily: boolean
    selectedFamily: number | undefined
    aggregateCoordinates: boolean
    heatMap: boolean

    // Relay flags
    showRelayTypes: Record<RelayType, boolean>
    relaysMustHaveFlag: Record<RelayFlag, boolean>
}

// Relay flag options to display. Tooltips according to https://github.com/torproject/torspec/blob/main/dir-spec.txt
export const relayMustIncludeFlagOptions: RelayMustIncludeFlagOption[] = [{
    relayFlag: RelayFlag.Authority,
    tooltip: "A router is called an 'Authority' if the authority generating the network-status document believes it is an authority."
}, {
    relayFlag: RelayFlag.BadExit,
    tooltip: "Any router that is determined to get the BadExit flag doesn't count into computing bandwidth weights."
}, {
    relayFlag: RelayFlag.Exit,
    tooltip: "A router is called an 'Exit' if it allows exits on both ports 80 and 443. (Up untilTor version 0.3.2, the flag was assigned if relays exit to at least two of the ports 80, 443, and 6667)."
}, {
    relayFlag: RelayFlag.Fast,
    tooltip: "A router is 'Fast' if it is active, and it's bandwidth is either in the top 7/8 ths for known active routers or at least 100KB/s fast."
}, {
    relayFlag: RelayFlag.Guard,
    tooltip: "A Guard router is a possible entry point to the network."
}, {
    relayFlag: RelayFlag.HSDir,
    tooltip: "A router is a v2 hidden service directory."
}, {
    relayFlag: RelayFlag.Named,
    tooltip: "Directory authorities no longer assign these flags. They were once used to determine whether a relay's nickname was canonically linked to its public key."
}, {
    relayFlag: RelayFlag.NoEdConsensus,
    tooltip: "Authorities should not vote on this flag. It is produced as part of the consensus for consensus method 22 or later."
}, {
    relayFlag: RelayFlag.Running,
    tooltip: "A router is 'Running' if the authority managed to connect to it successfully within the last 45 minutes on all its published ORPorts."
}, {
    relayFlag: RelayFlag.Stable,
    tooltip: "A router is Stable if it is active, and either it's Weighted MTBF is at least the median for known active routers or it's weighted MTBF corresponds to at least 7 days. Routers are never called Stable if they are running a version of Tor known to drop circuits stupidly."
}, {
    relayFlag: RelayFlag.StaleDesc,
    tooltip: "Authorities should vote to assign this flag if the published time on the descriptor is over 18 hours in the past. (This flag was added in 0.4.0.1-alpha.)"
}, {
    relayFlag: RelayFlag.Sybil,
    tooltip: "Authorities SHOULD NOT accept more than 2 relays on a single IP. If this happens, the authorities *should* vote for the excess relays, but should omit the Running or Valid flags and instead should assign the flag Sybil."
}, {
    relayFlag: RelayFlag.Unnamed,
    tooltip: "Directory authorities no longer assign these flags. They were once used to determine whether a relay's nickname was canonically linked to its public key."
}, {
    relayFlag: RelayFlag.Valid,
    tooltip: "A router is 'Valid' if it is running a version of Tor not known to be broken, and the directory authorities have not blacklisted it as suspicious."
}, {
    relayFlag: RelayFlag.V2Dir,
    tooltip: "A router supports the v2 directory protocol."
}]

interface RelayMustIncludeFlagOption {
    relayFlag: RelayFlag,
    tooltip: string,
}