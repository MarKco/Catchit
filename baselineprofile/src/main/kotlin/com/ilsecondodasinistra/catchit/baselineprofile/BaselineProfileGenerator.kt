package com.ilsecondodasinistra.catchit.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records what the app runs while starting and while it is used in the usual ways, so that those code paths are
 * compiled ahead of time when the app is installed.
 *
 * Run: `./gradlew :app:generateBaselineProfile` (a device or emulator must be connected).
 * The profile lands in `app/src/main/generated/baselineProfiles/`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndMainJourneys() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        // Cold start up to the first list.
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Andata")), TIMEOUT)

        // Scroll the list, switch to the other leg and back.
        swipeList(device)
        device.findObject(By.text("Ritorno"))?.click()
        device.waitForIdle()
        device.findObject(By.text("Andata"))?.click()
        device.waitForIdle()

        // Open the sheet of a passage and close it.
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        device.wait(Until.hasObject(By.text("Partenza")), TIMEOUT)
        device.pressBack()
        device.waitForIdle()

        // The other destinations of the bottom bar.
        for (label in listOf("Venezia", "Stazione", "Aeroporto", "Centro")) {
            device.findObject(By.text(label))?.click()
            device.waitForIdle()
        }

        // Menu: the day picker, then settings.
        device.findObject(By.desc("Altre azioni"))?.click()
        device.wait(Until.hasObject(By.text("Impostazioni")), TIMEOUT)
        device.findObject(By.text("Scegli giorno e ora"))?.click()
        device.wait(Until.hasObject(By.text("Annulla")), TIMEOUT)
        device.findObject(By.text("Annulla"))?.click()
        device.waitForIdle()

        device.findObject(By.desc("Altre azioni"))?.click()
        device.wait(Until.hasObject(By.text("Impostazioni")), TIMEOUT)
        device.findObject(By.text("Impostazioni"))?.click()
        device.wait(Until.hasObject(By.text("Barra in basso")), TIMEOUT)
        swipeList(device)
        device.pressBack()
        device.waitForIdle()
    }

    private fun swipeList(device: UiDevice) {
        val x = device.displayWidth / 2
        repeat(2) {
            device.swipe(x, device.displayHeight * 3 / 4, x, device.displayHeight / 3, 20)
            device.waitForIdle()
        }
    }

    private companion object {
        const val PACKAGE = "com.ilsecondodasinistra.catchit"
        const val TIMEOUT = 8_000L
    }
}
