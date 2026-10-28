import {describe, expect, it} from "vitest";

import {makeRelay} from "../test/fixtures";
import {RelayFlag} from "../types/relay";
import {buildRelayAutonomousSystemMap} from "./aggregate-relays";
import {autonomousSystemMatches, AutonomousSystemRow, buildAutonomousSystemRows} from "./autonomous-systems";

describe("buildAutonomousSystemRows", () => {
    const relays = [
        makeRelay({asNumber: 24940, country: "DE", consensusWeight: 30, flags: [RelayFlag.Exit]}),
        makeRelay({asNumber: 24940, country: "FI", consensusWeight: 10}),
        makeRelay({asNumber: 16276, country: "FR", consensusWeight: null}),
    ]
    const asMap = buildRelayAutonomousSystemMap(relays)

    it("sums the relays, exits, countries and consensus weight of each autonomous system", () => {
        const rows = buildAutonomousSystemRows([24940, 16276], asMap, 80, [{number: 24940, name: "Hetzner Online GmbH"}])
        expect(rows).toEqual([
            {
                number: 24940,
                name: "Hetzner Online GmbH",
                relayCount: 2,
                exitCount: 1,
                countryCount: 2,
                consensusWeight: 40,
                consensusWeightShare: 0.5,
            },
            {
                number: 16276,
                name: undefined,
                relayCount: 1,
                exitCount: 0,
                countryCount: 1,
                consensusWeight: undefined,
                consensusWeightShare: undefined,
            },
        ])
    })

    it("has no share without any weight on the map", () => {
        expect(buildAutonomousSystemRows([24940], asMap, 0)[0].consensusWeightShare).toBeUndefined()
    })
})

describe("autonomousSystemMatches", () => {
    const row: AutonomousSystemRow = {number: 24940, name: "Hetzner Online GmbH", relayCount: 1, exitCount: 0, countryCount: 1}

    it.each(["", "hetzner", "ONLINE", "24940", "AS24940", "as 24940", "249"])("matches %j", search => {
        expect(autonomousSystemMatches(row, search)).toBe(true)
    })

    it.each(["ovh", "AS16276", "12345"])("does not match %j", search => {
        expect(autonomousSystemMatches(row, search)).toBe(false)
    })

    it("matches by number without a name", () => {
        expect(autonomousSystemMatches({...row, name: undefined}, "24940")).toBe(true)
    })
})
