package com.ilsecondodasinistra.catchit.domain

import com.ilsecondodasinistra.catchit.data.DestinationInfo
import com.ilsecondodasinistra.catchit.data.Leg
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
                RouteInfo("12L_UM", "12L", "Venezia - 27 Ottobre", "E31E24", "FFFFFF", 3),
            ),
            stops = listOf(StopInfo("6059", "San Marco Musatti"), StopInfo("6060", "San Marco Musatti")),
            destinations = listOf(DestinationInfo("d", "Dest", "Musatti", "Venezia")),
            services = listOf(
                ServiceInfo("weekday", listOf(true, true, true, true, true, false, false), "20261022", "20271021"),
                ServiceInfo("holiday", listOf(false, false, false, false, false, false, false), "20261022", "20271021", added = listOf("20261023")),
            ),
            patterns = listOf(
                Pattern("d", "andata", "6059", "T1_UM", 0, "VENEZIA CORSIA C", "weekday", listOf(seconds(8, 0), seconds(8, 10), seconds(8, 20))),
                Pattern("d", "andata", "6059", "31H_UM", 0, "Ospedale dell'Angelo", "weekday", listOf(seconds(8, 5))),
                Pattern("d", "andata", "6059", "T1_UM", 0, "VENEZIA CORSIA C", "holiday", listOf(seconds(9, 0))),
                // The other direction of the same line, at the other pole: not part of the "andata" leg.
                Pattern("d", "ritorno", "6060", "T1_UM", 1, "Monte Celo Favaro", "weekday", listOf(seconds(8, 2))),
            ),
        ),
    )

    private fun scheduled(from: Instant = at(7, 0)) = timetable.scheduled("d", Leg.Outbound, from, from.plusSeconds(6 * 3600), zone)

    private fun update(route: String, time: Instant, delay: Int?, canceled: Boolean = false) =
        RealtimeUpdate("trip", route, 0, "6059", time.epochSecond, delay, canceled)

    @Test
    fun scheduledFollowsServiceCalendar() {
        val times = scheduled().map { it.time }
        assertEquals(listOf(at(8, 0), at(8, 5), at(8, 10), at(8, 20)), times)
        // Friday 23rd: the weekday service plus the extra "holiday" run added for that date.
        val friday = timetable.scheduled("d", Leg.Outbound, at(7, 0).plusSeconds(86_400), at(23, 0).plusSeconds(86_400), zone)
        assertEquals(5, friday.size)
        assertTrue(friday.any { it.time == day.plusDays(1).atTime(9, 0).atZone(zone).toInstant() })
        // Saturday 24th: nothing runs.
        assertTrue(timetable.scheduled("d", Leg.Outbound, at(7, 0).plusSeconds(2 * 86_400), at(23, 0).plusSeconds(2 * 86_400), zone).isEmpty())
    }

    @Test
    fun liveTramReplacesItsTimetableRow() {
        val live = update("T1_UM", at(8, 13), delay = 180) // timetable 08:10, three minutes late
        val merged = DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), listOf(live), at(7, 55), 10)

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
        val empty = Timetable(
            TimetableData(
                routes = timetable.routes.values.toList(),
                stops = emptyList(),
                destinations = emptyList(),
                services = emptyList(),
                patterns = listOf(Pattern("d", "andata", "6059", "31H_UM", 0, "Ospedale dell'Angelo", "none", listOf(0))),
            ),
        )
        val live = update("31H_UM", at(8, 2), delay = 107)
        val merged = DepartureMerger.merge("d", Leg.Outbound, empty, emptyList(), listOf(live), at(8, 0), 10)

        assertEquals(1, merged.size)
        assertEquals("Ospedale dell'Angelo", merged.single().headsign)
        assertEquals(at(8, 2).minusSeconds(107), merged.single().scheduled)
    }

    @Test
    fun canceledTripIsKeptAndFlagged() {
        val live = update("T1_UM", at(8, 10), delay = 0, canceled = true)
        val merged = DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), listOf(live), at(7, 55), 10)
        val canceled = merged.single { it.isCanceled }
        assertEquals(at(8, 10), canceled.time)
        assertFalse(canceled.isRealtime)
        assertEquals(3, merged.count { it.route.id == "T1_UM" }) // 08:00, canceled 08:10, 08:20: no duplicate of 08:10
    }

    @Test
    fun recentlyGonePassagesStayForFifteenMinutes() {
        val at812 = DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), emptyList(), at(8, 12), 10).map { it.time }
        assertEquals(listOf(at(8, 0), at(8, 5), at(8, 10), at(8, 20)), at812) // 08:00 left 12 min ago
        val at816 = DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), emptyList(), at(8, 16), 10).map { it.time }
        assertEquals(listOf(at(8, 5), at(8, 10), at(8, 20)), at816) // 08:00 is now 16 min old
        assertTrue(DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), emptyList(), at(8, 12), 10).first().isPast(at(8, 12)))
    }

    @Test
    fun limitApplies() {
        assertEquals(2, DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), emptyList(), at(7, 0), 2).size)
    }

    @Test
    fun sixHoursPastMidnightIncludeTomorrow() {
        val from = at(23, 0)
        val rows = timetable.scheduled("d", Leg.Outbound, from, from.plusSeconds(9 * 3600), zone)
        assertTrue(rows.any { it.time == day.plusDays(1).atTime(8, 0).atZone(zone).toInstant() })
    }

    @Test
    fun beforeTheFeedStartsTheFirstTimetableIsIndicative() {
        val dayBefore = day.minusDays(1) // Wednesday 21st, feed starts on the 22nd
        val from = dayBefore.atTime(7, 0).atZone(zone).toInstant()
        val rows = timetable.scheduled("d", Leg.Outbound, from, from.plusSeconds(3600 * 2), zone)
        assertEquals(dayBefore.atTime(8, 0).atZone(zone).toInstant(), rows.first().time)
    }

    @Test
    fun otherLineWithinToleranceIsNotMatched() {
        val live = update("31H_UM", at(8, 10), delay = 0) // 31H is due at 08:05 only: 5 minutes apart
        val merged = DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), listOf(live), at(7, 55), 10)
        assertEquals(2, merged.count { it.route.id == "31H_UM" })
    }

    @Test
    fun horizonCutsFarPassages() {
        val far = update("T1_UM", at(8, 0).plusSeconds(7 * 3600), delay = 0) // 15:00, beyond 6 h from 07:55
        val merged = DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), listOf(far), at(7, 55), 50)
        assertTrue(merged.none { it.time > at(7, 55).plusSeconds(6 * 3600) })
        assertEquals(4, merged.size)
    }

    @Test
    fun liveUpdatesOfOtherLinesOrPolesAreIgnored() {
        val otherPole = RealtimeUpdate("t", "T1_UM", 1, "6060", at(8, 4).epochSecond, 0, false) // "ritorno" pole
        val otherLine = RealtimeUpdate("t", "12L_UM", 0, "6059", at(8, 6).epochSecond, 0, false) // 12L isn't on this leg
        val merged = DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), listOf(otherPole, otherLine), at(7, 55), 10)
        assertFalse(merged.any { it.isRealtime })
        assertEquals(4, merged.size)
    }

    @Test
    fun oppositeDirectionArrivingAtTheBoardingPoleIsNotADeparture() {
        // The same line and pole, but the vehicle is ending its run there (direction 1): it arrives, it doesn't leave.
        val arriving = RealtimeUpdate("t", "T1_UM", 1, "6059", at(8, 7).epochSecond, 0, false)
        val merged = DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), listOf(arriving), at(7, 55), 10)
        assertFalse(merged.any { it.isRealtime })
        assertEquals(listOf(at(8, 0), at(8, 5), at(8, 10), at(8, 20)), merged.map { it.time })
    }

    @Test
    fun updateWithoutDirectionFallsBackToStopAndLine() {
        val noDirection = RealtimeUpdate("t", "T1_UM", null, "6059", at(8, 10).epochSecond, 0, false)
        val merged = DepartureMerger.merge("d", Leg.Outbound, timetable, scheduled(), listOf(noDirection), at(7, 55), 10)
        assertEquals(1, merged.count { it.isRealtime })
    }

    @Test
    fun returnLegHasItsOwnDepartures() {
        val rows = timetable.scheduled("d", Leg.Return, at(7, 0), at(13, 0), zone)
        assertEquals(listOf(at(8, 2)), rows.map { it.time })
        assertEquals("6060", rows.single().stopId)
    }
}
