import {useMap} from "react-leaflet";
import React, {Fragment, FunctionComponent, useCallback, useEffect, useMemo, useState} from "react";
import {LayerGroup, LeafletMouseEvent} from "leaflet";
import 'leaflet/dist/leaflet.css';
import "leaflet.heat"
import {
    applyRelayFilter,
    buildFamilyCoordinatesMap,
    buildRelayCoordinatesMap,
    buildRelayCountryMap,
    buildRelayFamilyMap,
    buildStatistics
} from "../../util/aggregate-relays";
import {
    buildAggregatedCoordinatesLayer,
    buildCountryLayer,
    buildFamilyCoordinatesLayer,
    buildRelayCountryLayer,
    buildRelayHeatmapLayer,
    buildRelayLayer,
    buildSelectedFamilyLayer
} from "../../util/layer-construction";
import {RelayLocationDto} from "../../dto/relay";
import {SnackbarMessage} from "../../types/ui";
import {useSnackbar} from "notistack";
import {useSettings} from "../../context/settings-context";
import {ResponsiveRelayDetailsDialog} from "../dialogs/relay/ResponsiveRelayDetailsDialog";
import {FamilySelectionDialog} from "../dialogs/family/FamilySelectionUtil";
import {useStatistics} from "../../context/statistics-context";


interface Props {
    /**
     * The relays which should be displayed on the map
     */
    relays?: RelayLocationDto[]

    /**
     * A variable callback whether the map is currently fetching a new date
     * @param b whether the map is currently fetching a new date
     */
    setIsLoading: (b: boolean) => void
}

export const LeafletLayers: FunctionComponent<Props> = ({relays, setIsLoading}) => {
    const [showFamilySelectionDialog, setShowFamilySelectionDialog] = useState(false)
    const [familiesForSelectionDialog, setFamiliesForSelectionDialog] = useState<number[]>([])
    const [showRelayDetailsDialog, setShowRelayDetailsDialog] = useState(false)
    const [relaysForDetailsDialog, setRelaysForDetailsDialog] = useState<RelayLocationDto[]>([])
    const [leafletMarkerLayer] = useState<LayerGroup>(new LayerGroup())

    const settings = useSettings().settings
    const setSettings = useSettings().setSettings
    const setStatistics = useStatistics().setStatistics
    const leafletMap = useMap()
    // TODO fix console error: Cannot update during an existing state transition
    const {enqueueSnackbar, closeSnackbar} = useSnackbar();

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
        }
        return layerGroup
    }, [aggregatedCoordinatesLayer, closeSnackbar, countryBordersLayer, enqueueSnackbar, filteredRelays.length, leafletMap, relayCountryLayer, relayCountryMap, relayFamilyCoordinatesLayer, relaySelectedFamilyLayer, relayFamilyMap, relayLocationHeatmapLayer, relayLayer, relays, setSettings, settings])

    /**
     * Redraw relays whenever settings get changed or new relays got downloaded.
     * Draws a group of layers according to settings on the map.
     */
    useEffect(() => {
        if (relays) {
            leafletMarkerLayer.clearLayers()
            leafletLayerGroup.getLayers().forEach((layer) => layer.addTo(leafletMarkerLayer))
            setStatistics(statistics)
        }
    }, [leafletLayerGroup, leafletMarkerLayer, relays, statistics, setStatistics])

    useEffect(() => {
        leafletMap.addLayer(leafletMarkerLayer)
        /* eslint-disable react-hooks/exhaustive-deps */
    }, [])

    return (
        <>
            <ResponsiveRelayDetailsDialog
                showDialog={showRelayDetailsDialog}
                closeDialog={useCallback(() => setShowRelayDetailsDialog(false), [])}
                relayLocations={relaysForDetailsDialog}
            />
            <FamilySelectionDialog
                showDialog={showFamilySelectionDialog}
                closeDialog={useCallback(() => setShowFamilySelectionDialog(false), [])}
                refreshDayData={useCallback(() => setIsLoading(true), [])}
                familyIds={familiesForSelectionDialog}
            />
        </>
    );
};
