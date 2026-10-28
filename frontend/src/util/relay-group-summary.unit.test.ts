import {describe, expect, it} from "vitest";

import {makeRelay} from "../test/fixtures";
import {RelayFlag} from "../types/relay";
import {summarizeRelays} from "./relay-group-summary";

describe("summarizeRelays", () => {
    it("counts relays, exits and countries and sums the consensus weight", () => {
        const relays = [
            makeRelay({country: "DE", consensusWeight: 30, flags: [RelayFlag.Exit, RelayFlag.Guard]}),
            makeRelay({country: "FI", consensusWeight: 10, flags: [RelayFlag.Guard]}),
            makeRelay({country: "DE", consensusWeight: null}),
        ]
        expect(summarizeRelays(relays, 80)).toEqual({
            relayCount: 3,
            exitCount: 1,
            countryCount: 2,
            consensusWeight: 40,
            consensusWeightShare: 0.5,
        })
    })

    it("has no weight without any weight of its relays", () => {
        const summary = summarizeRelays([makeRelay({consensusWeight: null})], 80)
        expect(summary.consensusWeight).toBeUndefined()
        expect(summary.consensusWeightShare).toBeUndefined()
    })

    it("has no share without any weight on the map", () => {
        expect(summarizeRelays([makeRelay({consensusWeight: 5})], 0).consensusWeightShare).toBeUndefined()
    })

    it("summarizes no relays", () => {
        expect(summarizeRelays([], 10)).toEqual({relayCount: 0, exitCount: 0, countryCount: 0, consensusWeight: undefined, consensusWeightShare: undefined})
    })
})
