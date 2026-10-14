import {useAtom} from "jotai";
import {FunctionComponent, useEffect} from "react";
import {describe, expect, it} from "vitest";
import {page} from "vitest/browser";

import {defaultSettings} from "../../config";
import {useSettings} from "../../context/settings-context";
import {RelayLocationDto} from "../../dto/relay";
import {makeRelay} from "../../test/fixtures";
import {renderWithProviders} from "../../test/render";
import {RelayType} from "../../types/relay";
import {relaySearchOnMapAtom} from "../dialogs/relay/relay-details-atoms";
import {CountryUsage, countryUsageAtom} from "../leaflet/country-usage-atoms";
import {filteredRelaysAtom} from "../leaflet/LeafletLayers";
import {AppSettings} from "./AppSettings";
import {MapStats} from "./MapStats";

const State = () => {
    const {settings} = useSettings()
    return <output aria-label="state">{JSON.stringify(settings.relayTypeColors)}</output>
}

const relayTypeColors = () =>
    JSON.parse(page.getByRole("status", {name: "state"}).element().textContent ?? "{}")

const colorInput = (name: string) => page.getByLabelText(name).element() as HTMLInputElement

/**
 * Simulates the native color picker: input events while dragging, a change event once a color was picked
 */
const pickColor = (input: HTMLInputElement, color: string, commit = true) => {
    input.value = color
    input.dispatchEvent(new Event("input", {bubbles: true}))
    if (commit) input.dispatchEvent(new Event("change", {bubbles: true}))
}

const renderAppSettings = () => renderWithProviders(<><AppSettings elevation={0}/><State/></>)

describe("AppSettings relay type colors", () => {
    it("shows a color input with the current color per relay type", async () => {
        await renderAppSettings()
        expect(colorInput("Exit relay color").value).toBe(defaultSettings.relayTypeColors[RelayType.Exit])
        expect(colorInput("Guard relay color").value).toBe(defaultSettings.relayTypeColors[RelayType.Guard])
        expect(colorInput("Other relay color").value).toBe(defaultSettings.relayTypeColors[RelayType.Other])
        await expect.element(page.getByRole("button", {name: "Reset colors"})).not.toBeInTheDocument()
    })

    it("updates the setting once a color was picked", async () => {
        await renderAppSettings()
        const input = colorInput("Guard relay color")

        pickColor(input, "#123456", false)
        expect(input.value).toBe("#123456")
        expect(relayTypeColors()[RelayType.Guard]).toBe(defaultSettings.relayTypeColors[RelayType.Guard])

        pickColor(input, "#123456")
        await expect.poll(() => relayTypeColors()[RelayType.Guard]).toBe("#123456")
        expect(relayTypeColors()[RelayType.Exit]).toBe(defaultSettings.relayTypeColors[RelayType.Exit])
    })

    it("resets custom colors to the defaults", async () => {
        await renderAppSettings()
        pickColor(colorInput("Exit relay color"), "#000000")

        await page.getByRole("button", {name: "Reset colors"}).click()

        await expect.poll(relayTypeColors).toEqual(defaultSettings.relayTypeColors)
        expect(colorInput("Exit relay color").value).toBe(defaultSettings.relayTypeColors[RelayType.Exit])
        await expect.element(page.getByRole("button", {name: "Reset colors"})).not.toBeInTheDocument()
    })
})

describe("AppSettings", () => {
    const restoreButton = {name: "Reset all settings"}

    it("hides the restore button while the settings are default", async () => {
        const screen = await renderWithProviders(<AppSettings elevation={0}/>)

        await expect.element(screen.getByLabelText("Density heatmap")).toBeVisible()
        await expect.element(screen.getByRole("button", restoreButton)).not.toBeInTheDocument()
    })

    it("shows the restore button after a change and restores the defaults", async () => {
        const screen = await renderWithProviders(<AppSettings elevation={0}/>)
        const heatMap = screen.getByLabelText("Density heatmap")

        await heatMap.click()
        await expect.element(heatMap).toBeChecked()
        await expect.element(screen.getByRole("button", restoreButton)).toBeVisible()

        await screen.getByRole("button", restoreButton).click()
        await expect.element(heatMap).not.toBeChecked()
        await expect.element(screen.getByRole("button", restoreButton)).not.toBeInTheDocument()
    })
})

