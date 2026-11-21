import {describe, expect, it} from "vitest";

import {RelayFlag} from "../types/relay";
import {monthOfDay, relayDetailsPath, toFamilyIdentifier, toRelayLocation} from "./api-v1";

const fingerprint = "A".repeat(40)

describe("toRelayLocation", () => {
    it("converts flag names and renames the autonomous system", () => {
        expect(toRelayLocation({
            fingerprint,
            nickname: "alpha",
            country: "DE",
            lat: 50.1,
            long: 8.6,
            flags: ["Exit", "Guard", "SomeFutureFlag"],
            consensusWeight: 42,
            autonomousSystemNumber: 24940,
            familyId: 7,
        })).toEqual({
            fingerprint,
            hasDetails: true,
            nickname: "alpha",
            country: "DE",
            lat: 50.1,
            long: 8.6,
            flags: [RelayFlag.Exit, RelayFlag.Guard],
            consensusWeight: 42,
            asNumber: 24940,
            familyId: 7,
        })
    })

    it("marks relays without details", () => {
        const relay = toRelayLocation({fingerprint, nickname: null, country: "DE", lat: 0, long: 0, flags: []})
        expect(relay.hasDetails).toBe(false)
        expect(relay.nickname).toBe("")
    })
})

describe("toFamilyIdentifier", () => {
    it("joins the nicknames and the names of the autonomous systems", () => {
        expect(toFamilyIdentifier({
            id: 7,
            members: [{fingerprint, nickname: "alpha"}, {fingerprint: "B".repeat(40), nickname: "bravo"}],
            autonomousSystems: [{number: 24940, name: "Hetzner"}, {number: 16276, name: null}],
        })).toEqual({id: 7, memberCount: 2, nicknames: "alpha, bravo", autonomousSystems: "Hetzner"})
    })

    it("has no autonomous systems without names", () => {
        expect(toFamilyIdentifier({id: 7, members: [], autonomousSystems: []}).autonomousSystems).toBeUndefined()
    })
})

describe("paths", () => {
    it("requests relay details by fingerprint for the month of a day", () => {
        expect(relayDetailsPath(fingerprint, monthOfDay("2024-05-17"))).toBe(`/v1/relays/${fingerprint}?month=2024-05`)
    })
})
