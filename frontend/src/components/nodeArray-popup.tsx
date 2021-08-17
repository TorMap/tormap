import React, {useEffect, useState} from "react";
import {
    Dialog,
    DialogContent,
    DialogTitle,
    Drawer, IconButton,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    makeStyles, Typography
} from "@material-ui/core";
import {NodeDetails} from "../types/node-details";
import {GeoRelayView} from "../types/geo-relay";


import InboxIcon from '@material-ui/icons/MoveToInbox';
import MailIcon from '@material-ui/icons/Mail';
import {apiBaseUrl} from "../util/constants";
import CloseIcon from "@material-ui/icons/Close";

/**
 *
 */
const useStyle = makeStyles(() => ({
    closeButton: {
        position: "absolute",
        right: "10px",
        top: "10px",
    },
    drawer: {
        width: "200px",
    },
    infoPadding: {
        paddingLeft: "200px",
    }
}))

interface Props {
    showNodePopup: boolean

    relays: GeoRelayView[]

    closeNodePopup: () => void
}

const formatBytesToMBPerSecond = (bandwidthInBytes?: number) => bandwidthInBytes ?
    (bandwidthInBytes / 1000000).toFixed(2) + " MB/s"
    : undefined

const formatSecondsToHours = (seconds?: number) => seconds ?
    (seconds / 3600).toFixed(2) + " hours"
    : undefined

const formatBoolean = (value?: boolean) => value === null || value === undefined ? undefined : value ? "yes" : "no"

export const NodeArrayPopup: React.FunctionComponent<Props> = ({
                                                                   showNodePopup,
                                                                   relays,
                                                                   closeNodePopup,
                                                               }) => {
    const [nodeDetailsId, setNodeDetailsId] = useState<number | undefined>()
    const [nodeDetails, setNodeDetails] = useState<NodeDetails>()
    const [relayInfos, setRelayInfos] = useState<RelayInfo[]>()
    const classes = useStyle()

    useEffect(() => {
        setNodeDetailsId(+relays[0].detailsId)
    }, [relays])

    useEffect(() => {
        if (nodeDetailsId) {
            fetch(`${apiBaseUrl}/archive/node/details/${nodeDetailsId}`)
                .then(response => response.json())
                .then((relay: NodeDetails) => {
                    setNodeDetails(relay)
                    setRelayInfos([
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
                        {name: "Infos published on", value: relay.day},
                    ])
                })
                .catch(console.log)
        }
    }, [nodeDetailsId])

    return (
        <Dialog
            open={showNodePopup}
            onClose={closeNodePopup}
            onBackdropClick={closeNodePopup}
        >
            <DialogTitle className={classes.infoPadding}>
                <Typography variant="h6">{nodeDetailsId ? (`${nodeDetailsId.toString()}: ${nodeDetails?.nickname}`) : (`${nodeDetailsId?.toString()} No information`)}</Typography>
                <IconButton aria-label="close" className={classes.closeButton} onClick={closeNodePopup}>
                    <CloseIcon/>
                </IconButton>
            </DialogTitle>
            <DialogContent
                dividers
                className={"dialogfield"}>

                <div className={classes.infoPadding}>
                    {nodeDetailsId ? relayInfos?.map((relayInfo) =>
                        relayInfo.value ? <p key={relayInfo.name}><b>{relayInfo.name}</b>: {relayInfo.value}</p> : undefined
                    ) : <p>Currently we do not have more information about this node.</p>}
                </div>

                <Drawer
                    className={classes.drawer}
                    PaperProps={{
                        style: {
                            position: "absolute",
                            width: "170px",
                        }
                    }}
                    anchor={"left"}
                    variant={"permanent"}>
                    <List>
                        {relays.map((relay, index) =>
                            (relay.detailsId ?
                                (<ListItem button key={relay.detailsId}
                                          onClick={() => setNodeDetailsId(+relay.detailsId)}>
                                    <ListItemText primary={relay.detailsId}/>
                                </ListItem>) : (null) ))}
                    </List>
                </Drawer>
            </DialogContent>
        </Dialog>
    )
}

interface RelayInfo {
    name: string
    value: string | number | undefined
}