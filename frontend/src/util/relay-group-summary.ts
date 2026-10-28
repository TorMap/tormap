import {RelayLocationDto} from "../dto/relay";
import {RelayType} from "../types/relay";
import {getRelayType} from "./aggregate-relays";

/**
 * What the map shows of a group of relays, e.g. a family or an autonomous system
 */
export interface RelayGroupSummary {
    relayCount: number
    exitCount: number
    countryCount: number

    /**
     * The summed consensus weight of the relays, undefined if none of them has a weight
     */
    consensusWeight?: number

    /**
     * The share of the consensus weight of all relays on the map, undefined without weights
     */
    consensusWeightShare?: number
}

/**
 * Summarizes a group of relays for a selection table
 * @param relays - The relays of the group
 * @param totalConsensusWeight - The consensus weight of all relays on the map, which the share is relative to
 */
export const summarizeRelays = (relays: RelayLocationDto[], totalConsensusWeight: number): RelayGroupSummary => {
    const hasWeight = relays.some(relay => relay.consensusWeight != null)
    const consensusWeight = hasWeight
        ? relays.reduce((sum, relay) => sum + (relay.consensusWeight ?? 0), 0)
        : undefined
    return {
        relayCount: relays.length,
        exitCount: relays.filter(relay => getRelayType(relay) === RelayType.Exit).length,
        countryCount: new Set(relays.map(relay => relay.country)).size,
        consensusWeight,
        consensusWeightShare: consensusWeight !== undefined && totalConsensusWeight > 0
            ? consensusWeight / totalConsensusWeight
            : undefined,
    }
}

/**
 * Explains the consensus weight column of the selection tables
 */
export const consensusWeightColumnTooltip = (group: string) =>
    `The share of the consensus weight of all relays on the map. It shows how likely Tor clients pick a relay of this ${group}.`
