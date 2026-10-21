package com.ilsecondodasinistra.catchit.ui.board

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tram
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilsecondodasinistra.catchit.R
import com.ilsecondodasinistra.catchit.data.RouteInfo
import com.ilsecondodasinistra.catchit.domain.Departure
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun DepartureCard(departure: Departure, now: Instant, isNext: Boolean, modifier: Modifier = Modifier) {
    val colors = if (isNext) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    }
    Card(modifier = modifier, shape = RoundedCornerShape(if (isNext) 32.dp else 24.dp), colors = colors) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = if (isNext) 20.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RouteBadge(departure.route, large = isNext)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = departure.headsign.toTitleCase(),
                    style = if (isNext) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusLine(departure)
            }
            Countdown(departure, now, isNext)
        }
    }
}

@Composable
private fun RouteBadge(route: RouteInfo, large: Boolean) {
    val background = route.color.toColor(Color(0xFF5B5B5B))
    val content = route.textColor.toColor(Color.White)
    Column(
        modifier = Modifier
            .size(if (large) 64.dp else 56.dp)
            .background(background, RoundedCornerShape(if (large) 22.dp else 18.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (route.isTram) Icons.Rounded.Tram else Icons.Rounded.DirectionsBus,
            contentDescription = stringResource(if (route.isTram) R.string.board_tram else R.string.board_bus),
            tint = content,
            modifier = Modifier.size(18.dp),
        )
        Text(text = route.name, color = content, fontSize = if (large) 20.sp else 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun StatusLine(departure: Departure) {
    val live = MaterialTheme.colorScheme.tertiary
    when {
        departure.isCanceled -> Text(
            text = stringResource(R.string.board_canceled),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        departure.isRealtime -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LiveDot(live)
                val delayMinutes = departure.delay?.let { (it.seconds / 60.0).roundToLong() }
                when {
                    delayMinutes == null -> StatusText(stringResource(R.string.board_live), live)
                    delayMinutes == 0L -> StatusText(stringResource(R.string.board_on_time), live)
                    delayMinutes > 0 -> StatusText(pluralStringResource(R.plurals.board_late, delayMinutes.toInt(), delayMinutes), MaterialTheme.colorScheme.error)
                    else -> StatusText(pluralStringResource(R.plurals.board_early, abs(delayMinutes).toInt(), abs(delayMinutes)), live)
                }
            }
            val late = departure.delay?.let { (it.seconds / 60.0).roundToLong() } != 0L
            departure.scheduled?.takeIf { late }?.let {
                StatusText(stringResource(R.string.board_scheduled_at, it.clockText()), LocalMuted.current())
            }
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = LocalMuted.current())
            StatusText(stringResource(R.string.board_from_timetable), LocalMuted.current())
        }
    }
}

private object LocalMuted {
    @Composable
    fun current(): Color = MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun StatusText(text: String, color: Color) {
    Text(text = text, color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, maxLines = 1)
}

@Composable
private fun LiveDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "liveAlpha",
    )
    Box(Modifier.size(8.dp).alpha(alpha).background(color, CircleShape))
}

@Composable
private fun Countdown(departure: Departure, now: Instant, isNext: Boolean) {
    val minutes = minutesUntil(departure.time, now)
    val strike = if (departure.isCanceled) TextDecoration.LineThrough else null
    Column(horizontalAlignment = Alignment.End) {
        val big = if (isNext) 34.sp else 26.sp
        when {
            minutes >= 60 -> Text(departure.time.clockText(), fontSize = big, fontWeight = FontWeight.ExtraBold, textDecoration = strike)
            minutes == 0L -> Text(stringResource(R.string.board_now), fontSize = big, fontWeight = FontWeight.ExtraBold, textDecoration = strike)
            else -> Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(minutes.toString(), fontSize = big, fontWeight = FontWeight.ExtraBold, textDecoration = strike)
                Text(stringResource(R.string.board_min), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        if (minutes < 60) {
            Text(departure.time.clockText(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isNext) 0.8f else 1f), textDecoration = strike)
        }
    }
}

private fun String.toColor(fallback: Color): Color =
    runCatching { Color(("FF$this").toLong(16)) }.getOrDefault(fallback)

/** The feed shouts ("VENEZIA CORSIA C"); read it as Title Case, leaving mixed-case names alone. */
private fun String.toTitleCase(): String =
    if (any { it.isLowerCase() }) this else lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
