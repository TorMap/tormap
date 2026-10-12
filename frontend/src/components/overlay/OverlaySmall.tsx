import {Close as CloseIcon, Settings as SettingsIcon} from "@mui/icons-material";
import {AppBar, Box, Button, Dialog, DialogActions, Fab, IconButton, Toolbar, Typography} from "@mui/material";
import {useAtomValue} from "jotai";
import {FunctionComponent, useState} from "react";

import {SlideUpTransition} from "../../types/ui";
import {AppSettings} from "../accordion/AppSettings";
import {MapStats} from "../accordion/MapStats";
import {ResponsiveDatePicker} from "../date/ResponsiveDatePicker";
import {relaySearchOnMapAtom} from "../dialogs/relay/relay-details-atoms";

/**
 * A component wrapping all UI elements for devices with small screen sizes
 */
export const OverlaySmall: FunctionComponent = () => {
    // Component state
    const [open, setOpen] = useState(false);

    // Close the settings as soon as search results are shown on the map, otherwise they would cover the map
    const relaySearchOnMap = useAtomValue(relaySearchOnMapAtom)
    const [syncedRelaySearchOnMap, setSyncedRelaySearchOnMap] = useState(relaySearchOnMap)
    if (syncedRelaySearchOnMap !== relaySearchOnMap) {
        setSyncedRelaySearchOnMap(relaySearchOnMap)
        if (relaySearchOnMap) setOpen(false)
    }

    return (
        <Box>
            <Fab
                size={"large"}
                color={"primary"}
                aria-label={"more settings"}
                sx={{position: "fixed", right: 20, bottom: 20}}
                onClick={() => setOpen(true)}
            >
                <SettingsIcon/>
            </Fab>
            <Dialog
                open={open}
                fullScreen
                slotProps={{paper: {"aria-label": "Settings"}}}
                onClose={() => setOpen(false)}
                slots={{transition: SlideUpTransition}}
            >
                <AppBar sx={{position: 'relative'}}>
                    <Toolbar>
                        <Typography variant="h6">
                            Settings
                        </Typography>
                        <IconButton aria-label="close" sx={{
                            position: "absolute",
                            right: "10px",
                            top: "10px",
                        }} onClick={() => setOpen(false)}>
                            <CloseIcon/>
                        </IconButton>
                    </Toolbar>
                </AppBar>
                <ResponsiveDatePicker largeScreen={false} />
                <Box sx={{padding: 1}}>
                    <AppSettings elevation={0}/>
                    <Box sx={{height: "10px"}}/>
                    <MapStats elevation={0} defaultExpanded={false}/>
                </Box>
                <DialogActions sx={{
                    position: "fixed",
                    bottom: 5,
                    right: 5,
                }}>
                    <Button
                        onClick={() => setOpen(false)}
                        variant={"contained"}
                        size={"large"}
                    >
                        Back
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    )
}

export default OverlaySmall
