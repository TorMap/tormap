import React, {FunctionComponent, useCallback, useEffect, useState} from 'react';
import {LeafletWorldMap} from "./leaflet/LeafletWorldMap";
import {Box, Button, Link, useMediaQuery, useTheme} from "@mui/material";
import "@mui/styles";
import "../index.css";
import {AboutInformation} from "./dialogs/AboutInformation";
import {backend} from "../util/util";
import {useSnackbar} from "notistack";
import {SnackbarMessage} from "../types/ui";
import {LoadingAnimation} from "./loading/LoadingAnimation";
import {useDate} from "../context/date-context";
import {OverlayLarge} from "./overlay/OverlayLarge";
import {OverlaySmall} from "./overlay/OverlaySmall";

export const App: FunctionComponent = () => {
    // Component state
    const [isLoading, setIsLoading] = useState(true)
    const [connectionRetryCount, setConnectionRetryCount] = useState<number>(0)

    // App context
    const {enqueueSnackbar, closeSnackbar} = useSnackbar();
    const {setAvailableDays} = useDate()
    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

    useEffect(() => {
        closeSnackbar()
        setIsLoading(true)
        backend.get<string[]>('/relay/location/days').then(response => {
            setAvailableDays(response.data)
            setIsLoading(false)
        }).catch(() => {
            enqueueSnackbar(SnackbarMessage.ConnectionFailed, {
                variant: "error",
                action: <Button onClick={() => setConnectionRetryCount(connectionRetryCount + 1)}>Retry</Button>,
                persist: true,
                preventDuplicate: false,
            })
            setIsLoading(false)
        })
    }, [connectionRetryCount, closeSnackbar, enqueueSnackbar, setAvailableDays])

    return (
        <>
            {isLoading ? <LoadingAnimation/> : null}
            <LeafletWorldMap
                setIsLoading={useCallback(setIsLoading, [setIsLoading])}
            />
            {isLargeScreen ? <OverlayLarge /> : <OverlaySmall />}
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
        </>
    )
}