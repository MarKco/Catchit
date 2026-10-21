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
import com.ilsecondodasinistra.catchit.domain.isPast
import com.ilsecondodasinistra.catchit.ui.theme.lineColors
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun DepartureCard(departure: Departure, now: Instant, isNext: Boolean, modifier: Modifier = Modifier) {
    val past = departure.isPast(now)
    val colors = if (past) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    } else if (isNext) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    }
    Card(modifier = modifier.alpha(if (past) 0.65f else 1f), shape = RoundedCornerShape(16.dp), colors = colors) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RouteBadge(departure.route)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = departure.headsign.toTitleCase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusLine(departure)
            }
            Countdown(departure, now)
        }
    }
}

@Composable
private fun RouteBadge(route: RouteInfo) {
    val (background, content) = route.lineColors()
    Column(
        modifier = Modifier
            .size(44.dp)
            .background(background, RoundedCornerShape(14.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (route.isTram) Icons.Rounded.Tram else Icons.Rounded.DirectionsBus,
            contentDescription = stringResource(if (route.isTram) R.string.board_tram else R.string.board_bus),
            tint = content,
            modifier = Modifier.size(14.dp),
        )
        Text(text = route.name, color = content, fontSize = 14.sp, lineHeight = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun StatusLine(departure: Departure) {
    val live = MaterialTheme.colorScheme.tertiary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when {
        departure.isCanceled -> StatusText(stringResource(R.string.board_canceled), MaterialTheme.colorScheme.error, FontWeight.Bold)
        departure.isRealtime -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            LiveDot(live)
            val delayMinutes = departure.delay?.let { (it.seconds / 60.0).roundToLong() }
            when {
                delayMinutes == null -> StatusText(stringResource(R.string.board_live), live)
                delayMinutes == 0L -> StatusText(stringResource(R.string.board_on_time), live)
                delayMinutes > 0 -> StatusText(pluralStringResource(R.plurals.board_late, delayMinutes.toInt(), delayMinutes), MaterialTheme.colorScheme.error)
                else -> StatusText(pluralStringResource(R.plurals.board_early, abs(delayMinutes).toInt(), abs(delayMinutes)), live)
            }
            if (delayMinutes != 0L) {
                departure.scheduled?.let {
                    StatusText("·  " + stringResource(R.string.board_scheduled_at, it.clockText()), muted, modifier = Modifier.weight(1f, fill = false))
                }
            }
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(13.dp), tint = muted)
            StatusText(stringResource(R.string.board_from_timetable), muted)
        }
    }
}

@Composable
private fun StatusText(text: String, color: Color, weight: FontWeight = FontWeight.Medium, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = weight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
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
    Box(Modifier.size(7.dp).alpha(alpha).background(color, CircleShape))
}

@Composable
private fun Countdown(departure: Departure, now: Instant) {
    val minutes = minutesUntil(departure.time, now)
    val strike = if (departure.isCanceled) TextDecoration.LineThrough else null
    Column(horizontalAlignment = Alignment.End) {
        if (departure.isPast(now)) {
            val ago = minutesSince(departure.time, now)
            Text(departure.time.clockText(), fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, textDecoration = strike)
            Text(
                text = if (ago == 0L) stringResource(R.string.board_just_left) else stringResource(R.string.board_ago, ago),
                style = MaterialTheme.typography.labelMedium,
            )
            return@Column
        }
        when {
            minutes >= 60 -> Text(departure.time.clockText(), fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, textDecoration = strike)
            minutes == 0L -> Text(stringResource(R.string.board_now), fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, textDecoration = strike)
            else -> Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(minutes.toString(), fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, textDecoration = strike)
                Text(stringResource(R.string.board_min), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        if (minutes < 60) {
            Text(departure.time.clockText(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = strike)
        } else {
            val days = daysAhead(departure.time, now)
            if (days > 0) {
                Text(
                    text = if (days == 1L) stringResource(R.string.board_tomorrow) else stringResource(R.string.board_in_days, days),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The feed shouts ("VENEZIA CORSIA C"); read it as Title Case, leaving mixed-case names alone. */
private fun String.toTitleCase(): String =
    if (any { it.isLowerCase() }) this else lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
