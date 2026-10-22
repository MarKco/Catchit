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

/** San Marco Musatti and San Marco Cattaneo, both poles each (one per direction). */
private val musattiPlace = Place("Musatti", ids = setOf("6059", "6060"))
private val cattaneoPlace = Place("Cattaneo", ids = setOf("6057", "6058"))

/**
 * No stop is called "uffici giudiziari" or "tribunale": Hermada (both poles) is the guess; every pole of
 * Forte Marghera is served by the lines 15 (station + airport) and, from Hermada on, 43.
 */
private val forteMarghera = Place("Forte Marghera", ids = setOf("172", "1172"))

/** Ospedale dell'Angelo (Mestre), the two bus poles: the SFMR train stops next to it are not part of it. */
private val ospedale = Place("Ospedale", ids = setOf("1400", "1401"))

private fun destinationsFrom(home: Place, extra: List<Destination> = emptyList()) = listOf(
    Destination(
        id = "venezia",
        label = "Venezia",
        origin = home,
        target = Place("Venezia", exactNames = setOf("VENEZIA", "VENEZIA CORSIA C", "Piazzale Roma")),
    ),
    Destination(
        id = "centro",
        label = "Centro",
        origin = home,
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
) + extra

/**
 * The app never ships the whole network: only the lines that directly link these places.
 * Shared by the importer tool (bundled asset) and the app (updates downloaded on the phone): edit it and regenerate
 * the asset with `./gradlew :tools:importer:run`.
 */
object TimetableConfig {
    /** Bump when the shape of the generated timetable changes: older downloaded files are then discarded. */
    private const val SCHEMA = 3

    const val FEED_URL = "https://actv.avmspa.it/sites/default/files/attachments/opendata/automobilistico/actv_aut.zip"

    /** One app flavor: which stop the first two destinations leave from (cattaneo adds the hospital). Its bundled files are `app/src/<id>/assets`. */
    enum class Profile(val id: String, internal val destinations: List<Destination>) {
        MUSATTI("musatti", destinationsFrom(musattiPlace)),
        // Only the 31H and 32H (Mestre loops) link Cattaneo and the hospital without changing; Vespucci Cattaneo has no direct line.
        CATTANEO("cattaneo", destinationsFrom(cattaneoPlace, listOf(Destination("ospedale", "Ospedale", cattaneoPlace, ospedale)))),
    }

    /** The app picks its flavor's profile at start (`BuildConfig.PROFILE`); the importer takes `--profile=<id>`. */
    @Volatile
    var profile: Profile = Profile.MUSATTI
        private set

    fun select(id: String) {
        profile = Profile.entries.firstOrNull { it.id == id } ?: error("Unknown profile $id")
    }

    val destinations: List<Destination>
        get() = profile.destinations

    /**
     * Identifies the destinations of the selected profile. Stored in every generated timetable: a timetable built (and downloaded)
     * by an older version of the app with other destinations must not be used.
     */
    val fingerprint: String
        get() = ("schema$SCHEMA;" + destinations.joinToString(";") { "${it.id}:${it.label}:${it.origin.describe()}:${it.target.describe()}" }
            ).hashCode().toUInt().toString(16)
}
