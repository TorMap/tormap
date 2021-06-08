import React from "react";
import {MapContainer, Marker, TileLayer} from "react-leaflet";
import basicRelay from "../utils/types";
import {types} from "util";

export class Map extends React.Component {
    constructor(props: {}) {
        super(props);
        this.state = {
            loading: true
        };
    };

    //ToDo: change URL to backend
    async fetchRelays() {
        // GET request using fetch with async/await
        const response = await fetch('https://onionoo.torproject.org/details?limit=1000');
        const data = await response.json();

        //ToDo: safe relays somewhere
        let relays: Array<basicRelay>
        let basicRelay: basicRelay
        data.relays.map(relay => {
            basicRelay.fingerprint = relay.fingerprint;
            basicRelay.as = relay.as;
            basicRelay.first_seen = relay.first_seen;
            basicRelay.last_see = relay.last_see;
            basicRelay.latitude = relay.latitude;
            basicRelay.longitude = relay.longitude;
            relays.push(basicRelay);
        })

        this.setState({relays: relays})
        this.setState({loading: false})
    };

    drawMarkers(){
        this.state.relays.map(relay => {
            <Marker key={relay.fingerprint} position={[relay.latitude, relay.longitude]}/>
        })
    };

    componentDidMount() {
        this.fetchRelays()
    };

    render() {
        return(
            <MapContainer center={[15,0]} zoom={3} scrollWheelZoom={false}>
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
                    subdomains="abcd"
                    maxZoom={19}
                    url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                />
                {this.state.loading ? null : this.drawMarkers()}
            </MapContainer>
        )};
}
