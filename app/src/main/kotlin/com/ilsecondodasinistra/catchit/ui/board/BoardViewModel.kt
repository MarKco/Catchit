package com.ilsecondodasinistra.catchit.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.catchit.data.Leg
import com.ilsecondodasinistra.catchit.data.RealtimeSource
import com.ilsecondodasinistra.catchit.data.RealtimeUpdate
import com.ilsecondodasinistra.catchit.data.Timetable
import com.ilsecondodasinistra.catchit.data.TimetableSource
import com.ilsecondodasinistra.catchit.data.TripUpdatesParser
import com.ilsecondodasinistra.catchit.domain.Departure
import com.ilsecondodasinistra.catchit.domain.DepartureMerger
import com.ilsecondodasinistra.catchit.domain.isPast
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
import java.time.ZoneId
import javax.inject.Inject

/** One tab: the departures of a [Leg] of a destination, ending at [towards]. */
data class LegBoard(val leg: Leg, val towards: String, val departures: List<Departure>)

/** One bottom-bar entry: [origin] ⇄ [target], with its two tabs. */
data class DestinationBoard(
    val id: String,
    val label: String,
    val origin: String,
    val target: String,
    val legs: List<LegBoard>,
)

data class BoardUiState(
    val isLoading: Boolean = true,
    val destinations: List<DestinationBoard> = emptyList(),
    val now: Instant = Instant.EPOCH,
    /** Last successful realtime fetch. */
    val lastUpdate: Instant? = null,
    val realtimeFailed: Boolean = false,
    val isRefreshing: Boolean = false,
    /** Set while the bundled timetable doesn't cover today yet: timetable rows are indicative until then. */
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
                TripUpdatesParser.parse(realtime.fetchTripUpdates(), tt.stopIds)
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
        val destinations = tt.destinations.map { d ->
            val legs = Leg.entries.map { leg ->
                val departures = departures(tt, d.id, leg, live, now, zone)
                LegBoard(leg, towards = if (leg == Leg.Outbound) d.target else d.origin, departures = departures)
            }
            DestinationBoard(d.id, d.label, d.origin, d.target, legs)
        }
        val today = now.atZone(zone).toLocalDate()
        return BoardUiState(
            isLoading = false,
            destinations = destinations,
            now = now,
            lastUpdate = live.lastUpdate,
            realtimeFailed = live.failed,
            isRefreshing = live.refreshing,
            scheduleStartsOn = tt.validFrom?.takeIf { today < it },
        )
    }

    /**
     * 6 h ahead; when that leaves fewer than [MIN_UPCOMING] passages (a line that stops for the night) look into
     * tomorrow until there are enough.
     */
    private fun departures(tt: Timetable, destinationId: String, leg: Leg, live: LiveState, now: Instant, zone: ZoneId): List<Departure> {
        var result = emptyList<Departure>()
        for (horizon in HORIZONS) {
            val scheduled = tt.scheduled(destinationId, leg, now.minus(LOOKBACK), now.plus(horizon), zone)
            result = DepartureMerger.merge(destinationId, leg, tt, scheduled, live.updates, now, LIMIT, horizon)
            if (result.count { !it.isPast(now) } >= MIN_UPCOMING) break
        }
        return result
    }

    private companion object {
        val TICK: Long = 10_000
        // Wider than the visible past: a late tram is matched to a timetable row that is already behind it.
        val LOOKBACK: Duration = Duration.ofMinutes(60)
        val HORIZONS: List<Duration> = listOf(DepartureMerger.DEFAULT_HORIZON, Duration.ofHours(30))
        const val MIN_UPCOMING = 5
        const val LIMIT = 150 // a tram every ~6 min for 6 h is ~60 rows per pole
    }
}
