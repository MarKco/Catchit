package com.ilsecondodasinistra.catchit.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.ilsecondodasinistra.catchit.ui.theme.glass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Sailing
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ilsecondodasinistra.catchit.R
import com.ilsecondodasinistra.catchit.data.Leg
import com.ilsecondodasinistra.catchit.domain.Departure
import com.ilsecondodasinistra.catchit.domain.isPast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

private const val POLL_MS = 20_000L
private const val START_DESTINATION = "centro"

@Composable
fun BoardScreen(viewModel: BoardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Poll only while the screen is visible: no service, no background traffic.
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refresh()
                delay(POLL_MS)
            }
        }
    }

    BoardContent(state = state, onRefresh = viewModel::requestRefresh)
}

@Composable
fun BoardContent(state: BoardUiState, onRefresh: () -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf(START_DESTINATION) }
    val selected = state.destinations.firstOrNull { it.id == selectedId } ?: state.destinations.firstOrNull()

    val haze = rememberHazeState()
    val density = LocalDensity.current
    var barHeight by remember { mutableStateOf(0.dp) }

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
                key(selected.id) { DestinationPage(selected, state, haze, barHeight, onRefresh) }
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
                        icon = { Icon(destination.icon(), contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        }
    }
}

private fun DestinationBoard.icon(): ImageVector = when (id) {
    "venezia" -> Icons.Rounded.Sailing
    "centro" -> Icons.Rounded.LocationCity
    "stazione" -> Icons.Rounded.Train
    "aeroporto" -> Icons.Rounded.Flight
    else -> Icons.Rounded.Place
}

@Composable
private fun liveStatus(state: BoardUiState): String = when {
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
    onRefresh: () -> Unit,
) {
    val pagerState = rememberPagerState { destination.legs.size }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(0.dp) }

    Box(Modifier.fillMaxSize()) {
        // The lists live in the haze source too, so the header blurs them; they start below the header.
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                LegPage(
                    pageKey = "${destination.id}/${destination.legs[page].leg.id}",
                    departures = destination.legs[page].departures,
                    state = state,
                    topPadding = headerHeight,
                    bottomPadding = bottomPadding,
                    onRefresh = onRefresh,
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
            Column(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) {
                Text(
                    text = stringResource(R.string.board_title, destination.origin, destination.target),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = liveStatus(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                state.scheduleStartsOn?.let { Icons.Rounded.Info to stringResource(R.string.board_schedule_starts, it.dayText()) },
                if (state.realtimeFailed) Icons.Rounded.WifiOff to stringResource(R.string.board_offline_hint) else null,
            )
            if (notices.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    notices.forEach { (icon, text) -> Notice(icon, text) }
                }
            }
        }
    }
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
) {
    val listState = rememberLazyListState()
    val firstUpcoming = departures.indexOfFirst { !it.isPast(state.now) }

    // Open on the next passage, the few already gone (last 15 min) sit above it: scroll up to see them.
    // Done once, when the header has been measured; after that item keys keep the list anchored while rows age.
    var positioned by rememberSaveable(pageKey) { mutableStateOf(false) }
    LaunchedEffect(positioned, departures.isNotEmpty(), topPadding > 0.dp) {
        if (!positioned && departures.isNotEmpty() && topPadding > 0.dp) {
            listState.scrollToItem(firstUpcoming.coerceAtLeast(0))
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
                DepartureCard(departure, state.now, isNext = index == firstUpcoming, modifier = Modifier.fillMaxWidth())
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
