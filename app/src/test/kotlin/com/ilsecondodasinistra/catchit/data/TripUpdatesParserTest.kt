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
