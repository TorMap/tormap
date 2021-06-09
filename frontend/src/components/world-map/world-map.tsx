import {MapContainer, TileLayer} from "react-leaflet";
import React, {useEffect, useState} from "react";
import {RelayView} from "../../types/relay-view";
import {apiBaseUrl} from "../../util/constants";
import {circleMarker, LeafletMouseEvent, Map} from "leaflet";
import {PopupModal} from "../popup-modal";
import "./world-map.css"

export const WorldMap = () => {
    const [showNodePopup, setShowNodePopup] = useState(false)
    const [nodePopupContent, setNodePopupContent] = useState("")
    let leafletMap: Map;

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

    useEffect(() => {
        console.log("Fetching relays")
        fetch(apiBaseUrl + "/node/relays")
            .then(response => response.json())
            .then((relays: RelayView[]) => {
                relays.forEach(relay => {
                    circleMarker(
                        [relay.lat, relay.long],
                        {
                            radius: 2,
                            className: relay.id,
                        },
                    )
                        .on("click", onMarkerClick)
                        .addTo(leafletMap);
                });
            })
            .catch(console.log)
        // eslint-disable-next-line react-hooks/exhaustive-deps
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
                leafletMap = newMap
            }}
        >
            <PopupModal
                show={showNodePopup}
                onClose={() => setShowNodePopup(false)}
                content={nodePopupContent}
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
