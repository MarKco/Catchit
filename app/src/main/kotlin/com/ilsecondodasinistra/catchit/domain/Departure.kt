package com.ilsecondodasinistra.catchit.domain

import com.ilsecondodasinistra.catchit.data.RouteInfo
import com.ilsecondodasinistra.catchit.data.ShapeInfo
import java.time.Duration
import java.time.Instant

/** A passage shown on the board: either live (predicted by the AVM feed) or taken from the timetable. */
data class Departure(
    val route: RouteInfo,
    val headsign: String,
    /** Best estimate: the prediction when [isRealtime], the timetable otherwise. */
    val time: Instant,
    /** Timetable time, when known. */
    val scheduled: Instant?,
    val isRealtime: Boolean,
    val isCanceled: Boolean = false,
    /** Where it leaves from. */
    val stopId: String = "",
    /** The realtime feed's id of the trip (only for live passages): ties it to a vehicle. */
    val tripId: String? = null,
    /** Where it gets to the other end of the leg, after [durationSeconds]; stops in between in [shape]. */
    val alightStopId: String? = null,
    val durationSeconds: Int? = null,
    val shape: ShapeInfo? = null,
) {
    /** Positive = late. Only meaningful for live passages with a known timetable time. */
    val delay: Duration? get() = if (isRealtime && scheduled != null) Duration.between(scheduled, time) else null
}

private val PAST_GRACE: Duration = Duration.ofSeconds(30)

/** Gone by more than a moment: such rows are shown dimmed above the next passage. */
fun Departure.isPast(now: Instant): Boolean = time < now.minus(PAST_GRACE)
