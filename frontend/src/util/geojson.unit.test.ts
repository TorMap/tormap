import {describe, expect, it} from "vitest";

import countryMeta from "../resources/country-meta.json";
import worldGeoData from "../resources/world.geo.json";
import {getFullCountryName, getUniqueCountryColor, loadWorldFeatures} from "./geojson";

describe("getUniqueCountryColor", () => {
    it("returns the map color of a known country", () => {
        expect(getUniqueCountryColor("DE")).toBe(5)
        expect(getUniqueCountryColor("FR")).toBe(9)
    })

    it("returns 0 for unknown countries", () => {
        expect(getUniqueCountryColor("ZZ")).toBe(0)
    })
})

describe("countries without a code", () => {
    it("uses the first country of a shared code, like before the meta data was split off", () => {
        expect(getFullCountryName("-99")).toBe("N. Cyprus")
        expect(getUniqueCountryColor("-99")).toBe(4)
    })
})

describe("getFullCountryName", () => {
    it("returns the name of a known country", () => {
        expect(getFullCountryName("DE")).toBe("Germany")
    })

    it("falls back to the country code for unknown countries", () => {
        expect(getFullCountryName("ZZ")).toBe("ZZ")
    })
})

describe("country meta data", () => {
    it("has an entry for every country border, in the same order", () => {
        expect(countryMeta.map(country => country.iso_a2))
            .toEqual(worldGeoData.features.map(feature => feature.properties.iso_a2))
    })

    it("has a name and a map color for every country", () => {
        countryMeta.forEach(country => {
            expect(country.name).not.toBe("")
            expect(country.mapcolor9).toBeGreaterThanOrEqual(1)
        })
    })
})

describe("loadWorldFeatures", () => {
    it("loads the country borders once and shares them", async () => {
        const features = await loadWorldFeatures()
        expect(features.length).toBe(countryMeta.length)
        expect(features.find(feature => feature.properties.iso_a2 === "DE")?.geometry).toBeDefined()
        expect(await loadWorldFeatures()).toBe(features)
    })
})
