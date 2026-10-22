package com.ilsecondodasinistra.catchit.domain

import com.ilsecondodasinistra.catchit.data.Boarding
import com.ilsecondodasinistra.catchit.data.Leg
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

    /** How far ahead the board looks. */
    val DEFAULT_HORIZON: Duration = Duration.ofHours(6)

    /** How far back the board still lists passages that have gone. */
    val DEFAULT_PAST: Duration = Duration.ofMinutes(15)

    fun merge(
        destinationId: String,
        leg: Leg,
        timetable: Timetable,
        scheduled: List<ScheduledDeparture>,
        updates: List<RealtimeUpdate>,
        now: Instant,
        limit: Int,
        horizon: Duration = DEFAULT_HORIZON,
        past: Duration = DEFAULT_PAST,
    ): List<Departure> {
        val free = scheduled.toMutableList()
        val out = ArrayList<Departure>()
        val ofLeg = timetable.boardings(destinationId, leg)

        // The feed has every line of a stop, in both directions (the tram that ends its run there too): only the
        // vehicles that leave in this leg's direction count. Without a direction in the feed, line and stop will do.
        val live = updates.filter { u ->
            u.directionId?.let { Boarding(u.stopId, u.routeId, it) in ofLeg } ?: ofLeg.any { it.stopId == u.stopId && it.routeId == u.routeId }
        }.sortedBy { it.epochSecond ?: Long.MAX_VALUE }
        for (update in live) {
            val route = timetable.routes[update.routeId] ?: continue
            val predicted = update.epochSecond?.let(Instant::ofEpochSecond)
            val implied = predicted?.let { p -> update.delaySeconds?.let { p.minusSeconds(it.toLong()) } }
            val reference = implied ?: predicted

            val match = reference?.let { ref ->
                free.filter { it.routeId == update.routeId && it.stopId == update.stopId }
                    .minByOrNull { abs(Duration.between(it.time, ref).seconds) }
                    ?.takeIf { abs(Duration.between(it.time, ref).seconds) <= MATCH_TOLERANCE.seconds }
            }
            match?.let(free::remove)

            val time = predicted ?: match?.time ?: continue
            // A live passage that matches no timetable row still needs a destination and a duration: borrow them.
            val typical = if (match == null) timetable.typical(destinationId, leg, update.stopId, update.routeId, update.directionId) else null
            val headsign = match?.headsign ?: typical?.headsign ?: route.longName
            out += Departure(
                route = route,
                headsign = headsign,
                time = time,
                scheduled = implied ?: match?.time,
                isRealtime = predicted != null && !update.tripCanceled,
                isCanceled = update.tripCanceled,
                stopId = update.stopId,
                tripId = update.tripId.ifEmpty { null },
                alightStopId = match?.alightStopId ?: typical?.alightStopId,
                durationSeconds = match?.durationSeconds ?: typical?.durationSeconds,
                shape = match?.shape ?: typical?.shape,
            )
        }

        for (s in free) {
            val route = timetable.routes[s.routeId] ?: continue
            out += Departure(
                route, s.headsign, s.time, s.time, isRealtime = false,
                stopId = s.stopId, alightStopId = s.alightStopId, durationSeconds = s.durationSeconds, shape = s.shape,
            )
        }

        val earliest = now.minus(past)
        val latest = now.plus(horizon)
        return out.filter { it.time in earliest..latest }.sortedBy { it.time }.take(limit)
    }
}
