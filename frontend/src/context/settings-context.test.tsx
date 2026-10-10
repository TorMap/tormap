import React from "react";
import {renderHook} from "vitest-browser-react";
import {describe, expect, it} from "vitest";

import {relaysMustIncludeFlagInput, showRelayTypesInput} from "../components/accordion/AppSettings";
import {makeSettings} from "../test/fixtures";
import {RelayFlag, RelayType} from "../types/relay";
import {Settings} from "../types/settings";
import {SettingsProvider, useSettings} from "./settings-context";

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
