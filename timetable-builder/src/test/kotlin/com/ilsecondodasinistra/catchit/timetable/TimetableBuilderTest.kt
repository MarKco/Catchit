package com.ilsecondodasinistra.catchit.timetable

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TimetableBuilderTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun zip(files: Map<String, String>): File {
        val file = tmp.newFile("feed.zip")
        ZipOutputStream(file.outputStream()).use { out ->
            for ((name, text) in files) {
                out.putNextEntry(ZipEntry(name))
                out.write(text.trimIndent().toByteArray())
                out.closeEntry()
            }
        }
        return file
    }

    private val destination = Destination("d", "Dest", Place("Alfa", ids = setOf("A")), Place("Beta", namePrefixes = listOf("Beta ")))

    private val feed = mapOf(
        "stops.txt" to """
            stop_id,stop_name,stop_lat,stop_lon
            A,Alfa,45.1,12.1
            M,Mezzo,45.2,12.2
            B1,Beta uno,45.3,12.3
            B2,Beta due,45.4,12.4
            C,Gamma,45.5,12.5
        """,
        "routes.txt" to """
            route_id,route_short_name,route_long_name,route_type,route_color,route_text_color
            R1,1,Uno,3,112233,FFFFFF
            R2,2,Due,3,,
            R3,3,Tre,3,445566,FFFFFF
            R4,4,Quattro,0,778899,000000
        """,
        "trips.txt" to """
            route_id,trip_id,service_id,trip_headsign,direction_id
            R1,t1,wk,Beta,0
            R1,t1b,wk,Beta,0
            R1,t1c,wk,Beta,0
            R2,t2,wk,Alfa,1
            R3,t3,wk,Gamma,0
            R4,t4,feast,Loop,0
        """,
        // t1: A -> B1. t2: B2 -> A. t3: A -> C (never reaches Beta). t4 loops A -> ... -> A and ends at B1 flagged no drop-off.
        "stop_times.txt" to """
            trip_id,stop_id,stop_sequence,arrival_time,departure_time,pickup_type,drop_off_type
            t1,A,1,08:00:00,08:00:00,,1
            t1,M,2,08:05:00,08:05:00,,
            t1,B1,3,08:10:00,08:10:00,1,
            t1b,A,1,09:00:00,09:00:00,,1
            t1b,M,2,09:06:00,09:06:00,,
            t1b,B1,3,09:14:00,09:14:00,1,
            t1c,A,1,10:00:00,10:00:00,,1
            t1c,M,2,10:07:00,10:07:00,,
            t1c,B1,3,10:12:00,10:12:00,1,
            t2,B2,1,09:00:00,09:00:00,,1
            t2,A,2,09:10:00,09:10:00,1,
            t3,A,1,10:00:00,10:00:00,,
            t3,C,2,10:05:00,10:05:00,,
            t4,A,1,25:00:00,25:00:00,,1
            t4,C,2,25:05:00,25:05:00,,
            t4,B1,3,25:10:00,25:10:00,,1
        """,
        "calendar.txt" to """
            service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
            wk,1,1,1,1,1,0,0,20261022,20271021
        """,
        "calendar_dates.txt" to """
            service_id,date,exception_type
            feast,20261207,1
            wk,20261207,2
        """,
        "feed_info.txt" to """
            feed_id,feed_start_date,feed_end_date
            0,20261022,20271021
        """,
    )

    private fun build(): JsonObject {
        val text = TimetableBuilder.build(zip(feed), listOf(destination))
        return Json.parseToJsonElement(text).jsonObject
    }

    private fun JsonArray.patterns() = map { it.jsonObject }

    @Test
    fun keepsOnlyDirectConnectionsInBothLegs() {
        val patterns = build().getValue("patterns").jsonArray.patterns()
        val byLeg = patterns.groupBy({ it.getValue("leg").jsonPrimitive.content }, { it.getValue("route").jsonPrimitive.content })
        assertEquals(setOf("R1", "R4"), byLeg.getValue("andata").toSet()) // R3 never reaches Beta
        assertEquals(listOf("R2"), byLeg.getValue("ritorno"))
    }

    @Test
    fun departureIsTakenAtTheStopWhereTheLegStarts() {
        val patterns = build().getValue("patterns").jsonArray.patterns()
        val outbound = patterns.single { it.getValue("route").jsonPrimitive.content == "R1" }
        assertEquals("A", outbound.getValue("stop").jsonPrimitive.content)
        assertEquals(listOf(8 * 3600, 9 * 3600, 10 * 3600), outbound.getValue("times").jsonArray.map { it.jsonPrimitive.content.toInt() })
        val back = patterns.single { it.getValue("route").jsonPrimitive.content == "R2" }
        assertEquals("B2", back.getValue("stop").jsonPrimitive.content)
    }

    @Test
    fun terminusAlwaysAllowsGettingOffEvenWhenTheFeedSaysOtherwise() {
        // t4 reaches Beta only at its last stop, flagged "no drop-off" (the night loops of the real feed).
        val patterns = build().getValue("patterns").jsonArray.patterns()
        val loop = patterns.single { it.getValue("route").jsonPrimitive.content == "R4" }
        assertEquals(listOf(25 * 3600), loop.getValue("times").jsonArray.map { it.jsonPrimitive.content.toInt() })
    }

    @Test
    fun servicesOnlyInCalendarDatesAreKept() {
        val services = build().getValue("services").jsonArray.map { it.jsonObject }.associateBy { it.getValue("id").jsonPrimitive.content }
        val feast = services.getValue("feast")
        assertTrue(feast.getValue("days").jsonArray.none { it.jsonPrimitive.content == "true" })
        assertEquals(listOf("20261207"), feast.getValue("added").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("20261207"), services.getValue("wk").getValue("removed").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun routeColorsFallBackWhenMissing() {
        val routes = build().getValue("routes").jsonArray.map { it.jsonObject }.associateBy { it.getValue("id").jsonPrimitive.content }
        assertEquals("5B5B5B", routes.getValue("R2").getValue("color").jsonPrimitive.content)
        assertEquals("FFFFFF", routes.getValue("R2").getValue("textColor").jsonPrimitive.content)
    }

    @Test
    fun failsWhenAPlaceMatchesNoStop() {
        val missing = Destination("x", "X", Place("Nowhere", ids = setOf("ZZ")), destination.target)
        try {
            TimetableBuilder.build(zip(feed), listOf(missing))
            throw AssertionError("expected a failure")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("origin"))
        }
    }

    @Test
    fun eachDepartureKnowsWhereAndAfterHowLongYouGetOff() {
        val outbound = build().getValue("patterns").jsonArray.patterns().single { it.getValue("route").jsonPrimitive.content == "R1" }
        assertEquals("B1", outbound.getValue("alight").jsonPrimitive.content)
        // 08:00→08:10, 09:00→09:14, 10:00→10:12
        assertEquals(listOf(600, 840, 720), outbound.getValue("durations").jsonArray.map { it.jsonPrimitive.content.toInt() })
    }

    @Test
    fun theStopsBetweenCarryTheMedianOffset() {
        val root = build()
        val outbound = root.getValue("patterns").jsonArray.patterns().single { it.getValue("route").jsonPrimitive.content == "R1" }
        val shape = root.getValue("shapes").jsonArray.map { it.jsonObject }.single { it.getValue("id").jsonPrimitive.content == outbound.getValue("shape").jsonPrimitive.content }
        assertEquals(listOf("A", "M", "B1"), shape.getValue("stops").jsonArray.map { it.jsonPrimitive.content })
        // Offsets of M: 300, 360, 420 -> 360; of B1: 600, 840, 720 -> 720.
        assertEquals(listOf(0, 360, 720), shape.getValue("offsets").jsonArray.map { it.jsonPrimitive.content.toInt() })
    }

    @Test
    fun stopsCarryNamesAndCoordinatesIncludingTheIntermediateOnes() {
        val stops = build().getValue("stops").jsonArray.map { it.jsonObject }.associateBy { it.getValue("id").jsonPrimitive.content }
        val middle = stops.getValue("M")
        assertEquals("Mezzo", middle.getValue("name").jsonPrimitive.content)
        assertEquals(45.2, middle.getValue("lat").jsonPrimitive.content.toDouble(), 1e-9)
        assertEquals(12.2, middle.getValue("lon").jsonPrimitive.content.toDouble(), 1e-9)
    }
}
