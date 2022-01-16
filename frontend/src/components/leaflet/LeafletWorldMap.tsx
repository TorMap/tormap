import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useCallback, useEffect, useState} from "react";
import 'leaflet/dist/leaflet.css';
import "leaflet.heat"
import {RelayLocationDto} from "../../dto/relay";
import {SnackbarMessage} from "../../types/ui";
import {backend} from "../../util/util";
import {useSnackbar} from "notistack";
import {useDate} from "../../context/date-context";
import {LeafletLayers} from "./LeafletLayers";


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

    /**
     * Query all relays for the selected date whenever a new date is selected
     */
    useEffect(() => {
        if (selectedDate) {
            let currentTimeStamp = Date.now()
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
            center={[15, 0]}
            minZoom={2}
            zoom={3}
            scrollWheelZoom={true}
            zoomSnap={0.5}
            zoomDelta={0.5}
            wheelPxPerZoomLevel={200}
            preferCanvas={true}
            attributionControl={false}
            maxBounds={[[-180, -360], [180, 360]]}
        >
            <LeafletLayers
                relays={relays}
                reloadSelectedDay={useCallback(() => setRefreshDayCount(prev => prev + 1), [])}
            />
            <TileLayer
                maxZoom={19}
                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                noWrap={true}
            />
        </MapContainer>
    );
};
