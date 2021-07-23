import {Dialog, DialogContent, DialogTitle, IconButton, Typography} from "@material-ui/core";
import CloseIcon from "@material-ui/icons/Close";
import React, {useEffect, useState} from "react";
import "./node-popup.scss"
import {apiBaseUrl} from "../../util/constants";
import {NodeDetails} from "../../types/node-details";

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

export const NodePopup: React.FunctionComponent<Props> = ({
                                                              showNodePopup,
                                                              setShowNodePopup,
                                                              nodeDetailsId,
                                                          }) => {
    const [nodeDetails, setNodeDetails] = useState<NodeDetails>()
    const [relayInfos, setRelayInfos] = useState<RelayInfo[]>()

    const convertBandwidthBytesToMB = (bandwidthInBytes: number | undefined) => bandwidthInBytes ?
        (bandwidthInBytes / 1000000).toFixed(2) + " MB/s"
        : undefined

    useEffect(() => {
        if (nodeDetailsId) {
            fetch(`${apiBaseUrl}/archive/node/details/${nodeDetailsId}`)
                .then(response => response.json())
                .then((relay: NodeDetails) => {
                    setNodeDetails(relay)
                    setRelayInfos([
                        {name: "Fingerprint", value: relay.fingerprint},
                        {name: "Contact", value: relay.contact},
                        {name: "Observed bandwidth", value: convertBandwidthBytesToMB(relay.bandwidthObserved)},
                        {name: "Platform", value: relay.platform},
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
                <IconButton aria-label="close" className={"closeButton"} onClick={setShowNodePopup}>
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
