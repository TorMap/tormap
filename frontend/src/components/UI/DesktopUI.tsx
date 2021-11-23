import React, {FunctionComponent} from "react";
import {UIProps} from "../../types/ui";
import {DateSlider} from "./UI-elements/date-slider";
import {Box} from "@mui/material";
import {AppSettings} from "./UI-elements/app-settings";
import {MapStats} from "./UI-elements/map-stats";
import {TorUsageDatePicker} from "./UI-elements/date-picker";

/**
 * A component wrapping all UI elements for Desktop devices
 *
 * @param statistics - a Statistics object for data to display
 */
export const DesktopUI: FunctionComponent<UIProps> = ({statistics}) =>{

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
            <TorUsageDatePicker/>
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