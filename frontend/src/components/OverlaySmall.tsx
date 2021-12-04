import React, {FunctionComponent, useState} from "react";
import {Transition, OverlayProps} from "../types/ui";
import {AppBar, Box, Button, Dialog, DialogActions, Fab, IconButton, Toolbar, Typography} from "@mui/material";
import SettingsIcon from '@mui/icons-material/Settings';
import CloseIcon from "@mui/icons-material/Close";
import {AppSettings} from "./accordion/AppSettings";
import {MapStats} from "./accordion/MapStats";
import {DatePickerSmall} from "./date/DatePickerSmall";
import MapIcon from '@mui/icons-material/Map';

/**
 * A component wrapping all UI elements for devices with small screen sizes
 *
 * @param statistics - a Statistics object for data to display
 */
export const OverlaySmall: FunctionComponent<OverlayProps> = ({statistics}) => {
    const [open, setOpen] = useState(false);

    return (
        <Box>
            <Fab
                size={"large"}
                color={"primary"}
                aria-label={"more settings"}
                sx={{position: "fixed", right: 20, bottom: 20}}
                onClick={() => setOpen(true)}
            >
                <SettingsIcon />
            </Fab>
            <Dialog
                open={open}
                fullScreen
                onClose={() => setOpen(false)}
                TransitionComponent={Transition}
            >
                <AppBar sx={{ position: 'relative' }}>
                    <Toolbar>
                        <IconButton
                            edge="start"
                            color="inherit"
                            onClick={() => setOpen(false)}
                            aria-label="close"
                        >
                            <CloseIcon />
                        </IconButton>
                        <Typography sx={{ ml: 2, flex: 1 }} variant="h6" component="div">
                            Settings
                        </Typography>
                    </Toolbar>
                </AppBar>
                <DatePickerSmall />
                <Box sx={{padding: 1}}>
                    <AppSettings elevation={0}/>
                    <Box height={"10px"}/>
                    {statistics && <MapStats stats={statistics} elevation={0} defaultExpanded={false}/>}
                </Box>
                <DialogActions sx={{
                    position: "fixed",
                    bottom: 5,
                    right: 5,
                }}>
                    <Button
                        autoFocus
                        onClick={() => setOpen(false)}
                        variant={"contained"}
                        size={"large"}
                        endIcon={<MapIcon />}
                    >
                        apply
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    )
}