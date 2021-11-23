import {FunctionComponent} from "react";
import {UIProps} from "../../types/ui";
import {useMediaQuery, useTheme} from "@mui/material";
import {DesktopUI} from "./DesktopUI";
import {MobileUI} from "./MobileUI";

/**
 * A component wrapping all UI elements and deciding whether Dexctop or Mobile UI is renderd
 *
 * @param statistics - a Statistics object for data to display
 */
export const UI: FunctionComponent<UIProps> = ({statistics}) => {

    const theme = useTheme()
    const desktop = useMediaQuery(theme.breakpoints.up("lg"))

    return (desktop ? <DesktopUI statistics={statistics}/>
        : <MobileUI statistics={statistics}/>)
}