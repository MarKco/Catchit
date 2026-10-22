package com.ilsecondodasinistra.catchit.data

import kotlinx.serialization.Serializable

/**
 * Shape of `assets/geo.json`, written next to `timetable.json` by `:tools:importer` and read only when a map is
 * opened: the whole run of each trip that links the configured places (not just the part between boarding and
 * getting off) and the path it follows.
 */
@Serializable
data class GeoData(
    /** `TimetableConfig.fingerprint` of the destinations it was built for. */
    val config: String = "",
    val feedStart: String = "",
    val feedEnd: String = "",
    /** Every stop of every [runs] entry. */
    val stops: List<StopInfo>,
    val runs: List<RunInfo>,
    /** Simplified paths, as ints: `lat, lon` of the first point in 1e-5 degrees, then the difference from the previous one. */
    val paths: List<List<Int>> = emptyList(),
)

/** One way a line runs: [stops] in order from terminus to terminus, following the path number [path] (-1 = unknown). */
@Serializable
data class RunInfo(val id: Int, val route: String, val direction: Int, val stops: List<String>, val path: Int = -1)

data class LatLon(val lat: Double, val lon: Double)

/** A run with its stops and path resolved, ready to be drawn. */
data class GeoRun(val route: String, val direction: Int, val stops: List<StopInfo>, val path: List<LatLon>)

/**
 * The geography of one or more timetable versions. A run is looked up in the newest version that has it: a line
 * rarely changes its path between two feeds.
 */
class GeoCatalog(datas: List<GeoData>) {
    private class Version(val data: GeoData) {
        val stops = data.stops.associateBy { it.id }
    }

    /** Newest first. */
    private val versions = datas.sortedByDescending { it.feedStart }.map(::Version)

    /** A stop by id (name and coordinates), newest version first. */
    fun stop(id: String): StopInfo? = versions.firstNotNullOfOrNull { it.stops[id] }

    /**
     * The run of [routeId] that boards at [boardStopId] and later gets off at [alightStopId], or null when the catalog
     * doesn't know it. When several fit (branches, short runs) the one that visits the most of [seenStops] wins: the
     * stops the live feed says the trip has been to or is going to.
     */
    fun findRun(routeId: String, boardStopId: String, alightStopId: String?, seenStops: Set<String>): GeoRun? {
        for (version in versions) {
            val fitting = version.data.runs.filter { it.route == routeId && it.boards(boardStopId, alightStopId) }
            val best = fitting.maxWithOrNull(compareBy<RunInfo>({ run -> run.stops.count { it in seenStops } }, { -it.stops.size }))
                ?: continue
            return resolve(version, best)
        }
        return null
    }

    private fun RunInfo.boards(boardStopId: String, alightStopId: String?): Boolean {
        val from = stops.indexOf(boardStopId)
        if (from < 0) return false
        return alightStopId == null || stops.subList(from + 1, stops.size).contains(alightStopId)
    }

    private fun resolve(version: Version, run: RunInfo): GeoRun {
        val stops = run.stops.mapNotNull { version.stops[it] ?: stop(it) }
        return GeoRun(run.route, run.direction, stops, version.data.paths.getOrNull(run.path)?.let(::decodePath).orEmpty())
    }

    companion object {
        /** Inverse of the importer's packing: `lat, lon, dLat, dLon, ...` in 1e-5 degrees. */
        fun decodePath(packed: List<Int>): List<LatLon> {
            val out = ArrayList<LatLon>(packed.size / 2)
            var lat = 0
            var lon = 0
            for (i in 0 until packed.size / 2) {
                lat += packed[2 * i]
                lon += packed[2 * i + 1]
                out += LatLon(lat / 1e5, lon / 1e5)
            }
            return out
        }
    }
}
