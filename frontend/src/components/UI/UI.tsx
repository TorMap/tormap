import {FunctionComponent} from "react";
import {UIProps} from "../../types/ui";
import {useMediaQuery, useTheme} from "@mui/material";
import {DesktopUI} from "./DesktopUI";
import {MobileUI} from "./MobileUI";

/**
 * A component wrapping all UI elements and deciding whether Dexctop or Mobile UI is renderd
 *
 * @param availableDays - Days available for display
 * @param sliderValue - the current slider value
 * @param setSliderValue - a callback for setting the slider value
 * @param statistics - a Statistics object for data to display
 */
export const UI: FunctionComponent<UIProps> = ({availableDays, sliderValue, setSliderValue, statistics}) => {

    const theme = useTheme()
    const desktop = useMediaQuery(theme.breakpoints.up("lg"))

    return (desktop ? <DesktopUI availableDays={availableDays} sliderValue={sliderValue} setSliderValue={setSliderValue} statistics={statistics}/>
        : <MobileUI availableDays={availableDays} sliderValue={sliderValue} setSliderValue={setSliderValue} statistics={statistics}/>)
}