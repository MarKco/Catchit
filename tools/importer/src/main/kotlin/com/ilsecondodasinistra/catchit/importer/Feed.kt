package com.ilsecondodasinistra.catchit.importer

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URI
import java.util.zip.ZipFile

/** Downloads the AVM feed (cached) and opens its files one at a time. */
internal class Feed(private val zip: ZipFile) : AutoCloseable {
    fun <T> read(name: String, block: (Sequence<CsvRow>) -> T): T {
        val entry = zip.getEntry(name) ?: error("$name missing from the feed")
        return zip.getInputStream(entry).use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 1 shl 16).use { block(it.csvRows()) }
        }
    }

    fun has(name: String): Boolean = zip.getEntry(name) != null

    override fun close() = zip.close()

    companion object {
        fun open(cache: File, url: String, refresh: Boolean): Feed {
            if (refresh || !cache.exists()) {
                cache.parentFile.mkdirs()
                println("Downloading $url")
                val tmp = File(cache.path + ".part")
                // URL.openStream follows http→https redirects only for the same protocol: the feed is https.
                URI(url).toURL().openStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
                tmp.renameTo(cache)
            } else {
                println("Using cached ${cache.path} (pass --refresh to download again)")
            }
            return Feed(ZipFile(cache))
        }
    }
}
