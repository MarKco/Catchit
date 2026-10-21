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

internal const val LEG_OUTBOUND = "andata"
internal const val LEG_RETURN = "ritorno"

private data class TripInfo(val route: String, val direction: Int, val headsign: String, val service: String)

/** One visit of a trip to a stop we care about. */
private class Call(val sequence: Int, val stop: String, val time: Int, val canBoard: Boolean, val canAlight: Boolean)

private data class PatternKey(
    val destination: String,
    val leg: String,
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
 * Reduces the full feed to the lines that directly link each destination's origin and target.
 *
 * Output (see `data/TimetableData.kt` in the app): routes, stops, destinations, services (weekly calendar plus
 * exceptions) and *patterns* = the departure times of one (destination, leg, stop, route, direction, headsign,
 * service), sorted.
 */
internal fun buildTimetable(feed: Feed, destinations: List<Destination>): JsonObject {
    val stopNames = feed.read("stops.txt") { rows -> rows.associate { it["stop_id"] to it["stop_name"] } }

    val originStops = destinations.associate { d -> d.id to stopNames.filter { (id, n) -> d.origin.matches(id, n) }.keys }
    val targetStops = destinations.associate { d -> d.id to stopNames.filter { (id, n) -> d.target.matches(id, n) }.keys }
    for (d in destinations) {
        require(originStops.getValue(d.id).isNotEmpty()) { "No stop matches the origin of ${d.id}" }
        require(targetStops.getValue(d.id).isNotEmpty()) { "No stop matches the target of ${d.id}" }
        println("${d.id}: ${d.origin.name} ${originStops.getValue(d.id).sorted()} ⇄ ${d.target.name} ${targetStops.getValue(d.id).sorted()}")
    }
    val relevant = (originStops.values + targetStops.values).flatten().toSet()

    val trips = feed.read("trips.txt") { rows ->
        rows.associate {
            it["trip_id"] to TripInfo(it["route_id"], it["direction_id"].toIntOrNull() ?: 0, it["trip_headsign"], it["service_id"])
        }
    }

    val callsByTrip = HashMap<String, MutableList<Call>>()
    val lastSequence = HashMap<String, Int>()
    feed.read("stop_times.txt") { rows ->
        for (row in rows) {
            row["stop_sequence"].toIntOrNull()?.let { lastSequence.merge(row["trip_id"], it, ::maxOf) }
            val stop = row["stop_id"]
            if (stop !in relevant) continue
            val time = parseGtfsTime(row["departure_time"].ifBlank { row["arrival_time"] }) ?: continue
            val sequence = row["stop_sequence"].toIntOrNull() ?: continue
            // pickup_type / drop_off_type 1 = not available (e.g. last stop of a run: nobody boards there).
            callsByTrip.getOrPut(row["trip_id"]) { ArrayList(2) }
                .add(Call(sequence, stop, time, row["pickup_type"] != "1", row["drop_off_type"] != "1"))
        }
    }

    val times = HashMap<PatternKey, MutableSet<Int>>()
    for ((tripId, calls) in callsByTrip) {
        val trip = trips[tripId] ?: continue
        calls.sortBy { it.sequence }
        // The feed marks the terminus of the night loops as "no drop-off", yet that is where everybody gets off.
        val last = lastSequence[tripId]
        calls.replaceAll { if (it.sequence == last && !it.canAlight) Call(it.sequence, it.stop, it.time, it.canBoard, true) else it }
        for (d in destinations) {
            val origins = originStops.getValue(d.id)
            val targets = targetStops.getValue(d.id)
            fun collect(leg: String, from: Set<String>, to: Set<String>) {
                for ((i, call) in calls.withIndex()) {
                    if (call.stop !in from || !call.canBoard) continue
                    if (calls.subList(i + 1, calls.size).none { it.stop in to && it.canAlight }) continue
                    times.getOrPut(PatternKey(d.id, leg, call.stop, trip.route, trip.direction, trip.headsign, trip.service)) { sortedSetOf() }.add(call.time)
                }
            }
            collect(LEG_OUTBOUND, origins, targets)
            collect(LEG_RETURN, targets, origins)
        }
    }
    require(times.isNotEmpty()) { "No direct connection found" }
    for (d in destinations) for (leg in listOf(LEG_OUTBOUND, LEG_RETURN)) {
        val lines = times.keys.filter { it.destination == d.id && it.leg == leg }.map { it.route.substringBefore('_') }.toSortedSet()
        println("  ${d.id}/$leg: $lines")
    }

    val usedRoutes = times.keys.map { it.route }.toSet()
    val usedServices = times.keys.map { it.service }.toSet()
    val usedStops = times.keys.map { it.stop }.toSet()

    val routes = feed.read("routes.txt") { rows -> rows.filter { it["route_id"] in usedRoutes }.associateBy { it["route_id"] } }
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
            for (id in usedStops.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }) {
                add(buildJsonObject {
                    put("id", id)
                    put("name", stopNames.getValue(id))
                })
            }
        })
        put("destinations", buildJsonArray {
            for (d in destinations) {
                add(buildJsonObject {
                    put("id", d.id)
                    put("label", d.label)
                    put("origin", d.origin.name)
                    put("target", d.target.name)
                })
            }
        })
        put("services", buildJsonArray {
            // Services of holidays are often only in calendar_dates (extra days, no weekly pattern).
            val weekly = calendar.associateBy { it["service_id"] }
            for (id in usedServices.sorted()) {
                val c = weekly[id]
                add(buildJsonObject {
                    put("id", id)
                    put("days", JsonArray(days.map { JsonPrimitive(c?.get(it) == "1") }))
                    put("start", c?.get("start_date").orEmpty())
                    put("end", c?.get("end_date").orEmpty())
                    putJsonArray("added") { exceptions.filter { it["service_id"] == id && it["exception_type"] == "1" }.forEach { add(it["date"]) } }
                    putJsonArray("removed") { exceptions.filter { it["service_id"] == id && it["exception_type"] == "2" }.forEach { add(it["date"]) } }
                })
            }
        })
        put("patterns", buildJsonArray {
            val order = compareBy<PatternKey>({ it.destination }, { it.leg }, { it.stop }, { it.route }, { it.direction }, { it.headsign }, { it.service })
            for (key in times.keys.sortedWith(order)) {
                add(buildJsonObject {
                    put("destination", key.destination)
                    put("leg", key.leg)
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
