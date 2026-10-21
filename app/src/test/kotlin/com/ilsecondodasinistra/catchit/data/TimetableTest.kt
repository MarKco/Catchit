package com.ilsecondodasinistra.catchit.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TimetableTest {
    private val zone = ZoneId.of("Europe/Rome")

    @Test
    fun serviceOnlyListedInCalendarDatesRunsOnItsExtraDaysOnly() {
        // Holiday services have no weekly pattern: the importer emits them with no days and empty dates.
        val timetable = Timetable(
            TimetableData(
                feedStart = "20261022",
                feedEnd = "20271021",
                routes = emptyList(),
                stops = emptyList(),
                destinations = emptyList(),
                services = listOf(ServiceInfo("feast", List(7) { false }, start = "", end = "", added = listOf("20261207"))),
                patterns = listOf(Pattern("d", "andata", "1", "R", 0, "X", "feast", listOf(8 * 3600))),
            ),
        )
        fun runs(date: LocalDate): Boolean {
            val from = date.atStartOfDay(zone).toInstant()
            return timetable.scheduled("d", Leg.Outbound, from, from.plusSeconds(86_400 - 1), zone).isNotEmpty()
        }
        assertTrue(runs(LocalDate.of(2026, 12, 7)))
        assertEquals(false, runs(LocalDate.of(2026, 12, 8)))
        assertEquals(false, runs(LocalDate.of(2026, 10, 21))) // before the feed: no weekly pattern to borrow
    }
}

class TimetableVersionsTest {
    private val zone = ZoneId.of("Europe/Rome")

    private fun version(start: String, at: Int) = TimetableData(
        feedStart = start,
        feedEnd = "20271231",
        routes = listOf(RouteInfo("R", "R", "", "112233", "FFFFFF", 3)),
        stops = emptyList(),
        destinations = listOf(DestinationInfo("d", "D", "A", "B")),
        services = listOf(ServiceInfo("s", List(7) { true }, start, "20271231")),
        patterns = listOf(Pattern("d", "andata", "1", "R", 0, "X", "s", listOf(at))),
    )

    private val timetable = Timetable(listOf(version("20261022", 9 * 3600), version("20260907", 8 * 3600)))

    private fun firstOf(day: LocalDate): Int {
        val from = day.atStartOfDay(zone).toInstant()
        return timetable.scheduled("d", Leg.Outbound, from, from.plusSeconds(86_399), zone).single().time.atZone(zone).hour
    }

    @Test
    fun eachDayUsesTheVersionInForceThatDay() {
        assertEquals(8, firstOf(LocalDate.of(2026, 10, 21))) // new one published, not started yet
        assertEquals(9, firstOf(LocalDate.of(2026, 10, 22)))
        assertEquals(9, firstOf(LocalDate.of(2026, 12, 1)))
    }

    @Test
    fun aWindowCrossingTheStartDateSwitchesVersionAtMidnight() {
        val from = LocalDate.of(2026, 10, 21).atTime(7, 0).atZone(zone).toInstant()
        val rows = timetable.scheduled("d", Leg.Outbound, from, from.plusSeconds(36 * 3600), zone)
        assertEquals(listOf(8, 9), rows.map { it.time.atZone(zone).hour })
    }

    @Test
    fun beforeEveryVersionTheOldestIsIndicative() {
        assertEquals(8, firstOf(LocalDate.of(2026, 9, 1)))
        assertEquals(LocalDate.of(2026, 9, 7), timetable.validFrom)
    }

    @Test
    fun obsoleteKeepsNewestAndTheOneInForce() {
        val d = { m: Int, day: Int -> LocalDate.of(2026, m, day) }
        val today = d(10, 21)
        assertEquals(setOf(d(7, 1)), SnapshotPolicy.obsolete(listOf(d(7, 1), d(9, 7), d(10, 22)), today))
        assertEquals(emptySet<LocalDate>(), SnapshotPolicy.obsolete(listOf(d(9, 7)), today))
        assertEquals(emptySet<LocalDate>(), SnapshotPolicy.obsolete(listOf(d(10, 22), d(11, 5)), today)) // none started: keep both
        assertEquals(setOf(d(9, 7)), SnapshotPolicy.obsolete(listOf(d(9, 7), d(10, 1)), today)) // newest is already in force
    }
}
