import type {Feature, GeometryObject} from "geojson";

import countryMeta from "../resources/country-meta.json";

/**
 * The name and map color of every country, from the Natural Earth data that `world.geo.json` was made from. The borders
 * only carry the country code and have coordinates rounded to 4 decimals (about 11 m), and they are loaded on demand.
 */
const countryMetaByIso = new Map<string, (typeof countryMeta)[number]>()
// Unknown countries (N. Cyprus, Somaliland, Kosovo) share the code "-99", the first one wins
countryMeta.forEach(country => {
    if (!countryMetaByIso.has(country.iso_a2)) countryMetaByIso.set(country.iso_a2, country)
})

/**
 * Returns a color for a country so that no touching countries have the same color
 * @param iso_a2 The ISO-3166-A2 Country code
 */
export function getUniqueCountryColor(iso_a2: string): number {
    return countryMetaByIso.get(iso_a2)?.mapcolor9 ?? 0
}

/**
 * Returns the name of a country with according country code
 * @param iso_a2 The ISO-3166-A2 Country code
 */
export function getFullCountryName(iso_a2: string): string {
    return countryMetaByIso.get(iso_a2)?.name ?? iso_a2
}

export type CountryFeature = Feature<GeometryObject, { iso_a2: string }>

let worldFeatures: Promise<CountryFeature[]> | undefined

/**
 * Loads the country borders on first use, as they are a separate chunk of the build. Later calls share the result.
 */
export function loadWorldFeatures(): Promise<CountryFeature[]> {
    worldFeatures ??= import("../resources/world.geo.json")
        .then(module => module.default.features as unknown as CountryFeature[])
        .catch(error => {
            // Allow a retry after a failed (network) load
            worldFeatures = undefined
            throw error
        })
    return worldFeatures
}
