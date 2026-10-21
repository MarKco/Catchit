package com.ilsecondodasinistra.catchit.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.catchit.R
import com.ilsecondodasinistra.catchit.data.LogEntry
import com.ilsecondodasinistra.catchit.data.LogLevel
import com.ilsecondodasinistra.catchit.data.RouteInfo
import com.ilsecondodasinistra.catchit.data.UpdateLog
import com.ilsecondodasinistra.catchit.data.VersionInfo
import com.ilsecondodasinistra.catchit.ui.destinationIcon
import com.ilsecondodasinistra.catchit.ui.theme.LineColors
import com.ilsecondodasinistra.catchit.ui.theme.lineColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ROW_HEIGHT = 64.dp
private val whenFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ITALIAN).withZone(ZoneId.of("Europe/Rome"))
private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")
private val clockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("Europe/Rome"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The bar is edited on a local copy while a row is dragged, and saved when the finger lifts.
    var order by remember(state.bar) { mutableStateOf(state.bar) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { ROW_HEIGHT.toPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "h-timetable") { SectionTitle(stringResource(R.string.settings_section_timetable)) }
            item(key = "timetable") {
                TimetableCard(
                    state = state,
                    onCheck = { viewModel.checkNow() },
                    onRebuild = { viewModel.checkNow(force = true) },
                )
            }

            item(key = "h-bar") { SectionTitle(stringResource(R.string.settings_section_bar)) }
            item(key = "bar-hint") {
                Text(stringResource(R.string.settings_bar_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(order, key = { "bar-${it.id}" }) { row ->
                val currentOrder by rememberUpdatedState(order)
                val visibleCount = order.count { it.visible }
                BarRowItem(
                    row = row,
                    dragging = draggingId == row.id,
                    dragOffset = if (draggingId == row.id) dragOffset else 0f,
                    canHide = !(row.visible && visibleCount == 1),
                    onToggle = { visible ->
                        order = order.map { if (it.id == row.id) it.copy(visible = visible) else it }
                        viewModel.saveBar(order)
                    },
                    onDragStart = { draggingId = row.id; dragOffset = 0f },
                    onDrag = { dy ->
                        dragOffset += dy
                        // Swap with the neighbour once the row is dragged past half of its height.
                        val index = currentOrder.indexOfFirst { it.id == row.id }
                        val steps = (dragOffset / rowHeightPx).let { if (it >= 0) (it + 0.5f).toInt() else (it - 0.5f).toInt() }
                        val target = (index + steps).coerceIn(0, currentOrder.lastIndex)
                        if (target != index) {
                            order = currentOrder.toMutableList().apply { add(target, removeAt(index)) }
                            dragOffset -= (target - index) * rowHeightPx
                        }
                    },
                    onDragEnd = {
                        draggingId = null
                        dragOffset = 0f
                        viewModel.saveBar(order)
                    },
                )
            }

            item(key = "h-log") { SectionTitle(stringResource(R.string.settings_section_log)) }
            item(key = "log-hint") {
                Text(
                    stringResource(R.string.settings_log_hint, UpdateLog.CAPACITY),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item(key = "log-actions") {
                LogActions(
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.settings_log_share_subject), viewModel.logText()))
                        Toast.makeText(context, R.string.settings_log_copied, Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_log_share_subject))
                            putExtra(Intent.EXTRA_TEXT, viewModel.logText())
                        }
                        context.startActivity(Intent.createChooser(send, context.getString(R.string.settings_log_share_subject)))
                    },
                    onClear = viewModel::clearLog,
                )
            }
            item(key = "log") { LogCard(state.log) }

            item(key = "about") {
                Text(
                    stringResource(R.string.settings_about, state.appVersion),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }

    if (state.askMetered) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMetered,
            title = { Text(stringResource(R.string.settings_metered_title)) },
            text = { Text(stringResource(R.string.settings_metered_message)) },
            confirmButton = { TextButton(onClick = viewModel::confirmMetered) { Text(stringResource(R.string.settings_metered_yes)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissMetered) { Text(stringResource(R.string.board_pick_cancel)) } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimetableCard(state: SettingsUiState, onCheck: () -> Unit, onRebuild: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Labeled(stringResource(R.string.settings_versions)) {
                state.versions.forEach { Text(it.describe(), style = MaterialTheme.typography.bodyMedium) }
            }
            Labeled(stringResource(R.string.settings_lines)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.lines.forEach { LineChip(it) }
                }
                Text(stringResource(R.string.settings_stops, state.stopCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Labeled(stringResource(R.string.settings_last_check)) {
                val last = state.lastCheck
                Text(
                    text = if (last == null) stringResource(R.string.settings_never_checked)
                    else stringResource(R.string.settings_last_check_value, whenFormat.format(Instant.ofEpochMilli(last.epochMillis)), last.trigger, last.outcome),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(stringResource(R.string.settings_auto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onCheck, enabled = !state.checking) {
                    if (state.checking) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = stringResource(if (state.checking) R.string.settings_checking else R.string.settings_check_now),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                TextButton(onClick = onRebuild, enabled = !state.checking) { Text(stringResource(R.string.settings_rebuild)) }
            }
            state.result?.let {
                Text(stringResource(R.string.settings_result, it), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun VersionInfo.describe(): String {
    fun String.pretty() = runCatching { LocalDate.parse(this, DateTimeFormatter.BASIC_ISO_DATE).format(dateFormat) }.getOrDefault(this)
    val source = stringResource(if (downloaded) R.string.settings_version_downloaded else R.string.settings_version_bundled)
    return stringResource(R.string.settings_version_line, feedStart.pretty(), feedEnd.pretty(), source)
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun LineChip(name: String) {
    val colors = lineColorsOf(name)
    Box(
        modifier = Modifier.background(colors.container, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(name, color = colors.content, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}

private fun lineColorsOf(name: String): LineColors =
    RouteInfo(id = name, name = name, color = "", textColor = "", type = 3).lineColors()

@Composable
private fun BarRowItem(
    row: BarRow,
    dragging: Boolean,
    dragOffset: Float,
    canHide: Boolean,
    onToggle: (Boolean) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val container = if (dragging) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer { translationY = dragOffset },
    ) {
        Row(Modifier.fillMaxSize().padding(start = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Only the handle starts a drag: the rest of the row must stay free for scrolling the page.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDrag = { change, amount ->
                                change.consume()
                                onDrag(amount.y)
                            },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragEnd,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.DragHandle, contentDescription = stringResource(R.string.settings_bar_drag), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(destinationIcon(row.id), contentDescription = null, modifier = Modifier.padding(horizontal = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(row.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Switch(checked = row.visible, onCheckedChange = onToggle, enabled = canHide)
        }
    }
}

@Composable
private fun LogActions(onCopy: () -> Unit, onShare: () -> Unit, onClear: () -> Unit) {
    val padding = PaddingValues(horizontal = 12.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = onCopy, contentPadding = padding, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.settings_log_copy), maxLines = 1, modifier = Modifier.padding(start = 6.dp))
        }
        FilledTonalButton(onClick = onShare, contentPadding = padding, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.settings_log_share), maxLines = 1, modifier = Modifier.padding(start = 6.dp))
        }
        OutlinedButton(onClick = onClear, contentPadding = padding, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.settings_log_clear), maxLines = 1, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun LogCard(entries: List<LogEntry>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (entries.isEmpty()) {
                Text(stringResource(R.string.settings_log_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Newest first on screen; the copied text is oldest first, like a log file.
            entries.asReversed().forEach { entry ->
                val color = when (entry.level) {
                    LogLevel.Error -> MaterialTheme.colorScheme.error
                    LogLevel.Warning -> MaterialTheme.colorScheme.tertiary
                    LogLevel.Info -> MaterialTheme.colorScheme.onSurface
                }
                Column {
                    Text(
                        text = "${clockFormat.format(entry.time)} [${entry.kind.tag}] ${entry.message}",
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                    entry.detail?.let {
                        Text(it, color = color.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 13.sp, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
    }
}
