import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useEffect, useState} from "react";
import {apiBaseUrl} from "../util/constants";
import L, {GeoJSON, Layer, LayerGroup, LeafletMouseEvent, Map as LeafletMap, PathOptions} from "leaflet";
import 'leaflet/dist/leaflet.css';
import {NodePopup} from "./node-popup";
import {GeoRelayView} from "../types/geo-relay";
import {Settings, snackbarMessage, Statistics} from "../types/variousTypes";
import "leaflet.heat"
import {makeStyles} from "@material-ui/core";
import worldGeoData from "../data/world.geo.json"; // data from https://geojson-maps.ash.ms/
import {Feature, GeoJsonObject, GeoJsonProperties, GeometryObject} from "geojson";
import {
    aggregatedCoordinatesLayer,
    applyFilter,
    calculateStatistics,
    countryMarkerLayer,
    defaultMarkerLayer,
    familyCordLayer,
    familyLayer,
    getCountryMap,
    getFamCordMap,
    getFamilyMap,
    getLatLonMap,
    onEachFeature
} from "./world-map-helper";
import {NodeArrayPopup} from "./nodeArray-popup";

/**
 * Styles according to Material UI doc for components used in WorldMap component
 */
const useStyle = makeStyles(() => ({
    leafletContainer: {
        width: "100vw",
        height: "100vh",
        backgroundColor: "#262626",
        position: "fixed",
    }
}))

interface Props {
    /**
     * This days data will be fetched from backend and visualized on the map
     */
    dayToDisplay?: string

    /**
     * the app settings
     */
    settings: Settings

    /**
     * A callback to change settings
     * @param s the settings variable that should be changed
     */
    setSettingsCallback: (s: Settings) => void

    /**
     * A variable callback whether the map is currently fetching a new date
     * @param b whether the map is currently fetching a new date
     */
    setLoadingStateCallback: (b: boolean) => void

    /**
     * A callback to change statistics
     * @param stat the statistic variable that should be changed
     */
    setStatisticsCallback: (stat: Statistics) => void

    /**
     * callback for errormessages
     * @param snackbarMessage Objekt of type snackbarMessage with message and severity
     */
    handleSnackbar: (snackbarMessage: snackbarMessage) => void
}

// Variable needs to be outside component to keep track of the last selected date
let latestRequestTimestamp: number | undefined = undefined

