/**
 * Format number represented as bytes to rounded mega byte string representation
 * @param bandwidthInBytes - number to be formatted
 */
import React, {FunctionComponent, useEffect, useMemo, useState} from "react";
import {RelayDetailsDialogLarge} from "./RelayDetailsDialogLarge";
import {RelayDetailsDto, RelayIdentifierDto, RelayLocationDto} from "../../../dto/relay";
import {useMediaQuery, useTheme} from "@mui/material";
import {RelayDetailsDialogSmall} from "./RelayDetailsDialogSmall";
import {useSnackbar} from "notistack";
import {getRelayType} from "../../../util/aggregate-relays";
import {SnackbarMessage} from "../../../types/ui";
import {backend, nameOfFactory} from "../../../util/util";
import {SelectChangeEvent} from "@mui/material/Select/SelectInput";
import {RelayDetailsMatch, RelayMatch} from "../../../types/relay";

export interface Props {
    shouldShowDialog: boolean
    closeDialog: () => void
    relayLocations: RelayLocationDto[]
}

export interface DetailsDialogProps {
    shouldShowDialog: boolean
    closeDialog: () => void
    relayDetailsMatch?: RelayDetailsMatch,
    sortedRelayMatches: RelayMatch[]
    sortRelaysBy: keyof RelayMatch
    handleSelectSortByChange: (event: SelectChangeEvent<keyof RelayMatch>) => void
    relayDetailsId?: number
    setRelayDetailsId: (id: number) => void
}

export const ResponsiveRelayDetailsDialog: FunctionComponent<Props> = ({
                                                                                  shouldShowDialog,
                                                                                  closeDialog,
                                                                                  relayLocations,
                                                                              }) => {
    // Component state
    const [relayIdentifiers, setRelayIdentifiers] = useState<RelayIdentifierDto[]>([])
    const [selectedRelayDetailsId, setRelayDetailsId] = useState<number>()
    const [selectedRelayDetails, setRelayDetails] = useState<RelayDetailsDto>()
    const [sortRelaysBy, setSortRelaysBy] = useState<keyof RelayMatch>("nickname")

    // App context
    const {enqueueSnackbar} = useSnackbar();
    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

    const nameOfRelayMatch = nameOfFactory<RelayMatch>()

    const relayDetailsIdToLocationMap = useMemo(() => {
        const relayDetailsIdToLocationMap = new Map<number, RelayLocationDto>()
        relayLocations.filter(relay => relay.detailsId).forEach(relay => relayDetailsIdToLocationMap.set(relay.detailsId!!, relay))
        return relayDetailsIdToLocationMap
    }, [relayLocations])

    const relayMatches = useMemo(
        () => {
            const relayMatches: RelayMatch[] = [];
            relayIdentifiers.forEach(identifier => {
                const relayLocation = relayDetailsIdToLocationMap.get(identifier.id)
                if (relayLocation) {
                    relayMatches.push({
                        ...identifier,
                        ...relayLocation,
                        relayType: getRelayType(relayLocation)
                    })
                }
            })
            return relayMatches
        },
        [relayIdentifiers, relayDetailsIdToLocationMap]
    )

    const sortedRelayMatches = useMemo(
        () => relayMatches.sort((a, b) => {
                if (sortRelaysBy === nameOfRelayMatch("familyId")) {
                    return a[sortRelaysBy]!! < b[sortRelaysBy]!! ? 1 : -1
                }
                return a[sortRelaysBy]!! > b[sortRelaysBy]!! ? 1 : -1
            }
        ),
        [nameOfRelayMatch, relayMatches, sortRelaysBy]
    )

    const selectedRelayLocation = useMemo<RelayLocationDto | undefined>(
        () => relayLocations.find((relay) => selectedRelayDetailsId && relay.detailsId === selectedRelayDetailsId),
        [selectedRelayDetailsId, relayLocations]
    )

    const relayDetailsMatch = useMemo<RelayDetailsMatch | undefined>(
        () => {
            if (selectedRelayDetails && selectedRelayLocation && selectedRelayDetails.id === selectedRelayDetailsId) {
                return {
                    ...selectedRelayDetails,
                    ...selectedRelayLocation,
                    relayType: getRelayType(selectedRelayLocation)
                }
            }
            return undefined
        },
        [selectedRelayDetails, selectedRelayDetailsId, selectedRelayLocation]
    )

    /**
     * Query relayIdentifiers for relays from backend
     */
    useEffect(() => {
        setRelayDetailsId(undefined)
        setRelayDetails(undefined)
        setRelayIdentifiers([])
        if (relayLocations.length > 0 && relayDetailsIdToLocationMap.size === 0) {
            enqueueSnackbar(SnackbarMessage.NoRelayDetails, {variant: "warning"})
            closeDialog()
        } else if (relayDetailsIdToLocationMap.size === 1) {
            setRelayDetailsId(relayDetailsIdToLocationMap.keys().next().value)
        } else if (relayDetailsIdToLocationMap.size > 1) {
            backend.post<RelayIdentifierDto[]>(
                '/relay/details/relay/identifiers',
                Array.from(relayDetailsIdToLocationMap.keys())
            ).then(response => {
                const requestedRelayIdentifiers = response.data
                setRelayIdentifiers(requestedRelayIdentifiers)
            }).catch(() => {
                enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                closeDialog()
            })
        }

    }, [closeDialog, relayLocations, enqueueSnackbar, relayDetailsIdToLocationMap])

    /**
     * Query more information for the selected relay
     */
    useEffect(() => {
        if (!selectedRelayDetailsId && sortedRelayMatches.length > 0) {
            setRelayDetailsId(sortedRelayMatches[0].id)
        } else if (selectedRelayDetailsId) {
            backend.get<RelayDetailsDto>(`/relay/details/relay/${selectedRelayDetailsId}`).then(response => {
                setRelayDetails(response.data)
            })
                .catch(() => {
                    enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                })
        }
    }, [sortedRelayMatches, selectedRelayDetailsId, relayDetailsIdToLocationMap, enqueueSnackbar])

    const handeSelectSortByChange = (event: SelectChangeEvent<keyof RelayMatch>) => {
        switch (event.target.value) {
            case nameOfRelayMatch("relayType"):
                setSortRelaysBy(nameOfRelayMatch("relayType"))
                break
            case nameOfRelayMatch("familyId"):
                setSortRelaysBy(nameOfRelayMatch("familyId"))
                break
            default:
                setSortRelaysBy(nameOfRelayMatch("nickname"))
                break
        }
    }

    return (isLargeScreen ?
            <RelayDetailsDialogLarge
                relayDetailsMatch={relayDetailsMatch}
                shouldShowDialog={shouldShowDialog}
                closeDialog={closeDialog}
                sortRelaysBy={sortRelaysBy}
                handleSelectSortByChange={handeSelectSortByChange}
                setRelayDetailsId={setRelayDetailsId}
                sortedRelayMatches={sortedRelayMatches}
                relayDetailsId={selectedRelayDetailsId}
            />
            : <RelayDetailsDialogSmall
                relayDetailsMatch={relayDetailsMatch}
                shouldShowDialog={shouldShowDialog}
                closeDialog={closeDialog}
                sortRelaysBy={sortRelaysBy}
                handleSelectSortByChange={handeSelectSortByChange}
                setRelayDetailsId={setRelayDetailsId}
                sortedRelayMatches={sortedRelayMatches}
                relayDetailsId={selectedRelayDetailsId}
            />
    )
}