import React, {FunctionComponent, useCallback, useEffect, useState} from 'react';
import {WorldMap} from "./WorldMap";
import {Box, Button, Link} from "@mui/material";
import "@mui/styles";
import "../index.css";
import {Statistics} from "../types/app-state";
import {AboutInformation} from "./dialogs/AboutInformation";
import {backend} from "../util/util";
import {useSnackbar} from "notistack";
import {SnackbarMessage} from "../types/ui";
import {Overlay} from "./Overlay";
import {LoadingAnimation} from "./loading/LoadingAnimation";
import {useDate} from "../util/date-context";

export const App: FunctionComponent = () => {
    const [isLoading, setIsLoading] = useState(true)
    const [statistics, setStatistics] = useState<Statistics>()
    const [connectionRetryCount, setConnectionRetryCount] = useState<number>(0)

    const {enqueueSnackbar, closeSnackbar} = useSnackbar();

    const date = useDate()
    const setAvailableDays = date.setAvailableDays

    useEffect(() => {
        closeSnackbar()
        setIsLoading(true)
        backend.get<string[]>('/relay/location/days').then(response => {
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
            {isLoading ? <LoadingAnimation/> : null}
            <WorldMap
                setIsLoading={useCallback(setIsLoading, [setIsLoading])}
                setStatistics={useCallback(setStatistics, [setStatistics])}
            />
            <Overlay statistics={statistics}/>
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