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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.runtime.LaunchedEffect
import com.ilsecondodasinistra.catchit.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

private const val POLL_MS = 20_000L

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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(state.stopName.ifEmpty { stringResource(R.string.app_name) }, fontWeight = FontWeight.Bold) },
                subtitle = { if (!state.isLoading) LiveSubtitle(state) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading || state.boards.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                Boards(state, onRefresh)
            }
        }
    }
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
private fun Boards(state: BoardUiState, onRefresh: () -> Unit) {
    val pagerState = rememberPagerState { state.boards.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            state.boards.forEachIndexed { index, board ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(board.label, fontWeight = FontWeight.SemiBold) },
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
            val board = state.boards[page]
            PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.scheduleStartsOn?.let { date ->
                        item(key = "notice") {
                            Notice(Icons.Rounded.Info, stringResource(R.string.board_schedule_starts, date.dayText()))
                        }
                    }
                    if (state.realtimeFailed) {
                        item(key = "offline") {
                            Notice(Icons.Rounded.WifiOff, stringResource(R.string.board_offline_hint))
                        }
                    }
                    if (board.departures.isEmpty()) {
                        item(key = "empty") { EmptyState() }
                    }
                    itemsIndexed(board.departures, key = { _, d -> "${d.route.id}-${d.time.epochSecond}" }) { index, departure ->
                        DepartureCard(departure, state.now, isNext = index == 0, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun Notice(icon: ImageVector, text: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
