import {describe, expect, it} from "vitest";

import {makeRelay, makeRelays, makeSettings} from "../test/fixtures";
import {RelayFlag, RelayType} from "../types/relay";
import {
    buildFamilyCoordinatesMap,
    buildFilteredRelays,
    buildRelayCoordinatesMap,
    buildRelayCountryMap,
    buildRelayFamilyMap,
    buildStatistics,
    createLatLonKey,
    filterRelaysByFlags,
    findRelayLocationById,
    getRelayType,
    sortFamilyCoordinatesMap,
} from "./aggregate-relays";

describe("getRelayType", () => {
    it("prefers Exit over Guard", () => {
        expect(getRelayType(makeRelay({flags: [RelayFlag.Guard, RelayFlag.Exit]}))).toBe(RelayType.Exit)
    })

    it("returns Guard for guard-only relays", () => {
        expect(getRelayType(makeRelay({flags: [RelayFlag.Guard]}))).toBe(RelayType.Guard)
    })

    it.each([[undefined], [null], [[]], [[RelayFlag.Fast, RelayFlag.Stable]]])(
        "returns Other without Exit or Guard (%j)",
        (flags) => {
            expect(getRelayType(makeRelay({flags}))).toBe(RelayType.Other)
        }
    )
})

describe("filterRelaysByFlags", () => {
    const exit = makeRelay({nickname: "exit", flags: [RelayFlag.Exit, RelayFlag.Fast]})
    const guard = makeRelay({nickname: "guard", flags: [RelayFlag.Guard, RelayFlag.Fast, RelayFlag.Stable]})
    const other = makeRelay({nickname: "other", flags: null})
    const relays = [exit, guard, other]

    it("returns everything when nothing is required and all types are shown", () => {
        expect(filterRelaysByFlags(relays, makeSettings())).toEqual(relays)
    })

    it("requires every selected flag", () => {
        const settings = makeSettings()
        settings.relaysMustHaveFlag[RelayFlag.Fast] = true
        settings.relaysMustHaveFlag[RelayFlag.Stable] = true
        expect(filterRelaysByFlags(relays, settings)).toEqual([guard])
    })

    it("drops relays without flags when a flag is required", () => {
        const settings = makeSettings()
        settings.relaysMustHaveFlag[RelayFlag.Fast] = true
        expect(filterRelaysByFlags(relays, settings)).toEqual([exit, guard])
    })

    it.each([
        [RelayType.Exit, ["guard", "other"]],
        [RelayType.Guard, ["exit", "other"]],
        [RelayType.Other, ["exit", "guard"]],
    ])("hides relay type %s when disabled", (type, expectedNicknames) => {
        const settings = makeSettings()
        settings.showRelayTypes[type] = false
        expect(filterRelaysByFlags(relays, settings).map(r => r.nickname)).toEqual(expectedNicknames)
    })

    it("returns an empty list for no relays", () => {
        expect(filterRelaysByFlags([], makeSettings())).toEqual([])
    })
})

describe("buildRelayCoordinatesMap", () => {
    it("groups relays by coordinate pair", () => {
        const a1 = makeRelay({lat: 1, long: 2})
        const a2 = makeRelay({lat: 1, long: 2})
        const b = makeRelay({lat: 3, long: 4})
        const map = buildRelayCoordinatesMap([a1, b, a2])
        expect(map.size).toBe(2)
        expect(map.get("1,2")).toEqual([a1, a2])
        expect(map.get("3,4")).toEqual([b])
    })

    it("builds keys as lat,long", () => {
        expect(createLatLonKey(makeRelay({lat: 50.1, long: -8.5}))).toBe("50.1,-8.5")
    })
})

describe("buildRelayFamilyMap", () => {
    it("groups relays by family id", () => {
        const a1 = makeRelay({familyId: 7})
        const a2 = makeRelay({familyId: 7})
        const b = makeRelay({familyId: 9})
        const map = buildRelayFamilyMap([a1, b, a2])
        expect(map.get(7)).toEqual([a1, a2])
        expect(map.get(9)).toEqual([b])
    })

    it("ignores relays without a family, including family id 0", () => {
        const map = buildRelayFamilyMap([
            makeRelay({familyId: null}),
            makeRelay({familyId: undefined}),
            makeRelay({familyId: 0}),
        ])
        expect(map.size).toBe(0)
    })
})

