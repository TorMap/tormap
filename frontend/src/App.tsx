import React from 'react';
import './App.css';
import {MapContainer, Marker, TileLayer} from "react-leaflet";
import * as test_data from './Example-data-details.json';

function App() {
  return (
        <MapContainer center={[0,0]} zoom={3} scrollWheelZoom={false}>
            <TileLayer
                attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {
                test_data.relays.map(relay => {
                    if (!(isNaN(relay.latitude))) {
                        return  (<Marker
                                    key={relay.nickname}
                                    position={[relay.latitude, relay.longitude]}
                                />)
                    } else {return -1};
                })
            }
        </MapContainer>
  );
}

export default App;
