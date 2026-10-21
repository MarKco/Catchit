package com.ilsecondodasinistra.catchit.data

import android.content.Context
import com.ilsecondodasinistra.catchit.timetable.TimetableConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The timetables the app knows: the one bundled in the APK (always there, the fallback) plus those downloaded
 * by [com.ilsecondodasinistra.catchit.update.TimetableUpdater], one file per start date.
 */
@Singleton
class TimetableRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val directory = File(context.filesDir, "timetables")
    private val prefs = context.getSharedPreferences("timetable_updates", Context.MODE_PRIVATE)
    private val _timetable = MutableStateFlow<Timetable?>(null)

    /** Null until the first [load]; a new value whenever an update is installed. */
    val timetable: StateFlow<Timetable?> = _timetable

    /** Identifies the AVM zip last turned into a timetable (and the destinations it was built for). */
    var processedStamp: String?
        get() = prefs.getString(KEY_PROCESSED, null)
        set(value) = prefs.edit().putString(KEY_PROCESSED, value).apply()

    /** The zip that could not be turned into a timetable: not retried until AVM publishes another one. */
    var failedStamp: String?
        get() = prefs.getString(KEY_FAILED, null)
        set(value) = prefs.edit().putString(KEY_FAILED, value).apply()

    suspend fun load() = withContext(Dispatchers.IO) { _timetable.value = read() }

    /**
     * Validates [jsonText] and stores it as the timetable starting on its `feedStart`. Throws when it is not a
     * usable timetable for the current destinations, so a bad download never replaces anything.
     */
    suspend fun install(jsonText: String, today: LocalDate) = withContext(Dispatchers.IO) {
        val data = decode(jsonText)
        require(data.config == TimetableConfig.fingerprint) { "Built for other destinations" }
        require(data.patterns.isNotEmpty() && data.destinations.isNotEmpty()) { "Empty timetable" }
        requireNotNull(data.feedStart.toDateOrNull()) { "No start date" }

        directory.mkdirs()
        val target = File(directory, "$PREFIX${data.feedStart}$SUFFIX")
        val tmp = File(directory, target.name + ".part")
        tmp.writeText(jsonText)
        check(tmp.renameTo(target)) { "Could not install ${target.name}" }
        prune(today)
        _timetable.value = read()
    }

    private fun read(): Timetable {
        val bundled = decode(context.assets.open(ASSET).bufferedReader().use { it.readText() })
        // One version per start date; a downloaded one is a republication of the bundled one: it wins.
        val byStart = LinkedHashMap<String, TimetableData>()
        byStart[bundled.feedStart] = bundled
        for (file in files()) {
            val data = runCatching { decode(file.readText()) }.getOrNull()
            // A file from an older app version (other destinations) or a damaged one is useless: drop it.
            if (data == null || data.config != TimetableConfig.fingerprint) file.delete() else byStart[data.feedStart] = data
        }
        return Timetable(byStart.values.toList())
    }

    private fun prune(today: LocalDate) {
        val dated = files().mapNotNull { f -> f.startDate()?.let { it to f } }.toMap().entries.associate { (k, v) -> v to k }
        val doomed = SnapshotPolicy.obsolete(dated.values.toList(), today)
        dated.filter { it.value in doomed }.keys.forEach { it.delete() }
    }

    private fun files(): List<File> =
        directory.listFiles { f -> f.name.startsWith(PREFIX) && f.name.endsWith(SUFFIX) }?.toList().orEmpty()

    private fun File.startDate(): LocalDate? = name.removePrefix(PREFIX).removeSuffix(SUFFIX).toDateOrNull()

    private fun decode(text: String): TimetableData = json.decodeFromString(TimetableData.serializer(), text)

    private fun String.toDateOrNull(): LocalDate? =
        runCatching { LocalDate.parse(this, DateTimeFormatter.BASIC_ISO_DATE) }.getOrNull()

    private companion object {
        const val ASSET = "timetable.json"
        const val PREFIX = "timetable-"
        const val SUFFIX = ".json"
        const val KEY_PROCESSED = "processed"
        const val KEY_FAILED = "failed"
    }
}

object SnapshotPolicy {
    /** Timetables already superseded by one in force [today]; the one in force and the upcoming ones are kept. */
    fun obsolete(starts: List<LocalDate>, today: LocalDate): Set<LocalDate> {
        val inForce = starts.filter { it <= today }.maxOrNull() ?: return emptySet()
        return starts.filterTo(HashSet()) { it < inForce }
    }
}
