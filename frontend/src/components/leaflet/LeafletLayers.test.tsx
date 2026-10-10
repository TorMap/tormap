import {CircleMarker, GeoJSON, Layer, Map as LeafletMap, Path} from "leaflet";
import {useAtomValue} from "jotai";
import {ReactNode, useEffect} from "react";
import {MapContainer, useMap} from "react-leaflet";
import {describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {useSettings} from "../../context/settings-context";
import {useStatistics} from "../../context/statistics-context";
import {RelayLocationDto} from "../../dto/relay";
import {makeFamilyIdentifier, makeRelay, makeRelayDetails, makeRelays, makeSettings} from "../../test/fixtures";
import {mockBackend} from "../../test/mock-backend";
import {renderWithProviders} from "../../test/render";
import {RelayFlag} from "../../types/relay";
import {Settings} from "../../types/settings";
import {SnackbarMessage} from "../../types/ui";
import {filteredRelaysAtom, LeafletLayers} from "./LeafletLayers";

let leafletMap: LeafletMap

// Gives the tests access to the Leaflet map that LeafletLayers draws on
const MapProbe = () => {
    const map = useMap()
    useEffect(() => {
        leafletMap = map
    }, [map])
    return null
}

const State = () => {
    const {settings} = useSettings()
    const {statistics} = useStatistics()
    const filtered = useAtomValue(filteredRelaysAtom)
    return (
        <output aria-label="state">{JSON.stringify({
            selectedCountry: settings.selectedCountry ?? null,
            selectedFamily: settings.selectedFamily ?? null,
            statistics,
            filtered: filtered.length,
        })}</output>
    )
}

// Everything LeafletLayers has drawn onto the map
const drawn = () => {
    const layers: Layer[] = []
    leafletMap.eachLayer(layer => layers.push(layer))
    return layers
}
const circles = () => drawn().filter((layer): layer is CircleMarker => layer instanceof CircleMarker)
const polygons = () => drawn().filter(layer => layer instanceof GeoJSON === false && "feature" in layer) as (Path & {
    feature: { properties: { iso_a2: string } }
})[]
// Family circles are the only ones drawn with a border weight of 1 (relay markers use 3, aggregates .3)
const familyCircles = () => circles().filter(circle => circle.options.weight === 1)
const hasHeatLayer = ()=> drawn().some(layer => "_latlngs" in layer)

// Polls the state output until it matches. (Regular expressions do not work with toHaveTextContent in browser mode.)
const expectState = (pattern: RegExp) =>
    expect.poll(() => page.getByRole("status", {name: "state"}).element().textContent).toMatch(pattern)

const renderLayers = (relays: RelayLocationDto[] | undefined, settings: Partial<Settings> = {}, extra?: ReactNode) => {
    const reloadSelectedDay = vi.fn()
    const screen = renderWithProviders(
        <>
            <MapContainer center={[30, 0]} zoom={3} style={{width: 800, height: 600}} preferCanvas={true}>
                <MapProbe/>
                <LeafletLayers relays={relays} reloadSelectedDay={reloadSelectedDay}/>
            </MapContainer>
            <State/>
            {extra}
        </>,
        {settings}
    )
    return {screen, reloadSelectedDay}
}

// Two relays in Germany at one place and one in France
const relays = [
    makeRelay({country: "DE", lat: 50, long: 8, flags: [RelayFlag.Exit], detailsId: 1, nickname: "de-exit"}),
    makeRelay({country: "DE", lat: 50, long: 8, flags: [RelayFlag.Guard], detailsId: 2, nickname: "de-guard"}),
    makeRelay({country: "FR", lat: 48, long: 2, detailsId: 3, nickname: "fr-other"}),
]

describe("LeafletLayers", () => {
    it("draws a marker and a hover target per location and reports the statistics", async () => {
        await renderLayers(relays)

        await expectState(/"relayGuardCount":1,"relayExitCount":1,"relayOtherCount":1/)
        await expectState(/"countryCount":2/)
        await expectState(/"filtered":3/)
        expect(circles()).toHaveLength(4)
    })

    it("draws nothing before relays are available", async () => {
        await renderLayers(undefined)

        await expectState(/"filtered":0/)
        expect(circles()).toHaveLength(0)
    })

    it("warns when the settings filter out every relay", async () => {
        const settings = makeSettings()
        await renderLayers([makeRelay({flags: []})], {relaysMustHaveFlag: {...settings.relaysMustHaveFlag, [RelayFlag.Exit]: true}})

        await expect.element(page.getByText(SnackbarMessage.NoRelaysWithFlags)).toBeVisible()
        expect(circles()).toHaveLength(0)
    })

    it("adds larger markers for crowded locations when aggregating", async () => {
        const crowded = makeRelays(5, {lat: 10, long: 10})
        await renderLayers([...crowded, makeRelay({lat: 20, long: 20})], {aggregateCoordinates: true})

        await expectState(/"filtered":6/)
        // 2 locations x (marker + hover target) + 1 aggregate circle
        expect(circles()).toHaveLength(5)
    })

    it("draws a heat layer", async () => {
        await renderLayers(relays, {heatMap: true})

        await expectState(/"filtered":3/)
        expect(hasHeatLayer()).toBe(true)
    })

    it("does not draw a heat layer by default", async () => {
        await renderLayers(relays)

        await expectState(/"filtered":3/)
        expect(hasHeatLayer()).toBe(false)
    })

    describe("grouping by country", () => {
        it("draws the countries that have relays and selects one on click", async () => {
            await renderLayers(relays, {sortCountry: true})
            await expectState(/"filtered":3/)

            expect(polygons().map(layer => layer.feature.properties.iso_a2).sort()).toEqual(["DE", "FR"])
            polygons().find(layer => layer.feature.properties.iso_a2 === "FR")!.fire("click")

            await expectState(/"selectedCountry":"FR"/)
            await expectState(/"filtered":1/)
        })

        it("forgets a selected country that has no relays", async () => {
            await renderLayers(relays, {sortCountry: true, selectedCountry: "US"})

            await expectState(/"selectedCountry":null/)
        })
    })

    describe("grouping by family", () => {
        const familyRelays = [
            makeRelay({lat: 1, long: 1, familyId: 10, detailsId: 11}),
            makeRelay({lat: 1, long: 1, familyId: 10, detailsId: 12}),
            makeRelay({lat: 2, long: 2, familyId: 20, detailsId: 21}),
            makeRelay({lat: 2, long: 2, familyId: 30, detailsId: 31}),
        ]

        it("warns when there is no family data", async () => {
            await renderLayers(relays, {sortFamily: true})

            await expect.element(page.getByText(SnackbarMessage.NoFamilyData)).toBeVisible()
        })

        it("selects a family directly when it is alone at its location", async () => {
            await renderLayers(familyRelays, {sortFamily: true})
            await expectState(/"filtered":4/)

            familyCircles().find(circle => circle.options.className === "1,1")!.fire("click")

            await expectState(/"selectedFamily":10/)
        })

        it("lets the user pick between families that share a location", async () => {
            mockBackend.onPost("/relay/details/family/identifiers", [
                makeFamilyIdentifier({id: 20, nicknames: "twenty"}),
                makeFamilyIdentifier({id: 30, nicknames: "thirty"}),
            ])
            await renderLayers(familyRelays, {sortFamily: true})
            await expectState(/"filtered":4/)

            familyCircles().find(circle => circle.options.className === "2,2")!.fire("click")

            await expect.element(page.getByText("thirty")).toBeVisible()
            expect(mockBackend.requests.at(-1)?.data).toEqual([20, 30])
            await page.getByText("thirty").click()
            await expectState(/"selectedFamily":30/)
        })

        it("forgets a selected family that has no relays", async () => {
            await renderLayers(familyRelays, {sortFamily: true, selectedFamily: 99})

            await expectState(/"selectedFamily":null/)
        })

        it("draws only the selected family and deselects it on click", async () => {
            await renderLayers(familyRelays, {sortFamily: true, selectedFamily: 10})
            await expectState(/"selectedFamily":10/)

            familyCircles()[0].fire("click")

            await expectState(/"selectedFamily":null/)
        })
    })

    describe("marker click", () => {
        it("opens the details of a single relay", async () => {
            await page.viewport(1400, 900)
            mockBackend.onGet("/relay/details/relay/3", makeRelayDetails({id: 3, nickname: "fr-other"}))
            await renderLayers(relays)
            await expectState(/"filtered":3/)

            circles().find(circle => circle.options.className === "48,2")!.fire("click")

            await expect.element(page.getByRole("heading", {name: "Relay's nickname"})).toHaveTextContent("fr-other")
        })
    })
})
