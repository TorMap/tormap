import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useCallback, useEffect, useMemo, useState} from "react";
import {LayerGroup, LeafletMouseEvent, Map as LeafletMap} from "leaflet";
import 'leaflet/dist/leaflet.css';
import {Settings, Statistics} from "../types/app-state";
import "leaflet.heat"
import {
    applyRelayFilter,
    buildFamilyCoordinatesMap,
    buildRelayCoordinatesMap,
    buildRelayCountryMap,
    buildRelayFamilyMap,
    buildStatistics
} from "../util/aggregate-relays";
import {
    buildAggregatedCoordinatesLayer,
    buildCountryLayer,
    buildRelayCountryLayer,
    buildRelayFamilyCoordinatesLayer,
    buildRelayFamilyLayer,
    buildRelayHeatmapLayer,
    buildRelayLayer
} from "../util/layer-construction";
import {backendApiUrl} from "../util/config";
import {GeoRelayView} from "../types/responses";
import {RelayDetailsDialog} from "./relay-details-dialog";
import {FamilySelectionDialog} from "./family-selection-dialog";
import {SnackbarMessage} from "../types/ui";
import {backend} from "../util/util";
import {useSnackbar} from "notistack";


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
                                                   }) => {
    const [showFamilySelectionDialog, setShowFamilySelectionDialog] = useState(false)
    const [familiesForSelectionDialog, setFamiliesForSelectionDialog] = useState<number[]>([])
    const [showRelayDetailsDialog, setShowRelayDetailsDialog] = useState(false)
    const [relaysForDetailsDialog, setRelaysForDetailsDialog] = useState<GeoRelayView[]>([])
    const [leafletMap, setLeafletMap] = useState<LeafletMap>()
    const [leafletMarkerLayer] = useState<LayerGroup>(new LayerGroup())
    const [relays, setRelays] = useState<GeoRelayView[]>()
    const [refreshDayCount, setRefreshDayCount] = useState(0)

    const {enqueueSnackbar, closeSnackbar} = useSnackbar();

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

    /**
     * After a marker was clicked on the map the corresponding relays are passed to the details dialog
     * @param event - The leaflet marker click event
     */
    const openRelayDetailsDialog = useCallback((event: LeafletMouseEvent) => {
        if (relays) {
            const relaysAtCoordinate = relayCoordinatesMap.get(event.target.options.className)!!
            setRelaysForDetailsDialog(relaysAtCoordinate)
            setShowRelayDetailsDialog(true)
        }
    }, [relayCoordinatesMap, relays])

    // Leaflet layer with heatmap of relay geo location
    const relayHeatmapLayer = useMemo(() => buildRelayHeatmapLayer(filteredRelays), [filteredRelays])

    // Leaflet layer with selectable country borders
    const countryLayer = useMemo(() => buildCountryLayer(relayCountryMap, settings, setSettings), [relayCountryMap, setSettings, settings])

    // Leaflet layer with aggregated coordinates
    const aggregatedCoordinatesLayer = useMemo(() => buildAggregatedCoordinatesLayer(relayCoordinatesMap, openRelayDetailsDialog), [openRelayDetailsDialog, relayCoordinatesMap])

    // Leaflet layer with selectable families
    const relayFamilyLayer = useMemo(() => buildRelayFamilyLayer(relayFamilyMap, settings, setSettings), [relayFamilyMap, setSettings, settings])

    // Leaflet layer with selectable country borders
    const relayFamilyCoordinatesLayer = useMemo(() =>
        buildRelayFamilyCoordinatesLayer(relayFamilyCoordinatesMap, settings, setSettings, (event: LeafletMouseEvent) => {
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
        }), [relayCoordinatesMap, relayFamilyCoordinatesMap, relays, setSettings, settings]
    )

    // Leaflet layer with selectable country borders
    const relayLayer = useMemo(() => buildRelayLayer(relayCoordinatesMap, settings.sortFamily, openRelayDetailsDialog), [openRelayDetailsDialog, relayCoordinatesMap, settings.sortFamily])

    // Leaflet layer with selectable country borders
    const relayCountryLayer = useMemo(() => buildRelayCountryLayer(relayCountryMap, settings, openRelayDetailsDialog), [openRelayDetailsDialog, relayCountryMap, settings])

    // Leaflet layer with selectable country borders
    const statistics = useMemo(() => buildStatistics(filteredRelays, relayCountryMap, relayFamilyMap, settings), [filteredRelays, relayCountryMap, relayFamilyMap, settings])

    // Leaflet layer with selectable country borders
    const leafletLayerGroup = useMemo(() => {
        const layerGroup = new LayerGroup()

        if (relays) {
            if (filteredRelays.length) {
                closeSnackbar(SnackbarMessage.NoRelaysWithFlags)
            } else {
                enqueueSnackbar(SnackbarMessage.NoRelaysWithFlags, {
                    variant: "warning",
                    key: SnackbarMessage.NoRelaysWithFlags
                })
                return layerGroup
            }

            if (settings.selectedFamily && !relayFamilyMap.has(settings.selectedFamily)) {
                setSettings({...settings, selectedFamily: undefined})
            }

            if (settings.sortFamily && relayFamilyMap.size === 0) {
                enqueueSnackbar(SnackbarMessage.NoFamilyData, {variant: "warning", key: SnackbarMessage.NoFamilyData})
            } else {
                closeSnackbar(SnackbarMessage.NoFamilyData)
            }

            if (settings.selectedCountry && !relayCountryMap.has(settings.selectedCountry)) {
                setSettings({...settings, selectedCountry: undefined})
            }

            // Draw Heatmap, draws a heatmap with a point for each coordinate
            if (settings.heatMap) {
                layerGroup.addLayer(relayHeatmapLayer)
            }

            // Draw Country's, used to draw all countries to the map if at least one relay is hosted there
            if (leafletMap && relayCountryMap.size > 0 && settings.sortCountry) {
                layerGroup.addLayer(countryLayer)
            }

            // Draw aggregated marker's, used to draw all markers to the map with more than 4 relays on the same coordinate
            if (settings.aggregateCoordinates) {
                aggregatedCoordinatesLayer.addTo(layerGroup)
            }

            // Draw familyCord marker's, used to draw all markers to the map with colors according to their family
            if (settings.sortFamily) {
                if (settings.selectedFamily) relayFamilyLayer.addTo(layerGroup)
                else relayFamilyCoordinatesLayer.addTo(layerGroup)
            }

            // Draw marker's, used to draw all markers to the map with colors according to their type
            if (!settings.sortCountry) {
                relayLayer.addTo(layerGroup)
            }

            // Draw country marker's, used to draw all markers to the map with colors according to their country
            if (settings.sortCountry) {
                relayCountryLayer.addTo(layerGroup)
            }

            setStatistics(statistics)
        }
        return layerGroup
    }, [aggregatedCoordinatesLayer, closeSnackbar, countryLayer, enqueueSnackbar, filteredRelays.length, leafletMap, relayCountryLayer, relayCountryMap, relayFamilyCoordinatesLayer, relayFamilyLayer, relayFamilyMap, relayHeatmapLayer, relayLayer, relays, setSettings, setStatistics, settings, statistics])


    /**
     * Query all Relays from the selected date whenever a new date is selected
     */
    useEffect(() => {
        if (dayToDisplay) {
            let currentTimeStamp = Date.now()
            setIsLoading(true)
            backend.get<GeoRelayView[]>(`${backendApiUrl}/archive/geo/relay/day/${dayToDisplay}`).then(response => {
                setIsLoading(false)
                if (currentTimeStamp === latestRequestTimestamp) setRelays(response.data)
            }).catch(() => {
                setIsLoading(false)
                enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
            })
            latestRequestTimestamp = currentTimeStamp
        }
    }, [dayToDisplay, enqueueSnackbar, setIsLoading, refreshDayCount])

    /**
     * Redraw relays whenever settings get changed or an new relays got downloaded.
     * Draws a group of layers according to settings on the map.
     */
    useEffect(() => {
        if (relays) {
            leafletMarkerLayer.clearLayers()
            leafletLayerGroup.getLayers().forEach((layer) => layer.addTo(leafletMarkerLayer))
        }
    }, [leafletLayerGroup, leafletMarkerLayer, relays])

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
            style={{
                width: "100vw",
                height: "100vh",
                backgroundColor: "#262626",
                position: "fixed",
            }}
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
                leafletMarkerLayer.addTo(newMap)
                setLeafletMap(newMap)
            }}
        >
            <RelayDetailsDialog
                showDialog={showRelayDetailsDialog}
                closeDialog={useCallback(() => setShowRelayDetailsDialog(false), [])}
                relays={relaysForDetailsDialog}
            />
            <FamilySelectionDialog
                showDialog={showFamilySelectionDialog}
                closeDialog={useCallback(() => setShowFamilySelectionDialog(false), [])}
                refreshDayData={useCallback(() => setRefreshDayCount(prevState => prevState + 1), [])}
                familyIds={familiesForSelectionDialog}
                familySelectionCallback={useCallback(handleFamilySelection, [setSettings, settings])}
            />
            <TileLayer
                maxZoom={19}
                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                noWrap={true}
            />
        </MapContainer>
    );
};
