import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useCallback, useEffect, useMemo, useState} from "react";
import L, {Layer, LayerGroup, LeafletMouseEvent, Map as LeafletMap} from "leaflet";
import 'leaflet/dist/leaflet.css';
import {Settings, Statistics} from "../types/app-state";
import "leaflet.heat"
import {makeStyles} from "@material-ui/core";
import {
    applyRelayFilter,
    buildFamilyCoordinatesMap,
    buildRelayFamilyMap,
    buildRelayCoordinatesMap,
    calculateStatistics,
    buildRelayCountryMap
} from "../util/aggregate-relays";
import {
    aggregatedCoordinatesLayer,
    countryLayer,
    relayCountryLayer,
    relayLayer,
    relayFamilyCoordinatesLayer,
    relayFamilyLayer, buildRelayHeatmapLayer
} from "../util/layer-construction";
import {apiBaseUrl} from "../util/config";
import {GeoRelayView} from "../types/responses";
import {RelayDetailsDialog} from "./relay-details-dialog";
import {FamilySelectionDialog} from "./family-selection-dialog";
import {SnackbarMessage, SnackbarMessages} from "../types/ui";

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
    setSettings: (s: Settings) => void

    /**
     * A variable callback whether the map is currently fetching a new date
     * @param b whether the map is currently fetching a new date
     */
    setIsLoading: (b: boolean) => void

    /**
     * A callback to change statistics
     * @param stats - the statistic variable that should be changed
     */
    setStatistics: (stats: Statistics) => void

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
Instead only the last selected date will be drawn.
 */
let latestRequestTimestamp: number | undefined = undefined

