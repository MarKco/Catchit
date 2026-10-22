package com.ilsecondodasinistra.catchit.ui.map

import androidx.compose.ui.graphics.toArgb
import com.ilsecondodasinistra.catchit.data.GeoCatalog
import com.ilsecondodasinistra.catchit.data.LatLon
import com.ilsecondodasinistra.catchit.data.Leg
import com.ilsecondodasinistra.catchit.data.VehicleFix
import com.ilsecondodasinistra.catchit.domain.Departure
import com.ilsecondodasinistra.catchit.domain.isPast
import com.ilsecondodasinistra.catchit.ui.board.BoardUiState
import com.ilsecondodasinistra.catchit.ui.board.distanceMeters
import com.ilsecondodasinistra.catchit.ui.board.toTitleCase
import com.ilsecondodasinistra.catchit.ui.theme.lineColors
import java.time.Instant

/** What the map was asked for: a leg of a destination, optionally with one passage's whole run highlighted. */
data class MapTarget(val destinationId: String, val leg: Leg, val highlight: Departure? = null)

/** A stop the passages of the leg leave from. */
data class MapStop(val id: String, val name: String, val position: LatLon)

/**
 * A vehicle on the map. [line] is null for one we can't tell (a yellow dot); [eta] is when it gets to the stop, if the feed
 * says so; [highlighted] marks the one whose run is being shown.
 */
data class MapVehicle(
    val key: String,
    val position: LatLon,
    val bearing: Float?,
    val line: String?,
    val colorArgb: Int?,
    val eta: Instant?,
    val highlighted: Boolean,
    /** When the GPS fix was taken (epoch seconds), null for none. */
    val fixEpochSecond: Long? = null,
)

enum class RouteStopKind { Passed, Upcoming, Boarding, Alight }

data class RouteStop(val name: String, val position: LatLon, val kind: RouteStopKind)

/** The run of the highlighted passage: its path and every stop, told apart by where the vehicle is. */
data class RouteOverlay(val colorArgb: Int, val path: List<LatLon>, val stops: List<RouteStop>)

data class MapModel(
    val boardingStops: List<MapStop>,
    val vehicles: List<MapVehicle>,
    val route: RouteOverlay?,
    /** What the camera opens on. */
    val focus: List<LatLon>,
    val title: String,
    /** The instant the model was worked out at: the minutes in a vehicle's bubble count from here. */
    val now: Instant,
)

/** Vehicles farther than this from every boarding stop are left out: the map is about what is about to arrive. */
const val MAP_RADIUS_METERS = 2_000.0

/** The camera never opens tighter than this around the stops, so a vehicle that is close by is still in view. */
const val MAP_MIN_HALF_SPAN_METERS = 900.0

/**
 * Works out what the map shows from the board's state: the boarding stops of the leg, and the vehicles of the
 * upcoming live passages of that leg that are within [MAP_RADIUS_METERS] of them (plus the highlighted one wherever it is).
 * The run drawn is the one of the vehicle tapped on the map ([selectedTripId]) or, failing that, of [MapTarget.highlight].
 * Pure, so the map keeps up with every live update and is easy to test.
 */
