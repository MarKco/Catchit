package com.ilsecondodasinistra.catchit.ui.board

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Straight-line distance in metres between two points on Earth (haversine). */
internal fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * r * asin(sqrt(a))
}

/** "350 m", "2,4 km". */
internal fun formatDistance(meters: Double): String =
    if (meters < 950) "${(meters / 10).toInt() * 10} m" else String.format(Locale.ITALIAN, "%.1f km", meters / 1000)
