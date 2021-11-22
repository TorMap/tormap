import {FunctionComponent} from "react";
import {UIProps} from "../types/ui";
import {useMediaQuery, useTheme} from "@mui/material";
import {DesktopUI} from "./DesktopUI";
import {MobileUI} from "./MobileUI";

export const UI: FunctionComponent<UIProps> = ({availableDays, sliderValue, setSliderValue, statistics}) => {

    const theme = useTheme()
    const uiBreak = useMediaQuery(theme.breakpoints.up("lg"))

    return (uiBreak ? <DesktopUI availableDays={availableDays} sliderValue={sliderValue} setSliderValue={setSliderValue} statistics={statistics}/>
        : <MobileUI availableDays={availableDays} sliderValue={sliderValue} setSliderValue={setSliderValue} statistics={statistics}/>)
}

//<DesktopUI availableDays={availableDays} setSliderValue={setSliderValue} statistics={statistics}/>
//<MobileUI availableDays={availableDays} setSliderValue={setSliderValue} statistics={statistics}/>