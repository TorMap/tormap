import {RelayLocationDto} from "../dto/relay";
import {RelayFlag, RelayFlags, RelayType} from "../types/relay";
import {Settings} from "../types/settings";
import {Statistics} from "../types/statistics";

/**
 * Returns a Array of GeoRelayViews that are filtered according to app settings.
 * @param relays - The relays to filter
 * @param settings - The app settings for filtering
 */
export const filterRelaysByFlags = (relays: RelayLocationDto[], settings: Settings): RelayLocationDto[] => {
    const filtered: RelayLocationDto[] = []
    relays.forEach(relay => {
        let relayMissesRequiredFlag = false
        RelayFlags.forEach(flag => {
                if (settings.relaysMustHaveFlag[flag] && !relay.flags?.includes(flag)) {
                    relayMissesRequiredFlag = true
                    return
                }
            }
        )
        const relayType = getRelayType(relay)
        if (!relayMissesRequiredFlag && settings.showRelayTypes[relayType]) {
            filtered.push(relay)
        }
    })
    // The limit belongs to the consensus weight setting and only applies while it is active
    return keepRelaysWithHighestWeight(filtered, settings.sizeByWeight ? settings.topRelayCount : undefined)
}

/**
 * Returns the `count` relays with the highest consensus weight. Relays are returned unchanged if no count is
 * given or if none of them has a weight (e.g. before the backend processed the weights of a day).
 * @param relays - The relays to select from
 * @param count - How many relays to keep, undefined to keep all
 */
export const keepRelaysWithHighestWeight = (relays: RelayLocationDto[], count: number | undefined): RelayLocationDto[] => {
    if (count === undefined || relays.length <= count) return relays
    if (!relays.some(relay => relay.consensusWeight != null)) return relays
    // Array.prototype.sort is stable, so relays with the same weight keep their order
    return [...relays]
        .sort((a, b) => (b.consensusWeight ?? -1) - (a.consensusWeight ?? -1))
        .slice(0, count)
}

/**
 * Returns a Key-Value-Map where Key is the coordinate pair as string and Value is a GeoRelayView[] with all Relays at this coordinate pair
 * @param relays - The relays
 */
export const buildRelayCoordinatesMap = (relays: RelayLocationDto[]): Map<string, RelayLocationDto[]> => {
    const latLonMap: Map<string, RelayLocationDto[]> = new Map<string, RelayLocationDto[]>()
    relays.forEach(relay => {
        const key: string = createLatLonKey(relay)
        const old = latLonMap.get(key)
        if (old) {
            old.push(relay)
            latLonMap.set(key, old)
        } else {
            latLonMap.set(key, [relay])
        }
    })
    return latLonMap
}

export const createLatLonKey = (relay: RelayLocationDto) => `${relay.lat},${relay.long}`

/**
 * Returns a Key-Value-Map where Key is the ID of a group (e.g. a family) and Value is a GeoRelayView[] with all Relays
 * that are part of this group. Relays without a group are left out.
 * @param relays - The relays
 * @param groupOf - The ID of the group a relay is part of
 */
export const buildRelayGroupMap = (
    relays: RelayLocationDto[],
    groupOf: (relay: RelayLocationDto) => number | null | undefined,
): Map<number, RelayLocationDto[]> => {
    const groupMap: Map<number, RelayLocationDto[]> = new Map<number, RelayLocationDto[]>()
    relays.forEach(relay => {
        const key = groupOf(relay)
        if (key) {
            const old = groupMap.get(key)
            if (old) {
                old.push(relay)
            } else {
                groupMap.set(key, [relay])
            }
        }
    })
    return groupMap
}

/**
 * Returns a Key-Value-Map where Key is the Family ID and Value is a GeoRelayView[] with all Relays that are part of this Family
 * @param relays - The relays
 */
export const buildRelayFamilyMap = (relays: RelayLocationDto[]): Map<number, RelayLocationDto[]> =>
    buildRelayGroupMap(relays, relay => relay.familyId)

