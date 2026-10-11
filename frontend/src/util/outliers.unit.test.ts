import {describe, expect, it} from "vitest";

import {findOutliers} from "./outliers";

// A slowly growing series with small deterministic noise
const series = (length: number) => Array.from({length}, (_, i) => 1000 + i * 2 + (i % 5) * 3)

describe("findOutliers", () => {
    it("finds nothing in a smooth series", () => {
        expect(findOutliers(series(100))).toEqual([])
    })

    it("flags a sudden drop", () => {
        const values: (number | undefined)[] = series(100)
        values[50] = 400
        const outliers = findOutliers(values)
        expect(outliers.map(outlier => outlier.index)).toEqual([50])
        expect(outliers[0].expected).toBeGreaterThan(1000)
    })

    it("flags a sudden spike", () => {
        const values: (number | undefined)[] = series(100)
        values[20] = 2500
        expect(findOutliers(values).map(outlier => outlier.index)).toEqual([20])
    })

    it("does not flag a permanent change of level", () => {
        const values = series(100).map((value, i) => i >= 50 ? value + 600 : value)
        const indexes = findOutliers(values).map(outlier => outlier.index)
        // Only the values directly around the step may deviate, never the plateau itself
        expect(indexes.every(index => index >= 44 && index <= 56)).toBe(true)
        expect(indexes).not.toContain(80)
    })

    it("ignores small fluctuations below the minimum deviation", () => {
        const values: (number | undefined)[] = Array.from({length: 60}, () => 1000)
        values[30] = 1050
        expect(findOutliers(values)).toEqual([])
    })

    it("skips missing values", () => {
        const values: (number | undefined)[] = series(60)
        values[10] = undefined
        values[11] = undefined
        expect(findOutliers(values)).toEqual([])
    })

    it("returns nothing for too short series", () => {
        expect(findOutliers([])).toEqual([])
        expect(findOutliers([1, 2, 3])).toEqual([])
    })
})
