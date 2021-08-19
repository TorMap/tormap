import {GeoRelayView} from "../types/geo-relay";
import {Settings, Statistics} from "../types/variousTypes";
import {RelayFlag, RelayType} from "../types/relay";

/**
 * Returns a Array of GeoRelayViews that are filtered according to app settings.
 * @param relays Relays to filter
 * @param settings App settings
 */
export const applyFilter = (relays: GeoRelayView[], settings: Settings): GeoRelayView[] => {
    let filtered: GeoRelayView[] = []
    relays.forEach(relay => {
        //Filter must include settings
        if (settings.miValid && !relay.flags?.includes(RelayFlag.Valid)) {
            return
        }
        if (settings.miNamed && !relay.flags?.includes(RelayFlag.Named)) {
            return
        }
        if (settings.miUnnamed && !relay.flags?.includes(RelayFlag.Unnamed)) {
            return
        }
        if (settings.miRunning && !relay.flags?.includes(RelayFlag.Running)) {
            return
        }
        if (settings.miStable && !relay.flags?.includes(RelayFlag.Stable)) {
            return
        }
        if (settings.miExit && !relay.flags?.includes(RelayFlag.Exit)) {
            return
        }
        if (settings.miFast && !relay.flags?.includes(RelayFlag.Fast)) {
            return
        }
        if (settings.miGuard && !relay.flags?.includes(RelayFlag.Guard)) {
            return
        }
        if (settings.miAuthority && !relay.flags?.includes(RelayFlag.Authority)) {
            return
        }
        if (settings.miV2Dir && !relay.flags?.includes(RelayFlag.V2Dir)) {
            return
        }
        if (settings.miHSDir && !relay.flags?.includes(RelayFlag.HSDir)) {
            return
        }
        if (settings.miNoEdConsensus && !relay.flags?.includes(RelayFlag.NoEdConsensus)) {
            return
        }
        if (settings.miStaleDesc && !relay.flags?.includes(RelayFlag.StaleDesc)) {
            return
        }
        if (settings.miSybil && !relay.flags?.includes(RelayFlag.Sybil)) {
            return
        }
        if (settings.miBadExit && !relay.flags?.includes(RelayFlag.BadExit)) {
            return
        }

        //Filter relay types
        if (!settings.Exit && relay.flags?.includes(RelayFlag.Exit)) {
            return
        }
        if (!settings.Guard && (relay.flags?.includes(RelayFlag.Guard))
            && !(relay.flags?.includes(RelayFlag.Exit))) {
            return
        }
        if (!settings.Default && (!relay.flags?.includes(RelayFlag.Guard)
            && !relay.flags?.includes(RelayFlag.Exit))) {
            return
        }
        filtered.push(relay)
    })
    return filtered
}

// Map for coordinate's, used to get an Array of GeoRelayView with relays on the same coordinate
//todo: remove settings
/**
 * Returns a Key-Value-Map where Key is the coordinate pair as string and Value is a GeoRelayView[] with all Relays at this coordinate pair
 * @param relays Relays
 * @param settings
 */
export const getLatLonMap = (
    relays: GeoRelayView[], settings: Settings
): Map<string, GeoRelayView[]> => {
    let latLonMap: Map<string, GeoRelayView[]> = new Map<string, GeoRelayView[]>()
    //if (settings.aggregateCoordinates || settings.heatMap || settings.sortFamily) {
        relays.forEach(relay => {
            const key: string = `${relay.lat},${relay.long}`
            if (latLonMap.has(key)) {
                let old = latLonMap.get(key)!!
                old.push(relay)
                latLonMap.set(key, old)
            } else {
                latLonMap.set(key, [relay])
            }
        })
    //}
    return latLonMap
}

//Map for family's, used to get an Array of GeoRelayView with relays in the same family / autonomsystem
//todo: remove settings
/**
 * Returns a Key-Value-Map where Key is the Family ID and Value is a GeoRelayView[] with all Relays that are part of this Family
 * @param relays Relays
 * @param settings
 */
