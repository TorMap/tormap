import {Feature, GeoJsonObject, GeometryObject} from "geojson";
import L, {circleMarker, GeoJSON, Layer, LayerGroup, LeafletMouseEvent, PathOptions} from "leaflet";

import {Colors} from "../config";
import {RelayLocationDto} from "../dto/relay";
import worldGeoData from "../resources/world.geo.json";
import {RelayType} from "../types/relay";
import {Settings} from "../types/settings";
import {
    buildRelayCoordinatesMap,
    createLatLonKey,
    getRelayType,
    RelayFamilyLocation,
    sortFamilyCoordinatesMap
} from "./aggregate-relays";
import {getMapColor9} from "./geojson";

/**
 * Returns a Layer with markers with size relative to number of relays on a coordinate.
 * @param latLonMap - The LatLonMap
 * @param onMarkerClick - Event handler for clicking on a marker
 */
export const buildAggregatedCoordinatesLayer = (
    latLonMap: Map<string, RelayLocationDto[]>,
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
                radius: calcRadiusForValue(relays.length),
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
 * @param latLonMap - The LatLonMap
 * @param singleColor - Whether all markers should have the same color
 * @param onMarkerClick - Event handler for clicking on a marker
 */
export const buildRelayLayer = (
    latLonMap: Map<string, RelayLocationDto[]>,
    singleColor: boolean,
    onMarkerClick: (e: LeafletMouseEvent) => void,
): LayerGroup => {
    const defaultLayer = new LayerGroup()
    const exitLayer = new LayerGroup()
    const guardLayer = new LayerGroup()
    const defaultMarkerLayer = new LayerGroup([defaultLayer, guardLayer, exitLayer])
    latLonMap.forEach((coordinate, key) => {
        coordinate.forEach(relay => {
            let color = Colors.Default
            let layer = defaultLayer
            switch (getRelayType(relay)) {
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
            if (singleColor) color = "#989898"

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
 * Returns a Layer with circles only for the selected family
 * @param familyMap - The FamilyMap
 * @param settings - The app settings
 * @param setSettingsCallback - The callback for changing settings
 */
export const buildSelectedFamilyLayer = (
    familyMap: Map<number, RelayLocationDto[]>,
    settings: Settings,
    setSettingsCallback: (s: Settings) => void,
): LayerGroup => {
    const familyLayer: LayerGroup = new LayerGroup()
    familyMap.forEach((family, familyId) => {
        if (familyId !== settings.selectedFamily) return
        const latLonMap: Map<string, RelayLocationDto[]> = buildRelayCoordinatesMap(family)
        latLonMap.forEach((relays, key) => {
            const coordinates = key.split(",")
            circleMarker(
                [+coordinates[0], +coordinates[1]],
                {
                    color: calculateFamilyColor(familyId),
                    radius: calcRadiusForValue(relays.length),
                    fillOpacity: .2,
                    weight: 1,
                },
            )
                .on("click", () => {
                    if (familyId === settings.selectedFamily) {
                        setSettingsCallback({...settings, selectedFamily: undefined})
                    } else {
                        setSettingsCallback({...settings, selectedFamily: familyId})
                    }
                })
                .addTo(familyLayer)
        })
    })
    return familyLayer
}

/**
 * Returns a Layer with markers for families with size relative to number of relays in given family on a coordinate. And scales families size so there are no markers with same size
 * @param famCordMap - The famCordMap
 * @param settings - The app settings
 * @param setSettings - The callback for changing settings
 * @param setFamiliesForSelectionDialog
 * @param setShowFamilySelectionDialog
 */
export const buildFamilyCoordinatesLayer = (
    famCordMap: Map<string, Map<number, RelayLocationDto[]>>,
    settings: Settings,
    setSettings: (s: Settings) => void,
    setFamiliesForSelectionDialog: (f: number[]) => void,
    setShowFamilySelectionDialog: (b: boolean) => void,
): LayerGroup => {
    const familyLayer: LayerGroup = new LayerGroup()
    const sortedFamilyCoordinatesMap: Map<string, RelayFamilyLocation[]> = sortFamilyCoordinatesMap(famCordMap)
    sortedFamilyCoordinatesMap.forEach((familiesAtLocation, location) => {
        const coordinates = location.split(",")
        const latLng: L.LatLngExpression = [+coordinates[0], +coordinates[1]]
        familiesAtLocation.forEach((familyLocation) => {
            const radius = calcRadiusForValue(familyLocation.relays.length + familyLocation.padding)
            let fillOpacity = .2

            // not selected
            if (settings.selectedFamily !== undefined && settings.selectedFamily !== familyLocation.familyId) {
                fillOpacity = 0
            }

            circleMarker(
                latLng,
                {
                    color: calculateFamilyColor(familyLocation.familyId),
                    radius: radius,
                    fillOpacity: fillOpacity,
                    weight: 1,
                    className: location,
                }
            )
                .on("click", () =>
                    onMultiFamilyCircleClick(settings, setSettings, familiesAtLocation, setFamiliesForSelectionDialog, setShowFamilySelectionDialog)
                )
                .addTo(familyLayer)
        })
    })
    return familyLayer
}

const onMultiFamilyCircleClick = (
    settings: Settings,
    setSettings: (s: Settings) => void,
    familiesAtLocation: RelayFamilyLocation[],
    setFamiliesForSelectionDialog: (f: number[]) => void,
    setShowFamilySelectionDialog: (b: boolean) => void,
) => {
    if (settings.selectedFamily) {
        setSettings({...settings, selectedFamily: undefined})
    } else if (familiesAtLocation.length === 1) {
        setSettings({...settings, selectedFamily: familiesAtLocation[0].familyId})
    } else {
        setFamiliesForSelectionDialog(familiesAtLocation.map(family => family.familyId))
        setShowFamilySelectionDialog(true)
    }
}

export const calculateFamilyColor = (familyId: number) => {
    const hue = (familyId % 8) * (360 / 8)
    const sat = "90%"
    return `hsl(${hue},${sat},60%)`
}

export const buildRelayHeatmapLayer = (relays: RelayLocationDto[]): LayerGroup => {
    const coordinates = new Array<number[]>()
    relays.forEach(relay => coordinates.push([relay.lat, relay.long, 1]))
    return (L as any).heatLayer(coordinates, {
        radius: 25,
        max: 1,
        blur: 35,
        minOpacity: .55,
        gradient: {0.4: '#2e53dc', 0.65: '#c924ae', .75: '#ff4646', .83: "#ff0000"},
    })
}

/**
 * Returns a Layer with markers for each relay. Color is same for countries in same country.
 * @param countryMap - The CountryMap
 * @param settings - The app settings
 * @param onMarkerClick - Event handler for clicking on a marker
 */
export const buildRelayCountryLayer = (
    countryMap: Map<string, RelayLocationDto[]>,
    settings: Settings,
    onMarkerClick: (e: LeafletMouseEvent) => void,
): LayerGroup => {
    const countryLayer: LayerGroup = new LayerGroup()
    countryMap.forEach((country, key) => {
        const hue = getMapColor9(key) * 360 / 9
        country.forEach((relay) => {
            let sat = "90%"
            const radius = 1

            // not selected
            if (settings.selectedCountry !== undefined && settings.selectedCountry !== relay.country) sat = "0%"

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
    })
    return countryLayer
}

/**
 * Returns an Layer with all Countries that contain relays
 * @param countryMap - The CountryMap
 * @param settings - The app settings
 * @param setSettingsCallback - The callback for changing settings
 */
export const buildCountryLayer = (
    countryMap: Map<string, RelayLocationDto[]>,
    settings: Settings,
    setSettingsCallback: (s: Settings) => void
): LayerGroup => {
    // style for countries
    const style = (feature: Feature<GeometryObject>): PathOptions => {
        if (settings.selectedCountry === feature.properties?.iso_a2) {
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

    // draw countries according above style
    const geoData = worldGeoData
    const filteredGeoData = new GeoJSON(undefined, {
        style: style as PathOptions,
        onEachFeature(feature: Feature<GeometryObject>, layer: Layer) {
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
 * @param feature - The GeoJSON-feature the event belongs to
 * @param layer - The layer the event is bound to
 * @param settings - The app settings
 * @param setSettingsCallback - The callback for changing settings
 */
const onEachCountry = (
    feature: Feature<GeometryObject>,
    layer: Layer,
    settings: Settings,
    setSettingsCallback: (s: Settings) => void
) => {
    layer.on({
        click: () => {
            if (feature.properties?.iso_a2 !== settings.selectedCountry)
                setSettingsCallback({...settings, selectedCountry: feature.properties?.iso_a2})
            else setSettingsCallback({...settings, selectedCountry: undefined})
        }
    });
}

/**
 * Maps a number of range x1 to y1 to the range x2 to y2
 */
const mapRange = (value: number, x1: number, y1: number, x2: number, y2: number): number => (value - x1) * (y2 - x2) / (y1 - x1) + x2;

/**
 * Returns a scaled value, for better scaling of markers
 * @param value
 */
const calcRadiusForValue = (value: number): number => {
    if (value < 10) return 10
    else if (value < 100) return mapRange(value, 10, 100, 10, 50)
    else if (value < 200) return mapRange(value, 100, 200, 50, 70)
    else if (value < 300) return mapRange(value, 200, 300, 70, 100)
    else return 100
}
