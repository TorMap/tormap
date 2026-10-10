import {describe, expect, it} from "vitest";
import {page} from "vitest/browser";

import {defaultSettings} from "../../config";
import {useSettings} from "../../context/settings-context";
import {renderWithProviders} from "../../test/render";
import {RelayType} from "../../types/relay";
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
