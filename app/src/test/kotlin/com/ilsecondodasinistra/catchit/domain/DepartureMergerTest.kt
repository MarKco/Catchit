package com.ilsecondodasinistra.catchit.domain

import com.ilsecondodasinistra.catchit.data.Pattern
import com.ilsecondodasinistra.catchit.data.RealtimeUpdate
import com.ilsecondodasinistra.catchit.data.RouteInfo
import com.ilsecondodasinistra.catchit.data.ServiceInfo
import com.ilsecondodasinistra.catchit.data.StopInfo
import com.ilsecondodasinistra.catchit.data.Timetable
import com.ilsecondodasinistra.catchit.data.TimetableData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DepartureMergerTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val day = LocalDate.of(2026, 10, 22) // a Thursday

    private fun at(h: Int, m: Int, s: Int = 0): Instant = day.atTime(h, m, s).atZone(zone).toInstant()
    private fun seconds(h: Int, m: Int) = h * 3600 + m * 60

    private val timetable = Timetable(
        TimetableData(
            feedStart = "20261022",
            feedEnd = "20271021",
            routes = listOf(
                RouteInfo("T1_UM", "T1", "Favaro - Venezia", "E31E24", "FFFFFF", 0),
                RouteInfo("31H_UM", "31H", "Mestre loop", "E31E24", "FFFFFF", 3),
            ),
            stops = listOf(StopInfo("6059", "San Marco Musatti", "Tram per Venezia")),
            services = listOf(
                ServiceInfo("weekday", listOf(true, true, true, true, true, false, false), "20261022", "20271021"),
                ServiceInfo("holiday", listOf(false, false, false, false, false, false, false), "20261022", "20271021", added = listOf("20261023")),
            ),
            patterns = listOf(
                Pattern("6059", "T1_UM", 0, "VENEZIA CORSIA C", "weekday", listOf(seconds(8, 0), seconds(8, 10), seconds(8, 20))),
                Pattern("6059", "31H_UM", 0, "Ospedale dell'Angelo", "weekday", listOf(seconds(8, 5))),
                Pattern("6059", "T1_UM", 0, "VENEZIA CORSIA C", "holiday", listOf(seconds(9, 0))),
            ),
        ),
    )

    private fun scheduled(from: Instant = at(7, 0)) = timetable.scheduled("6059", from, from.plusSeconds(6 * 3600), zone)

    private fun update(route: String, time: Instant, delay: Int?, canceled: Boolean = false) =
        RealtimeUpdate("trip", route, 0, "6059", time.epochSecond, delay, canceled)

    @Test
    fun scheduledFollowsServiceCalendar() {
        val times = scheduled().map { it.time }
        assertEquals(listOf(at(8, 0), at(8, 5), at(8, 10), at(8, 20)), times)
        // Friday 23rd: the weekday service plus the extra "holiday" run added for that date.
        val friday = timetable.scheduled("6059", at(7, 0).plusSeconds(86_400), at(23, 0).plusSeconds(86_400), zone)
        assertEquals(5, friday.size)
        assertTrue(friday.any { it.time == day.plusDays(1).atTime(9, 0).atZone(zone).toInstant() })
        // Saturday 24th: nothing runs.
        assertTrue(timetable.scheduled("6059", at(7, 0).plusSeconds(2 * 86_400), at(23, 0).plusSeconds(2 * 86_400), zone).isEmpty())
    }

    @Test
    fun liveTramReplacesItsTimetableRow() {
        val live = update("T1_UM", at(8, 13), delay = 180) // timetable 08:10, three minutes late
        val merged = DepartureMerger.merge("6059", timetable, scheduled(), listOf(live), at(7, 55), 10)

        val t1 = merged.filter { it.route.id == "T1_UM" }
        assertEquals(listOf(at(8, 0), at(8, 13), at(8, 20)), t1.map { it.time })
        val delayed = t1[1]
        assertTrue(delayed.isRealtime)
        assertEquals(at(8, 10), delayed.scheduled)
        assertEquals(180, delayed.delay!!.seconds)
        assertEquals("VENEZIA CORSIA C", delayed.headsign)
        assertFalse(t1[0].isRealtime)
    }

    @Test
    fun liveOnlyPassageWorksWithoutTimetableForToday() {
        val empty = Timetable(TimetableData(routes = timetable.routes.values.toList(), stops = timetable.stops, services = emptyList(), patterns = timetable.let {
            listOf(Pattern("6059", "31H_UM", 0, "Ospedale dell'Angelo", "none", listOf(0)))
        }))
        val live = update("31H_UM", at(8, 2), delay = 107)
        val merged = DepartureMerger.merge("6059", empty, emptyList(), listOf(live), at(8, 0), 10)

        assertEquals(1, merged.size)
        assertEquals("Ospedale dell'Angelo", merged.single().headsign)
        assertEquals(at(8, 2).minusSeconds(107), merged.single().scheduled)
    }

    @Test
    fun canceledTripIsKeptAndFlagged() {
        val live = update("T1_UM", at(8, 10), delay = 0, canceled = true)
        val merged = DepartureMerger.merge("6059", timetable, scheduled(), listOf(live), at(7, 55), 10)
        val canceled = merged.single { it.isCanceled }
        assertEquals(at(8, 10), canceled.time)
        assertFalse(canceled.isRealtime)
        assertEquals(3, merged.count { it.route.id == "T1_UM" }) // 08:00, canceled 08:10, 08:20: no duplicate of 08:10
    }

    @Test
    fun passedDeparturesDropAndLimitApplies() {
        val merged = DepartureMerger.merge("6059", timetable, scheduled(), emptyList(), at(8, 12), 2)
        assertEquals(listOf(at(8, 20)), merged.map { it.time })
        assertEquals(2, DepartureMerger.merge("6059", timetable, scheduled(), emptyList(), at(7, 0), 2).size)
    }

    @Test
    fun otherLineWithinToleranceIsNotMatched() {
        val live = update("31H_UM", at(8, 10), delay = 0) // 31H is due at 08:05 only: 5 minutes apart
        val merged = DepartureMerger.merge("6059", timetable, scheduled(), listOf(live), at(7, 55), 10)
        assertEquals(2, merged.count { it.route.id == "31H_UM" })
    }
}
