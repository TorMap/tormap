import {renderHook} from "vitest-browser-react";
import {describe, expect, it} from "vitest";

import {StatisticsProvider, useStatistics} from "./statistics-context";

const render = () =>
    renderHook(() => useStatistics(), {
        wrapper: ({children}) => <StatisticsProvider>{children}</StatisticsProvider>,
    })

describe("StatisticsProvider", () => {
    it("starts with zero counts", async () => {
        const {result} = await render()
        expect(result.current.statistics).toEqual({
            relayGuardCount: 0,
            relayExitCount: 0,
            relayOtherCount: 0,
            countryCount: 0,
            familyCount: 0,
        })
    })

    it("updates the statistics", async () => {
        const {result, act} = await render()
        const next = {relayGuardCount: 1, relayExitCount: 2, relayOtherCount: 3, countryCount: 4, familyCount: 5}
        await act(() => result.current.setStatistics(next))
        expect(result.current.statistics).toEqual(next)
    })
})
