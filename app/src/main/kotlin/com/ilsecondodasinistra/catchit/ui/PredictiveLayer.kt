package com.ilsecondodasinistra.catchit.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private const val ENTER_MS = 300
private const val EXIT_MS = 220

/**
 * A full-screen layer over the screen below (which stays composed, so it keeps its state and its polling). It fades and
 * scales in; leaving (a button calling `dismiss`, or back) plays it backwards, and the system's back gesture drives it
 * while the finger moves (predictive back), the screen below showing through. [onBack] runs once it is gone.
 *
 * [content] gets `entered` (true once the layer has finished coming in: heavy views are better built then, so they
 * don't stall the animation) and `dismiss`.
 */
@Composable
fun PredictiveLayer(onBack: () -> Unit, content: @Composable (entered: Boolean, dismiss: () -> Unit) -> Unit) {
    val appear = remember { Animatable(0f) }
    val drag = remember { Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    val currentOnBack by rememberUpdatedState(onBack)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(ENTER_MS, easing = FastOutSlowInEasing))
        entered = true
    }
    suspend fun leave() {
        if (leaving) return
        leaving = true
        appear.animateTo(0f, tween(EXIT_MS, easing = FastOutLinearInEasing))
        currentOnBack()
    }
    PredictiveBackHandler(enabled = !leaving) { progress ->
        try {
            progress.collect { drag.snapTo(it.progress) }
            leave()
        } catch (cancelled: CancellationException) {
            drag.animateTo(0f, tween(EXIT_MS))
            throw cancelled
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize().graphicsLayer {
            val scale = (0.94f + 0.06f * appear.value) * (1f - 0.1f * drag.value)
            scaleX = scale
            scaleY = scale
            alpha = appear.value
            shape = RoundedCornerShape((28 * drag.value).dp)
            clip = drag.value > 0f
        },
    ) {
        content(entered) { scope.launch { leave() } }
    }
}
