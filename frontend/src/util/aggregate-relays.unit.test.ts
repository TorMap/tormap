import {describe, expect, it} from "vitest";

import {RelayLocationDto} from "../dto/relay";
import {RelayFlag, RelayType} from "../types/relay";
import {getRelayType} from "./aggregate-relays";

const relay = (flags?: RelayFlag[] | null): RelayLocationDto => ({
    lat: 0,
    long: 0,
    country: "DE",
    nickname: "test",
    flags,
})

describe("getRelayType", () => {
    it("prefers Exit over Guard", () => {
        expect(getRelayType(relay([RelayFlag.Guard, RelayFlag.Exit]))).toBe(RelayType.Exit)
    })

    it("returns Guard for guard-only relays", () => {
        expect(getRelayType(relay([RelayFlag.Guard]))).toBe(RelayType.Guard)
    })

    it.each([[undefined], [null], [[]]])("returns Other without relevant flags (%j)", (flags) => {
        expect(getRelayType(relay(flags as RelayFlag[] | null | undefined))).toBe(RelayType.Other)
    })
})
