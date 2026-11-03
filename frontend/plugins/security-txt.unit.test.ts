import {describe, expect, it} from "vitest";

import {securityTxt} from "./security-txt";

describe("securityTxt", () => {
    it("has the required fields and expires in less than a year", () => {
        expect(securityTxt(new Date("2026-11-03T22:30:00Z"))).toBe(
            "Contact: https://github.com/TorMap/tormap/security/advisories/new\n" +
            "Expires: 2027-05-02T00:00:00Z\n" +
            "Policy: https://github.com/TorMap/tormap/security/policy\n" +
            "Preferred-Languages: en\n",
        )
    })
})
