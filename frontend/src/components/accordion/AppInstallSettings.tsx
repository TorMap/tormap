import {ExpandMore as ExpandMoreIcon, InstallDesktop as InstallDesktopIcon} from "@mui/icons-material";
import {Accordion, AccordionDetails, AccordionSummary, Button, Typography} from "@mui/material";
import {FunctionComponent} from "react";

import {promptInstall} from "../../pwa/install";
import {usePwaState} from "../../pwa/pwa-state";

interface Props {
    elevation: number
}

/**
 * Offers installing TorMap as an app, only while the browser allows it. Collapsed below all other settings, so it does
 * not get in the way.
 */
export const AppInstallSettings: FunctionComponent<Props> = ({elevation}) => {
    const {installPrompt} = usePwaState()
    if (!installPrompt) return null

    return (
        <Accordion elevation={elevation}>
            <AccordionSummary
                expandIcon={<ExpandMoreIcon/>}
                aria-controls="app-install-content"
                id="app-install-header"
            >
                <Typography className={"heading"}>App</Typography>
            </AccordionSummary>
            <AccordionDetails sx={{padding: "0px 16px 20px 16px"}}>
                <Typography variant="body1" sx={{mb: 2}}>
                    Open TorMap in its own window. It starts without network access, the relay data still needs a
                    connection.
                </Typography>
                <Button variant="outlined" startIcon={<InstallDesktopIcon/>} onClick={() => void promptInstall()}>
                    Install app
                </Button>
            </AccordionDetails>
        </Accordion>
    )
}
