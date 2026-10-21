package com.ilsecondodasinistra.catchit.ui.board

import com.ilsecondodasinistra.catchit.domain.Departure
import java.time.Instant

/** When a passage gets to the other end of the leg; [live] when the realtime feed said so, else timetable plus the usual duration. */
data class Arrival(val time: Instant, val live: Boolean)

/**
 * Same answer for the card and the sheet: the live arrival at the stop where it gets off when the feed has one for this
 * trip, otherwise the departure plus the usual duration of the run; null when neither is known.
 */
internal fun arrivalOf(departure: Departure, state: BoardUiState): Arrival? {
    val live = departure.tripId?.let { state.liveTrips[it] }.takeIf { departure.isRealtime && state.browsing == null }
    val liveArrival = departure.alightStopId?.let { live?.stopTimes?.get(it) }
    if (liveArrival != null) return Arrival(liveArrival, live = true)
    return departure.durationSeconds?.let { Arrival(departure.time.plusSeconds(it.toLong()), live = false) }
}
