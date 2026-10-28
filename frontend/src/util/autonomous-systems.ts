import {AutonomousSystemDto, RelayLocationDto} from "../dto/relay";
import {RelayType} from "../types/relay";
import {getRelayType} from "./aggregate-relays";

/**
 * An autonomous system with what the map shows of it
 */
export interface AutonomousSystemRow {
    number: number

    /**
     * Undefined if the backend does not know the name, e.g. while it is offline
     */
    name?: string

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
 * Summarizes the relays of each given autonomous system for the selection table
 * @param asNumbers - The autonomous systems to summarize
 * @param relayAutonomousSystemMap - The relays of each autonomous system
 * @param totalConsensusWeight - The consensus weight of all relays on the map, which the shares are relative to
 * @param names - The names of the autonomous systems, as far as known
 */
export const buildAutonomousSystemRows = (
    asNumbers: number[],
    relayAutonomousSystemMap: Map<number, RelayLocationDto[]>,
    totalConsensusWeight: number,
    names: AutonomousSystemDto[] = [],
): AutonomousSystemRow[] => {
    const nameByNumber = new Map(names.map(autonomousSystem => [autonomousSystem.number, autonomousSystem.name]))
    return asNumbers.map(number => {
        const relays = relayAutonomousSystemMap.get(number) ?? []
        const hasWeight = relays.some(relay => relay.consensusWeight != null)
        const consensusWeight = hasWeight
            ? relays.reduce((sum, relay) => sum + (relay.consensusWeight ?? 0), 0)
            : undefined
        return {
            number,
            name: nameByNumber.get(number) ?? undefined,
            relayCount: relays.length,
            exitCount: relays.filter(relay => getRelayType(relay) === RelayType.Exit).length,
            countryCount: new Set(relays.map(relay => relay.country)).size,
            consensusWeight,
            consensusWeightShare: consensusWeight !== undefined && totalConsensusWeight > 0
                ? consensusWeight / totalConsensusWeight
                : undefined,
        }
    })
}

/**
 * Whether a row matches a search for its name or number, e.g. "hetzner", "24940" or "AS24940"
 */
export const autonomousSystemMatches = (row: AutonomousSystemRow, search: string): boolean => {
    const term = search.trim().toLowerCase()
    if (!term) return true
    return (row.name?.toLowerCase().includes(term) ?? false) || `as${row.number}`.includes(term.replace(/^as\s*/, "as"))
}
