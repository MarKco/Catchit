package com.ilsecondodasinistra.catchit.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimetableSource @Inject constructor(@ApplicationContext private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): Timetable = withContext(Dispatchers.IO) {
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        Timetable(json.decodeFromString(TimetableData.serializer(), text))
    }

    private companion object {
        const val ASSET = "timetable.json"
    }
}