fun buildMapModel(state: BoardUiState, target: MapTarget, geo: GeoCatalog?, selectedTripId: String? = null): MapModel? {
    val timetable = state.timetable ?: return null
    val destination = state.destinations.firstOrNull { it.id == target.destinationId } ?: return null
    val board = destination.legs.firstOrNull { it.leg == target.leg } ?: return null

    val stops = timetable.stopRoutes(target.destinationId, target.leg).map { it.stopId }.distinct().sorted().mapNotNull { id ->
        val stop = timetable.stop(id)
        val lat = stop?.lat
        val lon = stop?.lon
        if (stop != null && lat != null && lon != null) MapStop(id, stop.name.toTitleCase(), LatLon(lat, lon)) else null
    }
    if (stops.isEmpty()) return null

    val live = state.browsing == null
    val vehicles = ArrayList<MapVehicle>()

    // The earliest upcoming live passage of each trip (a trip may serve two poles of the leg).
    val upcoming = board.departures
        .filter { live && it.isRealtime && it.tripId != null && !it.isPast(state.now) }
        .groupBy { it.tripId }
        .mapValues { (_, list) -> list.minBy { it.time } }
    val highlight = selectedTripId?.let { upcoming[it] } ?: target.highlight
    for ((tripId, departure) in upcoming) {
        val fix = state.liveTrips[tripId]?.vehicle ?: continue
        val isHighlighted = highlight?.tripId == tripId
        if (!isHighlighted && stops.none { it.position.metersTo(fix) <= MAP_RADIUS_METERS }) continue
        vehicles += MapVehicle(
            key = tripId.orEmpty(),
            position = LatLon(fix.latitude, fix.longitude),
            bearing = fix.bearing,
            line = departure.route.name,
            colorArgb = departure.route.lineColors().container.toArgb(),
            eta = departure.time,
            highlighted = isHighlighted,
            fixEpochSecond = fix.epochSecond,
        )
    }
    // The highlighted passage may have fallen out of the leg's list (it left): keep its vehicle on the map anyway.
    if (highlight != null && live && vehicles.none { it.highlighted }) {
        val fix = highlight.tripId?.let { state.liveTrips[it]?.vehicle }
        if (fix != null) {
            vehicles += MapVehicle(highlight.tripId.orEmpty(), LatLon(fix.latitude, fix.longitude), fix.bearing, highlight.route.name, highlight.route.lineColors().container.toArgb(), highlight.time, true, fix.epochSecond)
        }
    }
    if (live) {
        for (tram in state.looseTrams) {
            if (stops.none { it.position.metersTo(tram.fix) <= MAP_RADIUS_METERS }) continue
            vehicles += MapVehicle("loose:${tram.label}", LatLon(tram.fix.latitude, tram.fix.longitude), tram.fix.bearing, null, null, null, false, tram.fix.epochSecond)
        }
    }

    val route = highlight?.let { routeOverlay(state, it, geo) }
    val boarding = highlight?.let { h -> stops.firstOrNull { it.id == h.stopId } }
    val highlightedVehicle = vehicles.firstOrNull { it.highlighted }
    val focus = if (highlight != null && highlightedVehicle != null) {
        listOfNotNull(highlightedVehicle.position, boarding?.position)
    } else {
        stops.map { it.position }
    }
    return MapModel(stops, vehicles, route, focus, title = "${destination.origin} ⇄ ${destination.target}", now = state.now)
}

private fun routeOverlay(state: BoardUiState, departure: Departure, geo: GeoCatalog?): RouteOverlay? {
    val timetable = state.timetable ?: return null
    val trip = departure.tripId?.let { state.liveTrips[it] }.takeIf { departure.isRealtime && state.browsing == null }
    val fix = trip?.vehicle
    val seen = HashSet<String>().apply {
        trip?.stopTimes?.keys?.let(::addAll)
        fix?.stopId?.let(::add)
    }
    val color = departure.route.lineColors().container.toArgb()

    val run = geo?.findRun(departure.route.id, departure.stopId, departure.alightStopId, seen)
    val ids: List<String>
    val points: List<RouteStopPoint>
    val path: List<LatLon>
    if (run != null) {
        points = run.stops.mapNotNull { s -> s.position()?.let { RouteStopPoint(s.id, s.name.toTitleCase(), it) } }
        path = run.path
    } else {
        // The catalog doesn't know this run: at least the stops between boarding and getting off, joined by straight lines.
        val shape = departure.shape ?: return null
        points = shape.stops.mapNotNull { id -> timetable.stop(id)?.let { s -> s.position()?.let { RouteStopPoint(id, s.name.toTitleCase(), it) } } }
        path = emptyList()
    }
    ids = points.map { it.id }
    if (points.size < 2) return null

    // Where the vehicle is along the run: the stop it last served, else the first stop the feed still expects it at.
    val lastServed = fix?.stopId?.let { ids.indexOf(it) }?.takeIf { it >= 0 }
        ?: trip?.stopTimes?.keys?.let { live -> ids.indexOfFirst { it in live } - 1 }?.takeIf { it >= 0 }
        ?: -1
    val boardIndex = ids.indexOf(departure.stopId)
    val alightIndex = departure.alightStopId?.let { alight -> ids.withIndex().lastOrNull { it.value == alight && it.index > boardIndex }?.index } ?: -1
    val stops = points.mapIndexed { i, p ->
        val kind = when {
            i == boardIndex -> RouteStopKind.Boarding
            i == alightIndex -> RouteStopKind.Alight
            i <= lastServed -> RouteStopKind.Passed
            else -> RouteStopKind.Upcoming
        }
        RouteStop(p.name, p.position, kind)
    }
    return RouteOverlay(color, path.ifEmpty { points.map { it.position } }, stops)
}

private class RouteStopPoint(val id: String, val name: String, val position: LatLon)

private fun com.ilsecondodasinistra.catchit.data.StopInfo.position(): LatLon? {
    val lat = lat ?: return null
    val lon = lon ?: return null
    return LatLon(lat, lon)
}

private fun LatLon.metersTo(fix: VehicleFix): Double = distanceMeters(lat, lon, fix.latitude, fix.longitude)
