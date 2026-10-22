package com.ilsecondodasinistra.catchit.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ilsecondodasinistra.catchit.R
import com.ilsecondodasinistra.catchit.data.GeoCatalog
import com.ilsecondodasinistra.catchit.ui.board.BoardUiState

/**
 * The map of a leg (the vehicles about to arrive at its boarding stops) or, with [MapTarget.highlight], of one passage:
 * the same map with that run's path and stops drawn, so you see how far along it is. A full-screen layer of the board
 * screen, so the board's polling keeps the map live.
 */
@Composable
fun MapScreen(state: BoardUiState, target: MapTarget, loadGeo: suspend () -> GeoCatalog, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val geo by produceState<GeoCatalog?>(null) { value = loadGeo() }
    // The vehicle tapped on the map: its run is drawn; a tap on it again, or on the map, goes back to what was asked for.
    var selected by remember(target.destinationId, target.leg) { mutableStateOf<String?>(null) }
    val model = remember(state, target, geo, selected) { buildMapModel(state, target, geo, selected) }
    val density = LocalDensity.current
    val dark = isSystemInDarkTheme()
    var recenter by remember { mutableIntStateOf(0) }
    var barHeight by remember { mutableStateOf(0.dp) }
    val cameraKey = remember(target.destinationId, target.leg, target.highlight?.tripId, target.highlight?.scheduled) {
        listOf(target.destinationId, target.leg, target.highlight?.tripId, target.highlight?.scheduled)
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            if (model != null) {
                TransitMap(
                    model, cameraKey, topPadding = barHeight, dark = dark, recenter = recenter,
                    onVehicleTap = { key -> selected = key.takeIf { it != selected } },
                )
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .onSizeChanged { barHeight = with(density) { it.height.toDp() + 8.dp + 8.dp } },
            ) {
                Row(Modifier.padding(start = 4.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.map_back))
                    }
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = model?.title.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle(state, target, model),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            SmallFloatingActionButton(
                onClick = { recenter++ },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp),
            ) {
                Icon(Icons.Rounded.CenterFocusStrong, contentDescription = stringResource(R.string.map_recenter))
            }

            // A yellow "Tram" pill was tapped: say why it has no name and no minutes.
            val loose = model?.vehicles?.firstOrNull { it.key == selected && it.line == null }
            if (loose != null && model != null) {
                LooseTramCard(loose, model.now, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 88.dp))
            }

            // Required by the OpenStreetMap licence; the map's own (i) button is off.
            Surface(
                shape = RoundedCornerShape(topEnd = 8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding(),
            ) {
                Text(stringResource(R.string.map_attribution), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun subtitle(state: BoardUiState, target: MapTarget, model: MapModel?): String {
    val shown = model?.vehicles?.firstOrNull { it.highlighted }
    val line = shown?.line ?: target.highlight?.route?.name
    return when {
        state.realtimeFailed -> stringResource(R.string.board_realtime_unavailable)
        line != null -> {
            val route = stringResource(R.string.map_route_of, line)
            val age = shown?.fixEpochSecond?.let { model.now.epochSecond - it }?.coerceAtLeast(0)
            when {
                age == null -> route
                age < 90 -> route + " · " + stringResource(R.string.map_position_seconds, age.toInt())
                else -> route + " · " + stringResource(R.string.map_position_minutes, (age / 60).toInt())
            }
        }
        else -> {
            val lined = model?.vehicles?.count { it.line != null } ?: 0
            val loose = model?.vehicles?.count { it.line == null } ?: 0
            val base = if (lined == 0) stringResource(R.string.map_no_vehicles) else pluralStringResource(R.plurals.map_vehicles, lined, lined)
            if (loose == 0) base else base + " · " + pluralStringResource(R.plurals.map_loose_trams, loose, loose)
        }
    }
}

@Composable
private fun LooseTramCard(tram: MapVehicle, now: java.time.Instant, modifier: Modifier = Modifier) {
    val name = tram.key.removePrefix("loose:").replace("TRAM_", "Tram ")
    val age = tram.fixEpochSecond?.let { now.epochSecond - it }?.coerceAtLeast(0)
    val position = when {
        age == null -> null
        age < 90 -> stringResource(R.string.map_position_seconds, age.toInt())
        else -> stringResource(R.string.map_position_minutes, (age / 60).toInt())
    }
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp, modifier = modifier) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.map_loose_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.map_loose_body), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = listOfNotNull(name, position).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
