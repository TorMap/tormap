import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useCallback, useEffect, useState} from "react";
import {RelayView} from "../../types/relay";
import {apiBaseUrl} from "../../util/constants";
import {CircleMarker, circleMarker, LeafletMouseEvent, Map} from "leaflet";
import 'leaflet/dist/leaflet.css';
import "./world-map.css"
import {NodePopUp} from "../NodePopUp";

interface Props {
    /**
     * This range is used to select nodes which will be displayed on the map
     */
    dateRangeToDisplay: { startDate: Date, endDate: Date }
}

export const WorldMap: FunctionComponent<Props> = ({dateRangeToDisplay}) => {
    const [showNodePopup, setShowNodePopup] = useState(false)
    const [nodePopupContent, setNodePopupContent] = useState("")
    const [relays, setRelays] = useState<RelayView[]>([])
    const [leafletMap, setLeafletMap] = useState<Map>()
    const [activeMarkers, setActiveMarkers] = useState<CircleMarker[]>([])

    const onMarkerClick = (click: LeafletMouseEvent) => {
        console.log("Marker clicked, show node details")
        const relayId = click.sourceTarget.options.className
        fetch(apiBaseUrl + "/node/relay/" + relayId)
            .then(response => response.json())
            .then((data: any) => {
                setNodePopupContent(JSON.stringify(data))
                setShowNodePopup(true)
            })
            .catch(console.log)
    }

    const removeMapMarkers = useCallback(() =>{
        if (leafletMap) {
            activeMarkers.forEach(marker => leafletMap.removeLayer(marker));
        }
    }, [activeMarkers, leafletMap])

    useEffect(() => {
        if (leafletMap) {
            removeMapMarkers()
            const newActiveMarkers: CircleMarker[] = [];
            relays.forEach(relay => {
                if (
                    new Date(relay.firstSeen) < dateRangeToDisplay.endDate
                    && new Date(relay.lastSeen) > dateRangeToDisplay.startDate
                ) {
                    const marker = circleMarker(
                        [relay.lat, relay.long],
                        {
                            radius: 2,
                            className: relay.id,
                        },
                    )
                        .on("click", onMarkerClick)
                        .addTo(leafletMap);
                    newActiveMarkers.push(marker);
                    leafletMap.addLayer(marker);
                }
            });
            setActiveMarkers(newActiveMarkers)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [relays, dateRangeToDisplay]);

    useEffect(() => {
        console.log("Fetching relays")
        fetch(apiBaseUrl + "/node/relays")
            .then(response => response.json())
            .then(newRelays => {
                setRelays(newRelays)
            })
            .catch(console.log)
    }, []);

    return (
        <MapContainer
            center={[15, 0]}
            minZoom={2}
            zoom={3}
            scrollWheelZoom={true}
            zoomSnap={0.5}
            zoomDelta={0.5}
            wheelPxPerZoomLevel={200}
            preferCanvas={true}
            whenCreated={(newMap: Map) => {
                setLeafletMap(newMap)
            }}
        >
            <NodePopUp
                showNodePopup={showNodePopup}
                setShowNodePopup={() => setShowNodePopup(false)}
                nodePopupContent={nodePopupContent}
            />
            <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
                subdomains="abcd"
                maxZoom={19}
                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                noWrap={true}
            />
        </MapContainer>
    );
};
