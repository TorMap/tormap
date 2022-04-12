import React, {FunctionComponent, Suspense, useCallback, useEffect, useState} from 'react';
import {Box, Button, useMediaQuery, useTheme} from "@mui/material";
import {AboutInformation} from "./dialogs/AboutInformation";
import {backend} from "../util/util";
import {useSnackbar} from "notistack";
import {SnackbarMessage} from "../types/ui";
import {LoadingAnimation} from "./loading/LoadingAnimation";
import {useDate} from "../context/date-context";

// Lazy loaded components
const ExternalLink = React.lazy(() => import('./link/ExternalLink'));
const OverlayLarge = React.lazy(() => import('./overlay/OverlayLarge'));
const OverlaySmall = React.lazy(() => import('./overlay/OverlaySmall'));
const LeafletWorldMap = React.lazy(() => import('./leaflet/LeafletWorldMap'));

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
            <Suspense fallback={<LoadingAnimation/>}>
                <React.StrictMode>
                    {isLoading ? <LoadingAnimation/> : null}
                </React.StrictMode>
                <LeafletWorldMap
                    setIsLoading={useCallback(setIsLoading, [setIsLoading])}
                />
                <React.StrictMode>
                    {isLargeScreen ? <OverlayLarge/> : <OverlaySmall/>}
                    <Box sx={{
                        color: "#b4b4b4",
                        background: "#262626",
                        position: "fixed",
                        right: "2px",
                        bottom: "2px",
                        fontSize: ".7rem",
                    }}>
                    <span>
                        <ExternalLink href="https://leafletjs.com" label={"Leaflet"}/>
                        {" | © "}
                        <ExternalLink href="https://www.openstreetmap.org/copyright" label={"OpenStreetMap"}/>
                        {" contributors © "}
                        <ExternalLink href="https://carto.com/attributions" label={"CARTO"}/>
                    </span>
                    </Box>
                    <AboutInformation/>
                </React.StrictMode>
            </Suspense>
        </>
    )
}