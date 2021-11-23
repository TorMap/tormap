import React, {FunctionComponent, useCallback} from "react";
import {UIProps} from "../../types/ui";
import {DateSlider} from "./UI-elements/date-slider";
import {Box} from "@mui/material";
import {AppSettings} from "./UI-elements/app-settings";
import {MapStats} from "./UI-elements/map-stats";

/**
 * A component wrapping all UI elements for Desktop devices
 *
 * @param availableDays - Days available for display
 * @param sliderValue - the current slider value
 * @param setSliderValue - a callback for setting the slider value
 * @param statistics - a Statistics object for data to display
 */
export const DesktopUI: FunctionComponent<UIProps> = ({availableDays, setSliderValue, statistics}) =>{

    return (
        <Box>
            <DateSlider availableDays={availableDays} setValue={useCallback(setSliderValue, [setSliderValue])}/>
            <Box sx={{
                position: "absolute",
                right: "1%",
                top: "15px",
                paddingBottom: "10px",
                maxWidth: "20%",
            }}>
                <AppSettings elevation={24}/>
            </Box>
            {statistics &&
                <Box sx={{
                    position: "fixed",
                    left: "1%",
                    bottom: "15px",
                    maxWidth: "20%",
                }} >
                    <MapStats stats={statistics} elevation={24} defaultExpanded={true}/>
                </Box>
            }
        </Box>
    )
}