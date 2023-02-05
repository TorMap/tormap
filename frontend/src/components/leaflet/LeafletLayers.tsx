import {LayerGroup, LeafletMouseEvent} from "leaflet";
import {useSnackbar} from "notistack";
import React, {FunctionComponent, Suspense, useCallback, useEffect, useMemo, useState} from "react";
import {useMap} from "react-leaflet";

import {useSettings} from "../../context/settings-context";
import {useStatistics} from "../../context/statistics-context";
import {RelayLocationDto} from "../../dto/relay";
import {SnackbarMessage} from "../../types/ui";
import {
    filterRelaysByFlags,
    buildFamilyCoordinatesMap,
    buildRelayCoordinatesMap,
    buildRelayCountryMap,
    buildRelayFamilyMap,
    buildStatistics, buildFilteredRelays
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
import {LoadingAnimation} from "../loading/LoadingAnimation";
import {atom, useAtom} from "jotai";
import {
    relaysForDetailsDialogAtom,
    showRelayDetailsDialogAtom
} from "../dialogs/relay/ResponsiveRelayDetailsDialog";
import {relayDetailsDialogSearchAtom} from "../dialogs/relay/RelayDetailsSelectionHeader";

// Lazy loaded components
const ResponsiveRelayDetailsDialog = React.lazy(() => import('../dialogs/relay/ResponsiveRelayDetailsDialog'));
const FamilySelectionDialog = React.lazy(() => import('../dialogs/family/FamilySelectionDialog'));

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

export const filteredRelaysAtom = atom<RelayLocationDto[]>([]);

export const LeafletLayers: FunctionComponent<Props> = ({relays, reloadSelectedDay}) => {
    // Component state
    const [showFamilySelectionDialog, setShowFamilySelectionDialog] = useState(false)
    const [familiesForSelectionDialog, setFamiliesForSelectionDialog] = useState<number[]>([])
    const [leafletMarkerLayers] = useState<LayerGroup>(new LayerGroup())

    // Atom state
    const [, setShowRelayDetailsDialog] = useAtom(showRelayDetailsDialogAtom)
    const [, setRelaysForDetailsDialog] = useAtom(relaysForDetailsDialogAtom)
    const [, setRelayDetailsDialogSearch] = useAtom(relayDetailsDialogSearchAtom)
    const [, setFilteredRelays] = useAtom(filteredRelaysAtom)

    // App context
    const leafletMap = useMap()
    const {settings, setSettings} = useSettings()
    const {setStatistics} = useStatistics()
    const {enqueueSnackbar, closeSnackbar} = useSnackbar();

    const filteredRelaysByFlags = useMemo(
        () => relays ? filterRelaysByFlags(relays, settings) : [],
        [relays, settings]
    )
    const relayCoordinatesMap = useMemo(
        () => buildRelayCoordinatesMap(filteredRelaysByFlags),
        [filteredRelaysByFlags]
    )
    const relayCountryMap = useMemo(
        () => buildRelayCountryMap(filteredRelaysByFlags),
        [filteredRelaysByFlags]
    )
    const relayFamilyMap = useMemo(
        () => buildRelayFamilyMap(filteredRelaysByFlags),
        [filteredRelaysByFlags]
    )
    const relayFamilyCoordinatesMap = useMemo(
        () => buildFamilyCoordinatesMap(relayCoordinatesMap),
        [relayCoordinatesMap]
    )
    const filteredRelays = useMemo(
        () => buildFilteredRelays(filteredRelaysByFlags, relayCountryMap, relayFamilyMap, settings),
        [filteredRelaysByFlags, relayCountryMap, relayFamilyMap, settings, setFilteredRelays]
    )

    /**
     * After a marker was clicked on the map the corresponding relays are passed to the details dialog
     * @param event - The leaflet marker click event
     */
    const openRelayDetailsDialog = useCallback((event: LeafletMouseEvent) => {
        if (relays) {
            const relaysAtCoordinate = relayCoordinatesMap?.get(event.target.options.className) ?? []
            setRelaysForDetailsDialog(relaysAtCoordinate)
            setShowRelayDetailsDialog(true)
            setRelayDetailsDialogSearch("")
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
        () => buildRelayHeatmapLayer(filteredRelaysByFlags),
        [filteredRelaysByFlags]
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
            setFilteredRelays(filteredRelays)

            if (!leafletMap.hasLayer(leafletMarkerLayers)) {
                leafletMap.addLayer(leafletMarkerLayers)
            }
            leafletMarkerLayers.clearLayers()

            if (filteredRelaysByFlags.length) {
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
    }, [filteredRelays, leafletMarkerLayers, relays, statistics, setStatistics, filteredRelaysByFlags.length, settings, relayFamilyMap, leafletMap, relayCountryMap, closeSnackbar, enqueueSnackbar, setSettings, relayLocationHeatmapLayer, countryBordersLayer, aggregatedCoordinatesLayer, relayCountryLayer, relayLayer, relaySelectedFamilyLayer, relayFamilyCoordinatesLayer])

    return (
        <Suspense fallback={<LoadingAnimation/>}>
            <ResponsiveRelayDetailsDialog/>
            <FamilySelectionDialog
                shouldShowDialog={showFamilySelectionDialog}
                closeDialog={useCallback(() => setShowFamilySelectionDialog(false), [])}
                reloadSelectedDay={reloadSelectedDay}
                familyIds={familiesForSelectionDialog}
            />
        </Suspense>
    );
};
