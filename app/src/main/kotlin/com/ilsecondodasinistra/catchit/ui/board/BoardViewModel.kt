package com.ilsecondodasinistra.catchit.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.catchit.StartupTrace
import com.ilsecondodasinistra.catchit.data.BarEntry
import com.ilsecondodasinistra.catchit.data.BarPreferences
import com.ilsecondodasinistra.catchit.data.GeoCatalog
import com.ilsecondodasinistra.catchit.data.Leg
import com.ilsecondodasinistra.catchit.data.LogKind
import com.ilsecondodasinistra.catchit.data.UpdateLog
import com.ilsecondodasinistra.catchit.data.RealtimeSource
import com.ilsecondodasinistra.catchit.data.RealtimeUpdate
import com.ilsecondodasinistra.catchit.data.Timetable
import com.ilsecondodasinistra.catchit.data.TimetableRepository
import com.ilsecondodasinistra.catchit.data.TripUpdatesParser
import com.ilsecondodasinistra.catchit.data.VehicleFix
import com.ilsecondodasinistra.catchit.data.VehicleSnapshot
import com.ilsecondodasinistra.catchit.data.VehiclePositionsParser
import com.ilsecondodasinistra.catchit.domain.Departure
import com.ilsecondodasinistra.catchit.domain.DepartureMerger
import com.ilsecondodasinistra.catchit.domain.isPast
import com.ilsecondodasinistra.catchit.shortcuts.ShortcutPublisher
import com.ilsecondodasinistra.catchit.update.TimetableUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

/** What the realtime feed knows about one trip: predicted time at each of its stops and where its vehicle is. */
data class LiveTrip(val stopTimes: Map<String, Instant>, val vehicle: VehicleFix?)

/** Another day picked from the calendar: that whole day's timetable, the list opening at [focus]. */
data class Browsing(val day: LocalDate, val focus: Instant)

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
    /** Null: the board shows now, with live data. */
    val browsing: Browsing? = null,
    val today: LocalDate? = null,
    /** Last day with timetable data: the calendar stops there. */
    val validUntil: LocalDate? = null,
    /** Live details by trip id, for the sheet of a passage. */
    val liveTrips: Map<String, LiveTrip> = emptyMap(),
    /** Stop names and coordinates, for the sheet of a passage. */
    val timetable: Timetable? = null,
    /** Trams the feed places but doesn't say which line they run: the map shows them as yellow dots. */
    val looseTrams: List<VehicleSnapshot> = emptyList(),
) {
    /** At least one passage to show: what the user opened the app for. */
    val hasContent: Boolean get() = !isLoading && destinations.any { d -> d.legs.any { it.departures.isNotEmpty() } }
}

