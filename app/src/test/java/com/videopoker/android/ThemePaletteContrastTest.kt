package com.videopoker.android

import androidx.compose.ui.graphics.Color
import com.videopoker.android.ui.theme.AppTheme
import com.videopoker.android.ui.theme.ThemeManager
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ThemePaletteContrastTest {

    private fun relativeLuminance(color: Color): Double {
        fun linearize(c: Float): Double {
            val d = c.toDouble()
            return if (d <= 0.03928) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)
    }

    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun allThemesMeetWcagAaFooterContrast() {
        AppTheme.entries.forEach { theme ->
            val palette = ThemeManager.getPalette(theme)
            val ratio = contrastRatio(palette.footerTextColor, palette.backgroundColorEnd)
            assertTrue(
                "Footer contrast for ${theme.displayName} is too low: $ratio:1",
                ratio >= 4.5
            )
        }
    }
}
