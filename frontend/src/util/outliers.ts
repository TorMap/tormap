export interface Outlier {
    /**
     * Index of the outlier in the given values
     */
    index: number

    /**
     * The typical value around the outlier
     */
    expected: number
}

interface OutlierOptions {
    /**
     * Number of neighbouring values used to compute the typical value
     */
    windowSize: number

    /**
     * How many inter quartile ranges a deviation has to exceed the quartiles by
     */
    iqrFactor: number

    /**
     * Minimum relative deviation from the typical value, so that very stable series do not flag tiny changes
     */
    minRelativeDeviation: number
}

const defaultOptions: OutlierOptions = {windowSize: 15, iqrFactor: 3, minRelativeDeviation: 0.1}

function median(sorted: number[]): number {
    const middle = Math.floor(sorted.length / 2)
    return sorted.length % 2 === 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2
}

function quantile(sorted: number[], q: number): number {
    const position = (sorted.length - 1) * q
    const lower = Math.floor(position)
    const upper = Math.ceil(position)
    return sorted[lower] + (sorted[upper] - sorted[lower]) * (position - lower)
}

/**
 * Finds values that deviate unusually much from the median of their neighbours.
 * A permanent change of level is not an outlier, since the neighbours follow it.
 * @param values - A series of values, `undefined` for missing values
 * @param options - Overrides for the default detection parameters
 */
export function findOutliers(values: (number | undefined)[], options: Partial<OutlierOptions> = {}): Outlier[] {
    const {windowSize, iqrFactor, minRelativeDeviation} = {...defaultOptions, ...options}
    const half = Math.floor(windowSize / 2)

    const expectedValues: (number | undefined)[] = values.map((_, index) => {
        const neighbours: number[] = []
        for (let i = Math.max(0, index - half); i <= Math.min(values.length - 1, index + half); i++) {
            const value = values[i]
            if (i !== index && value !== undefined) neighbours.push(value)
        }
        if (neighbours.length < 3) return undefined
        return median(neighbours.sort((a, b) => a - b))
    })

    const deviations: number[] = []
    values.forEach((value, index) => {
        const expected = expectedValues[index]
        if (value !== undefined && expected !== undefined && expected > 0) deviations.push(value / expected - 1)
    })
    if (deviations.length < 4) return []

    const sorted = [...deviations].sort((a, b) => a - b)
    const q1 = quantile(sorted, 0.25)
    const q3 = quantile(sorted, 0.75)
    const iqr = q3 - q1
    const lowerFence = q1 - iqrFactor * iqr
    const upperFence = q3 + iqrFactor * iqr

    const outliers: Outlier[] = []
    values.forEach((value, index) => {
        const expected = expectedValues[index]
        if (value === undefined || expected === undefined || expected <= 0) return
        const deviation = value / expected - 1
        if (Math.abs(deviation) >= minRelativeDeviation && (deviation < lowerFence || deviation > upperFence)) {
            outliers.push({index, expected})
        }
    })
    return outliers
}
