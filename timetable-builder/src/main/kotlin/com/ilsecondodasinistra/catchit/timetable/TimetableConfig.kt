package com.ilsecondodasinistra.catchit.timetable

/** A named group of stops, matched by id or by name (case-insensitive). */
class Place(
    val name: String,
    private val ids: Set<String> = emptySet(),
    private val exactNames: Set<String> = emptySet(),
    private val namePrefixes: List<String> = emptyList(),
) {
    /** Everything that decides which stops match, for [TimetableConfig.fingerprint]. */
    internal fun describe(): String = "$name|${ids.sorted()}|${exactNames.sorted()}|$namePrefixes"

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
 * Shared by the importer tool (bundled asset) and the app (updates downloaded on the phone): edit it and regenerate
 * the asset with `./gradlew :tools:importer:run`.
 */
object TimetableConfig {
    const val FEED_URL = "https://actv.avmspa.it/sites/default/files/attachments/opendata/automobilistico/actv_aut.zip"

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

    /**
     * Identifies the destinations above. Stored in every generated timetable: a timetable built (and downloaded)
     * by an older version of the app with other destinations must not be used.
     */
    val fingerprint: String
        get() = destinations.joinToString(";") { "${it.id}:${it.label}:${it.origin.describe()}:${it.target.describe()}" }
            .hashCode().toUInt().toString(16)
}
