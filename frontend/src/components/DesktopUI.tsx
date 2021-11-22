import React, {FunctionComponent, useCallback} from "react";
import {UIProps} from "../types/ui";
import {DateSlider} from "./date-slider";
import {Box} from "@mui/material";
import {AppSettings} from "./app-settings";
import {MapStats} from "./map-stats";

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
                    <MapStats stats={statistics}/>
                </Box>
            }
        </Box>
    )
}