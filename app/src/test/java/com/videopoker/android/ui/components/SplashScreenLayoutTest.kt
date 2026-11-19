package com.videopoker.android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashScreenLayoutTest {

    @Test
    fun `splash subtitle shows Strategic Trainer without game count`() {
        assertEquals("STRATEGIC TRAINER", SPLASH_SUBTITLE)
        assertFalse(SPLASH_SUBTITLE.any { it.isDigit() })
    }

    @Test
    fun `phone and tablet splash metrics scale cleanly inside viewport bounds`() {
        val phone = calculateSplashLayoutMetrics(viewportWidthDp = 914f, viewportHeightDp = 411f)
        val tablet = calculateSplashLayoutMetrics(viewportWidthDp = 1280f, viewportHeightDp = 800f)

        assertTrue(phone.scale in 0.9f..1.1f)
        assertTrue(tablet.scale > phone.scale)
        assertTrue(tablet.borderInsetDp > phone.borderInsetDp)
        assertTrue(tablet.titleSizeSp > phone.titleSizeSp)
        assertTrue(phone.heroFillFractionH in 0.8f..0.95f)
        assertTrue(tablet.heroFillFractionH in 0.8f..0.95f)
    }

    @Test
    fun `splash screen omits tap-to-deal prompt and version badge`() {
        val splashSource = java.io.File("src/main/java/com/videopoker/android/ui/components/StartupSplashScreen.kt").readText()
        assertFalse(splashSource.contains("Tap anywhere to deal"))
        assertFalse(splashSource.contains("appVersion"))
    }
}
