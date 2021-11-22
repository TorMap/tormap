import React, {FunctionComponent, useCallback, useEffect, useState} from 'react';
import {WorldMap} from "./world-map";
import {Box, Button, CircularProgress, Link} from "@mui/material";
import "@material-ui/styles";
import "../index.css";
import {AppSettings} from "./app-settings";
import {Statistics} from "../types/app-state";
import {MapStats} from "./map-stats";
import {DateSlider} from "./date-slider";
import {AboutInformation} from "./about-information";
import {backend} from "../util/util";
import {useSnackbar} from "notistack";
import {SnackbarMessage} from "../types/ui";
import {useSettings} from "../util/SettingsContext";

export const App: FunctionComponent = () => {
    const [availableDays, setAvailableDays] = useState<string[]>([])
    const [sliderValue, setSliderValue] = useState<number>(-1)
    const [isLoading, setIsLoading] = useState(true)
    const [statistics, setStatistics] = useState<Statistics>()
    const [connectionRetryCount, setConnectionRetryCount] = useState<number>(0)

    const settings = useSettings().settings
    const setSettings = useSettings().setSettings


    const {enqueueSnackbar, closeSnackbar} = useSnackbar();

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
    }, [settings, setSettings])

    return (
        <div>
            {isLoading &&
                <CircularProgress
                    color={"inherit"}
                    sx={{
                    position: "fixed",
                    left: "calc(50% - 25px)",
                    top: "calc(50% - 25px)",
                    margin: "auto",
                    backgroundColor: "transparent",
                    color: "rgba(255,255,255,.6)",
                    zIndex: 1000,
                }}/>
            }
            <WorldMap
                dayToDisplay={sliderValue >= 0 ? availableDays[sliderValue] : undefined}
                setIsLoading={useCallback(setIsLoading, [setIsLoading])}
                setStatistics={useCallback(setStatistics, [setStatistics])}
            />
            <DateSlider availableDays={availableDays} setValue={useCallback(setSliderValue, [setSliderValue])}/>
            <AppSettings/>
            {statistics && <MapStats stats={statistics}/>}
            <Box sx={{
                color: "#b4b4b4",
                background: "#262626",
                position: "fixed",
                right: "0px",
                bottom: "0px",
                fontSize: ".7rem",
            }}>
                <span>
                    <Link href="https://leafletjs.com" target={"_blank"}>Leaflet</Link> | &copy;&nbsp;
                    <Link href="https://www.openstreetmap.org/copyright" target={"_blank"}>OpenStreetMap</Link>&nbsp;
                    contributors &copy; <Link href="https://carto.com/attributions" target={"_blank"}>CARTO</Link>
                </span>
            </Box>

            <AboutInformation/>
        </div>
    )
}