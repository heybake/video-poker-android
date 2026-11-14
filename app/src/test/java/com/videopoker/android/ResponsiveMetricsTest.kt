package com.videopoker.android

import com.videopoker.android.ui.theme.ResponsiveLayoutCalculator
import com.videopoker.android.ui.theme.calculateCardDeckLayout
import com.videopoker.android.ui.theme.calculateModalLayout
import com.videopoker.android.ui.theme.calculateResponsiveMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveMetricsTest {

    @Test
    fun pixel10ProBaselinePreservesPhoneProportions() {
        val metrics = calculateResponsiveMetrics(viewportWidthDp = 914f, viewportHeightDp = 411f)
        assertEquals(1.0f, metrics.uiScale, 0.01f)
        assertTrue(metrics.actionBarWidth.value <= metrics.contentAvailWidth.value)
        assertTrue(metrics.feltRailWidth.value <= metrics.contentAvailWidth.value)

        val midAvailH = metrics.contentAvailHeight.value *
            (metrics.midWeight / (metrics.topWeight + metrics.midWeight + metrics.bottomWeight))
        val deck = calculateCardDeckLayout(
            availWidthDp = metrics.contentAvailWidth.value,
            availHeightDp = midAvailH,
            uiScale = metrics.uiScale
        )
        assertTrue(deck.cardWidth.value in 98f..105f)
        assertEquals(deck.cardWidth.value / deck.cardHeight.value, ResponsiveLayoutCalculator.CARD_ASPECT_W_OVER_H, 0.005f)
    }

    @Test
    fun pixelTabletScalesProportionalCardsHudAndControls() {
        val phone = calculateResponsiveMetrics(viewportWidthDp = 914f, viewportHeightDp = 411f)
        val tablet = calculateResponsiveMetrics(viewportWidthDp = 1280f, viewportHeightDp = 800f)

        assertTrue("Expected tablet uiScale > 1.4x, was ${tablet.uiScale}", tablet.uiScale >= 1.45f)
        assertTrue(tablet.actionBarWidth.value <= tablet.contentAvailWidth.value)
        assertTrue(tablet.hudContainerWidth.value >= tablet.feltRailWidth.value)
        assertTrue(tablet.hudContainerWidth.value <= tablet.contentAvailWidth.value)

        val tabletMidH = tablet.contentAvailHeight.value *
            (tablet.midWeight / (tablet.topWeight + tablet.midWeight + tablet.bottomWeight))
        val tabletDeck = calculateCardDeckLayout(
            availWidthDp = tablet.contentAvailWidth.value,
            availHeightDp = tabletMidH,
            uiScale = tablet.uiScale
        )

        val phoneMidH = phone.contentAvailHeight.value *
            (phone.midWeight / (phone.topWeight + phone.midWeight + phone.bottomWeight))
        val phoneDeck = calculateCardDeckLayout(
            availWidthDp = phone.contentAvailWidth.value,
            availHeightDp = phoneMidH,
            uiScale = phone.uiScale
        )

        assertTrue(
            "Expected tablet card width (${tabletDeck.cardWidth}) to be ~2x phone card width (${phoneDeck.cardWidth})",
            tabletDeck.cardWidth.value >= phoneDeck.cardWidth.value * 1.9f
        )
        assertEquals(
            tabletDeck.cardWidth.value / tabletDeck.cardHeight.value,
            ResponsiveLayoutCalculator.CARD_ASPECT_W_OVER_H,
            0.005f
        )
        assertTrue(tabletDeck.feltRailWidth.value <= tablet.contentAvailWidth.value)
    }

    @Test
    fun compactViewportNeverOverflowsHorizontally() {
        val compact = calculateResponsiveMetrics(viewportWidthDp = 800f, viewportHeightDp = 360f)
        assertTrue(compact.actionBarWidth.value <= compact.contentAvailWidth.value)
        assertTrue(compact.feltRailWidth.value <= compact.contentAvailWidth.value)
    }

    @Test
    fun modalLayoutsScaleProportionallyAcrossPhoneAndTablet() {
        val phone = calculateResponsiveMetrics(viewportWidthDp = 914f, viewportHeightDp = 411f)
        val tablet = calculateResponsiveMetrics(viewportWidthDp = 1280f, viewportHeightDp = 800f)

        val phoneModal = calculateModalLayout(phone, isWideList = false)
        val tabletModal = calculateModalLayout(tablet, isWideList = false)
        val tabletWideModal = calculateModalLayout(tablet, isWideList = true)

        assertTrue("Phone modal width ratio out of range", phoneModal.width.value / 914f in 0.72f..0.85f)
        assertTrue("Phone modal height ratio out of range", phoneModal.height.value / 411f in 0.75f..0.88f)
        assertTrue("Tablet modal width ratio out of range", tabletModal.width.value / 1280f in 0.75f..0.86f)
        assertTrue("Tablet modal height ratio out of range", tabletModal.height.value / 800f in 0.75f..0.86f)
        assertTrue("Tablet wide list modal should exceed standard modal width", tabletWideModal.width.value > tabletModal.width.value)

        assertTrue("Expected tablet modal scale >= 1.5x, was ${tabletModal.scale}", tabletModal.scale >= 1.5f)
        assertTrue("Phone modal scale should stay near 1.0x, was ${phoneModal.scale}", phoneModal.scale in 0.95f..1.10f)
    }
}
