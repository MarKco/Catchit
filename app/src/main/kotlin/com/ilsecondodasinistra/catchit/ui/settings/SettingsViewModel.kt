package com.ilsecondodasinistra.catchit.ui.settings

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.catchit.data.BarEntry
import com.ilsecondodasinistra.catchit.data.BarPreferences
import com.ilsecondodasinistra.catchit.data.LastCheck
import com.ilsecondodasinistra.catchit.data.LogEntry
import com.ilsecondodasinistra.catchit.data.TimetableRepository
import com.ilsecondodasinistra.catchit.data.UpdateLog
import com.ilsecondodasinistra.catchit.data.VersionInfo
import com.ilsecondodasinistra.catchit.update.TimetableUpdater
import com.ilsecondodasinistra.catchit.update.UpdateOutcome
import com.ilsecondodasinistra.catchit.update.UpdateTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A bottom-bar button as the settings page shows it. */
data class BarRow(val id: String, val label: String, val subtitle: String, val visible: Boolean)

data class SettingsUiState(
    val versions: List<VersionInfo> = emptyList(),
    val lines: List<String> = emptyList(),
    val stopCount: Int = 0,
    val lastCheck: LastCheck? = null,
    val bar: List<BarRow> = emptyList(),
    val log: List<LogEntry> = emptyList(),
    val checking: Boolean = false,
    /** Outcome of the check started from this page. */
    val result: String? = null,
    /** A download is pending on mobile data: waiting for the user's yes. */
    val askMetered: Boolean = false,
    val appVersion: String = "",
)

private data class CheckState(val checking: Boolean = false, val result: String? = null, val askMetered: Boolean = false, val force: Boolean = false)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timetables: TimetableRepository,
    private val updater: TimetableUpdater,
    private val bar: BarPreferences,
    private val log: UpdateLog,
) : ViewModel() {
    private val check = MutableStateFlow(CheckState())

    private val appVersion: String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "${info.versionName} (${info.longVersionCode})"
    }.getOrDefault("?")

    val state: StateFlow<SettingsUiState> = combine(
        timetables.timetable,
        timetables.versions,
        timetables.lastCheckFlow,
        combine(bar.saved, log.entries, ::Pair),
        check,
    ) { tt, versions, last, (saved, entries), check ->
        val known = tt?.destinations.orEmpty()
        val labels = known.associateBy { it.id }
        SettingsUiState(
            versions = versions,
            lines = tt?.lineNames.orEmpty(),
            stopCount = tt?.stopIds?.size ?: 0,
            lastCheck = last,
            bar = BarPreferences.resolve(known.map { it.id }, saved).mapNotNull { e ->
                labels[e.id]?.let { BarRow(e.id, it.label, "${it.origin} ⇄ ${it.target}", e.visible) }
            },
            log = entries,
            checking = check.checking,
            result = check.result,
            askMetered = check.askMetered,
            appVersion = appVersion,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(appVersion = appVersion))

    /** Saves the bar as shown: [rows] in their new order with their visibility. */
    fun saveBar(rows: List<BarRow>) = bar.save(rows.map { BarEntry(it.id, it.visible) })

    fun checkNow(force: Boolean = false, allowMetered: Boolean = false) {
        if (check.value.checking) return
        viewModelScope.launch {
            check.value = CheckState(checking = true)
            val outcome = updater.check(UpdateTrigger.Manual, allowMetered = allowMetered, force = force)
            check.value = if (outcome == UpdateOutcome.Deferred) {
                CheckState(askMetered = true, force = force)
            } else {
                CheckState(result = outcome.label)
            }
        }
    }

    fun confirmMetered() {
        val force = check.value.force
        check.update { it.copy(askMetered = false) }
        checkNow(force = force, allowMetered = true)
    }

    fun dismissMetered() = check.update { CheckState(result = UpdateOutcome.Deferred.label) }

    fun clearLog() = log.clear()

    /** The text to copy or share: who is asking (app, Android, device), what the app has, then the log. */
    fun logText(): String {
        val s = state.value
        val header = listOf(
            "Catchit $appVersion",
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}",
            "Orari in uso: " + s.versions.joinToString("; ") { "${it.feedStart}→${it.feedEnd} (${if (it.downloaded) "scaricata" else "inclusa"}, ${it.patterns} corse)" },
            "Linee: ${s.lines.joinToString(" ")}",
            "Ultimo controllo: " + (s.lastCheck?.let { "${it.trigger} → ${it.outcome}" } ?: "mai"),
        )
        return UpdateLog.format(header, s.log)
    }
}