export const WorldMap: FunctionComponent<Props> = ({
                                                       dayToDisplay,
                                                       settings,
                                                       setSettings,
                                                       setIsLoading,
                                                       setStatistics,
                                                       showSnackbarMessage,
                                                       closeSnackbar,
                                                   }) => {
    const [showFamilySelectionDialog, setShowFamilySelectionDialog] = useState(false)
    const [familiesForSelectionDialog, setFamiliesForSelectionDialog] = useState<number[]>([])
    const [showRelayDetailsDialog, setShowRelayDetailsDialog] = useState(false)
    const [relaysForDetailsDialog, setRelaysForDetailsDialog] = useState<GeoRelayView[]>([])
    const [leafletMap, setLeafletMap] = useState<LeafletMap>()
    const [markerLayer] = useState<LayerGroup>(new LayerGroup())
    const [relayHeatmapLayerReference, setRelayHeatmapLayerReference] = useState<Layer>(new Layer())
    const [relays, setRelays] = useState<GeoRelayView[]>()
    const classes = useStyle()

    // Remaining relays after filters from settings are applied
    const filteredRelays = useMemo(() => relays ? applyRelayFilter(relays, settings) : [], [relays, settings])

    // Map of relays with the same coordinates
    const relayCoordinatesMap = useMemo(() => buildRelayCoordinatesMap(filteredRelays), [filteredRelays])

    // Map of relays with the same family
    const relayFamilyMap = useMemo(() => buildRelayFamilyMap(filteredRelays), [filteredRelays])

    // Map of families with the same coordinates
    const relayFamilyCoordinatesMap = useMemo(() => buildFamilyCoordinatesMap(relayCoordinatesMap), [relayCoordinatesMap])

    // Map of relays in the same country
    const relayCountryMap = useMemo(() => buildRelayCountryMap(filteredRelays), [filteredRelays])

    // Leaflet layer with heatmap of relay geo location
    const relayHeatmapLayer = useMemo(() => buildRelayHeatmapLayer(filteredRelays), [filteredRelays])

    /**
     * Query all Relays from the selected date whenever a new date is selected
     */
    useEffect(() => {
        if (dayToDisplay) {
            let currentTimeStamp = Date.now()
            setIsLoading(true)
            fetch(`${apiBaseUrl}/archive/geo/relay/day/${dayToDisplay}`)
                .then(response => response.json())
                .then((newRelays: GeoRelayView[]) => {
                    setIsLoading(false)
                    if (currentTimeStamp === latestRequestTimestamp) setRelays(newRelays)
                })
                .catch(() => {
                    showSnackbarMessage({message: SnackbarMessages.ConnectionFailed, severity: "error"})
                })
            latestRequestTimestamp = currentTimeStamp
        }
    }, [dayToDisplay, setIsLoading, showSnackbarMessage])

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
                const relaysAtCoordinate = relayCoordinatesMap.get(event.target.options.className)!!
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
                const relaysAtCoordinate = relayCoordinatesMap.get(event.target.options.className)!!
                const familyMap = buildRelayFamilyMap(relaysAtCoordinate)
                let families: number[] = []
                familyMap.forEach((family, familyID) => {
                    families.push(familyID)
                })
                if (families.length === 1) {
                    setSettings({...settings, selectedFamily: families[0]})
                    return
                }
                setFamiliesForSelectionDialog(families)
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
                if (!filteredRelays.length) {
                    showSnackbarMessage({message: SnackbarMessages.NoRelaysWithFlags, severity: "warning"})
                    return layerToReturn
                }

                if (settings.selectedFamily && !relayFamilyMap.has(settings.selectedFamily)) {
                    setSettings({...settings, selectedFamily: undefined})
                }
                if (settings.sortFamily && relayFamilyMap.size === 0) showSnackbarMessage({
                    message: SnackbarMessages.NoFamilyData,
                    severity: "warning"
                })

                if (settings.selectedCountry && !relayCountryMap.has(settings.selectedCountry)) {
                    setSettings({...settings, selectedCountry: undefined})
                }

                // Draw Heatmap, draws a heatmap with a point for each coordinate
                // As this Layer is part of the component state and has to be applied to the map object directly, it cant be moved to util/layer-construction.ts
                // https://github.com/Leaflet/Leaflet.heat
                leafletMap?.removeLayer(relayHeatmapLayerReference)
                if (settings.heatMap) {
                    leafletMap?.addLayer(relayHeatmapLayer)
                    setRelayHeatmapLayerReference(relayHeatmapLayer)
                }

                // Draw Country's, used to draw all countries to the map if at least one relay is hosted there
                if (leafletMap && relayCountryMap.size > 0 && settings.sortCountry) {
                    countryLayer(relayCountryMap, settings, setSettings).addTo(layerToReturn)
                }

                // Draw aggregated marker's, used to draw all markers to the map with more than 4 relays on the same coordinate
                if (settings.aggregateCoordinates) {
                    aggregatedCoordinatesLayer(relayCoordinatesMap, openRelayDetailsDialog).addTo(layerToReturn)
                }

                // Draw familyCord marker's, used to draw all markers to the map with colors according to their family
                if (settings.sortFamily) {
                    if (settings.selectedFamily) relayFamilyLayer(relayFamilyMap, settings, setSettings).addTo(layerToReturn)
                    else relayFamilyCoordinatesLayer(relayFamilyCoordinatesMap, settings, setSettings, handleFamilyMarkerClick).addTo(layerToReturn)
                }

                // Draw marker's, used to draw all markers to the map with colors according to their type
                if (!settings.sortCountry) {
                    const singleColor = settings.sortFamily
                    relayLayer(relayCoordinatesMap, singleColor, openRelayDetailsDialog).addTo(layerToReturn)
                }

                // Draw country marker's, used to draw all markers to the map with colors according to their country
                if (settings.sortCountry) {
                    relayCountryLayer(relayCountryMap, settings, openRelayDetailsDialog).addTo(layerToReturn)
                }

                setStatistics(calculateStatistics(relayCountryMap, relayFamilyMap, settings))
            }
            return layerToReturn
        }

        closeSnackbar()
        if (relays) {
            drawLayerGroup(relaysToLayerGroup())
        }
        // TODO add missing dependencies and refactor useEffect
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [closeSnackbar, relays, setSettings, setStatistics, settings, showSnackbarMessage])

    /**
     * Handler for family selection
     * @param familyID selected familyID
     */
    function handleFamilySelection(familyID: number) {
        setSettings({...settings, selectedFamily: familyID})
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
                closeDialog={useCallback(() => setShowRelayDetailsDialog(false), [])}
                relays={relaysForDetailsDialog}
                showSnackbarMessage={showSnackbarMessage}
            />
            <FamilySelectionDialog
                showDialog={showFamilySelectionDialog}
                closeDialog={() => setShowFamilySelectionDialog(false)}
                families={familiesForSelectionDialog}
                familySelectionCallback={handleFamilySelection}
                showSnackbarMessage={showSnackbarMessage}
            />
            <TileLayer
                maxZoom={19}
                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                noWrap={true}
            />
        </MapContainer>
    );
};
