package com.ilsecondodasinistra.catchit.data

import kotlinx.serialization.Serializable

/**
 * Shape of `assets/timetable.json`, written by `:tools:importer`. It only holds the lines that directly link the
 * configured places, never the whole ACTV network.
 */
@Serializable
data class TimetableData(
    /** `TimetableConfig.fingerprint` of the destinations it was built for. */
    val config: String = "",
    val feedStart: String = "",
    val feedEnd: String = "",
    val routes: List<RouteInfo>,
    val stops: List<StopInfo>,
    val destinations: List<DestinationInfo>,
    val services: List<ServiceInfo>,
    val patterns: List<Pattern>,
)

@Serializable
data class RouteInfo(
    val id: String,
    val name: String,
    val longName: String = "",
    /** Hex without '#', as in GTFS. */
    val color: String,
    val textColor: String,
    /** GTFS route_type: 0 = tram, 3 = bus. */
    val type: Int,
) {
    val isTram: Boolean get() = type == 0
}

@Serializable
data class StopInfo(val id: String, val name: String)

/** One bottom-bar entry: [origin] ⇄ [target] (display names), e.g. "Musatti" ⇄ "Mestre Centro". */
@Serializable
data class DestinationInfo(val id: String, val label: String, val origin: String, val target: String)

/** The two tabs of a destination. */
enum class Leg(val id: String) {
    /** Departures from the origin that reach the target without changing. */
    Outbound("andata"),

    /** Departures from the target that reach the origin without changing. */
    Return("ritorno"),
}

@Serializable
data class ServiceInfo(
    val id: String,
    /** Monday..Sunday. */
    val days: List<Boolean>,
    val start: String,
    val end: String,
    val added: List<String> = emptyList(),
    val removed: List<String> = emptyList(),
)

/**
 * Departure times (seconds after the service day's noon-minus-12h, may exceed 24h) of one
 * destination/leg/stop/route/service.
 */
@Serializable
data class Pattern(
    val destination: String,
    val leg: String,
    val stop: String,
    val route: String,
    val direction: Int,
    val headsign: String,
    val service: String,
    val times: List<Int>,
)
