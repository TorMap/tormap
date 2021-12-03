import React, {useEffect, useMemo, useState} from "react";
import {
    Box,
    CircularProgress,
    DialogTitle,
    Divider, FormControl,
    Grid,
    IconButton, InputLabel, Link, MenuItem, Select,
    Typography,
    useMediaQuery,
    useTheme
} from "@mui/material";
import CloseIcon from "@material-ui/icons/Close";
import {getIcon} from "../../types/icons";
import {getRelayType} from "../../util/aggregate-relays";
import {DetailsInfo, RelayLocationDto, RelayDetailsDto, RelayIdentifierDto} from "../../types/responses";
import {FullHeightDialog, SnackbarMessage} from "../../types/ui";
import {RelayFlag, RelayFlagLabel, RelayType} from "../../types/relay";
import {backend, nameOfFactory} from "../../util/util";
import {useSnackbar} from "notistack";
import {RelayList} from "./RelayList";
import {RelayDetails} from "./RelayDetails";
import {SelectChangeEvent} from "@mui/material/Select/SelectInput";

interface Props {
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
    relays: RelayLocationDto[]
}

/**
 * Format number represented as bytes to rounded mega byte string representation
 * @param bandwidthInBytes - number to be formatted
 */
const formatBytesToMBPerSecond = (bandwidthInBytes?: number) => bandwidthInBytes ?
    (bandwidthInBytes / 1000000).toFixed(2) + " MB/s"
    : undefined

/**
 * Format number of seconds into an string representation in hours
 * @param seconds - number to be formatted
 */
const formatSecondsToHours = (seconds?: number) => seconds ?
    (seconds / 3600).toFixed(2) + " hours"
    : undefined

/**
 * Format a boolean value to a string representation
 * @param value - value to be formatted
 */
const formatBoolean = (value?: boolean) => value === null || value === undefined ? undefined : value ? "yes" : "no"

/**
 * The Dialog for Relay Details
 * @param showDialog - Whether the details dialog should be displayed
 * @param closeDialog - Event handler for closing the dialog
 * @param relays - Selectable relays
 * @param enqueueSnackbar - The event handler for showing a snackbar message
 */
