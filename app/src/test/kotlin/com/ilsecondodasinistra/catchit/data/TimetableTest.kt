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
