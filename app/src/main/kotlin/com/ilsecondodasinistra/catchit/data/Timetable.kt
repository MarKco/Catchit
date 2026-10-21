package com.ilsecondodasinistra.catchit.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ScheduledDeparture(
    val stopId: String,
    val routeId: String,
    val direction: Int,
    val headsign: String,
    val time: Instant,
)

/** Queries over the bundled [TimetableData]. */
class Timetable(private val data: TimetableData) {
    val stops: List<StopInfo> get() = data.stops
    val routes: Map<String, RouteInfo> = data.routes.associateBy { it.id }

    /** First day covered by the feed, null when the asset doesn't say. */
    val validFrom: LocalDate? = data.feedStart.toDateOrNull()
    val validUntil: LocalDate? = data.feedEnd.toDateOrNull()

    private val services = data.services.associateBy { it.id }

    fun scheduled(stopId: String, from: Instant, until: Instant, zone: ZoneId): List<ScheduledDeparture> {
        val firstDay = from.atZone(zone).toLocalDate().minusDays(1) // after-midnight runs of yesterday's service
        val lastDay = until.atZone(zone).toLocalDate()
        val out = ArrayList<ScheduledDeparture>()
        var day = firstDay
        while (!day.isAfter(lastDay)) {
            // GTFS: times count from "noon minus 12 hours", which survives DST changes.
            val base = day.atTime(12, 0).atZone(zone).minusHours(12).toInstant()
            for (pattern in data.patterns) {
                if (pattern.stop != stopId) continue
                val service = services[pattern.service] ?: continue
                if (!service.runsOn(day)) continue
                for (t in pattern.times) {
                    val time = base.plusSeconds(t.toLong())
                    if (time >= from && time <= until) {
                        out += ScheduledDeparture(stopId, pattern.route, pattern.direction, pattern.headsign, time)
                    }
                }
            }
            day = day.plusDays(1)
        }
        return out.sortedBy { it.time }
    }

    /** Destination for a realtime-only passage: the feed carries no headsign, so borrow it from the timetable. */
    fun headsign(stopId: String, routeId: String, direction: Int?): String? {
        val candidates = data.patterns.filter { it.stop == stopId && it.route == routeId }
        val byDirection = direction?.let { d -> candidates.filter { it.direction == d } }.orEmpty()
        return (byDirection.ifEmpty { candidates }).maxByOrNull { it.times.size }?.headsign
    }

    private fun ServiceInfo.runsOn(date: LocalDate): Boolean {
        val iso = date.format(DateTimeFormatter.BASIC_ISO_DATE)
        if (iso in removed) return false
        if (iso in added) return true
        return iso in start..end && days.getOrElse(date.dayOfWeek.ordinal) { false }
    }

    private fun String.toDateOrNull(): LocalDate? =
        runCatching { LocalDate.parse(this, DateTimeFormatter.BASIC_ISO_DATE) }.getOrNull()
}
