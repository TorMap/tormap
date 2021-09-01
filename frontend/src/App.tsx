import React, {useEffect, useState} from 'react';
import {WorldMap} from "./components/world-map";
import {CircularProgress, createMuiTheme, Link, makeStyles, Snackbar, ThemeProvider} from "@material-ui/core";
import "@material-ui/styles";
import "./index.scss";
import {AppSettings, relaysMustIncludeFlagInput, showRelayTypesInput} from "./components/app-settings";
import {Settings, Statistics} from "./types/app-state";
import {MapStats} from "./components/map-stats";
import {DateSlider} from "./components/date-slider";
import MuiAlert from '@material-ui/lab/Alert';
import {apiBaseUrl, defaultSettings} from "./util/config";
import {AboutInformation} from "./components/about-information";
import {ErrorMessages, SnackbarMessage} from "./types/ui";


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
    },
}))

function App() {
    const [availableDays, setAvailableDays] = useState<string[]>([])
    const [sliderValue, setSliderValue] = useState<number>(-1)
    const [isLoading, setIsLoading] = useState(true)
    const [showSnackbar, setShowSnackbar] = useState(false)
    const [snackbarMessage, setSnackbarMessage] = useState<SnackbarMessage>({message: "", severity: "info"})
    const [settings, setSettings] = useState<Settings>(defaultSettings)
    const [statistics, setStatistics] = useState<Statistics | undefined>(undefined)
    const classes = useStyle()

    const [theme] = useState(createMuiTheme({
        palette: {
            type: "dark",
        },
        overrides: {
            MuiTooltip: {
                tooltip: {
                    fontSize: ".85em",
                }
            },
            MuiLink: {
                root: {
                    color: "rgba(255, 255, 255, 0.7)",
                    fontSize: ".9em"
                }
            }
        }
    }))

    /**
     * input event handler for setting changes
     * @param event
     */
    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        switch (event.target.name) {
            case showRelayTypesInput:
                setSettings({
                    ...settings,
                    showRelayTypes: {...settings.showRelayTypes, [event.target.id]: event.target.checked}
                })
                break;
            case relaysMustIncludeFlagInput:
                setSettings({
                    ...settings,
                    relaysMustIncludeFlag: {...settings.relaysMustIncludeFlag, [event.target.id]: event.target.checked}
                })
                break;
            default:
                setSettings({...settings, [event.target.name]: event.target.checked})
        }
    };

    // Loads available days from the backend
    useEffect(() => {
        setIsLoading(true)
        fetch(`${apiBaseUrl}/archive/geo/relay/days`)
            .then(response => response.json())
            .then((availableDays: string[]) => {
                setAvailableDays(availableDays)
                setSliderValue(availableDays.length - 1)
                setIsLoading(false)
            })
            .catch(() => {
                showSnackbarMessage({message: `${ErrorMessages.ConectionToBackendFailed}`, severity: "error"})
                setIsLoading(false)
            })
    }, [])

    // Resets selection if grouping gets disabled
    useEffect(() => {
        if (!settings.sortCountry && settings.selectedCountry) {
            setSettings({...settings, selectedCountry: undefined})
        }
        if (!settings.sortFamily && settings.selectedFamily) {
            setSettings({...settings, selectedFamily: undefined})
        }
    }, [settings])

    const handleCloseSnackbar = (event?: React.SyntheticEvent, reason?: string) => {
        if (reason === 'clickaway') {
            return;
        }
        setShowSnackbar(false)
    }

    const showSnackbarMessage = (message: SnackbarMessage) => {
        setSnackbarMessage(message)
        setShowSnackbar(true)
    }

    return (
        <ThemeProvider theme={theme}>
            <div>
                {isLoading &&
                <div className={classes.progressCircle}>
                    <CircularProgress color={"inherit"}/>
                </div>
                }
                <WorldMap
                    dayToDisplay={sliderValue >= 0 ? availableDays[sliderValue] : undefined}
                    settings={settings}
                    setSettingsCallback={setSettings}
                    setLoadingStateCallback={setIsLoading}
                    setStatisticsCallback={setStatistics}
                    showSnackbarMessage={showSnackbarMessage}
                    closeSnackbar={() => setShowSnackbar(false)}
                />
                <DateSlider availableDays={availableDays} setValue={setSliderValue}/>
                <AppSettings settings={settings} onChange={handleInputChange}/>
                {statistics && <MapStats settings={settings} stats={statistics}/>}
            </div>
            <Snackbar
                open={showSnackbar}
                autoHideDuration={snackbarMessage.severity === "error" ? null : 6000}
                onClose={handleCloseSnackbar}
                anchorOrigin={{vertical: "top", horizontal: "center"}}
            >
                <MuiAlert
                    elevation={6}
                    variant="filled"
                    onClose={handleCloseSnackbar}
                    severity={snackbarMessage.severity}
                >
                    {snackbarMessage.message}
                </MuiAlert>
            </Snackbar>
            <span className={classes.attribution}>
                <Link href="https://leafletjs.com" target={"_blank"}>Leaflet</Link> | &copy;&nbsp;
                <Link href="https://www.openstreetmap.org/copyright" target={"_blank"}>OpenStreetMap</Link>&nbsp;
                contributors &copy; <Link href="https://carto.com/attributions" target={"_blank"}>CARTO</Link>
            </span>
            <AboutInformation/>
        </ThemeProvider>
    )
}

export default App;
