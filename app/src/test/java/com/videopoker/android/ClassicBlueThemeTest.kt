package com.videopoker.android

import androidx.compose.ui.graphics.Color
import com.videopoker.android.ui.theme.ThemeManager
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ClassicBlueThemeTest {

    private fun linearize(channel: Float): Double {
        val c = channel.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun relativeLuminance(c: Color): Double =
        0.2126 * linearize(c.red) + 0.7152 * linearize(c.green) + 0.0722 * linearize(c.blue)

    private fun contrast(fg: Color, bg: Color): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    @Test
    fun classicBlueMatchesCabinetContrastAndTokens() {
        val p = ThemeManager.ClassicPalette

        assertTrue(
            "Yellow button text contrast on buttonBgStart must exceed WCAG AA (4.5)",
            contrast(p.buttonTextColor, p.buttonBgStart) >= 4.5
        )
        assertTrue(
            "Yellow button text contrast on buttonBgEnd must exceed WCAG AA (4.5)",
            contrast(p.buttonTextColor, p.buttonBgEnd) >= 4.5
        )
        assertTrue(
            "Active coin column yellow numeral contrast on crimson red bg must exceed WCAG AA (4.5)",
            contrast(p.payTableActiveColText, p.payTableActiveColBg) >= 4.5
        )
        assertTrue(
            "Paytable yellow text contrast on deep cobalt panelColor must exceed WCAG AA (4.5)",
            contrast(p.payTableTextColor, p.panelColor) >= 4.5
        )
        assertTrue(
            "Deal button white text contrast on crimson red bg must exceed WCAG AA (4.5)",
            contrast(p.dealButtonTextColor, p.dealButtonBgStart) >= 4.5 &&
                contrast(p.dealButtonTextColor, p.dealButtonBgEnd) >= 4.5
        )
        assertTrue(
            "Footer text & version contrast on backgroundColorEnd must exceed WCAG AA (4.5)",
            contrast(p.footerTextColor, p.backgroundColorEnd) >= 4.5 &&
                contrast(p.footerVersionColor, p.backgroundColorEnd) >= 4.5
        )
    }
}
