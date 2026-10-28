import {CloudOff as CloudOffIcon, History as HistoryIcon} from "@mui/icons-material";
import {Chip, Tooltip} from "@mui/material";
import {format} from "date-fns";
import {FunctionComponent} from "react";

import {tooltipTimeDelay} from "../../config";
import {useOffline} from "../../util/online";

interface Props {
    /** When the shown day was downloaded (ISO 8601), undefined if it came from the network */
    cachedAt?: string
}

/**
 * Tells that the map shows a copy of the day that this browser kept, and how old the copy is
 */
export const CachedDataChip: FunctionComponent<Props> = ({cachedAt}) => {
    const offline = useOffline()
    if (!cachedAt) return null
    const downloaded = format(new Date(cachedAt), "yyyy-MM-dd HH:mm")

    return (
        <Tooltip
            title={offline
                ? "You are offline. The map shows the copy of this day that was saved in this browser."
                : "The map shows the copy of this day that was saved in this browser. A newer version, if any, loads in the background for the next time."}
            placement={"top-start"}
            enterDelay={tooltipTimeDelay}
            describeChild
        >
            <Chip
                icon={offline ? <CloudOffIcon/> : <HistoryIcon/>}
                label={`${offline ? "Offline · saved" : "Saved"} ${downloaded}`}
                size={"small"}
                sx={{
                    position: "fixed",
                    // Large screens show the map stats in the bottom left corner, so the chip sits above the date field
                    left: {xs: "10px", lg: "auto"},
                    right: {lg: "1%"},
                    bottom: {xs: "24px", lg: "72px"},
                    backgroundColor: "rgba(38, 38, 38, 0.85)",
                    color: "#b4b4b4",
                    "& .MuiChip-icon": {color: "#b4b4b4"},
                }}
            />
        </Tooltip>
    )
}
