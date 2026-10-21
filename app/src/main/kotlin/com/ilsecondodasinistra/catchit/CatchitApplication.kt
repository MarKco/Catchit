package com.ilsecondodasinistra.catchit

import android.app.Application
import com.ilsecondodasinistra.catchit.data.TimetableRepository
import com.ilsecondodasinistra.catchit.update.TimetableUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CatchitApplication : Application() {
    @Inject
    lateinit var timetables: TimetableRepository

    override fun onCreate() {
        StartupTrace.mark("Application.onCreate begin")
        super.onCreate()
        // The one thing every start needs: read it while the process and the first screen are still being set up.
        timetables.preload()
        StartupTrace.section("schedule daily update") { TimetableUpdateWorker.schedule(this) }
        StartupTrace.mark("Application.onCreate end")
    }
}