describe("buildFamilyCoordinatesMap", () => {
    it("builds a family map for every coordinate", () => {
        const relays = [
            makeRelay({lat: 1, long: 1, familyId: 1}),
            makeRelay({lat: 1, long: 1, familyId: 2}),
            makeRelay({lat: 2, long: 2, familyId: 1}),
        ]
        const map = buildFamilyCoordinatesMap(buildRelayCoordinatesMap(relays))
        expect([...map.get("1,1")!.keys()]).toEqual([1, 2])
        expect([...map.get("2,2")!.keys()]).toEqual([1])
    })
})

describe("sortFamilyCoordinatesMap", () => {
    const build = (sizes: Record<number, number>) => {
        const families = new Map(Object.entries(sizes).map(
            ([id, size]) => [Number(id), makeRelays(size, {familyId: Number(id)})]
        ))
        return new Map([["1,1", families]])
    }

    it("sorts families by size, largest first, and pads larger ones", () => {
        const sorted = sortFamilyCoordinatesMap(build({1: 1, 2: 3, 3: 2})).get("1,1")!
        expect(sorted.map(f => f.familyId)).toEqual([2, 3, 1])
        expect(sorted.map(f => f.relays.length)).toEqual([3, 2, 1])
        // padding keeps the displayed circle sizes of families at one location distinct
        expect(sorted.map(f => f.padding)).toEqual([5, 2, 0])
    })

    it("keeps insertion order for equal sizes", () => {
        const sorted = sortFamilyCoordinatesMap(build({5: 2, 6: 2})).get("1,1")!
        expect(sorted.map(f => f.familyId)).toEqual([5, 6])
    })

    it("handles a single family without padding", () => {
        const sorted = sortFamilyCoordinatesMap(build({4: 2})).get("1,1")!
        expect(sorted).toHaveLength(1)
        expect(sorted[0].padding).toBe(0)
    })

    it("handles coordinates without families", () => {
        expect(sortFamilyCoordinatesMap(new Map([["1,1", new Map()]])).get("1,1")).toEqual([])
    })

    // Documents current behavior: the input map is consumed while sorting.
    it("empties the family maps it is given", () => {
        const input = build({1: 1, 2: 2})
        sortFamilyCoordinatesMap(input)
        expect(input.get("1,1")!.size).toBe(0)
    })
})

describe("buildRelayCountryMap", () => {
    it("groups relays by country and skips relays without one", () => {
        const de = makeRelay({country: "DE"})
        const fr = makeRelay({country: "FR"})
        const none = makeRelay({country: undefined as unknown as string})
        const map = buildRelayCountryMap([de, fr, none])
        expect([...map.keys()]).toEqual(["DE", "FR"])
        expect(map.get("DE")).toEqual([de])
    })

    it("collects multiple relays of one country", () => {
        const relays = makeRelays(3, {country: "US"})
        expect(buildRelayCountryMap(relays).get("US")).toEqual(relays)
    })
})

