package com.ilsecondodasinistra.catchit

import android.content.Context
import android.content.Intent
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
        // A launcher shortcut opens straight on its destination (not again after a rotation: the choice is saved then).
        if (savedInstanceState == null) board.openDestination(intent.destinationExtra())

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

    /** The activity is `singleTop`: a shortcut used while the app is open lands here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        board.openDestination(intent.destinationExtra())
    }

    private fun Intent?.destinationExtra(): String? =
        this?.takeIf { it.action == ACTION_OPEN_DESTINATION }?.getStringExtra(EXTRA_DESTINATION)

    companion object {
        const val ACTION_OPEN_DESTINATION = "com.ilsecondodasinistra.catchit.OPEN_DESTINATION"
        const val EXTRA_DESTINATION = "destination"
        private const val SPLASH_MAX_MS = 800L
    }
}
