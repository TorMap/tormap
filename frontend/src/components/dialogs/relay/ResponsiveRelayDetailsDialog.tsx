/**
 * Format number represented as bytes to rounded megabyte string representation
 * @param bandwidthInBytes - number to be formatted
 */
import {useMediaQuery, useTheme} from "@mui/material";
import {useSnackbar} from "notistack";
import {FunctionComponent, useEffect, useMemo, useState} from "react";

import {RelayDetailsDto, RelayIdentifierDto, RelayLocationDto} from "../../../dto/relay";
import {RelayDetailsMatch, RelayIdentifierMatch} from "../../../types/relay";
import {SnackbarMessage} from "../../../types/ui";
import {getRelayType} from "../../../util/aggregate-relays";
import {backend, nameOfFactory} from "../../../util/util";
import {RelayDetailsDialogLarge} from "./RelayDetailsDialogLarge";
import {RelayDetailsDialogSmall} from "./RelayDetailsDialogSmall";
import {atom, useAtom} from "jotai";
import {relayDetailsDialogOrderAtom, relayDetailsDialogSearchAtom} from "./RelayDetailsSelectionHeader";

export interface DetailsDialogProps {
    showDialog: boolean
    closeDialog: () => void
    relayDetailsMatch?: RelayDetailsMatch,
    filteredRelayMatches: RelayIdentifierMatch[]
    relayDetailsId?: number
    setRelayDetailsId: (id: number) => void
    canShowRelayList: boolean
}

export const showRelayDetailsDialogAtom = atom(false);
export const relaysForDetailsDialogAtom = atom<RelayLocationDto[]>([]);

export const ResponsiveRelayDetailsDialog: FunctionComponent = () => {
    // Component state
    const [relayIdentifiers, setRelayIdentifiers] = useState<RelayIdentifierDto[]>([])
    const [relayDetailsId, setRelayDetailsId] = useState<number>()
    const [relayDetailsMatch, setRelayDetailsMatch] = useState<RelayDetailsMatch>()

    // App context
    const {enqueueSnackbar} = useSnackbar();
    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

    // Atom state
    const [showRelayDetailsDialog, setShowRelayDetailsDialog] = useAtom(showRelayDetailsDialogAtom)
    const [relaysForDetailsDialog] = useAtom(relaysForDetailsDialogAtom)
    const [searchRelaysBy] = useAtom(relayDetailsDialogSearchAtom)
    const [orderRelaysBy] = useAtom(relayDetailsDialogOrderAtom)

    const nameOfRelayMatch = useMemo(() => nameOfFactory<RelayIdentifierMatch>(), [])

    const relayDetailsIdToLocationMap = useMemo(() => {
        const relayDetailsIdToLocationMap = new Map<number, RelayLocationDto>()
        relaysForDetailsDialog.forEach(relay => {
            if (relay.detailsId) {
                relayDetailsIdToLocationMap.set(relay.detailsId, relay)
            }
        })
        return relayDetailsIdToLocationMap
    }, [relaysForDetailsDialog])

    const relayIdentifierMatches = useMemo(
        () => {
            const matches: RelayIdentifierMatch[] = [];
            if (relayDetailsIdToLocationMap.size > 1) {
                relayIdentifiers.forEach(identifier => {
                    const relayLocation = relayDetailsIdToLocationMap.get(identifier.id)
                    if (relayLocation) {
                        matches.push({
                            ...identifier,
                            ...relayLocation,
                            relayType: getRelayType(relayLocation)
                        })
                    }
                })
            }
            return matches
        },
        [relayIdentifiers, relayDetailsIdToLocationMap]
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
            setRelayDetailsId(searchedRelayIdentifierMatches[0].id)
        }
    }

    // Discard the previous relays as soon as the relays of another location are passed to the dialog
    const [syncedRelayDetailsIdToLocationMap, setSyncedRelayDetailsIdToLocationMap] = useState(relayDetailsIdToLocationMap)
    if (syncedRelayDetailsIdToLocationMap !== relayDetailsIdToLocationMap) {
        setSyncedRelayDetailsIdToLocationMap(relayDetailsIdToLocationMap)
        setRelayDetailsMatch(undefined)
        setRelayIdentifiers([])
        if (relayDetailsIdToLocationMap.size === 1) {
            setRelayDetailsId(relayDetailsIdToLocationMap.keys().next().value)
        } else if (relayDetailsIdToLocationMap.size > 1) {
            setRelayDetailsId(undefined)
        }
    }

    // Until the user picks a relay, the first one of the list is selected
    const selectedRelayDetailsId = relayDetailsId || orderedRelayIdentifierMatches[0]?.id

    /**
     * Query relayIdentifiers for relays from backend
     */
    useEffect(() => {
        if (relaysForDetailsDialog.length > 0 && relayDetailsIdToLocationMap.size === 0) {
            enqueueSnackbar(SnackbarMessage.NoRelayDetails, {variant: "warning"})
            setShowRelayDetailsDialog(false)
        } else if (relayDetailsIdToLocationMap.size > 1) {
            backend.post<RelayIdentifierDto[]>(
                '/relay/details/relay/identifiers',
                Array.from(relayDetailsIdToLocationMap.keys())
            ).then(response => {
                setRelayIdentifiers(response.data)
            }).catch(() => {
                enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                setShowRelayDetailsDialog(false)
            })
        }
    }, [enqueueSnackbar, relayDetailsIdToLocationMap, relaysForDetailsDialog, setShowRelayDetailsDialog])

    /**
     * Query more information for the selected relay
     */
    useEffect(() => {
        if (selectedRelayDetailsId) {
            backend.get<RelayDetailsDto>(`/relay/details/relay/${selectedRelayDetailsId}`).then(response => {
                const relayLocation = relayDetailsIdToLocationMap.get(selectedRelayDetailsId)
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
    }, [selectedRelayDetailsId, relayDetailsIdToLocationMap, enqueueSnackbar])

    return (isLargeScreen ?
            <RelayDetailsDialogLarge
                relayDetailsMatch={relayDetailsMatch}
                showDialog={showRelayDetailsDialog}
                closeDialog={() => setShowRelayDetailsDialog(false)}
                setRelayDetailsId={setRelayDetailsId}
                filteredRelayMatches={searchedRelayIdentifierMatches}
                relayDetailsId={selectedRelayDetailsId}
                canShowRelayList={relayIdentifierMatches.length > 1}
            />
            : <RelayDetailsDialogSmall
                relayDetailsMatch={relayDetailsMatch}
                showDialog={showRelayDetailsDialog}
                closeDialog={() => setShowRelayDetailsDialog(false)}
                setRelayDetailsId={setRelayDetailsId}
                filteredRelayMatches={searchedRelayIdentifierMatches}
                relayDetailsId={selectedRelayDetailsId}
                canShowRelayList={relayIdentifierMatches.length > 1}
            />
    )
}

export default ResponsiveRelayDetailsDialog
