import React, {FunctionComponent, useState} from "react";
import {Transition, UIProps} from "../../types/ui";
import {AppBar, Box, Button, Dialog, Fab, IconButton, TextField, Toolbar, Typography} from "@mui/material";
import SettingsIcon from '@mui/icons-material/Settings';
import CloseIcon from "@material-ui/icons/Close";
import {AppSettings} from "./UI-elements/app-settings";
import {MapStats} from "./UI-elements/map-stats";
import AdapterDateFns from "@mui/lab/AdapterDateFns";
import {enCA} from "date-fns/locale";
import {DatePicker, LocalizationProvider} from "@mui/lab";
import dateFormat from "dateformat";
import moment from "moment";

/**
 * A component wrapping all UI elements for Mobile devices
 *
 * @param availableDays - Days available for display
 * @param sliderValue - the current slider value
 * @param setSliderValue - a callback for setting the slider value
 * @param statistics - a Statistics object for data to display
 */
export const MobileUI: FunctionComponent<UIProps> = ({
                                                         availableDays,
                                                         sliderValue,
                                                         setSliderValue,
                                                         statistics
}) => {
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
                <LocalizationProvider dateAdapter={AdapterDateFns} locale={enCA}>
                    <DatePicker
                        value={sliderValue ? availableDays[sliderValue] : undefined}
                        renderInput={(params) =>
                            <TextField variant={"standard"}
                                       {...params}
                                       sx={{
                                           padding: 2
                                       }}
                                       helperText={"A day in the life of the Tor Network"}

                            />
                        }
                        onChange={(date) => {
                            const day: string = dateFormat(date!!, "yyyy-mm-dd")
                            if (availableDays.includes(day)) {
                                setSliderValue(availableDays.findIndex(element => element === day))
                            }
                        }}
                        onError={() => console.log("error")}
                        minDate={new Date(availableDays[0])}
                        maxDate={new Date(availableDays[availableDays.length-1])}
                        shouldDisableDate={date => {
                            return !(availableDays.includes(moment(date).format("YYYY-MM-DD")))
                        }}
                        views={["year","month","day"]}
                    />
                </LocalizationProvider>
                <AppSettings elevation={0}/>
                <Box height={"10px"}/>
                {statistics && <MapStats stats={statistics} elevation={0} defaultExpanded={false}/>}
            </Dialog>
        </Box>
    )
}