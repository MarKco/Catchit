package com.ilsecondodasinistra.catchit.importer

import java.io.BufferedReader

/** Minimal RFC 4180 reader: GTFS headsigns can be quoted and contain commas. */
internal fun splitCsvLine(line: String): List<String> {
    val out = ArrayList<String>(32)
    val sb = StringBuilder()
    var quoted = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            quoted && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
            c == '"' -> quoted = !quoted
            c == ',' && !quoted -> { out.add(sb.toString()); sb.setLength(0) }
            else -> sb.append(c)
        }
        i++
    }
    out.add(sb.toString())
    return out
}

internal class CsvRow(private val header: Map<String, Int>, private val cells: List<String>) {
    operator fun get(column: String): String = header[column]?.let { cells.getOrNull(it) }.orEmpty()
}

/** Streams a CSV with a header row; skips blank lines and the UTF-8 BOM. */
internal fun BufferedReader.csvRows(): Sequence<CsvRow> = sequence {
    val first = readLine()?.removePrefix("﻿") ?: return@sequence
    val header = splitCsvLine(first).withIndex().associate { (i, name) -> name.trim() to i }
    while (true) {
        val line = readLine() ?: break
        if (line.isBlank()) continue
        yield(CsvRow(header, splitCsvLine(line)))
    }
}
