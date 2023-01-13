import worldGeoData from "../resources/world.geo.json";

/**
 * Returns a color for a country so that no touching countries have the same color
 * @param iso_a2 The ISO-3166-A2 Country code
 */
export function getUniqueCountryColor(iso_a2: string): number {
    return worldGeoData.features.find(feature => feature.properties.iso_a2 === iso_a2)?.properties.mapcolor9 ?? 0
}

/**
 * Returns the name of a country with according country code
 * @param iso_a2 The ISO-3166-A2 Country code
 */
export function getFullCountryName(iso_a2: string): string {
    return worldGeoData.features.find(feature => feature.properties.iso_a2 === iso_a2)?.properties.name ?? iso_a2
}
