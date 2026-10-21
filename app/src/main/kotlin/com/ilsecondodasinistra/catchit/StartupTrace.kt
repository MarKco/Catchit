package com.ilsecondodasinistra.catchit

import android.os.Process
import android.os.SystemClock
import android.os.Trace
import android.util.Log

/**
 * Where the start-up time goes. Sections show in Perfetto/System Trace; the log lines are off unless asked for:
 * `adb shell setprop log.tag.CatchitStartup DEBUG`, then `adb logcat -s CatchitStartup`.
 */
object StartupTrace {
    private const val TAG = "CatchitStartup"

    inline fun <T> section(name: String, block: () -> T): T {
        Trace.beginSection(name)
        val start = SystemClock.elapsedRealtime()
        try {
            return block()
        } finally {
            Trace.endSection()
            log("$name took ${SystemClock.elapsedRealtime() - start} ms")
        }
    }

    fun mark(name: String) = log(name)

    fun log(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "+${SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()} ms $message")
        }
    }
}