describe("AppSettings relay search on map", () => {
    const restoreButton = {name: "Reset all settings"}

    const ShowSearchOnMap = () => {
        const [relaySearchOnMap, setRelaySearchOnMap] = useAtom(relaySearchOnMapAtom)
        return <>
            <button onClick={() => setRelaySearchOnMap({search: "relay", relays: [makeRelay()]})}>Show search</button>
            <output aria-label="search">{relaySearchOnMap?.search ?? "none"}</output>
        </>
    }

    it("restoring the defaults shows all relays again", async () => {
        const screen = await renderWithProviders(<><AppSettings elevation={0}/><ShowSearchOnMap/></>)
        await expect.element(screen.getByRole("button", restoreButton)).not.toBeInTheDocument()

        await screen.getByRole("button", {name: "Show search"}).click()
        await expect.element(screen.getByText(/Matching "relay"/)).toBeVisible()

        await screen.getByRole("button", restoreButton).click()
        await expect.element(screen.getByRole("status", {name: "search"})).toHaveTextContent("none")
        await expect.element(screen.getByText(/Matching "relay"/)).not.toBeInTheDocument()
        await expect.element(screen.getByRole("button", restoreButton)).not.toBeInTheDocument()
    })
})

const Weighted: FunctionComponent<{ relays: RelayLocationDto[] }> = ({relays}) => {
    const [, setFilteredRelays] = useAtom(filteredRelaysAtom)
    useEffect(() => setFilteredRelays(relays), [relays, setFilteredRelays])
    return null
}

const WeightState = () => {
    const {settings} = useSettings()
    return <output aria-label="weight state">{JSON.stringify({
        sizeByWeight: settings.sizeByWeight,
        topRelayCount: settings.topRelayCount ?? null,
    })}</output>
}

const weightState = () =>
    JSON.parse(page.getByRole("status", {name: "weight state"}).element().textContent ?? "{}")

describe("AppSettings consensus weight", () => {
    const openSection = () => page.getByRole("button", {name: "Consensus weight"}).click()
    const weightedRelays = [makeRelay({consensusWeight: 10})]

    it("explains that there are no weights for days without them", async () => {
        await renderWithProviders(<><AppSettings elevation={0}/><Weighted relays={[makeRelay()]}/></>)
        await openSection()
        await expect.element(page.getByText("There are no consensus weights for this day yet.")).toBeVisible()
        await expect.element(page.getByRole("switch", {name: "Size relay markers"})).toBeDisabled()
    })

    it("explains the weight in a tooltip of the switch", async () => {
        await renderWithProviders(<><AppSettings elevation={0}/><Weighted relays={weightedRelays}/></>)
        await openSection()
        await page.getByRole("switch", {name: "Size relay markers"}).hover()
        await expect.element(page.getByRole("tooltip")).toHaveTextContent("Sizes the markers by the weight directory authorities assign to a relay. Tor clients use it to pick relays, so it shows how likely a relay is picked, not how much traffic it carried.")
    })

    it("toggles the sizing once weights are available", async () => {
        await renderWithProviders(
            <><AppSettings elevation={0}/><WeightState/><Weighted relays={weightedRelays}/></>
        )
        await openSection()
        expect(weightState().sizeByWeight).toBe(false)
        await page.getByRole("switch", {name: "Size relay markers"}).click()
        await expect.poll(() => weightState().sizeByWeight).toBe(true)
    })

    it("tells about the limit and shows all relays again", async () => {
        await renderWithProviders(
            <><AppSettings elevation={0}/><WeightState/><Weighted relays={weightedRelays}/></>,
            {settings: {topRelayCount: 50, sizeByWeight: true}}
        )
        await expect.element(page.getByText(/Top consensus weight/)).toBeVisible()

        await page.getByRole("img", {name: "Show all relays"}).click()
        await expect.poll(() => weightState().topRelayCount).toBe(null)
        await expect.element(page.getByText(/Top consensus weight/)).not.toBeInTheDocument()
    })

    it("only limits the relays while the consensus weight switch is on", async () => {
        await renderWithProviders(
            <><AppSettings elevation={0}/><Weighted relays={weightedRelays}/></>,
            {settings: {topRelayCount: 50, sizeByWeight: false}}
        )
        await openSection()
        await expect.element(page.getByText(/Top consensus weight/)).not.toBeInTheDocument()
        await expect.element(page.getByRole("combobox", {name: "Show top"})).toHaveAttribute("aria-disabled", "true")
    })

    it("does not tell about a limit that has no effect without weights", async () => {
        await renderWithProviders(
            <><AppSettings elevation={0}/><Weighted relays={[makeRelay()]}/></>,
            {settings: {topRelayCount: 50}}
        )
        await expect.element(page.getByText(/Top consensus weight/)).not.toBeInTheDocument()
    })

    it("selects how many of the heaviest relays are shown", async () => {
        await renderWithProviders(
            <><AppSettings elevation={0}/><WeightState/><Weighted relays={weightedRelays}/></>,
            {settings: {sizeByWeight: true}}
        )
        await openSection()
        await page.getByRole("combobox", {name: "Show top"}).click()
        await page.getByRole("option", {name: "50 relays"}).click()
        await expect.poll(() => weightState().topRelayCount).toBe(50)

        await page.getByRole("combobox", {name: "Show top"}).click()
        await page.getByRole("option", {name: "All"}).click()
        await expect.poll(() => weightState().topRelayCount).toBe(null)
    })
})