export const RelayDetailsDialogLarge: React.FunctionComponent<Props> = ({
                                                                            showDialog,
                                                                            closeDialog,
                                                                            relays,
                                                                        }) => {
    const [relayIdentifiers, setRelayIdentifiers] = useState<RelayIdentifierDto[]>([])
    const [relayDetailsId, setRelayDetailsId] = useState<number>()
    const [rawRelayDetails, setRawRelayDetails] = useState<RelayDetailsDto>()
    const [relayDetails, setRelayDetails] = useState<DetailsInfo[]>()
    const [sortRelaysBy, setSortRelaysBy] = useState<keyof RelayMatch>("relayType")

    const {enqueueSnackbar} = useSnackbar();

    const relayDetailsIdToLocationMap = useMemo(() => {
        const relayDetailsIdToLocationMap = new Map<number, RelayLocationDto>()
        relays.filter(relay => relay.detailsId).forEach(relay => relayDetailsIdToLocationMap.set(relay.detailsId!!, relay))
        return relayDetailsIdToLocationMap
    }, [relays])

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
        () =>  relayMatches.sort((a, b) => a[sortRelaysBy] > b[sortRelaysBy] ? 1 : -1),
        [relayMatches, sortRelaysBy]
    )

    /**
     * Query relayIdentifiers for relays from backend
     */
    useEffect(() => {
        setRelayDetailsId(undefined)
        setRawRelayDetails(undefined)
        setRelayDetails(undefined)
        setRelayIdentifiers([])
        if (relays.length > 0 && relayDetailsIdToLocationMap.size === 0) {
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

    }, [closeDialog, relays, enqueueSnackbar, relayDetailsIdToLocationMap])

    /**
     * Query more information for the selected relay
     */
    useEffect(() => {
        function constructFlagString(flags: RelayFlag[] | null | undefined): string {
            if (flags) {
                return flags.map(flag => RelayFlagLabel[flag]).join(", ")
            }
            return "no flags assigned"
        }

        if (!relayDetailsId && sortedRelayMatches.length > 0) {
            setRelayDetailsId(sortedRelayMatches[0].id)
        } else if (relayDetailsId) {
            backend.get<RelayDetailsDto>(`/relay/details/relay/${relayDetailsId}`).then(response => {
                const relay = response.data
                setRawRelayDetails(relay)
                setRelayDetails([
                    {
                        name: "Fingerprint",
                        value:
                            <Link
                                href={`https://metrics.torproject.org/rs.html#details/${relay.fingerprint}`}
                                target={"_blank"}>
                                {relay.fingerprint}
                            </Link>
                    },
                    {
                        name: "IP address",
                        value:
                            <Link
                                href={`https://metrics.torproject.org/rs.html#search/${relay.address}`}
                                target={"_blank"}>
                                {relay.address}
                            </Link>
                    },
                    {
                        name: "Flags assigned by authorities",
                        value: constructFlagString(relayDetailsIdToLocationMap.get(relay.id)?.flags)
                    },
                    {name: "Autonomous System", value: relay.autonomousSystemName},
                    {
                        name: "Autonomous System Number",
                        value:
                            <Link
                                href={`https://metrics.torproject.org/rs.html#search/as:${relay.autonomousSystemNumber}`}
                                target={"_blank"}>
                                {relay.autonomousSystemNumber}
                            </Link>
                    },
                    {name: "Platform", value: relay.platform},
                    {name: "Uptime", value: formatSecondsToHours(relay.uptime)},
                    {name: "Contact", value: relay.contact},
                    {name: "Bandwidth for short intervals", value: formatBytesToMBPerSecond(relay.bandwidthBurst)},
                    {name: "Bandwidth for long periods", value: formatBytesToMBPerSecond(relay.bandwidthRate)},
                    {name: "Bandwidth observed", value: formatBytesToMBPerSecond(relay.bandwidthObserved)},
                    {name: "Supported protocols", value: relay.protocols},
                    {name: "Allows single hop exit", value: formatBoolean(relay.allowSingleHopExits)},
                    {name: "Is hibernating", value: formatBoolean(relay.isHibernating)},
                    {name: "Caches extra info", value: formatBoolean(relay.cachesExtraInfo)},
                    {name: "Is a hidden service directory", value: formatBoolean(relay.isHiddenServiceDir)},
                    {name: "Accepts tunneled directory requests", value: formatBoolean(relay.tunnelledDirServer)},
                    {name: "Link protocol versions", value: relay.linkProtocolVersions},
                    {name: "Circuit protocol versions", value: relay.circuitProtocolVersions},
                    {name: "Self reported family members", value: relay.familyEntries},
                    {name: "Infos published by relay on", value: relay.day},
                ])
            })
                .catch(() => {
                    enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                })
        }
    }, [sortedRelayMatches, relayDetailsId, relayDetailsIdToLocationMap, enqueueSnackbar])

    const theme = useTheme()
    const desktop = useMediaQuery(theme.breakpoints.up("lg"))
    const relay = relays.find((relay) => relayDetailsId && relay.detailsId === relayDetailsId)

    const handeSelectSortByChange = (event: SelectChangeEvent<keyof RelayMatch>) => {
        switch (event.target.value) {
            case "nickname": setSortRelaysBy("nickname")
                break
            default: setSortRelaysBy("relayType")
                break
        }
    }

    return (
        <FullHeightDialog
            open={showDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={relays.length > 1 ? "lg" : "md"}
            fullWidth={true}
            fullScreen={!desktop}
            sx={{
                paper: {
                    height: '70vh',
                },
            }}
        >
            <DialogTitle>
                <Grid container>
                    {relayIdentifiers.length > 1 && <Grid item xs={12} sm={3}>
                        <Typography sx={{display: "inline"}} variant="h6">
                            Relays
                        </Typography>
                        <FormControl variant="standard" sx={{marginLeft: "20px"}}>
                            <Select
                                value={sortRelaysBy}
                                label="Sort by"
                                onChange={handeSelectSortByChange}
                            >
                                <MenuItem value={"relayType"}>Type</MenuItem>
                                <MenuItem value={"nickname"}>Nickname</MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>}
                    <Grid item xs={12} sm={relayIdentifiers.length > 1 ? 9 : 12}>
                        {rawRelayDetails ?
                            <Box display="flex" alignItems={"center"}>
                                <Box sx={{
                                    display: "inline",
                                    paddingRight: "16px",
                                }}>
                                    {relay ? getIcon(getRelayType(relay)) : null}
                                </Box>
                                <Typography sx={{display: "inline"}} variant="h6">
                                    {rawRelayDetails.nickname}
                                </Typography>
                            </Box> : <CircularProgress color={"inherit"} size={24}/>
                        }
                        <IconButton aria-label="close" sx={{
                            position: "absolute",
                            right: "10px",
                            top: "10px",
                        }} onClick={closeDialog}>
                            <CloseIcon/>
                        </IconButton>
                    </Grid>
                </Grid>

            </DialogTitle>
            <Divider/>
            <Grid container>
                {relayIdentifiers.length > 1 && <Grid item xs={12} sm={3}
                                                      sx={{maxHeight: "65vh", overflow: 'auto'}}>
                    <RelayList
                        relayMatches={sortedRelayMatches}
                        relayDetailsId={relayDetailsId}
                        setRelayDetailsId={setRelayDetailsId}
                    />
                </Grid>}
                <Grid item xs={12} sm={relayIdentifiers.length > 1 ? 9 : 12}
                      sx={{maxHeight: "65vh", overflow: 'auto'}}>
                    <RelayDetails relayDetails={relayDetails}/>
                </Grid>
            </Grid>
        </FullHeightDialog>
    )
}

export interface RelayMatch extends RelayIdentifierDto {
    location: RelayLocationDto
    relayType: RelayType
}