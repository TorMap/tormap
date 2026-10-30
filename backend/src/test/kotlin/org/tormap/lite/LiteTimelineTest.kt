package org.tormap.lite

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.tormap.adapter.dto.DailyRelayCountDto
import java.awt.Color
import java.io.ByteArrayInputStream
import java.time.LocalDate
import javax.imageio.ImageIO

class LiteTimelineTest : StringSpec({
    val start = LocalDate.of(2024, 1, 1)
    val days = (0L until 100L).map { start.plusDays(it) }
    fun counts(total: (Int) -> Long) = days.mapIndexed { i, day -> DailyRelayCountDto(day, total(i), 0, 0) }

    "picks the day at a position" {
        val timeline = LiteTimeline(days, counts { 1000L + it })
        timeline.dayAt(0, 1024) shouldBe days.first()
        timeline.dayAt(1024, 1024) shouldBe days.last()
        timeline.dayAt(512, 1024) shouldBe days[50]
        timeline.dayAt(-5, 1024) shouldBe days.first()
        timeline.dayAt(5000, 1024) shouldBe days.last()
        LiteTimeline(emptyList(), emptyList()).dayAt(10, 1024) shouldBe null
    }

    "lists days with an unusual number of relays" {
        val timeline = LiteTimeline(days, counts { if (it == 60) 500L else 1000L + it * 2 + (it % 5) * 3 })
        timeline.outliers.map { it.day } shouldBe listOf(days[60])
        timeline.outliers[0].relayCount shouldBe 500L
        // The median of the surrounding days is about 1126 relays
        timeline.outliers[0].changePercent shouldBe -56
    }

    "draws the counts and the selected day" {
        val timeline = LiteTimeline(days, counts { 1000L + it })
        timeline.isShown shouldBe true
        timeline.minCount shouldBe 1000L
        timeline.maxCount shouldBe 1099L
        val image = ImageIO.read(ByteArrayInputStream(timeline.render(640, days[50])))
        image.width shouldBe 640
        image.height shouldBe LiteTimeline.HEIGHT
        // The selected day is a vertical line through the whole image
        val x = (50.0 / 99 * 640).toInt()
        Color(image.getRGB(x, 1)) shouldBe Color(image.getRGB(x, LiteTimeline.HEIGHT - 2))
        (Color(image.getRGB(x, 1)) != Color(0x2c353c)) shouldBe true
    }

    "is not shown without counts" {
        LiteTimeline(days, emptyList()).isShown shouldBe false
        LiteTimeline(days.take(1), counts { 1L }).isShown shouldBe false
    }
})
