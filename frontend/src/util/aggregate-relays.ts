import {RelayLocationDto} from "../dto/relay";
import {RelayFlag, RelayFlags, RelayType} from "../types/relay";
import {Settings} from "../types/settings";
import {Statistics} from "../types/statistics";

/**
 * Returns a Array of GeoRelayViews that are filtered according to app settings.
 * @param relays - The relays to filter
 * @param settings - The app settings for filtering
 */
export const applyRelayFilter = (relays: RelayLocationDto[], settings: Settings): RelayLocationDto[] => {
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
    return filtered
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
 * Returns a Key-Value-Map where Key is the Family ID and Value is a GeoRelayView[] with all Relays that are part of this Family
 * @param relays - The relays
 */
export const buildRelayFamilyMap = (relays: RelayLocationDto[]): Map<number, RelayLocationDto[]> => {
    const familyMap: Map<number, RelayLocationDto[]> = new Map<number, RelayLocationDto[]>()
    relays.forEach(relay => {
        if (relay.familyId !== null) {
            const key = relay.familyId
            if (key) {
                const old = familyMap.get(key)
                if (old) {
                    old.push(relay)
                    familyMap.set(key, old)
                } else {
                    familyMap.set(key, [relay])
                }
            }
        }
    })
    return familyMap
}

/**
 * Returns a Key-Value-Map where Key is the coordinate pair as string and Value is also a Key-Value-Map where Key is the family ID and value is GeoRelayView[] with all Relays that are part of this Family
 * @param latLonMap - A LatLonMap to build the famCordMap on
 */
export const buildFamilyCoordinatesMap = (latLonMap: Map<string, RelayLocationDto[]>): Map<string, Map<number, RelayLocationDto[]>> => {
    const famCordMap: Map<string, Map<number, RelayLocationDto[]>> = new Map<string, Map<number, RelayLocationDto[]>>()
    latLonMap.forEach((relaysOnLocation, location) => {
        famCordMap.set(location, buildRelayFamilyMap(relaysOnLocation))
    })
    return famCordMap
}

/**
 * Returns a Key-Value-Map where Key is the coordinate pair as string and Value is an Array of famCordArr-Objects that are sorted for family size
 * The largest Family on this coordinate is first
 * @param famCordMap - The famCordMap-Object to sort
 */
export const sortFamilyCoordinatesMap = (famCordMap: Map<string, Map<number, RelayLocationDto[]>>): Map<string, RelayFamilyLocation[]> => {
    const output: Map<string, RelayFamilyLocation[]> = new Map<string, RelayFamilyLocation[]>()
    // For each coordinate
    famCordMap.forEach((famMapForLocation, coordinateKey) => {
        const sorted: RelayFamilyLocation[] = []
        // For each family at coordinate
        while (famMapForLocation.size > 0) {
            let largest: RelayFamilyLocation = {relays: [], familyId: 0, padding: 0}
            // Find largest
            famMapForLocation.forEach((relays, famID) => {
                if (relays.length > largest.relays.length) largest = {relays: relays, familyId: famID, padding: 0}
            })
            // Add padding to all larger ones (so there are no markers with same size on same coordinate)
            if (sorted.length > 0) {
                sorted.map(value => value.padding = value.padding + sorted[sorted.length - 1].relays.length)
            }
            // Cleanup for next iteration
            sorted.push(largest)
            famMapForLocation.delete(largest.familyId)
            largest = {relays: [], familyId: 0, padding: 0}
        }
        output.set(coordinateKey, sorted)
    })
    return output
}

export type RelayFamilyLocation = {
    familyId: number,
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

/**
 * Returns a Statistics-Object for given parameters
 * @param filteredRelays - The already filtered relays
 * @param relayCountryMap - A map of relays with the same country
 * @param relayFamilyMap - A map of relays with the same family
 * @param settings - Tha app settings
 */
export const buildStatistics = (
    filteredRelays: RelayLocationDto[],
    relayCountryMap: Map<string, RelayLocationDto[]>,
    relayFamilyMap: Map<number, RelayLocationDto[]>,
    settings: Settings
): Statistics => {
    let countryCount = relayCountryMap.size
    let familyCount = relayFamilyMap.size

    if (settings.selectedCountry && settings.selectedFamily) {
        filteredRelays = []
        relayFamilyMap.get(settings.selectedFamily)?.forEach(familyRelay => {
            if (settings.selectedCountry) {
                relayCountryMap.get(settings.selectedCountry)?.forEach(countryRelay => {
                    if (familyRelay.detailsId === countryRelay.detailsId) filteredRelays.push(familyRelay)
                })
            }
        })
        countryCount = 1
        familyCount = 1
    } else if (settings.selectedCountry && relayCountryMap.has(settings.selectedCountry)) {
        filteredRelays = relayCountryMap.get(settings.selectedCountry) ?? []
        countryCount = 1
        familyCount = new Set(filteredRelays.map(relay => relay.familyId)).size
    } else if (settings.selectedFamily && relayFamilyMap.has(settings.selectedFamily)) {
        filteredRelays = relayFamilyMap.get(settings.selectedFamily) ?? []
        familyCount = 1
        countryCount = new Set(filteredRelays.map(relay => relay.country)).size
    }

    const stats: Statistics = {
        relayGuardCount: 0,
        relayExitCount: 0,
        relayOtherCount: 0,
        countryCount,
        familyCount,
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
