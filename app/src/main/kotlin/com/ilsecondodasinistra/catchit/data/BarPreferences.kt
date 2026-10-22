package com.ilsecondodasinistra.catchit.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** One button of the bottom bar. */
data class BarEntry(val id: String, val visible: Boolean)

/** Which destinations the bottom bar shows, and in which order. The first visible one is the start screen. */
@Singleton
class BarPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("bar", Context.MODE_PRIVATE)
    private val _saved = MutableStateFlow(parse(prefs.getString(KEY, null)))

    /** As saved; reconcile with the destinations that exist through [resolve]. */
    val saved: StateFlow<List<BarEntry>> = _saved

    fun save(entries: List<BarEntry>) {
        _saved.value = entries
        prefs.edit().putString(KEY, entries.joinToString(",") { "${it.id}:${if (it.visible) 1 else 0}" }).apply()
    }

    companion object {
        private const val KEY = "entries"

        /** The bottom bar as it comes: Venezia, Centro, Stazione, Aeroporto (and Ospedale, in the cattaneo flavor). */
        val DEFAULT_ORDER = listOf("venezia", "centro", "stazione", "aeroporto", "ospedale")

        internal fun parse(text: String?): List<BarEntry> =
            text.orEmpty().split(',').mapNotNull { part ->
                val (id, flag) = part.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
                BarEntry(id, flag == "1")
            }

        /**
         * Saved order for the destinations that still exist; new ones go last and visible; destinations that
         * disappeared are dropped. At least one stays visible: a bar with no buttons would strand the user.
         */
        fun resolve(known: List<String>, saved: List<BarEntry>): List<BarEntry> {
            val kept = saved.filter { it.id in known }.distinctBy { it.id }
            // Buttons nobody has placed yet follow the default order, whatever order the timetable lists them in.
            val added = known.filter { id -> kept.none { it.id == id } }
                .sortedBy { DEFAULT_ORDER.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }
                .map { BarEntry(it, visible = true) }
            val all = kept + added
            return if (all.isNotEmpty() && all.none { it.visible }) listOf(all.first().copy(visible = true)) + all.drop(1) else all
        }
    }
}
