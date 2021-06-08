import {MapContainer, Marker, TileLayer} from "react-leaflet";
import React, {useEffect, useState} from "react";
import {Relay} from "../types/relay";

export const WorldMap = () => {
    const [relays, setRelays] = useState<Relay[]>([])

    useEffect(() => {
        console.log("Fetching relays")
        fetch('http://localhost:8080/node/relays')
        .then(res => res.json())
        .then((data) => {
            setRelays(data)
            console.log("Fetched relays")
        })
        .catch(console.log)
    }, []);

    return (
        <MapContainer center={[15,0]} zoom={3} scrollWheelZoom={false}>
            <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
                subdomains="abcd"
                maxZoom={19}
                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
            />
            {relays.map(relay => <Marker key={relay.fingerprint} position={[relay.latitude, relay.longitude]}/>)}
        </MapContainer>
    );
};
