package com.ilsecondodasinistra.catchit.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoardContent(state: BoardUiState, onRefresh: () -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf(START_DESTINATION) }
    val selected = state.destinations.firstOrNull { it.id == selectedId } ?: state.destinations.firstOrNull()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = selected?.let { stringResource(R.string.board_title, it.origin, it.target) } ?: stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                },
                subtitle = { if (!state.isLoading) LiveSubtitle(state) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            NavigationBar {
                state.destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = destination.id == selected?.id,
                        onClick = { selectedId = destination.id },
                        icon = { Icon(destination.icon(), contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading || selected == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                // A fresh pager (and tab) per destination.
                key(selected.id) { DestinationPage(selected, state, onRefresh) }
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
private fun LiveSubtitle(state: BoardUiState) {
    val text = when {
        state.realtimeFailed -> stringResource(R.string.board_realtime_unavailable)
        state.lastUpdate == null -> stringResource(R.string.board_connecting)
        else -> {
            val seconds = Duration.between(state.lastUpdate, state.now).seconds.coerceAtLeast(0)
            if (seconds < 5) stringResource(R.string.board_updated_now) else stringResource(R.string.board_updated_ago, seconds)
        }
    }
    Text(text)
}

@Composable
private fun DestinationPage(destination: DestinationBoard, state: BoardUiState, onRefresh: () -> Unit) {
    val pagerState = rememberPagerState { destination.legs.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
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
        // Outside the list: the list opens scrolled past the notices, and they must stay visible.
        Column(Modifier.padding(horizontal = 12.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.scheduleStartsOn?.let { date ->
                Notice(Icons.Rounded.Info, stringResource(R.string.board_schedule_starts, date.dayText()))
            }
            if (state.realtimeFailed) {
                Notice(Icons.Rounded.WifiOff, stringResource(R.string.board_offline_hint))
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
            LegPage("${destination.id}/${destination.legs[page].leg.id}", destination.legs[page].departures, state, onRefresh)
        }
    }
}

private fun Leg.title(): Int = if (this == Leg.Outbound) R.string.board_leg_outbound else R.string.board_leg_return

@Composable
private fun LegPage(pageKey: String, departures: List<Departure>, state: BoardUiState, onRefresh: () -> Unit) {
    val listState = rememberLazyListState()
    val firstUpcoming = departures.indexOfFirst { !it.isPast(state.now) }

    // Open on the next passage, the few already gone (last 15 min) sit above it: scroll up to see them.
    // Done once; after that item keys keep the list anchored while rows age into the past.
    var positioned by rememberSaveable(pageKey) { mutableStateOf(false) }
    LaunchedEffect(positioned, departures.isNotEmpty()) {
        if (!positioned && departures.isNotEmpty()) {
            listState.scrollToItem(firstUpcoming.coerceAtLeast(0))
            positioned = true
        }
    }

    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
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
