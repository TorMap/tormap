import React, {FunctionComponent, useCallback, useEffect, useState} from 'react';
import {WorldMap} from "./world-map";
import {Box, Button, Link} from "@mui/material";
import "@material-ui/styles";
import "../index.css";
import {Statistics} from "../types/app-state";
import {AboutInformation} from "./UI/UI-elements/about-information";
import {backend} from "../util/util";
import {useSnackbar} from "notistack";
import {SnackbarMessage} from "../types/ui";
import {useSettings} from "../util/SettingsContext";
import {UI} from "./UI/UI";
import {LoadingAnimation} from "./UI/UI-elements/LoadingAnimation";
import {useDate} from "../util/DateContext";

export const App: FunctionComponent = () => {
    //const [availableDays, setAvailableDays] = useState<string[]>([])
    //const [sliderValue, setSliderValue] = useState<number>(-1)
    const [isLoading, setIsLoading] = useState(true)
    const [statistics, setStatistics] = useState<Statistics>()
    const [connectionRetryCount, setConnectionRetryCount] = useState<number>(0)

    const settings = useSettings().settings
    const setSettings = useSettings().setSettings
    const {enqueueSnackbar, closeSnackbar} = useSnackbar();

    const selectedDate = useDate().selectedDate
    const availableDays = useDate().availableDays
    const setAvailableDays = useDate().setAvailableDays
    const sliderValue = useDate().sliderValue
    const setSliderValue = useDate().setSliderValue

    useEffect(() => {console.log(selectedDate)},[selectedDate])

    // todo Move to Date context?
    // Loads available days from the backend
    useEffect(() => {
        closeSnackbar()
        setIsLoading(true)
        backend.get<string[]>('/archive/geo/relay/days').then(response => {
            setAvailableDays(response.data)
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
    }, [connectionRetryCount, closeSnackbar, enqueueSnackbar, setAvailableDays])

    // todo Move to Settings context?
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
            {isLoading ? <LoadingAnimation /> : null}
            <WorldMap
                setIsLoading={useCallback(setIsLoading, [setIsLoading])}
                setStatistics={useCallback(setStatistics, [setStatistics])}
            />
            <UI
                availableDays={availableDays}
                sliderValue={sliderValue}
                setSliderValue={setSliderValue}
                statistics={statistics}
            />
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