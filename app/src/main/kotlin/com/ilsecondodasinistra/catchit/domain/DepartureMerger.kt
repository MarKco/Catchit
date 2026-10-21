package com.ilsecondodasinistra.catchit.domain

import com.ilsecondodasinistra.catchit.data.RealtimeUpdate
import com.ilsecondodasinistra.catchit.data.ScheduledDeparture
import com.ilsecondodasinistra.catchit.data.Timetable
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

/**
 * Joins the timetable with the live feed.
 *
 * GTFS-RT `trip_id`s carry the feed version (`AUT_ACTV_792_…`) and change whenever AVM publishes a new timetable,
 * so they can't be joined to the bundled one. A live passage is matched to a scheduled one by line and by
 * the timetable time it implies (`time - delay`) instead; whatever stays unmatched is shown as it comes.
 */
object DepartureMerger {
    private val MATCH_TOLERANCE = Duration.ofSeconds(150)

    /** Live passages already gone by more than this are dropped. */
    private val GRACE = Duration.ofSeconds(30)

    fun merge(
        stopId: String,
        timetable: Timetable,
        scheduled: List<ScheduledDeparture>,
        updates: List<RealtimeUpdate>,
        now: Instant,
        limit: Int,
    ): List<Departure> {
        val free = scheduled.toMutableList()
        val out = ArrayList<Departure>()

        for (update in updates.filter { it.stopId == stopId }.sortedBy { it.epochSecond ?: Long.MAX_VALUE }) {
            val route = timetable.routes[update.routeId] ?: continue
            val predicted = update.epochSecond?.let(Instant::ofEpochSecond)
            val implied = predicted?.let { p -> update.delaySeconds?.let { p.minusSeconds(it.toLong()) } }
            val reference = implied ?: predicted

            val match = reference?.let { ref ->
                free.filter { it.routeId == update.routeId }
                    .minByOrNull { abs(Duration.between(it.time, ref).seconds) }
                    ?.takeIf { abs(Duration.between(it.time, ref).seconds) <= MATCH_TOLERANCE.seconds }
            }
            match?.let(free::remove)

            val time = predicted ?: match?.time ?: continue
            val headsign = match?.headsign ?: timetable.headsign(stopId, update.routeId, update.directionId) ?: route.longName
            out += Departure(
                route = route,
                headsign = headsign,
                time = time,
                scheduled = implied ?: match?.time,
                isRealtime = predicted != null && !update.tripCanceled,
                isCanceled = update.tripCanceled,
            )
        }

        for (s in free) {
            val route = timetable.routes[s.routeId] ?: continue
            out += Departure(route, s.headsign, s.time, s.time, isRealtime = false)
        }

        val earliest = now.minus(GRACE)
        return out.filter { it.time >= earliest }.sortedBy { it.time }.take(limit)
    }
}
