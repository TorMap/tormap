import {Settings, Statistics} from "../types/app-state";
import {RelayFlag, RelayFlags, RelayType} from "../types/relay";
import {GeoRelayView} from "../types/responses";

/**
 * Returns a Array of GeoRelayViews that are filtered according to app settings.
 * @param relays Relays to filter
 * @param settings App settings
 */
export const applyFilter = (relays: GeoRelayView[], settings: Settings): GeoRelayView[] => {
    let filtered: GeoRelayView[] = []
    relays.forEach(relay => {
        // Filter relay flags
        let relayMissesRequiredFlag = false
        RelayFlags.forEach(flag => {
                if (settings.relaysMustIncludeFlag[flag] && !relay.flags?.includes(flag)) {
                    relayMissesRequiredFlag = true
                    return
                }
            }
        )
        if (relayMissesRequiredFlag) {
            return
        }

        // Filter relay types
        const relayType = getRelayType(relay)
        if (!settings.showRelayTypes[RelayType.Exit] && relayType === RelayType.Exit) {
            return
        }
        if (!settings.showRelayTypes[RelayType.Guard] && relayType === RelayType.Guard) {
            return
        }
        if (!settings.showRelayTypes[RelayType.Other] && relayType === RelayType.Other) {
            return
        }
        filtered.push(relay)
    })
    return filtered
}

/**
 * Returns a Key-Value-Map where Key is the coordinate pair as string and Value is a GeoRelayView[] with all Relays at this coordinate pair
 * @param relays Relays
 */
export const buildLatLonMap = (relays: GeoRelayView[]): Map<string, GeoRelayView[]> => {
    let latLonMap: Map<string, GeoRelayView[]> = new Map<string, GeoRelayView[]>()
    relays.forEach(relay => {
        const key: string = createLatLonKey(relay)
        if (latLonMap.has(key)) {
            let old = latLonMap.get(key)!!
            old.push(relay)
            latLonMap.set(key, old)
        } else {
            latLonMap.set(key, [relay])
        }
    })
    return latLonMap
}

export const createLatLonKey = (relay: GeoRelayView) => `${relay.lat},${relay.long}`

/**
 * Returns a Key-Value-Map where Key is the Family ID and Value is a GeoRelayView[] with all Relays that are part of this Family
 * @param relays Relays
 */
export const buildFamilyMap = (relays: GeoRelayView[]): Map<number, GeoRelayView[]> => {
    let familyMap: Map<number, GeoRelayView[]> = new Map<number, GeoRelayView[]>()
    relays.forEach(relay => {
        if (relay.familyId !== null) {
            const key = relay.familyId
            if (key) {
                if (familyMap.has(key)) {
                    let old = familyMap.get(key)!!
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
 * @param latLonMap
 */
export const buildFamilyCoordinatesMap = (latLonMap: Map<string, GeoRelayView[]>): Map<string, Map<number, GeoRelayView[]>> => {
    let famCordMap: Map<string, Map<number, GeoRelayView[]>> = new Map<string, Map<number, GeoRelayView[]>>()
    latLonMap.forEach((relaysOnLocation, location) => {
        famCordMap.set(location, buildFamilyMap(relaysOnLocation))
    })
    return famCordMap
}

/**
 * Returns a Key-Value-Map where Key is the coordinate pair as string and Value is an Array of famCordArr-Objects that are sorted for family size
 * The largest Family on this coordinate is first
 * @param famCordMap famCordMap-Object to sort
 */
export const sortFamilyCoordinatesMap = (famCordMap: Map<string, Map<number, GeoRelayView[]>>): Map<string, famCordArr[]> => {
    let output: Map<string, famCordArr[]> = new Map<string, famCordArr[]>()
    // For each coordinate
    famCordMap.forEach((famMapForLocation, coordinateKey) => {
        let sorted: famCordArr[] = []
        // For each family at coordinate
        while (famMapForLocation.size > 0) {
            let largest: famCordArr = {relays: [], familyID: 0, padding: 0}
            // Find largest
            famMapForLocation.forEach((relays, famID) => {
                if (relays.length > largest.relays.length) largest = {relays: relays, familyID: famID, padding: 0}
            })
            // Add padding to all larger ones (so there are no markers with same size on same coordinate)
            if (sorted.length > 0) {
                sorted.map(value => value.padding = value.padding + sorted[sorted.length - 1].relays.length)
            }
            // Cleanup for next iteration
            sorted.push(largest)
            famMapForLocation.delete(largest.familyID)
            largest = {relays: [], familyID: 0, padding: 0}
        }
        output.set(coordinateKey, sorted)
    })
    return output
}

export type famCordArr = {
    familyID: number,
    relays: GeoRelayView[],
    padding: number
}

/**
 * Returns a Key-Value-Map where Key is the countries ISO-3166-A2 ID
 * @param relays Relays
 */
export const getCountryMap = (relays: GeoRelayView[]): Map<string, GeoRelayView[]> => {
    let countryMap: Map<string, GeoRelayView[]> = new Map<string, GeoRelayView[]>()
    relays.forEach(relay => {
        if (relay.country !== undefined) {
            const key: string = relay.country
            if (countryMap.has(key)) {
                let old = countryMap.get(key)!!
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
 * @param relays
 * @param countryMap
 * @param familyMap
 * @param settings App settings
 */
export const calculateStatistics = (
    relays: GeoRelayView[],
    countryMap: Map<string, GeoRelayView[]>,
    familyMap: Map<number, GeoRelayView[]>,
    settings: Settings
): Statistics => {
    let stats: Statistics = {
        relayGuardCount: 0,
        relayExitCount: 0,
        relayOtherCount: 0,
    }
    if (!settings.selectedCountry && !settings.selectedFamily) {
        stats.countryCount = countryMap.size
        stats.familyCount = familyMap.size
    }
    relays.forEach(relay => {
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
 * @param relay
 */
export function getRelayType(relay?: GeoRelayView): RelayType | undefined {
    if (relay === undefined) return undefined
    if (relay.flags?.includes(RelayFlag.Exit)) {
        return RelayType.Exit
    } else if (relay.flags?.includes(RelayFlag.Guard)) {
        return RelayType.Guard
    } else {
        return RelayType.Other
    }
}

/**
 *
 * Returns a GeoRelayView-Object if there exists an Relay with this ID
 * @param id detailsID of the relay
 * @param relays GeoRelayView[] to be searched
 */
export function findGeoRelayViewByID(id: number, relays: GeoRelayView[]): GeoRelayView | undefined {
    return relays.find((relay) => relay.detailsId === id)
}