/**
 * Returns a Key-Value-Map where Key is the number of an autonomous system and Value is a GeoRelayView[] with all Relays
 * located in it
 * @param relays - The relays
 */
export const buildRelayAutonomousSystemMap = (relays: RelayLocationDto[]): Map<number, RelayLocationDto[]> =>
    buildRelayGroupMap(relays, relay => relay.asNumber)

/**
 * Returns a Key-Value-Map where Key is the coordinate pair as string and Value is also a Key-Value-Map where Key is the group ID and value is GeoRelayView[] with all Relays that are part of this group
 * @param latLonMap - A LatLonMap to build the map on
 * @param buildGroupMap - Groups the relays at a coordinate, e.g. buildRelayFamilyMap
 */
export const buildGroupCoordinatesMap = (
    latLonMap: Map<string, RelayLocationDto[]>,
    buildGroupMap: (relays: RelayLocationDto[]) => Map<number, RelayLocationDto[]>,
): Map<string, Map<number, RelayLocationDto[]>> => {
    const groupCoordinatesMap: Map<string, Map<number, RelayLocationDto[]>> = new Map<string, Map<number, RelayLocationDto[]>>()
    latLonMap.forEach((relaysOnLocation, location) => {
        groupCoordinatesMap.set(location, buildGroupMap(relaysOnLocation))
    })
    return groupCoordinatesMap
}

/**
 * Returns a Key-Value-Map where Key is the coordinate pair as string and Value is an Array of RelayGroupLocations that are sorted for group size
 * The largest group on this coordinate is first
 * @param groupCoordinatesMap - The map to sort, as built by buildGroupCoordinatesMap
 */
export const sortGroupCoordinatesMap = (groupCoordinatesMap: Map<string, Map<number, RelayLocationDto[]>>): Map<string, RelayGroupLocation[]> => {
    const output: Map<string, RelayGroupLocation[]> = new Map<string, RelayGroupLocation[]>()
    // For each coordinate
    groupCoordinatesMap.forEach((groupMapForLocation, coordinateKey) => {
        const sorted: RelayGroupLocation[] = []
        // For each group at coordinate
        while (groupMapForLocation.size > 0) {
            let largest: RelayGroupLocation = {relays: [], groupId: 0, padding: 0}
            // Find largest
            groupMapForLocation.forEach((relays, groupId) => {
                if (relays.length > largest.relays.length) largest = {relays: relays, groupId: groupId, padding: 0}
            })
            // Add padding to all larger ones (so there are no markers with same size on same coordinate)
            if (sorted.length > 0) {
                sorted.map(value => value.padding = value.padding + sorted[sorted.length - 1].relays.length)
            }
            // Cleanup for next iteration
            sorted.push(largest)
            groupMapForLocation.delete(largest.groupId)
            largest = {relays: [], groupId: 0, padding: 0}
        }
        output.set(coordinateKey, sorted)
    })
    return output
}

export type RelayGroupLocation = {
    groupId: number,
    relays: RelayLocationDto[],
    padding: number
}

/**
 * Returns a Key-Value-Map where Key is the countries ISO-3166-A2 ID
 * @param relays - The relays
 */
export const buildRelayCountryMap = (relays: RelayLocationDto[]): Map<string, RelayLocationDto[]> => {
    const countryMap: Map<string, RelayLocationDto[]> = new Map<string, RelayLocationDto[]>()
    relays.forEach(relay => {
        if (relay.country !== undefined) {
            const key: string = relay.country
            const old = countryMap.get(key)
            if (old) {
                old.push(relay)
                countryMap.set(key, old)
            } else {
                countryMap.set(key, [relay])
            }
        }
    })
    return countryMap
}

