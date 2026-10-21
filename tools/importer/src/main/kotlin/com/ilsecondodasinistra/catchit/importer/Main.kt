package com.ilsecondodasinistra.catchit.importer

import java.io.File

fun main(args: Array<String>) {
    val refresh = "--refresh" in args
    val out = File(ImporterConfig.OUTPUT_FILE)
    Feed.open(File(ImporterConfig.CACHE_FILE), ImporterConfig.FEED_URL, refresh).use { feed ->
        val timetable = buildTimetable(feed, ImporterConfig.destinations)
        out.parentFile.mkdirs()
        out.writeText(prettyJson.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), timetable))
    }
    println("Wrote ${out.path} (${out.length() / 1024} KB)")
}
