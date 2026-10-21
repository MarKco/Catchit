package com.ilsecondodasinistra.catchit.data

import com.ilsecondodasinistra.catchit.data.ProtoReader.Companion.WIRE_LENGTH
import com.ilsecondodasinistra.catchit.data.ProtoReader.Companion.WIRE_VARINT

/** One predicted passage of one trip at one stop. */
data class RealtimeUpdate(
    val tripId: String,
    val routeId: String,
    val directionId: Int?,
    val stopId: String,
    /** Predicted departure, epoch seconds. Null only for cancelled trips that carry no time. */
    val epochSecond: Long?,
    /** Seconds late (negative = early); subtracting it from [epochSecond] gives the timetable time. */
    val delaySeconds: Int?,
    val tripCanceled: Boolean,
)

/** Reads GTFS-Realtime `TripUpdate` entities (field numbers from gtfs-realtime.proto). */
object TripUpdatesParser {
    private const val TRIP_CANCELED = 3L
    private const val STOP_SKIPPED = 1L
    private const val STOP_NO_DATA = 2L

    /** Keeps only updates for [stopIds]: the feed lists every remaining stop of every running trip. */
    fun parse(bytes: ByteArray, stopIds: Set<String>): List<RealtimeUpdate> {
        val out = ArrayList<RealtimeUpdate>()
        val feed = ProtoReader(bytes)
        feed.forEachField { field, wire ->
            if (field == 2 && wire == WIRE_LENGTH) readEntity(feed.readLengthDelimited(), stopIds, out) else feed.skip(wire)
        }
        return out
    }

    private fun readEntity(entity: ProtoReader, stopIds: Set<String>, out: MutableList<RealtimeUpdate>) {
        entity.forEachField { field, wire ->
            if (field == 3 && wire == WIRE_LENGTH) readTripUpdate(entity.readLengthDelimited(), stopIds, out) else entity.skip(wire)
        }
    }

    private class Trip(
        var id: String = "",
        var routeId: String = "",
        var direction: Int? = null,
        var canceled: Boolean = false,
    )

    private class StopUpdate(
        var stopId: String = "",
        var time: Long? = null,
        var delay: Int? = null,
        var skipped: Boolean = false,
        var noData: Boolean = false,
    )

    private fun readTripUpdate(tu: ProtoReader, stopIds: Set<String>, out: MutableList<RealtimeUpdate>) {
        val trip = Trip()
        val stops = ArrayList<StopUpdate>(2)
        tu.forEachField { field, wire ->
            when {
                field == 1 && wire == WIRE_LENGTH -> readTrip(tu.readLengthDelimited(), trip)
                field == 2 && wire == WIRE_LENGTH -> readStopUpdate(tu.readLengthDelimited())
                    .takeIf { it.stopId in stopIds }?.let(stops::add)
                else -> tu.skip(wire)
            }
        }
        for (stop in stops) {
            if (stop.skipped || stop.noData) continue
            if (stop.time == null && !trip.canceled) continue
            out += RealtimeUpdate(trip.id, trip.routeId, trip.direction, stop.stopId, stop.time, stop.delay, trip.canceled)
        }
    }

    private fun readTrip(r: ProtoReader, trip: Trip) {
        r.forEachField { field, wire ->
            when {
                field == 1 && wire == WIRE_LENGTH -> trip.id = r.readString()
                field == 4 && wire == WIRE_VARINT -> trip.canceled = r.readVarint() == TRIP_CANCELED
                field == 5 && wire == WIRE_LENGTH -> trip.routeId = r.readString()
                field == 6 && wire == WIRE_VARINT -> trip.direction = r.readVarint().toInt()
                else -> r.skip(wire)
            }
        }
    }

    private fun readStopUpdate(r: ProtoReader): StopUpdate {
        val stop = StopUpdate()
        var arrivalTime: Long? = null
        var arrivalDelay: Int? = null
        r.forEachField { field, wire ->
            when {
                field == 2 && wire == WIRE_LENGTH -> readEvent(r.readLengthDelimited()).let { (t, d) -> arrivalTime = t; arrivalDelay = d }
                field == 3 && wire == WIRE_LENGTH -> readEvent(r.readLengthDelimited()).let { (t, d) -> stop.time = t; stop.delay = d }
                field == 4 && wire == WIRE_LENGTH -> stop.stopId = r.readString()
                field == 5 && wire == WIRE_VARINT -> r.readVarint().let { stop.skipped = it == STOP_SKIPPED; stop.noData = it == STOP_NO_DATA }
                else -> r.skip(wire)
            }
        }
        // Departure wins; terminus-style updates only carry an arrival.
        if (stop.time == null) {
            stop.time = arrivalTime
            stop.delay = arrivalDelay
        }
        return stop
    }

    /** StopTimeEvent: delay (1, int32, sign-extended varint) and absolute time (2, int64). */
    private fun readEvent(r: ProtoReader): Pair<Long?, Int?> {
        var time: Long? = null
        var delay: Int? = null
        r.forEachField { field, wire ->
            when {
                field == 1 && wire == WIRE_VARINT -> delay = r.readVarint().toInt()
                field == 2 && wire == WIRE_VARINT -> time = r.readVarint().takeIf { it > 0 }
                else -> r.skip(wire)
            }
        }
        return time to delay
    }
}
