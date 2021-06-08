import React from "react";
import {MapContainer, Marker, TileLayer} from "react-leaflet";

export class Map extends React.Component {
    constructor(props: {}) {
        super(props);
        this.state = {
            relays: null
        }
    }

    async fetchRelays() {
        // GET request using fetch with async/await
        const response = await fetch('http:localhost:8080/relays');
        const data = await response.json();
        this.setState({ relays: data })
    }

    componentDidMount() {
        this.fetchRelays()
    }

    render() {
        return(
            <MapContainer center={[15,0]} zoom={3} scrollWheelZoom={false}>
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
                    subdomains="abcd"
                    maxZoom={19}
                    url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                />
            </MapContainer>
        )};
}
