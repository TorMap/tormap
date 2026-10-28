import {canvas, GeoJSON, GeoJSONOptions, PathOptions, svg} from "leaflet";
import {FunctionComponent, useEffect, useState} from "react";

import {CountryFeature, loadWorldFeatures} from "../../util/geojson";
import {useMap} from "./map";

export const offlineBasemapPane = "offlineBasemap"
// Above the tiles (200), below the country usage (350) and the relays (overlay pane, 400)
const offlineBasemapPaneZIndex = 250

/**
 * Draws the bundled country borders while the browser is offline, in place of the map tiles. The borders are part of
 * the app shell, so they are available offline, and the CARTO tiles are never downloaded in advance.
 */
export const OfflineBasemap: FunctionComponent = () => {
    const map = useMap()
    const [worldFeatures, setWorldFeatures] = useState<CountryFeature[]>()

    useEffect(() => {
        let cancelled = false
        loadWorldFeatures().then(features => {
            if (!cancelled) setWorldFeatures(features)
        }).catch(() => {
            // Then the relays are shown on the plain background
        })
        return () => {
            cancelled = true
        }
    }, [])

    useEffect(() => {
        if (!worldFeatures) return
        if (!map.getPane(offlineBasemapPane)) {
            map.createPane(offlineBasemapPane).style.zIndex = String(offlineBasemapPaneZIndex)
        }
        // An own renderer, as Leaflet would pick it, so it is removed with the layer instead of staying in the pane
        const renderer = map.options.preferCanvas ? canvas({pane: offlineBasemapPane}) : svg({pane: offlineBasemapPane})
        // GeoJSON passes the renderer on to its paths, its types only miss the option
        const options: GeoJSONOptions & PathOptions = {
            pane: offlineBasemapPane,
            renderer,
            interactive: false,
            // Land a little lighter than the water color of the map background
            style: {color: "#5b666e", weight: 0.7, fillColor: "#3a444b", fillOpacity: 1},
        }
        const layer = new GeoJSON(undefined, options)
        worldFeatures.forEach(feature => layer.addData(feature))
        layer.addTo(map)
        return () => {
            layer.remove()
            renderer.remove()
        }
    }, [map, worldFeatures])

    return null
}
