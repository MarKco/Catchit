package com.ilsecondodasinistra.catchit.data

import com.ilsecondodasinistra.catchit.data.ProtoReader.Companion.WIRE_FIXED32
import com.ilsecondodasinistra.catchit.data.ProtoReader.Companion.WIRE_LENGTH
import com.ilsecondodasinistra.catchit.data.ProtoReader.Companion.WIRE_VARINT

/**
 * Where a vehicle was, according to its GPS, [epochSecond] being the time of the fix. [bearing] is null when the feed
 * says nothing useful (it sends 0.0 for a vehicle that is parked). [stopId] is the last stop the vehicle served.
 */
data class VehicleFix(
    val latitude: Double,
    val longitude: Double,
    val bearing: Float?,
    val epochSecond: Long,
    val stopId: String? = null,
)

/** A vehicle of the feed: [label] is what the fleet calls it ("TRAM_07"), [tripId] and [routeId] are empty when it isn't on a trip. */
data class VehicleSnapshot(val label: String, val tripId: String, val routeId: String, val fix: VehicleFix) {
    val isTram: Boolean get() = label.startsWith("TRAM")
}

/** Reads the `VehiclePosition` entities of the GTFS-Realtime feed (field numbers from gtfs-realtime.proto). */
object VehiclePositionsParser {
    /** The fix of each of [tripIds] that has one. */
    fun parse(bytes: ByteArray, tripIds: Set<String>): Map<String, VehicleFix> {
        if (tripIds.isEmpty()) return emptyMap()
        return parseAll(bytes).filter { it.tripId.isNotEmpty() && it.tripId in tripIds }.associate { it.tripId to it.fix }
    }

    /** Every vehicle that has a position, on a trip or not. */
    fun parseAll(bytes: ByteArray): List<VehicleSnapshot> {
        val out = ArrayList<VehicleSnapshot>()
        val feed = ProtoReader(bytes)
        feed.forEachField { field, wire ->
            if (field == 2 && wire == WIRE_LENGTH) {
                val entity = feed.readLengthDelimited()
                entity.forEachField { f, w ->
                    if (f == 4 && w == WIRE_LENGTH) readVehicle(entity.readLengthDelimited())?.let(out::add) else entity.skip(w)
                }
            } else feed.skip(wire)
        }
        return out
    }

    private fun readVehicle(vp: ProtoReader): VehicleSnapshot? {
        var tripId = ""
        var routeId = ""
        var label = ""
        var stopId = ""
        var latitude: Double? = null
        var longitude: Double? = null
        var bearing: Float? = null
        var time = 0L
        vp.forEachField { field, wire ->
            when {
                field == 1 && wire == WIRE_LENGTH -> {
                    val trip = vp.readLengthDelimited()
                    trip.forEachField { f, w ->
                        when {
                            f == 1 && w == WIRE_LENGTH -> tripId = trip.readString()
                            f == 5 && w == WIRE_LENGTH -> routeId = trip.readString()
                            else -> trip.skip(w)
                        }
                    }
                }
                field == 2 && wire == WIRE_LENGTH -> {
                    val position = vp.readLengthDelimited()
                    position.forEachField { f, w ->
                        when {
                            f == 1 && w == WIRE_FIXED32 -> latitude = position.readFloat().toDouble()
                            f == 2 && w == WIRE_FIXED32 -> longitude = position.readFloat().toDouble()
                            f == 3 && w == WIRE_FIXED32 -> bearing = position.readFloat()
                            else -> position.skip(w)
                        }
                    }
                }
                field == 5 && wire == WIRE_VARINT -> time = vp.readVarint()
                field == 7 && wire == WIRE_LENGTH -> stopId = vp.readString()
                field == 8 && wire == WIRE_LENGTH -> {
                    val descriptor = vp.readLengthDelimited()
                    descriptor.forEachField { f, w -> if (f == 2 && w == WIRE_LENGTH) label = descriptor.readString() else descriptor.skip(w) }
                }
                else -> vp.skip(wire)
            }
        }
        val lat = latitude ?: return null
        val lon = longitude ?: return null
        return VehicleSnapshot(label, tripId, routeId, VehicleFix(lat, lon, bearing?.takeIf { it != 0f }, time, stopId.ifEmpty { null }))
    }
}
