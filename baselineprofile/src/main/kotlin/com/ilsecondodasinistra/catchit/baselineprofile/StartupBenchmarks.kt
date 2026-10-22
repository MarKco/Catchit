package com.ilsecondodasinistra.catchit.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold start with and without the baseline profile, to see what it is worth:
 * `./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest`, results in the test report and Android Studio.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmarks {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartWithoutProfile() = startup(CompilationMode.None())

    @Test
    fun coldStartWithBaselineProfile() = startup(CompilationMode.Partial())

    private fun startup(mode: CompilationMode) = rule.measureRepeated(
        packageName = "com.ilsecondodasinistra.catchit",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.COLD,
        iterations = 10,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        // The list is what the user came for.
        device.wait(Until.hasObject(By.text("verso Venezia")), 8_000)
    }
}
