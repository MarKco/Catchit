package com.ilsecondodasinistra.catchit.importer

import com.ilsecondodasinistra.catchit.timetable.TimetableBuilder
import com.ilsecondodasinistra.catchit.timetable.TimetableConfig
import java.io.File
import java.net.URI

private const val CACHE_FILE = "tools/cache/actv_aut.zip"
private const val DEFAULT_PROFILE = "musatti"

/**
 * Regenerates the timetable and the map data (paths, whole runs) bundled in the APK (the fallback of the app's own updates).
 * `./gradlew :tools:importer:run [--args="--refresh --profile=cattaneo"]`: the profile (default musatti) is the app flavor, whose
 * assets are written to `app/src/<profile>/assets`.
 */
fun main(args: Array<String>) {
    val profile = args.firstOrNull { it.startsWith("--profile=") }?.substringAfter('=') ?: DEFAULT_PROFILE
    TimetableConfig.select(profile)
    val assets = File("app/src/$profile/assets")
    val cache = File(CACHE_FILE)
    if ("--refresh" in args || !cache.exists()) {
        cache.parentFile.mkdirs()
        println("Downloading ${TimetableConfig.FEED_URL}")
        val tmp = File(cache.path + ".part")
        URI(TimetableConfig.FEED_URL).toURL().openStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
        tmp.renameTo(cache)
    } else {
        println("Using cached ${cache.path} (pass --refresh to download again)")
    }
    val out = File(assets, "timetable.json")
    out.parentFile.mkdirs()
    val built = TimetableBuilder.buildFiles(cache, TimetableConfig.destinations, ::println)
    out.writeText(built.timetable)
    println("Wrote ${out.path} (${out.length() / 1024} KB)")
    val geo = File(assets, "geo.json")
    geo.writeText(built.geo)
    println("Wrote ${geo.path} (${geo.length() / 1024} KB)")
}
