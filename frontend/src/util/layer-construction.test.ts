import {CircleMarker, GeoJSON, Layer, LayerGroup, LeafletMouseEvent, Path} from "leaflet";
import {describe, expect, it, vi} from "vitest";

import {RelayLocationDto} from "../dto/relay";
import {makeRelay, makeRelays, makeSettings} from "../test/fixtures";
import {RelayFlag, RelayType} from "../types/relay";
import {buildFamilyCoordinatesMap, buildRelayCoordinatesMap, buildRelayCountryMap, buildRelayFamilyMap} from "./aggregate-relays";
import {getUniqueCountryColor} from "./geojson";
import {
    buildAggregatedCoordinatesLayer,
    buildCountryLayer,
    buildFamilyCoordinatesLayer,
    buildRelayCountryLayer,
    buildRelayHeatmapLayer,
    buildRelayLayer,
    buildSelectedFamilyLayer,
    calculateFamilyColor,
} from "./layer-construction";

const markers = (layer: LayerGroup) => layer.getLayers() as CircleMarker[]

describe("buildAggregatedCoordinatesLayer", () => {
    it("skips coordinates with fewer than 4 relays", () => {
        const map = new Map([["1,1", makeRelays(3)], ["2,2", makeRelays(4)]])
        const layer = markers(buildAggregatedCoordinatesLayer(map, vi.fn()))
        expect(layer).toHaveLength(1)
        expect(layer[0].getLatLng()).toMatchObject({lat: 2, lng: 2})
        expect(layer[0].options.className).toBe("2,2")
    })

    it.each([
        [4, 10],
        [9, 10],
        [10, 10],
        [55, 30],
        [100, 50],
        [150, 60],
        [200, 70],
        [250, 85],
        [300, 100],
        [1000, 100],
    ])("scales the radius for %i relays to %d", (count, radius) => {
        const layer = markers(buildAggregatedCoordinatesLayer(new Map([["1,1", makeRelays(count)]]), vi.fn()))
        expect(layer[0].getRadius()).toBeCloseTo(radius)
    })

    it("calls the click handler", () => {
        const onClick = vi.fn()
        const [marker] = markers(buildAggregatedCoordinatesLayer(new Map([["1,1", makeRelays(4)]]), onClick))
        marker.fire("click")
        expect(onClick).toHaveBeenCalledOnce()
    })
})

describe("buildRelayLayer", () => {
    const colors = {
        [RelayType.Exit]: "#000001",
        [RelayType.Guard]: "#000002",
        [RelayType.Other]: "#000003",
    }

    // Each coordinate yields a visible marker followed by an invisible, larger hover target
    const build = (relays: RelayLocationDto[], singleColor = false, onClick = vi.fn()) => {
        const layer = buildRelayLayer(buildRelayCoordinatesMap(relays), singleColor, colors, onClick)
        return {layer: markers(layer), onClick}
    }

    it("adds a marker and a hover target per coordinate", () => {
        const {layer} = build([makeRelay({lat: 1, long: 1}), makeRelay({lat: 2, long: 2})])
        expect(layer).toHaveLength(4)
    })

    it("adds nothing for no relays", () => {
        expect(build([]).layer).toHaveLength(0)
    })

    it("skips coordinates that have no relays", () => {
        const layer = buildRelayLayer(new Map([["1,1", []]]), false, colors, vi.fn())
        expect(layer.getLayers()).toHaveLength(0)
    })

    it.each([
        ["exit", [RelayFlag.Exit], colors[RelayType.Exit]],
        ["guard", [RelayFlag.Guard], colors[RelayType.Guard]],
        ["other", [RelayFlag.Fast], colors[RelayType.Other]],
    ])("colors a %s relay with its relay type color", (_name, flags, color) => {
        const {layer} = build([makeRelay({flags})])
        expect(layer[0].options.color).toBe(color)
        expect(layer[0].options.className).toBe("0,0")
    })

    it("uses the most important relay at a shared coordinate", () => {
        const guardAndExit = [
            makeRelay({flags: []}),
            makeRelay({flags: [RelayFlag.Guard]}),
            makeRelay({flags: [RelayFlag.Exit]}),
            makeRelay({flags: [RelayFlag.Guard]}),
        ]
        expect(build(guardAndExit).layer[0].options.color).toBe(colors[RelayType.Exit])
        expect(build(guardAndExit.slice(0, 2)).layer[0].options.color).toBe(colors[RelayType.Guard])
    })

    it("uses one color when singleColor is set", () => {
        const {layer} = build([makeRelay({flags: [RelayFlag.Exit]})], true)
        expect(layer[0].options.color).toBe("#989898")
    })

    it("calls the click handler for the marker and forwards hover target clicks", () => {
        const {layer, onClick} = build([makeRelay()])
        layer[0].fire("click")
        expect(onClick).toHaveBeenCalledTimes(1)
        layer[1].fire("click")
        expect(onClick).toHaveBeenCalledTimes(2)
    })

    describe("tooltip", () => {
        const tooltipAfterHover = (relays: RelayLocationDto[]) => {
            const {layer} = build(relays)
            layer[1].fire("mouseover")
            return {marker: layer[0], hover: layer[1], tooltip: layer[0].getTooltip()}
        }

        it("lists up to three nicknames", () => {
            const relays = ["a", "b", "c"].map(nickname => makeRelay({nickname}))
            expect(tooltipAfterHover(relays).tooltip?.getContent()).toBe("a, b, c")
        })

        it("summarizes more than three relays", () => {
            expect(tooltipAfterHover(makeRelays(4)).tooltip?.getContent()).toBe("4 relays")
        })

        it("summarizes relays without nicknames", () => {
            const relays = [makeRelay({nickname: ""}), makeRelay({nickname: ""})]
            expect(tooltipAfterHover(relays).tooltip?.getContent()).toBe("2 relays")
        })

        it("removes the tooltip on mouseout", () => {
            const {marker, hover} = tooltipAfterHover([makeRelay({nickname: "a"})])
            expect(marker.getTooltip()).toBeTruthy()
            hover.fire("mouseout")
            expect(marker.getTooltip()).toBeFalsy()
        })
    })
})

