package com.ilsecondodasinistra.catchit.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GeoCatalogTest {
    private fun stop(id: String, lat: Double) = StopInfo(id, "Stop $id", lat, 12.0)

    private val data = GeoData(
        config = "c",
        feedStart = "20261022",
        stops = listOf("A", "B", "C", "D").mapIndexed { i, id -> stop(id, 45.0 + i / 100.0) },
        runs = listOf(
            RunInfo(0, "R1", 0, listOf("A", "B", "C", "D"), path = 0),
            RunInfo(1, "R1", 0, listOf("A", "B"), path = -1),
            RunInfo(2, "R2", 0, listOf("D", "C", "B", "A")),
        ),
        paths = listOf(listOf(4_500_000, 1_200_000, 1_000, -500)),
    )
    private val catalog = GeoCatalog(listOf(data))

    @Test
    fun pathIsUnpackedFromDeltas() {
        val path = GeoCatalog.decodePath(listOf(4_500_000, 1_200_000, 1_000, -500))
        assertEquals(listOf(LatLon(45.0, 12.0), LatLon(45.01, 11.995)), path)
    }

    @Test
    fun aRunMustBoardBeforeGettingOff() {
        assertNotNull(catalog.findRun("R1", "A", "D", emptySet()))
        assertNull(catalog.findRun("R1", "D", "A", emptySet()))
        assertNull(catalog.findRun("R3", "A", "B", emptySet()))
    }

    @Test
    fun theRunThatVisitsMostOfTheLiveStopsWins() {
        // Both R1 runs fit A→B; the live feed has the trip going to C and D: it is the long one.
        val run = catalog.findRun("R1", "A", "B", setOf("C", "D"))
        assertEquals(listOf("A", "B", "C", "D"), run?.stops?.map { it.id })
        assertEquals(2, run?.path?.size)
    }

    @Test
    fun withNothingToGoByTheShorterRunWins() {
        val run = catalog.findRun("R1", "A", "B", emptySet())
        assertEquals(listOf("A", "B"), run?.stops?.map { it.id })
        assertEquals(emptyList<LatLon>(), run?.path)
    }

    @Test
    fun newestVersionAnswersFirst() {
        val newer = data.copy(feedStart = "20270101", runs = listOf(RunInfo(0, "R1", 0, listOf("A", "C"))))
        val run = GeoCatalog(listOf(data, newer)).findRun("R1", "A", "C", emptySet())
        assertEquals(listOf("A", "C"), run?.stops?.map { it.id })
    }
}
