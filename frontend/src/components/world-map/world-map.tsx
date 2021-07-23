import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useEffect, useState} from "react";
import {apiBaseUrl} from "../../util/constants";
import {circleMarker, LayerGroup, LeafletMouseEvent, Map as LeafletMap} from "leaflet";
import 'leaflet/dist/leaflet.css';
import "./world-map.scss"
import {NodePopup} from "../node-popup/node-popup";
import {ArchiveGeoRelayView} from "../../types/archive-geo-relay";
import {RelayFlag} from "../../types/relay";
import {Statistics, TempSettings} from "../../types/variousTypes";

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

        const layer = new LayerGroup([defaultLayer, exitLayer, guardLayer])

        let stats: Statistics = {
            guard: 0,
            default: 0,
            exit: 0,
        }

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


            let color = "#833ab4";
            let layer = defaultLayer;
            if (relay.flags?.includes(RelayFlag.Exit)) {
                color = "#f96969"
                layer = exitLayer
                stats.exit++
            } else if (relay.flags?.includes(RelayFlag.Guard)) {
                color = "#fcb045"
                layer = guardLayer
                stats.guard++
            } else {
                stats.default++
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

        setStatisticsCallback(stats)

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
