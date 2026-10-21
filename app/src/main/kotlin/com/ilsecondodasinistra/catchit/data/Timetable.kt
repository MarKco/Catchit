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
    /** Where it gets to the other end of the leg, after [durationSeconds]; the stops in between are in [shape]. */
    val alightStopId: String? = null,
    val durationSeconds: Int? = null,
    val shape: ShapeInfo? = null,
)

/** What a run of a line usually looks like, for live passages that match no timetable row. */
data class TypicalRun(val headsign: String, val alightStopId: String?, val durationSeconds: Int?, val shape: ShapeInfo?)

/** A line at a stop: what a live update must match to belong to a leg. */
data class StopRoute(val stopId: String, val routeId: String)

/**
 * Queries over one or more [TimetableData] versions (the bundled one and those downloaded later). Each day is
 * answered by the version in force that day, so an AVM timetable published in advance doesn't replace the current
 * one before its start date.
 */
class Timetable(datas: List<TimetableData>) {
    constructor(data: TimetableData) : this(listOf(data))

    private class Snapshot(val data: TimetableData) {
        val validFrom: LocalDate? = data.feedStart.toDateOrNull()
        val services = data.services.associateBy { it.id }
        val patternsByLeg = data.patterns.groupBy { it.destination to it.leg }
        val shapes = data.shapes.associateBy { it.id }
    }

    /** Oldest first. */
    private val snapshots = datas.map(::Snapshot).sortedBy { it.validFrom ?: LocalDate.MIN }

    init {
        require(snapshots.isNotEmpty()) { "At least one timetable is needed" }
    }

    val destinations: List<DestinationInfo> = snapshots.last().data.destinations

    /** A route keeps the colors of its newest version. */
    val routes: Map<String, RouteInfo> = snapshots.flatMap { it.data.routes }.associateBy { it.id }

    /** Every stop the app departs from: the realtime feed is filtered down to these. */
    val stopIds: Set<String> = snapshots.flatMapTo(HashSet()) { s -> s.data.patterns.map { it.stop } }

    /** First day covered by any version, null when the asset doesn't say. Before it, timetables are indicative. */
    val validFrom: LocalDate? = snapshots.first().validFrom

    /** Last day covered by any version: the newest one's end. */
    val validUntil: LocalDate? = snapshots.mapNotNull { it.data.feedEnd.toDateOrNull() }.maxOrNull()

    /** Lines of the newest versions by name, for the settings page. */
    val lineNames: List<String> = routes.values.map { it.name }.distinct().sortedWith(compareBy({ it.length }, { it }))

    /** The version in force on [day]: the newest one that has started; before all of them, the oldest (indicative). */
    private fun snapshotFor(day: LocalDate): Snapshot =
        snapshots.lastOrNull { it.validFrom == null || it.validFrom <= day } ?: snapshots.first()

    fun scheduled(destinationId: String, leg: Leg, from: Instant, until: Instant, zone: ZoneId): List<ScheduledDeparture> {
        val firstDay = from.atZone(zone).toLocalDate().minusDays(1) // after-midnight runs of yesterday's service
        val lastDay = until.atZone(zone).toLocalDate()
        val out = ArrayList<ScheduledDeparture>()
        var day = firstDay
        while (!day.isAfter(lastDay)) {
            val snapshot = snapshotFor(day)
            // GTFS: times count from "noon minus 12 hours", which survives DST changes.
            val base = day.atTime(12, 0).atZone(zone).minusHours(12).toInstant()
            for (pattern in snapshot.patternsByLeg[destinationId to leg.id].orEmpty()) {
                val service = snapshot.services[pattern.service] ?: continue
                if (!service.runsOn(day, snapshot)) continue
                val shape = snapshot.shapes[pattern.shape]
                for ((i, t) in pattern.times.withIndex()) {
                    val time = base.plusSeconds(t.toLong())
                    if (time >= from && time <= until) {
                        out += ScheduledDeparture(
                            pattern.stop, pattern.route, pattern.direction, pattern.headsign, time,
                            alightStopId = pattern.alight.ifEmpty { null },
                            durationSeconds = pattern.durations.getOrNull(i),
                            shape = shape,
                        )
                    }
                }
            }
            day = day.plusDays(1)
        }
        return out.sortedBy { it.time }
    }

    /** Lines that serve a leg, by stop: other lines passing the same poles are not part of it. */
    fun stopRoutes(destinationId: String, leg: Leg): Set<StopRoute> =
        snapshots.flatMapTo(HashSet()) { s ->
            s.patternsByLeg[destinationId to leg.id].orEmpty().map { StopRoute(it.stop, it.route) }
        }

    /** A stop by id, newest version first (names and coordinates). */
    fun stop(id: String): StopInfo? = snapshots.asReversed().firstNotNullOfOrNull { s -> s.data.stops.firstOrNull { it.id == id } }

    /** The run to borrow for a realtime-only passage: the busiest timetable row of that line at that stop. */
    fun typical(destinationId: String, leg: Leg, stopId: String, routeId: String, direction: Int?): TypicalRun? =
        snapshots.asReversed().firstNotNullOfOrNull { s ->
            val candidates = s.patternsByLeg[destinationId to leg.id].orEmpty().filter { it.stop == stopId && it.route == routeId }
            val byDirection = direction?.let { d -> candidates.filter { it.direction == d } }.orEmpty()
            (byDirection.ifEmpty { candidates }).maxByOrNull { it.times.size }?.let { p ->
                TypicalRun(
                    headsign = p.headsign,
                    alightStopId = p.alight.ifEmpty { null },
                    durationSeconds = p.durations.sorted().let { if (it.isEmpty()) null else it[it.size / 2] },
                    shape = s.shapes[p.shape],
                )
            }
        }

    /** Destination for a realtime-only passage: the feed carries no headsign, so borrow it from the timetable. */
    fun headsign(destinationId: String, leg: Leg, stopId: String, routeId: String, direction: Int?): String? =
        snapshots.asReversed().firstNotNullOfOrNull { s ->
            val candidates = s.patternsByLeg[destinationId to leg.id].orEmpty().filter { it.stop == stopId && it.route == routeId }
            val byDirection = direction?.let { d -> candidates.filter { it.direction == d } }.orEmpty()
            (byDirection.ifEmpty { candidates }).maxByOrNull { it.times.size }?.headsign
        }

    private fun ServiceInfo.runsOn(date: LocalDate, snapshot: Snapshot): Boolean {
        val iso = date.format(DateTimeFormatter.BASIC_ISO_DATE)
        if (iso in removed) return false
        if (iso in added) return true
        // Before the first feed starts, its timetable stands in for the old one (indicative: AVM's previous version
        // isn't published). Only for services that begin with the feed, not for later seasonal ones.
        val from = if (snapshot.validFrom != null && date < snapshot.validFrom && start == snapshot.data.feedStart) "" else start
        return iso >= from && iso <= end && days.getOrElse(date.dayOfWeek.ordinal) { false }
    }
}

private fun String.toDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this, DateTimeFormatter.BASIC_ISO_DATE) }.getOrNull()
