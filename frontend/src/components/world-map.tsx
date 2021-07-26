import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useEffect, useState} from "react";
import {apiBaseUrl} from "../util/constants";
import L, {circleMarker, Layer, LayerGroup, LeafletMouseEvent, Map as LeafletMap} from "leaflet";
import 'leaflet/dist/leaflet.css';
import {NodePopup} from "./node-popup";
import {GeoRelayView} from "../types/geo-relay";
import {RelayFlag} from "../types/relay";
import {Settings, Statistics} from "../types/variousTypes";
import "leaflet.heat"
import {makeStyles} from "@material-ui/core";

const useStyle = makeStyles(theme => ({
    leafletContainer: {
        width: "100vw",
        height: "100vh",
        backgroundColor: "#262626",
        position: "fixed",
    }
}))

interface Props {
    /**
     * This months data will be fetched from backend and visualized on the map
     */
    dayToDisplay?: string

    settings: Settings

    setSettingsCallback: (s: Settings) => void

    setLoadingStateCallback: (b: boolean) => void

    setStatisticsCallback: (stat: Statistics) => void
}

// Variable needs to be outside component
let latestRequestTimestamp: number | undefined = undefined

export const WorldMap: FunctionComponent<Props> = ({dayToDisplay, settings, setSettingsCallback, setLoadingStateCallback, setStatisticsCallback}) => {
    const [showNodePopup, setShowNodePopup] = useState(false)
    const [nodePopupRelayId, setNodePopupRelayId] = useState<number>()
    const [leafletMap, setLeafletMap] = useState<LeafletMap>()
    const [markerLayer] = useState<LayerGroup>(new LayerGroup())
    const [heatLayer, setHeatLayer] = useState<Layer>(new Layer())
    const [relays, setRelays] = useState<GeoRelayView[]>([])
    const classes = useStyle()

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
        let gradientMap: number[] = []
        let countrys: string[] = []

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

            if (settings.sortFamily && relay.familyId != undefined) {
                if (!gradientMap.includes(relay.familyId)) gradientMap.push(relay.familyId)
            }
            if (settings.sortContry && relay.country != undefined){
                if (!countrys.includes(relay.country)) countrys.push(relay.country)
            }

            if (settings.aggregateCoordinates || settings.heatMap){ //Draw only one Point for same coordinates
                const key: string = `${relay.lat},${relay.long}`
                if (latLonMap.has(key)){
                    latLonMap.set(key, latLonMap.get(key)!! + 1)
                }else{
                    latLonMap.set(key, 1)
                }
            }

            if(settings.showMarker && !settings.aggregateCoordinates && !settings.sortFamily){ //Draw a point for each Relay
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

        if (settings.sortFamily) {
            relays.forEach(relay => {
                let color = "#787d7d"
                if (relay.familyId != undefined) {
                    const index = gradientMap.findIndex((value, i) => {
                        if (value === relay.familyId) return i
                    })
                    const selectedFamily = settings.selectedFamily
                    const hue = index * 360 / gradientMap.length
                    let sat = "90%"
                    let radius = 4
                    if (selectedFamily !== undefined) sat = "30%"
                    if (selectedFamily !== undefined && selectedFamily === relay.familyId) {
                        sat = "90%"
                        radius = 7
                    }
                        color = `hsl(${hue},${sat},60%)`
                    circleMarker(
                        [relay.lat,relay.long],
                        {color: color,
                            radius: radius,
                        }
                    )
                        .on("click", () => {
                            if (relay.familyId === selectedFamily) {
                                setSettingsCallback({...settings, selectedFamily: undefined})
                            }else{
                                setSettingsCallback({...settings, selectedFamily: relay.familyId})
                            }
                        })
                        .addTo(exitLayer)
                }
                if (settings.showMarker){
                    circleMarker(
                        [relay.lat, relay.long],
                        {
                            color: color,
                            radius: 1,
                        }
                    )
                        .on("click", () => setSettingsCallback({...settings, selectedFamily: undefined}))
                        .addTo(defaultLayer)
                }
            })
        }

        if (settings.sortContry) {
            relays.forEach(relay => {
                let color = "#FFFFFF"
                if (relay.country != undefined) {
                    const index = countrys.findIndex((value, i) => {
                        if (value === relay.country) return i
                    })
                    //const hue = index * 360 / countrys.length
                    const hue = index * 86.507
                    let sat = "90%"
                    if (settings.selectedCountry != undefined) sat = "10%"
                    if (settings.selectedCountry != undefined && settings.selectedCountry === relay.country) sat = "90%"
                    color = `hsl(${hue},${sat},60%)`
                    circleMarker(
                        [relay.lat,relay.long],
                        {color: color,
                            radius: 1,
                        }
                    )
                        .on("click", () => {
                            if (settings.selectedCountry === relay.country) {
                                setSettingsCallback({...settings, selectedCountry: undefined})
                            }else{
                                setSettingsCallback({...settings, selectedCountry: relay.country})
                            }
                        })
                        .addTo(exitLayer)
                }
                if (settings.showMarker){
                    circleMarker(
                        [relay.lat, relay.long],
                        {
                            color: color,
                            radius: 1,
                        }
                    )
                        .on("click", () => setSettingsCallback({...settings, selectedCountry: undefined}))
                        .addTo(defaultLayer)
                }
            })
        }

        if (settings.aggregateCoordinates){
            latLonMap.forEach((value, key) => {
                const coordinates= key.split(",")
                circleMarker(
                    [+coordinates[0],+coordinates[1]],
                    {
                        radius: value/7,
                        color: "#ffffff"
                    }
                )
                    .addTo(defaultLayer)
            })
        }

        if (settings.heatMap){
            leafletMap?.removeLayer(heatLayer)

            let latlngs: Array<number[]> = new Array<number[]>()
            let maxValue: number = 1

            latLonMap.forEach((value, key) => {
                const coordinates= key.split(",")
                if (value > maxValue) maxValue = value
                latlngs.push([+coordinates[0],+coordinates[1], value/maxValue])})


            //relays.forEach(relay => latlngs.push([relay.lat, relay.long, 1]))
            // @ts-ignore
            let heat = L.heatLayer(latlngs, {radius: 25, max: 100, blur: 35, minOpacity: .55, gradient: {0.4: '#2e53dc', 0.65: '#c924ae', .75: '#ff4646', .83: "#ff0000"}}).addTo(leafletMap)
            setHeatLayer(heat)
            stats ={...stats, maxValueOnSameCoordinate: maxValue}

        }else{
            leafletMap?.removeLayer(heatLayer)
        }

        setStatisticsCallback(stats)

        console.timeLog(`relaysToLayerGroup`, `New Layer with ${relays.length} elements finished`)
        console.timeEnd(`relaysToLayerGroup`, )

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
            className={classes.leafletContainer}
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
