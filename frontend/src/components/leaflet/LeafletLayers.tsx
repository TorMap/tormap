import {LayerGroup, latLngBounds, LeafletMouseEvent} from "leaflet";
import React, {FunctionComponent, Suspense, useCallback, useEffect, useMemo, useState} from "react";

import {useDate} from "../../context/date-context";
import {useSettings} from "../../context/settings-context";
import {useSnackbar} from "../../context/snackbar-context";
import {useStatistics} from "../../context/statistics-context";
import {RelayLocationDto} from "../../dto/relay";
import {SnackbarMessage} from "../../types/ui";
import {
    filterRelaysByFlags,
    buildGroupCoordinatesMap,
    buildRelayAutonomousSystemMap,
    buildRelayCoordinatesMap,
    buildRelayCountryMap,
    buildRelayFamilyMap,
    buildStatistics, buildFilteredRelays
} from "../../util/aggregate-relays";
import {CountryFeature, loadWorldFeatures} from "../../util/geojson";
import {
    buildAggregatedCoordinatesLayer,
    buildCountryLayer,
    buildGroupCoordinatesLayer,
    buildRelayCountryLayer,
    buildRelayHeatmapLayer,
    buildRelayLayer,
    buildSelectedGroupLayer,
    calculateAutonomousSystemColor,
    calculateFamilyColor,
    RelayGrouping
} from "../../util/layer-construction";
import {LoadingAnimation} from "../loading/LoadingAnimation";
import {atom, useAtom} from "jotai";
import {
    relayDetailsDialogSearchAtom,
    relaysForDetailsDialogAtom,
    relaySearchOnMapAtom,
    showRelayDetailsDialogAtom
} from "../dialogs/relay/relay-details-atoms";
import {autonomousSystemSelectionAtom, familySelectionAtom, groupsOnMapAtom} from "../dialogs/group-selection-atoms";
import {useMap} from "./map";

// Lazy loaded components
const ResponsiveRelayDetailsDialog = React.lazy(() => import('../dialogs/relay/ResponsiveRelayDetailsDialog'));
const FamilySelectionDialog = React.lazy(() => import('../dialogs/family/FamilySelectionDialog'));
const AutonomousSystemSelectionDialog = React.lazy(() => import('../dialogs/autonomous-system/AutonomousSystemSelectionDialog'));

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

/**
 * How many of the relays of the selected day the map shows after the search, flag and weight limits
 */
export const shownRelayCountAtom = atom<{ shown: number, total: number }>({shown: 0, total: 0});

