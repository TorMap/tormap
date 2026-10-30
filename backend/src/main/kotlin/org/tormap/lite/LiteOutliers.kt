package org.tormap.lite

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Finds values that deviate unusually much from the median of their neighbours, like findOutliers() of the frontend
 * (`util/outliers.ts`). A permanent change of level is not an outlier, since the neighbours follow it.
 */
object LiteOutliers {
    /**
     * @param index - the index of the outlier in the values
     * @param expected - the typical value around the outlier
     */
    data class Outlier(val index: Int, val expected: Double)

    private const val WINDOW_SIZE = 15
    private const val IQR_FACTOR = 3.0
    private const val MIN_RELATIVE_DEVIATION = 0.1

    /**
     * @param values - a series of values, null for missing values
     */
    fun find(values: List<Long?>): List<Outlier> {
        val half = WINDOW_SIZE / 2
        val expectedValues = values.indices.map { index ->
            val neighbours = (maxOf(0, index - half)..minOf(values.lastIndex, index + half))
                .filter { it != index }
                .mapNotNull { values[it]?.toDouble() }
            if (neighbours.size < 3) null else median(neighbours.sorted())
        }

        val deviations = values.indices.mapNotNull { index ->
            val value = values[index]
            val expected = expectedValues[index]
            if (value != null && expected != null && expected > 0) value / expected - 1 else null
        }
        if (deviations.size < 4) return emptyList()

        val sorted = deviations.sorted()
        val q1 = quantile(sorted, 0.25)
        val q3 = quantile(sorted, 0.75)
        val iqr = q3 - q1
        val lowerFence = q1 - IQR_FACTOR * iqr
        val upperFence = q3 + IQR_FACTOR * iqr

        return values.indices.mapNotNull { index ->
            val value = values[index] ?: return@mapNotNull null
            val expected = expectedValues[index]?.takeIf { it > 0 } ?: return@mapNotNull null
            val deviation = value / expected - 1
            if (abs(deviation) >= MIN_RELATIVE_DEVIATION && (deviation < lowerFence || deviation > upperFence)) {
                Outlier(index, expected)
            } else {
                null
            }
        }
    }

    private fun median(sorted: List<Double>): Double {
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
    }

    private fun quantile(sorted: List<Double>, q: Double): Double {
        val position = (sorted.size - 1) * q
        val lower = floor(position).toInt()
        val upper = ceil(position).toInt()
        return sorted[lower] + (sorted[upper] - sorted[lower]) * (position - lower)
    }
}
