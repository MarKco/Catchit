package com.ilsecondodasinistra.catchit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ilsecondodasinistra.catchit.data.RouteInfo

/** Badge colors of a line. */
data class LineColors(val container: Color, val content: Color)

// ACTV paints every urban line the same red, which makes a list of passages unreadable at a glance. Each line gets
// its own hue instead (same hue for the same line, bus or tram), chosen far apart from the others.
private val known = mapOf(
    "T1" to Color(0xFFC62828), // the tram keeps the classic red
    "31H" to Color(0xFF1565C0),
    "32H" to Color(0xFF00838F),
    "12L" to Color(0xFF2E7D32),
    "15" to Color(0xFFEF6C00),
    "43" to Color(0xFFAD1457),
    "N1" to Color(0xFF4527A0),
    "N2" to Color(0xFF7B1FA2),
    "9S" to Color(0xFF6D4C41),
    "9E" to Color(0xFF455A64),
    "5E" to Color(0xFFF9A825),
)

/** Lines we have no color for (new ones in a downloaded timetable) get a stable hue from their name. */
private fun fallback(name: String): Color {
    val hue = ((name.hashCode() and 0x7fffffff) % 360).toFloat()
    return Color.hsv(hue, 0.65f, 0.55f)
}

fun RouteInfo.lineColors(): LineColors {
    val container = known[name.uppercase()] ?: fallback(name)
    return LineColors(container, if (container.luminance() > 0.4f) Color.Black else Color.White)
}
