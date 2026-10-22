package com.ilsecondodasinistra.catchit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Sailing
import androidx.compose.material.icons.rounded.Train
import androidx.compose.ui.graphics.vector.ImageVector

/** Bottom-bar icon of a destination; a new destination from a timetable update falls back to a pin. */
fun destinationIcon(id: String): ImageVector = when (id) {
    "venezia" -> Icons.Rounded.Sailing
    "centro" -> Icons.Rounded.LocationCity
    "stazione" -> Icons.Rounded.Train
    "aeroporto" -> Icons.Rounded.Flight
    "ospedale" -> Icons.Rounded.LocalHospital
    else -> Icons.Rounded.Place
}