export const WorldMap: FunctionComponent<Props> = ({
                                                       dayToDisplay,
                                                       settings,
                                                       setSettingsCallback,
                                                       setLoadingStateCallback,
                                                       setStatisticsCallback,
                                                       handleSnackbar
                                                   }) => {
    const [showNodePopup, setShowNodePopup] = useState(false)
    const [nodePopupRelayId, setNodePopupRelayId] = useState<number>()
    const [showNodeArrayPopup, setShowNodeArrayPopup] = useState(false)
    const [nodePopupRelays, setNodePopupRelays] = useState<GeoRelayView[]>([])
    const [leafletMap, setLeafletMap] = useState<LeafletMap>()
    const [markerLayer] = useState<LayerGroup>(new LayerGroup())
    const [heatLayer, setHeatLayer] = useState<Layer>(new Layer())
    const [relays, setRelays] = useState<GeoRelayView[]>([])
    const classes = useStyle()

    //Update listener, fires whenever a new date is to be downloaded, only the last selected date gets displayed
    useEffect(() => {
        if (dayToDisplay) {
            console.log("fetching")
            let currentTimeStamp = Date.now()
            setLoadingStateCallback(true)
            fetch(`${apiBaseUrl}/archive/geo/relay/day/${dayToDisplay}`)
                .then(response => response.json())
                .then(newRelays => {
                    setLoadingStateCallback(false)
                    if (currentTimeStamp === latestRequestTimestamp) setRelays(newRelays)
                })
                .catch(reason => {
                    handleSnackbar({message: `${reason}`, severity: "error"})
                })
            latestRequestTimestamp = currentTimeStamp
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [dayToDisplay])

    //Update listener, fires whenever settings get changed or an new date got downloaded
    useEffect(() => {
        if (dayToDisplay) drawLayerGroup(relaysToLayerGroup(relays))
    }, [relays, settings])

    //Eventhandler for markers to show the node-popup component
    const onMarkerClick = (event: LeafletMouseEvent) => {
        console.log("Marker clicked, show node details")
        setNodePopupRelayId(event.sourceTarget.options.className)
        setShowNodePopup(true)
    }

    const onMarkerGroupClick = (event: LeafletMouseEvent) => {
        console.log("Marker Group clicked")
        const relaysAtCoordinate = getLatLonMap(applyFilter(relays, settings), settings).get(event.target.options.className)!!
        setNodePopupRelays(relaysAtCoordinate)
        console.log(`${event.target.options.className} has ${relaysAtCoordinate.length}`)
        console.log(relaysAtCoordinate)
        setShowNodeArrayPopup(true)
    }

    // Processes an array of Relays according to settings and returns an layerGroup with all relevant layers that have to be added to the map
    const relaysToLayerGroup = (relays: GeoRelayView[]): LayerGroup => {
        console.time(`relaysToLayerGroup`)
        console.timeLog(`relaysToLayerGroup`, `New Layer with ${relays.length} elements`)

        const layerToReturn = new LayerGroup()

        // filter relays
        relays = applyFilter(relays, settings)
        if (!relays.length && dayToDisplay) {
            handleSnackbar({message: "There are no relays with the filtered flags!", severity: "warning"})
            return layerToReturn
        }

        // Map for coordinate's, used to get an Array of GeoRelayView with relays on the same coordinate
        const latLonMap: Map<string, GeoRelayView[]> = getLatLonMap(relays, settings)

        //Map for family's, used to get an Array of GeoRelayView with relays in the same family / autonomsystem
        const familyMap: Map<number, GeoRelayView[]> = getFamilyMap(relays, settings)
        if (settings.selectedFamily && !familyMap.has(settings.selectedFamily)) {
            setSettingsCallback({...settings, selectedFamily: undefined})
        }
        if (settings.sortFamily && familyMap.size === 0) handleSnackbar({
            message: "There are no families available for this day!",
            severity: "warning"
        })

        const famCordMap: Map<string, Map<number, GeoRelayView[]>> = getFamCordMap(latLonMap)

        //Map for country's, used to get an Array of GeoRelayView with relays in the same country
        const countryMap: Map<string, GeoRelayView[]> = getCountryMap(relays, settings, setSettingsCallback)

        //Draw Country's, used to draw all countries to the map if at least one relay is hosted there
        if (settings.sortCountry) {
            if (leafletMap && countryMap.size > 0) {
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
                worldGeoLayer.addTo(layerToReturn)
            }
        }

        //Draw aggregated marker's, used to draw all markers to the map with more than 4 relays on the same coordinate
        if (settings.aggregateCoordinates) {
            aggregatedCoordinatesLayer(latLonMap, onMarkerGroupClick).addTo(layerToReturn)
        }

        //Draw marker's, used to draw all markers to the map with colors according to their type
        if (!settings.sortCountry) {
            defaultMarkerLayer(relays, onMarkerClick).addTo(layerToReturn)
        }

        //Draw familyCord marker's, used to draw all markers to the map with colors according to their family
        if (settings.sortFamily) {
            familyCordLayer(famCordMap, settings, setSettingsCallback).addTo(layerToReturn)
        }

        //Draw Heatmap, draws a heatmap with a point for each coordinate
        // https://github.com/Leaflet/Leaflet.heat
        if (settings.heatMap) {
            leafletMap?.removeLayer(heatLayer)
            let latlngs: Array<number[]> = new Array<number[]>()
            relays.forEach(relay => latlngs.push([relay.lat, relay.long, 1]))
            // @ts-ignore needed for compatibility with js code of the heatmap package
            let heat = L.heatLayer(latlngs, {
                radius: 25,
                max: 1,
                blur: 35,
                minOpacity: .55,
                gradient: {0.4: '#2e53dc', 0.65: '#c924ae', .75: '#ff4646', .83: "#ff0000"}
            }).addTo(leafletMap)
            setHeatLayer(heat)
        } else {
            leafletMap?.removeLayer(heatLayer)
        }

        //Draw country marker's, used to draw all markers to the map with colors according to their country
        if (settings.sortCountry) {
            countryMarkerLayer(countryMap, settings, onMarkerClick).addTo(layerToReturn)
        }

        //Calculate statistics
        if (settings.selectedCountry && settings.selectedFamily && familyMap && countryMap) {
            relays = []
            familyMap.get(settings.selectedFamily)?.forEach(familyRelay => {
                countryMap.get(settings.selectedCountry!!)?.forEach(countryRelay => {
                    if (familyRelay.detailsId === countryRelay.detailsId) relays.push(familyRelay)
                })
            })
        } else if (settings.selectedCountry && countryMap.has(settings.selectedCountry)) {
            relays = countryMap.get(settings.selectedCountry)!!
        } else if (settings.selectedFamily && familyMap.has(settings.selectedFamily)) {
            relays = familyMap.get(settings.selectedFamily)!!
        }

        setStatisticsCallback(calculateStatistics(relays, countryMap, familyMap, settings))

        console.timeLog(`relaysToLayerGroup`, `New Layer with ${relays.length} elements finished`)
        console.timeEnd(`relaysToLayerGroup`,)
        return layerToReturn
    }

    // Draws a group of layers to the map
    const drawLayerGroup = (layerGroup: LayerGroup) => {
        if (leafletMap && dayToDisplay) {
            console.log(`drawing for ${dayToDisplay}`)
            markerLayer.clearLayers()
            const layers = layerGroup.getLayers()
            layers.forEach((layer) => layer.addTo(markerLayer))
        }
    }

    return (
        <MapContainer
            className={classes.leafletContainer}
            center={[15, 0]}
            minZoom={2}
            zoom={3}
            scrollWheelZoom={true}
            zoomSnap={0.5}
            zoomDelta={0.5}
            wheelPxPerZoomLevel={200}
            preferCanvas={true}
            attributionControl={false}
            whenCreated={(newMap: LeafletMap) => {
                markerLayer.addTo(newMap)
                setLeafletMap(newMap)
            }}
        >
            <NodePopup
                showNodePopup={showNodePopup}
                setShowNodePopup={() => setShowNodePopup(false)}
                nodeDetailsId={nodePopupRelayId}
            />
            <NodeArrayPopup
                showNodePopup={showNodeArrayPopup}
                relays={nodePopupRelays}
                closeNodePopup={() => setShowNodeArrayPopup(false)}
            />
            <TileLayer
                subdomains="abcd"
                maxZoom={19}
                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                noWrap={true}
            />
        </MapContainer>
    );
};
