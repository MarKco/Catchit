package com.ilsecondodasinistra.catchit.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

enum class LogKind(val tag: String) {
    /** Timetable checks, downloads and installs. */
    Timetable("orari"),

    /** Errors of the realtime feed. */
    Live("live"),
}

enum class LogLevel { Info, Warning, Error }

@Serializable
data class LogEntry(
    val epochMillis: Long,
    val kind: LogKind,
    val level: LogLevel,
    val message: String,
    /** Condensed stack trace of an error, for pasting into a bug report. */
    val detail: String? = null,
) {
    val time: Instant get() = Instant.ofEpochMilli(epochMillis)
}

/**
 * The last [CAPACITY] things the app did about timetables and the realtime feed, kept on the phone so that
 * a problem can be reported afterwards (Settings → copy / share).
 */
@Singleton
class UpdateLog @Inject constructor(@ApplicationContext context: Context) {
    private val file = File(context.filesDir, FILE)
    private val json = Json { ignoreUnknownKeys = true }
    private val _entries = MutableStateFlow(load())

    /** Newest last. */
    val entries: StateFlow<List<LogEntry>> = _entries

    fun info(kind: LogKind, message: String) = add(LogEntry(System.currentTimeMillis(), kind, LogLevel.Info, message))

    fun warning(kind: LogKind, message: String) = add(LogEntry(System.currentTimeMillis(), kind, LogLevel.Warning, message))

    fun error(kind: LogKind, message: String, cause: Throwable? = null) =
        add(LogEntry(System.currentTimeMillis(), kind, LogLevel.Error, message, cause?.let(::condensedTrace)))

    fun clear() = synchronized(this) {
        _entries.value = emptyList()
        file.delete()
    }

    private fun add(entry: LogEntry) = synchronized(this) {
        val all = (_entries.value + entry).takeLast(CAPACITY)
        _entries.value = all
        runCatching { file.writeText(all.joinToString("\n") { json.encodeToString(LogEntry.serializer(), it) }) }
    }

    private fun load(): List<LogEntry> = runCatching {
        if (!file.exists()) emptyList()
        else file.readLines().filter { it.isNotBlank() }.mapNotNull { runCatching { json.decodeFromString(LogEntry.serializer(), it) }.getOrNull() }.takeLast(CAPACITY)
    }.getOrDefault(emptyList())

    companion object {
        const val CAPACITY = 50
        private const val FILE = "update-log.jsonl"
        private const val TRACE_LINES = 12

        private val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Europe/Rome"))

        fun condensedTrace(cause: Throwable): String {
            val writer = StringWriter()
            cause.printStackTrace(PrintWriter(writer))
            return writer.toString().lineSequence().filter { it.isNotBlank() }.take(TRACE_LINES).joinToString("\n")
        }

        /** The text people copy or share: [header] lines, then the entries oldest first, errors with their trace. */
        fun format(header: List<String>, entries: List<LogEntry>): String = buildString {
            header.forEach { appendLine(it) }
            appendLine()
            if (entries.isEmpty()) appendLine("(registro vuoto)")
            for (e in entries) {
                appendLine("${timestamp.format(e.time)} [${e.kind.tag}] ${e.level.name.uppercase()} ${e.message}")
                e.detail?.lineSequence()?.forEach { appendLine("    $it") }
            }
        }.trimEnd()
    }
}
