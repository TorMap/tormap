import {describe, expect, it} from "vitest";

import {makeSettings} from "../test/fixtures";
import {RelayFlag, RelayType} from "../types/relay";
import {parseStoredSettings} from "./settings-storage";

describe("parseStoredSettings", () => {
    const defaults = makeSettings()

    it.each([
        ["nothing is stored", null],
        ["invalid JSON", "{"],
        ["a non object", "[true]"],
        ["null", "null"],
    ])("returns the defaults if %s", (_name, json) => {
        expect(parseStoredSettings(json, defaults)).toEqual(defaults)
    })

    it("restores stored settings", () => {
        const stored = makeSettings({
            sortCountry: true,
            selectedCountry: "DE",
            sortFamily: true,
            selectedFamily: 42,
            aggregateCoordinates: true,
            heatMap: true,
            showRelayTypes: {[RelayType.Exit]: false, [RelayType.Guard]: true, [RelayType.Other]: false},
            relayTypeColors: {[RelayType.Exit]: "#000001", [RelayType.Guard]: "#000002", [RelayType.Other]: "#000003"},
        })
        stored.relaysMustHaveFlag[RelayFlag.Stable] = true
        expect(parseStoredSettings(JSON.stringify(stored), defaults)).toEqual(stored)
    })

    it("keeps defaults for missing settings", () => {
        const parsed = parseStoredSettings(JSON.stringify({heatMap: true, showRelayTypes: {[RelayType.Exit]: false}}), defaults)
        expect(parsed).toEqual({
            ...defaults,
            heatMap: true,
            showRelayTypes: {...defaults.showRelayTypes, [RelayType.Exit]: false},
        })
    })

    it("ignores invalid values and unknown keys", () => {
        const parsed = parseStoredSettings(JSON.stringify({
            sortCountry: "yes",
            selectedCountry: 1,
            selectedFamily: 1.5,
            heatMap: null,
            showRelayTypes: [false, false, false],
            relaysMustHaveFlag: {[RelayFlag.Exit]: 1, 99: true},
            relayTypeColors: {[RelayType.Exit]: "red", [RelayType.Guard]: "#FFD371", [RelayType.Other]: "#123456;x"},
            unknown: true,
        }), defaults)
        expect(parsed).toEqual(defaults)
    })
})
