import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useEffect, useState} from "react";
import {apiBaseUrl} from "../../util/constants";
import {circleMarker, LayerGroup, LeafletMouseEvent, Map as LeafletMap} from "leaflet";
import 'leaflet/dist/leaflet.css';
import "./world-map.scss"
import {NodePopup} from "../node-popup/node-popup";
import {ArchiveGeoRelayView} from "../../types/archive-geo-relay";
import {RelayFlag} from "../../types/relay";
import {Settings} from "../../types/variousTypes";

interface Props {
    /**
     * This months data will be fetched from backend and visualized on the map
     */
    dayToDisplay?: string

    settings: Settings

    setLoadingStateCallback: (b: boolean) => void
}

// Variable needs to be outside component
let latestRequestTimestamp: number | undefined = undefined

export const WorldMap: FunctionComponent<Props> = ({dayToDisplay, settings, setLoadingStateCallback}) => {
    const [showNodePopup, setShowNodePopup] = useState(false)
    const [nodePopupRelayId, setNodePopupRelayId] = useState<number>()
    const [leafletMap, setLeafletMap] = useState<LeafletMap>()
    const [markerLayer] = useState<LayerGroup>(new LayerGroup())

    useEffect(() => {
        if (dayToDisplay) {
            let currentTimeStamp = Date.now()
            setLoadingStateCallback(true)
            fetch(`${apiBaseUrl}/archive/geo/relay/day/${dayToDisplay}`)
                .then(response => response.json())
                .then(newRelays => {
                    setLoadingStateCallback(false)
                    drawLayerGroup(currentTimeStamp, relaysToLayerGroup(newRelays))
                })
                .catch(console.log)
            latestRequestTimestamp = currentTimeStamp
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [dayToDisplay, settings])

    const onMarkerClick = (click: LeafletMouseEvent) => {
        console.log("Marker clicked, show node details")
        setNodePopupRelayId(click.sourceTarget.options.className)
        setShowNodePopup(true)
    }

    const relaysToLayerGroup = (relays: ArchiveGeoRelayView[]): LayerGroup => {

        console.time(`relaysToLayerGroup`)
        console.timeLog(`relaysToLayerGroup`, `New Layer with ${relays.length} elements`)

        const defaultLayer = new LayerGroup()
        const exitLayer = new LayerGroup()
        const guardLayer = new LayerGroup()
        const bridgeLayer = new LayerGroup()

        const layer = new LayerGroup([defaultLayer, exitLayer, guardLayer])

        relays.forEach(relay => {
            let color = "#833ab4";
            let layer = defaultLayer;
            if (relay.flags?.includes(RelayFlag.Exit)) {
                color = "#f96969"
                layer = exitLayer
            } else if (relay.flags?.includes(RelayFlag.Guard)) {
                color = "#fcb045"
                layer = guardLayer
            } else if (relay.flags?.includes(RelayFlag.Bridge)) {
                color = "#abffab"
                layer = bridgeLayer
            }
            circleMarker(
                [relay.lat, relay.long],
                {
                    radius: 1,
                    className: relay.finger,
                    color: color
                },
            )
                .on("click", onMarkerClick)
                .addTo(layer)
        })

        console.timeLog(`relaysToLayerGroup`, `New Layer with ${relays.length} elements finished`)
        console.timeEnd(`relaysToLayerGroup`)

        return layer
    }

    const drawLayerGroup = (currentTimeStamp: number, layerGroup: LayerGroup) => {
        if (currentTimeStamp !== latestRequestTimestamp) {
            console.log(`skipped drawing for ${dayToDisplay}`)
        } else if (leafletMap && dayToDisplay) {
            console.log(`drawing for ${dayToDisplay}`)
            markerLayer.clearLayers()
            const layers = layerGroup.getLayers()

            if (settings.default) layers[0].addTo(markerLayer)
            if (settings.exit) layers[1].addTo(markerLayer)
            if (settings.guard) layers[2].addTo(markerLayer)
            if (settings.bridge) layers[3].addTo(markerLayer)
        }
    }

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
            whenCreated={(newMap: LeafletMap) => {
                markerLayer.addTo(newMap)
                setLeafletMap(newMap)
            }}
        >
            <NodePopup
                showNodePopup={showNodePopup}
                setShowNodePopup={() => setShowNodePopup(false)}
                relayId={nodePopupRelayId}
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
