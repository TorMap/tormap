import {MapContainer, TileLayer} from "react-leaflet";
import React, {FunctionComponent, useEffect, useState} from "react";
import {apiBaseUrl} from "../util/constants";
import L, {circleMarker, GeoJSON, Layer, LayerGroup, LeafletMouseEvent, Map as LeafletMap, PathOptions} from "leaflet";
import 'leaflet/dist/leaflet.css';
import {NodePopup} from "./node-popup";
import {GeoRelayView} from "../types/geo-relay";
import {RelayFlag} from "../types/relay";
import {Settings, snackbarMessage, Statistics} from "../types/variousTypes";
import "leaflet.heat"
import {makeStyles} from "@material-ui/core";
import worldGeoData from "../data/world.geo.json"; // data from https://geojson-maps.ash.ms/
import {Feature, GeoJsonObject, GeoJsonProperties, GeometryObject} from "geojson";
import {Colors} from "../util/Config";

/**
 * Styles according to Material UI doc for components used in WorldMap component
 */
const useStyle = makeStyles(() => ({
    leafletContainer: {
        width: "100vw",
        height: "100vh",
        backgroundColor: "#262626",
        position: "fixed",
    }
}))

interface Props {
    /**
     * This days data will be fetched from backend and visualized on the map
     */
    dayToDisplay?: string

    /**
     * the app settings
     */
    settings: Settings

    /**
     * A callback to change settings
     * @param s the settings variable that should be changed
     */
    setSettingsCallback: (s: Settings) => void

    /**
     * A variable callback whether the map is currently fetching a new date
     * @param b whether the map is currently fetching a new date
     */
    setLoadingStateCallback: (b: boolean) => void

    /**
     * A callback to change statistics
     * @param stat the statistic variable that should be changed
     */
    setStatisticsCallback: (stat: Statistics) => void

    /**
     * callback for errormessages
     * @param snackbarMessage Objekt of type snackbarMessage with message and severity
     */
    handleSnackbar: (snackbarMessage: snackbarMessage) => void
}

// Variable needs to be outside component to keep track of the last selected date
let latestRequestTimestamp: number | undefined = undefined

