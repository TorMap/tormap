import {useAtom} from "jotai";
import {describe, expect, it} from "vitest";
import {page} from "vitest/browser";

import {defaultSettings} from "../../config";
import {useSettings} from "../../context/settings-context";
import {makeRelay} from "../../test/fixtures";
import {renderWithProviders} from "../../test/render";
import {RelayType} from "../../types/relay";
import {relaySearchOnMapAtom} from "../dialogs/relay/relay-details-atoms";
import {AppSettings} from "./AppSettings";

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
    const restoreButton = {name: "Restore settings"}

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
    const restoreButton = {name: "Restore settings"}

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
        await expect.element(screen.getByText('Map only shows relays matching "relay"')).toBeVisible()

        await screen.getByRole("button", restoreButton).click()
        await expect.element(screen.getByRole("status", {name: "search"})).toHaveTextContent("none")
        await expect.element(screen.getByText('Map only shows relays matching "relay"')).not.toBeInTheDocument()
        await expect.element(screen.getByRole("button", restoreButton)).not.toBeInTheDocument()
    })
})
