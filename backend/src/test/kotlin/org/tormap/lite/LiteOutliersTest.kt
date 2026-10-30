package org.tormap.lite

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

// The same cases as the frontend's outliers.unit.test.ts
class LiteOutliersTest : StringSpec({
    // A slowly growing series with small deterministic noise
    fun series(length: Int): MutableList<Long?> = MutableList(length) { i -> 1000L + i * 2 + (i % 5) * 3 }

    "finds nothing in a smooth series" {
        LiteOutliers.find(series(100)) shouldBe emptyList()
    }

    "flags a sudden drop" {
        val values = series(100).also { it[50] = 400 }
        val outliers = LiteOutliers.find(values)
        outliers.map { it.index } shouldBe listOf(50)
        outliers[0].expected shouldBeGreaterThan 1000.0
    }

    "flags a sudden spike" {
        LiteOutliers.find(series(100).also { it[20] = 2500 }).map { it.index } shouldBe listOf(20)
    }

    "does not flag a permanent change of level" {
        val values = series(100).mapIndexed { i, value -> if (i >= 50) value!! + 600 else value }
        val indexes = LiteOutliers.find(values).map { it.index }
        indexes.all { it in 44..56 } shouldBe true
        indexes shouldNotContain 80
    }

    "ignores small fluctuations below the minimum deviation" {
        val values = MutableList<Long?>(60) { 1000 }.also { it[30] = 1050 }
        LiteOutliers.find(values) shouldBe emptyList()
    }

    "skips missing values" {
        LiteOutliers.find(series(60).also { it[10] = null; it[11] = null }) shouldBe emptyList()
    }

    "returns nothing for too short series" {
        LiteOutliers.find(emptyList()) shouldBe emptyList()
        LiteOutliers.find(listOf(1, 2, 3)) shouldBe emptyList()
    }
})
