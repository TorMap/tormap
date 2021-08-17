import {GeoRelayView} from "../types/geo-relay";
import TimelineIcon from "@material-ui/icons/Timeline";
import {Colors} from "./Config";
import {RelayFlag} from "../types/relay";
import DirectionsRunIcon from "@material-ui/icons/DirectionsRun";
import React from "react";
import SecurityIcon from "@material-ui/icons/Security";


export const getIcon = (relayID: string, relays: GeoRelayView[]) => {
    const relay: GeoRelayView | undefined = relays.find((value) => value.detailsId === relayID)
    if (relay === undefined) return <TimelineIcon style={{color: Colors.Default}}/>
    if (relay.flags?.includes(RelayFlag.Exit)) return <DirectionsRunIcon style={{color: Colors.Exit}}/>
    else if (relay.flags?.includes(RelayFlag.Guard)) return <SecurityIcon style={{color: Colors.Guard}}/>
    return <TimelineIcon style={{color: Colors.Default}}/>
}