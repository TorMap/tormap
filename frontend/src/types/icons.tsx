import {Colors} from "../util/config";
import DirectionsRunIcon from "@mui/icons-material/DirectionsRun";
import React from "react";
import SecurityIcon from "@mui/icons-material/Security";
import TimelineIcon from "@mui/icons-material/Timeline";
import SubdirectoryArrowRightIcon from "@mui/icons-material/SubdirectoryArrowRight";
import GroupIcon from "@mui/icons-material/Group";
import PublicIcon from "@mui/icons-material/Public";
import {RelayType} from "./relay";

/**
 * Returns the Icon's JSX.Element
 * @param relayType the Icon-/ Relay-Type
 */
export function getIcon(relayType: RelayType): JSX.Element | null {
    switch (relayType) {
        case RelayType.Exit:
            return ExitRelayIcon
        case RelayType.Guard:
            return GuardRelayIcon
        case RelayType.Other:
            return OtherRelayIcon
        default:
            return null
    }
}

export const ExitRelayIcon = <DirectionsRunIcon style={{color: Colors.Exit}}/>
export const GuardRelayIcon = <SecurityIcon style={{color: Colors.Guard}}/>
export const OtherRelayIcon = <TimelineIcon style={{color: Colors.Default}}/>
export const TotalRelaysIcon = <SubdirectoryArrowRightIcon/>
export const FamilyCountIcon = <GroupIcon/>
export const CountryCountIcon = <PublicIcon/>
