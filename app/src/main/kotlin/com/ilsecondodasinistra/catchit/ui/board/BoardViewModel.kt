package com.ilsecondodasinistra.catchit.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.catchit.data.RealtimeSource
import com.ilsecondodasinistra.catchit.data.RealtimeUpdate
import com.ilsecondodasinistra.catchit.data.Timetable
import com.ilsecondodasinistra.catchit.data.TimetableSource
import com.ilsecondodasinistra.catchit.data.TripUpdatesParser
import com.ilsecondodasinistra.catchit.domain.Departure
import com.ilsecondodasinistra.catchit.domain.DepartureMerger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class StopBoard(val stopId: String, val label: String, val departures: List<Departure>)

data class BoardUiState(
    val isLoading: Boolean = true,
    val stopName: String = "",
    val boards: List<StopBoard> = emptyList(),
    val now: Instant = Instant.EPOCH,
    /** Last successful realtime fetch. */
    val lastUpdate: Instant? = null,
    val realtimeFailed: Boolean = false,
    val isRefreshing: Boolean = false,
    /** Set while the bundled timetable doesn't cover today yet: only live passages exist until then. */
    val scheduleStartsOn: LocalDate? = null,
)

private data class LiveState(
    val updates: List<RealtimeUpdate> = emptyList(),
    val lastUpdate: Instant? = null,
    val failed: Boolean = false,
    val refreshing: Boolean = false,
)

@HiltViewModel
class BoardViewModel @Inject constructor(
    timetableSource: TimetableSource,
    private val realtime: RealtimeSource,
    private val clock: Clock,
) : ViewModel() {
    private val timetable = MutableStateFlow<Timetable?>(null)
    private val live = MutableStateFlow(LiveState())
    private val refreshLock = Mutex()

    /** Re-evaluated every few seconds so countdowns and the "next" list keep moving between fetches. */
    private val ticker = flow {
        while (true) {
            emit(clock.instant())
            delay(TICK)
        }
    }

    val state: StateFlow<BoardUiState> = combine(timetable, live, ticker) { tt, live, now ->
        if (tt == null) BoardUiState(isLoading = true) else buildState(tt, live, now)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BoardUiState())

    init {
        viewModelScope.launch { timetable.value = timetableSource.load() }
    }

    fun requestRefresh() {
        viewModelScope.launch { refresh() }
    }

    /** One realtime fetch; overlapping calls (poll + pull-to-refresh) are serialised. */
    suspend fun refresh() = refreshLock.withLock {
        val tt = timetable.filterNotNull().first()
        live.update { it.copy(refreshing = true) }
        try {
            val updates = withContext(Dispatchers.Default) {
                TripUpdatesParser.parse(realtime.fetchTripUpdates(), tt.stops.mapTo(HashSet()) { it.id })
            }
            live.value = LiveState(updates, clock.instant(), failed = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep the last good predictions (they age out by themselves) and say the feed is unreachable.
            live.update { it.copy(failed = true, refreshing = false) }
        }
    }

    private fun buildState(tt: Timetable, live: LiveState, now: Instant): BoardUiState {
        val zone = clock.zone
        val boards = tt.stops.map { stop ->
            val scheduled = tt.scheduled(stop.id, now.minus(LOOKBACK), now.plus(HORIZON), zone)
            val departures = DepartureMerger.merge(stop.id, tt, scheduled, live.updates, now, LIMIT, HORIZON)
            StopBoard(stop.id, stop.label, departures)
        }
        val today = now.atZone(zone).toLocalDate()
        return BoardUiState(
            isLoading = false,
            stopName = tt.stops.firstOrNull()?.name.orEmpty(),
            boards = boards,
            now = now,
            lastUpdate = live.lastUpdate,
            realtimeFailed = live.failed,
            isRefreshing = live.refreshing,
            scheduleStartsOn = tt.validFrom?.takeIf { today < it },
        )
    }

    private companion object {
        val TICK: Long = 10_000
        // Wider than the visible past: a late tram is matched to a timetable row that is already behind it.
        val LOOKBACK: Duration = Duration.ofMinutes(60)
        val HORIZON: Duration = DepartureMerger.DEFAULT_HORIZON
        const val LIMIT = 150 // a tram every ~6 min for 6 h is ~60 rows per pole
    }
}
