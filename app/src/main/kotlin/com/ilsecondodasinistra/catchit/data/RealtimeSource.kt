package com.ilsecondodasinistra.catchit.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Where the raw GTFS-RT `tripUpdates` bytes come from; a seam for tests. */
interface RealtimeSource {
    suspend fun fetchTripUpdates(): ByteArray

    /** Where the vehicles are: optional extra, the board works without it. */
    suspend fun fetchVehiclePositions(): ByteArray
}

/** Bus and tram feed of AVM/ACTV (the water-bus one lives under `/nav/`). */
@Singleton
class AvmRealtimeSource @Inject constructor() : RealtimeSource {
    override suspend fun fetchTripUpdates(): ByteArray = fetch(TRIP_UPDATES_URL)

    override suspend fun fetchVehiclePositions(): ByteArray = fetch(VEHICLE_POSITIONS_URL)

    private suspend fun fetch(url: String): ByteArray = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "AVM answered ${connection.responseCode}" }
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TRIP_UPDATES_URL = "https://tpl.actv.it/aut/GTFSRT/tripUpdates"
        const val VEHICLE_POSITIONS_URL = "https://tpl.actv.it/aut/GTFSRT/vehiclePositions"
        const val TIMEOUT_MS = 8_000
    }
}
