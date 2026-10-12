import {GeoJSON, Path, Tooltip} from "leaflet";
import {describe, expect, it} from "vitest";

import {CountryUsageDto} from "../dto/relay";
import {buildCountryUsageLayer, describeUsage, formatUsers, usageColor} from "./country-usage";

const usage = (country: string, users: number, extra: Partial<CountryUsageDto> = {}): CountryUsageDto =>
    ({country, users, lower: null, upper: null, frac: 70, ...extra})

const featureLayers = (layer: GeoJSON) => layer.getLayers() as Path[]
const countryOf = (path: Path) => (path as unknown as { feature: { properties: { iso_a2: string } } }).feature.properties.iso_a2

describe("usageColor", () => {
    const lightness = (color: string) => Number(/,(\d+)%\)$/.exec(color)?.[1])

    it("gets lighter with more users", () => {
        const colors = [0, 10, 1000, 100000, 1000000].map(users => lightness(usageColor(users, 1000000)))
        expect([...colors].sort((a, b) => a - b)).toEqual(colors)
        expect(new Set(colors).size).toBe(colors.length)
    })

    it("uses the extreme colors for no users and the maximum", () => {
        expect(usageColor(0, 1000)).toBe("hsl(250,60%,30%)")
        expect(usageColor(1000, 1000)).toBe("hsl(50,95%,58%)")
    })

    it("clamps and handles a missing maximum", () => {
        expect(usageColor(5000, 1000)).toBe(usageColor(1000, 1000))
        expect(usageColor(-5, 1000)).toBe(usageColor(0, 1000))
        expect(usageColor(10, 0)).toBe(usageColor(0, 1000))
    })
})

describe("describeUsage", () => {
    it("shows the estimate", () => {
        expect(describeUsage("Germany", usage("DE", 12345))).toBe("Germany: about 12,345 users")
    })

    it("shows the range if there is one", () => {
        expect(describeUsage("Germany", usage("DE", 1000, {lower: 800, upper: 1200})))
            .toBe("Germany: about 1,000 users (probably between 800 and 1,200)")
    })

    it("treats a range starting at 0 as a range", () => {
        expect(describeUsage("Chad", usage("TD", 0, {lower: 0, upper: 20})))
            .toBe("Chad: about 0 users (probably between 0 and 20)")
    })
})

describe("formatUsers", () => {
    it("adds thousands separators", () => {
        expect(formatUsers(1234567)).toBe("1,234,567")
    })
})

describe("buildCountryUsageLayer", () => {
    it("draws only countries with an estimate", () => {
        const layer = buildCountryUsageLayer([usage("DE", 1000), usage("FR", 500)])
        expect(featureLayers(layer).map(countryOf).sort()).toEqual(["DE", "FR"])
    })

    it("ignores codes that are not on the map", () => {
        const layer = buildCountryUsageLayer([usage("DE", 1000), usage("ZZ", 5), usage("AN", 1)])
        expect(featureLayers(layer).map(countryOf)).toEqual(["DE"])
    })

    it("is empty without estimates", () => {
        expect(featureLayers(buildCountryUsageLayer([]))).toHaveLength(0)
    })

    it("colors countries relative to the highest estimate", () => {
        const layer = buildCountryUsageLayer([usage("DE", 100000), usage("FR", 10)])
        const fill = Object.fromEntries(featureLayers(layer).map(path => [countryOf(path), path.options.fillColor]))
        expect(fill.DE).toBe(usageColor(100000, 100000))
        expect(fill.FR).toBe(usageColor(10, 100000))
        expect(fill.DE).not.toBe(fill.FR)
    })

    it("draws in the pane below the relay markers", () => {
        const layer = buildCountryUsageLayer([usage("DE", 1)])
        expect(featureLayers(layer)[0].options.pane).toBe("countryUsage")
    })

    it("describes the estimate as plain text", () => {
        const layer = buildCountryUsageLayer([usage("DE", 1000, {lower: 900, upper: 1100})])
        const tooltip = featureLayers(layer)[0].getTooltip() as Tooltip
        const content = tooltip.getContent() as HTMLElement
        expect(content).toBeInstanceOf(HTMLElement)
        expect(content.textContent).toBe("Germany: about 1,000 users (probably between 900 and 1,100)")
        expect(content.children).toHaveLength(0)
    })
})
