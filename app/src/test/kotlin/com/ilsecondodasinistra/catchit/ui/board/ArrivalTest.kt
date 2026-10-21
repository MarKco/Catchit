package com.ilsecondodasinistra.catchit.ui.board

import com.ilsecondodasinistra.catchit.data.RouteInfo
import com.ilsecondodasinistra.catchit.domain.Departure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ArrivalTest {
    private val route = RouteInfo("R", "R", "", "112233", "FFFFFF", 3)
    private val at = Instant.parse("2026-10-22T08:00:00Z")

    private fun departure(live: Boolean = false, tripId: String? = null, alight: String? = "B", duration: Int? = 600) = Departure(
        route = route, headsign = "X", time = at, scheduled = at, isRealtime = live,
        stopId = "A", tripId = tripId, alightStopId = alight, durationSeconds = duration,
    )

    private fun state(vararg trips: Pair<String, Map<String, Instant>>, browsing: Boolean = false) = BoardUiState(
        isLoading = false,
        liveTrips = trips.associate { (id, times) -> id to LiveTrip(times, vehicle = null) },
        browsing = if (browsing) Browsing(LocalDate.of(2026, 10, 25), at) else null,
    )

    @Test
    fun theTimetableGivesDeparturePlusTheUsualDuration() {
        val arrival = arrivalOf(departure(), state())!!
        assertEquals(at.plusSeconds(600), arrival.time)
        assertEquals(false, arrival.live)
    }

    @Test
    fun aLiveTripWithAPredictionAtTheStopWhereItGetsOffWins() {
        val live = at.plusSeconds(777)
        val arrival = arrivalOf(departure(live = true, tripId = "T"), state("T" to mapOf("B" to live)))!!
        assertEquals(live, arrival.time)
        assertEquals(true, arrival.live)
    }

    @Test
    fun aLiveTripWithoutThatStopFallsBackToTheDuration() {
        val arrival = arrivalOf(departure(live = true, tripId = "T"), state("T" to mapOf("OTHER" to at)))!!
        assertEquals(at.plusSeconds(600), arrival.time)
        assertEquals(false, arrival.live)
    }

    @Test
    fun anotherDayIsAlwaysTheTimetable() {
        val live = at.plusSeconds(777)
        val arrival = arrivalOf(departure(live = true, tripId = "T"), state("T" to mapOf("B" to live), browsing = true))!!
        assertEquals(at.plusSeconds(600), arrival.time)
        assertEquals(false, arrival.live)
    }

    @Test
    fun nothingKnownMeansNoArrival() {
        assertNull(arrivalOf(departure(alight = null, duration = null), state()))
    }
}