export const getFamilyMap = (
    relays: GeoRelayView[],
    settings: Settings,
): Map<number, GeoRelayView[]> => {
    let familyMap: Map<number, GeoRelayView[]> = new Map<number, GeoRelayView[]>()
    // true for forcing the calculation to include it in statistics
    //if (settings.sortFamily) {
        relays.forEach(relay => {
            if (relay.familyId !== null) {
                const key: number = relay.familyId
                if (familyMap.has(key)) {
                    let old = familyMap.get(key)!!
                    old.push(relay)
                    familyMap.set(key, old)
                } else {
                    familyMap.set(key, [relay])
                }
            }
        })

    //}
    return familyMap
}

/**
 * Returns a Key-Value-Map where Key is the coordinate pair as string and Value is also a Key-Value-Map where Key is the family ID and value is GeoRelayView[] with all Relays that are part of this Family
 * @param latLonMap
 */
export const getFamCordMap = (
    latLonMap: Map<string, GeoRelayView[]>,
): Map<string, Map<number, GeoRelayView[]>> => {
    let famCordMap: Map<string, Map<number, GeoRelayView[]>> = new Map<string, Map<number, GeoRelayView[]>>()
    latLonMap.forEach((relaysOnLocation, location) => {
        let famMapforLocation: Map<number, GeoRelayView[]> = new Map<number, GeoRelayView[]>()
        relaysOnLocation.forEach(relay => {
            if (relay.familyId !== null) {
                const key: number = relay.familyId
                if (famMapforLocation.has(key)) {
                    let old = famMapforLocation.get(key)!!
                    old.push(relay)
                    famMapforLocation.set(key, old)
                } else {
                    famMapforLocation.set(key, [relay])
                }
            }
        })
        famCordMap.set(location, famMapforLocation)
    })
    return famCordMap
}

/**
 * Returns a Key-Value-Map where Key is the coordinate pair as string and Value is an Array of famCordArr-Objects that are sorted for family size
 * @param input
 */
export const sortFamCordMap = (
    famCordMap: Map<string, Map<number, GeoRelayView[]>>
): Map<string, famCordArr[]> => {
    let output: Map<string, famCordArr[]> = new Map<string, famCordArr[]>()
    famCordMap.forEach((famMapForLocation, coordinateKey) => {
        let sorted: famCordArr[] = []
        let largest: famCordArr = {relays: [], familyID: 0, padding: 0}
        while (famMapForLocation.size > 0) {
            famMapForLocation.forEach((relays, famID) => {
                if (relays.length > largest.relays.length) largest = {relays: relays, familyID: famID, padding: 0}
            })
            if (sorted.length > 0 && largest.relays.length === sorted[sorted.length - 1].relays.length) {
                sorted.map(value => {
                    value.padding = value.padding + 1
                })
            }
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

//todo: remove settings
/**
 * Returns a Key-Value-Map where Key is the countries ISO-2 ID
 * @param relays Relays
 * @param settings
 * @param setSettingsCallback
 */
export const getCountryMap = (
    relays: GeoRelayView[],
    settings: Settings,
    setSettingsCallback: (s: Settings) => void
): Map<string, GeoRelayView[]> => {
    let countryMap: Map<string, GeoRelayView[]> = new Map<string, GeoRelayView[]>()
    if (settings.sortCountry) {
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
        if (settings.selectedCountry && !countryMap.has(settings.selectedCountry)) {
            setSettingsCallback({...settings, selectedCountry: undefined})
        }
    }
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
        switch (type){
            case RelayType.Exit: return stats.relayExitCount++
            case RelayType.Guard: return stats.relayGuardCount++
            default: return stats.relayOtherCount++
        }
    })
    return stats
}

/**
 * Returns the type of the relay
 * @param relay
 */
export function getRelayType(relay: GeoRelayView | undefined): RelayType | undefined {
    if (relay === undefined) return undefined
    if (relay.flags?.includes(RelayFlag.Exit)) {
        return RelayType.Exit
    } else if (relay.flags?.includes(RelayFlag.Guard)) {
        return RelayType.Guard
    } else {
        return RelayType.default
    }
}

/**
 *
 * Returns a GeoRelayView-Object if there exists an Relay with this ID
 * @param id detailsID of the relay
 * @param relays Relays to be searched
 */
export function findRelayByID(id: string | number, relays: GeoRelayView[]): GeoRelayView | undefined {
    //id has to be of type string
    if (typeof id === "number"){
        id = id.toString()
    }
    const relay = relays.find((relay) => relay.detailsId == id)
    return relay
}