import worldGeoData from "../data/world.geo.json";

/**
 * Returns a color for a country so that no touching countries have the same color
 * @param iso_a2 The ISO-3166-A2 Country code
 */
export function getMapColor9(iso_a2: string): number {
    const geoData = worldGeoData
    const mapColor9 = geoData.features.find(feature => feature.properties.iso_a2 === iso_a2)?.properties.mapcolor9
    if (mapColor9 !== undefined) return mapColor9
    return 0
}

/**
 * Returns the name of a country with according country code
 * @param iso_a2 The ISO-3166-A2 Country code
 */
export function getFullName(iso_a2: string): string {
    const geoData = worldGeoData
    const name = geoData.features.find(feature => feature.properties.iso_a2 === iso_a2)?.properties.name
    if (name !== undefined) return name
    return iso_a2
}