export const buildFilteredRelays = (
    filteredRelaysByFlags: RelayLocationDto[],
    relayCountryMap: Map<string, RelayLocationDto[]>,
    relayFamilyMap: Map<number, RelayLocationDto[]>,
    settings: Settings,
    relayAutonomousSystemMap: Map<number, RelayLocationDto[]> = new Map(),
): RelayLocationDto[] => {
    const relays = filterByCountryAndFamily(filteredRelaysByFlags, relayCountryMap, relayFamilyMap, settings)
    const selectedAutonomousSystem = settings.selectedAutonomousSystem
    if (!selectedAutonomousSystem || !relayAutonomousSystemMap.has(selectedAutonomousSystem)) return relays
    if (relays === filteredRelaysByFlags) return relayAutonomousSystemMap.get(selectedAutonomousSystem) ?? []
    return relays.filter(relay => relay.asNumber === selectedAutonomousSystem)
}

const filterByCountryAndFamily = (
    filteredRelaysByFlags: RelayLocationDto[],
    relayCountryMap: Map<string, RelayLocationDto[]>,
    relayFamilyMap: Map<number, RelayLocationDto[]>,
    settings: Settings,
): RelayLocationDto[] => {
    if (settings.selectedCountry && settings.selectedFamily) {
        return relayCountryMap.get(settings.selectedCountry)?.filter(
            countryRelay => countryRelay.familyId === settings.selectedFamily
        ) ?? []
    } else if (settings.selectedCountry && relayCountryMap.has(settings.selectedCountry)) {
        return relayCountryMap.get(settings.selectedCountry) ?? []
    } else if (settings.selectedFamily && relayFamilyMap.has(settings.selectedFamily)) {
        return relayFamilyMap.get(settings.selectedFamily) ?? []
    }
    return filteredRelaysByFlags
}

export const buildStatistics = (
    filteredRelays: RelayLocationDto[],
    relayCountryMap: Map<string, RelayLocationDto[]>,
    relayFamilyMap: Map<number, RelayLocationDto[]>,
    settings: Settings,
    relayAutonomousSystemMap: Map<number, RelayLocationDto[]> = new Map(),
): Statistics => {
    let countryCount = relayCountryMap.size
    let familyCount = relayFamilyMap.size

    if (settings.selectedCountry && settings.selectedFamily) {
        countryCount = 1
        familyCount = 1
    } else if (settings.selectedCountry && relayCountryMap.has(settings.selectedCountry)) {
        countryCount = 1
        familyCount = new Set(filteredRelays.map(relay => relay.familyId)).size
    } else if (settings.selectedFamily && relayFamilyMap.has(settings.selectedFamily)) {
        familyCount = 1
        countryCount = new Set(filteredRelays.map(relay => relay.country)).size
    }
    // The relays of a selected autonomous system are only a part of the countries and families above
    if (settings.selectedAutonomousSystem && relayAutonomousSystemMap.has(settings.selectedAutonomousSystem)) {
        countryCount = new Set(filteredRelays.map(relay => relay.country)).size
        familyCount = buildRelayFamilyMap(filteredRelays).size
    }

    const stats: Statistics = {
        relayGuardCount: 0,
        relayExitCount: 0,
        relayOtherCount: 0,
        countryCount,
        familyCount,
        autonomousSystemCount: buildRelayAutonomousSystemMap(filteredRelays).size,
    }

    filteredRelays.forEach(relay => {
        const type = getRelayType(relay)
        switch (type) {
            case RelayType.Exit:
                return stats.relayExitCount++
            case RelayType.Guard:
                return stats.relayGuardCount++
            default:
                return stats.relayOtherCount++
        }
    })
    return stats
}

/**
 * Returns the type of a relay
 * @param relay - The Relay to identify
 */
export function getRelayType(relay: RelayLocationDto): RelayType {
    if (relay.flags?.includes(RelayFlag.Exit)) {
        return RelayType.Exit
    } else if (relay.flags?.includes(RelayFlag.Guard)) {
        return RelayType.Guard
    }
    return RelayType.Other
}

/**
 *
 * Returns a GeoRelayView-Object if there exists an Relay with this ID
 * @param id - The detailsID of the searched relay
 * @param relays - The GeoRelayView[] to be searched
 */
export function findRelayLocationById(id: number, relays: RelayLocationDto[]): RelayLocationDto | undefined {
    return relays.find((relay) => relay.detailsId === id)
}
