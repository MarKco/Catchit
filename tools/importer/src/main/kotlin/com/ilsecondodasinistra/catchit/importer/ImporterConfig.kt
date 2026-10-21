package com.ilsecondodasinistra.catchit.importer

/**
 * The app never ships the whole network: only these lines, at these stops.
 * Edit and run `./gradlew :tools:importer:run` to regenerate `app/src/main/assets/timetable.json`.
 */
object ImporterConfig {
    const val FEED_URL = "https://actv.avmspa.it/sites/default/files/attachments/opendata/automobilistico/actv_aut.zip"
    const val CACHE_FILE = "tools/cache/actv_aut.zip"
    const val OUTPUT_FILE = "app/src/main/assets/timetable.json"

    /** `route_short_name`s to keep. */
    val lines = setOf("T1", "31H", "32H", "12L", "N1", "N2")

    /** Stop id → label shown in the tab. Both poles are named "San Marco Musatti" in the feed; labelled after the T1 headsign (31H/32H are loops). */
    val stops = linkedMapOf(
        "6059" to "Tram per Venezia",
        "6060" to "Tram per Favaro",
    )
}
