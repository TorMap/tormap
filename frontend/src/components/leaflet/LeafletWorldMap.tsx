import React, {FunctionComponent, useCallback, useEffect, useState} from "react";

import {useDate} from "../../context/date-context";
import {useSnackbar} from "../../context/snackbar-context";
import {RelayLocationDto} from "../../dto/relay";
import {SnackbarMessage} from "../../types/ui";
import {readPermalink, writePermalink} from "../../util/permalink";
import {backend} from "../../util/util";
import {CountryUsageLayer} from "./CountryUsageLayer";
import {LeafletLayers} from "./LeafletLayers";
import {MapContainer, TileLayer, useMapEvents} from "./map";

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

const defaultCenter: [number, number] = [30, 0]
const defaultZoom = 3

/**
 * Keeps the map position and zoom in the URL, so the current view can be shared
 */
const MapPermalink: FunctionComponent = () => {
    const map = useMapEvents({
        moveend: () => {
            const center = map.getCenter()
            writePermalink({lat: center.lat, lng: center.lng, zoom: map.getZoom()})
        }
    })
    return null
}

export const LeafletWorldMap: FunctionComponent<Props> = ({setIsLoading}) => {
    // Component state
    const [relays, setRelays] = useState<RelayLocationDto[]>()
    const [refreshDayCount, setRefreshDayCount] = useState<number>(0)

    // App context
    const {enqueueSnackbar} = useSnackbar();
    const {selectedDate} = useDate()

    // The initial view is read once from the URL
    const [initialView] = useState(() => {
        const permalink = readPermalink()
        const center: [number, number] = permalink.lat !== undefined && permalink.lng !== undefined
            ? [permalink.lat, permalink.lng]
            : defaultCenter
        return {center, zoom: permalink.zoom ?? defaultZoom}
    })

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
                // Matches the water of the CARTO dark_all tiles, so the area around the tiles looks the same
                backgroundColor: "#2e353b",
                position: "fixed",
            }}
            center={initialView.center}
            zoom={initialView.zoom}
            minZoom={2}
            maxZoom={18}
            scrollWheelZoom={true}
            zoomSnap={1}
            zoomDelta={1}
            wheelPxPerZoomLevel={128}
            preferCanvas={true}
            attributionControl={false}
            maxBounds={[[-180, -360], [180, 360]]}
        >
            <MapPermalink/>
            <CountryUsageLayer/>
            <React.StrictMode>
                <LeafletLayers
                    relays={relays}
                    reloadSelectedDay={useCallback(() => setRefreshDayCount(prev => prev + 1), [])}
                />
                {/* CARTO's documented endpoint, the legacy {s}.basemaps.cartocdn.com/dark_all URLs return tiles in mismatched styles */}
                <TileLayer
                    maxZoom={19}
                    url="https://basemaps.cartocdn.com/rastertiles/dark_all/{z}/{x}/{y}{r}.png?key=cb1_2kmx_1_d709cb99db288b90b866bc72"
                    noWrap={true}
                />
            </React.StrictMode>
        </MapContainer>
    );
};

export default LeafletWorldMap
