import {
    Dialog,
    DialogContent,
    DialogTitle,
    Drawer,
    IconButton,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    makeStyles,
    Typography
} from "@material-ui/core";
import CloseIcon from "@material-ui/icons/Close";
import React, {useEffect, useState} from "react";
import {NodeDetails} from "../types/node-details";
import InboxIcon from "@material-ui/icons/MoveToInbox";
import MailIcon from "@material-ui/icons/Mail";
import {apiBaseUrl} from "../util/Config";

/**
 * Styles according to Material UI doc for components used in NodePopup component
 */
const useStyle = makeStyles(() => ({
    closeButton: {
        position: "absolute",
        right: "10px",
        top: "10px",
    }
}))

interface Props {
    /**
     * Whether the node popup will be shown
     */
    showNodePopup: boolean,
    /**
     * Method to set dialog visibility
     */
    setShowNodePopup: () => void,
    /**
     * Id of the relay node details
     */
    nodeDetailsId?: number,
}

const formatBytesToMBPerSecond = (bandwidthInBytes?: number) => bandwidthInBytes ?
    (bandwidthInBytes / 1000000).toFixed(2) + " MB/s"
    : undefined

const formatSecondsToHours = (seconds?: number) => seconds ?
    (seconds / 3600).toFixed(2) + " hours"
    : undefined

const formatBoolean = (value?: boolean) => value === null || value === undefined ? undefined : value ? "yes" : "no"

export const NodePopup: React.FunctionComponent<Props> = ({
                                                              showNodePopup,
                                                              setShowNodePopup,
                                                              nodeDetailsId,
                                                          }) => {
    const [nodeDetails, setNodeDetails] = useState<NodeDetails>()
    const [relayInfos, setRelayInfos] = useState<RelayInfo[]>()
    const classes = useStyle()

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
            onClose={setShowNodePopup}
            onBackdropClick={setShowNodePopup}
        >
            <DialogTitle
                className={"dialogfield"}
            >
                <Typography variant="h6">{nodeDetailsId ? nodeDetails?.nickname : "No information"}</Typography>
                <IconButton aria-label="close" className={classes.closeButton} onClick={setShowNodePopup}>
                    <CloseIcon/>
                </IconButton>
            </DialogTitle>
            <DialogContent
                dividers
                className={"dialogfield"}
            >
                {nodeDetailsId ? relayInfos?.map((relayInfo) =>
                    relayInfo.value ? <p key={relayInfo.name}><b>{relayInfo.name}</b>: {relayInfo.value}</p> : undefined
                ) : <p>Currently we do not have more information about this node.</p>}
            </DialogContent>
        </Dialog>
    );
}

interface RelayInfo {
    name: string
    value: string | number | undefined
}
