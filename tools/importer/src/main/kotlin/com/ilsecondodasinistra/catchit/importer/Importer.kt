package com.ilsecondodasinistra.catchit.importer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private data class TripInfo(val route: String, val direction: Int, val headsign: String, val service: String)

private data class PatternKey(
    val stop: String,
    val route: String,
    val direction: Int,
    val headsign: String,
    val service: String,
)

/** Seconds after midnight of the service day; GTFS allows 24:xx:xx and beyond for after-midnight trips. */
internal fun parseGtfsTime(text: String): Int? {
    val parts = text.trim().split(':')
    if (parts.size != 3) return null
    val (h, m, s) = parts.map { it.toIntOrNull() ?: return null }
    return h * 3600 + m * 60 + s
}

/**
 * Reduces the full feed to the configured lines and stops.
 *
 * Output (see `data/Timetable.kt` in the app): routes, stops, services (weekly calendar plus exceptions) and
 * *patterns* = the departure times of one (stop, route, direction, headsign, service), sorted.
 */
internal fun buildTimetable(feed: Feed, lines: Set<String>, stopLabels: Map<String, String>): JsonObject {
    val wantedLines = lines.map { it.uppercase() }.toSet()

    val routes = feed.read("routes.txt") { rows ->
        rows.filter { it["route_short_name"].uppercase() in wantedLines }.associateBy { it["route_id"] }
    }
    require(routes.isNotEmpty()) { "None of $lines found in routes.txt" }

    val trips = feed.read("trips.txt") { rows ->
        rows.filter { it["route_id"] in routes }.associate {
            it["trip_id"] to TripInfo(it["route_id"], it["direction_id"].toIntOrNull() ?: 0, it["trip_headsign"], it["service_id"])
        }
    }
    println("${routes.size} routes, ${trips.size} trips")

    val times = HashMap<PatternKey, MutableSet<Int>>()
    feed.read("stop_times.txt") { rows ->
        for (row in rows) {
            val stop = row["stop_id"]
            if (stop !in stopLabels) continue
            val trip = trips[row["trip_id"]] ?: continue
            // pickup_type 1 = no boarding (e.g. last stop of the run): not a departure.
            if (row["pickup_type"] == "1") continue
            val time = parseGtfsTime(row["departure_time"].ifBlank { row["arrival_time"] }) ?: continue
            times.getOrPut(PatternKey(stop, trip.route, trip.direction, trip.headsign, trip.service)) { sortedSetOf() }.add(time)
        }
    }
    require(times.isNotEmpty()) { "No departures at ${stopLabels.keys}" }

    val usedRoutes = times.keys.map { it.route }.toSet()
    val usedServices = times.keys.map { it.service }.toSet()

    val stopNames = feed.read("stops.txt") { rows ->
        rows.filter { it["stop_id"] in stopLabels }.associate { it["stop_id"] to it["stop_name"] }
    }

    val calendar = feed.read("calendar.txt") { rows -> rows.filter { it["service_id"] in usedServices }.toList() }
    val exceptions = if (feed.has("calendar_dates.txt")) {
        feed.read("calendar_dates.txt") { rows -> rows.filter { it["service_id"] in usedServices }.toList() }
    } else emptyList()
    val feedInfo = if (feed.has("feed_info.txt")) feed.read("feed_info.txt") { it.firstOrNull() } else null

    val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

    return buildJsonObject {
        put("feedStart", feedInfo?.get("feed_start_date").orEmpty())
        put("feedEnd", feedInfo?.get("feed_end_date").orEmpty())
        put("routes", buildJsonArray {
            for (id in usedRoutes.sorted()) {
                val r = routes.getValue(id)
                add(buildJsonObject {
                    put("id", id)
                    put("name", r["route_short_name"])
                    put("longName", r["route_long_name"])
                    put("color", r["route_color"].ifBlank { "5B5B5B" })
                    put("textColor", r["route_text_color"].ifBlank { "FFFFFF" })
                    put("type", r["route_type"].toIntOrNull() ?: 3)
                })
            }
        })
        put("stops", buildJsonArray {
            for ((id, label) in stopLabels) {
                add(buildJsonObject {
                    put("id", id)
                    put("name", stopNames[id] ?: id)
                    put("label", label)
                })
            }
        })
        put("services", buildJsonArray {
            for (c in calendar.sortedBy { it["service_id"] }) {
                val id = c["service_id"]
                add(buildJsonObject {
                    put("id", id)
                    put("days", JsonArray(days.map { JsonPrimitive(c[it] == "1") }))
                    put("start", c["start_date"])
                    put("end", c["end_date"])
                    putJsonArray("added") { exceptions.filter { it["service_id"] == id && it["exception_type"] == "1" }.forEach { add(it["date"]) } }
                    putJsonArray("removed") { exceptions.filter { it["service_id"] == id && it["exception_type"] == "2" }.forEach { add(it["date"]) } }
                })
            }
        })
        put("patterns", buildJsonArray {
            val order = compareBy<PatternKey>({ it.stop }, { it.route }, { it.direction }, { it.headsign }, { it.service })
            for (key in times.keys.sortedWith(order)) {
                add(buildJsonObject {
                    put("stop", key.stop)
                    put("route", key.route)
                    put("direction", key.direction)
                    put("headsign", key.headsign)
                    put("service", key.service)
                    put("times", JsonArray(times.getValue(key).map { JsonPrimitive(it) }))
                })
            }
        })
    }
}

internal val prettyJson = Json { prettyPrint = false }
