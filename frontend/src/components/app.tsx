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
import {UI} from "./UI/UI";
import {LoadingAnimation} from "./UI/UI-elements/LoadingAnimation";
import {useDate} from "../util/DateContext";

export const App: FunctionComponent = () => {
    const [isLoading, setIsLoading] = useState(true)
    const [statistics, setStatistics] = useState<Statistics>()
    const [connectionRetryCount, setConnectionRetryCount] = useState<number>(0)

    const {enqueueSnackbar, closeSnackbar} = useSnackbar();

    const date = useDate()
    const selectedDate = date.selectedDate
    const setAvailableDays = date.setAvailableDays

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

    return (
        <div>
            {isLoading ? <LoadingAnimation /> : null}
            <WorldMap
                setIsLoading={useCallback(setIsLoading, [setIsLoading])}
                setStatistics={useCallback(setStatistics, [setStatistics])}
            />
            <UI statistics={statistics} />
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