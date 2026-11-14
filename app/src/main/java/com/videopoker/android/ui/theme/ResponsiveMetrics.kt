package com.videopoker.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Runtime-calculated layout metrics calibrated to a 914x411dp phone landscape baseline
 * and proportionally scaled for larger/taller displays such as the 1280x800dp Pixel Tablet.
 */
data class ResponsiveMetrics(
    val viewportWidth: Dp,
    val viewportHeight: Dp,
    val contentAvailWidth: Dp,
    val contentAvailHeight: Dp,
    val outerPadH: Dp,
    val outerPadV: Dp,
    val uiScale: Float,
    val topWeight: Float,
    val midWeight: Float,
    val bottomWeight: Float,
    val actionBarWidth: Dp,
    val hudContainerWidth: Dp,
    val feltRailWidth: Dp
) {
    fun s(baseDp: Float): Dp = (baseDp * uiScale).dp
}

data class CardDeckLayout(
    val cardWidth: Dp,
    val cardHeight: Dp,
    val spacing: Dp,
    val railPadH: Dp,
    val railPadTop: Dp,
    val railPadBottom: Dp,
    val plaqueSpacer: Dp,
    val feltRailWidth: Dp,
    val cardScale: Float
)

val LocalResponsiveMetrics = staticCompositionLocalOf {
    calculateResponsiveMetrics(viewportWidthDp = 914f, viewportHeightDp = 411f)
}

object ResponsiveLayoutCalculator {
    const val REF_PHONE_W = 914f
    const val REF_PHONE_H = 411f

    const val BASE_CARD_W = 100f
    const val BASE_CARD_H = 140f
    const val CARD_ASPECT_W_OVER_H = 5f / 7f

    const val BASE_ACTION_BAR_W = 786f
    const val BASE_ACTION_BAR_H = 40f
    const val BASE_HUD_H = 20f
    const val BASE_FOOTER_H = 16f
    const val BASE_BOTTOM_GAPS_H = 5f
}

fun calculateResponsiveMetrics(viewportWidthDp: Float, viewportHeightDp: Float): ResponsiveMetrics {
    val safeW = viewportWidthDp.coerceAtLeast(360f)
    val safeH = viewportHeightDp.coerceAtLeast(280f)

    val isExpandedWidth = safeW >= 1050f && safeH >= 550f
    val outerPadH = if (isExpandedWidth) 14f else 8f
    val outerPadV = if (isExpandedWidth) 6f else 2f

    val contentAvailW = (safeW - outerPadH * 2f).coerceAtLeast(320f)
    val contentAvailH = (safeH - outerPadV * 2f).coerceAtLeast(240f)

    val widthFactor = safeW / ResponsiveLayoutCalculator.REF_PHONE_W
    val heightFactor = safeH / ResponsiveLayoutCalculator.REF_PHONE_H

    val maxScaleByActionBar = (contentAvailW * 0.96f) / ResponsiveLayoutCalculator.BASE_ACTION_BAR_W
    val uiScale = minOf(widthFactor * 1.10f, heightFactor, maxScaleByActionBar)
        .coerceIn(0.84f, 1.56f)

    val neededBottomH = (
        ResponsiveLayoutCalculator.BASE_HUD_H +
            ResponsiveLayoutCalculator.BASE_ACTION_BAR_H +
            ResponsiveLayoutCalculator.BASE_FOOTER_H +
            ResponsiveLayoutCalculator.BASE_BOTTOM_GAPS_H
        ) * uiScale

    val bottomFraction = (neededBottomH / contentAvailH).coerceIn(0.155f, 0.22f)
    val remainingFraction = 1f - bottomFraction
    // Preserve the authentic phone ratio (140 : 190 = 0.4242 : 0.5758) between top & mid decks
    val topFraction = remainingFraction * (140f / 330f)
    val midFraction = remainingFraction * (190f / 330f)

    val estimatedMidH = contentAvailH * midFraction
    val estimatedDeck = calculateCardDeckLayout(
        availWidthDp = contentAvailW,
        availHeightDp = estimatedMidH,
        uiScale = uiScale
    )

    val actionBarWidth = (ResponsiveLayoutCalculator.BASE_ACTION_BAR_W * uiScale)
        .coerceAtMost(contentAvailW)
    val hudContainerWidth = maxOf(estimatedDeck.feltRailWidth.value, actionBarWidth)
        .coerceAtMost(contentAvailW)

    return ResponsiveMetrics(
        viewportWidth = safeW.dp,
        viewportHeight = safeH.dp,
        contentAvailWidth = contentAvailW.dp,
        contentAvailHeight = contentAvailH.dp,
        outerPadH = outerPadH.dp,
        outerPadV = outerPadV.dp,
        uiScale = uiScale,
        topWeight = topFraction * 412f,
        midWeight = midFraction * 412f,
        bottomWeight = bottomFraction * 412f,
        actionBarWidth = actionBarWidth.dp,
        hudContainerWidth = hudContainerWidth.dp,
        feltRailWidth = estimatedDeck.feltRailWidth
    )
}

fun calculateCardDeckLayout(
    availWidthDp: Float,
    availHeightDp: Float,
    uiScale: Float
): CardDeckLayout {
    val plaqueH = 24f * uiScale
    val plaqueSpacer = (3f * uiScale).coerceIn(2f, 8f)
    val railPadH = (10f * uiScale).coerceIn(8f, 18f)
    val railPadTop = (13f * uiScale).coerceIn(11f, 22f)
    val railPadBottom = (5f * uiScale).coerceIn(4f, 10f)
    val spacing = (8f * uiScale).coerceIn(6f, 15f)
    val borderExtra = 4f // 2dp border top + 2dp bottom

    val maxCardHFromH = (availHeightDp - plaqueH - plaqueSpacer - railPadTop - railPadBottom - borderExtra)
        .coerceAtLeast(72f)
    val maxRailW = availWidthDp * 0.94f
    val maxCardWFromW = ((maxRailW - railPadH * 2f - spacing * 4f) / 5f)
        .coerceAtLeast(52f)

    val cardW = minOf(
        maxCardWFromW,
        maxCardHFromH * ResponsiveLayoutCalculator.CARD_ASPECT_W_OVER_H
    )
    val cardH = cardW / ResponsiveLayoutCalculator.CARD_ASPECT_W_OVER_H
    val feltRailWidth = cardW * 5f + spacing * 4f + railPadH * 2f
    val cardScale = cardW / ResponsiveLayoutCalculator.BASE_CARD_W

    return CardDeckLayout(
        cardWidth = cardW.dp,
        cardHeight = cardH.dp,
        spacing = spacing.dp,
        railPadH = railPadH.dp,
        railPadTop = railPadTop.dp,
        railPadBottom = railPadBottom.dp,
        plaqueSpacer = plaqueSpacer.dp,
        feltRailWidth = feltRailWidth.dp,
        cardScale = cardScale
    )
}
