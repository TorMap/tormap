import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useEffect, useState} from "react";
import L, {Layer, LayerGroup, LeafletMouseEvent, Map as LeafletMap} from "leaflet";
import 'leaflet/dist/leaflet.css';
import {Settings, SnackbarMessage, Statistics} from "../types/variousTypes";
import "leaflet.heat"
import {makeStyles} from "@material-ui/core";
import {
    applyFilter,
    buildFamilyCoordinatesMap,
    buildFamilyMap,
    buildLatLonMap,
    calculateStatistics,
    getCountryMap
} from "../util/aggregate-relays";
import {
    aggregatedCoordinatesLayer,
    countryLayer,
    countryMarkerLayer,
    defaultMarkerLayer,
    familyCordLayer,
    familyLayer
} from "../util/layer-construction";
import {apiBaseUrl} from "../util/config";
import {GeoRelayView} from "../types/responses";
import {RelayDetailsDialog} from "./relay-details-dialog";
import {FamilySelectionDialog} from "./family-selection-dialog";

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
     * @param stats - the statistic variable that should be changed
     */
    setStatisticsCallback: (stats: Statistics) => void

    /**
     * Show a message in the snackbar
     * @param message - what to display to user and at which severity
     */
    showSnackbarMessage: (message: SnackbarMessage) => void

    /**
     * Hide the snackbar
     */
    closeSnackbar: () => void
}

/*
Variable needs to be outside component to keep track of the last selected date
This prevents the case that multiple dates get loaded and the last received date is displayed.
Instead only the last requested date will be drawn.
 */
let latestRequestTimestamp: number | undefined = undefined

