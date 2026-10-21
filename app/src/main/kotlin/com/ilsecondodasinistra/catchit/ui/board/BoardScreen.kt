package com.ilsecondodasinistra.catchit.ui.board

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ilsecondodasinistra.catchit.R
import com.ilsecondodasinistra.catchit.StartupTrace
import com.ilsecondodasinistra.catchit.data.Leg
import com.ilsecondodasinistra.catchit.domain.Departure
import com.ilsecondodasinistra.catchit.domain.isPast
import com.ilsecondodasinistra.catchit.ui.destinationIcon
import com.ilsecondodasinistra.catchit.ui.theme.glass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private const val POLL_MS = 20_000L
private val DEFAULT_BROWSE_TIME = LocalTime.of(8, 0)

/** What the header menu can do. */
private class BoardActions(
    val onPickDay: () -> Unit,
    val onToday: () -> Unit,
    val onSettings: () -> Unit,
)

@Composable
fun BoardScreen(onOpenSettings: () -> Unit, viewModel: BoardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val browsing by rememberUpdatedState(state.browsing != null)

    // Poll only while the screen is visible: no service, no background traffic. Another day has no live data.
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                if (!browsing) viewModel.refresh()
                delay(POLL_MS)
            }
        }
    }

    BoardContent(
        state = state,
        onRefresh = viewModel::requestRefresh,
        onBrowse = viewModel::browse,
        onToday = viewModel::backToToday,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun BoardContent(
    state: BoardUiState,
    onRefresh: () -> Unit,
    onBrowse: (Instant) -> Unit,
    onToday: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    // The first visible button is the start screen; a button hidden from the settings falls back to it too.
    val selected = state.destinations.firstOrNull { it.id == selectedId } ?: state.destinations.firstOrNull()

    // Back from any other button goes to the first one (the start screen); only from there it leaves the app.
    val first = state.destinations.firstOrNull()
    BackHandler(enabled = first != null && selected != null && selected.id != first.id) {
        selectedId = first?.id
    }

    val haze = rememberHazeState()
    val density = LocalDensity.current
    val context = LocalContext.current
    var barHeight by remember { mutableStateOf(0.dp) }

    // The passage whose sheet is open.
    var sheet by remember { mutableStateOf<Departure?>(null) }

    var pickingDay by rememberSaveable { mutableStateOf(false) }
    var pickedDay by rememberSaveable { mutableStateOf<String?>(null) } // ISO date, between the two dialogs

    val actions = BoardActions(
        onPickDay = { pickingDay = true },
        onToday = {
            if (state.browsing == null) {
                Toast.makeText(context, R.string.board_toast_already_today, Toast.LENGTH_SHORT).show()
            } else {
                onToday()
                Toast.makeText(context, R.string.board_toast_today, Toast.LENGTH_SHORT).show()
            }
        },
        onSettings = onOpenSettings,
    )

    // The moment the user sees what they came for: the system logs it as "Fully drawn" in the app start-up time.
    val contentReady = state.hasContent
    ReportDrawnWhen { contentReady }
    LaunchedEffect(contentReady) { if (contentReady) StartupTrace.mark("content ready") }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            // The pages are the haze source: the glass header and bar blur the list scrolling behind them.
            Box(Modifier.fillMaxSize().hazeSource(haze)) {
                if (state.isLoading || selected == null) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            if (!state.isLoading && selected != null) {
                // A fresh pager (and tab) per destination.
                key(selected.id) { DestinationPage(selected, state, haze, barHeight, actions, onRefresh) { sheet = it } }
            }
            NavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { barHeight = with(density) { it.height.toDp() } }
                    .glass(haze),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
            ) {
                state.destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = destination.id == selected?.id,
                        onClick = { selectedId = destination.id },
                        icon = { Icon(destinationIcon(destination.id), contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        }
    }

    sheet?.let { shown ->
        // The list moves under the sheet (live data arrives, rows age): follow the same passage, not the stale copy.
        val all = selected?.legs?.flatMap { it.departures }.orEmpty()
        val current = all.firstOrNull { it.sameRun(shown) } ?: shown
        val next = all.filter { it.route.id == current.route.id && it.stopId == current.stopId && it.time > current.time }
            .sortedBy { it.time }.take(3).map { it.time }
        DepartureSheet(current, next, state, onDismiss = { sheet = null })
    }

    val today = state.today
    val last = state.validUntil
    if (pickingDay && today != null) {
        DayPickerDialog(
            today = today,
            last = last?.takeIf { !it.isBefore(today) } ?: today,
            initial = state.browsing?.day ?: today,
            onDismiss = { pickingDay = false },
            onPicked = { day ->
                pickingDay = false
                pickedDay = day.toString()
            },
        )
    }
    pickedDay?.let { iso ->
        val day = LocalDate.parse(iso)
        TimePickerDialog(
            initial = state.browsing?.focus?.atZone(veniceZone)?.toLocalTime()
                ?: if (day == today) state.now.atZone(veniceZone).toLocalTime() else DEFAULT_BROWSE_TIME,
            onDismiss = { pickedDay = null },
            onPicked = { time ->
                pickedDay = null
                val focus = day.atTime(time).atZone(veniceZone).toInstant()
                onBrowse(focus)
                Toast.makeText(
                    context,
                    context.getString(R.string.board_toast_browse, day.fullDayText(), time.toString()),
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
}

@Composable
private fun headerStatus(state: BoardUiState): String = when {
    state.browsing != null -> stringResource(R.string.board_browsing_status, state.browsing.day.fullDayText())
    state.realtimeFailed -> stringResource(R.string.board_realtime_unavailable)
    state.lastUpdate == null -> stringResource(R.string.board_connecting)
    else -> {
        val seconds = Duration.between(state.lastUpdate, state.now).seconds.coerceAtLeast(0)
        if (seconds < 5) stringResource(R.string.board_updated_now) else stringResource(R.string.board_updated_ago, seconds)
    }
}

@Composable
private fun DestinationPage(
    destination: DestinationBoard,
    state: BoardUiState,
    haze: HazeState,
    bottomPadding: Dp,
    actions: BoardActions,
    onRefresh: () -> Unit,
    onOpen: (Departure) -> Unit,
) {
    val pagerState = rememberPagerState { destination.legs.size }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(0.dp) }

    Box(Modifier.fillMaxSize()) {
        // The lists live in the haze source too, so the header blurs them; they start below the header.
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 0) { page ->
                // A new day (or time) picked: a new key, so the list opens at the new time again.
                val position = state.browsing?.focus?.epochSecond ?: "now"
                LegPage(
                    pageKey = "${destination.id}/${destination.legs[page].leg.id}/$position",
                    departures = destination.legs[page].departures,
                    state = state,
                    topPadding = headerHeight,
                    bottomPadding = bottomPadding,
                    onRefresh = onRefresh,
                    onOpen = onOpen,
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { headerHeight = with(density) { it.height.toDp() } }
                .glass(haze),
        ) {
            Row(
                modifier = Modifier.statusBarsPadding().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.board_title, destination.origin, destination.target),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = headerStatus(state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OverflowMenu(state.today, actions)
            }
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage, containerColor = Color.Transparent) {
                destination.legs.forEachIndexed { index, board ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(board.leg.title()), fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.board_towards, board.towards), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                    )
                }
            }
            // In the header, not in the list: they stay visible after the list opens on the next passage.
            val notices = listOfNotNull(
                state.browsing?.let { Icons.Rounded.CalendarMonth to stringResource(R.string.board_browsing_notice, it.day.fullDayText()) },
                if (state.browsing == null) state.scheduleStartsOn?.let { Icons.Rounded.Info to stringResource(R.string.board_schedule_starts, it.dayText()) } else null,
                if (state.browsing == null && state.realtimeFailed) Icons.Rounded.WifiOff to stringResource(R.string.board_offline_hint) else null,
            )
            if (notices.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    notices.forEach { (icon, text) -> Notice(icon, text) }
                }
            }
        }
    }
}

/** The three dots: pick a day, back to today (calendar with today's number), settings. */
@Composable
private fun OverflowMenu(today: LocalDate?, actions: BoardActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.board_menu))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.board_menu_pick)) },
                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                onClick = { open = false; actions.onPickDay() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.board_menu_today)) },
                leadingIcon = { DayOfMonthIcon(today?.dayOfMonth) },
                onClick = { open = false; actions.onToday() },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.board_menu_settings)) },
                leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                onClick = { open = false; actions.onSettings() },
            )
        }
    }
}

