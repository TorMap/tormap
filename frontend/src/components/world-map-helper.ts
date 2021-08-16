//Helper for relaysToLayerGroup, applys all Filters to the downloaded data
import {GeoRelayView} from "../types/geo-relay";
import {RelayFlag} from "../types/relay";
import {Feature, GeoJsonProperties, GeometryObject} from "geojson";
import {Layer} from "leaflet";
import {Settings} from "../types/variousTypes";

export const applyFilter = (relays: GeoRelayView[], settings: Settings): GeoRelayView[] => {
    let filtered: GeoRelayView[] = []
    relays.forEach(relay => {
        //Filter must include settings
        if (settings.miValid &&         !relay.flags?.includes(RelayFlag.Valid))        {return}
        if (settings.miNamed &&         !relay.flags?.includes(RelayFlag.Named))        {return}
        if (settings.miUnnamed &&       !relay.flags?.includes(RelayFlag.Unnamed))      {return}
        if (settings.miRunning &&       !relay.flags?.includes(RelayFlag.Running))      {return}
        if (settings.miStable &&        !relay.flags?.includes(RelayFlag.Stable))       {return}
        if (settings.miExit &&          !relay.flags?.includes(RelayFlag.Exit))         {return}
        if (settings.miFast &&          !relay.flags?.includes(RelayFlag.Fast))         {return}
        if (settings.miGuard &&         !relay.flags?.includes(RelayFlag.Guard))        {return}
        if (settings.miAuthority &&     !relay.flags?.includes(RelayFlag.Authority))    {return}
        if (settings.miV2Dir &&         !relay.flags?.includes(RelayFlag.V2Dir))        {return}
        if (settings.miHSDir &&         !relay.flags?.includes(RelayFlag.HSDir))        {return}
        if (settings.miNoEdConsensus && !relay.flags?.includes(RelayFlag.NoEdConsensus)){return}
        if (settings.miStaleDesc &&     !relay.flags?.includes(RelayFlag.StaleDesc))    {return}
        if (settings.miSybil &&         !relay.flags?.includes(RelayFlag.Sybil))        {return}
        if (settings.miBadExit &&       !relay.flags?.includes(RelayFlag.BadExit))      {return}

        //Filter relay types
        if (!settings.Exit &&           relay.flags?.includes(RelayFlag.Exit))          {return}
        if (!settings.Guard &&          (relay.flags?.includes(RelayFlag.Guard))
            && !(relay.flags?.includes(RelayFlag.Exit)))    {return}
        if (!settings.Default &&        (!relay.flags?.includes(RelayFlag.Guard)
            && !relay.flags?.includes(RelayFlag.Exit)))     {return}
        filtered.push(relay)
    })
    return filtered
}

//Helper for relaysToLayerGroup, used for adding eventlisteners to the countries
export const onEachFeature = (
    feature: Feature<GeometryObject,
    GeoJsonProperties>,
    layer: Layer,
    settings: Settings,
    setSettingsCallback: (s: Settings) => void
) => {
    layer.on({
        click: () => {
            if (feature.properties!!.iso_a2 !== settings.selectedCountry)
                setSettingsCallback({...settings, selectedCountry: feature.properties?.iso_a2})
            else setSettingsCallback({...settings, selectedCountry: undefined})
        }
    });
}


// Map for coordinate's, used to get an Array of GeoRelayView with relays on the same coordinate
export const getLatLonMap = (
    relays: GeoRelayView[], settings: Settings
): Map<string, GeoRelayView[]> => {
    let latLonMap: Map<string, GeoRelayView[]> = new Map<string, GeoRelayView[]>()
    if (settings.aggregateCoordinates || settings.heatMap || settings.sortFamily){
        relays.forEach(relay => {
            const key: string = `${relay.lat},${relay.long}`
            if (latLonMap.has(key)){
                let old = latLonMap.get(key)!!
                old.push(relay)
                latLonMap.set(key, old)
            }else{
                latLonMap.set(key, [relay])
            }
        })
    }
    return latLonMap
}

//Map for family's, used to get an Array of GeoRelayView with relays in the same family / autonomsystem
export const getFamilyMap = (
    relays: GeoRelayView[],
    settings: Settings,
): Map<number, GeoRelayView[]> => {
    let familyMap: Map<number, GeoRelayView[]> = new Map<number, GeoRelayView[]>()
    // true for forcing the calculation to include it in statistics
    if (settings.sortFamily) {
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

    }
    return familyMap
}

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

export type famCordArr = {
    familyID: number,
    relays: GeoRelayView[],
    padding: number
}

export const sortFamCordMap = (
    input: Map<string, Map<number, GeoRelayView[]>>
): Map<string, famCordArr[]> => {
    let output: Map<string, famCordArr[]> = new Map<string, famCordArr[]>()
    input.forEach((famMapForLocation, coordinateKey) => {
        let sorted: famCordArr[] = []
        let largest: famCordArr = {relays: [], familyID: 0, padding: 0}
        while(famMapForLocation.size > 0){
            famMapForLocation.forEach((relays, famID) => {
                if (relays.length > largest.relays.length) largest = {relays: relays, familyID: famID, padding: 0}
            })
            if (sorted.length > 0 && largest.relays.length === sorted[sorted.length-1].relays.length) {
                sorted.forEach((value, index, array) => {
                    array[index] = {...value, padding: value.padding++}
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