export const WorldMap: FunctionComponent<Props> = ({
                                                       dayToDisplay,
                                                       settings,
                                                       setSettingsCallback,
                                                       setLoadingStateCallback,
                                                       setStatisticsCallback,
                                                       showSnackbarMessage,
                                                       closeSnackbar,
                                                   }) => {
    const [showFamilySelectionDialog, setShowFamilySelectionDialog] = useState(false)
    const [familiesForDetailsDialog, setFamiliesForDetailsDialog] = useState<number[]>([])
    const [showRelayDetailsDialog, setShowRelayDetailsDialog] = useState(false)
    const [relaysForDetailsDialog, setRelaysForDetailsDialog] = useState<GeoRelayView[]>([])
    const [leafletMap, setLeafletMap] = useState<LeafletMap>()
    const [markerLayer] = useState<LayerGroup>(new LayerGroup())
    const [heatLayer, setHeatLayer] = useState<Layer>(new Layer())
    const [relays, setRelays] = useState<GeoRelayView[]>()
    const classes = useStyle()

    /**
     * Query all Relays from the selected date whenever a new date is selected
     */
    useEffect(() => {
        if (dayToDisplay) {
            console.log("fetching")
            let currentTimeStamp = Date.now()
            setLoadingStateCallback(true)
            fetch(`${apiBaseUrl}/archive/geo/relay/day/${dayToDisplay}`)
                .then(response => response.json())
                .then((newRelays: GeoRelayView[]) => {
                    setLoadingStateCallback(false)
                    if (currentTimeStamp === latestRequestTimestamp) setRelays(newRelays)
                })
                .catch(reason => {
                    showSnackbarMessage({message: `${reason}`, severity: "error"})
                })
            latestRequestTimestamp = currentTimeStamp
        }
    }, [dayToDisplay])

    /**
     * Redraw relays whenever settings get changed or an new relays got downloaded
     */
    useEffect(() => {
        /**
         * After a marker was clicked on the map the corresponding relays are passed to the details dialog
         * @param event - The leaflet marker click event
         */
        function openRelayDetailsDialog(event: LeafletMouseEvent) {
            if (relays) {
                const relaysAtCoordinate = buildLatLonMap(applyFilter(relays, settings)).get(event.target.options.className)!!
                setRelaysForDetailsDialog(relaysAtCoordinate)
                setShowRelayDetailsDialog(true)
            }
        }

        /**
         * After a marker was clicked on the map the corresponding families are passed to the family selection dialog
         * @param event - The leaflet marker click event
         */
        function handleFamilyMarkerClick(event: LeafletMouseEvent) {
            if (relays) {
                const relaysAtCoordinate = buildLatLonMap(applyFilter(relays, settings)).get(event.target.options.className)!!
                const familyMap = buildFamilyMap(relaysAtCoordinate)
                let families: number[] = []
                familyMap.forEach((family, familyID) => {
                    if (family.length > 1) families.push(familyID)
                })
                setFamiliesForDetailsDialog(families)
                setShowFamilySelectionDialog(true)
            }
        }

        /**
         * Draws a group of layers on the map
         * @param layerGroup - The group of layers to be drawn
         */
        function drawLayerGroup(layerGroup: LayerGroup) {
            markerLayer.clearLayers()
            const layers = layerGroup.getLayers()
            layers.forEach((layer) => layer.addTo(markerLayer))
        }

        /**
         * Processes relays according to settings and create multiple layers which can be added to the world map
         * @return LayerGroup - The layer group which contains all relevant layers
         */
        function relaysToLayerGroup(): LayerGroup {
            const layerToReturn = new LayerGroup()

            if (relays) {
                console.time(`relaysToLayerGroup`)
                console.timeLog(`relaysToLayerGroup`, `New Layer with ${relays.length} elements`)

                // Filter relays
                let filteredRelays = applyFilter(relays, settings)
                if (!filteredRelays.length) {
                    showSnackbarMessage({message: "There are no relays with the filtered flags!", severity: "warning"})
                    return layerToReturn
                }

                // Map for coordinate's, used to get an Array of GeoRelayView with relays on the same coordinate
                const latLonMap: Map<string, GeoRelayView[]> = buildLatLonMap(filteredRelays)

                // Map for family's, used to get an Array of GeoRelayView with relays in the same family
                const familyMap: Map<number, GeoRelayView[]> = buildFamilyMap(filteredRelays)
                if (settings.selectedFamily && !familyMap.has(settings.selectedFamily)) {
                    setSettingsCallback({...settings, selectedFamily: undefined})
                }
                if (settings.sortFamily && familyMap.size === 0) showSnackbarMessage({
                    message: "There are no families available for this day!",
                    severity: "warning"
                })

                // Concatenated Map for Families at Coordinates
                const familyCoordinatesMap: Map<string, Map<number, GeoRelayView[]>> = buildFamilyCoordinatesMap(latLonMap)

                // Map for country's, used to get an Array of GeoRelayView with relays in the same country
                const countryMap: Map<string, GeoRelayView[]> = getCountryMap(filteredRelays)
                if (settings.selectedCountry && !countryMap.has(settings.selectedCountry)) {
                    setSettingsCallback({...settings, selectedCountry: undefined})
                }

                // Draw Heatmap, draws a heatmap with a point for each coordinate
                // As this Layer is part of the component state and has to be applied to the map object directly, it cant be moved to util
                // https://github.com/Leaflet/Leaflet.heat
                if (settings.heatMap) {
                    leafletMap?.removeLayer(heatLayer)
                    let coordinates: Array<number[]> = new Array<number[]>()
                    filteredRelays.forEach(relay => coordinates.push([relay.lat, relay.long, 1]))
                    // @ts-ignore needed for compatibility with js code of the heatmap package
                    let heat = L.heatLayer(coordinates, {
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

                // Draw Country's, used to draw all countries to the map if at least one relay is hosted there
                if (leafletMap && countryMap.size > 0 && settings.sortCountry) {
                    countryLayer(countryMap, settings, setSettingsCallback).addTo(layerToReturn)
                }

                // Draw aggregated marker's, used to draw all markers to the map with more than 4 relays on the same coordinate
                if (settings.aggregateCoordinates) {
                    aggregatedCoordinatesLayer(latLonMap, openRelayDetailsDialog).addTo(layerToReturn)
                }

                // Draw familyCord marker's, used to draw all markers to the map with colors according to their family
                if (settings.sortFamily) {
                    if (settings.selectedFamily) familyLayer(familyMap, settings, setSettingsCallback).addTo(layerToReturn)
                    else familyCordLayer(familyCoordinatesMap, settings, setSettingsCallback, handleFamilyMarkerClick).addTo(layerToReturn)
                }

                // Draw marker's, used to draw all markers to the map with colors according to their type
                if (!settings.sortCountry) {
                    defaultMarkerLayer(latLonMap, openRelayDetailsDialog).addTo(layerToReturn)
                }



                // Draw country marker's, used to draw all markers to the map with colors according to their country
                if (settings.sortCountry) {
                    countryMarkerLayer(countryMap, settings, openRelayDetailsDialog).addTo(layerToReturn)
                }

                // Calculate statistics
                if (settings.selectedCountry && settings.selectedFamily && familyMap && countryMap) {
                    filteredRelays = []
                    familyMap.get(settings.selectedFamily)?.forEach(familyRelay => {
                        countryMap.get(settings.selectedCountry!!)?.forEach(countryRelay => {
                            if (familyRelay.detailsId === countryRelay.detailsId) filteredRelays.push(familyRelay)
                        })
                    })
                } else if (settings.selectedCountry && countryMap.has(settings.selectedCountry)) {
                    filteredRelays = countryMap.get(settings.selectedCountry)!!
                } else if (settings.selectedFamily && familyMap.has(settings.selectedFamily)) {
                    filteredRelays = familyMap.get(settings.selectedFamily)!!
                }

                setStatisticsCallback(calculateStatistics(filteredRelays, countryMap, familyMap, settings))

                console.timeLog(`relaysToLayerGroup`, `New Layer with ${filteredRelays.length} elements finished`)
                console.timeEnd(`relaysToLayerGroup`,)
            }
            return layerToReturn
        }

        closeSnackbar()
        if (relays) {
            drawLayerGroup(relaysToLayerGroup())
        }
    }, [relays, settings])

    /**
     * Handler for family selection
     * @param familyID selected familyID
     */
    function handleFamilySelection(familyID: number) {
        setSettingsCallback({...settings, selectedFamily: familyID})
        setShowFamilySelectionDialog(false)
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
            <RelayDetailsDialog
                showDialog={showRelayDetailsDialog}
                closeDialog={() => setShowRelayDetailsDialog(false)}
                relays={relaysForDetailsDialog}
            />
            <FamilySelectionDialog
                showDialog={showFamilySelectionDialog}
                closeDialog={() => setShowFamilySelectionDialog(false)}
                families={familiesForDetailsDialog}
                familySelectionCallback={handleFamilySelection}
            />
            <TileLayer
                maxZoom={19}
                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                noWrap={true}
            />
        </MapContainer>
    );
};
