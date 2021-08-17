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

import {apiBaseUrl} from "../util/constants";
import CloseIcon from "@material-ui/icons/Close";
import {Colors} from "../util/Config";
import DirectionsRunIcon from "@material-ui/icons/DirectionsRun";
import {getIcon} from "../util/jsx-helpers";

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
        width: "250px",
    },
    infoPadding: {
        paddingLeft: "270px",
    },
    dialogSize: {
        maxWidth: "700px",
        minHeight: "500px",
    },
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
    const [relayNicknames, setRelayNicknames] = useState<RelayNickname[]>([])
    const [nodeDetailsId, setNodeDetailsId] = useState<number | undefined>()
    const [nodeDetails, setNodeDetails] = useState<NodeDetails>()
    const [relayInfos, setRelayInfos] = useState<RelayInfo[]>()
    const classes = useStyle()

    useEffect(() => {
        setNodeDetailsId(undefined)
        let relayIDs: Array<string> = []
        relays.forEach((relay) => {
            relayIDs.push(relay.detailsId)
        })
        const payload = JSON.stringify(relayIDs)
        setRelayNicknames([])
        fetch(`${apiBaseUrl}/archive/node/identifiers`,{
            headers: {
                'Content-Type': 'application/json',
            },
            method: "post",
            body: payload,
        })
            .then(response => response.json())
            .then(relays => setRelayNicknames(relays))
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
            <div className={classes.dialogSize}>
                <DialogTitle className={classes.infoPadding}>
                    <Typography variant="h6">{nodeDetailsId ? (nodeDetails?.nickname) : (`No information`)}</Typography>
                    <IconButton aria-label="close" className={classes.closeButton} onClick={closeNodePopup}>
                        <CloseIcon/>
                    </IconButton>
                </DialogTitle>
                <DialogContent
                    dividers
                    className={classes.infoPadding}>

                    <div >
                        {nodeDetailsId ? relayInfos?.map((relayInfo) =>
                            relayInfo.value ? <p key={relayInfo.name}><b>{relayInfo.name}</b>: {relayInfo.value}</p> : undefined
                        ) : <p>Currently we do not have more information about this node.</p>}
                    </div>
                </DialogContent>
                <Drawer
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
                        {relayNicknames.map((relay, index) =>
                            (relay.id ?
                                (<ListItem button key={relay.id}
                                           selected={+relay.id === nodeDetailsId}
                                           onClick={() => setNodeDetailsId(+relay.id)}>
                                    <ListItemIcon>{getIcon(relay.id, relays)}</ListItemIcon>
                                    <ListItemText primary={relay.nickname}/>
                                </ListItem>) : (null) ))}
                    </List>
                </Drawer>
            </div>
        </Dialog>
    )
}

interface RelayInfo {
    name: string
    value: string | number | undefined
}

interface RelayNickname {
    id: string,
    fingerprint: string
    nickname: string
}