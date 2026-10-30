package org.tormap.lite

import org.tormap.adapter.dto.DailyRelayCountDto
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/**
 * The number of relays per day, like the timeline above the date slider of the JavaScript map
 * (`RelayCountTimeline.tsx`). The days are spread evenly over the width, so a click picks a day by its position.
 * @param days - all days with relay locations, ordered
 * @param counts - the number of relays of each day
 */
class LiteTimeline(val days: List<LocalDate>, counts: List<DailyRelayCountDto>) {
    /** A day with an unusual number of relays */
    class Outlier(val day: LocalDate, val relayCount: Long, val changePercent: Int)

    private val totals: List<Long?> = counts.associate { it.day to it.total }.let { totalByDay -> days.map { totalByDay[it] } }

    /** Whether there is anything to draw */
    val isShown = days.size >= 2 && totals.any { it != null }

    val minCount = totals.filterNotNull().minOrNull() ?: 0
    val maxCount = totals.filterNotNull().maxOrNull() ?: 0

    val outliers: List<Outlier> = LiteOutliers.find(totals).map { outlier ->
        val count = totals[outlier.index]!!
        Outlier(days[outlier.index], count, ((count / outlier.expected - 1) * 100).roundToInt())
    }

    /** The day at a horizontal pixel of an image of the given width */
    fun dayAt(x: Int, width: Int): LocalDate? {
        if (days.isEmpty()) return null
        if (days.size == 1) return days.first()
        val index = (x.coerceIn(0, width).toDouble() / width * days.lastIndex).roundToInt()
        return days[index]
    }

    fun render(width: Int, selectedDay: LocalDate?): ByteArray {
        val image = BufferedImage(width, HEIGHT, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            graphics.color = BACKGROUND
            graphics.fillRect(0, 0, width, HEIGHT)
            if (isShown) {
                val range = maxOf(maxCount - minCount, 1).toDouble()
                fun x(index: Int) = index.toDouble() / days.lastIndex * width
                fun y(count: Long) = HEIGHT - PADDING - (count - minCount) / range * (HEIGHT - 2 * PADDING)

                // Missing days break the line instead of interpolating over them
                val line = Path2D.Double()
                var drawing = false
                totals.forEachIndexed { index, count ->
                    if (count == null) {
                        drawing = false
                    } else if (drawing) {
                        line.lineTo(x(index), y(count))
                    } else {
                        line.moveTo(x(index), y(count))
                        drawing = true
                    }
                }
                graphics.color = LINE
                graphics.stroke = BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                graphics.draw(line)

                selectedDay?.let { days.indexOf(it) }?.takeIf { it >= 0 }?.let { index ->
                    graphics.color = SELECTED
                    graphics.stroke = BasicStroke(1f)
                    graphics.draw(Line2D.Double(x(index), 0.0, x(index), HEIGHT.toDouble()))
                }

                graphics.color = OUTLIER
                outliers.forEach { outlier ->
                    val index = days.indexOf(outlier.day)
                    graphics.fill(RoundRectangle2D.Double(x(index) - 2, y(outlier.relayCount) - 8, 4.0, 16.0, 4.0, 4.0))
                }
            }
        } finally {
            graphics.dispose()
        }
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }

    companion object {
        const val HEIGHT = 64
        private const val PADDING = 6
        private val BACKGROUND = Color(0x2c353c)
        // The colors of MUI's dark theme that the JavaScript timeline uses: primary, secondary text and warning
        private val LINE = Color(0x90caf9)
        private val SELECTED = Color(0xb3b3b3)
        private val OUTLIER = Color(0xffa726)
    }
}
