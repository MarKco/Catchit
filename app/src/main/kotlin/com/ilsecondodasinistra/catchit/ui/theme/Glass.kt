package com.ilsecondodasinistra.catchit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/** Frosted glass over whatever is registered as haze source in [state]. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.glass(state: HazeState): Modifier =
    hazeEffect(state = state, style = HazeMaterials.thin(MaterialTheme.colorScheme.surface))
