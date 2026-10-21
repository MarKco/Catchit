package com.ilsecondodasinistra.catchit.domain

import com.ilsecondodasinistra.catchit.data.DestinationInfo
import com.ilsecondodasinistra.catchit.data.Leg
import com.ilsecondodasinistra.catchit.data.Pattern
import com.ilsecondodasinistra.catchit.data.RealtimeUpdate
import com.ilsecondodasinistra.catchit.data.RouteInfo
import com.ilsecondodasinistra.catchit.data.ServiceInfo
import com.ilsecondodasinistra.catchit.data.ShapeInfo
import com.ilsecondodasinistra.catchit.data.StopInfo
import com.ilsecondodasinistra.catchit.data.Timetable
import com.ilsecondodasinistra.catchit.data.TimetableData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Arrival, duration and intermediate stops travel from the timetable to the passage the sheet shows. */
class DepartureDetailsTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val day = LocalDate.of(2026, 10, 22)
    private fun at(h: Int, m: Int): Instant = day.atTime(h, m).atZone(zone).toInstant()

    private val shape = ShapeInfo(0, listOf("A", "M", "B"), listOf(0, 300, 720))

    private val timetable = Timetable(
        TimetableData(
            feedStart = "20261022",
            feedEnd = "20271021",
            routes = listOf(RouteInfo("R", "R", "Alfa - Beta", "112233", "FFFFFF", 3)),
            stops = listOf(StopInfo("A", "Alfa", 45.0, 12.0), StopInfo("M", "Mezzo", 45.1, 12.1), StopInfo("B", "Beta", 45.2, 12.2)),
            shapes = listOf(shape),
            destinations = listOf(DestinationInfo("d", "D", "Alfa", "Beta")),
            services = listOf(ServiceInfo("s", List(7) { true }, "20261022", "20271021")),
            patterns = listOf(
                Pattern("d", "andata", "A", "R", 0, "Beta", alight = "B", shape = 0, service = "s",
                    times = listOf(8 * 3600, 9 * 3600), durations = listOf(720, 780)),
            ),
        ),
    )

    private fun scheduled() = timetable.scheduled("d", Leg.Outbound, at(7, 0), at(12, 0), zone)

    @Test
    fun aTimetableRowKnowsWhereAndWhenItGetsOffAndThroughWhichStops() {
        val rows = DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), emptyList(), at(7, 55), 10)
        val first = rows.first()
        assertEquals("A", first.stopId)
        assertEquals("B", first.alightStopId)
        assertEquals(720, first.durationSeconds)
        assertEquals(shape, first.shape)
        assertEquals(780, rows[1].durationSeconds) // each departure has its own duration
    }

    @Test
    fun aLivePassageMatchedToARowKeepsTheRowsDetailsAndTheTripId() {
        val live = RealtimeUpdate("TRIP-1", "R", 0, "A", at(8, 3).epochSecond, 180, false)
        val first = DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), listOf(live), at(7, 55), 10).first()
        assertEquals(true, first.isRealtime)
        assertEquals("TRIP-1", first.tripId)
        assertEquals("B", first.alightStopId)
        assertEquals(720, first.durationSeconds)
        assertEquals(shape, first.shape)
    }

    @Test
    fun aLiveOnlyPassageBorrowsTheTypicalRun() {
        val live = RealtimeUpdate("TRIP-2", "R", 0, "A", at(10, 30).epochSecond, 0, false) // no 10:30 on the timetable
        val row = DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), listOf(live), at(7, 55), 10).single { it.tripId == "TRIP-2" }
        assertEquals("Beta", row.headsign)
        assertEquals("B", row.alightStopId)
        assertEquals(780, row.durationSeconds) // upper median of 720 and 780
        assertEquals(shape, row.shape)
    }

    @Test
    fun timetableRowsHaveNoTripId() {
        assertNull(DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), emptyList(), at(7, 55), 10).first().tripId)
    }

    @Test
    fun stopNamesAndCoordinatesCanBeLookedUp() {
        assertEquals("Mezzo", timetable.stop("M")?.name)
        assertNotNull(timetable.stop("M")?.lat)
        assertNull(timetable.stop("nope"))
    }
}
