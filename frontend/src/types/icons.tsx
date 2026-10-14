import {
    DirectionsRun as DirectionsRunIcon,
    Functions as FunctionsIcon,
    Group as GroupIcon,
    Public as PublicIcon,
    Security as SecurityIcon,
    Timeline as TimelineIcon,
} from "@mui/icons-material";
import {FunctionComponent} from "react";

import {useSettings} from "../context/settings-context";
import {RelayType} from "./relay";

const relayTypeIcons = {
    [RelayType.Exit]: DirectionsRunIcon,
    [RelayType.Guard]: SecurityIcon,
    [RelayType.Other]: TimelineIcon,
}

/**
 * The icon of a relay type, painted in the color selected in the settings
 * @param relayType the Icon-/ Relay-Type
 * @param color paints the icon in this color instead, e.g. while a new color is being picked
 */
export const RelayTypeIcon: FunctionComponent<{ relayType: RelayType, color?: string }> = ({relayType, color}) => {
    const {settings} = useSettings()
    const Icon = relayTypeIcons[relayType]
    return <Icon sx={{color: color ?? settings.relayTypeColors[relayType]}}/>
}

export const TotalRelaysIcon = <FunctionsIcon/>
export const RelayFamilyIcon = <GroupIcon/>
export const EarthIcon = <PublicIcon/>
