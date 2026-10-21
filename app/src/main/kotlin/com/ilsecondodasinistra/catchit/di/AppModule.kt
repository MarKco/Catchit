package com.ilsecondodasinistra.catchit.di

import com.ilsecondodasinistra.catchit.data.AvmRealtimeSource
import com.ilsecondodasinistra.catchit.data.RealtimeSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun realtimeSource(impl: AvmRealtimeSource): RealtimeSource

    companion object {
        /** Service days are Venice's: the timetable must not follow the phone's time zone. */
        @Provides
        @Singleton
        fun clock(): Clock = Clock.system(ZoneId.of("Europe/Rome"))
    }
}
