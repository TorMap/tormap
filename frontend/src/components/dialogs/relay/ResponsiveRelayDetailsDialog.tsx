/**
 * Format number represented as bytes to rounded megabyte string representation
 * @param bandwidthInBytes - number to be formatted
 */
import {useMediaQuery, useTheme} from "@mui/material";
import {FunctionComponent, useEffect, useMemo, useState} from "react";

import {useSnackbar} from "../../../context/snackbar-context";
import {useDate} from "../../../context/date-context";
import {RelayDetailsDto, RelayLocationDto} from "../../../dto/relay";
import {RelayDetailsMatch, RelayIdentifierMatch} from "../../../types/relay";
import {SnackbarMessage} from "../../../types/ui";
import {getRelayType} from "../../../util/aggregate-relays";
import {monthOfDay, relayDetailsPath} from "../../../util/api-v1";
import {backend, nameOfFactory} from "../../../util/util";
import {RelayDetailsDialogLarge} from "./RelayDetailsDialogLarge";
import {RelayDetailsDialogSmall} from "./RelayDetailsDialogSmall";
import {useAtom, useAtomValue, useSetAtom} from "jotai";
import {
    dayRelaysAtom,
    followRelayAtom,
    relayDetailsDialogOrderAtom,
    relayDetailsDialogSearchAtom,
    relaysForDetailsDialogAtom,
    relaySearchOnMapAtom,
    showRelayDetailsDialogAtom,
} from "./relay-details-atoms";

export interface DetailsDialogProps {
    showDialog: boolean
    closeDialog: () => void
    relayDetailsMatch?: RelayDetailsMatch,
    filteredRelayMatches: RelayIdentifierMatch[]
    /**
     * The fingerprint of the selected relay
     */
    relayDetailsId?: string
    setRelayDetailsId: (fingerprint: string) => void
    canShowRelayList: boolean
    showMatchesOnMap?: () => void
}

