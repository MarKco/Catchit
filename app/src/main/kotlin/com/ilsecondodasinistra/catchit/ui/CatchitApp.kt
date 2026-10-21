package com.ilsecondodasinistra.catchit.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ilsecondodasinistra.catchit.ui.board.BoardScreen
import com.ilsecondodasinistra.catchit.ui.settings.SettingsScreen

/** Two screens, no navigation library needed: the board and its settings. */
@Composable
fun CatchitApp() {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(onBack = { showSettings = false })
    } else {
        BoardScreen(onOpenSettings = { showSettings = true })
    }
}
