import React, {FunctionComponent, useState} from "react";
import {Transition, UIProps} from "../../types/ui";
import {AppBar, Box, Button, Dialog, Fab, IconButton, Toolbar, Typography} from "@mui/material";
import SettingsIcon from '@mui/icons-material/Settings';
import CloseIcon from "@material-ui/icons/Close";
import {AppSettings} from "./UI-elements/app-settings";
import {MapStats} from "./UI-elements/map-stats";
import {TorUsageDatePickerMobile} from "./UI-elements/date-picker-mobile";

/**
 * A component wrapping all UI elements for Mobile devices
 *
 * @param statistics - a Statistics object for data to display
 */
export const MobileUI: FunctionComponent<UIProps> = ({statistics}) => {
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
                        <Button autoFocus color="inherit" onClick={() => setOpen(false)}>
                            apply
                        </Button>
                    </Toolbar>
                </AppBar>
                <TorUsageDatePickerMobile />
                <AppSettings elevation={0}/>
                <Box height={"10px"}/>
                {statistics && <MapStats stats={statistics} elevation={0} defaultExpanded={false}/>}
            </Dialog>
        </Box>
    )
}