export const WorldMap: FunctionComponent<Props> = ({dayToDisplay, settings, setSettingsCallback, setLoadingStateCallback, setStatisticsCallback, handleSnackbar}) => {
    const [showNodePopup, setShowNodePopup] = useState(false)
    const [nodePopupRelayId, setNodePopupRelayId] = useState<number>()
    const [leafletMap, setLeafletMap] = useState<LeafletMap>()
    const [markerLayer] = useState<LayerGroup>(new LayerGroup())
    const [heatLayer, setHeatLayer] = useState<Layer>(new Layer())
    const [relays, setRelays] = useState<GeoRelayView[]>([])
    const classes = useStyle()

    //Update listener, fires whenever a new date is to be downloaded, only the last selected date gets displayed
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
                .catch(reason => {
                    handleSnackbar({message: `${reason}`, severity: "error"})
                })
            latestRequestTimestamp = currentTimeStamp
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [dayToDisplay])

    //Update listener, fires whenever settings get changed or an new date got downloaded
    useEffect(() => {
        if (dayToDisplay) drawLayerGroup(relaysToLayerGroup(relays))
    },[relays, settings])

    //Eventhandler for markers to show the node-popup component
    const onMarkerClick = (click: LeafletMouseEvent) => {
        console.log("Marker clicked, show node details")
        setNodePopupRelayId(click.sourceTarget.options.className)
        setShowNodePopup(true)
    }

    //Helper for relaysToLayerGroup, applys all Filters to the downloaded data
    const applyFilter = (relays: GeoRelayView[]): GeoRelayView[] => {
        let filtered: GeoRelayView[] = []
        relays.forEach(relay => {
            //Filter must include settings
            if (settings.miValid &&         !relay.flags?.includes(RelayFlag.Valid))        {return}
            if (settings.miNamed &&         !relay.flags?.includes(RelayFlag.Named))        {return}
            if (settings.miUnnamed &&       !relay.flags?.includes(RelayFlag.Unnamed))      {return}
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

            //Filter relay types
            if (!settings.Exit &&           relay.flags?.includes(RelayFlag.Exit))          {return}
            if (!settings.Guard &&          relay.flags?.includes(RelayFlag.Guard))         {return}
            if (!settings.Default &&        (!relay.flags?.includes(RelayFlag.Guard)
                                            && !relay.flags?.includes(RelayFlag.Exit)))      {return}
            filtered.push(relay)
        })
        return filtered
    }

    //Helper for relaysToLayerGroup, used for adding eventlisteners to the countries
    const onEachFeature = (feature: Feature<GeometryObject, GeoJsonProperties>, layer: Layer) => {
        layer.on({
            click: () => {
                if (feature.properties!!.iso_a2 !== settings.selectedCountry)
                    setSettingsCallback({...settings, selectedCountry: feature.properties?.iso_a2})
                else setSettingsCallback({...settings, selectedCountry: undefined})
            }

        });
    }

    // Processes an array of Relays according to settings and returns an layerGroup with all relevant layers that have to be added to the map
    const relaysToLayerGroup = (relays: GeoRelayView[]): LayerGroup => {
        console.time(`relaysToLayerGroup`)
        console.timeLog(`relaysToLayerGroup`, `New Layer with ${relays.length} elements`)

        const layerToReturn = new LayerGroup()

        // filter relays
        relays = applyFilter(relays)
        if (!relays.length && dayToDisplay) handleSnackbar({message: "there are no Relays withe the filtered flags", severity:"warning"})

        // Map for coordinate's, used to get an Array of GeoRelayView with relays on the same coordinate
        let latLonMap: Map<string, GeoRelayView[]> = new Map<string, GeoRelayView[]>()
        if (settings.aggregateCoordinates || settings.heatMap){
            relays.forEach(relay => {
                const key: string = `${relay.lat},${relay.long}`
                if (latLonMap.has(key)){
                    let old = latLonMap.get(key)!!
                    old.push(relay)
                    latLonMap.set(key, old)
                }else{
                    latLonMap.set(key, [relay])
                }
            })
        }

        //Map for family's, used to get an Array of GeoRelayView with relays in the same family / autonomsystem
        let familyMap: Map<number, GeoRelayView[]> = new Map<number, GeoRelayView[]>()
        // true for forcing the calculation to include it in statistics
        if (settings.sortFamily) {
            relays.forEach(relay => {
                if (relay.familyId !== null) {
                    const key: number = relay.familyId
                    if (familyMap.has(key)) {
                        let old = familyMap.get(key)!!
                        old.push(relay)
                        familyMap.set(key, old)
                    } else {
                        familyMap.set(key, [relay])
                    }
                }
            })
            if (settings.selectedFamily && !familyMap.has(settings.selectedFamily)){
                setSettingsCallback({...settings, selectedFamily: undefined})
            }
            if (settings.sortFamily && familyMap.size === 0) handleSnackbar({message: "There are no Families available for this day!", severity: "warning"})
        }

        //Map for country's, used to get an Array of GeoRelayView with relays in the same country
        let countryMap: Map<string, GeoRelayView[]> = new Map<string, GeoRelayView[]>()
        // true for forcing the calculation to include it in statistics
        if (settings.sortCountry) {
            relays.forEach(relay => {
                if (relay.country !== undefined) {
                    const key: string = relay.country
                    if (countryMap.has(key)) {
                        let old = countryMap.get(key)!!
                        old.push(relay)
                        countryMap.set(key, old)
                    } else {
                        countryMap.set(key, [relay])
                    }
                }
            })
            if(settings.selectedCountry && !countryMap.has(settings.selectedCountry)){
                setSettingsCallback({...settings, selectedCountry: undefined})
            }
        }

        //Draw Country's, used to draw all countries to the map if at least one relay is hosted there
        if (settings.sortCountry){
            if (leafletMap && countryMap.size > 0) {
                const style = (feature: Feature<GeometryObject, GeoJsonProperties>): PathOptions => {
                    if (settings.selectedCountry === feature.properties!!.iso_a2){
                        return {
                            fillColor: "rgba(255,255,255,0.7)",
                            weight: .5,
                        }
                    } else {
                        return {
                            fillColor: "rgba(255,255,255,0.3)",
                            weight: .5,
                        }
                    }
                }

                const geoData = worldGeoData
                let filteredGeoData = new GeoJSON(undefined,{
                    style: style as PathOptions,
                    onEachFeature(feature: Feature<GeometryObject, GeoJsonProperties>, layer: Layer) {
                        onEachFeature(feature, layer)
                    }
                })
                geoData.features.forEach(feature => {
                    if (countryMap.has(feature.properties.iso_a2)){
                        filteredGeoData.addData(feature as GeoJsonObject)
                    }
                })

                const worldGeoLayer = new LayerGroup()
                filteredGeoData.addTo(worldGeoLayer)
                worldGeoLayer.addTo(layerToReturn)
            }
        }

        //Draw aggregated marker's, used to draw all markers to the map with more than 4 relays on the same coordinate
        if (settings.aggregateCoordinates){
            const aggregatedCoordinatesLayer = new LayerGroup()
            latLonMap.forEach((value, key) => {
                const coordinates= key.split(",")
                // skip if a coordinate has less than 4 relays
                if (value.length < 4) return
                circleMarker(
                    [+coordinates[0],+coordinates[1]],
                    {
                        radius: value.length / 2,
                        color: "#ffffff",
                        weight: .3,
                    }
                )
                    .addTo(aggregatedCoordinatesLayer)
            })
            aggregatedCoordinatesLayer.addTo(layerToReturn)
        }

        //Draw marker's, used to draw all markers to the map with colors according to their type
        if (!settings.sortCountry){
            const defaultLayer = new LayerGroup()
            const exitLayer = new LayerGroup()
            const guardLayer = new LayerGroup()
            const defaultMarkerLayer = new LayerGroup([defaultLayer, guardLayer, exitLayer])
            relays.forEach(relay => {
                let color = Colors.Default
                let layer = defaultLayer
                if (relay.flags?.includes(RelayFlag.Exit)) {
                    color = Colors.Exit
                    layer = exitLayer
                }
                else if (relay.flags?.includes(RelayFlag.Guard)) {
                    color = Colors.Guard
                    layer = guardLayer
                }

                circleMarker(
                    [relay.lat, relay.long],
                    {
                        radius: 1,
                        className: relay.detailsId,
                        color: color,
                        weight: 3,
                    },
                )
                    .on("click", onMarkerClick)
                    .addTo(layer)
            })
            defaultMarkerLayer.addTo(layerToReturn)
        }

        //Draw family marker's, used to draw all markers to the map with colors according to their family
        if (settings.sortFamily){
            const familyLayer: LayerGroup = new LayerGroup()
            let index = 0
            familyMap.forEach(family => {
                family.forEach((relay,i , family) => {
                    let hue = index * 360 / familyMap.size * (2 / 3)
                    let sat = "90%"
                    let radius = family.length * 10

                    if (settings.selectedFamily !== undefined && settings.selectedFamily !== relay.familyId) sat = "30%"
                    if (settings.selectedFamily !== undefined && settings.selectedFamily && settings.selectedFamily !== relay.familyId) sat = "0%"

                    const color = `hsl(${hue},${sat},60%)`
                    circleMarker(
                        [relay.lat, relay.long],
                        {color: color,
                                radius: radius,}
                    )
                        .on("click", () => {
                            if (relay.familyId === settings.selectedFamily) {
                                setSettingsCallback({...settings, selectedFamily: undefined})
                            }else{
                                setSettingsCallback({...settings, selectedFamily: relay.familyId})
                            }
                        })
                        .addTo(familyLayer)
                })
                index ++
            })
            familyLayer.addTo(layerToReturn)
        }

        //Draw Heatmap, draws a heatmap with a point for each coordinate
        // https://github.com/Leaflet/Leaflet.heat
        if (settings.heatMap){
            leafletMap?.removeLayer(heatLayer)
            let latlngs: Array<number[]> = new Array<number[]>()
            relays.forEach(relay => latlngs.push([relay.lat, relay.long, 1]))
            // @ts-ignore needed for compatibility with js code of the heatmap package
            let heat = L.heatLayer(latlngs, {radius: 25, max: 1, blur: 35, minOpacity: .55, gradient: {0.4: '#2e53dc', 0.65: '#c924ae', .75: '#ff4646', .83: "#ff0000"}}).addTo(leafletMap)
            setHeatLayer(heat)
        }else{
            leafletMap?.removeLayer(heatLayer)
        }

        //Draw country marker's, used to draw all markers to the map with colors according to their country
        if (settings.sortCountry){
            const countryLayer: LayerGroup = new LayerGroup()
            let index = 0
            const geoData = worldGeoData
            countryMap.forEach((country, key) => {
                let hue = 0
                const mapColor9 = (geoData.features.find(feature => feature.properties.iso_a2 === key)?.properties.mapcolor9)
                if (mapColor9) hue = mapColor9 * 360 / 9
                else hue = 9 * 360 / 9

                country.forEach((relay,i , country) => {
                    let sat = "90%"
                    let radius = 1

                    if (settings.selectedCountry !== undefined && settings.selectedCountry !== relay.country) sat = "30%"
                    if (settings.selectedCountry !== undefined && settings.selectedCountry && settings.selectedCountry !== relay.country) sat = "0%"

                    const color = `hsl(${hue},${sat},60%)`
                    circleMarker(
                        [relay.lat, relay.long],
                        {color: color,
                            radius: radius,
                            className: relay.detailsId,
                        }
                    )
                        .on("click", onMarkerClick)
                        .addTo(countryLayer)
                })
                index ++
            })
            countryLayer.addTo(layerToReturn)
        }

        //Calculate statistics
        if (settings.selectedCountry && settings.selectedFamily && familyMap && countryMap) {
            relays = []
            familyMap.get(settings.selectedFamily)?.forEach(familyRelay => {
                countryMap.get(settings.selectedCountry!!)?.forEach( countryRelay => {
                    if (familyRelay.detailsId === countryRelay.detailsId) relays.push(familyRelay)
                })
            })
        } else if(settings.selectedCountry && countryMap.has(settings.selectedCountry)){
            relays = countryMap.get(settings.selectedCountry)!!
        } else if(settings.selectedFamily && familyMap.has(settings.selectedFamily)){
            relays = familyMap.get(settings.selectedFamily)!!
        }
        let stats: Statistics = {
            guard: 0,
            exit: 0,
            default: 0,
        }
        relays.forEach(relay => {
            if (relay.flags?.includes(RelayFlag.Exit)) {
                stats.exit++
            }else if( relay.flags?.includes(RelayFlag.Guard)){
                stats.guard++
            }else{
                stats.default++
            }
        })

        // true for forcing the calculation to include it in statistics
        if(settings.sortCountry || true){
            stats = {...stats, countryCount: countryMap.size}
            if(settings.selectedCountry) {
                stats = {...stats, countryRelayCount: countryMap.get(settings.selectedCountry)?.length}
            }
        }
        // true for forcing the calculation to include it in statistics
        if(settings.sortFamily || true){
            stats = {...stats, familyCount: familyMap.size}
            if(settings.selectedFamily) stats = {...stats, familyRelayCount: familyMap.get(settings.selectedFamily)?.length}
        }

        setStatisticsCallback(stats)

        console.timeLog(`relaysToLayerGroup`, `New Layer with ${relays.length} elements finished`)
        console.timeEnd(`relaysToLayerGroup`, )
        return layerToReturn
    }

    // Draws a group of layers to the map
    const drawLayerGroup = (layerGroup: LayerGroup) => {
        if (leafletMap && dayToDisplay) {
            console.log(`drawing for ${dayToDisplay}`)
            markerLayer.clearLayers()
            const layers = layerGroup.getLayers()
            layers.forEach((layer) => layer.addTo(markerLayer))
        }
    }

    // ToDo: fix handleUnselect
    const handleUnselect = () => {
        setSettingsCallback({...settings, selectedCountry: undefined, selectedFamily: undefined})
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
                newMap.on("contextmenu", handleUnselect)
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