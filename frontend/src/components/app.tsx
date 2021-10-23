import React, {FunctionComponent, useCallback, useEffect, useState} from 'react';
import {WorldMap} from "./world-map";
import {Button, CircularProgress, Link, makeStyles} from "@material-ui/core";
import "@material-ui/styles";
import "../index.css";
import {AppSettings, relaysMustIncludeFlagInput, showRelayTypesInput} from "./app-settings";
import {Settings, Statistics} from "../types/app-state";
import {MapStats} from "./map-stats";
import {DateSlider} from "./date-slider";
import {defaultSettings} from "../util/config";
import {AboutInformation} from "./about-information";
import {backend} from "../util/util";
import {useSnackbar} from "notistack";
import {SnackbarMessage} from "../types/ui";


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

export const App: FunctionComponent = () => {
    const [availableDays, setAvailableDays] = useState<string[]>([])
    const [sliderValue, setSliderValue] = useState<number>(-1)
    const [isLoading, setIsLoading] = useState(true)
    const [settings, setSettings] = useState<Settings>(defaultSettings)
    const [statistics, setStatistics] = useState<Statistics>()
    const [connectionRetryCount, setConnectionRetryCount] = useState<number>(0)

    const classes = useStyle()
    const {enqueueSnackbar, closeSnackbar} = useSnackbar();

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
        closeSnackbar()
        setIsLoading(true)
        backend.get<string[]>('/archive/geo/relay/days').then(response => {
            setAvailableDays(response.data)
            setSliderValue(response.data.length - 1)
            setIsLoading(false)
        }).catch(() => {
            enqueueSnackbar(SnackbarMessage.ConnectionFailed, {
                variant: "error",
                action: <Button onClick={() => setConnectionRetryCount(previous => previous + 1)}>Retry</Button>,
                persist: true,
                preventDuplicate: false,
            })
            setIsLoading(false)
        })
    }, [connectionRetryCount, closeSnackbar, enqueueSnackbar])

    // Resets selection if grouping gets disabled
    useEffect(() => {
        if (!settings.sortCountry && settings.selectedCountry) {
            setSettings({...settings, selectedCountry: undefined})
        }
        if (!settings.sortFamily && settings.selectedFamily) {
            setSettings({...settings, selectedFamily: undefined})
        }
    }, [settings])

    return (
        <div>
            {isLoading &&
            <div className={classes.progressCircle}>
                <CircularProgress color={"inherit"}/>
            </div>
            }
            <WorldMap
                dayToDisplay={sliderValue >= 0 ? availableDays[sliderValue] : undefined}
                settings={settings}
                setSettings={useCallback(setSettings, [setSettings])}
                setIsLoading={useCallback(setIsLoading, [setIsLoading])}
                setStatistics={useCallback(setStatistics, [setStatistics])}
            />
            <DateSlider availableDays={availableDays} setValue={useCallback(setSliderValue, [setSliderValue])}/>
            <AppSettings settings={settings} onChange={handleInputChange}/>
            {statistics && <MapStats settings={settings} stats={statistics}/>}
            <span className={classes.attribution}>
                    <Link href="https://leafletjs.com" target={"_blank"}>Leaflet</Link> | &copy;&nbsp;
                <Link href="https://www.openstreetmap.org/copyright" target={"_blank"}>OpenStreetMap</Link>&nbsp;
                contributors &copy; <Link href="https://carto.com/attributions" target={"_blank"}>CARTO</Link>
                </span>
            <AboutInformation/>
        </div>
    )
}