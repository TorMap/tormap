/**
 * Format number represented as bytes to rounded mega byte string representation
 * @param bandwidthInBytes - number to be formatted
 */
import React, {FunctionComponent, useCallback, useEffect, useMemo, useState} from "react";
import {RelayDetailsDialogLarge} from "./RelayDetailsDialogLarge";
import {RelayDetailsDto, RelayIdentifierDto, RelayLocationDto} from "../../../dto/relay";
import {useMediaQuery, useTheme} from "@mui/material";
import {RelayDetailsDialogSmall} from "./RelayDetailsDialogSmall";
import {useSnackbar} from "notistack";
import {getRelayType} from "../../../util/aggregate-relays";
import {SnackbarMessage} from "../../../types/ui";
import {backend, nameOfFactory} from "../../../util/util";
import {SelectChangeEvent} from "@mui/material/Select/SelectInput";
import {RelayDetailsMatch, RelayIdentifierMatch} from "../../../types/relay";

export interface Props {
    shouldShowDialog: boolean
    closeDialog: () => void
    relayLocations: RelayLocationDto[]
}

export interface DetailsDialogProps {
    shouldShowDialog: boolean
    closeDialog: () => void
    relayDetailsMatch?: RelayDetailsMatch,
    sortedRelayMatches: RelayIdentifierMatch[]
    sortRelaysBy: keyof RelayIdentifierMatch
    handleSelectSortByChange: (event: SelectChangeEvent<keyof RelayIdentifierMatch>) => void
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
    const [relayDetailsId, setRelayDetailsId] = useState<number>()
    const [relayDetailsMatch, setRelayDetailsMatch] = useState<RelayDetailsMatch>()
    const [sortRelaysBy, setSortRelaysBy] = useState<keyof RelayIdentifierMatch>("nickname")

    // App context
    const {enqueueSnackbar} = useSnackbar();
    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

    const nameOfRelayMatch = useMemo(() => nameOfFactory<RelayIdentifierMatch>(), [])

    const relayDetailsIdToLocationMap = useMemo(() => {
        const relayDetailsIdToLocationMap = new Map<number, RelayLocationDto>()
        relayLocations.filter(relay => relay.detailsId).forEach(relay => relayDetailsIdToLocationMap.set(relay.detailsId!!, relay))
        return relayDetailsIdToLocationMap
    }, [relayLocations])

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

    const sortedRelayIdentifierMatches = useMemo(
        () => relayIdentifierMatches.sort((a, b) => {
                if (sortRelaysBy === nameOfRelayMatch("familyId")) {
                    return a[sortRelaysBy]!! < b[sortRelaysBy]!! ? 1 : -1
                }
                return a[sortRelaysBy]!! > b[sortRelaysBy]!! ? 1 : -1
            }
        ),
        [nameOfRelayMatch, relayIdentifierMatches, sortRelaysBy]
    )

    /**
     * Query relayIdentifiers for relays from backend
     */
    useEffect(() => {
        setRelayDetailsMatch(undefined)
        setRelayIdentifiers([])
        if (relayLocations.length > 0 && relayDetailsIdToLocationMap.size === 0) {
            enqueueSnackbar(SnackbarMessage.NoRelayDetails, {variant: "warning"})
            closeDialog()
        } else if (relayDetailsIdToLocationMap.size === 1) {
            setRelayDetailsId(relayDetailsIdToLocationMap.keys().next().value)
        } else if (relayDetailsIdToLocationMap.size > 1) {
            setRelayDetailsId(undefined)
            backend.post<RelayIdentifierDto[]>(
                '/relay/details/relay/identifiers',
                Array.from(relayDetailsIdToLocationMap.keys())
            ).then(response => {
                setRelayIdentifiers(response.data)
            }).catch(() => {
                enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                closeDialog()
            })
        }

    }, [closeDialog, enqueueSnackbar, relayDetailsIdToLocationMap, relayLocations])

    /**
     * Query more information for the selected relay
     */
    useEffect(() => {
        if (!relayDetailsId && sortedRelayIdentifierMatches.length > 0) {
            setRelayDetailsId(sortedRelayIdentifierMatches[0].id)
        } else if (relayDetailsId) {
            backend.get<RelayDetailsDto>(`/relay/details/relay/${relayDetailsId}`).then(response => {
                const relayLocation = relayDetailsIdToLocationMap.get(relayDetailsId)
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
    }, [sortedRelayIdentifierMatches, relayDetailsId, relayDetailsIdToLocationMap, enqueueSnackbar])

    const handeSelectSortByChange = useCallback((event: SelectChangeEvent<keyof RelayIdentifierMatch>) => {
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
    }, [nameOfRelayMatch])

    return (isLargeScreen ?
            <RelayDetailsDialogLarge
                relayDetailsMatch={relayDetailsMatch}
                shouldShowDialog={shouldShowDialog}
                closeDialog={closeDialog}
                sortRelaysBy={sortRelaysBy}
                handleSelectSortByChange={handeSelectSortByChange}
                setRelayDetailsId={setRelayDetailsId}
                sortedRelayMatches={sortedRelayIdentifierMatches}
                relayDetailsId={relayDetailsId}
            />
            : <RelayDetailsDialogSmall
                relayDetailsMatch={relayDetailsMatch}
                shouldShowDialog={shouldShowDialog}
                closeDialog={closeDialog}
                sortRelaysBy={sortRelaysBy}
                handleSelectSortByChange={handeSelectSortByChange}
                setRelayDetailsId={setRelayDetailsId}
                sortedRelayMatches={sortedRelayIdentifierMatches}
                relayDetailsId={relayDetailsId}
            />
    )
}

export default ResponsiveRelayDetailsDialog