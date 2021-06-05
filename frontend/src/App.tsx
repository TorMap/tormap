import React from 'react';
import './App.css';
import {MapContainer, Marker, TileLayer} from "react-leaflet";
import * as test_data from './relays.json';

function App() {
  return (
        <MapContainer center={[15,0]} zoom={3} scrollWheelZoom={false} >
            <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
                subdomains="abcd"
                maxZoom={19}
                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"

            />
            {
                test_data.relays.map(relay => {
                    if (relay.latitude !== undefined) {
                        return  (<Marker
                                    key={relay.fingerprint}
                                    position={[relay.latitude, relay.longitude]}
                                />)
                    } else {return -1}
                })
            }
        </MapContainer>
  );
}

export default App;
