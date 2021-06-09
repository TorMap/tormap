
import {CircleMarker, MapContainer, TileLayer} from "react-leaflet";
import React, {useEffect, useState} from "react";
import {Relay} from "../types/relay";

export const WorldMap = () => {
    const [relays, setRelays] = useState<Relay[]>([])

    useEffect(() => {
        console.log("Fetching relays")
        fetch('http://localhost:8080/node/relays')
            .then(response => response.json())
            .then((relays: Relay[]) => {
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
            wheelPxPerZoomLevel={200}
            preferCanvas={true}
        >
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
                    center={[relay.latitude, relay.longitude]}
                    radius={1}
                />
            )}
        </MapContainer>
    );
};
