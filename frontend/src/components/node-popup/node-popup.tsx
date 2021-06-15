import {
    Dialog,
    DialogContent,
    DialogTitle,
    IconButton,
    Typography
} from "@material-ui/core";
import CloseIcon from "@material-ui/icons/Close";
import React, {useEffect, useState} from "react";
import "./node-popup.scss"
import {Relay} from "../../types/relay";
import {apiBaseUrl} from "../../util/constants";

interface Props{
    /**
     * Whether the node popup will be shown
     */
    showNodePopup: boolean,
    /**
     * Method to set dialog visibility
     */
    setShowNodePopup: () => void,
    /**
     * Id of the relay to be displayed
     */
    relayId?: number,
}

export const NodePopup: React.FunctionComponent<Props> = ({
    showNodePopup,
    setShowNodePopup,
    relayId,
}) => {
    const [relay, setRelay] = useState<Relay>()
    const [relayInfos, setRelayInfos] = useState<RelayInfo[]>()

    useEffect(() => {
        fetch(apiBaseUrl + "/node/relay/" + relayId)
            .then(response => response.json())
            .then((relay: Relay) => {
                setRelay(relay)
                setRelayInfos([
                        {name: "Fingerprint", value: relay.fingerprint},
                        {name: "AS family", value: relay.as_name},
                        {name: "Contact", value: relay.contact},
                        {name: "First seen", value: relay.first_seen},
                        {name: "Last seen", value: relay.last_seen},
                        {name: "Flags", value: relay.flags?.join(", ")},
                        {name: "Observed bandwidth", value: relay.observed_bandwidth ? (relay.observed_bandwidth / 1000000).toFixed(2) + " MB/s" : undefined},
                        {name: "Consensus weight", value: relay.consensus_weight},
                        {name: "Platform", value: relay.platform},
                ])
            })
            .catch(console.log)
    }, [relayId])

    return(
        <Dialog
            open={showNodePopup}
            onClose={setShowNodePopup}
            onBackdropClick={setShowNodePopup}
        >
            <DialogTitle
                className={"dialogfield"}
            >
                <Typography variant="h6">{relay?.nickname}</Typography>
                <IconButton aria-label="close" className={"closeButton"} onClick={setShowNodePopup}>
                    <CloseIcon />
                </IconButton>
            </DialogTitle>
            <DialogContent
                dividers
                className={"dialogfield"}
            >
                {relayInfos?.map((relayInfo) =>
                    relayInfo.value ? <p><b>{relayInfo.name}</b>: {relayInfo.value}</p> : undefined
                )}
            </DialogContent>
        </Dialog>
    );
}

interface RelayInfo {
    name: string
    value: string | number | undefined
}
