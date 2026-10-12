package org.tormap.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PathSelectionTest : StringSpec({
    val weights = mapOf("Wgg" to 5000, "Wgd" to 2500, "Wgm" to 0)
    val running = setOf("Running", "Valid")

    "guard relays use Wgg" {
        PathSelection.guardWeight(1000, running + "Guard", weights) shouldBe 500
    }

    "guard relays that are also exits use Wgd" {
        PathSelection.guardWeight(1000, running + "Guard" + "Exit", weights) shouldBe 250
    }

    "relays without guard flag use Wgm" {
        PathSelection.guardWeight(1000, running + "Exit", weights) shouldBe 0
        PathSelection.guardWeight(1000, running, mapOf("Wgm" to 1000)) shouldBe 100
    }

    "relays that are not running or valid can not be picked" {
        PathSelection.guardWeight(1000, setOf("Valid", "Guard"), weights) shouldBe 0
        PathSelection.guardWeight(1000, setOf("Running", "Guard"), weights) shouldBe 0
    }

    "the result is rounded" {
        PathSelection.guardWeight(3, running + "Guard", mapOf("Wgg" to 5000)) shouldBe 2
    }

    "missing weights result in null" {
        PathSelection.guardWeight(1000, running + "Guard", null) shouldBe null
        PathSelection.guardWeight(1000, running + "Guard", mapOf("Wmm" to 10000)) shouldBe null
    }
})
