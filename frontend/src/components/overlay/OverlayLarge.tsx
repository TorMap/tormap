import React, {FunctionComponent} from "react";
import {DateSlider} from "../date/DateSlider";
import {Box} from "@mui/material";
import {AppSettings} from "../accordion/AppSettings";
import {MapStats} from "../accordion/MapStats";
import {ResponsiveDatePicker} from "../date/ResponsiveDatePicker";

/**
 * A component wrapping all UI elements for devices with large screen sizes
 */
export const OverlayLarge: FunctionComponent = () => {
    return (
        <Box>
            <Box sx={{
                position: "fixed",
                bottom: "15px",
                width: "50%",
                left: "25%",
            }}>
                <DateSlider/>
            </Box>
            <ResponsiveDatePicker largeScreen={true}/>
            <Box sx={{
                position: "absolute",
                right: "1%",
                top: "15px",
                paddingBottom: "10px",
                maxWidth: "20%",
            }}>
                <AppSettings elevation={24}/>
            </Box>
            <Box sx={{
                position: "fixed",
                left: "1%",
                bottom: "15px",
                maxWidth: "20%",
            }}>
                <MapStats elevation={24} defaultExpanded={true}/>
            </Box>
        </Box>
    )
}