describe("buildSelectedFamilyLayer", () => {
    const family1 = [makeRelay({lat: 1, long: 1, familyId: 1}), makeRelay({lat: 2, long: 2, familyId: 1})]
    const family2 = [makeRelay({lat: 3, long: 3, familyId: 2})]
    const familyMap = buildRelayFamilyMap([...family1, ...family2])

    it("draws only the selected family", () => {
        const layer = markers(buildSelectedFamilyLayer(familyMap, makeSettings({selectedFamily: 1}), vi.fn()))
        expect(layer).toHaveLength(2)
        expect(layer[0].options.color).toBe(calculateFamilyColor(1))
    })

    it("draws nothing without a selection", () => {
        expect(buildSelectedFamilyLayer(familyMap, makeSettings(), vi.fn()).getLayers()).toHaveLength(0)
    })

    it("deselects the family on click", () => {
        const settings = makeSettings({selectedFamily: 1})
        const setSettings = vi.fn()
        markers(buildSelectedFamilyLayer(familyMap, settings, setSettings))[0].fire("click")
        expect(setSettings).toHaveBeenCalledWith({...settings, selectedFamily: undefined})
    })
})

describe("buildFamilyCoordinatesLayer", () => {
    const build = (relays: RelayLocationDto[], selectedFamily?: number) => {
        const settings = makeSettings({selectedFamily})
        const callbacks = {setSettings: vi.fn(), setFamilies: vi.fn(), setShowDialog: vi.fn()}
        const famCordMap = buildFamilyCoordinatesMap(buildRelayCoordinatesMap(relays))
        const layer = markers(buildFamilyCoordinatesLayer(
            famCordMap, settings, callbacks.setSettings, callbacks.setFamilies, callbacks.setShowDialog
        ))
        return {layer, settings, ...callbacks}
    }
    const twoFamiliesAtOnePlace = [
        ...makeRelays(3, {lat: 1, long: 1, familyId: 1}),
        ...makeRelays(1, {lat: 1, long: 1, familyId: 2}),
    ]

    it("draws one circle per family at a coordinate, largest first", () => {
        const {layer} = build(twoFamiliesAtOnePlace)
        expect(layer).toHaveLength(2)
        expect(layer[0].options.color).toBe(calculateFamilyColor(1))
        expect(layer[1].options.color).toBe(calculateFamilyColor(2))
        expect(layer[0].options.className).toBe("1,1")
        expect(layer.every(circle => circle.options.fillOpacity === 0.2)).toBe(true)
    })

    it("hides families that are not selected", () => {
        const {layer} = build(twoFamiliesAtOnePlace, 1)
        expect(layer[0].options.fillOpacity).toBe(0.2)
        expect(layer[1].options.fillOpacity).toBe(0)
    })

    it("opens the selection dialog when several families share a coordinate", () => {
        const {layer, setFamilies, setShowDialog, setSettings} = build(twoFamiliesAtOnePlace)
        layer[0].fire("click")
        expect(setFamilies).toHaveBeenCalledWith([1, 2])
        expect(setShowDialog).toHaveBeenCalledWith(true)
        expect(setSettings).not.toHaveBeenCalled()
    })

    it("selects the family directly when it is the only one at the coordinate", () => {
        const {layer, settings, setSettings, setShowDialog} = build(makeRelays(2, {familyId: 5}))
        layer[0].fire("click")
        expect(setSettings).toHaveBeenCalledWith({...settings, selectedFamily: 5})
        expect(setShowDialog).not.toHaveBeenCalled()
    })

    it("clears the selection when a family is already selected", () => {
        const {layer, settings, setSettings} = build(makeRelays(2, {familyId: 5}), 5)
        layer[0].fire("click")
        expect(setSettings).toHaveBeenCalledWith({...settings, selectedFamily: undefined})
    })
})

