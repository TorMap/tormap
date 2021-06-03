import React from 'react';
import './App.css';
import {MapContainer, Marker, Popup, TileLayer} from "react-leaflet";
import "./leaflet.css"


const style={
    height: "800%"
}

function App() {
  return (
    <div className="App">
            <MapContainer center={[0,0]} zoom={1} scrollWheelZoom={false} style={style}>
                <TileLayer
                    attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                <Marker position={[51.505, -0.09]}>
                    <Popup>
                        A pretty CSS3 popup. <br /> Easily customizable.
                    </Popup>
                </Marker>
            </MapContainer>
    </div>
  );
}

export default App;
