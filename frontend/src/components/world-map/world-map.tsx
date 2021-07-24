import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useEffect, useState} from "react";
import {apiBaseUrl} from "../../util/constants";
import {circleMarker, LayerGroup, LeafletMouseEvent, Map as LeafletMap} from "leaflet";
import 'leaflet/dist/leaflet.css';
import "./world-map.scss"
import {NodePopup} from "../node-popup/node-popup";
import {GeoRelayView} from "../../types/geo-relay";
import {RelayFlag} from "../../types/relay";
import {Settings, Statistics, TempSettings} from "../../types/variousTypes";
import {Colors} from "../../types/colors";

interface Props {
    /**
     * This months data will be fetched from backend and visualized on the map
     */
    dayToDisplay?: string

    settings: TempSettings

    setLoadingStateCallback: (b: boolean) => void

    setStatisticsCallback: (stat: Statistics) => void
}

// Variable needs to be outside component
let latestRequestTimestamp: number | undefined = undefined

export const WorldMap: FunctionComponent<Props> = ({dayToDisplay, settings, setLoadingStateCallback, setStatisticsCallback}) => {
    const [showNodePopup, setShowNodePopup] = useState(false)
    const [nodePopupRelayId, setNodePopupRelayId] = useState<number>()
    const [leafletMap, setLeafletMap] = useState<LeafletMap>()
    const [markerLayer] = useState<LayerGroup>(new LayerGroup())
    const [relays, setRelays] = useState<GeoRelayView[]>([])

    useEffect(() => {
        if (dayToDisplay) {
            console.log("fetching")
            let currentTimeStamp = Date.now()
            setLoadingStateCallback(true)
            fetch(`${apiBaseUrl}/archive/geo/relay/day/${dayToDisplay}`)
                .then(response => response.json())
                .then(newRelays => {
                    setLoadingStateCallback(false)
                    if (currentTimeStamp === latestRequestTimestamp) setRelays(newRelays)
                })
                .catch(console.log)
            latestRequestTimestamp = currentTimeStamp
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [dayToDisplay])

    useEffect(() => {
        drawLayerGroup(relaysToLayerGroup(relays))
    },[relays, settings])

    const onMarkerClick = (click: LeafletMouseEvent) => {
        console.log("Marker clicked, show node details")
        setNodePopupRelayId(click.sourceTarget.options.className)
        setShowNodePopup(true)
    }

    const relaysToLayerGroup = (relays: GeoRelayView[]): LayerGroup => {

        console.time(`relaysToLayerGroup`)
        console.timeLog(`relaysToLayerGroup`, `New Layer with ${relays.length} elements`)

        const defaultLayer = new LayerGroup()
        const exitLayer = new LayerGroup()
        const guardLayer = new LayerGroup()

        const layer = new LayerGroup([defaultLayer, exitLayer, guardLayer])

        let stats: Statistics = {
            guard: 0,
            default: 0,
            exit: 0,
        }

        let latLonMap: Map<string, number> = new Map<string, number>()

        relays.forEach(relay => {
            if (settings.miValid &&         !relay.flags?.includes(RelayFlag.Valid))        {return}
            if (settings.miNamed &&         !relay.flags?.includes(RelayFlag.Named))        {return}
            if (settings.miUnnamed &&        !relay.flags?.includes(RelayFlag.Unnamed))     {return}
            if (settings.miRunning &&       !relay.flags?.includes(RelayFlag.Running))      {return}
            if (settings.miStable &&        !relay.flags?.includes(RelayFlag.Stable))       {return}
            if (settings.miExit &&          !relay.flags?.includes(RelayFlag.Exit))         {return}
            if (settings.miFast &&          !relay.flags?.includes(RelayFlag.Fast))         {return}
            if (settings.miGuard &&         !relay.flags?.includes(RelayFlag.Guard))        {return}
            if (settings.miAuthority &&     !relay.flags?.includes(RelayFlag.Authority))    {return}
            if (settings.miV2Dir &&         !relay.flags?.includes(RelayFlag.V2Dir))        {return}
            if (settings.miHSDir &&         !relay.flags?.includes(RelayFlag.HSDir))        {return}
            if (settings.miNoEdConsensus && !relay.flags?.includes(RelayFlag.NoEdConsensus)){return}
            if (settings.miStaleDesc &&     !relay.flags?.includes(RelayFlag.StaleDesc))    {return}
            if (settings.miSybil &&         !relay.flags?.includes(RelayFlag.Sybil))        {return}
            if (settings.miBadExit &&       !relay.flags?.includes(RelayFlag.BadExit))      {return}

            if (settings.agregateCoordinates){ //Draw only one Point for same coordinates
                const key: string = `${relay.lat},${relay.long}`
                if (latLonMap.has(key)){
                    latLonMap.set(key, latLonMap.get(key)!! + 1)
                }else{
                    latLonMap.set(key, 1)
                }
            }

            if(!settings.agregateCoordinates){ //Draw a point for each Relay
                let color = "#833ab4"
                let layer = defaultLayer
                if (relay.flags?.includes(RelayFlag.Exit)) {
                    if (settings.colorNodesAccordingToType) color = "#f96969"
                    layer = exitLayer
                    stats.exit++
                } else if (relay.flags?.includes(RelayFlag.Guard)) {
                    if (settings.colorNodesAccordingToType) color = "#fcb045"
                    layer = guardLayer
                    stats.guard++
                } else {
                    stats.default++
                }
                circleMarker(
                    [relay.lat, relay.long],
                    {
                        radius: 1,
                        className: relay.detailsId,
                        color: color
                    },
                )
                    .on("click", onMarkerClick)
                    .addTo(layer)
            }

        })

        if(settings.agregateCoordinates){
            latLonMap.forEach((value, key) => {
                const coordinates= key.split(",")
                circleMarker(
                    [+coordinates[0],+coordinates[1]],
                    {
                        radius: value/3,
                        color: "#ffffff"
                    }
                )
                    .addTo(defaultLayer)
            })
        }

        setStatisticsCallback(stats)

        console.timeLog(`relaysToLayerGroup`, `New Layer with ${relays.length} elements finished`)
        console.timeEnd(`relaysToLayerGroup`)

        return layer
    }

    const drawLayerGroup = (layerGroup: LayerGroup) => {
        if (leafletMap && dayToDisplay) {
            console.log(`drawing for ${dayToDisplay}`)
            markerLayer.clearLayers()
            const layers = layerGroup.getLayers()

            if (settings.Default) layers[0].addTo(markerLayer)
            if (settings.Exit) layers[1].addTo(markerLayer)
            if (settings.Guard) layers[2].addTo(markerLayer)
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
                nodeDetailsId={nodePopupRelayId}
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
