
import L, {circleMarker, GeoJSON, latLng, Layer, LayerGroup, LeafletMouseEvent, PathOptions} from "leaflet";
import {Colors} from "./Config";
import {RelayType} from "../types/relay";
import {Settings} from "../types/variousTypes";
import worldGeoData from "../data/world.geo.json";
import {Feature, GeoJsonObject, GeoJsonProperties, GeometryObject} from "geojson";
import {famCordArr, buildLatLonMap, getRelayType, sortFamilyCoordinatesMap, createLatLonKey} from "./aggregate-relays";
import {getMapColor9} from "./geojson";
import {GeoRelayView} from "../types/responses";

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
    latLonMap.forEach((relays, key) => {
        const coordinates = key.split(",")
        // skip if a coordinate has less than 4 relays
        if (relays.length < 4) return
        circleMarker(
            [+coordinates[0], +coordinates[1]],
            {
                radius: relays.length,
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
 * @param latLonMap
 * @param onMarkerClick
 */
export const defaultMarkerLayer = (
    latLonMap: Map<string, GeoRelayView[]>,
    onMarkerClick: (e: LeafletMouseEvent) => void,
): LayerGroup => {
    const defaultLayer = new LayerGroup()
    const exitLayer = new LayerGroup()
    const guardLayer = new LayerGroup()
    const defaultMarkerLayer = new LayerGroup([defaultLayer, guardLayer, exitLayer])
    latLonMap.forEach((coordinate, key) =>{
        coordinate.forEach(relay => {
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
                    className: key,
                    color: color,
                    weight: 3,
                },
            )
                .on("click", onMarkerClick)
                .addTo(layer)
        })
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
    familyMap.forEach((family, familyID) => {
        if (familyID !== settings.selectedFamily) return
        const latLonMap: Map<string, GeoRelayView[]> = buildLatLonMap(family)
        latLonMap.forEach((relays, key) => {
            let hue = familyID % 360
            let sat = "90%"
            let radius = 10 + relays.length

            const coordinates = key.split(",")

            const color = `hsl(${hue},${sat},60%)`
            circleMarker(
                [+coordinates[0], +coordinates[1]],
                {
                    color: color,
                    radius: radius,
                    fillOpacity: .3,
                    weight: .1,
                },
            )
                .on("click", () => {
                    if (familyID === settings.selectedFamily) {
                        setSettingsCallback({...settings, selectedFamily: undefined})
                    } else {
                        setSettingsCallback({...settings, selectedFamily: familyID})
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
 * @param onMarkerClick
 */
export const familyCordLayer = (
    famCordMap: Map<string, Map<number, GeoRelayView[]>>,
    settings: Settings,
    setSettingsCallback: (s: Settings) => void,
    onMarkerClick: (e: LeafletMouseEvent) => void,
): LayerGroup => {
    const familyLayer: LayerGroup = new LayerGroup()
    const sortedFamCordMap: Map<string, famCordArr[]> = sortFamilyCoordinatesMap(famCordMap)
    sortedFamCordMap.forEach((famMapForLocation, location) => {
        const coordinates = location.split(",")
        const latlng: L.LatLngExpression = [+coordinates[0], +coordinates[1]]
        famMapForLocation.forEach((famCordArr) => {
            let hue = famCordArr.familyID % 360
            let sat = "90%"
            let radius = 5 + (famCordArr.relays.length + famCordArr.padding)
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
                    className: location,
                }
            )
                .on("click", onMarkerClick)
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
        let hue = getMapColor9(key) * 360 / 9
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
                    className: createLatLonKey(relay),
                }
            )
                .on("click", onMarkerClick)
                .addTo(countryLayer)
        })
        index++
    })
    return countryLayer
}
/**
 * Returns an Layer with all Countries that contain relays
 * @param countryMap
 * @param settings
 * @param setSettingsCallback
 */
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
            onEachCountry(feature, layer, settings, setSettingsCallback)
        }
    })
    geoData.features.forEach(feature => {
        if (countryMap.has(feature.properties.iso_a2)) {
            filteredGeoData.addData(feature as GeoJsonObject)
        }
    })

    return filteredGeoData
}

/**
 * Helper for adding events to countries
 * @param feature
 * @param layer
 * @param settings
 * @param setSettingsCallback
 */
const onEachCountry = (
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