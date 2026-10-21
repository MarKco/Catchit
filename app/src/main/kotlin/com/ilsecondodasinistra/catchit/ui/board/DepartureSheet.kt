package com.ilsecondodasinistra.catchit.ui.board

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ilsecondodasinistra.catchit.R
import com.ilsecondodasinistra.catchit.domain.Departure
import java.time.Duration
import java.time.Instant

/** What the sheet of a passage shows, worked out from the passage and what the live feed knows about its trip. */
private class SheetModel(val departure: Departure, val state: BoardUiState) {
    private val timetable = state.timetable
    val browsing = state.browsing != null
    val live = departure.tripId?.let { state.liveTrips[it] }.takeIf { departure.isRealtime && !browsing }

    private fun name(stopId: String?): String? = stopId?.let { timetable?.stop(it)?.name }?.toTitleCase()

    val from = name(departure.stopId)
    val to = name(departure.alightStopId)

    /** Live arrival when the feed has one for the stop where it gets off; otherwise departure plus the usual duration. */
    private val liveArrival = departure.alightStopId?.let { live?.stopTimes?.get(it) }
    val arrival: Instant? = liveArrival ?: departure.durationSeconds?.let { departure.time.plusSeconds(it.toLong()) }
    val arrivalIsLive = liveArrival != null
    val minutes: Long? = arrival?.let { Duration.between(departure.time, it).toMinutes() }

    class StopRow(val name: String, val time: Instant, val approximate: Boolean)

    val stops: List<StopRow> = departure.shape?.let { shape ->
        shape.stops.indices.map { i ->
            val id = shape.stops[i]
            val exact = live?.stopTimes?.get(id)
            val label = name(id) ?: id
            when {
                i == 0 -> StopRow(label, departure.time, approximate = false)
                exact != null -> StopRow(label, exact, approximate = false)
                else -> StopRow(label, departure.time.plusSeconds(shape.offsets[i].toLong()), approximate = true)
            }
        }
    }.orEmpty()

    /** Straight-line distance from the vehicle to the stop where this passage leaves from. */
    val vehicleDistance: Double? = live?.vehicle?.let { fix ->
        timetable?.stop(departure.stopId)?.let { stop ->
            val lat = stop.lat
            val lon = stop.lon
            if (lat != null && lon != null) distanceMeters(fix.latitude, fix.longitude, lat, lon) else null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DepartureSheet(
    departure: Departure,
    /** The next passages of the same line at the same stop. */
    next: List<Instant>,
    state: BoardUiState,
    onDismiss: () -> Unit,
) {
    val model = SheetModel(departure, state)
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RouteBadge(departure.route, size = 56.dp)
                Column {
                    Text(stringResource(R.string.board_terminus_title, departure.headsign.toTitleCase()), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    // The night loops are called "VENEZIA - VENEZIA": no use repeating it.
                    val halves = departure.route.longName.split(" - ")
                    if (halves.size != 2 || !halves[0].equals(halves[1], ignoreCase = true)) {
                        Text(
                            text = departure.route.longName.toTitleCase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            TimesBlock(model)

            if (!model.browsing) StatusLine(departure)

            model.vehicleDistance?.let { distance ->
                Section(stringResource(R.string.sheet_vehicle_title)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.NearMe, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Column {
                            Text(
                                stringResource(R.string.sheet_vehicle_at, formatDistance(distance), model.from ?: ""),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            model.live?.vehicle?.let {
                                Text(
                                    stringResource(R.string.sheet_vehicle_age, Instant.ofEpochSecond(it.epochSecond).clockText()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (model.stops.size > 1) {
                Section(stringResource(R.string.sheet_stops_title)) {
                    Column {
                        model.stops.forEachIndexed { index, stop ->
                            StopLine(stop, first = index == 0, last = index == model.stops.lastIndex)
                        }
                    }
                    Text(
                        stringResource(if (model.live != null) R.string.sheet_stops_note else R.string.sheet_stops_note_timetable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (next.isNotEmpty()) {
                Section(stringResource(R.string.sheet_next_title)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        next.forEach { time ->
                            Text(
                                text = time.clockText(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            HorizontalDivider()
            Text(sourceText(model, state), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            FilledTonalButton(onClick = { share(context, model) }, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.sheet_share), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun TimesBlock(model: SheetModel) {
    val d = model.departure
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Label(stringResource(R.string.sheet_from))
            Text(d.time.clockText(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
            model.from?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2) }
        }
        model.minutes?.let { minutes ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.sheet_duration, minutes.toInt()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        model.arrival?.let { arrival ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Label(stringResource(R.string.sheet_to))
                Text(arrival.clockText(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                model.to?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2) }
                if (!model.browsing) {
                    Text(
                        stringResource(if (model.arrivalIsLive) R.string.sheet_arrival_live else R.string.sheet_arrival_timetable),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (model.arrivalIsLive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

/** One stop of the run on a vertical line: a filled dot at the ends, a hollow one in between. */
@Composable
private fun StopLine(stop: SheetModel.StopRow, first: Boolean, last: Boolean) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val dot = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.surfaceContainerLow
    Row(Modifier.height(IntrinsicSize.Min).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val top = if (first) size.height / 2 else 0f
                    val bottom = if (last) size.height / 2 else size.height
                    drawLine(line, Offset(x, top), Offset(x, bottom), strokeWidth = 3.dp.toPx())
                },
            contentAlignment = Alignment.Center,
        ) {
            val end = first || last
            Box(
                Modifier
                    .size(if (end) 12.dp else 10.dp)
                    .background(if (end) dot else background, CircleShape)
                    .then(if (end) Modifier else Modifier.border(2.dp, dot, CircleShape)),
            )
        }
        Text(
            text = stop.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (first || last) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f).padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
        )
        Text(
            text = (if (stop.approximate) "~" else "") + stop.time.clockText(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (first || last) FontWeight.Bold else FontWeight.Normal,
            color = if (stop.approximate) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun sourceText(model: SheetModel, state: BoardUiState): String {
    val d = model.departure
    val browsing = state.browsing
    return when {
        browsing != null -> stringResource(R.string.sheet_source_day, browsing.day.fullDayText())
        d.isRealtime -> stringResource(R.string.sheet_source_live, (state.lastUpdate ?: state.now).atZone(veniceZone).toLocalTime().withNano(0).toString())
        d.route.isTram -> stringResource(R.string.sheet_source_tram)
        state.realtimeFailed -> stringResource(R.string.sheet_source_timetable)
        else -> stringResource(R.string.sheet_source_no_live)
    }
}

private fun share(context: android.content.Context, model: SheetModel) {
    val d = model.departure
    val text = buildString {
        append(context.getString(R.string.sheet_share_text, d.route.name, d.headsign.toTitleCase(), model.from ?: "", d.time.clockText()))
        val arrival = model.arrival
        val to = model.to
        if (arrival != null && to != null) append(context.getString(R.string.sheet_share_arrival, to, arrival.clockText()))
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, null))
}
