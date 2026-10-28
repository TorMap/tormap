import {AutonomousSystemDto, RelayLocationDto} from "../dto/relay";
import {RelayGroupSummary, summarizeRelays} from "./relay-group-summary";

/**
 * An autonomous system with what the map shows of it
 */
export interface AutonomousSystemRow extends RelayGroupSummary {
    number: number

    /**
     * Undefined if the backend does not know the name, e.g. while it is offline
     */
    name?: string
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
    return asNumbers.map(number => ({
        number,
        name: nameByNumber.get(number) ?? undefined,
        ...summarizeRelays(relayAutonomousSystemMap.get(number) ?? [], totalConsensusWeight),
    }))
}

/**
 * Whether a row matches a search for its name or number, e.g. "hetzner", "24940" or "AS24940"
 */
export const autonomousSystemMatches = (row: AutonomousSystemRow, search: string): boolean => {
    const term = search.trim().toLowerCase()
    if (!term) return true
    return (row.name?.toLowerCase().includes(term) ?? false) || `as${row.number}`.includes(term.replace(/^as\s*/, "as"))
}
