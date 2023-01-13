import {useMediaQuery, useTheme} from "@mui/material";
import {useSnackbar} from "notistack";
import React, {FunctionComponent, useCallback, useEffect, useState} from "react";
import {MapContainer, TileLayer} from "react-leaflet";

import {useDate} from "../../context/date-context";
import {RelayLocationDto} from "../../dto/relay";
import {SnackbarMessage} from "../../types/ui";
import {backend} from "../../util/util";
import {LeafletLayers} from "./LeafletLayers";

import "leaflet.heat"
import 'leaflet/dist/leaflet.css';

interface Props {
    /**
     * A variable callback whether the map is currently fetching a new date
     * @param b whether the map is currently fetching a new date
     */
    setIsLoading: (b: boolean) => void
}

/*
Variable needs to be outside component to keep track of the last selected date
This prevents the case that multiple dates get loaded and the last received date is displayed.
Instead, only the last selected date will be drawn.
 */
let latestRequestTimestamp: number | undefined = undefined

export const LeafletWorldMap: FunctionComponent<Props> = ({setIsLoading}) => {
    // Component state
    const [relays, setRelays] = useState<RelayLocationDto[]>()
    const [refreshDayCount, setRefreshDayCount] = useState<number>(0)

    // App context
    const {enqueueSnackbar} = useSnackbar();
    const {selectedDate} = useDate()
    const theme = useTheme()
    const isAtLeastMediumScreen = useMediaQuery(theme.breakpoints.up("md"))

    /**
     * Query all relays for the selected date whenever a new date is selected
     */
    useEffect(() => {
        if (selectedDate) {
            const currentTimeStamp = Date.now()
            setIsLoading(true)
            backend.get<RelayLocationDto[]>(`/relay/location/day/${selectedDate}`).then(response => {
                setIsLoading(false)
                if (currentTimeStamp === latestRequestTimestamp) {
                    setRelays(response.data)
                }
            }).catch(() => {
                setIsLoading(false)
                enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
            })
            latestRequestTimestamp = currentTimeStamp
        }
    }, [selectedDate, enqueueSnackbar, setIsLoading, refreshDayCount])

    return (
        <MapContainer
            style={{
                width: "100vw",
                height: "100vh",
                backgroundColor: "#262626",
                position: "fixed",
            }}
            center={[30, 0]}
            zoom={3}
            minZoom={2}
            maxZoom={18}
            scrollWheelZoom={true}
            zoomSnap={1}
            zoomDelta={1}
            wheelPxPerZoomLevel={128}
            preferCanvas={true}
            attributionControl={false}
            maxBounds={[[-180, -360], [180, 360]]}
            tap={isAtLeastMediumScreen ? false : undefined} // fixes macOS/Safari bug for Leaflet v1.7.1
        >
            <React.StrictMode>
                <LeafletLayers
                    relays={relays}
                    reloadSelectedDay={useCallback(() => setRefreshDayCount(prev => prev + 1), [])}
                />
                <TileLayer
                    maxZoom={19}
                    url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                    noWrap={true}
                />
            </React.StrictMode>
        </MapContainer>
    );
};

export default LeafletWorldMap
