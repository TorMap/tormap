import React, {useEffect, useState} from "react";
import {
    CircularProgress,
    Dialog,
    DialogContent,
    DialogTitle,
    Drawer, IconButton,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    makeStyles, Table, TableCell, TableRow, Tooltip, Typography, withStyles
} from "@material-ui/core";
import CloseIcon from "@material-ui/icons/Close";
import {apiBaseUrl} from "../util/Config";
import {getIcon} from "../types/icons";
import {findGeoRelayViewByID, getRelayType} from "../util/aggregate-relays";
import {GeoRelayView, NodeDetails, NodeDetailsInfo, NodeIdentifier} from "../types/responses";


const useStyle = makeStyles(() => ({
    closeButton: {
        position: "absolute",
        right: "10px",
        top: "10px",
    },
    drawer: {
        width: "250px",
    },
    infoPadding: {
        paddingLeft: "270px",
    },
    valueName: {
        minWidth: "150px",
    },
}))

const FullHeightDialog = withStyles((theme) => ({
    paper: {
        height: '100%'
    },
}))(Dialog);


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
     * Relays which the user ca view detailed information about
     */
    relays: GeoRelayView[]
}

/**
 * Format number represented as bytes to rounded mega byte string representation
 * @param bandwidthInBytes - number to be formatted
 */
const formatBytesToMBPerSecond = (bandwidthInBytes?: number) => bandwidthInBytes ?
    (bandwidthInBytes / 1000000).toFixed(2) + " MB/s"
    : undefined

/**
 * Format number of seconds into an string representaiton in hours
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

export const RelayDetailsDialog: React.FunctionComponent<Props> = ({
                                                                      showDialog,
                                                                      closeDialog,
                                                                      relays,
                                                                  }) => {
    const [relayIdentifiers, setRelayIdentifiers] = useState<NodeIdentifier[]>([])
    const [nodeDetailsId, setNodeDetailsId] = useState<number | undefined>()
    const [rawRelayDetails, setRawRelayDetails] = useState<NodeDetails>()
    const [relayDetails, setRelayDetails] = useState<NodeDetailsInfo[]>()
    const [isLoading, setIsLoading] = useState(true)

    const classes = useStyle()

    // Load identifiers for relays
    useEffect(() => {
        setIsLoading(true)
        setNodeDetailsId(undefined)
        setRelayIdentifiers([])
        fetch(`${apiBaseUrl}/archive/node/identifiers`, {
            headers: {
                'Content-Type': 'application/json',
            },
            method: "post",
            body: JSON.stringify(relays.map(relay => relay.detailsId)),
        })
            .then(response => response.json())
            .then(identifiers => {
                setRelayIdentifiers(identifiers)
                if (identifiers.length > 0) {
                    setNodeDetailsId(+identifiers[0].id)
                }
                setIsLoading(false)
            })
            .catch(reason => {
                setIsLoading(false)
            })
    }, [relays])

    // Load details for selected relay
    useEffect(() => {
        if (nodeDetailsId) {
            setIsLoading(true)
            fetch(`${apiBaseUrl}/archive/node/details/${nodeDetailsId}`)
                .then(response => response.json())
                .then((relay: NodeDetails) => {
                    setRawRelayDetails(relay)
                    setRelayDetails([
                        {name: "Fingerprint", value: relay.fingerprint},
                        {name: "IP address", value: relay.address},
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
                    setIsLoading(false)
                })
                .catch(reason => {
                    setIsLoading(false)
                })
        }
    }, [nodeDetailsId])

    return (
        <FullHeightDialog
            open={showDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={relays.length > 1 ? "lg" : "md"}
            fullWidth={true}
        >
            <div>
                {!isLoading && <DialogTitle className={relays.length > 1 ? classes.infoPadding : undefined}>
                    <Typography
                        variant="h6">{nodeDetailsId ? (rawRelayDetails?.nickname) : (`No information`)}</Typography>
                    <IconButton aria-label="close" className={classes.closeButton} onClick={closeDialog}>
                        <CloseIcon/>
                    </IconButton>
                </DialogTitle>}
                <DialogContent
                    dividers
                    className={relays.length > 1 ? classes.infoPadding : undefined}>
                    <div>
                        {isLoading ?
                            <CircularProgress color={"inherit"}/> : nodeDetailsId ?
                                <Table size={"small"}>
                                    {relayDetails?.map((relayInfo) =>
                                        relayInfo.value &&
                                        <TableRow>
                                            <TableCell scope="row" className={classes.valueName}>
                                                <Typography>{relayInfo.name}</Typography>
                                            </TableCell>
                                            <TableCell scope="row">
                                                <Typography>{relayInfo.value}</Typography>
                                            </TableCell>
                                        </TableRow>
                                    )}
                                </Table>
                                 : <p>We do not have any information about this relay for this date.</p>}
                    </div>
                </DialogContent>
                {relays.length > 1 && <Drawer
                    className={classes.drawer}
                    PaperProps={{
                        style: {
                            position: "absolute",
                            width: "250px",
                        }
                    }}
                    anchor={"left"}
                    variant={"permanent"}>
                    <List>
                        {relayIdentifiers.map((relay) =>
                            (relay.id &&
                                <Tooltip title={relay.fingerprint} arrow={true}>
                                    <div>
                                        <ListItem button key={relay.id}
                                                  selected={+relay.id === nodeDetailsId}
                                                  onClick={() => setNodeDetailsId(+relay.id)}>
                                            <ListItemIcon>{getIcon(getRelayType(findGeoRelayViewByID(relay.id, relays)))}</ListItemIcon>
                                            <ListItemText primary={relay.nickname}/>
                                        </ListItem>
                                    </div>
                                </Tooltip>))}
                    </List>
                </Drawer>}
            </div>
        </FullHeightDialog>
    )
}
