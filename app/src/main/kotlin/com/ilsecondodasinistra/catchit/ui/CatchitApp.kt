package com.ilsecondodasinistra.catchit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ilsecondodasinistra.catchit.ui.board.BoardScreen
import com.ilsecondodasinistra.catchit.ui.settings.SettingsScreen

/**
 * Two screens, no navigation library needed: the board and its settings. The settings are a layer over the board, which
 * stays composed: coming back finds the same destination, tab and scroll position.
 */
@Composable
fun CatchitApp() {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    BoardScreen(onOpenSettings = { showSettings = true })
    if (showSettings) {
        PredictiveLayer(onBack = { showSettings = false }) { _, dismiss -> SettingsScreen(onBack = dismiss) }
    }
}
