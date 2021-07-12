import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useCallback, useEffect, useState} from "react";
import {apiBaseUrl} from "../../util/constants";
import {CircleMarker, circleMarker, LeafletMouseEvent, Map as LeafletMap} from "leaflet";
import 'leaflet/dist/leaflet.css';
import "./world-map.scss"
import {NodePopup} from "../node-popup/node-popup";
import {ArchiveGeoRelayView} from "../../types/archive-geo-relay";
import {RelayFlag} from "../../types/relay";

interface Props {
    /**
     * This months data will be fetched from backend and visualized on the map
     */
    monthToDisplay?: string

    colorFlags: boolean
}

export const WorldMap: FunctionComponent<Props> = ({monthToDisplay, colorFlags= false}) => {
    const [showNodePopup, setShowNodePopup] = useState(false)
    const [nodePopupRelayId, setNodePopupRelayId] = useState<number>()
    const [monthToRelays] = useState<Map<string, ArchiveGeoRelayView[]>>(new Map())
    const [leafletMap, setLeafletMap] = useState<LeafletMap>()
    const [activeMarkers, setActiveMarkers] = useState<CircleMarker[]>([])

    const onMarkerClick = (click: LeafletMouseEvent) => {
        console.log("Marker clicked, show node details")
        setNodePopupRelayId(click.sourceTarget.options.className)
        setShowNodePopup(true)
    }

    const removeMapMarkers = useCallback(() => {
        if (leafletMap) {
            activeMarkers.forEach(marker => leafletMap.removeLayer(marker))
        }
    }, [activeMarkers, leafletMap])

    const drawRelays = async (month: string, relays: ArchiveGeoRelayView[]) => {
        if (leafletMap) {
            removeMapMarkers()
            const newActiveMarkers: CircleMarker[] = []
            relays.forEach(relay => {

                var color = "#833ab4"
                if(colorFlags) {
                    if (relay.flags?.includes(RelayFlag.Exit)) {
                        color = "#f96969"
                    } else if (relay.flags?.includes(RelayFlag.Guard)) {
                        color = "#fcb045"
                    }
                }

                const marker = circleMarker(
                    [relay.lat, relay.long],
                    {
                        radius: 1,
                        className: relay.finger,
                        color: color
                    },
                )
                    .on("click", onMarkerClick)
                    .addTo(leafletMap);
                newActiveMarkers.push(marker)
                leafletMap.addLayer(marker)
            });
            setActiveMarkers(newActiveMarkers)
        }
    }

    useEffect(() => {
        if (monthToDisplay) {
            if (!monthToRelays.has(monthToDisplay)) {
                console.log("Fetching relays")
                fetch(`${apiBaseUrl}/archive/geo/relay/${monthToDisplay}`)
                    .then(response => response.json())
                    .then(newRelays => {
                        monthToRelays.set(monthToDisplay, newRelays)
                        drawRelays(monthToDisplay, newRelays)
                    })
                    .catch(console.log)
            } else {
                drawRelays(monthToDisplay, monthToRelays.get(monthToDisplay)!!)
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [monthToDisplay, colorFlags])


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
