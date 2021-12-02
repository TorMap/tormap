import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useCallback, useEffect, useMemo, useState} from "react";
import {LayerGroup, LeafletMouseEvent, Map as LeafletMap} from "leaflet";
import 'leaflet/dist/leaflet.css';
import {Statistics} from "../types/app-state";
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
    buildFamilyCoordinatesLayer,
    buildSelectedFamilyLayer,
    buildRelayHeatmapLayer,
    buildRelayLayer
} from "../util/layer-construction";
import {RelayLocationDto} from "../types/responses";
import {RelayDetailsDialogLarge} from "./dialogs/RelayDetailsDialogLarge";
import {FamilySelectionDialogLarge} from "./dialogs/FamilySelectionDialogLarge";
import {SnackbarMessage} from "../types/ui";
import {backend} from "../util/util";
import {useSnackbar} from "notistack";
import {useSettings} from "../util/settings-context";
import {useDate} from "../util/date-context";


interface Props {
    /**
     * This days data will be fetched from backend and visualized on the map
     */
    //dayToDisplay?: string


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
                                                       setIsLoading,
                                                       setStatistics,
                                                   }) => {
    const [showFamilySelectionDialog, setShowFamilySelectionDialog] = useState(false)
    const [familiesForSelectionDialog, setFamiliesForSelectionDialog] = useState<number[]>([])
    const [showRelayDetailsDialog, setShowRelayDetailsDialog] = useState(false)
    const [relaysForDetailsDialog, setRelaysForDetailsDialog] = useState<RelayLocationDto[]>([])
    const [leafletMap, setLeafletMap] = useState<LeafletMap>()
    const [leafletMarkerLayer] = useState<LayerGroup>(new LayerGroup())
    const [relays, setRelays] = useState<RelayLocationDto[]>()
    const [refreshDayCount, setRefreshDayCount] = useState(0)

    const {enqueueSnackbar, closeSnackbar} = useSnackbar();
    const settings = useSettings().settings
    const setSettings = useSettings().setSettings
    const dayToDisplay = useDate().selectedDate

    const filteredRelays = useMemo(
        () => relays ? applyRelayFilter(relays, settings) : [],
        [relays, settings]
    )
    const relayCoordinatesMap = useMemo(
        () => buildRelayCoordinatesMap(filteredRelays),
        [filteredRelays]
    )
    const relayCountryMap = useMemo(
        () => buildRelayCountryMap(filteredRelays),
        [filteredRelays]
    )
    const relayFamilyMap = useMemo(
        () => buildRelayFamilyMap(filteredRelays),
        [filteredRelays]
    )
    const relayFamilyCoordinatesMap = useMemo(
        () => buildFamilyCoordinatesMap(relayCoordinatesMap),
        [relayCoordinatesMap]
    )

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

    const relayLayer = useMemo(
        () => buildRelayLayer(relayCoordinatesMap, settings.sortFamily, openRelayDetailsDialog),
        [openRelayDetailsDialog, relayCoordinatesMap, settings.sortFamily]
    )
    const relayCountryLayer = useMemo(
        () => buildRelayCountryLayer(relayCountryMap, settings, openRelayDetailsDialog),
        [openRelayDetailsDialog, relayCountryMap, settings]
    )
    const countryBordersLayer = useMemo(
        () => buildCountryLayer(relayCountryMap, settings, setSettings),
        [relayCountryMap, setSettings, settings]
    )
    const relayLocationHeatmapLayer = useMemo(
        () => buildRelayHeatmapLayer(filteredRelays),
        [filteredRelays]
    )
    const aggregatedCoordinatesLayer = useMemo(
        () => buildAggregatedCoordinatesLayer(relayCoordinatesMap, openRelayDetailsDialog),
        [openRelayDetailsDialog, relayCoordinatesMap]
    )
    const relaySelectedFamilyLayer = useMemo(
        () => buildSelectedFamilyLayer(relayFamilyMap, settings, setSettings),
        [relayFamilyMap, setSettings, settings]
    )
    const relayFamilyCoordinatesLayer = useMemo(
        () => buildFamilyCoordinatesLayer(relayFamilyCoordinatesMap, settings, setSettings, setFamiliesForSelectionDialog, setShowFamilySelectionDialog),
        [relayFamilyCoordinatesMap, settings, setSettings, setFamiliesForSelectionDialog, setShowFamilySelectionDialog]
    )

    const statistics = useMemo(
        () => buildStatistics(filteredRelays, relayCountryMap, relayFamilyMap, settings),
        [filteredRelays, relayCountryMap, relayFamilyMap, settings]
    )

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

            if (settings.heatMap) {
                layerGroup.addLayer(relayLocationHeatmapLayer)
            }

            if (leafletMap && relayCountryMap.size > 0 && settings.sortCountry) {
                layerGroup.addLayer(countryBordersLayer)
            }

            if (settings.aggregateCoordinates) {
                layerGroup.addLayer(aggregatedCoordinatesLayer)
            }

            if (settings.sortCountry) {
                if (settings.selectedCountry && !relayCountryMap.has(settings.selectedCountry)) {
                    setSettings({...settings, selectedCountry: undefined})
                }
                layerGroup.addLayer(relayCountryLayer)
            } else {
                layerGroup.addLayer(relayLayer)
            }

            if (settings.sortFamily) {
                if (settings.selectedFamily) layerGroup.addLayer(relaySelectedFamilyLayer)
                else layerGroup.addLayer(relayFamilyCoordinatesLayer)
            }
            if (settings.sortFamily && relayFamilyMap.size === 0) {
                enqueueSnackbar(SnackbarMessage.NoFamilyData, {variant: "warning", key: SnackbarMessage.NoFamilyData})
            } else {
                closeSnackbar(SnackbarMessage.NoFamilyData)
            }

            setStatistics(statistics)
        }
        return layerGroup
    }, [aggregatedCoordinatesLayer, closeSnackbar, countryBordersLayer, enqueueSnackbar, filteredRelays.length, leafletMap, relayCountryLayer, relayCountryMap, relayFamilyCoordinatesLayer, relaySelectedFamilyLayer, relayFamilyMap, relayLocationHeatmapLayer, relayLayer, relays, setSettings, setStatistics, settings, statistics])


    /**
     * Query all Relays from the selected date whenever a new date is selected
     */
    useEffect(() => {
        if (dayToDisplay) {
            let currentTimeStamp = Date.now()
            setIsLoading(true)
            backend.get<RelayLocationDto[]>(`/relay/location/day/${dayToDisplay}`).then(response => {
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
            <RelayDetailsDialogLarge
                showDialog={showRelayDetailsDialog}
                closeDialog={useCallback(() => setShowRelayDetailsDialog(false), [])}
                relays={relaysForDetailsDialog}
            />
            <FamilySelectionDialogLarge
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