export const ResponsiveRelayDetailsDialog: FunctionComponent = () => {
    // Component state
    const [relayDetailsId, setRelayDetailsId] = useState<string>()
    const [relayDetailsMatch, setRelayDetailsMatch] = useState<RelayDetailsMatch>()

    // App context
    const {enqueueSnackbar} = useSnackbar();
    const {selectedDate} = useDate()
    const month = selectedDate ? monthOfDay(selectedDate) : undefined
    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

    // Atom state
    const [showRelayDetailsDialog, setShowRelayDetailsDialog] = useAtom(showRelayDetailsDialogAtom)
    const [relaysForDetailsDialog, setRelaysForDetailsDialog] = useAtom(relaysForDetailsDialogAtom)
    const [followRelay, setFollowRelay] = useAtom(followRelayAtom)
    const dayRelays = useAtomValue(dayRelaysAtom)
    const [searchRelaysBy] = useAtom(relayDetailsDialogSearchAtom)
    const [orderRelaysBy] = useAtom(relayDetailsDialogOrderAtom)
    const setRelaySearchOnMap = useSetAtom(relaySearchOnMapAtom)

    const nameOfRelayMatch = useMemo(() => nameOfFactory<RelayIdentifierMatch>(), [])

    // Relays by fingerprint. Relays without details in the month can not be shown, so they are left out.
    const relayFingerprintToLocationMap = useMemo(() => {
        const relayFingerprintToLocationMap = new Map<string, RelayLocationDto>()
        relaysForDetailsDialog.forEach(relay => {
            if (relay.hasDetails) {
                relayFingerprintToLocationMap.set(relay.fingerprint, relay)
            }
        })
        return relayFingerprintToLocationMap
    }, [relaysForDetailsDialog])

    // The relays of the day carry fingerprint and nickname, so the list needs no request
    const relayIdentifierMatches = useMemo(
        (): RelayIdentifierMatch[] => relayFingerprintToLocationMap.size > 1 ?
            [...relayFingerprintToLocationMap.values()].map(relayLocation => ({
                ...relayLocation,
                relayType: getRelayType(relayLocation),
            })) : [],
        [relayFingerprintToLocationMap]
    )

    const orderedRelayIdentifierMatches = useMemo(
        () => [...relayIdentifierMatches].sort((a, b) => {
                if (orderRelaysBy === nameOfRelayMatch("familyId")) {
                    return a[orderRelaysBy]! < b[orderRelaysBy]! ? 1 : -1
                }
                return a[orderRelaysBy]! > b[orderRelaysBy]! ? 1 : -1
            }
        ),
        [nameOfRelayMatch, relayIdentifierMatches, orderRelaysBy]
    )

    const searchedRelayIdentifierMatches = useMemo(
        () => {
            const searchLowerCase = searchRelaysBy.toLowerCase()
            return orderedRelayIdentifierMatches.filter(relay =>
                relay.nickname.toLowerCase().includes(searchLowerCase) ||
                relay.fingerprint.toLowerCase() == searchLowerCase
            )
        },
        [orderedRelayIdentifierMatches, searchRelaysBy]
    )

    // Select the relay if the search narrowed the list down to a single one
    const [syncedSearchedMatches, setSyncedSearchedMatches] = useState(searchedRelayIdentifierMatches)
    if (syncedSearchedMatches !== searchedRelayIdentifierMatches) {
        setSyncedSearchedMatches(searchedRelayIdentifierMatches)
        if (searchedRelayIdentifierMatches.length === 1) {
            setRelayDetailsId(searchedRelayIdentifierMatches[0].fingerprint)
        }
    }

    // Discard the previous relays as soon as the relays of another location are passed to the dialog.
    // Starts without a value, because the dialog is only mounted when it is opened for the first time, so the relays of
    // that first location have to count as a change too.
    const [syncedRelayFingerprintToLocationMap, setSyncedRelayFingerprintToLocationMap] =
        useState<Map<string, RelayLocationDto>>()
    if (syncedRelayFingerprintToLocationMap !== relayFingerprintToLocationMap) {
        setSyncedRelayFingerprintToLocationMap(relayFingerprintToLocationMap)
        setRelayDetailsMatch(undefined)
        if (relayFingerprintToLocationMap.size === 1) {
            setRelayDetailsId(relayFingerprintToLocationMap.keys().next().value)
        } else if (relayFingerprintToLocationMap.size > 1) {
            setRelayDetailsId(undefined)
        }
    }

    // After a jump to another month, show the relay as it is on the new day, once that day is loaded. If it is not on
    // the map that day, the details of the new month are still shown with the relay's last known location.
    useEffect(() => {
        if (!followRelay || dayRelays?.day !== followRelay.day) return
        setFollowRelay(undefined)
        const relay = dayRelays.relays.find(relay => relay.fingerprint === followRelay.fingerprint && relay.hasDetails)
        if (relay) setRelaysForDetailsDialog([relay])
    }, [followRelay, dayRelays, setFollowRelay, setRelaysForDetailsDialog])

    // Offer to show the search results on the map once the user searched for something
    const showMatchesOnMap = searchRelaysBy.trim() && searchedRelayIdentifierMatches.length > 0 ?
        () => {
            setRelaySearchOnMap({search: searchRelaysBy.trim(), relays: searchedRelayIdentifierMatches})
            setShowRelayDetailsDialog(false)
        } : undefined

    // Relays without details can not be shown. The dialog must not be opened for them at all: it is mounted already
    // open the first time, and closing it again before its opening transition finished leaves a modal with an
    // invisible backdrop on the page, which blocks all other UI elements.
    const hasNoDetails = relaysForDetailsDialog.length > 0 && relayFingerprintToLocationMap.size === 0
    const showDialog = showRelayDetailsDialog && !hasNoDetails

    // Until the user picks a relay, the first one of the list is selected
    const selectedFingerprint = relayDetailsId || orderedRelayIdentifierMatches[0]?.fingerprint

    useEffect(() => {
        if (hasNoDetails) {
            enqueueSnackbar(SnackbarMessage.NoRelayDetails, {variant: "warning"})
            setShowRelayDetailsDialog(false)
        }
    }, [enqueueSnackbar, hasNoDetails, setShowRelayDetailsDialog])

    /**
     * Query more information for the selected relay, in the month of the selected day
     */
    useEffect(() => {
        if (selectedFingerprint && month) {
            backend.get<RelayDetailsDto>(relayDetailsPath(selectedFingerprint, month)).then(response => {
                const relayLocation = relayFingerprintToLocationMap.get(selectedFingerprint)
                if (relayLocation) {
                    setRelayDetailsMatch({
                        ...response.data,
                        ...relayLocation,
                        relayType: getRelayType(relayLocation)
                    })
                }
            }).catch(() => {
                enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
            })
        }
    }, [selectedFingerprint, month, relayFingerprintToLocationMap, enqueueSnackbar])

    return (isLargeScreen ?
            <RelayDetailsDialogLarge
                relayDetailsMatch={relayDetailsMatch}
                showDialog={showDialog}
                closeDialog={() => setShowRelayDetailsDialog(false)}
                setRelayDetailsId={setRelayDetailsId}
                filteredRelayMatches={searchedRelayIdentifierMatches}
                relayDetailsId={selectedFingerprint}
                canShowRelayList={relayIdentifierMatches.length > 1}
                showMatchesOnMap={showMatchesOnMap}
            />
            : <RelayDetailsDialogSmall
                relayDetailsMatch={relayDetailsMatch}
                showDialog={showDialog}
                closeDialog={() => setShowRelayDetailsDialog(false)}
                setRelayDetailsId={setRelayDetailsId}
                filteredRelayMatches={searchedRelayIdentifierMatches}
                relayDetailsId={selectedFingerprint}
                canShowRelayList={relayIdentifierMatches.length > 1}
                showMatchesOnMap={showMatchesOnMap}
            />
    )
}

export default ResponsiveRelayDetailsDialog
