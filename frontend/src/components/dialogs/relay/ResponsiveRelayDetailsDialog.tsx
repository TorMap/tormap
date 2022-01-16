/**
 * Format number represented as bytes to rounded mega byte string representation
 * @param bandwidthInBytes - number to be formatted
 */
import React, {FunctionComponent, useEffect, useMemo, useState} from "react";
import {RelayDetailsDialogLarge, RelayMatch} from "./RelayDetailsDialogLarge";
import {RelayDetailsDto, RelayIdentifierDto, RelayLocationDto} from "../../../dto/relay";
import {useMediaQuery, useTheme} from "@mui/material";
import {RelayDetailsDialogSmall} from "./RelayDetailsDialogSmall";
import {useSnackbar} from "notistack";
import {getRelayType} from "../../../util/aggregate-relays";
import {SnackbarMessage} from "../../../types/ui";
import {backend} from "../../../util/util";
import {SelectChangeEvent} from "@mui/material/Select/SelectInput";

export interface DetailsProps {
    /**
     * Whether the modal should currently be visible
     */
    showDialog: boolean

    /**
     * Hide the modal
     */
    closeDialog: () => void

    /**
     * Relays which the user can view detailed information about
     */
    relayLocations: RelayLocationDto[]
}

export interface DetailsDialogProps extends DetailsProps {
    relayIdentifiers: RelayIdentifierDto[]
    sortRelaysBy: keyof RelayMatch
    handleSelectSortByChange: (event: SelectChangeEvent<keyof RelayMatch>) => void
    setRelayDetailsId: (id: number) => void
    sortedRelayMatches: RelayMatch[]
    relayDetailsId?: number
    relayDetails?: RelayDetailsDto
    relayLocation?: RelayLocationDto
}

export const ResponsiveRelayDetailsDialog: FunctionComponent<DetailsProps> = ({
                                                                                  showDialog,
                                                                                  closeDialog,
                                                                                  relayLocations,
                                                                              }) => {
    // Component state
    const [relayIdentifiers, setRelayIdentifiers] = useState<RelayIdentifierDto[]>([])
    const [relayDetailsId, setRelayDetailsId] = useState<number>()
    const [relayDetails, setRelayDetails] = useState<RelayDetailsDto>()
    const [sortRelaysBy, setSortRelaysBy] = useState<keyof RelayMatch>("relayType")

    // App context
    const {enqueueSnackbar} = useSnackbar();
    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

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
                        location: relayLocation,
                        relayType: getRelayType(relayLocation)
                    })
                }
            })
            return relayMatches.sort((a, b) => a.relayType > b.relayType ? 1 : -1)
        },
        [relayIdentifiers, relayDetailsIdToLocationMap]
    )

    const sortedRelayMatches = useMemo(
        () => relayMatches.sort((a, b) => a[sortRelaysBy] > b[sortRelaysBy] ? 1 : -1),
        [relayMatches, sortRelaysBy]
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
        if (!relayDetailsId && sortedRelayMatches.length > 0) {
            setRelayDetailsId(sortedRelayMatches[0].id)
        } else if (relayDetailsId) {
            backend.get<RelayDetailsDto>(`/relay/details/relay/${relayDetailsId}`).then(response => {
                setRelayDetails(response.data)
            })
                .catch(() => {
                    enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                })
        }
    }, [sortedRelayMatches, relayDetailsId, relayDetailsIdToLocationMap, enqueueSnackbar])

    const relayLocation = relayLocations.find((relay) => relayDetailsId && relay.detailsId === relayDetailsId)
    const handeSelectSortByChange = (event: SelectChangeEvent<keyof RelayMatch>) => {
        switch (event.target.value) {
            case "nickname":
                setSortRelaysBy("nickname")
                break
            default:
                setSortRelaysBy("relayType")
                break
        }
    }

    return (isLargeScreen ?
            <RelayDetailsDialogLarge
                showDialog={showDialog}
                closeDialog={closeDialog}
                relayLocations={relayLocations}
                relayIdentifiers={relayIdentifiers}
                sortRelaysBy={sortRelaysBy}
                handleSelectSortByChange={handeSelectSortByChange}
                relayDetails={relayDetails}
                setRelayDetailsId={setRelayDetailsId}
                sortedRelayMatches={sortedRelayMatches}
                relayDetailsId={relayDetailsId}
                relayLocation={relayLocation}
            />
            : <RelayDetailsDialogSmall
                showDialog={showDialog}
                closeDialog={closeDialog}
                relayLocations={relayLocations}
                relayIdentifiers={relayIdentifiers}
                sortRelaysBy={sortRelaysBy}
                handleSelectSortByChange={handeSelectSortByChange}
                relayDetails={relayDetails}
                setRelayDetailsId={setRelayDetailsId}
                sortedRelayMatches={sortedRelayMatches}
                relayDetailsId={relayDetailsId}
                relayLocation={relayLocation}
            />
    )
}