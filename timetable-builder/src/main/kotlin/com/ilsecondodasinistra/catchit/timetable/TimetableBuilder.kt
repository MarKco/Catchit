package com.ilsecondodasinistra.catchit.timetable

import java.io.File
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

private data class TripInfo(val route: String, val direction: Int, val headsign: String, val service: String, val shape: String)

/** One visit of a trip to a stop. */
private class Call(
    val sequence: Int,
    val stop: String,
    val arrival: Int,
    val departure: Int,
    val canBoard: Boolean,
    val canAlight: Boolean,
)

/** What the run looks like apart from its calendar: one set of stops with typical offsets serves all services. */
private data class ShapeKey(
    val destination: String,
    val leg: String,
    val stop: String,
    val route: String,
    val direction: Int,
    val headsign: String,
    /** Where the run reaches the other end of the leg. */
    val alight: String,
)

private data class PatternKey(val shape: ShapeKey, val service: String)

/** A trip that links the two ends of a destination: boarding at [boardSequence], getting off at [alightSequence]. */
private class Qualification(val key: PatternKey, val boardSequence: Int, val alightSequence: Int, val departure: Int, val arrival: Int)

private class ShapeSample(val stops: List<String>, val offsets: List<Int>)

private data class ShapeDef(val stops: List<String>, val offsets: List<Int>)

private class StopRow(val name: String, val lat: String, val lon: String)

/** One way a line runs: its whole sequence of stops (not only the part of the leg), which also tells the trip's path. */
private data class VariantKey(val route: String, val direction: Int, val stops: List<String>)

/** The two files the app needs: the timetable, and the geography (paths, stops of the whole runs) read only by the map. */
class BuiltFiles(val timetable: String, val geo: String)

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
 * Output (see `data/TimetableData.kt` in the app): routes, stops (with coordinates), destinations, services (weekly
 * calendar plus exceptions), *shapes* (the stops of a run with their typical offsets from the departure) and
 * *patterns* = the departures of one (destination, leg, stop, route, direction, headsign, alight stop, service),
 * sorted, each with how long it takes to get off.
 */
