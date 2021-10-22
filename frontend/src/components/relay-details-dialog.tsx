import React, {useEffect, useState} from "react";
import {
    Box,
    CircularProgress,
    DialogContent,
    DialogTitle,
    Divider,
    Grid,
    IconButton,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    makeStyles,
    Table,
    TableBody,
    TableCell,
    TableRow,
    Tooltip,
    Typography
} from "@material-ui/core";
import CloseIcon from "@material-ui/icons/Close";
import {getIcon} from "../types/icons";
import {findGeoRelayViewByID, getRelayType} from "../util/aggregate-relays";
import {DetailsInfo, GeoRelayView, NodeDetails, NodeIdentifier} from "../types/responses";
import {FullHeightDialog, SnackbarMessage, SnackbarMessages} from "../types/ui";
import {RelayFlag, RelayFlagLabel} from "../types/relay";
import {backend} from "../util/util";

/**
 * Styles according to Material UI doc for components used in AppSettings component
 */
const useStyle = makeStyles(() => ({
    closeButton: {
        position: "absolute",
        right: "10px",
        top: "10px",
    },
    valueName: {
        minWidth: "150px",
    },
    noMaxWidth: {
        maxWidth: "none",
    },
    scroll: {
        height: "calc(80vh - 0px)",
        overflowY: "scroll",
        overflowX: "hidden",
    },
    title: {
        display: "inline",
    },
    titleTypeIcon: {
        display: "inline",
        paddingRight: "16px",
    },
}))

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
    relays: GeoRelayView[]

    /**
     * Show a message in the snackbar
     * @param message - what to display to user and at which severity
     */
    showSnackbarMessage: (message: SnackbarMessage) => void
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
 * @param showSnackbarMessage - The event handler for showing a snackbar message
 */
export const RelayDetailsDialog: React.FunctionComponent<Props> = ({
                                                                       showDialog,
                                                                       closeDialog,
                                                                       relays,
                                                                       showSnackbarMessage,
                                                                   }) => {
    const [relayIdentifiers, setRelayIdentifiers] = useState<NodeIdentifier[]>([])
    const [nodeDetailsId, setNodeDetailsId] = useState<number>()
    const [rawRelayDetails, setRawRelayDetails] = useState<NodeDetails>()
    const [relayDetails, setRelayDetails] = useState<DetailsInfo[]>()

    const classes = useStyle()

    /**
     * Query relayIdentifiers for relays from backend
     */
    useEffect(() => {
        setNodeDetailsId(undefined)
        setRawRelayDetails(undefined)
        setRelayDetails(undefined)
        setRelayIdentifiers([])
        const relayDetailsIds = relays.filter(relay => relay.detailsId).map(relay => relay.detailsId)
        if (relays.length > 0 && relayDetailsIds.length === 0) {
            showSnackbarMessage({message: SnackbarMessages.NoNodeDetails, severity: "warning"})
            closeDialog()
        } else if (relayDetailsIds.length === 1) {
            setNodeDetailsId(relayDetailsIds[0]!!)
        } else if (relayDetailsIds.length > 1) {
            backend.post<NodeIdentifier[]>('/archive/node/identifiers', relayDetailsIds).then(response => {
                const requestedRelayIdentifiers = response.data
                setRelayIdentifiers(requestedRelayIdentifiers)
                if (requestedRelayIdentifiers.length > 0) {
                    setNodeDetailsId(requestedRelayIdentifiers[0].id)
                }
            }).catch(() => {
                showSnackbarMessage({message: SnackbarMessages.ConnectionFailed, severity: "error"})
                closeDialog()
            })
        }

    }, [closeDialog, relays, showSnackbarMessage])

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
        if (nodeDetailsId) {
            backend.get<NodeDetails>(`/archive/node/details/${nodeDetailsId}`).then(response => {
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
                    showSnackbarMessage({message: SnackbarMessages.ConnectionFailed, severity: "error"})
                })
        }
    }, [nodeDetailsId, relays, showSnackbarMessage])

    return (
        <FullHeightDialog
            open={showDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={relays.length > 1 ? "lg" : "md"}
            fullWidth={true}
        >
            <DialogTitle>
                {rawRelayDetails ?
                    <Box display="flex" alignItems={"center"}>
                        <div className={classes.titleTypeIcon}>
                            {nodeDetailsId ? getIcon(getRelayType(relays.find((relay) => relay.detailsId === nodeDetailsId))) : null}
                        </div>
                        <Typography
                            className={classes.title}
                            variant="h6">
                            {rawRelayDetails.nickname}
                        </Typography>
                    </Box> : <CircularProgress color={"inherit"} size={24}/>
                }
                <IconButton aria-label="close" className={classes.closeButton} onClick={closeDialog}>
                    <CloseIcon/>
                </IconButton>
            </DialogTitle>
            <Divider/>
            <DialogContent>
                <Grid container>
                    {relayIdentifiers.length > 1 &&
                    <Grid item xs={3}>
                        <Box className={classes.scroll}>
                            <List>
                                {relayIdentifiers.map((identifier) =>
                                    (identifier.id &&
                                        <Tooltip
                                            key={identifier.id}
                                            title={identifier.fingerprint}
                                            arrow={true}
                                            classes={{tooltip: classes.noMaxWidth}}
                                        >
                                            <ListItem
                                                button={true}
                                                selected={identifier.id === nodeDetailsId}
                                                onClick={() => setNodeDetailsId(identifier.id)}
                                            >
                                                <ListItemIcon>
                                                    {getIcon(getRelayType(relays.find(
                                                        (relay) => relay.detailsId === identifier.id)
                                                    ))}
                                                </ListItemIcon>
                                                <ListItemText primary={identifier.nickname}/>
                                            </ListItem>
                                        </Tooltip>
                                    )
                                )}
                            </List>
                        </Box>
                    </Grid>
                    }
                    <Grid item xs={relayIdentifiers.length > 1 ? 9 : 12}>
                        <Box className={classes.scroll} p={2}>
                            {relayDetails ?
                                <Table size={"small"}>
                                    <TableBody>
                                        {relayDetails.map((relayInfo) =>
                                            relayInfo.value &&
                                            <TableRow key={relayInfo.name}>
                                                <TableCell scope="row" className={classes.valueName}>
                                                    <Typography>{relayInfo.name}</Typography>
                                                </TableCell>
                                                <TableCell scope="row">
                                                    <Typography>{relayInfo.value}</Typography>
                                                </TableCell>
                                            </TableRow>
                                        )}
                                    </TableBody>
                                </Table> : <CircularProgress color={"inherit"} size={24}/>
                            }
                        </Box>
                    </Grid>
                </Grid>
            </DialogContent>
        </FullHeightDialog>
    )
}
