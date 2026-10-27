/*
 React bindings for the parts of Leaflet that TorMap uses, replacing react-leaflet 5.0.0. Same names and behavior:
 the map is created once when its container mounts, its children only render once the map exists, and it is removed
 on unmount. Props of MapContainer are only read on creation, like in react-leaflet.
 */
import {LeafletEventHandlerFnMap, Map as LeafletMap, MapOptions, TileLayer as LeafletTileLayer, TileLayerOptions} from "leaflet";
import {createContext, CSSProperties, ReactNode, use, useCallback, useEffect, useRef, useState} from "react";

const MapContext = createContext<LeafletMap | null>(null)

interface MapContainerProps extends MapOptions {
    children?: ReactNode
    style?: CSSProperties
}

export const MapContainer = ({children, style, center, zoom, ...options}: MapContainerProps) => {
    const [map, setMap] = useState<LeafletMap | null>(null)
    const mapRef = useRef<LeafletMap | null>(null)
    // A callback ref instead of an effect, so the map exists before the effects of the children run
    const containerRef = useCallback((node: HTMLDivElement | null) => {
        if (node !== null && mapRef.current === null) {
            const created = new LeafletMap(node, options)
            if (center != null && zoom != null) {
                created.setView(center, zoom)
            }
            mapRef.current = created
            setMap(created)
        }
        // Only the initial props are used
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])
    useEffect(() => () => {
        map?.remove()
    }, [map])
    // Only the initial style is applied, Leaflet manages the container afterwards
    const [initialStyle] = useState(style)
    return (
        <div ref={containerRef} style={initialStyle}>
            {map && <MapContext value={map}>{children}</MapContext>}
        </div>
    )
}

/**
 * The Leaflet map of the enclosing MapContainer.
 */
export function useMap(): LeafletMap {
    const map = use(MapContext)
    if (map === null) {
        throw new Error("useMap() can only be used in a descendant of <MapContainer>")
    }
    return map
}

/**
 * Registers the handlers on the map while the component is mounted.
 */
export function useMapEvents(handlers: LeafletEventHandlerFnMap): LeafletMap {
    const map = useMap()
    useEffect(() => {
        map.on(handlers)
        return () => {
            map.off(handlers)
        }
    }, [map, handlers])
    return map
}

interface TileLayerProps extends TileLayerOptions {
    url: string
}

/**
 * Adds a tile layer to the map while mounted. The layer is created once; later prop changes are ignored.
 */
export const TileLayer = ({url, ...options}: TileLayerProps) => {
    const map = useMap()
    const [initial] = useState({url, options})
    useEffect(() => {
        const layer = new LeafletTileLayer(initial.url, initial.options).addTo(map)
        return () => {
            layer.remove()
        }
    }, [map, initial])
    return null
}