/** A calendar page with the day of the month written in it. */
@Composable
private fun DayOfMonthIcon(day: Int?) {
    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(24.dp))
        if (day != null) {
            Text(
                text = day.toString(),
                fontSize = 9.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(
    today: LocalDate,
    last: LocalDate,
    initial: LocalDate,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
) {
    fun Long.toDay(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        // From today to the end of the timetable we have: nothing before or after is known.
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis.toDay() in today..last
            override fun isSelectableYear(year: Int): Boolean = year in today.year..last.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = { pickerState.selectedDateMillis?.let { onPicked(it.toDay()) } },
            ) { Text(stringResource(R.string.board_pick_next)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.board_pick_cancel)) } },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, onPicked: (LocalTime) -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.board_time_title)) },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.board_time_show))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.board_pick_cancel)) } },
    )
}

private fun Leg.title(): Int = if (this == Leg.Outbound) R.string.board_leg_outbound else R.string.board_leg_return

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegPage(
    pageKey: String,
    departures: List<Departure>,
    state: BoardUiState,
    topPadding: Dp,
    bottomPadding: Dp,
    onRefresh: () -> Unit,
    onOpen: (Departure) -> Unit,
) {
    val listState = rememberLazyListState()
    // The instant the list opens on: now, or the time picked in the calendar.
    val reference = state.browsing?.focus ?: state.now
    val showCountdown = state.browsing == null
    val firstUpcoming = departures.indexOfFirst { !it.isPast(reference) }

    // Open on the next passage, the few already gone (last 15 min) sit above it: scroll up to see them.
    // Done once, when the header has been measured; after that item keys keep the list anchored while rows age.
    var positioned by rememberSaveable(pageKey) { mutableStateOf(false) }
    LaunchedEffect(positioned, departures.isNotEmpty(), topPadding > 0.dp) {
        if (!positioned && departures.isNotEmpty() && topPadding > 0.dp) {
            // Nothing at or after the picked time: show the end of the day, not the start.
            val index = if (firstUpcoming >= 0) firstUpcoming else departures.lastIndex
            listState.scrollToItem(index)
            positioned = true
        }
    }

    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        // Below the header, which is drawn over the list.
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = state.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = topPadding),
            )
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // Room for the glass header and bar: the first and last rows must scroll clear of them.
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = topPadding + 8.dp, bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (departures.isEmpty()) {
                item(key = "empty") { EmptyState() }
            }
            val keys = departures.uniqueKeys()
            itemsIndexed(departures, key = { index, _ -> keys[index] }) { index, departure ->
                DepartureCard(
                    departure = departure,
                    now = reference,
                    isNext = index == firstUpcoming,
                    modifier = Modifier.fillMaxWidth(),
                    showCountdown = showCountdown,
                    onClick = { onOpen(departure) },
                    arrivesAt = departure.alightStopId?.let { state.timetable?.stop(it)?.name },
                )
            }
        }
    }
}

/** Stable lazy-list keys; two passages of one line in the same second still get distinct ones. */
private fun List<Departure>.uniqueKeys(): List<String> {
    val seen = HashMap<String, Int>()
    return map { d ->
        val key = "${d.route.id}-${d.time.epochSecond}"
        val n = seen.merge(key, 1, Int::plus)!!
        if (n == 1) key else "$key#$n"
    }
}

@Composable
private fun Notice(icon: ImageVector, text: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.DirectionsBus, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Text(stringResource(R.string.board_empty), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The same run of a line, whether it is now live or still just on the timetable. */
private fun Departure.sameRun(other: Departure): Boolean =
    route.id == other.route.id && stopId == other.stopId && (scheduled ?: time) == (other.scheduled ?: other.time)
