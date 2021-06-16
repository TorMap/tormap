import {Button, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, Typography} from "@material-ui/core";
import CloseIcon from "@material-ui/icons/Close";
import React, {useEffect, useState} from "react";
import "./node-popup.scss"
import {Relay} from "../../types/relay";
import {apiBaseUrl} from "../../util/constants";

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
    const [showRelayDetails, setShowRelayDetails] = useState<boolean>(false)

    const formatLocation = (city: string | undefined, country: string | undefined) => {
        if (city) {
            return city + ", " + country
        } else return country
    }

    const convertBandwidthBytesToMB = (bandwidthInBytes: number | undefined) => bandwidthInBytes ?
        (bandwidthInBytes / 1000000).toFixed(2) + " MB/s"
        : undefined

    useEffect(() => {
        fetch(apiBaseUrl + "/node/relay/" + relayId)
            .then(response => response.json())
            .then((relay: Relay) => {
                setRelay(relay)
                setRelayInfos([
                    {name: "Fingerprint", value: relay.fingerprint},
                    {name: "AS family", value: relay.as_name},
                    {name: "Contact", value: relay.contact},
                    {name: "Location", value: formatLocation(relay.city_name, relay.country_name)},
                    {name: "Verified domains", value: relay.verified_host_names?.join(", ")},
                    {name: "First seen", value: relay.first_seen},
                    {name: "Last seen", value: relay.last_seen},
                    {name: "Flags", value: relay.flags?.join(", ")},
                    {name: "Observed bandwidth", value: convertBandwidthBytesToMB(relay.observed_bandwidth)},
                    {name: "Consensus weight", value: relay.consensus_weight},
                    {name: "Platform", value: relay.platform},
                ])
            })
            .catch(console.log)
    }, [relayId])

    return (
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
                    <CloseIcon/>
                </IconButton>
            </DialogTitle>
            <DialogContent
                dividers
                className={"dialogfield"}
            >
                {showRelayDetails ?
                    <pre>{JSON.stringify(relay, null, 2)}</pre>
                    : relayInfos?.map((relayInfo) =>
                        relayInfo.value ? <p key={relayInfo.name}><b>{relayInfo.name}</b>: {relayInfo.value}</p> : undefined
                    )
                }
            </DialogContent>
            <DialogActions className={"dialogfield"}>
                <Button
                    autoFocus
                    onClick={() => setShowRelayDetails(prevState => !prevState)}
                    color="primary"
                >
                    {showRelayDetails ? "Show summary" : "Show details"}
                </Button>
            </DialogActions>
        </Dialog>
    );
}

interface RelayInfo {
    name: string
    value: string | number | undefined
}