describe("buildFilteredRelays", () => {
    const deFam1 = makeRelay({country: "DE", familyId: 1})
    const deFam2 = makeRelay({country: "DE", familyId: 2})
    const frFam1 = makeRelay({country: "FR", familyId: 1})
    const flagFiltered = [deFam2]
    const all = [deFam1, deFam2, frFam1]
    const countryMap = buildRelayCountryMap(all)
    const familyMap = buildRelayFamilyMap(all)

    it("returns flag-filtered relays without a selection", () => {
        expect(buildFilteredRelays(flagFiltered, countryMap, familyMap, makeSettings())).toBe(flagFiltered)
    })

    it("returns the country's relays when only a country is selected", () => {
        const settings = makeSettings({selectedCountry: "DE"})
        expect(buildFilteredRelays(flagFiltered, countryMap, familyMap, settings)).toEqual([deFam1, deFam2])
    })

    it("returns the family's relays when only a family is selected", () => {
        const settings = makeSettings({selectedFamily: 1})
        expect(buildFilteredRelays(flagFiltered, countryMap, familyMap, settings)).toEqual([deFam1, frFam1])
    })

    it("intersects country and family when both are selected", () => {
        const settings = makeSettings({selectedCountry: "DE", selectedFamily: 1})
        expect(buildFilteredRelays(flagFiltered, countryMap, familyMap, settings)).toEqual([deFam1])
    })

    it("returns nothing when the selected country has no such family", () => {
        const settings = makeSettings({selectedCountry: "FR", selectedFamily: 2})
        expect(buildFilteredRelays(flagFiltered, countryMap, familyMap, settings)).toEqual([])
    })

    it("returns nothing when both are selected but the country is unknown", () => {
        const settings = makeSettings({selectedCountry: "XX", selectedFamily: 1})
        expect(buildFilteredRelays(flagFiltered, countryMap, familyMap, settings)).toEqual([])
    })

    it("falls back to flag-filtered relays for an unknown country", () => {
        const settings = makeSettings({selectedCountry: "XX"})
        expect(buildFilteredRelays(flagFiltered, countryMap, familyMap, settings)).toBe(flagFiltered)
    })

    it("falls back to flag-filtered relays for an unknown family", () => {
        const settings = makeSettings({selectedFamily: 99})
        expect(buildFilteredRelays(flagFiltered, countryMap, familyMap, settings)).toBe(flagFiltered)
    })
})

describe("buildStatistics", () => {
    const exit = makeRelay({country: "DE", familyId: 1, flags: [RelayFlag.Exit]})
    const guard = makeRelay({country: "FR", familyId: 1, flags: [RelayFlag.Guard]})
    const other1 = makeRelay({country: "FR", familyId: 2})
    const other2 = makeRelay({country: "US", familyId: null})
    const all = [exit, guard, other1, other2]
    const countryMap = buildRelayCountryMap(all)
    const familyMap = buildRelayFamilyMap(all)

    it("counts relay types, countries and families", () => {
        expect(buildStatistics(all, countryMap, familyMap, makeSettings())).toEqual({
            relayExitCount: 1,
            relayGuardCount: 1,
            relayOtherCount: 2,
            countryCount: 3,
            familyCount: 2,
        })
    })

    it("reports a single country and family when both are selected", () => {
        const settings = makeSettings({selectedCountry: "FR", selectedFamily: 1})
        const stats = buildStatistics([guard], countryMap, familyMap, settings)
        expect(stats).toMatchObject({countryCount: 1, familyCount: 1, relayGuardCount: 1})
    })

    it("counts the distinct families of a selected country", () => {
        const settings = makeSettings({selectedCountry: "FR"})
        const stats = buildStatistics([guard, other1], countryMap, familyMap, settings)
        expect(stats).toMatchObject({countryCount: 1, familyCount: 2})
    })

    it("counts the distinct countries of a selected family", () => {
        const settings = makeSettings({selectedFamily: 1})
        const stats = buildStatistics([exit, guard], countryMap, familyMap, settings)
        expect(stats).toMatchObject({countryCount: 2, familyCount: 1})
    })

    it("uses overall counts when the selection is not present in the data", () => {
        const settings = makeSettings({selectedCountry: "XX", selectedFamily: undefined})
        const stats = buildStatistics(all, countryMap, familyMap, settings)
        expect(stats).toMatchObject({countryCount: 3, familyCount: 2})
    })

    it("returns zeros for no relays", () => {
        expect(buildStatistics([], new Map(), new Map(), makeSettings())).toEqual({
            relayExitCount: 0,
            relayGuardCount: 0,
            relayOtherCount: 0,
            countryCount: 0,
            familyCount: 0,
        })
    })
})

describe("findRelayLocationById", () => {
    const relays = [makeRelay({detailsId: 10}), makeRelay({detailsId: 20})]

    it("finds a relay by details id", () => {
        expect(findRelayLocationById(20, relays)).toBe(relays[1])
    })

    it("returns undefined for unknown ids", () => {
        expect(findRelayLocationById(30, relays)).toBeUndefined()
    })
})
