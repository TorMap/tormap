
import {CircleMarker, MapContainer, TileLayer} from "react-leaflet";
import React, {useEffect, useState} from "react";
import {RelayView} from "../types/relay-view";
import {apiBaseUrl} from "../util/constants";
import {LeafletEventHandlerFnMap, LeafletMouseEvent, Map} from "leaflet";
import {PopupModal} from "./popup-modal";

export const WorldMap = () => {
    const [relays, setRelays] = useState<RelayView[]>([])
    const [showNodePopup, setShowNodePopup] = useState(false)
    const [nodePopupContent, setNodePopupContent] = useState("")

    const onMapCreated = (map: Map) => {
        // TODO configure map further if necessary
    }

    const markerEventHandlers: LeafletEventHandlerFnMap = {
        click: (click: LeafletMouseEvent) => {
            console.log("Marker clicked, show node details")
            const fingerprint = click.sourceTarget.options.className
            fetch(apiBaseUrl + "/node/relay/" + fingerprint)
                .then(response => response.json())
                .then((data: any) => {
                    setNodePopupContent(JSON.stringify(data))
                    setShowNodePopup(true)
                })
                .catch(console.log)
        }
    }

    useEffect(() => {
        console.log("Fetching relays")
        fetch(apiBaseUrl + "/node/relays")
            .then(response => response.json())
            .then((relays: RelayView[]) => {
                setRelays(relays)
                console.log("Fetched relays")
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
            whenCreated={onMapCreated}
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
            {relays.map(relay =>
                <CircleMarker
                    key={relay.fingerprint}
                    className={relay.fingerprint}
                    center={[relay.latitude, relay.longitude]}
                    radius={1}
                    eventHandlers={markerEventHandlers}
                />
            )}
        </MapContainer>
    );
};
