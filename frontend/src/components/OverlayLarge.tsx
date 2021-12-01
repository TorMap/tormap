import React, {FunctionComponent} from "react";
import {OverlayProps} from "../types/ui";
import {DateSlider} from "./date/DateSlider";
import {Box} from "@mui/material";
import {AppSettings} from "./accordion/AppSettings";
import {MapStats} from "./accordion/MapStats";
import {DatePickerLarge} from "./date/DatePickerLarge";

/**
 * A component wrapping all UI elements for devices with large screen sizes
 *
 * @param statistics - a Statistics object for data to display
 */
export const OverlayLarge: FunctionComponent<OverlayProps> = ({statistics}) =>{

    return (
        <Box>
            <Box sx={{
                position: "fixed",
                bottom: "2%",
                width: "50%",
                left: "25%",
            }}>
                <DateSlider />
            </Box>
            <DatePickerLarge/>
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