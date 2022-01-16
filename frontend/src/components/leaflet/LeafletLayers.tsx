import {useMap} from "react-leaflet";
import React, {FunctionComponent, useCallback, useEffect, useMemo, useState} from "react";
import {LayerGroup, LeafletMouseEvent} from "leaflet";
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
import {FamilySelectionDialog} from "../dialogs/family/FamilySelectionDialog";
import {useStatistics} from "../../context/statistics-context";


interface Props {
    /**
     * The relays which should be displayed on the map
     */
    relays?: RelayLocationDto[]

    /**
     * Trigger download of the current day
     */
    reloadSelectedDay: () => void
}

export const LeafletLayers: FunctionComponent<Props> = ({relays, reloadSelectedDay}) => {
    // Component state
    const [showFamilySelectionDialog, setShowFamilySelectionDialog] = useState(false)
    const [familiesForSelectionDialog, setFamiliesForSelectionDialog] = useState<number[]>([])
    const [showRelayDetailsDialog, setShowRelayDetailsDialog] = useState(false)
    const [relaysForDetailsDialog, setRelaysForDetailsDialog] = useState<RelayLocationDto[]>([])
    const [leafletMarkerLayers] = useState<LayerGroup>(new LayerGroup())

    // App context
    const leafletMap = useMap()
    const {settings, setSettings} = useSettings()
    const {setStatistics} = useStatistics()
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

    /**
     * Redraw relays whenever settings get changed or new relays got downloaded.
     */
    useEffect(() => {
        if (relays) {
            setStatistics(statistics)

            if (!leafletMap.hasLayer(leafletMarkerLayers)) {
                leafletMap.addLayer(leafletMarkerLayers)
            }
            leafletMarkerLayers.clearLayers()

            if (filteredRelays.length) {
                closeSnackbar(SnackbarMessage.NoRelaysWithFlags)
            } else {
                enqueueSnackbar(SnackbarMessage.NoRelaysWithFlags, {
                    variant: "warning",
                    key: SnackbarMessage.NoRelaysWithFlags
                })
                return
            }

            if (settings.selectedFamily && !relayFamilyMap.has(settings.selectedFamily)) {
                setSettings({...settings, selectedFamily: undefined})
            }

            if (settings.heatMap) {
                leafletMarkerLayers.addLayer(relayLocationHeatmapLayer)
            }

            if (leafletMap && relayCountryMap.size > 0 && settings.sortCountry) {
                leafletMarkerLayers.addLayer(countryBordersLayer)
            }

            if (settings.aggregateCoordinates) {
                leafletMarkerLayers.addLayer(aggregatedCoordinatesLayer)
            }

            if (settings.sortCountry) {
                if (settings.selectedCountry && !relayCountryMap.has(settings.selectedCountry)) {
                    setSettings({...settings, selectedCountry: undefined})
                }
                leafletMarkerLayers.addLayer(relayCountryLayer)
            } else {
                leafletMarkerLayers.addLayer(relayLayer)
            }

            if (settings.sortFamily) {
                if (settings.selectedFamily) leafletMarkerLayers.addLayer(relaySelectedFamilyLayer)
                else leafletMarkerLayers.addLayer(relayFamilyCoordinatesLayer)
            }
            if (settings.sortFamily && relayFamilyMap.size === 0) {
                enqueueSnackbar(SnackbarMessage.NoFamilyData, {variant: "warning", key: SnackbarMessage.NoFamilyData})
            } else {
                closeSnackbar(SnackbarMessage.NoFamilyData)
            }
        }
    }, [leafletMarkerLayers, relays, statistics, setStatistics, filteredRelays.length, settings, relayFamilyMap, leafletMap, relayCountryMap, closeSnackbar, enqueueSnackbar, setSettings, relayLocationHeatmapLayer, countryBordersLayer, aggregatedCoordinatesLayer, relayCountryLayer, relayLayer, relaySelectedFamilyLayer, relayFamilyCoordinatesLayer])

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
                reloadSelectedDay={reloadSelectedDay}
                familyIds={familiesForSelectionDialog}
            />
        </>
    );
};
