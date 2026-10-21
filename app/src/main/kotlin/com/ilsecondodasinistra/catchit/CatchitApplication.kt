package com.ilsecondodasinistra.catchit

import android.app.Application
import com.ilsecondodasinistra.catchit.update.TimetableUpdateWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CatchitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TimetableUpdateWorker.schedule(this)
    }
}
