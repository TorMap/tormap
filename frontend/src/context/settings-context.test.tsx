import React from "react";
import {renderHook} from "vitest-browser-react";
import {describe, expect, it} from "vitest";

import {relaysMustIncludeFlagInput, showRelayTypesInput} from "../components/accordion/AppSettings";
import {makeSettings} from "../test/fixtures";
import {RelayFlag, RelayType} from "../types/relay";
import {Settings} from "../types/settings";
import {SettingsProvider, settingsEqual, useSettings} from "./settings-context";

const render = (defaults: Settings = makeSettings()) =>
    renderHook(() => useSettings(), {
        wrapper: ({children}) => <SettingsProvider defaultSettings={defaults}>{children}</SettingsProvider>,
    })

const changeEvent = (name: string, id: string, checked: boolean) =>
    ({target: {name, id, checked}}) as React.ChangeEvent<HTMLInputElement>

describe("SettingsProvider", () => {
    it("provides the default settings", async () => {
        const defaults = makeSettings({heatMap: true})
        const {result} = await render(defaults)
        expect(result.current.settings).toEqual(defaults)
    })

    describe("changeSettings", () => {
        it("sets a boolean setting by input name", async () => {
            const {result, act} = await render()
            await act(() => result.current.changeSettings(changeEvent("heatMap", "heatMap", true)))
            expect(result.current.settings.heatMap).toBe(true)
        })

        it("sets a relay type visibility by input id", async () => {
            const {result, act} = await render()
            await act(() => result.current.changeSettings(
                changeEvent(showRelayTypesInput, String(RelayType.Guard), false)
            ))
            expect(result.current.settings.showRelayTypes).toEqual({
                [RelayType.Exit]: true,
                [RelayType.Guard]: false,
                [RelayType.Other]: true,
            })
        })

        it("sets a required relay flag by input id", async () => {
            const {result, act} = await render()
            await act(() => result.current.changeSettings(
                changeEvent(relaysMustIncludeFlagInput, String(RelayFlag.Stable), true)
            ))
            expect(result.current.settings.relaysMustHaveFlag[RelayFlag.Stable]).toBe(true)
            expect(result.current.settings.relaysMustHaveFlag[RelayFlag.Fast]).toBe(false)
        })
    })

    it("replaces the settings with setSettings", async () => {
        const {result, act} = await render()
        await act(() => result.current.setSettings(makeSettings({aggregateCoordinates: true})))
        expect(result.current.settings.aggregateCoordinates).toBe(true)
    })

    describe("restore defaults", () => {
        it("reports default settings on load", async () => {
            const {result} = await render()
            expect(result.current.isDefaultSettings).toBe(true)
        })

        it("reports non-default settings after a change", async () => {
            const {result, act} = await render()
            await act(() => result.current.changeSettings(
                changeEvent(relaysMustIncludeFlagInput, String(RelayFlag.Stable), true)
            ))
            expect(result.current.isDefaultSettings).toBe(false)
        })

        it("reports default settings again when a change is undone by hand", async () => {
            const {result, act} = await render()
            await act(() => result.current.changeSettings(changeEvent("heatMap", "heatMap", true)))
            await act(() => result.current.changeSettings(changeEvent("heatMap", "heatMap", false)))
            expect(result.current.isDefaultSettings).toBe(true)
        })

        it("restores the provided defaults", async () => {
            const defaults = makeSettings({aggregateCoordinates: true})
            const {result, act} = await render(defaults)
            await act(() => result.current.setSettings(makeSettings({
                sortCountry: true,
                selectedCountry: "DE",
                showRelayTypes: {[RelayType.Exit]: false, [RelayType.Guard]: true, [RelayType.Other]: true},
            })))
            expect(result.current.isDefaultSettings).toBe(false)

            await act(() => result.current.resetSettings())
            expect(result.current.settings).toEqual(defaults)
            expect(result.current.isDefaultSettings).toBe(true)
        })
    })

    describe("selection reset", () => {
        it("clears the selected country when country grouping is off", async () => {
            const {result} = await render(makeSettings({sortCountry: false, selectedCountry: "DE"}))
            expect(result.current.settings.selectedCountry).toBeUndefined()
        })

        it("clears the selected family when family grouping is off", async () => {
            const {result} = await render(makeSettings({sortFamily: false, selectedFamily: 3}))
            expect(result.current.settings.selectedFamily).toBeUndefined()
        })

        it("keeps selections while grouping is on", async () => {
            const {result} = await render(makeSettings({
                sortCountry: true,
                selectedCountry: "DE",
                sortFamily: true,
                selectedFamily: 3,
            }))
            expect(result.current.settings).toMatchObject({selectedCountry: "DE", selectedFamily: 3})
        })

        it("clears a selection when its grouping is switched off", async () => {
            const {result, act} = await render(makeSettings({sortCountry: true, selectedCountry: "DE"}))
            await act(() => result.current.changeSettings(changeEvent("sortCountry", "sortCountry", false)))
            expect(result.current.settings.selectedCountry).toBeUndefined()
        })
    })
})

describe("settingsEqual", () => {
    it("treats equal values in different objects as equal", () => {
        expect(settingsEqual(makeSettings(), makeSettings())).toBe(true)
    })

    it("detects a changed top-level value", () => {
        expect(settingsEqual(makeSettings(), makeSettings({sortFamily: true, selectedFamily: 1}))).toBe(false)
    })

    it("detects a changed nested value", () => {
        const changed = makeSettings()
        changed.relaysMustHaveFlag[RelayFlag.Exit] = true
        expect(settingsEqual(makeSettings(), changed)).toBe(false)
    })
})
