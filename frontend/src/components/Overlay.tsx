import {FunctionComponent} from "react";
import {OverlayProps} from "../types/ui";
import {useMediaQuery, useTheme} from "@mui/material";
import {OverlayLarge} from "./OverlayLarge";
import {OverlaySmall} from "./OverlaySmall";

/**
 * A component wrapping all UI elements and deciding whether large or small overlay components are rendered
 *
 * @param statistics - a Statistics object for data to display
 */
export const Overlay: FunctionComponent<OverlayProps> = ({statistics}) => {

    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

    return (isLargeScreen ? <OverlayLarge statistics={statistics}/>
        : <OverlaySmall statistics={statistics}/>)
}