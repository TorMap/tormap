import {GeoRelayView} from "../types/geo-relay";
import L, {circleMarker, GeoJSON, Layer, LayerGroup, LeafletMouseEvent, PathOptions} from "leaflet";
import {Colors} from "./Config";
import {RelayType} from "../types/relay";
import {Settings} from "../types/variousTypes";
import worldGeoData from "../data/world.geo.json";
import {Feature, GeoJsonObject, GeoJsonProperties, GeometryObject} from "geojson";
import {famCordArr, getRelayType, sortFamCordMap} from "./aggregate-relays";

/**
 * Returns a Layer with markers with size relative to number of relays on a coordinate.
 * @param latLonMap
 * @param onMarkerClick event handler
 */
export const aggregatedCoordinatesLayer = (
    latLonMap: Map<string, GeoRelayView[]>,
    onMarkerClick: (e: LeafletMouseEvent) => void,
): LayerGroup => {
    const aggregatedCoordinatesLayer = new LayerGroup()
    latLonMap.forEach((value, key) => {
        const coordinates = key.split(",")
        // skip if a coordinate has less than 4 relays
        if (value.length < 4) return
        circleMarker(
            [+coordinates[0], +coordinates[1]],
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

/**
 * Returns a Layer with markers for each relay.
 * @param relays
 * @param onMarkerClick
 */
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
        switch (getRelayType(relay)){
            case RelayType.Exit: {
                color = Colors.Exit
                layer = exitLayer
                break
            }
            case RelayType.Guard: {
                color = Colors.Guard
                layer = guardLayer
                break
            }
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

/**
 * Returns a Layer with markers for families with size relative to number of relays in given family on a coordinate.
 * @param familyMap
 * @param settings App settings
 * @param setSettingsCallback
 */
export const familyLayer = (
    familyMap: Map<number, GeoRelayView[]>,
    settings: Settings,
    setSettingsCallback: (s: Settings) => void,
): LayerGroup => {
    const familyLayer: LayerGroup = new LayerGroup()
    let index = 0
    familyMap.forEach(family => {
        family.forEach((relay, i, family) => {
            let hue = index * 360 / familyMap.size * (2 / 3)
            let sat = "90%"
            let radius = family.length * 10

            if (settings.selectedFamily !== undefined && settings.selectedFamily !== relay.familyId) sat = "30%"
            if (settings.selectedFamily !== undefined && settings.selectedFamily && settings.selectedFamily !== relay.familyId) sat = "0%"

            const color = `hsl(${hue},${sat},60%)`
            circleMarker(
                [relay.lat, relay.long],
                {
                    color: color,
                    radius: radius,
                    fillOpacity: .05,
                    weight: .1,
                }
            )
                .on("click", () => {
                    if (relay.familyId === settings.selectedFamily) {
                        setSettingsCallback({...settings, selectedFamily: undefined})
                    } else {
                        setSettingsCallback({...settings, selectedFamily: relay.familyId})
                    }
                })
                .addTo(familyLayer)
        })
        index++
    })
    return familyLayer
}

//todo: scaling anpassen
/**
 * Returns a Layer with markers for families with size relative to number of relays in given family on a coordinate. And scales families size so there are no markers with same size
 * @param famCordMap
 * @param settings
 * @param setSettingsCallback
 */
export const familyCordLayer = (
    famCordMap: Map<string, Map<number, GeoRelayView[]>>,
    settings: Settings,
    setSettingsCallback: (s: Settings) => void,
): LayerGroup => {
    const familyLayer: LayerGroup = new LayerGroup()
    const sortedFamCordMap: Map<string, famCordArr[]> = sortFamCordMap(famCordMap)
    sortedFamCordMap.forEach((famMapForLocation, location) => {
        const coordinates = location.split(",")
        const latlng: L.LatLngExpression = [+coordinates[0], +coordinates[1]]
        famMapForLocation.forEach((famCordArr, index) => {
            let hue = famCordArr.familyID % 360
            let sat = "90%"
            let radius = 5 + (famCordArr.relays.length + famCordArr.padding) * 2
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
                {
                    color: color,
                    radius: radius,
                    fillOpacity: fillOpacity,
                    weight: .5,
                }
            )
                .on("click", () => {
                    console.log(index)
                    if (famCordArr.familyID === settings.selectedFamily) {
                        setSettingsCallback({...settings, selectedFamily: undefined})
                    } else {
                        setSettingsCallback({...settings, selectedFamily: famCordArr.familyID})
                    }
                })
                .addTo(familyLayer)
        })
    })
    console.log(sortedFamCordMap)
    return familyLayer
}

/**
 * Returns a Layer with markers for each relay. Color is same for countries in same country.
 * @param countryMap
 * @param settings
 * @param onMarkerClick
 */
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

        country.forEach((relay, i, country) => {
            let sat = "90%"
            let radius = 1

            if (settings.selectedCountry !== undefined && settings.selectedCountry !== relay.country) sat = "30%"
            if (settings.selectedCountry !== undefined && settings.selectedCountry && settings.selectedCountry !== relay.country) sat = "0%"

            const color = `hsl(${hue},${sat},60%)`
            circleMarker(
                [relay.lat, relay.long],
                {
                    color: color,
                    radius: radius,
                    className: relay.detailsId,
                }
            )
                .on("click", onMarkerClick)
                .addTo(countryLayer)
        })
        index++
    })
    return countryLayer
}

export const countryLayer = (
    countryMap: Map<string, GeoRelayView[]>,
    settings: Settings,
    setSettingsCallback: (s: Settings) => void
): LayerGroup =>{
    const style = (feature: Feature<GeometryObject, GeoJsonProperties>): PathOptions => {
        if (settings.selectedCountry === feature.properties!!.iso_a2) {
            return {
                fillColor: "rgba(255,255,255,0.7)",
                weight: .5,
            }
        } else {
            return {
                fillColor: "rgba(255,255,255,0.3)",
                weight: .5,
            }
        }
    }

    const geoData = worldGeoData
    let filteredGeoData = new GeoJSON(undefined, {
        style: style as PathOptions,
        onEachFeature(feature: Feature<GeometryObject, GeoJsonProperties>, layer: Layer) {
            onEachFeature(feature, layer, settings, setSettingsCallback)
        }
    })
    geoData.features.forEach(feature => {
        if (countryMap.has(feature.properties.iso_a2)) {
            filteredGeoData.addData(feature as GeoJsonObject)
        }
    })

    const worldGeoLayer = new LayerGroup()
    filteredGeoData.addTo(worldGeoLayer)
    return worldGeoLayer
}
//Helper for relaysToLayerGroup, used for adding eventlisteners to the countries
/**
 * Helper for adding events to countries
 * @param feature
 * @param layer
 * @param settings
 * @param setSettingsCallback
 */
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