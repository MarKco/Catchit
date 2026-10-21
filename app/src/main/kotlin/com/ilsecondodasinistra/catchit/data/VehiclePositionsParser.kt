package com.ilsecondodasinistra.catchit.data

import com.ilsecondodasinistra.catchit.data.ProtoReader.Companion.WIRE_FIXED32
import com.ilsecondodasinistra.catchit.data.ProtoReader.Companion.WIRE_LENGTH
import com.ilsecondodasinistra.catchit.data.ProtoReader.Companion.WIRE_VARINT

/** Where a vehicle was, according to its GPS, [epochSecond] being the time of the fix. */
data class VehicleFix(val latitude: Double, val longitude: Double, val bearing: Float?, val epochSecond: Long)

/** Reads the `VehiclePosition` entities of the GTFS-Realtime feed (field numbers from gtfs-realtime.proto). */
object VehiclePositionsParser {
    /** The fix of each of [tripIds] that has one. Vehicles with no trip (all the trams) can't be told apart here. */
    fun parse(bytes: ByteArray, tripIds: Set<String>): Map<String, VehicleFix> {
        if (tripIds.isEmpty()) return emptyMap()
        val out = HashMap<String, VehicleFix>()
        val feed = ProtoReader(bytes)
        feed.forEachField { field, wire ->
            if (field == 2 && wire == WIRE_LENGTH) {
                val entity = feed.readLengthDelimited()
                entity.forEachField { f, w ->
                    if (f == 4 && w == WIRE_LENGTH) readVehicle(entity.readLengthDelimited(), tripIds, out) else entity.skip(w)
                }
            } else feed.skip(wire)
        }
        return out
    }

    private fun readVehicle(vp: ProtoReader, tripIds: Set<String>, out: MutableMap<String, VehicleFix>) {
        var tripId = ""
        var latitude: Double? = null
        var longitude: Double? = null
        var bearing: Float? = null
        var time = 0L
        vp.forEachField { field, wire ->
            when {
                field == 1 && wire == WIRE_LENGTH -> {
                    val trip = vp.readLengthDelimited()
                    trip.forEachField { f, w -> if (f == 1 && w == WIRE_LENGTH) tripId = trip.readString() else trip.skip(w) }
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
                else -> vp.skip(wire)
            }
        }
        val lat = latitude
        val lon = longitude
        // Vehicles with no trip (all the trams) have an empty id: never a match.
        if (tripId.isNotEmpty() && tripId in tripIds && lat != null && lon != null) out[tripId] = VehicleFix(lat, lon, bearing, time)
    }
}
