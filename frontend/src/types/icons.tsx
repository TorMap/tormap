import {Colors} from "../util/Config";
import DirectionsRunIcon from "@material-ui/icons/DirectionsRun";
import React from "react";
import SecurityIcon from "@material-ui/icons/Security";
import TimelineIcon from "@material-ui/icons/Timeline";
import SubdirectoryArrowRightIcon from "@material-ui/icons/SubdirectoryArrowRight";
import GroupIcon from "@material-ui/icons/Group";
import PublicIcon from "@material-ui/icons/Public";
import {RelayType} from "./relay";

/**
 * Icon-Types
 */
export enum Icon {
    ExitRelay,
    GuardRelay,
    DefaultRelay,
    TotalRelays,
    FamilyCount,
    CountryCount,
}

/**
 * Returns the Icon's JSX.Element
 * @param icon the Icon-/ Relay-Type
 */
export function getIcon(icon: Icon | RelayType | undefined): JSX.Element | null{
    switch (icon){
        case Icon.ExitRelay || RelayType.Exit: return <DirectionsRunIcon style={{color: Colors.Exit}}/>
        case Icon.GuardRelay || RelayType.Guard: return <SecurityIcon style={{color: Colors.Guard}}/>
        case Icon.DefaultRelay || RelayType.default: return <TimelineIcon style={{color: Colors.Default}}/>
        case Icon.TotalRelays: return <SubdirectoryArrowRightIcon/>
        case Icon.FamilyCount: return <GroupIcon/>
        case Icon.CountryCount: return <PublicIcon/>
    }
    return null
}