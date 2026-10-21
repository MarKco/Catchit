package com.ilsecondodasinistra.catchit.importer

/** A named group of stops, matched by id or by name (case-insensitive). */
class Place(
    val name: String,
    private val ids: Set<String> = emptySet(),
    private val exactNames: Set<String> = emptySet(),
    private val namePrefixes: List<String> = emptyList(),
) {
    fun matches(stopId: String, stopName: String): Boolean =
        stopId in ids ||
            exactNames.any { it.equals(stopName, ignoreCase = true) } ||
            namePrefixes.any { stopName.startsWith(it, ignoreCase = true) }
}

/**
 * One bottom-bar entry. Its two tabs are the departures from [origin] that reach [target] without changing
 * (andata) and the departures from [target] that reach [origin] (ritorno): every line that links them directly.
 */
class Destination(val id: String, val label: String, val origin: Place, val target: Place)

/**
 * The app never ships the whole network: only the lines that directly link these places.
 * Edit and run `./gradlew :tools:importer:run` to regenerate `app/src/main/assets/timetable.json`.
 */
object ImporterConfig {
    const val FEED_URL = "https://actv.avmspa.it/sites/default/files/attachments/opendata/automobilistico/actv_aut.zip"
    const val CACHE_FILE = "tools/cache/actv_aut.zip"
    const val OUTPUT_FILE = "app/src/main/assets/timetable.json"

    /** Both poles of San Marco Musatti. */
    private val musatti = Place("Musatti", ids = setOf("6059", "6060"))

    /**
     * No stop is called "uffici giudiziari" or "tribunale": Hermada (both poles) is the guess; every pole of
     * Forte Marghera is served by the lines 15 (station + airport) and, from Hermada on, 43.
     */
    private val forteMarghera = Place("Forte Marghera", ids = setOf("172", "1172"))

    val destinations = listOf(
        Destination(
            id = "venezia",
            label = "Venezia",
            origin = musatti,
            target = Place("Venezia", exactNames = setOf("VENEZIA", "VENEZIA CORSIA C", "Piazzale Roma")),
        ),
        Destination(
            id = "centro",
            label = "Centro",
            origin = musatti,
            target = Place("Mestre Centro", namePrefixes = listOf("MESTRE CENTRO")),
        ),
        Destination(
            id = "stazione",
            label = "Stazione",
            origin = forteMarghera,
            target = Place("Stazione", namePrefixes = listOf("Stazione MESTRE FS", "Favretti MESTRE FS")),
        ),
        Destination(
            id = "aeroporto",
            label = "Aeroporto",
            origin = forteMarghera,
            target = Place("Aeroporto", namePrefixes = listOf("Aeroporto MARCO POLO")),
        ),
    )
}
