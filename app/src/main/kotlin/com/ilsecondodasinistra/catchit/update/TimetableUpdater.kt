package com.ilsecondodasinistra.catchit.update

import android.content.Context
import android.net.ConnectivityManager
import android.os.Process
import com.ilsecondodasinistra.catchit.data.LastCheck
import com.ilsecondodasinistra.catchit.data.LogKind
import com.ilsecondodasinistra.catchit.data.TimetableRepository
import com.ilsecondodasinistra.catchit.data.UpdateLog
import com.ilsecondodasinistra.catchit.timetable.TimetableBuilder
import com.ilsecondodasinistra.catchit.timetable.TimetableConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

enum class UpdateOutcome(val label: String) {
    /** AVM has nothing newer than what the app already processed. */
    UpToDate("già aggiornato"),

    /** A new timetable was built and installed. */
    Updated("aggiornato"),

    /** There is a new zip, but the network is metered: the daily Wi-Fi job will fetch it. */
    Deferred("rimandato (rete a consumo)"),

    /** AVM unreachable or the download broke: worth retrying soon. */
    NetworkError("errore di rete"),

    /** The zip downloaded but did not yield a usable timetable: not retried until AVM publishes another one. */
    BuildError("errore nella costruzione"),
}

/** Why a check runs: shown in the log and the settings page. */
enum class UpdateTrigger(val label: String) {
    Start("avvio app"),
    Daily("controllo giornaliero"),
    Manual("manuale"),
}

/**
 * Keeps the timetable current: asks AVM whether `actv_aut.zip` changed (a cheap HEAD request), and only then
 * downloads the 14 MB zip, cuts it down to our destinations with the same builder as the bundled asset, and
 * installs the result. Everything runs off the main thread, at background priority; the board keeps using the
 * timetable it has until the new one is installed. Every step is written to the [UpdateLog].
 */
@Singleton
class TimetableUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TimetableRepository,
    private val log: UpdateLog,
    private val clock: Clock,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    /** One thread at background priority: parsing 55 MB of GTFS must not compete with the UI. */
    private val worker = Executors.newSingleThreadExecutor { r ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            r.run()
        }, "timetable-update").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    /** Check when the app opens, after the first screen is up. Fire and forget. */
    fun checkSoon() {
        scope.launch {
            delay(START_DELAY_MS)
            check(UpdateTrigger.Start)
        }
    }

    /** True when the connection is metered (mobile data): a download should ask first. */
    fun isMetered(): Boolean =
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered

    /**
     * @param allowMetered download even on mobile data (the user said yes).
     * @param force rebuild even when AVM's zip is the one already processed.
     */
    suspend fun check(trigger: UpdateTrigger, allowMetered: Boolean = false, force: Boolean = false): UpdateOutcome =
        mutex.withLock {
            val outcome = run(trigger, allowMetered, force)
            log.info(LogKind.Timetable, "Esito: ${outcome.label}")
            repository.lastCheck = LastCheck(System.currentTimeMillis(), trigger.label, outcome.label)
            outcome
        }

    private suspend fun run(trigger: UpdateTrigger, allowMetered: Boolean, force: Boolean): UpdateOutcome {
        log.info(LogKind.Timetable, "Controllo orari AVM (${trigger.label}${if (force) ", forzato" else ""})")
        val stamp = try {
            fetchStamp()
        } catch (e: java.io.IOException) {
            log.error(LogKind.Timetable, "AVM non raggiungibile", e)
            return UpdateOutcome.NetworkError
        }
        log.info(LogKind.Timetable, "Zip AVM: ${stamp.substringAfter('|')}")
        if (!force) {
            if (stamp == repository.processedStamp) return UpdateOutcome.UpToDate
            if (stamp == repository.failedStamp) {
                log.warning(LogKind.Timetable, "Questo zip ha già dato errore: aspetto che AVM ne pubblichi un altro")
                return UpdateOutcome.BuildError
            }
        }
        if (!allowMetered && isMetered()) {
            log.warning(LogKind.Timetable, "Rete a consumo: scarico rimandato al Wi-Fi")
            return UpdateOutcome.Deferred
        }

        val zip = File(context.cacheDir, ZIP_NAME)
        val downloadStart = System.nanoTime()
        try {
            withContext(worker) { download(zip) }
        } catch (e: java.io.IOException) {
            zip.delete()
            log.error(LogKind.Timetable, "Download interrotto", e)
            return UpdateOutcome.NetworkError
        }
        log.info(LogKind.Timetable, "Scaricati ${zip.length() / 1024} KB in ${millisSince(downloadStart)} ms")

        val buildStart = System.nanoTime()
        return try {
            val json = withContext(worker) { TimetableBuilder.build(zip) }
            log.info(LogKind.Timetable, "Orari costruiti in ${millisSince(buildStart)} ms (${json.length / 1024} KB)")
            repository.install(json, LocalDate.now(clock))
            repository.processedStamp = stamp
            repository.failedStamp = null
            val versions = repository.versions.value.joinToString { "${it.feedStart}→${it.feedEnd}" }
            log.info(LogKind.Timetable, "Installato. Versioni in uso: $versions")
            UpdateOutcome.Updated
        } catch (e: Exception) {
            repository.failedStamp = stamp
            log.error(LogKind.Timetable, "Impossibile costruire gli orari da questo zip", e)
            UpdateOutcome.BuildError
        } finally {
            zip.delete()
        }
    }

    private fun millisSince(startNanos: Long) = (System.nanoTime() - startNanos) / 1_000_000

    /** What identifies the published zip: the destinations we build for plus ETag/Last-Modified/size. */
    private suspend fun fetchStamp(): String = withContext(Dispatchers.IO) {
        val connection = open("HEAD")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw java.io.IOException("AVM ha risposto ${connection.responseCode}")
            listOf(
                TimetableConfig.fingerprint,
                connection.getHeaderField("ETag").orEmpty(),
                connection.getHeaderField("Last-Modified").orEmpty(),
                connection.contentLengthLong.toString(),
            ).joinToString("|")
        } finally {
            connection.disconnect()
        }
    }

    private fun download(target: File) {
        val connection = open("GET")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw java.io.IOException("AVM ha risposto ${connection.responseCode}")
            val expected = connection.contentLengthLong
            val tmp = File(target.path + ".part")
            connection.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (expected > 0 && tmp.length() != expected) throw java.io.IOException("Download incompleto (${tmp.length()} di $expected byte)")
            if (!tmp.renameTo(target)) throw java.io.IOException("Impossibile salvare il download")
        } finally {
            connection.disconnect()
        }
    }

    private fun open(method: String): HttpURLConnection =
        (URL(TimetableConfig.FEED_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }

    private companion object {
        const val ZIP_NAME = "actv_aut.zip"
        const val START_DELAY_MS = 5_000L
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
    }
}
