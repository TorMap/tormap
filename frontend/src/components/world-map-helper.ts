//Helper for relaysToLayerGroup, applys all Filters to the downloaded data
import {GeoRelayView} from "../types/geo-relay";
import {RelayFlag} from "../types/relay";
import {Feature, GeoJsonProperties, GeometryObject} from "geojson";
import L, {circleMarker, Layer, LayerGroup, LeafletMouseEvent} from "leaflet";
import {Settings, Statistics} from "../types/variousTypes";
import {Colors} from "../util/Config";
import worldGeoData from "../data/world.geo.json";

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
    feature: Feature<GeometryObject, GeoJsonProperties>,
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
                sorted.map(value => {
                    value.padding = value.padding +1
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

export const getCountryMap = (
    relays: GeoRelayView[],
    settings: Settings,
    setSettingsCallback: (s: Settings) => void
): Map<string, GeoRelayView[]> => {
    let countryMap: Map<string, GeoRelayView[]> = new Map<string, GeoRelayView[]>()
    // true for forcing the calculation to include it in statistics
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
        if(settings.selectedCountry && !countryMap.has(settings.selectedCountry)){
            setSettingsCallback({...settings, selectedCountry: undefined})
        }
    }
    return countryMap
}

export const aggregatedCoordinatesLayer = (
    latLonMap: Map<string, GeoRelayView[]>,
    onMarkerClick: (e: LeafletMouseEvent) => void,
): LayerGroup => {
    const aggregatedCoordinatesLayer = new LayerGroup()
    latLonMap.forEach((value, key) => {
        const coordinates= key.split(",")
        // skip if a coordinate has less than 4 relays
        if (value.length < 4) return
        circleMarker(
            [+coordinates[0],+coordinates[1]],
            {
                radius: value.length / 2,
                color: "#ffffff",
                weight: .3,
                className: key
            }
        )
            .on("click", onMarkerClick)
            .addTo(aggregatedCoordinatesLayer)
    })
    return aggregatedCoordinatesLayer
}

export const defaultMarkerLayer = (
    relays: GeoRelayView[],
    onMarkerClick: (e: LeafletMouseEvent) => void,
): LayerGroup => {
    const defaultLayer = new LayerGroup()
    const exitLayer = new LayerGroup()
    const guardLayer = new LayerGroup()
    const defaultMarkerLayer = new LayerGroup([defaultLayer, guardLayer, exitLayer])
    relays.forEach(relay => {
        let color = Colors.Default
        let layer = defaultLayer
        if (relay.flags?.includes(RelayFlag.Exit)) {
            color = Colors.Exit
            layer = exitLayer
        }
        else if (relay.flags?.includes(RelayFlag.Guard)) {
            color = Colors.Guard
            layer = guardLayer
        }

        circleMarker(
            [relay.lat, relay.long],
            {
                radius: 1,
                className: relay.detailsId,
                color: color,
                weight: 3,
            },
        )
            .on("click", onMarkerClick)
            .addTo(layer)
    })
    return defaultMarkerLayer
}


export const familyLayer = (
    familyMap: Map<number, GeoRelayView[]>,
    settings: Settings,
    setSettingsCallback: (s: Settings) => void,
): LayerGroup => {
    const familyLayer: LayerGroup = new LayerGroup()
    let index = 0
    familyMap.forEach(family => {
        family.forEach((relay,i , family) => {
            let hue = index * 360 / familyMap.size * (2 / 3)
            let sat = "90%"
            let radius = family.length * 10

            if (settings.selectedFamily !== undefined && settings.selectedFamily !== relay.familyId) sat = "30%"
            if (settings.selectedFamily !== undefined && settings.selectedFamily && settings.selectedFamily !== relay.familyId) sat = "0%"

            const color = `hsl(${hue},${sat},60%)`
            circleMarker(
                [relay.lat, relay.long],
                {color: color,
                    radius: radius,
                    fillOpacity: .05,
                    weight: .1,
                }
            )
                .on("click", () => {
                    if (relay.familyId === settings.selectedFamily) {
                        setSettingsCallback({...settings, selectedFamily: undefined})
                    }else{
                        setSettingsCallback({...settings, selectedFamily: relay.familyId})
                    }
                })
                .addTo(familyLayer)
        })
        index ++
    })
    return familyLayer
}

//todo: scaling anpassen
export const familyCordLayer = (
    famCordMap: Map<string, Map<number, GeoRelayView[]>>,
    settings: Settings,
    setSettingsCallback: (s: Settings) => void,
): LayerGroup => {
    const familyLayer: LayerGroup = new LayerGroup()
    const sortedFamCordMap: Map<string, famCordArr[]> = sortFamCordMap(famCordMap)
    sortedFamCordMap.forEach((famMapForLocation ,location) => {
        const coordinates= location.split(",")
        const latlng: L.LatLngExpression = [+coordinates[0],+coordinates[1]]
        famMapForLocation.forEach((famCordArr, index) => {
            let hue = famCordArr.familyID % 360
            let sat = "90%"
            let radius = 5 + (famCordArr.relays.length + famCordArr.padding)*2
            let fillOpacity = .25

            let selected = true

            if (settings.selectedFamily !== undefined && settings.selectedFamily !== famCordArr.familyID) selected = false
            if (settings.selectedFamily !== undefined && settings.selectedFamily && settings.selectedFamily !== famCordArr.familyID) sat = "0%"

            if (!selected) {
                sat = "0%"
                fillOpacity = 0
            }

            const color = `hsl(${hue},${sat},60%)`
            circleMarker(
                latlng,
                {color: color,
                    radius: radius,
                    fillOpacity: fillOpacity,
                    weight: .5,
                }
            )
                .on("click", () => {
                    console.log(index)
                    if (famCordArr.familyID === settings.selectedFamily) {
                        setSettingsCallback({...settings, selectedFamily: undefined})
                    }else{
                        setSettingsCallback({...settings, selectedFamily: famCordArr.familyID})
                    }
                })
                .addTo(familyLayer)
        })
    })
    console.log(sortedFamCordMap)
    return familyLayer
}

export const countryMarkerLayer = (
    countryMap: Map<string, GeoRelayView[]>,
    settings: Settings,
    onMarkerClick: (e: LeafletMouseEvent) => void,
): LayerGroup => {
    const countryLayer: LayerGroup = new LayerGroup()
    let index = 0
    const geoData = worldGeoData
    countryMap.forEach((country, key) => {
        let hue = 0
        const mapColor9 = (geoData.features.find(feature => feature.properties.iso_a2 === key)?.properties.mapcolor9)
        if (mapColor9) hue = mapColor9 * 360 / 9
        else hue = 9 * 360 / 9

        country.forEach((relay,i , country) => {
            let sat = "90%"
            let radius = 1

            if (settings.selectedCountry !== undefined && settings.selectedCountry !== relay.country) sat = "30%"
            if (settings.selectedCountry !== undefined && settings.selectedCountry && settings.selectedCountry !== relay.country) sat = "0%"

            const color = `hsl(${hue},${sat},60%)`
            circleMarker(
                [relay.lat, relay.long],
                {color: color,
                    radius: radius,
                    className: relay.detailsId,
                }
            )
                .on("click", onMarkerClick)
                .addTo(countryLayer)
        })
        index ++
    })
    return countryLayer
}

export const calculateStatistics = (
    relays: GeoRelayView[],
    countryMap: Map<string, GeoRelayView[]>,
    familyMap: Map<number, GeoRelayView[]>,
    settings: Settings,
): Statistics => {
    let stats: Statistics = {
        guard: 0,
        exit: 0,
        default: 0,
    }
    relays.forEach(relay => {
        if (relay.flags?.includes(RelayFlag.Exit)) {
            stats.exit++
        }else if( relay.flags?.includes(RelayFlag.Guard)){
            stats.guard++
        }else{
            stats.default++
        }
    })

    // true for forcing the calculation to include it in statistics
    if(settings.sortCountry || true){
        stats = {...stats, countryCount: countryMap.size}
        if(settings.selectedCountry) {
            stats = {...stats, countryRelayCount: countryMap.get(settings.selectedCountry)?.length}
        }
    }
    // true for forcing the calculation to include it in statistics
    if(settings.sortFamily || true){
        stats = {...stats, familyCount: familyMap.size}
        if(settings.selectedFamily) stats = {...stats, familyRelayCount: familyMap.get(settings.selectedFamily)?.length}
    }
    return stats
}