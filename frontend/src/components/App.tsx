import {Close as CloseIcon} from "@mui/icons-material";
import {Box, Button, IconButton, useMediaQuery, useTheme} from "@mui/material";
import React, {FunctionComponent, Suspense, useEffect, useState} from 'react';

import {useDate} from "../context/date-context";
import {useSnackbar} from "../context/snackbar-context";
import {applyUpdate} from "../pwa/register";
import {usePwaState} from "../pwa/pwa-state";
import {SnackbarMessage} from "../types/ui";
import {useIsFullscreen} from "../util/fullscreen";
import {isOffline, offlineSnackbarKey, onceOnline} from "../util/online";
import {backend} from "../util/util";
import {FullscreenButton} from "./buttons/FullscreenButton";
import {InstallAppButton} from "./buttons/InstallAppButton";
import {AboutInformation} from "./dialogs/AboutInformation";
import {KeyboardShortcuts} from "./keyboard/KeyboardShortcuts";
import {LeafletWorldMap} from "./leaflet/LeafletWorldMap";
import {ExternalLink} from "./link/ExternalLink";
import {LoadingAnimation} from "./loading/LoadingAnimation";

// Lazy loaded components. The map is part of the entry (it is always needed), so its chunk is requested together with
// the entry script instead of after it. The overlay has its own Suspense boundary, so the map does not wait for it.
const OverlayLarge = React.lazy(() => import('./overlay/OverlayLarge'));
const OverlaySmall = React.lazy(() => import('./overlay/OverlaySmall'));

const updateSnackbarKey = "update"

export const App: FunctionComponent = () => {
    // Component state
    const [isLoading, setIsLoading] = useState(true)
    const [connectionRetryCount, setConnectionRetryCount] = useState<number>(0)

    // App context
    const {enqueueSnackbar, closeSnackbar} = useSnackbar();
    const {setAvailableDays} = useDate()
    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))
    const {waitingWorker} = usePwaState()
    const isFullscreen = useIsFullscreen()

    useEffect(() => {
        closeSnackbar()
        // The request may fail after this effect was cleaned up, then it must not wait for going online anymore
        let active = true
        let cancelOnlineRetry: (() => void) | undefined
        backend.get<string[]>('/relay/location/days').then(response => {
            setAvailableDays(response.data)
            setIsLoading(false)
        }).catch(() => {
            setIsLoading(false)
            if (isOffline()) {
                if (!active) return
                enqueueSnackbar(SnackbarMessage.Offline, {key: offlineSnackbarKey, persist: true})
                cancelOnlineRetry = onceOnline(() => {
                    setIsLoading(true)
                    setConnectionRetryCount(count => count + 1)
                })
                return
            }
            enqueueSnackbar(SnackbarMessage.ConnectionFailed, {
                variant: "error",
                action: <Button onClick={() => {
                    setIsLoading(true)
                    setConnectionRetryCount(connectionRetryCount + 1)
                }}>Retry</Button>,
                persist: true,
                preventDuplicate: false,
            })
        })
        return () => {
            active = false
            cancelOnlineRetry?.()
        }
    }, [connectionRetryCount, closeSnackbar, enqueueSnackbar, setAvailableDays])

    useEffect(() => {
        // The notice stays until the user reloads or dismisses it. A dismissed update activates once all TorMap tabs
        // are closed, so the notice only returns on the next visit if that has not happened yet.
        if (waitingWorker) {
            enqueueSnackbar(SnackbarMessage.UpdateAvailable, {
                key: updateSnackbarKey,
                persist: true,
                action: <>
                    <Button onClick={() => applyUpdate(waitingWorker)}>Reload</Button>
                    <IconButton
                        aria-label={"Dismiss"}
                        size={"small"}
                        color={"inherit"}
                        onClick={() => closeSnackbar(updateSnackbarKey)}
                    >
                        <CloseIcon fontSize={"small"}/>
                    </IconButton>
                </>,
            })
        }
    }, [waitingWorker, enqueueSnackbar, closeSnackbar])

    return (
        <>
            <React.StrictMode>
                {isLoading ? <LoadingAnimation/> : null}
            </React.StrictMode>
            <LeafletWorldMap
                setIsLoading={setIsLoading}
            />
            <React.StrictMode>
                {/* Full screen shows only the map. The overlays stay mounted, so they keep their state. */}
                <Box sx={{display: isFullscreen ? "none" : "contents"}}>
                    <Suspense fallback={null}>
                        {isLargeScreen ? <OverlayLarge/> : <OverlaySmall/>}
                    </Suspense>
                    <AboutInformation/>
                    <InstallAppButton/>
                </Box>
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
                <FullscreenButton/>
                <KeyboardShortcuts/>
            </React.StrictMode>
        </>
    )
}