const UsageState: FunctionComponent<{ usage?: CountryUsage }> = ({usage}) => {
    const [, setCountryUsage] = useAtom(countryUsageAtom)
    const {settings} = useSettings()
    useEffect(() => setCountryUsage(usage), [usage, setCountryUsage])
    return <output aria-label="usage setting">{String(settings.showCountryUsage)}</output>
}

describe("AppSettings country usage", () => {
    const openSection = () => page.getByRole("button", {name: "Tor users"}).click()
    const enabled = {settings: {showCountryUsage: true}}

    it("toggles the layer", async () => {
        await renderWithProviders(<><AppSettings elevation={0}/><UsageState/></>)
        await openSection()
        await expect.element(page.getByRole("status", {name: "usage setting"})).toHaveTextContent("false")
        await page.getByRole("switch", {name: "Color countries"}).click()
        await expect.element(page.getByRole("status", {name: "usage setting"})).toHaveTextContent("true")
    })

    it("always shows the color scale and the link, and explains the data in a tooltip", async () => {
        await renderWithProviders(<AppSettings elevation={0}/>)
        await openSection()
        await expect.element(page.getByRole("img", {name: "Color scale from few to many users"})).toBeVisible()
        await expect.element(page.getByText("few users")).toBeVisible()
        await expect.element(page.getByText("many users")).toBeVisible()
        await expect.element(page.getByRole("link", {name: "Learn more"})).toBeVisible()

        await page.getByRole("switch", {name: "Color countries"}).hover()
        await expect.element(page.getByRole("tooltip")).toHaveTextContent("An estimate by the Tor Project, based on the directory requests that relays report. It counts users that connect to Tor directly, not the traffic they cause.")
    })

    it("greys out the scale while the layer is off or the day has no estimate", async () => {
        const scale = () => page.getByRole("img", {name: "Color scale from few to many users"}).element().parentElement
        const usage = {day: "2024-03-05", usage: [{country: "DE", users: 10, lower: null, upper: null, frac: 61}]}
        await renderWithProviders(<><AppSettings elevation={0}/><UsageState usage={usage}/></>)
        await openSection()
        expect(scale()?.getAttribute("aria-disabled")).toBe("true")

        await page.getByRole("switch", {name: "Color countries"}).click()
        await expect.poll(() => scale()?.getAttribute("aria-disabled")).toBe("false")
    })

    it("greys out the scale for a day without estimate", async () => {
        await renderWithProviders(
            <><AppSettings elevation={0}/><UsageState usage={{day: "2008-01-01", usage: []}}/></>, enabled
        )
        await openSection()
        const scale = page.getByRole("img", {name: "Color scale from few to many users"}).element().parentElement
        expect(scale?.getAttribute("aria-disabled")).toBe("true")
    })

    it("explains how reliable the estimate of the day is", async () => {
        const usage = {day: "2024-03-05", usage: [{country: "DE", users: 10, lower: null, upper: null, frac: 61}]}
        await renderWithProviders(<><AppSettings elevation={0}/><UsageState usage={usage}/></>, enabled)
        await openSection()
        await expect.element(page.getByText("Estimate based on 61% of relays.")).toBeVisible()
        await expect.element(page.getByText(/Early estimates are less reliable/)).not.toBeInTheDocument()
    })

    it("warns about estimates that are based on few relays", async () => {
        const usage = {day: "2011-03-06", usage: [{country: "DE", users: 10, lower: null, upper: null, frac: 11}]}
        await renderWithProviders(<><AppSettings elevation={0}/><UsageState usage={usage}/></>, enabled)
        await openSection()
        await expect.element(page.getByText(/Early estimates are less reliable/)).toBeVisible()
    })

    it("says when a day has no estimates", async () => {
        await renderWithProviders(
            <><AppSettings elevation={0}/><UsageState usage={{day: "2008-01-01", usage: []}}/></>, enabled
        )
        await openSection()
        await expect.element(page.getByText("2008-01-01 has no estimate")).toBeVisible()
    })
})

