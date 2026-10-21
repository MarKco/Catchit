package com.ilsecondodasinistra.catchit.timetable

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {
    @Test
    fun splitsQuotedCommas() {
        assertEquals(listOf("a", "b,c", "d\"e", ""), splitCsvLine("a,\"b,c\",\"d\"\"e\","))
    }

    @Test
    fun parsesAfterMidnightTimes() {
        assertEquals(5 * 3600 + 30 * 60, parseGtfsTime("05:30:00"))
        assertEquals(25 * 3600 + 5 * 60 + 9, parseGtfsTime("25:05:09"))
        assertEquals(null, parseGtfsTime(""))
    }
}
