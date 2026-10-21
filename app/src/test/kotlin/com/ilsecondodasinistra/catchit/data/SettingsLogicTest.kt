package com.ilsecondodasinistra.catchit.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarPreferencesTest {
    private val known = listOf("venezia", "centro", "stazione", "aeroporto")

    @Test
    fun withNothingSavedEverythingIsVisibleInTimetableOrder() {
        assertEquals(known.map { BarEntry(it, true) }, BarPreferences.resolve(known, emptyList()))
    }

    @Test
    fun savedOrderAndVisibilityWin() {
        val saved = listOf(BarEntry("stazione", true), BarEntry("centro", false), BarEntry("venezia", true), BarEntry("aeroporto", true))
        assertEquals(saved, BarPreferences.resolve(known, saved))
    }

    @Test
    fun newDestinationsGoLastAndVisibleAndVanishedOnesAreDropped() {
        val saved = listOf(BarEntry("centro", true), BarEntry("gone", true), BarEntry("venezia", false))
        assertEquals(
            listOf(BarEntry("centro", true), BarEntry("venezia", false), BarEntry("stazione", true), BarEntry("aeroporto", true)),
            BarPreferences.resolve(known, saved),
        )
    }

    @Test
    fun theBarNeverEndsUpEmpty() {
        val saved = known.map { BarEntry(it, false) }
        val resolved = BarPreferences.resolve(known, saved)
        assertEquals(1, resolved.count { it.visible })
        assertEquals("venezia", resolved.first { it.visible }.id)
    }

    @Test
    fun parsesWhatItSaves() {
        val text = "centro:1,venezia:0"
        assertEquals(listOf(BarEntry("centro", true), BarEntry("venezia", false)), BarPreferences.parse(text))
        assertEquals(emptyList<BarEntry>(), BarPreferences.parse(null))
        assertEquals(listOf(BarEntry("a", true)), BarPreferences.parse("a:1,garbage,b:1:2"))
    }
}

class UpdateLogFormatTest {
    private fun entry(at: Long, kind: LogKind, level: LogLevel, message: String, detail: String? = null) =
        LogEntry(at, kind, level, message, detail)

    @Test
    fun formatsHeaderThenEntriesOldestFirstWithTraces() {
        val text = UpdateLog.format(
            header = listOf("Catchit 1.0", "Android 16"),
            entries = listOf(
                // 2026-10-22 00:07:12 in Venice (UTC+2)
                entry(1_792_620_432_000, LogKind.Timetable, LogLevel.Info, "Controllo orari"),
                entry(1_792_620_433_000, LogKind.Live, LogLevel.Error, "Tempo reale non disponibile", "java.io.IOException: boom\n    at x.Y.z(Y.kt:1)"),
            ),
        )
        val lines = text.lines()
        assertEquals("Catchit 1.0", lines[0])
        assertEquals("Android 16", lines[1])
        assertEquals("", lines[2])
        assertEquals("2026-10-22 00:07:12 [orari] INFO Controllo orari", lines[3])
        assertEquals("2026-10-22 00:07:13 [live] ERROR Tempo reale non disponibile", lines[4])
        assertEquals("    java.io.IOException: boom", lines[5])
        assertEquals("        at x.Y.z(Y.kt:1)", lines[6])
    }

    @Test
    fun emptyLogSaysSo() {
        assertTrue(UpdateLog.format(listOf("h"), emptyList()).endsWith("(registro vuoto)"))
    }

    @Test
    fun traceIsCondensed() {
        val trace = UpdateLog.condensedTrace(IllegalStateException("x", RuntimeException("y")))
        assertTrue(trace.lines().size <= 12)
        assertTrue(trace.startsWith("java.lang.IllegalStateException: x"))
    }
}
