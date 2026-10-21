package com.ilsecondodasinistra.catchit.ui.board

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val venice: ZoneId = ZoneId.of("Europe/Rome")
private val clockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(venice)
private val fullDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMMM", java.util.Locale.ITALIAN)
private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", java.util.Locale.ITALIAN)

internal fun Instant.clockText(): String = clockFormat.format(this)

internal fun java.time.LocalDate.dayText(): String = dayFormat.format(this)

/** Whole minutes to go, rounded up, so "1 min" really means less than a minute is left. */
internal fun minutesUntil(time: Instant, now: Instant): Long {
    val seconds = Duration.between(now, time).seconds
    return if (seconds <= 0) 0 else (seconds + 59) / 60
}

/** Whole minutes since [time], rounded down. */
internal fun minutesSince(time: Instant, now: Instant): Long =
    (Duration.between(time, now).seconds / 60).coerceAtLeast(0)

/** Calendar days between [now] and [time] in Venice (0 = today). */
internal fun daysAhead(time: Instant, now: Instant): Long =
    java.time.temporal.ChronoUnit.DAYS.between(now.atZone(venice).toLocalDate(), time.atZone(venice).toLocalDate())

/** "ven 23 ottobre". */
internal fun java.time.LocalDate.fullDayText(): String = fullDayFormat.format(this)

internal val veniceZone: ZoneId get() = venice
