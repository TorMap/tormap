/*
 Minimal React bindings for the Leaflet API that TorMap uses: a map container that provides its L.Map to the
 components inside it, a hook to register map events, and a tile layer. Written against Leaflet's documented API
 (https://leafletjs.com/reference.html).
 */
import L from "leaflet";
import {createContext, CSSProperties, ReactNode, use, useEffect, useLayoutEffect, useRef, useState} from "react";

const LeafletMapContext = createContext<L.Map | undefined>(undefined)

interface MapContainerProps extends L.MapOptions {
    children?: ReactNode
    style?: CSSProperties
}

/**
 * Renders a div with a Leaflet map and, once the map exists, the children, which can use it with useMap().
 * The style and map options are taken from the first render. Leaflet changes the view itself afterwards.
 */
export const MapContainer = ({children, style, ...mapOptions}: MapContainerProps) => {
    const element = useRef<HTMLDivElement>(null)
    const [initial] = useState(() => ({style, mapOptions}))
    const [leafletMap, setLeafletMap] = useState<L.Map>()

    // A layout effect, so the map is attached before the browser paints the empty div.
    // Leaflet sets the initial view itself when the options contain both center and zoom.
    useLayoutEffect(() => {
        const created = L.map(element.current!, initial.mapOptions)
        setLeafletMap(created)
        return () => {
            setLeafletMap(undefined)
            created.remove()
        }
    }, [initial])

    return (
        <div ref={element} style={initial.style}>
            {leafletMap && <LeafletMapContext value={leafletMap}>{children}</LeafletMapContext>}
        </div>
    )
}

/**
 * The Leaflet map of the surrounding MapContainer.
 */
export function useMap(): L.Map {
    const leafletMap = use(LeafletMapContext)
    if (!leafletMap) {
        throw new Error("useMap() can only be used in a descendant of <MapContainer>")
    }
    return leafletMap
}

/**
 * Listens to the given map events while the calling component is mounted, and returns the map.
 */
export function useMapEvents(handlers: L.LeafletEventHandlerFnMap): L.Map {
    const leafletMap = useMap()
    useEffect(() => {
        const entries = Object.entries(handlers)
        for (const [type, handler] of entries) leafletMap.addEventListener(type, handler)
        return () => {
            for (const [type, handler] of entries) leafletMap.removeEventListener(type, handler)
        }
    }, [leafletMap, handlers])
    return leafletMap
}

interface TileLayerProps extends L.TileLayerOptions {
    url: string
}

/**
 * Shows a tile layer on the map while mounted. The url and options are taken from the first render.
 */
export const TileLayer = ({url, ...options}: TileLayerProps) => {
    const leafletMap = useMap()
    const [initial] = useState(() => ({url, options}))
    useEffect(() => {
        const layer = L.tileLayer(initial.url, initial.options).addTo(leafletMap)
        return () => {
            layer.remove()
        }
    }, [leafletMap, initial])
    return null
}
