package com.ilsecondodasinistra.catchit.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface UpdaterEntryPoint {
    fun updater(): TimetableUpdater
}

/** The daily check, run by the system when it suits it, on an unmetered network (Wi-Fi) and not on a low battery. */
class TimetableUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val updater = EntryPointAccessors.fromApplication(applicationContext, UpdaterEntryPoint::class.java).updater()
        return when (updater.check()) {
            UpdateOutcome.NetworkError -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        private const val NAME = "timetable-update"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TimetableUpdateWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .build()
            // KEEP: re-enqueuing at every start must not push the next run back.
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
