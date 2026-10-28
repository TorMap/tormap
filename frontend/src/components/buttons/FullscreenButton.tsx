import {Fullscreen as FullscreenIcon, FullscreenExit as FullscreenExitIcon} from "@mui/icons-material";
import {IconButton, Tooltip} from "@mui/material";
import {FunctionComponent} from "react";

import {tooltipTimeDelay} from "../../config";
import {usePwaState} from "../../pwa/pwa-state";
import {isFullscreenSupported, toggleFullscreen, useIsFullscreen} from "../../util/fullscreen";

/**
 * A subtle button below the about icon, and below the install button while that is shown, toggling full screen. Full
 * screen shows only the map, apart from this button. The F key does the same.
 */
export const FullscreenButton: FunctionComponent = () => {
    const isFullscreen = useIsFullscreen()
    const {installPrompt} = usePwaState()
    if (!isFullscreenSupported()) return null

    const label = isFullscreen ? "Exit full screen" : "Full screen"
    const tooltip = isFullscreen ? "Exit full screen and show the controls again" : "Full screen, showing only the map"
    return (
        <Tooltip title={`${tooltip} (F)`} placement={"right"} enterDelay={tooltipTimeDelay}>
            <IconButton
                aria-label={label}
                aria-keyshortcuts={"F"}
                onClick={() => void toggleFullscreen()}
                size={"small"}
                sx={{
                    position: "fixed",
                    top: installPrompt ? "180px" : "140px",
                    left: "11px",
                    color: "white",
                    opacity: 0.35,
                    transition: "opacity 150ms",
                    "&:hover, &:focus-visible": {opacity: 1},
                }}
            >
                {isFullscreen ? <FullscreenExitIcon/> : <FullscreenIcon/>}
            </IconButton>
        </Tooltip>
    )
}
