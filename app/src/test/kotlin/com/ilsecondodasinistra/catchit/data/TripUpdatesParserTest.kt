package com.ilsecondodasinistra.catchit.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripUpdatesParserTest {
    // Snapshot of https://tpl.actv.it/aut/GTFSRT/tripUpdates taken on 2026-10-21 20:23 Europe/Rome.
    private val sample = checkNotNull(javaClass.getResourceAsStream("/trip_updates_sample.pb")).use { it.readBytes() }

    @Test
    fun readsDepartureTimeDelayAndRoute() {
        val updates = TripUpdatesParser.parse(sample, setOf("6059", "6060"))
        val update = updates.firstOrNull { it.tripId == "AUT_ACTV_792_29202" && it.stopId == "6059" }
        assertNotNull(update)
        assertEquals("31H_UM", update!!.routeId)
        assertEquals(0, update.directionId)
        assertEquals(1792609967L, update.epochSecond)
        assertEquals(107, update.delaySeconds)
    }

    @Test
    fun keepsOnlyRequestedStops() {
        val updates = TripUpdatesParser.parse(sample, setOf("6060"))
        assertTrue(updates.isNotEmpty())
        assertTrue(updates.all { it.stopId == "6060" })
    }

    @Test
    fun readsNegativeDelays() {
        // Stop 1009 of the same trip is 26 s early (sign-extended 10-byte varint).
        val update = TripUpdatesParser.parse(sample, setOf("1009")).first { it.tripId == "AUT_ACTV_792_29202" }
        assertEquals(-26, update.delaySeconds)
    }

    @Test(expected = ProtoException::class)
    fun rejectsTruncatedFeed() {
        TripUpdatesParser.parse(sample.copyOf(sample.size / 2 + 1), setOf("6059"))
    }
}

class TerminusUpdatesTest {
    private fun varint(value: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var v = value
        while (v >= 0x80) { out.write(((v and 0x7f) or 0x80).toInt()); v = v ushr 7 }
        out.write(v.toInt())
        return out.toByteArray()
    }

    private fun message(field: Int, payload: ByteArray) = varint((field shl 3 or 2).toLong()) + varint(payload.size.toLong()) + payload
    private fun text(field: Int, value: String) = message(field, value.toByteArray())
    private fun number(field: Int, value: Long) = varint((field shl 3).toLong()) + varint(value)

    private fun stopUpdate(stop: String, time: Long) = message(2, message(3, number(2, time)) + text(4, stop))

    /** A feed with one trip of route "L" whose stop updates are [stops] (id to time), in order. */
    private fun feed(vararg stops: Pair<String, Long>): ByteArray {
        val trip = message(1, text(1, "trip") + text(5, "L") + number(6, 0))
        return message(2, message(3, trip + stops.fold(ByteArray(0)) { acc, (id, time) -> acc + stopUpdate(id, time) }))
    }

    @Test
    fun theLastStopOfATripIsAnArrivalNotADeparture() {
        val updates = TripUpdatesParser.parse(feed("A" to 100L, "B" to 200L, "C" to 300L), setOf("A", "C"))
        assertEquals(listOf("A"), updates.map { it.stopId })
    }

    @Test
    fun aLoopStartingAndEndingAtTheSamePoleKeepsOnlyItsDeparture() {
        val updates = TripUpdatesParser.parse(feed("A" to 100L, "B" to 200L, "A" to 300L), setOf("A"))
        assertEquals(listOf(100L), updates.map { it.epochSecond })
    }

    @Test
    fun aVehicleAboutToArriveAtItsTerminusHasNothingToBoard() {
        assertEquals(emptyList<RealtimeUpdate>(), TripUpdatesParser.parse(feed("A" to 100L), setOf("A")))
    }
}

class TripStopTimesTest {
    private val sample = checkNotNull(javaClass.getResourceAsStream("/trip_updates_sample.pb")).use { it.readBytes() }

    @Test
    fun givesThePredictedTimeAtEveryStopOfTheTrip() {
        val times = TripUpdatesParser.tripStopTimes(sample, setOf("AUT_ACTV_792_29202")).getValue("AUT_ACTV_792_29202")
        assertEquals(1792609967L, times["6059"]) // the stop we depart from
        assertEquals(1792609907L, times["6057"]) // the one before
        assertTrue(times.size > 20)
    }

    @Test
    fun ignoresTripsThatWereNotAsked() {
        assertEquals(emptyMap<String, Map<String, Long>>(), TripUpdatesParser.tripStopTimes(sample, emptySet()))
        assertEquals(setOf("AUT_ACTV_792_29202"), TripUpdatesParser.tripStopTimes(sample, setOf("AUT_ACTV_792_29202", "nope")).keys)
    }
}

class VehiclePositionsParserTest {
    // Snapshot of https://tpl.actv.it/aut/GTFSRT/vehiclePositions taken on 2026-10-21 21:40 Europe/Rome.
    private val sample = checkNotNull(javaClass.getResourceAsStream("/vehicle_positions_sample.pb")).use { it.readBytes() }

    @Test
    fun readsTheFixOfATripWithAVehicle() {
        val fix = VehiclePositionsParser.parse(sample, setOf("AUT_ACTV_792_33397")).getValue("AUT_ACTV_792_33397")
        assertTrue(fix.latitude in 45.0..46.0)
        assertTrue(fix.longitude in 12.0..13.0)
        assertTrue(fix.epochSecond > 1_700_000_000L)
    }

    @Test
    fun vehiclesWithoutATripAreNotReturned() {
        // 74 of the 127 vehicles of the snapshot (all the trams) carry no trip: nothing to tie them to.
        assertEquals(emptyMap<String, VehicleFix>(), VehiclePositionsParser.parse(sample, setOf("")))
        assertEquals(emptyMap<String, VehicleFix>(), VehiclePositionsParser.parse(sample, emptySet()))
    }

    @Test
    fun readsFloatsFromFixed32() {
        // 45.5f = 0x42360000, little endian on the wire.
        val reader = ProtoReader(byteArrayOf(0x00, 0x00, 0x36, 0x42))
        assertEquals(45.5f, reader.readFloat(), 0f)
    }
}
