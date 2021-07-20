import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useCallback, useEffect, useState} from "react";
import {apiBaseUrl} from "../../util/constants";
import {CircleMarker, circleMarker, control, Layer, LayerGroup, LeafletMouseEvent, Map as LeafletMap} from "leaflet";
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

    preLoadMonths?: string[]

    filter: {
        guard: boolean,
        exit: boolean,
        default: boolean,
    }
}

export const WorldMap: FunctionComponent<Props> = ({monthToDisplay, colorFlags= false, preLoadMonths, filter}) => {
    const [showNodePopup, setShowNodePopup] = useState(false)
    const [nodePopupRelayId, setNodePopupRelayId] = useState<number>()
    const [monthToLayer] = useState<Map<string, LayerGroup>>(new Map())
    const [previousMonth, setPreviousMonth] = useState("")
    const [leafletMap, setLeafletMap] = useState<LeafletMap>()

    const onMarkerClick = (click: LeafletMouseEvent) => {
        console.log("Marker clicked, show node details")
        setNodePopupRelayId(click.sourceTarget.options.className)
        setShowNodePopup(true)
    }

    useEffect(() => {
        if (preLoadMonths) {
            preLoadMonths.forEach(month => {
                if (!monthToLayer.has(month)) {
                    fetch(`${apiBaseUrl}/archive/geo/relay/${month}`)
                        .then(response => response.json())
                        .then(newRelays => {
                            monthToLayer.set(month, relaysToLayerGroup(newRelays))
                            console.log(`Preloaded ${month} `)
                        })
                }
            })
            console.log("Completed preloading")
        }
    }, [preLoadMonths])

    useEffect(() => {
        console.timeEnd(`Fetch`)
        console.time(`Fetch`)
        if (monthToDisplay) {
            if ( !monthToLayer.has(monthToDisplay) ) {
                console.timeLog(`Fetch`, `Fetching Layer for ${ monthToDisplay }`)
                fetch(`${apiBaseUrl}/archive/geo/relay/${monthToDisplay}`)
                    .then(response => response.json())
                    .then(newRelays => {
                        monthToLayer.set(monthToDisplay, relaysToLayerGroup(newRelays))
                    })
                    .then(() => console.timeLog(`Fetch`, `Fetching Layer for ${ monthToDisplay } finished`))
                    .then(() => {
                        console.timeLog(`Fetch`, `Drawing fetched Layer for ${ monthToDisplay } `)
                        drawLayer(monthToDisplay)
                        console.timeLog(`Fetch`, `Drawing fetched Layer for ${ monthToDisplay } finished`)
                        console.timeEnd(`Fetch`)
                    })
                    .catch(console.log)
            } else {
                console.timeLog(`Fetch`, `Drawing Layer for ${ monthToDisplay } `)
                drawLayer(monthToDisplay)
                console.timeLog(`Fetch`, `Drawing Layer for ${ monthToDisplay } finished`)
                console.timeEnd(`Fetch`)
            }
        }
    }, [monthToDisplay])

    const relaysToLayerGroup = (relays: ArchiveGeoRelayView[]) : LayerGroup => {

        console.time(`relaysToLayerGroup`)
        console.timeLog(`relaysToLayerGroup`, `New Layer with ${ relays.length } elements`)

        const defaultLayer = new LayerGroup()
        const exitLayer = new LayerGroup()
        const guardLayer = new LayerGroup()

        const layer = new LayerGroup([defaultLayer, exitLayer, guardLayer])

        relays.forEach(relay => {
            let color = "#833ab4";
            let layer = defaultLayer;
            if(colorFlags) {
                if (relay.flags?.includes(RelayFlag.Exit)) {
                    color = "#f96969"
                    layer = exitLayer
                } else if (relay.flags?.includes(RelayFlag.Guard)) {
                    color = "#fcb045"
                    layer = guardLayer
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
                .addTo(layer)
        })

        console.timeLog(`relaysToLayerGroup`, `New Layer with ${ relays.length } elements finished`)
        console.timeEnd(`relaysToLayerGroup`)

        return layer
    }

    const drawLayer = (monthToDisplay: string) =>{
        const map = leafletMap
        if(map){
            if(monthToLayer.has(previousMonth)) { monthToLayer.get(previousMonth)!!.eachLayer(layer => layer.removeFrom(map)) }
            const newLayer = monthToLayer.get(monthToDisplay)!!
            const layers = newLayer.getLayers()

            if (filter.default) layers[0].addTo(map)
            if (filter.exit) layers[1].addTo(map)
            if (filter.guard) layers[2].addTo(map)

            setPreviousMonth(monthToDisplay)
        }
    }

    useEffect(() => {
        if(monthToDisplay) {
            drawLayer(monthToDisplay)
        }
    }, [filter])

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
