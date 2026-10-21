package com.ilsecondodasinistra.catchit.update

import android.content.Context
import android.net.ConnectivityManager
import android.os.Process
import com.ilsecondodasinistra.catchit.data.TimetableRepository
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

enum class UpdateOutcome {
    /** AVM has nothing newer than what the app already processed. */
    UpToDate,

    /** A new timetable was built and installed. */
    Updated,

    /** There is a new zip, but the network is metered: the daily Wi-Fi job will fetch it. */
    Deferred,

    /** AVM unreachable or the download broke: worth retrying soon. */
    NetworkError,

    /** The zip downloaded but did not yield a usable timetable: not retried until AVM publishes another one. */
    BuildError,
}

/**
 * Keeps the timetable current: asks AVM whether `actv_aut.zip` changed (a cheap HEAD request), and only then
 * downloads the 14 MB zip, cuts it down to our destinations with the same builder as the bundled asset, and
 * installs the result. Everything runs off the main thread, at background priority; the board keeps using the
 * timetable it has until the new one is installed.
 */
@Singleton
class TimetableUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TimetableRepository,
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
            check()
        }
    }

    suspend fun check(): UpdateOutcome = mutex.withLock {
        val stamp = try {
            fetchStamp()
        } catch (e: java.io.IOException) {
            return UpdateOutcome.NetworkError
        }
        if (stamp == repository.processedStamp) return UpdateOutcome.UpToDate
        if (stamp == repository.failedStamp) return UpdateOutcome.BuildError
        if (isMetered()) return UpdateOutcome.Deferred

        val zip = File(context.cacheDir, ZIP_NAME)
        try {
            withContext(worker) {
                download(zip)
            }
        } catch (e: java.io.IOException) {
            zip.delete()
            return UpdateOutcome.NetworkError
        }
        try {
            val json = withContext(worker) { TimetableBuilder.build(zip) }
            repository.install(json, LocalDate.now(clock))
            repository.processedStamp = stamp
            repository.failedStamp = null
            UpdateOutcome.Updated
        } catch (e: Exception) {
            repository.failedStamp = stamp
            UpdateOutcome.BuildError
        } finally {
            zip.delete()
        }
    }

    /** What identifies the published zip: the destinations we build for plus ETag/Last-Modified/size. */
    private suspend fun fetchStamp(): String = withContext(Dispatchers.IO) {
        val connection = open("HEAD")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw java.io.IOException("AVM answered ${connection.responseCode}")
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
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw java.io.IOException("AVM answered ${connection.responseCode}")
            val expected = connection.contentLengthLong
            val tmp = File(target.path + ".part")
            connection.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (expected > 0 && tmp.length() != expected) throw java.io.IOException("Truncated download")
            if (!tmp.renameTo(target)) throw java.io.IOException("Could not store the download")
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

    private fun isMetered(): Boolean =
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered

    private companion object {
        const val ZIP_NAME = "actv_aut.zip"
        const val START_DELAY_MS = 5_000L
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
    }
}