internal fun buildTimetable(feed: GtfsZip, destinations: List<Destination>, log: (String) -> Unit = {}): Pair<JsonObject, JsonObject> {
    val stopRows = feed.read("stops.txt") { rows -> rows.associate { it["stop_id"] to StopRow(it["stop_name"], it["stop_lat"], it["stop_lon"]) } }
    val stopNames = stopRows.mapValues { it.value.name }

    val originStops = destinations.associate { d -> d.id to stopNames.filter { (id, n) -> d.origin.matches(id, n) }.keys }
    val targetStops = destinations.associate { d -> d.id to stopNames.filter { (id, n) -> d.target.matches(id, n) }.keys }
    for (d in destinations) {
        require(originStops.getValue(d.id).isNotEmpty()) { "No stop matches the origin of ${d.id}" }
        require(targetStops.getValue(d.id).isNotEmpty()) { "No stop matches the target of ${d.id}" }
        log("${d.id}: ${d.origin.name} ${originStops.getValue(d.id).sorted()} ⇄ ${d.target.name} ${targetStops.getValue(d.id).sorted()}")
    }
    val relevant = (originStops.values + targetStops.values).flatten().toSet()

    val trips = feed.read("trips.txt") { rows ->
        rows.associate {
            it["trip_id"] to TripInfo(it["route_id"], it["direction_id"].toIntOrNull() ?: 0, it["trip_headsign"], it["service_id"], it["shape_id"])
        }
    }

    // Pass 1: where does each trip touch the places we care about? (Only those stops are kept.)
    val callsByTrip = HashMap<String, MutableList<Call>>()
    val lastSequence = HashMap<String, Int>()
    feed.read("stop_times.txt") { rows ->
        for (row in rows) {
            row["stop_sequence"].toIntOrNull()?.let { lastSequence.merge(row["trip_id"], it, ::maxOf) }
            val stop = row["stop_id"]
            if (stop !in relevant) continue
            val arrival = parseGtfsTime(row["arrival_time"])
            val departure = parseGtfsTime(row["departure_time"])
            val sequence = row["stop_sequence"].toIntOrNull() ?: continue
            // pickup_type / drop_off_type 1 = not available (e.g. last stop of a run: nobody boards there).
            callsByTrip.getOrPut(row["trip_id"]) { ArrayList(2) }.add(
                Call(sequence, stop, arrival ?: departure ?: continue, departure ?: arrival ?: continue, row["pickup_type"] != "1", row["drop_off_type"] != "1"),
            )
        }
    }

    val qualifications = HashMap<String, MutableList<Qualification>>()
    for ((tripId, calls) in callsByTrip) {
        val trip = trips[tripId] ?: continue
        calls.sortBy { it.sequence }
        // The feed marks the terminus of the night loops as "no drop-off", yet that is where everybody gets off.
        val last = lastSequence[tripId]
        calls.replaceAll { if (it.sequence == last && !it.canAlight) Call(it.sequence, it.stop, it.arrival, it.departure, it.canBoard, true) else it }
        for (d in destinations) {
            val origins = originStops.getValue(d.id)
            val targets = targetStops.getValue(d.id)
            fun collect(leg: String, from: Set<String>, to: Set<String>) {
                for ((i, call) in calls.withIndex()) {
                    if (call.stop !in from || !call.canBoard) continue
                    val alight = calls.subList(i + 1, calls.size).firstOrNull { it.stop in to && it.canAlight } ?: continue
                    val key = PatternKey(ShapeKey(d.id, leg, call.stop, trip.route, trip.direction, trip.headsign, alight.stop), trip.service)
                    qualifications.getOrPut(tripId) { ArrayList(1) }.add(Qualification(key, call.sequence, alight.sequence, call.departure, alight.arrival))
                }
            }
            collect(LEG_OUTBOUND, origins, targets)
            collect(LEG_RETURN, targets, origins)
        }
    }
    require(qualifications.isNotEmpty()) { "No direct connection found" }

    // Pass 2: the stops between boarding and getting off, only for the trips that qualify.
    val fullCalls = HashMap<String, MutableList<Call>>()
    feed.read("stop_times.txt") { rows ->
        for (row in rows) {
            val tripId = row["trip_id"]
            if (tripId !in qualifications) continue
            val sequence = row["stop_sequence"].toIntOrNull() ?: continue
            val arrival = parseGtfsTime(row["arrival_time"])
            val departure = parseGtfsTime(row["departure_time"])
            fullCalls.getOrPut(tripId) { ArrayList(32) }.add(Call(sequence, row["stop_id"], arrival ?: departure ?: continue, departure ?: arrival ?: continue, true, true))
        }
    }

    // departure -> minutes to get off, per pattern; samples of the stops between, per shape.
    val times = HashMap<PatternKey, java.util.TreeMap<Int, Int>>()
    val samples = HashMap<ShapeKey, MutableList<ShapeSample>>()
    for ((tripId, list) in qualifications) {
        val calls = fullCalls[tripId]?.sortedBy { it.sequence }.orEmpty()
        for (q in list) {
            times.getOrPut(q.key) { java.util.TreeMap() }[q.departure] = (q.arrival - q.departure).coerceAtLeast(0)
            val between = calls.filter { it.sequence in q.boardSequence..q.alightSequence }
            if (between.size >= 2) {
                samples.getOrPut(q.key.shape) { ArrayList() }
                    .add(ShapeSample(between.map { it.stop }, between.mapIndexed { i, c -> if (i == 0) 0 else (c.arrival - q.departure).coerceAtLeast(0) }))
            }
        }
    }
    for (d in destinations) for (leg in listOf(LEG_OUTBOUND, LEG_RETURN)) {
        val lines = times.keys.filter { it.shape.destination == d.id && it.shape.leg == leg }.map { it.shape.route.substringBefore('_') }.toSortedSet()
        log("  ${d.id}/$leg: $lines")
    }

    // One shape per ShapeKey: the most common sequence of stops, each with its median offset.
    val shapeIds = LinkedHashMap<ShapeDef, Int>()
    val shapeOf = HashMap<ShapeKey, Int>()
    for ((key, list) in samples) {
        val common = list.groupBy { it.stops }.maxByOrNull { it.value.size }!!
        val offsets = common.key.indices.map { i -> common.value.map { it.offsets[i] }.sorted().let { it[it.size / 2] } }
        shapeOf[key] = shapeIds.getOrPut(ShapeDef(common.key, offsets)) { shapeIds.size }
    }
    log("  ${shapeIds.size} shapes")

    // The whole run of each qualifying trip, for the map: the stops before boarding and after getting off, and the path.
    val shapeVotes = HashMap<VariantKey, MutableMap<String, Int>>()
    for (tripId in qualifications.keys) {
        val trip = trips[tripId] ?: continue
        val stops = fullCalls[tripId]?.sortedBy { it.sequence }?.map { it.stop } ?: continue
        if (stops.size >= 2) shapeVotes.getOrPut(VariantKey(trip.route, trip.direction, stops)) { HashMap() }.merge(trip.shape, 1, Int::plus)
    }
    val variants = shapeVotes.keys.sortedWith(compareBy({ it.route }, { it.direction }, { it.stops.joinToString(",") }))
    val pathOf = variants.associateWith { key -> shapeVotes.getValue(key).filterKeys { it.isNotEmpty() }.maxByOrNull { it.value }?.key }
    val paths = readPaths(feed, pathOf.values.filterNotNull().toSet())
    val pathIndex = paths.keys.sorted().withIndex().associate { (i, id) -> id to i }
    log("  ${variants.size} runs, ${paths.size} paths (${paths.values.sumOf { it.size / 2 }} points)")

    val usedRoutes = times.keys.map { it.shape.route }.toSet()
    val usedServices = times.keys.map { it.service }.toSet()
    val usedStops = (times.keys.flatMap { listOf(it.shape.stop, it.shape.alight) } + shapeIds.keys.flatMap { it.stops }).toSet()

    val routes = feed.read("routes.txt") { rows -> rows.filter { it["route_id"] in usedRoutes }.associateBy { it["route_id"] } }
    val calendar = feed.read("calendar.txt") { rows -> rows.filter { it["service_id"] in usedServices }.toList() }
    val exceptions = if (feed.has("calendar_dates.txt")) {
        feed.read("calendar_dates.txt") { rows -> rows.filter { it["service_id"] in usedServices }.toList() }
    } else emptyList()
    val feedInfo = if (feed.has("feed_info.txt")) feed.read("feed_info.txt") { it.firstOrNull() } else null

    val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

    val feedStart = feedInfo?.get("feed_start_date").orEmpty()
    val feedEnd = feedInfo?.get("feed_end_date").orEmpty()

    val geo = buildJsonObject {
        put("config", TimetableConfig.fingerprint)
        put("feedStart", feedStart)
        put("feedEnd", feedEnd)
        put("stops", buildJsonArray {
            for (id in variants.flatMap { it.stops }.toSet().sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }) {
                val row = stopRows[id] ?: continue
                add(buildJsonObject {
                    put("id", id)
                    put("name", row.name)
                    row.lat.toDoubleOrNull()?.let { put("lat", it) }
                    row.lon.toDoubleOrNull()?.let { put("lon", it) }
                })
            }
        })
        put("runs", buildJsonArray {
            for ((i, key) in variants.withIndex()) {
                add(buildJsonObject {
                    put("id", i)
                    put("route", key.route)
                    put("direction", key.direction)
                    put("stops", JsonArray(key.stops.map { JsonPrimitive(it) }))
                    pathOf[key]?.let { pathIndex[it] }?.let { put("path", it) }
                })
            }
        })
        put("paths", buildJsonArray {
            for (id in paths.keys.sorted()) add(JsonArray(paths.getValue(id).map { JsonPrimitive(it) }))
        })
    }

    val timetable = buildJsonObject {
        put("config", TimetableConfig.fingerprint)
        put("feedStart", feedStart)
        put("feedEnd", feedEnd)
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
                val row = stopRows.getValue(id)
                add(buildJsonObject {
                    put("id", id)
                    put("name", row.name)
                    row.lat.toDoubleOrNull()?.let { put("lat", it) }
                    row.lon.toDoubleOrNull()?.let { put("lon", it) }
                })
            }
        })
        put("shapes", buildJsonArray {
            for ((shape, id) in shapeIds) {
                add(buildJsonObject {
                    put("id", id)
                    put("stops", JsonArray(shape.stops.map { JsonPrimitive(it) }))
                    put("offsets", JsonArray(shape.offsets.map { JsonPrimitive(it) }))
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
            val order = compareBy<PatternKey>({ it.shape.destination }, { it.shape.leg }, { it.shape.stop }, { it.shape.route }, { it.shape.direction }, { it.shape.headsign }, { it.shape.alight }, { it.service })
            for (key in times.keys.sortedWith(order)) {
                val byTime = times.getValue(key)
                add(buildJsonObject {
                    put("destination", key.shape.destination)
                    put("leg", key.shape.leg)
                    put("stop", key.shape.stop)
                    put("route", key.shape.route)
                    put("direction", key.shape.direction)
                    put("headsign", key.shape.headsign)
                    put("alight", key.shape.alight)
                    shapeOf[key.shape]?.let { put("shape", it) }
                    put("service", key.service)
                    put("times", JsonArray(byTime.keys.map { JsonPrimitive(it) }))
                    put("durations", JsonArray(byTime.values.map { JsonPrimitive(it) }))
                })
            }
        })
    }
    return timetable to geo
}

/** Paths are simplified to this: a vehicle drawn on the street never looks off by more than a few metres. */
private const val PATH_TOLERANCE_METERS = 4.0

/**
 * The path of each of [ids] in `shapes.txt`, simplified and packed as ints: the first point is absolute, the others
 * are the difference from the previous one, in 1e-5 degrees (about a metre), `lat, lon, dLat, dLon, ...`.
 */
private fun readPaths(feed: GtfsZip, ids: Set<String>): Map<String, List<Int>> {
    if (ids.isEmpty() || !feed.has("shapes.txt")) return emptyMap()
    class Point(val sequence: Int, val lat: Double, val lon: Double)
    val raw = HashMap<String, MutableList<Point>>()
    feed.read("shapes.txt") { rows ->
        for (row in rows) {
            val id = row["shape_id"]
            if (id !in ids) continue
            val lat = row["shape_pt_lat"].toDoubleOrNull() ?: continue
            val lon = row["shape_pt_lon"].toDoubleOrNull() ?: continue
            raw.getOrPut(id) { ArrayList(256) }.add(Point(row["shape_pt_sequence"].toIntOrNull() ?: continue, lat, lon))
        }
    }
    return raw.mapValues { (_, points) ->
        val line = points.sortedBy { it.sequence }.map { doubleArrayOf(it.lat, it.lon) }
        var previousLat = 0
        var previousLon = 0
        simplify(line, PATH_TOLERANCE_METERS).flatMap { (lat, lon) ->
            val a = Math.round(lat * 1e5).toInt()
            val b = Math.round(lon * 1e5).toInt()
            listOf(a - previousLat, b - previousLon).also { previousLat = a; previousLon = b }
        }
    }
}

private operator fun DoubleArray.component1() = this[0]
private operator fun DoubleArray.component2() = this[1]

/** Douglas-Peucker on (lat, lon) points, [tolerance] in metres (flat approximation: fine at city scale). */
internal fun simplify(points: List<DoubleArray>, tolerance: Double): List<DoubleArray> {
    if (points.size < 3) return points
    val metersPerDegreeLat = 111_000.0
    val metersPerDegreeLon = 111_000.0 * Math.cos(Math.toRadians(points[0][0]))
    val keep = BooleanArray(points.size).also { it[0] = true; it[points.lastIndex] = true }
    val pending = ArrayDeque<IntArray>().apply { add(intArrayOf(0, points.lastIndex)) }
    while (pending.isNotEmpty()) {
        val (first, last) = pending.removeLast().let { it[0] to it[1] }
        if (last <= first + 1) continue
        val dx = (points[last][1] - points[first][1]) * metersPerDegreeLon
        val dy = (points[last][0] - points[first][0]) * metersPerDegreeLat
        val length = Math.hypot(dx, dy)
        var farthest = -1.0
        var at = first
        for (i in first + 1 until last) {
            val px = (points[i][1] - points[first][1]) * metersPerDegreeLon
            val py = (points[i][0] - points[first][0]) * metersPerDegreeLat
            val distance = if (length == 0.0) Math.hypot(px, py) else Math.abs(px * dy - py * dx) / length
            if (distance > farthest) { farthest = distance; at = i }
        }
        if (farthest > tolerance) {
            keep[at] = true
            pending.add(intArrayOf(first, at))
            pending.add(intArrayOf(at, last))
        }
    }
    return points.filterIndexed { i, _ -> keep[i] }
}

private val compactJson = Json { prettyPrint = false }

/** Public entry point, shared by the importer tool and the app. */
object TimetableBuilder {
    /**
     * Reads the AVM GTFS [zip] and returns `timetable.json` and `geo.json` for the lines that directly link the
     * [destinations]' places. Throws when nothing matches, so callers keep the timetable they already have.
     */
    fun buildFiles(
        zip: File,
        destinations: List<Destination> = TimetableConfig.destinations,
        log: (String) -> Unit = {},
    ): BuiltFiles = GtfsZip(zip).use {
        val (timetable, geo) = buildTimetable(it, destinations, log)
        BuiltFiles(compactJson.encodeToString(JsonObject.serializer(), timetable), compactJson.encodeToString(JsonObject.serializer(), geo))
    }

    /** Just `timetable.json`. */
    fun build(
        zip: File,
        destinations: List<Destination> = TimetableConfig.destinations,
        log: (String) -> Unit = {},
    ): String = buildFiles(zip, destinations, log).timetable
}
