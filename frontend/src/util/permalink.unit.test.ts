import {describe, expect, it} from "vitest";

import {formatPermalink, parsePermalink} from "./permalink";

describe("parsePermalink", () => {
    it("parses a complete hash", () => {
        expect(parsePermalink("#lat=30.5&lng=-12.25&zoom=5&day=2024-05-01")).toEqual({
            lat: 30.5,
            lng: -12.25,
            zoom: 5,
            day: "2024-05-01",
        })
    })

    it("returns an empty permalink for an empty hash", () => {
        expect(parsePermalink("")).toEqual({})
        expect(parsePermalink("#")).toEqual({})
    })

    it("drops out of range and non numeric values", () => {
        expect(parsePermalink("#lat=91&lng=0&zoom=99&day=2024-13-45")).toEqual({})
        expect(parsePermalink("#lat=abc&lng=1&zoom=NaN&day=<script>")).toEqual({})
    })

    it("only returns latitude and longitude as a pair", () => {
        expect(parsePermalink("#lat=10")).toEqual({})
        expect(parsePermalink("#lng=10&zoom=4")).toEqual({zoom: 4})
    })

    it("rounds the zoom to an integer", () => {
        expect(parsePermalink("#zoom=3.6")).toEqual({zoom: 4})
    })
})

describe("formatPermalink", () => {
    it("round trips through parsePermalink", () => {
        const permalink = {lat: 30.123, lng: -5.5, zoom: 6, day: "2023-01-31"}
        expect(parsePermalink(formatPermalink(permalink))).toEqual(permalink)
    })

    it("omits unset values", () => {
        expect(formatPermalink({day: "2023-01-31"})).toBe("day=2023-01-31")
        expect(formatPermalink({})).toBe("")
    })
})
