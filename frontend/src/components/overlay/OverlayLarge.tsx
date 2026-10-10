import {Box} from "@mui/material";
import {FunctionComponent} from "react";

import {AppSettings} from "../accordion/AppSettings";
import {MapStats} from "../accordion/MapStats";
import {DateSlider} from "../date/DateSlider";
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
                position: "fixed",
                right: "1%",
                top: "15px",
                // Leave room for the date field in the bottom right corner and scroll the panel instead
                maxHeight: "calc(100vh - 110px)",
                overflowY: "auto",
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

export default OverlayLarge