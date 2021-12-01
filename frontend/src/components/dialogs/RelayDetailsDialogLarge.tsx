import React, {useEffect, useState} from "react";
import {
    Box,
    CircularProgress,
    DialogContent,
    DialogTitle,
    Divider,
    Grid,
    IconButton,
    Typography,
    useMediaQuery,
    useTheme
} from "@mui/material";
import CloseIcon from "@material-ui/icons/Close";
import {getIcon} from "../../types/icons";
import {findGeoRelayViewByID, getRelayType} from "../../util/aggregate-relays";
import {DetailsInfo, RelayLocationDto, RelayDetailsDto, RelayIdentifierDto} from "../../types/responses";
import {FullHeightDialog, SnackbarMessage} from "../../types/ui";
import {RelayFlag, RelayFlagLabel} from "../../types/relay";
import {backend} from "../../util/util";
import {useSnackbar} from "notistack";
import {RelayList} from "./RelayList";
import {RelayDetails} from "./RelayDetails";

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

    const { enqueueSnackbar } = useSnackbar();

    /**
     * Query relayIdentifiers for relays from backend
     */
    useEffect(() => {
        setRelayDetailsId(undefined)
        setRawRelayDetails(undefined)
        setRelayDetails(undefined)
        setRelayIdentifiers([])
        const relayDetailsIds = relays.filter(relay => relay.detailsId).map(relay => relay.detailsId)
        if (relays.length > 0 && relayDetailsIds.length === 0) {
            enqueueSnackbar(SnackbarMessage.NoRelayDetails, {variant: "warning"})
            closeDialog()
        } else if (relayDetailsIds.length === 1) {
            setRelayDetailsId(relayDetailsIds[0]!!)
        } else if (relayDetailsIds.length > 1) {
            backend.post<RelayIdentifierDto[]>('/relay/details/relay/identifiers', relayDetailsIds).then(response => {
                const requestedRelayIdentifiers = response.data
                setRelayIdentifiers(requestedRelayIdentifiers)
                if (requestedRelayIdentifiers.length > 0) {
                    setRelayDetailsId(requestedRelayIdentifiers[0].id)
                }
            }).catch(() => {
                enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                closeDialog()
            })
        }

    }, [closeDialog, relays, enqueueSnackbar])

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

        setRawRelayDetails(undefined)
        setRelayDetails(undefined)
        if (relayDetailsId) {
            backend.get<RelayDetailsDto>(`/relay/details/relay/${relayDetailsId}`).then(response => {
                const relay = response.data
                setRawRelayDetails(relay)
                setRelayDetails([
                    {name: "Fingerprint", value: relay.fingerprint},
                    {name: "IP address", value: relay.address},
                    {
                        name: "Flags assigned by authorities",
                        value: constructFlagString(findGeoRelayViewByID(relay.id, relays)?.flags)
                    },
                    {name: "Autonomous System", value: relay.autonomousSystemName},
                    {name: "Autonomous System Number", value: relay.autonomousSystemNumber},
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
                    {name: "Family members", value: relay.familyEntries},
                    {name: "Infos published by relay on", value: relay.day},
                ])
            })
                .catch(() => {
                    enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                })
        }
    }, [relayDetailsId, relays, enqueueSnackbar])

    const theme = useTheme()
    const desktop = useMediaQuery(theme.breakpoints.up("lg"))

    return (
        <FullHeightDialog
            open={showDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={relays.length > 1 ? "lg" : "md"}
            fullWidth={true}
            fullScreen={!desktop}
        >
            <DialogTitle>
                {rawRelayDetails ?
                    <Box display="flex" alignItems={"center"}>
                        <Box sx={{
                            display: "inline",
                            paddingRight: "16px",
                        }}>
                            {relayDetailsId ? getIcon(getRelayType(relays.find((relay) => relay.detailsId === relayDetailsId))) : null}
                        </Box>
                        <Typography
                            sx={{display: "inline"}}
                            variant="h6">
                            {relayIdentifiers.find((identifier) => {return identifier.id === relayDetailsId})?.nickname
                            || rawRelayDetails.nickname}
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
            </DialogTitle>
            <Divider/>
            <DialogContent>
                <Grid container>
                    <Grid item xs={12} sm={relayIdentifiers.length > 1 ? 3 : 0}>
                        <RelayList
                            relays={relays}
                            relayIdentifiers={relayIdentifiers}
                            relayDetailsId={relayDetailsId}
                            setRelayDetailsId={setRelayDetailsId}
                        />
                    </Grid>
                    <Grid item xs={12} sm={relayIdentifiers.length > 1 ? 9 : 12}>
                        <RelayDetails relayDetails={relayDetails}/>
                    </Grid>
                </Grid>
            </DialogContent>
        </FullHeightDialog>
    )
}
