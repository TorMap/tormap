import {GeoJSON, Layer, PathOptions} from "leaflet";
import {Feature, GeoJsonObject, GeometryObject} from "geojson";

import {CountryUsageDto} from "../dto/relay";
import worldGeoData from "../resources/world.geo.json";

/**
 * The name of the map pane of the country usage layer. It is placed below the relay markers.
 */
export const countryUsagePane = "countryUsage"
export const countryUsagePaneZIndex = 350

const lowColor = {hue: 250, saturation: 60, lightness: 30}
const highColor = {hue: 50, saturation: 95, lightness: 58}

/**
 * Returns the color for a number of users. The scale is logarithmic, because the number of users differs by orders of
 * magnitude between countries.
 * @param users - The estimated number of users of a country
 * @param maxUsers - The highest number of users of all countries
 */
export function usageColor(users: number, maxUsers: number): string {
    const share = maxUsers > 0 ? Math.log10(Math.max(users, 0) + 1) / Math.log10(maxUsers + 1) : 0
    const t = Math.min(Math.max(share, 0), 1)
    const mix = (low: number, high: number) => Math.round(low + (high - low) * t)
    return `hsl(${mix(lowColor.hue, highColor.hue)},${mix(lowColor.saturation, highColor.saturation)}%,${mix(lowColor.lightness, highColor.lightness)}%)`
}

/**
 * The CSS gradient from the color of few users to the color of many users
 */
export const usageColorGradient = `linear-gradient(to right, ${usageColor(1, 1000000)}, ${usageColor(1000, 1000000)}, ${usageColor(1000000, 1000000)})`

/**
 * Formats a number of users with thousands separators, e.g. 12345 as "12,345"
 */
export const formatUsers = (users: number): string => users.toLocaleString("en-US")

/**
 * Describes the estimate of a country as text, including the range if Tor Metrics provides one
 */
export function describeUsage(name: string, usage: CountryUsageDto): string {
    const range = usage.lower !== null && usage.lower !== undefined && usage.upper !== null && usage.upper !== undefined
        ? ` (probably between ${formatUsers(usage.lower)} and ${formatUsers(usage.upper)})`
        : ""
    return `${name}: about ${formatUsers(usage.users)} users${range}`
}

/**
 * Returns a layer that colors every country with an estimate by its number of users. Countries are only drawn if they
 * exist in the bundled map data.
 * @param usage - The estimates of a day
 */
export function buildCountryUsageLayer(usage: CountryUsageDto[]): GeoJSON {
    const usageByCountry = new Map(usage.map(entry => [entry.country, entry]))
    const maxUsers = usage.reduce((max, entry) => Math.max(max, entry.users), 0)

    const layer = new GeoJSON(undefined, {
        pane: countryUsagePane,
        style: ((feature: Feature<GeometryObject>) => {
            const entry = usageByCountry.get(feature.properties?.iso_a2)
            return {
                fillColor: usageColor(entry?.users ?? 0, maxUsers),
                fillOpacity: 0.6,
                color: "#ffffff",
                weight: 0.5,
            }
        }) as unknown as (feature?: Feature<GeometryObject>) => PathOptions,
        onEachFeature(feature: Feature<GeometryObject>, featureLayer: Layer) {
            const entry = usageByCountry.get(feature.properties?.iso_a2)
            if (!entry) return
            // A text node instead of a HTML string, so that nothing is interpreted as markup
            const content = document.createElement("div")
            content.textContent = describeUsage(feature.properties?.name ?? entry.country, entry)
            featureLayer.bindTooltip(content, {sticky: true})
        },
    })
    worldGeoData.features.forEach(feature => {
        if (usageByCountry.has(feature.properties.iso_a2)) {
            layer.addData(feature as GeoJsonObject)
        }
    })
    return layer
}
