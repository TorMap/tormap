import {describe, expect, it} from "vitest";

import {getFullCountryName, getUniqueCountryColor} from "./geojson";

describe("getUniqueCountryColor", () => {
    it("returns the map color of a known country", () => {
        expect(getUniqueCountryColor("DE")).toBe(5)
        expect(getUniqueCountryColor("FR")).toBe(9)
    })

    it("returns 0 for unknown countries", () => {
        expect(getUniqueCountryColor("ZZ")).toBe(0)
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
