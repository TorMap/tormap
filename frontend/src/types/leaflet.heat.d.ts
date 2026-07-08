import "leaflet";

declare module "leaflet" {
    export type HeatLatLngTuple = [number, number, number];

    export interface ColorGradientConfig {
        [key: number]: string;
    }

    export interface HeatMapOptions {
        minOpacity?: number | undefined;
        maxZoom?: number | undefined;
        max?: number | undefined;
        radius?: number | undefined;
        blur?: number | undefined;
        gradient?: ColorGradientConfig | undefined;
    }

    export interface HeatLayer extends TileLayer {
        setOptions(options: HeatMapOptions): HeatLayer;
        addLatLng(latlng: LatLng | HeatLatLngTuple): HeatLayer;
        setLatLngs(latlngs: Array<LatLng | HeatLatLngTuple>): HeatLayer;
    }

    export function heatLayer(latlngs: Array<LatLng | HeatLatLngTuple>, options: HeatMapOptions): HeatLayer;
}