export const LeafletLayers: FunctionComponent<Props> = ({relays, reloadSelectedDay}) => {
    // Component state
    // The dialogs are separate chunks and are only loaded when they are opened for the first time. They stay mounted
    // afterwards, so they can close with their transition.
    const [hasOpenedRelayDetailsDialog, setHasOpenedRelayDetailsDialog] = useState(false)
    const [hasOpenedFamilySelectionDialog, setHasOpenedFamilySelectionDialog] = useState(false)
    const [hasOpenedAutonomousSystemSelectionDialog, setHasOpenedAutonomousSystemSelectionDialog] = useState(false)
    const [leafletMarkerLayers] = useState<LayerGroup>(new LayerGroup())
    // The country borders are a separate chunk, which is only loaded once the countries are shown
    const [worldFeatures, setWorldFeatures] = useState<CountryFeature[]>()

    // Atom state
    const [showRelayDetailsDialog, setShowRelayDetailsDialog] = useAtom(showRelayDetailsDialogAtom)
    const [, setRelaysForDetailsDialog] = useAtom(relaysForDetailsDialogAtom)
    const [, setRelayDetailsDialogSearch] = useAtom(relayDetailsDialogSearchAtom)
    const [, setFilteredRelays] = useAtom(filteredRelaysAtom)
    const [, setShownRelayCount] = useAtom(shownRelayCountAtom)
    const [relaySearchOnMap, setRelaySearchOnMap] = useAtom(relaySearchOnMapAtom)
    const [familySelection, setFamilySelection] = useAtom(familySelectionAtom)
    const [autonomousSystemSelection, setAutonomousSystemSelection] = useAtom(autonomousSystemSelectionAtom)
    const [, setGroupsOnMap] = useAtom(groupsOnMapAtom)
    if (showRelayDetailsDialog && !hasOpenedRelayDetailsDialog) setHasOpenedRelayDetailsDialog(true)
    if (familySelection.show && !hasOpenedFamilySelectionDialog) setHasOpenedFamilySelectionDialog(true)
    if (autonomousSystemSelection.show && !hasOpenedAutonomousSystemSelectionDialog) setHasOpenedAutonomousSystemSelectionDialog(true)

    // App context
    const leafletMap = useMap()
    const {settings, setSettings} = useSettings()
    const {setStatistics} = useStatistics()
    const {enqueueSnackbar, closeSnackbar} = useSnackbar();
    const {selectedDate} = useDate()

    // Only the relays found by a search, as long as the selected day still has some of them
    const searchedRelays = useMemo(
        () => {
            if (!relays || !relaySearchOnMap) return relays
            const searchedIds = new Set(relaySearchOnMap.relays.map(relay => relay.detailsId))
            const matches = relays.filter(relay => relay.detailsId && searchedIds.has(relay.detailsId))
            return matches.length > 0 ? matches : relays
        },
        [relays, relaySearchOnMap]
    )
    const filteredRelaysByFlags = useMemo(
        () => searchedRelays ? filterRelaysByFlags(searchedRelays, settings) : [],
        [searchedRelays, settings]
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
        () => buildGroupCoordinatesMap(relayCoordinatesMap, buildRelayFamilyMap),
        [relayCoordinatesMap]
    )
    const relayAutonomousSystemMap = useMemo(
        () => buildRelayAutonomousSystemMap(filteredRelaysByFlags),
        [filteredRelaysByFlags]
    )
    const relayAutonomousSystemCoordinatesMap = useMemo(
        () => buildGroupCoordinatesMap(relayCoordinatesMap, buildRelayAutonomousSystemMap),
        [relayCoordinatesMap]
    )
    const totalConsensusWeight = useMemo(
        () => filteredRelaysByFlags.reduce((sum, relay) => sum + (relay.consensusWeight ?? 0), 0),
        [filteredRelaysByFlags]
    )
    const filteredRelays = useMemo(
        () => buildFilteredRelays(filteredRelaysByFlags, relayCountryMap, relayFamilyMap, settings, relayAutonomousSystemMap),
        [filteredRelaysByFlags, relayCountryMap, relayFamilyMap, settings, relayAutonomousSystemMap]
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
    }, [relayCoordinatesMap, relays, setRelaysForDetailsDialog, setShowRelayDetailsDialog, setRelayDetailsDialogSearch])

    const relayLayer = useMemo(
        () => buildRelayLayer(relayCoordinatesMap, settings.sortFamily || settings.sortAutonomousSystem, settings.relayTypeColors, openRelayDetailsDialog, settings.sizeByWeight, settings.sizeByWeight ? settings.topRelayCount : undefined),
        [openRelayDetailsDialog, relayCoordinatesMap, settings.sortFamily, settings.sortAutonomousSystem, settings.relayTypeColors, settings.sizeByWeight, settings.topRelayCount]
    )
    const relayCountryLayer = useMemo(
        () => buildRelayCountryLayer(relayCountryMap, settings, openRelayDetailsDialog),
        [openRelayDetailsDialog, relayCountryMap, settings]
    )
    const countryBordersLayer = useMemo(
        () => worldFeatures ? buildCountryLayer(relayCountryMap, settings, setSettings, worldFeatures) : undefined,
        [relayCountryMap, setSettings, settings, worldFeatures]
    )
    const relayLocationHeatmapLayer = useMemo(
        () => buildRelayHeatmapLayer(filteredRelaysByFlags),
        [filteredRelaysByFlags]
    )
    const aggregatedCoordinatesLayer = useMemo(
        () => buildAggregatedCoordinatesLayer(relayCoordinatesMap, openRelayDetailsDialog),
        [openRelayDetailsDialog, relayCoordinatesMap]
    )
    const familyGrouping = useMemo((): RelayGrouping => ({
        selectedId: settings.selectedFamily,
        color: calculateFamilyColor,
        select: familyId => setSettings({...settings, selectedFamily: familyId}),
        openSelectionDialog: familyIds => setFamilySelection({show: true, ids: familyIds}),
    }), [settings, setSettings, setFamilySelection])
    const autonomousSystemGrouping = useMemo((): RelayGrouping => ({
        selectedId: settings.selectedAutonomousSystem,
        color: calculateAutonomousSystemColor,
        select: asNumber => setSettings({...settings, selectedAutonomousSystem: asNumber}),
        openSelectionDialog: asNumbers => setAutonomousSystemSelection({show: true, ids: asNumbers}),
    }), [settings, setSettings, setAutonomousSystemSelection])
    const relaySelectedFamilyLayer = useMemo(
        () => buildSelectedGroupLayer(relayFamilyMap, familyGrouping),
        [relayFamilyMap, familyGrouping]
    )
    const relayFamilyCoordinatesLayer = useMemo(
        () => buildGroupCoordinatesLayer(relayFamilyCoordinatesMap, familyGrouping),
        [relayFamilyCoordinatesMap, familyGrouping]
    )
    const relaySelectedAutonomousSystemLayer = useMemo(
        () => buildSelectedGroupLayer(relayAutonomousSystemMap, autonomousSystemGrouping),
        [relayAutonomousSystemMap, autonomousSystemGrouping]
    )
    const relayAutonomousSystemCoordinatesLayer = useMemo(
        () => buildGroupCoordinatesLayer(relayAutonomousSystemCoordinatesMap, autonomousSystemGrouping),
        [relayAutonomousSystemCoordinatesMap, autonomousSystemGrouping]
    )
    const statistics = useMemo(
        () => buildStatistics(filteredRelays, relayCountryMap, relayFamilyMap, settings, relayAutonomousSystemMap),
        [filteredRelays, relayCountryMap, relayFamilyMap, settings, relayAutonomousSystemMap]
    )

    /**
     * Tell the search buttons which families and autonomous systems the map shows
     */
    useEffect(() => {
        setGroupsOnMap({familyIds: [...relayFamilyMap.keys()], asNumbers: [...relayAutonomousSystemMap.keys()]})
    }, [relayFamilyMap, relayAutonomousSystemMap, setGroupsOnMap])

    /**
     * Forget the search once the selected day has none of the searched relays, e.g. in another month
     */
    useEffect(() => {
        if (relays && relaySearchOnMap && searchedRelays === relays) {
            setRelaySearchOnMap(undefined)
        }
    }, [relays, relaySearchOnMap, searchedRelays, setRelaySearchOnMap])

    /**
     * Load the country borders once the countries are shown, switching the setting again retries a failed load
     */
    useEffect(() => {
        if (!settings.sortCountry || worldFeatures) return undefined
        let isCancelled = false
        loadWorldFeatures().then(features => {
            if (!isCancelled) setWorldFeatures(features)
        }).catch(() => {
            if (!isCancelled) enqueueSnackbar(SnackbarMessage.CountryBordersFailed, {variant: "error"})
        })
        return () => {
            isCancelled = true
        }
    }, [settings.sortCountry, worldFeatures, enqueueSnackbar])

    /**
     * Move the map to the relays of a new search
     */
    useEffect(() => {
        if (relaySearchOnMap && relaySearchOnMap.relays.length > 0) {
            leafletMap.fitBounds(
                latLngBounds(relaySearchOnMap.relays.map(relay => [relay.lat, relay.long])),
                {maxZoom: 6, padding: [50, 50], animate: false}
            )
        }
    }, [leafletMap, relaySearchOnMap])

    /**
     * Redraw relays whenever settings get changed or new relays got downloaded.
     */
    useEffect(() => {
        if (relays) {
            setStatistics(statistics)
            setFilteredRelays(filteredRelays)
            setShownRelayCount({shown: filteredRelaysByFlags.length, total: relays.length})

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
            if (settings.selectedAutonomousSystem && !relayAutonomousSystemMap.has(settings.selectedAutonomousSystem)) {
                setSettings({...settings, selectedAutonomousSystem: undefined})
            }

            if (settings.heatMap) {
                leafletMarkerLayers.addLayer(relayLocationHeatmapLayer)
            }

            if (leafletMap && relayCountryMap.size > 0 && settings.sortCountry && countryBordersLayer) {
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
            if (settings.sortAutonomousSystem) {
                if (settings.selectedAutonomousSystem) leafletMarkerLayers.addLayer(relaySelectedAutonomousSystemLayer)
                else leafletMarkerLayers.addLayer(relayAutonomousSystemCoordinatesLayer)
            }
            if (settings.sortFamily && relayFamilyMap.size === 0) {
                enqueueSnackbar(SnackbarMessage.NoFamilyData, {variant: "warning", key: SnackbarMessage.NoFamilyData})
            } else {
                closeSnackbar(SnackbarMessage.NoFamilyData)
            }
        }
    }, [filteredRelays, setFilteredRelays, setShownRelayCount, leafletMarkerLayers, relays, statistics, setStatistics, filteredRelaysByFlags.length, settings, relayFamilyMap, leafletMap, relayCountryMap, closeSnackbar, enqueueSnackbar, setSettings, relayLocationHeatmapLayer, countryBordersLayer, aggregatedCoordinatesLayer, relayCountryLayer, relayLayer, relaySelectedFamilyLayer, relayFamilyCoordinatesLayer, relayAutonomousSystemMap, relaySelectedAutonomousSystemLayer, relayAutonomousSystemCoordinatesLayer])

    const closeFamilySelectionDialog = useCallback(
        () => setFamilySelection(selection => ({...selection, show: false})),
        [setFamilySelection]
    )
    const closeAutonomousSystemSelectionDialog = useCallback(
        () => setAutonomousSystemSelection(selection => ({...selection, show: false})),
        [setAutonomousSystemSelection]
    )

    return (
        <Suspense fallback={<LoadingAnimation/>}>
            {hasOpenedRelayDetailsDialog ? <ResponsiveRelayDetailsDialog/> : null}
            {hasOpenedFamilySelectionDialog ? <FamilySelectionDialog
                shouldShowDialog={familySelection.show}
                closeDialog={closeFamilySelectionDialog}
                reloadSelectedDay={reloadSelectedDay}
                familyIds={familySelection.ids}
            /> : null}
            {hasOpenedAutonomousSystemSelectionDialog ? <AutonomousSystemSelectionDialog
                shouldShowDialog={autonomousSystemSelection.show}
                closeDialog={closeAutonomousSystemSelectionDialog}
                asNumbers={autonomousSystemSelection.ids}
                relayAutonomousSystemMap={relayAutonomousSystemMap}
                totalConsensusWeight={totalConsensusWeight}
                month={selectedDate?.slice(0, 7)}
            /> : null}
        </Suspense>
    );
};
