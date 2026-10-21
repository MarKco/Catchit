package com.ilsecondodasinistra.catchit.timetable

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipFile

/** A GTFS zip, read one file at a time without unpacking it (`stop_times.txt` alone is ~55 MB). */
class GtfsZip(file: File) : AutoCloseable {
    private val zip = ZipFile(file)

    internal fun <T> read(name: String, block: (Sequence<CsvRow>) -> T): T {
        val entry = zip.getEntry(name) ?: error("$name missing from the feed")
        return zip.getInputStream(entry).use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 1 shl 16).use { block(it.csvRows()) }
        }
    }

    fun has(name: String): Boolean = zip.getEntry(name) != null

    override fun close() = zip.close()
}