describe("accordions", () => {
    it("have unique ids and each controls a region with a matching id", async () => {
        await renderWithProviders(<><AppSettings elevation={0}/><MapStats elevation={0} defaultExpanded={true}/></>)

        const summaries = Array.from(document.querySelectorAll("button[aria-controls]"))
        expect(summaries.length).toBeGreaterThanOrEqual(5)

        const summaryIds = summaries.map(summary => summary.id)
        const regionIds = summaries.map(summary => summary.getAttribute("aria-controls")!)
        expect(new Set(summaryIds).size).toBe(summaries.length)
        expect(new Set(regionIds).size).toBe(summaries.length)
        regionIds.forEach(regionId => {
            expect(document.querySelectorAll(`[id="${regionId}"]`)).toHaveLength(1)
        })
    })
})

describe("MapStats", () => {
    it("uses a row header for the label of every statistic", async () => {
        const screen = await renderWithProviders(<MapStats elevation={0} defaultExpanded={true}/>)

        await expect.element(screen.getByRole("rowheader", {name: "Exit relays"})).toBeVisible()
        await expect.element(screen.getByRole("rowheader", {name: "Countries"})).toBeVisible()
    })
})

const TypesState = () => {
    const {settings} = useSettings()
    return <output aria-label="types state">{JSON.stringify(settings.showRelayTypes)}</output>
}

describe("AppSettings relay types", () => {
    const typesState = () => JSON.parse(page.getByRole("status", {name: "types state"}).element().textContent ?? "{}")

    it("does not show a chip while all relay types are shown", async () => {
        await renderWithProviders(<><AppSettings elevation={0}/><TypesState/></>)
        await expect.element(page.getByText(/^Only /)).not.toBeInTheDocument()
    })

    it("combines the hidden relay types in one chip and shows all of them again", async () => {
        await renderWithProviders(
            <><AppSettings elevation={0}/><TypesState/></>,
            {settings: {showRelayTypes: {[RelayType.Exit]: false, [RelayType.Guard]: true, [RelayType.Other]: false}}}
        )
        await expect.element(page.getByText(/^Only Guard: /)).toBeVisible()

        await page.getByRole("img", {name: "Show all relays"}).click()
        await expect.poll(typesState).toEqual(defaultSettings.showRelayTypes)
        await expect.element(page.getByText(/^Only /)).not.toBeInTheDocument()
    })

    it("tells when no relay type is shown", async () => {
        await renderWithProviders(
            <AppSettings elevation={0}/>,
            {settings: {showRelayTypes: {[RelayType.Exit]: false, [RelayType.Guard]: false, [RelayType.Other]: false}}}
        )
        await expect.element(page.getByText(/^No relay types: /)).toBeVisible()
    })

    it("has the group heading Relay types and the icon inside the color picker", async () => {
        await renderWithProviders(<AppSettings elevation={0}/>)
        await expect.element(page.getByRole("button", {name: "Relay types"})).toBeVisible()
        // The switch label is only the name, the icon belongs to the color picker
        await expect.element(page.getByRole("switch", {name: "Exit"})).toBeVisible()
        const colorInput = page.getByLabelText("Exit relay color").element()
        expect(colorInput.parentElement?.querySelector("svg")).not.toBeNull()
    })
})
