package com.ilsecondodasinistra.catchit

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.ilsecondodasinistra.catchit.ui.CatchitApp
import com.ilsecondodasinistra.catchit.ui.board.BoardViewModel
import com.ilsecondodasinistra.catchit.ui.theme.CatchitTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Created here, not when the first screen asks: its work (reading the timetables) starts ~200 ms earlier. */
    private val board: BoardViewModel by viewModels()

    /** The app is Italian only: date and time pickers must not follow the phone's language into English. */
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration).apply { setLocale(Locale.ITALIAN) }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupTrace.mark("MainActivity.onCreate begin")
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep the splash up until there is a list to show (a spinner flash is worse than 100 ms of splash), but never
        // for long: if something is slow the user must see the app.
        val begin = SystemClock.elapsedRealtime()
        splash.setKeepOnScreenCondition { !board.state.value.hasContent && SystemClock.elapsedRealtime() - begin < SPLASH_MAX_MS }
        // Start computing the first list now instead of when the screen subscribes; the state outlives this for 5 s.
        lifecycleScope.launch { board.state.first { it.hasContent } }

        setContent {
            CatchitTheme {
                CatchitApp()
            }
        }
        StartupTrace.mark("MainActivity.onCreate end")
    }

    private companion object {
        const val SPLASH_MAX_MS = 800L
    }
}