private data class LiveState(
    val updates: List<RealtimeUpdate> = emptyList(),
    val trips: Map<String, LiveTrip> = emptyMap(),
    val looseTrams: List<VehicleSnapshot> = emptyList(),
    val lastUpdate: Instant? = null,
    val failed: Boolean = false,
    val refreshing: Boolean = false,
)

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val timetables: TimetableRepository,
    updater: TimetableUpdater,
    private val log: UpdateLog,
    private val bar: BarPreferences,
    private val realtime: RealtimeSource,
    private val clock: Clock,
    private val shortcuts: ShortcutPublisher,
) : ViewModel() {
    private val live = MutableStateFlow(LiveState())
    private val refreshLock = Mutex()

    /** The instant picked in the calendar, null = now. */
    private val selection = MutableStateFlow<Instant?>(null)

    /** Re-evaluated every few seconds so countdowns and the "next" list keep moving between fetches. */
    private val ticker = flow {
        while (true) {
            emit(clock.instant())
            delay(TICK)
        }
    }

    val state: StateFlow<BoardUiState> = combine(timetables.timetable, live, ticker, selection, bar.saved) { tt, live, now, picked, saved ->
        if (tt == null) BoardUiState(isLoading = true) else StartupTrace.section("buildState") { buildState(tt, live, now, picked, saved) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BoardUiState())

    /** A destination asked for from outside (a launcher shortcut): the screen selects it and says when it has. */
    private val _requestedDestination = MutableStateFlow<String?>(null)
    val requestedDestination: StateFlow<String?> = _requestedDestination

    fun openDestination(id: String?) {
        _requestedDestination.value = id
    }

    fun destinationOpened() {
        _requestedDestination.value = null
    }

    init {
        StartupTrace.mark("BoardViewModel init")
        viewModelScope.launch { timetables.ensureLoaded() }
        // The launcher shortcuts follow the bottom bar; not urgent, so not while the app is starting.
        viewModelScope.launch(Dispatchers.Default) {
            delay(SHORTCUTS_DELAY_MS)
            combine(timetables.timetable.filterNotNull(), bar.saved) { tt, saved ->
                val known = tt.destinations.associateBy { it.id }
                BarPreferences.resolve(tt.destinations.map { it.id }, saved).filter { it.visible }.mapNotNull { known[it.id] }
            }.distinctUntilChanged().collect { shortcuts.publish(it) }
        }
        // In the background, a few seconds after the first screen: a newer AVM timetable swaps itself in.
        updater.checkSoon()
    }

    /** Paths and whole runs for the map, read from disk the first time it is opened. */
    suspend fun geo(): GeoCatalog = timetables.geo()

    /** Show the timetable of the day of [instant] (no live data), the lists opening at that time. */
    fun browse(instant: Instant) {
        selection.value = instant
    }

    /** Back to now, with live data. */
    fun backToToday() {
        selection.value = null
        requestRefresh()
    }

    fun requestRefresh() {
        viewModelScope.launch { refresh() }
    }

    /** One realtime fetch; overlapping calls (poll + pull-to-refresh) are serialised. */
    suspend fun refresh() = refreshLock.withLock {
        val tt = timetables.timetable.filterNotNull().first()
        live.update { it.copy(refreshing = true) }
        try {
            val parsed = withContext(Dispatchers.Default) {
                val bytes = realtime.fetchTripUpdates()
                val updates = TripUpdatesParser.parse(bytes, tt.stopIds)
                val tripIds = updates.mapTo(HashSet()) { it.tripId }
                updates to TripUpdatesParser.tripStopTimes(bytes, tripIds)
            }
            val (updates, stopTimes) = parsed
            // Where the vehicles are is a bonus: the board works without it, and its failure is not "no live data".
            val snapshots = try {
                VehiclePositionsParser.parseAll(realtime.fetchVehiclePositions())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            val vehicles = snapshots.filter { it.tripId in stopTimes }.associate { it.tripId to it.fix }
            val trips = stopTimes.mapValues { (id, times) ->
                LiveTrip(times.mapValues { Instant.ofEpochSecond(it.value) }, vehicles[id])
            }
            // A tram with no line can't be placed on a leg, but the map can still say "something is there".
            val looseTrams = snapshots.filter { it.isTram && it.routeId.isEmpty() }
            if (live.value.failed) log.info(LogKind.Live, "Tempo reale di nuovo disponibile")
            live.value = LiveState(updates, trips, looseTrams, clock.instant(), failed = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep the last good predictions (they age out by themselves) and say the feed is unreachable.
            // Only the first of a run of failures is logged: the poll repeats every 20 s.
            if (!live.value.failed) log.error(LogKind.Live, "Tempo reale non disponibile", e)
            live.update { it.copy(failed = true, refreshing = false) }
        }
    }

    private fun buildState(tt: Timetable, live: LiveState, now: Instant, picked: Instant?, saved: List<BarEntry>): BoardUiState {
        val zone = clock.zone
        val today = now.atZone(zone).toLocalDate()
        // A day picked yesterday and left open overnight is just "today" again.
        val focus = picked?.takeIf { !it.atZone(zone).toLocalDate().isBefore(today) }
        val browsing = focus?.let { Browsing(it.atZone(zone).toLocalDate(), it) }

        val known = tt.destinations.associateBy { it.id }
        val shown = BarPreferences.resolve(tt.destinations.map { it.id }, saved).filter { it.visible }.mapNotNull { known[it.id] }
        val destinations = shown.map { d ->
            val legs = Leg.entries.map { leg ->
                val departures = if (browsing != null) wholeDay(tt, d.id, leg, browsing.day, zone) else departures(tt, d.id, leg, live, now, zone)
                LegBoard(leg, towards = if (leg == Leg.Outbound) d.target else d.origin, departures = departures)
            }
            DestinationBoard(d.id, d.label, d.origin, d.target, legs)
        }
        return BoardUiState(
            isLoading = false,
            destinations = destinations,
            now = now,
            lastUpdate = live.lastUpdate,
            realtimeFailed = live.failed,
            isRefreshing = live.refreshing,
            scheduleStartsOn = tt.validFrom?.takeIf { today < it },
            browsing = browsing,
            today = today,
            validUntil = tt.validUntil,
            liveTrips = live.trips,
            timetable = tt,
            looseTrams = live.looseTrams,
        )
    }

    /** Timetable of a whole calendar day: live data only describes the next hour or so. */
    private fun wholeDay(tt: Timetable, destinationId: String, leg: Leg, day: LocalDate, zone: ZoneId): List<Departure> {
        val from = day.atStartOfDay(zone).toInstant()
        val until = day.plusDays(1).atStartOfDay(zone).toInstant().minusSeconds(1)
        val scheduled = tt.scheduled(destinationId, leg, from, until, zone)
        return DepartureMerger.merge(destinationId, leg, tt, scheduled, emptyList(), from, DAY_LIMIT, Duration.ofHours(25), Duration.ZERO)
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
        const val SHORTCUTS_DELAY_MS = 5_000L
        const val MIN_UPCOMING = 5
        const val DAY_LIMIT = 800
        const val LIMIT = 150 // a tram every ~6 min for 6 h is ~60 rows per pole
    }
}
