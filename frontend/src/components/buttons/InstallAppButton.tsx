import {InstallDesktop as InstallDesktopIcon} from "@mui/icons-material";
import {IconButton, Tooltip} from "@mui/material";
import {FunctionComponent} from "react";

import {tooltipTimeDelay} from "../../config";
import {promptInstall} from "../../pwa/install";
import {usePwaState} from "../../pwa/pwa-state";

/**
 * A subtle button below the about icon, shown only while the browser offers to install TorMap as an app
 */
export const InstallAppButton: FunctionComponent = () => {
    const {installPrompt} = usePwaState()
    if (!installPrompt) return null

    return (
        <Tooltip
            title={"Install TorMap as an app. It opens in its own window and starts without network access."}
            placement={"right"}
            enterDelay={tooltipTimeDelay}
        >
            <IconButton
                aria-label={"Install TorMap as an app"}
                onClick={() => void promptInstall()}
                size={"small"}
                sx={{
                    position: "fixed",
                    top: "140px",
                    left: "11px",
                    color: "white",
                    opacity: 0.35,
                    transition: "opacity 150ms",
                    "&:hover, &:focus-visible": {opacity: 1},
                }}
            >
                <InstallDesktopIcon/>
            </IconButton>
        </Tooltip>
    )
}
