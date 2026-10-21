package com.ilsecondodasinistra.catchit.importer

import com.ilsecondodasinistra.catchit.timetable.TimetableBuilder
import com.ilsecondodasinistra.catchit.timetable.TimetableConfig
import java.io.File
import java.net.URI

private const val CACHE_FILE = "tools/cache/actv_aut.zip"
private const val OUTPUT_FILE = "app/src/main/assets/timetable.json"

/**
 * Regenerates the timetable bundled in the APK (the fallback of the app's own updates).
 * `./gradlew :tools:importer:run [--args=--refresh]`
 */
fun main(args: Array<String>) {
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
    val out = File(OUTPUT_FILE)
    out.parentFile.mkdirs()
    out.writeText(TimetableBuilder.build(cache, TimetableConfig.destinations, ::println))
    println("Wrote ${out.path} (${out.length() / 1024} KB)")
}
