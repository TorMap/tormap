import React, {useEffect, useState} from 'react';
import {WorldMap} from "./components/world-map";
import {
    createMuiTheme, CircularProgress, ThemeProvider, makeStyles, Snackbar
} from "@material-ui/core";
import "@material-ui/styles";
import "./index.scss";
import {AppSettings} from "./components/app-settings";
import {Settings, snackbarMessage, Statistics} from "./types/variousTypes";
import {MapStats} from "./components/map-stats";
import {DateSlider} from "./components/date-slider";
import MuiAlert from '@material-ui/lab/Alert';
import {apiBaseUrl, defaultSettings} from "./util/Config";


/**
 * Styles according to Material UI doc for components used in the App component
 */
const useStyle = makeStyles(() => ({
    progressCircle: {
        position: "fixed",
        left: "calc(50% - 25px)",
        top: "calc(50% - 25px)",
        margin: "auto",
        backgroundColor: "transparent",
        color: "rgba(255,255,255,.6)",
        zIndex: 1000,
    },
    attribution: {
        color: "#b4b4b4",
        background: "#262626",
        position: "fixed",
        right: "0px",
        bottom: "0px",
        fontSize: ".7rem",
        "a, a:link, a:visited, a:hover, a:active, a:focus, &:focus, &:hover": {
            color: "#3a68a4",
        },
    },
}))

function App() {
    const [availableDays, setAvailableDays] = useState<string[]>([])
    const [sliderValue, setSliderValue] = useState<number>(-1)
    const [isLoading, setIsLoading] = useState(true)
    const [snackbar, setSnackbar] = useState(false)
    const [snackbarMessage, setSnackbarMessage] = useState<snackbarMessage>({message: "",severity: "info"})
    const [settings, setSettings] = useState<Settings>(defaultSettings)
    const [statistics, setStatistics] = useState<Statistics | undefined>(undefined)
    const classes = useStyle()

    const [theme] = useState(createMuiTheme({
        palette: {
            type: "dark",
        },
    }))

    /**
     * input event handler for setting changes
     * @param event
     */
    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setSettings({...settings, [event.target.name]: event.target.checked})
    };

    // Loads available days from the backend
    useEffect(() => {
        setIsLoading(true)
        fetch(`${apiBaseUrl}/archive/geo/relay/days`)
            .then(response => response.json())
            .then(availableDays => {
                setAvailableDays(availableDays)
                setSliderValue(availableDays.length - 1)
                setIsLoading(false)
            })
            .catch(reason => {
                handleSnackbar({message: `${reason.message}`, severity: "error"})
            })
    }, [])

    // Resets selection if grouping gets disabled
    useEffect(() => {
        if (!settings.sortCountry && settings.selectedCountry){
            setSettings({...settings, selectedCountry: undefined})
        }
        if (!settings.sortFamily && settings.selectedFamily){
            setSettings({...settings, selectedFamily: undefined})
        }
    }, [settings])

    const handleCloseSnackbar = (event?: React.SyntheticEvent, reason?: string) => {
        if (reason === 'clickaway') {
            return;
        }
        setSnackbar(false)
    }
    const handleSnackbar = (snackbarMessage: snackbarMessage) => {
        setSnackbarMessage(snackbarMessage)
        setSnackbar(true)
    }

    return (
        <ThemeProvider theme={theme}>
            <div>
                {isLoading ?
                    <div className={classes.progressCircle}>
                        <CircularProgress color={"inherit"}/>
                    </div>
                    : null
                }
                <WorldMap
                    dayToDisplay={sliderValue >= 0 ? availableDays[sliderValue] : undefined}
                    settings={settings}
                    setSettingsCallback={setSettings}
                    setLoadingStateCallback={setIsLoading}
                    setStatisticsCallback={setStatistics}
                    handleSnackbar={handleSnackbar}
                />
                <DateSlider availableDays={availableDays} setValue={setSliderValue} settings={settings}/>
                <AppSettings settings={settings} onChange={handleInputChange}/>
                {statistics ? <MapStats settings={settings} stats={statistics}/> : null}
            </div>
            <Snackbar
                open={snackbar}
                autoHideDuration={snackbarMessage.severity === "error" ? null : 6000}
                onClose={handleCloseSnackbar}
                anchorOrigin={{vertical: "top", horizontal: "center"}}
            >
                <MuiAlert elevation={6} variant="filled" onClose={handleCloseSnackbar} severity={snackbarMessage.severity}>
                    {snackbarMessage.message}
                </MuiAlert>
            </Snackbar>
            <span className={classes.attribution}>
                <a href="https://leafletjs.com">Leaflet</a> | &copy;&nbsp;
                <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>&nbsp;
                contributors &copy; <a href="https://carto.com/attributions">CARTO</a>
            </span>
        </ThemeProvider>
    )
}

export default App;