describe("calculateFamilyColor", () => {
    it("derives the hue from the family id in 8 steps", () => {
        expect(calculateFamilyColor(0)).toBe("hsl(0,90%,60%)")
        expect(calculateFamilyColor(1)).toBe("hsl(45,90%,60%)")
        expect(calculateFamilyColor(7)).toBe("hsl(315,90%,60%)")
        expect(calculateFamilyColor(8)).toBe(calculateFamilyColor(0))
    })
})

describe("buildRelayHeatmapLayer", () => {
    it("creates a heat layer with a point per relay", () => {
        const layer = buildRelayHeatmapLayer([makeRelay({lat: 1, long: 2}), makeRelay({lat: 3, long: 4})])
        const heat = layer as unknown as {_latlngs: number[][], options: {radius: number}}
        expect(heat._latlngs).toEqual([[1, 2, 1], [3, 4, 1]])
        expect(heat.options.radius).toBe(25)
    })
})

describe("buildRelayCountryLayer", () => {
    const relays = [
        makeRelay({country: "DE", lat: 1, long: 1}),
        makeRelay({country: "DE", lat: 2, long: 2}),
        makeRelay({country: "FR", lat: 3, long: 3}),
    ]
    const countryMap = buildRelayCountryMap(relays)
    const hueOf = (country: string) => getUniqueCountryColor(country) * 360 / 9

    it("draws a marker per relay in the color of its country", () => {
        const layer = markers(buildRelayCountryLayer(countryMap, makeSettings(), vi.fn()))
        expect(layer).toHaveLength(3)
        expect(layer[0].options.color).toBe(`hsl(${hueOf("DE")},90%,60%)`)
        expect(layer[2].options.color).toBe(`hsl(${hueOf("FR")},90%,60%)`)
        expect(layer[2].options.className).toBe("3,3")
    })

    it("greys out countries that are not selected", () => {
        const layer = markers(buildRelayCountryLayer(countryMap, makeSettings({selectedCountry: "DE"}), vi.fn()))
        expect(layer[0].options.color).toBe(`hsl(${hueOf("DE")},90%,60%)`)
        expect(layer[2].options.color).toBe(`hsl(${hueOf("FR")},0%,60%)`)
    })

    it("calls the click handler", () => {
        const onClick = vi.fn<(e: LeafletMouseEvent) => void>()
        markers(buildRelayCountryLayer(countryMap, makeSettings(), onClick))[0].fire("click")
        expect(onClick).toHaveBeenCalledOnce()
    })
})

describe("buildCountryLayer", () => {
    const countryMap = buildRelayCountryMap([makeRelay({country: "DE"}), makeRelay({country: "FR"})])
    const countryOf = (layer: Layer) => (layer as Layer & {feature: {properties: {iso_a2: string}}}).feature.properties.iso_a2
    const build = (selectedCountry?: string) => {
        const setSettings = vi.fn()
        const settings = makeSettings({selectedCountry})
        const layer = buildCountryLayer(countryMap, settings, setSettings) as GeoJSON
        return {layers: layer.getLayers() as Path[], settings, setSettings}
    }

    it("only contains countries that have relays", () => {
        const {layers} = build()
        expect(layers.map(countryOf).sort()).toEqual(["DE", "FR"])
    })

    it("highlights the selected country", () => {
        const {layers} = build("DE")
        const fillOf = (iso: string) => layers.find(l => countryOf(l) === iso)!.options.fillColor
        expect(fillOf("DE")).toBe("rgba(255,255,255,0.7)")
        expect(fillOf("FR")).toBe("rgba(255,255,255,0.3)")
    })

    it("selects a country on click", () => {
        const {layers, settings, setSettings} = build()
        layers.find(l => countryOf(l) === "FR")!.fire("click")
        expect(setSettings).toHaveBeenCalledWith({...settings, selectedCountry: "FR"})
    })

    it("deselects the country when the selected one is clicked", () => {
        const {layers, settings, setSettings} = build("DE")
        layers.find(l => countryOf(l) === "DE")!.fire("click")
        expect(setSettings).toHaveBeenCalledWith({...settings, selectedCountry: undefined})
    })
})
