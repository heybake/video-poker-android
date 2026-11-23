package com.videopoker.android.ui.decks

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.HandRank
import com.videopoker.android.data.models.PayTableRow
import com.videopoker.android.ui.components.MultiHandHoldPanel
import com.videopoker.android.ui.components.SessionBankrollPanel
import com.videopoker.android.ui.state.DealState
import com.videopoker.android.ui.state.GameMode
import com.videopoker.android.ui.state.GameState
import com.videopoker.android.ui.state.MultiHandLine
import com.videopoker.android.ui.theme.CardDeck
import com.videopoker.android.ui.theme.LocalResponsiveMetrics
import com.videopoker.android.ui.theme.ThemePalette
import kotlin.math.ceil

internal fun shouldShowMainDrawBadge(state: GameState): Boolean =
    state.isMainLineSettled &&
        state.mainLineWinCredits > 0 &&
        state.mainLineWinRank != HandRank.NOTHING &&
        !state.hideMultiWinBadges &&
        (state.isRevealingMultiDraw || state.dealState != DealState.DEALT_CHOOSING_HOLDS)

@Composable
fun MultiHandArena(
    state: GameState,
    palette: ThemePalette,
    onToggleHold: (Int) -> Unit,
    onSetHold: (Int, Boolean) -> Unit,
    onOpenSeePays: () -> Unit,
    onOpenCompleteList: () -> Unit = {},
    onSkipReveal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentHeldMask by rememberUpdatedState(state.heldMask)
    val currentDealState by rememberUpdatedState(state.dealState)
    val currentIsAnimating by rememberUpdatedState(state.isAnimating)
    val currentIsRevealing by rememberUpdatedState(state.isRevealingMultiDraw)
    val currentOnSetHold by rememberUpdatedState(onSetHold)
    val currentOnSkipReveal by rememberUpdatedState(onSkipReveal)
    val metrics = LocalResponsiveMetrics.current
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (state.isRevealingMultiDraw) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSkipReveal
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = metrics.s(4f), vertical = metrics.s(2f)),
        contentAlignment = Alignment.Center
    ) {
        val availW = maxWidth.value.coerceAtLeast(360f)
        val availH = maxHeight.value.coerceAtLeast(220f)

        val sideGapDp = (6f * metrics.uiScale).coerceIn(4f, 10f)
        val payColMaxW = (availW * 0.215f).coerceIn(170f, 285f)
        val centerMaxW = (availW - payColMaxW * 2f - 16f).coerceAtLeast(220f)

        val railPadHDp = (8f * metrics.uiScale).coerceIn(6f, 14f)
        val railPadTopDp = (9f * metrics.uiScale).coerceIn(6f, 15f)
        val railPadBotDp = (5f * metrics.uiScale).coerceIn(4f, 9f)
        val mainGapDp = (6f * metrics.uiScale).coerceIn(4f, 11f)
        val dockSpaceDp = (21f * metrics.uiScale).coerceIn(18f, 32f)
        val midGapDp = (9f * metrics.uiScale).coerceIn(6f, 16f)

        // Consistent bottom main hand geometry across 3-Hand, 5-Hand, and 10-Hand
        val maxMainCardH = (availH * 0.275f).coerceIn(60f, 162f)
        val maxMainCardWFromW = ((centerMaxW * 0.92f - railPadHDp * 2f - mainGapDp * 4f) / 5f).coerceAtLeast(40f)
        val mainCardW = minOf(maxMainCardWFromW, maxMainCardH * 5f / 7f)
        val mainCardH = mainCardW * 7f / 5f
        val mainRailW = mainCardW * 5f + mainGapDp * 4f + railPadHDp * 2f
        val mainRailTotalH = mainCardH + railPadTopDp + railPadBotDp + 4f

        val cornerPanelH = (mainRailTotalH + dockSpaceDp + 14f * metrics.uiScale).coerceIn(124f, availH * 0.44f)
        val splitPayAvailH = (availH - cornerPanelH - sideGapDp).coerceAtLeast(80f)

        val upperAvailH = (availH - mainRailTotalH - midGapDp - dockSpaceDp - 4f).coerceAtLeast(90f)
        val hands = state.handsPerDeal.coerceIn(3, 10)

        val badgeW = (mainRailW * 0.34f).coerceIn(96f, 186f).dp
        val badgeH = (mainRailW * 0.056f).coerceIn(18.5f, 33f).dp

        val rows = state.currentGame?.payTable?.rows ?: emptyList()
        val splitAt = rows.size / 2
        val leftRows = rows.subList(0, splitAt)
        val rightRows = rows.subList(splitAt, rows.size)

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Side: Top Split Pay Column + Bottom Compact Session Bankroll Plot
            Column(
                modifier = Modifier
                    .width(payColMaxW.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SplitPayColumn(
                    rows = leftRows,
                    currentBet = state.currentBet,
                    isCompact = rows.size > 10,
                    availHeightDp = splitPayAvailH,
                    uiScale = metrics.uiScale,
                    palette = palette,
                    onClick = onOpenSeePays
                )

                SessionBankrollPanel(
                    points = state.bankrollHistory,
                    targets = state.bankrollTargets,
                    cashBalance = state.cashBalance,
                    palette = palette,
                    showCompleteListButton = false,
                    showReviewToggleButton = false,
                    showMistakesSummary = shouldShowBankrollMistakes(state.currentMode),
                    compact = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cornerPanelH.dp)
                )
            }

            // Center Field: Upper Grid + Consistent Bottom Main Hand
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(bottom = dockSpaceDp.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    UpperHandsGrid(
                        handsPerDeal = hands,
                        upperLines = state.upperLines,
                        betPerHand = state.currentBet,
                        targetWidthDp = mainRailW,
                        availHeightDp = upperAvailH,
                        uiScale = metrics.uiScale,
                        badgeW = badgeW,
                        badgeH = badgeH,
                        hideWinBadges = state.hideMultiWinBadges || state.dealState == DealState.READY_TO_DEAL,
                        palette = palette,
                        cardDeck = state.currentCardDeck
                    )
                }

                Spacer(modifier = Modifier.height(midGapDp.dp))

                // Bottom Hand Felt Rail + Inner Gold Pinstripe + Swipe-to-Hold + Badges
                val railRadius = (11f * metrics.uiScale).dp
                val cardW = mainCardW.dp
                val cardH = mainCardH.dp
                val spacing = mainGapDp.dp
                val cardWidthPx = with(density) { cardW.toPx() }
                val spacingPx = with(density) { spacing.toPx() }
                val pitchPx = cardWidthPx + spacingPx
                val totalDeckWidthPx = with(density) { (cardW * 5 + spacing * 4).toPx() }

                val showDealtBadge = state.dealState == DealState.DEALT_CHOOSING_HOLDS &&
                    !state.isAnimating &&
                    state.dealtWinCredits > 0 &&
                    state.dealtPayingRank != HandRank.NOTHING &&
                    !state.hideMultiWinBadges

                val showMainDrawBadge = shouldShowMainDrawBadge(state)

                val dockTargetY = ((mainCardH + railPadBotDp) / 2f + badgeH.value / 2f + 4f).dp
                val badgeOffsetY by animateDpAsState(
                    targetValue = if (state.dealtBadgeDocked && showDealtBadge) dockTargetY else 0.dp,
                    animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                    label = "dealtBadgeSlideDown"
                )

                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(elevation = (5f * metrics.uiScale).dp, shape = RoundedCornerShape(railRadius))
                            .clip(RoundedCornerShape(railRadius))
                            .background(
                                Brush.radialGradient(
                                    listOf(palette.feltColor, palette.feltColorEnd),
                                    radius = 700f * metrics.uiScale
                                )
                            )
                            .border(2.dp, palette.railBorderColor, RoundedCornerShape(railRadius))
                    ) {
                        // Subtle inner gold pinstripe for crisp contrast on dark themes like Noir Ace
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(3.dp)
                                .border(
                                    1.dp,
                                    palette.goldColor.copy(alpha = 0.50f),
                                    RoundedCornerShape((8f * metrics.uiScale).dp)
                                )
                        )

                        Row(
                            modifier = Modifier
                                .padding(
                                    start = railPadHDp.dp,
                                    end = railPadHDp.dp,
                                    top = railPadTopDp.dp,
                                    bottom = railPadBotDp.dp
                                )
                                .pointerInput(pitchPx, totalDeckWidthPx) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        if (currentIsRevealing) {
                                            currentOnSkipReveal()
                                            return@awaitEachGesture
                                        }
                                        if (currentDealState != DealState.DEALT_CHOOSING_HOLDS || currentIsAnimating) {
                                            return@awaitEachGesture
                                        }
                                        val x = down.position.x
                                        if (x < 0f || x > totalDeckWidthPx) return@awaitEachGesture

                                        val startSlot = (x / pitchPx).toInt().coerceIn(0, 4)
                                        val currentHeld = currentHeldMask.getOrElse(startSlot) { false }
                                        val targetHold = !currentHeld
                                        currentOnSetHold(startSlot, targetHold)
                                        var lastSlot = startSlot

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) break
                                            val curSlot = (change.position.x / pitchPx).toInt().coerceIn(0, 4)
                                            if (curSlot != lastSlot) {
                                                val minS = minOf(lastSlot, curSlot)
                                                val maxS = maxOf(lastSlot, curSlot)
                                                for (s in minS..maxS) {
                                                    currentOnSetHold(s, targetHold)
                                                }
                                                lastSlot = curSlot
                                            }
                                            change.consume()
                                        }
                                    }
                                },
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val showDots = (
                                state.currentMode == GameMode.LEARN &&
                                    state.dealState == DealState.DEALT_CHOOSING_HOLDS &&
                                    !state.isAnimating &&
                                    !state.isRevealingMultiDraw
                                )
                            for (ci in 0 until 5) {
                                val revealed = state.revealedMask.getOrElse(ci) { true }
                                val card = if (revealed) state.cards.getOrNull(ci) else null
                                val isHeld = state.heldMask.getOrElse(ci) { false } && revealed
                                val choicesHoldingCard = if (showDots && revealed) {
                                    state.holdChoices.take(3).mapNotNull { choice ->
                                        if (choice.holdMask.getOrElse(ci) { false }) choice.row else null
                                    }
                                } else {
                                    emptyList()
                                }
                                MultiMainCardSlot(
                                    card = card,
                                    isHeld = isHeld,
                                    choicesHoldingCard = choicesHoldingCard,
                                    betCredits = if (ci == 4) state.currentBet else null,
                                    cardWidth = cardW,
                                    cardHeight = cardH,
                                    palette = palette,
                                    cardDeck = state.currentCardDeck,
                                    onTap = { onToggleHold(ci) }
                                )
                            }
                        }
                    }

                    if (showMainDrawBadge) {
                        UniformWinBadge(
                            rankName = state.mainLineWinRank.displayName,
                            creditsWon = state.mainLineWinCredits,
                            width = badgeW,
                            height = badgeH,
                            palette = palette,
                            uiScale = metrics.uiScale
                        )
                    } else if (showDealtBadge) {
                        UniformWinBadge(
                            rankName = state.dealtPayingRank.displayName,
                            creditsWon = state.dealtWinCredits,
                            width = badgeW,
                            height = badgeH,
                            palette = palette,
                            uiScale = metrics.uiScale,
                            modifier = Modifier.offset(y = badgeOffsetY)
                        )
                    }
                }
            }

            // Right Side: Top Split Pay Column + Bottom Compact Hold / Practice Review Panel (hidden in Casino)
            Column(
                modifier = Modifier
                    .width(payColMaxW.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SplitPayColumn(
                    rows = rightRows,
                    currentBet = state.currentBet,
                    isCompact = rows.size > 10,
                    availHeightDp = splitPayAvailH,
                    uiScale = metrics.uiScale,
                    palette = palette,
                    onClick = onOpenSeePays
                )

                if (state.currentMode != GameMode.CASINO) {
                    val isDealing = state.isAnimating &&
                        state.dealState == DealState.DEALT_CHOOSING_HOLDS &&
                        !state.isRevealingMultiDraw
                    MultiHandHoldPanel(
                        choices = state.holdChoices,
                        playerHeldMask = state.heldMask,
                        dealtCards = state.cards,
                        mode = state.currentMode,
                        dealState = state.dealState,
                        isDealing = isDealing,
                        isRevealingMultiDraw = state.isRevealingMultiDraw,
                        palette = palette,
                        onOpenCompleteList = onOpenCompleteList,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cornerPanelH.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitPayColumn(
    rows: List<PayTableRow>,
    currentBet: Int,
    isCompact: Boolean,
    availHeightDp: Float,
    uiScale: Float,
    palette: ThemePalette,
    onClick: () -> Unit
) {
    if (rows.isEmpty()) return
    val baseGap = if (isCompact) 3.5f else 5.5f
    val baseCardH = if (isCompact) 30f else 36f
    val estNeeded = rows.size * baseCardH * uiScale + (rows.size - 1).coerceAtLeast(0) * baseGap * uiScale
    val fitFactor = if (estNeeded > availHeightDp - 4f) {
        ((availHeightDp - 4f) / estNeeded).coerceIn(0.62f, 1f)
    } else {
        1f
    }
    val s = uiScale * fitFactor
    val gap = (baseGap * s).dp
    val corner = (5f * s).coerceIn(3f, 8f).dp
    val borderColor = if (palette.theme.id == "parchment") palette.panelBorderSecondary else palette.panelBorderColor

    Column(
        modifier = Modifier.width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            val payout = row.values.getOrElse((currentBet - 1).coerceIn(0, 4)) { 0 }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = (76f * uiScale).dp)
                    .shadow((2f * s).dp, RoundedCornerShape(corner))
                    .clip(RoundedCornerShape(corner))
                    .background(palette.panelColor)
                    .border(1.2.dp, borderColor, RoundedCornerShape(corner))
                    .clickable(onClick = onClick),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = row.label,
                    color = palette.payTableTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = (6f * s).dp, vertical = (2f * s).dp),
                    style = TextStyle(
                        fontSize = ((if (isCompact) 8.5f else 9.4f) * s).sp,
                        fontWeight = FontWeight.ExtraBold,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(palette.splitPayValueBgStart, palette.splitPayValueBgEnd)
                            )
                        )
                        .border(0.75.dp, palette.goldColor.copy(alpha = 0.65f))
                        .padding(horizontal = (6f * s).dp, vertical = (1.8f * s).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = payout.toString(),
                        color = palette.splitPayValueTextColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = ((if (isCompact) 10f else 11.2f) * s).sp,
                            fontWeight = FontWeight.Black,
                            fontFeatureSettings = "tnum",
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun UpperHandsGrid(
    handsPerDeal: Int,
    upperLines: List<MultiHandLine>,
    betPerHand: Int,
    targetWidthDp: Float,
    availHeightDp: Float,
    uiScale: Float,
    badgeW: Dp,
    badgeH: Dp,
    hideWinBadges: Boolean,
    palette: ThemePalette,
    cardDeck: CardDeck
) {
    val linesCount = handsPerDeal - 1
    val safeLines = List(linesCount) { idx -> upperLines.getOrNull(idx) ?: MultiHandLine(index = idx) }

    when (handsPerDeal) {
        3 -> {
            val spacing = (5f * uiScale).coerceIn(3f, 10f)
            val rowGap = (5f * uiScale).coerceIn(3f, 10f)
            val maxW = (targetWidthDp - spacing * 4f) / 5f
            val maxH = (availHeightDp - rowGap) / 2f
            val cardW = minOf(maxW, maxH * 5f / 7f).coerceAtLeast(30f)
            val cardH = cardW * 7f / 5f
            Column(
                verticalArrangement = Arrangement.spacedBy(rowGap.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                safeLines.forEach { line ->
                    UpperSingleRowHand(
                        line = line,
                        betPerHand = betPerHand,
                        cardWidth = cardW.dp,
                        cardHeight = cardH.dp,
                        spacing = spacing.dp,
                        badgeW = badgeW,
                        badgeH = badgeH,
                        hideWinBadges = hideWinBadges,
                        palette = palette,
                        cardDeck = cardDeck,
                        uiScale = uiScale
                    )
                }
            }
        }
        5 -> {
            val colGap = (12f * uiScale).coerceIn(6f, 22f)
            val rowGap = (6f * uiScale).coerceIn(4f, 12f)
            val maxW = (targetWidthDp - colGap) / (2f * 2.60f)
            val maxH = (availHeightDp - rowGap) / 2f
            val cardW = minOf(maxW, maxH * 5f / 7f).coerceAtLeast(28f)
            val cardH = cardW * 7f / 5f
            Column(
                verticalArrangement = Arrangement.spacedBy(rowGap.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (r in 0 until 2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(colGap.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in 0 until 2) {
                            val line = safeLines[r * 2 + c]
                            UpperFannedHand(
                                line = line,
                                betPerHand = betPerHand,
                                cardWidth = cardW.dp,
                                cardHeight = cardH.dp,
                                badgeW = badgeW,
                                badgeH = badgeH,
                                hideWinBadges = hideWinBadges,
                                palette = palette,
                                cardDeck = cardDeck,
                                uiScale = uiScale
                            )
                        }
                    }
                }
            }
        }
        else -> {
            val colGap = (9f * uiScale).coerceIn(4f, 18f)
            val rowGap = (4f * uiScale).coerceIn(3f, 9f)
            val maxW = (targetWidthDp - colGap * 2f) / (3f * 2.60f)
            val maxH = (availHeightDp - rowGap * 2f) / 3f
            val cardW = minOf(maxW, maxH * 5f / 7f).coerceAtLeast(22f)
            val cardH = cardW * 7f / 5f
            Column(
                verticalArrangement = Arrangement.spacedBy(rowGap.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (r in 0 until 3) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(colGap.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in 0 until 3) {
                            val line = safeLines[r * 3 + c]
                            UpperFannedHand(
                                line = line,
                                betPerHand = betPerHand,
                                cardWidth = cardW.dp,
                                cardHeight = cardH.dp,
                                badgeW = badgeW,
                                badgeH = badgeH,
                                hideWinBadges = hideWinBadges,
                                palette = palette,
                                cardDeck = cardDeck,
                                uiScale = uiScale
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpperSingleRowHand(
    line: MultiHandLine,
    betPerHand: Int,
    cardWidth: Dp,
    cardHeight: Dp,
    spacing: Dp,
    badgeW: Dp,
    badgeH: Dp,
    hideWinBadges: Boolean,
    palette: ThemePalette,
    cardDeck: CardDeck,
    uiScale: Float
) {
    Box(contentAlignment = Alignment.Center) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (ci in 0 until 5) {
                val card = line.cards.getOrNull(ci)
                Box(modifier = Modifier.size(cardWidth, cardHeight)) {
                    MiniCardSurface(
                        card = card,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        cardDeck = cardDeck
                    )
                    if (ci == 4) {
                        BetCoinBadge(
                            betCredits = betPerHand,
                            size = (13.5f * uiScale).coerceIn(11f, 20f).dp,
                            palette = palette,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
        if (line.isRevealed && line.winCredits > 0 && line.rank != HandRank.NOTHING && !hideWinBadges) {
            UniformWinBadge(
                rankName = line.rank.displayName,
                creditsWon = line.winCredits,
                width = badgeW,
                height = badgeH,
                palette = palette,
                uiScale = uiScale
            )
        }
    }
}

@Composable
private fun UpperFannedHand(
    line: MultiHandLine,
    betPerHand: Int,
    cardWidth: Dp,
    cardHeight: Dp,
    badgeW: Dp,
    badgeH: Dp,
    hideWinBadges: Boolean,
    palette: ThemePalette,
    cardDeck: CardDeck,
    uiScale: Float
) {
    val fanStep = cardWidth * 0.40f
    val totalWidth = cardWidth + fanStep * 4f
    Box(
        modifier = Modifier.size(totalWidth, cardHeight),
        contentAlignment = Alignment.Center
    ) {
        for (ci in 0 until 5) {
            val card = line.cards.getOrNull(ci)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = fanStep * ci)
                    .size(cardWidth, cardHeight)
            ) {
                MiniCardSurface(
                    card = card,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    cardDeck = cardDeck
                )
                if (ci == 4) {
                    BetCoinBadge(
                        betCredits = betPerHand,
                        size = (12.5f * uiScale).coerceIn(10f, 18f).dp,
                        palette = palette,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(1.5.dp)
                    )
                }
            }
        }
        if (line.isRevealed && line.winCredits > 0 && line.rank != HandRank.NOTHING && !hideWinBadges) {
            UniformWinBadge(
                rankName = line.rank.displayName,
                creditsWon = line.winCredits,
                width = badgeW,
                height = badgeH,
                palette = palette,
                uiScale = uiScale
            )
        }
    }
}

@Composable
private fun MiniCardSurface(
    card: Card?,
    cardWidth: Dp,
    cardHeight: Dp,
    cardDeck: CardDeck
) {
    val bmp = rememberCardAssetBitmap(card = card, cardDeck = cardDeck)
    val radius = (cardWidth.value * 0.08f).coerceIn(3f, 8f).dp
    Box(
        modifier = Modifier
            .size(cardWidth, cardHeight)
            .shadow(2.dp, RoundedCornerShape(radius))
            .clip(RoundedCornerShape(radius))
            .background(Color.White)
            .border(0.75.dp, Color(0xAA101830), RoundedCornerShape(radius))
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = card?.toString() ?: "Card back",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun MultiMainCardSlot(
    card: Card?,
    isHeld: Boolean,
    choicesHoldingCard: List<Int>,
    betCredits: Int?,
    cardWidth: Dp,
    cardHeight: Dp,
    palette: ThemePalette,
    cardDeck: CardDeck,
    onTap: () -> Unit
) {
    val bmp = rememberCardAssetBitmap(card = card, cardDeck = cardDeck)
    val cardScale = (cardWidth.value / 72f).coerceIn(0.65f, 2.0f)
    val radius = (6.5f * cardScale).dp

    Box(
        modifier = Modifier.size(cardWidth, cardHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = if (isHeld) (8f * cardScale).dp else (3f * cardScale).dp,
                    shape = RoundedCornerShape(radius)
                )
                .clip(RoundedCornerShape(radius))
                .background(Color.White)
                .then(
                    if (isHeld) {
                        Modifier.border((2.2f * cardScale).coerceAtLeast(1.8f).dp, palette.goldColor, RoundedCornerShape(radius))
                    } else {
                        Modifier.border(0.9.dp, Color(0xFF444444), RoundedCornerShape(radius))
                    }
                )
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = card?.toString() ?: "Card back",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (betCredits != null) {
            BetCoinBadge(
                betCredits = betCredits,
                size = (16.5f * cardScale).coerceIn(13f, 26f).dp,
                palette = palette,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding((3f * cardScale).dp)
            )
        }

        if (choicesHoldingCard.isNotEmpty()) {
            val dotSize = (8.5f * cardScale).coerceIn(7f, 15f).dp
            Row(
                modifier = Modifier
                    .align(if (betCredits != null) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding((3.2f * cardScale).dp),
                horizontalArrangement = Arrangement.spacedBy((2.5f * cardScale).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                choicesHoldingCard.forEach { choiceNum ->
                    val dotColor = when (choiceNum) {
                        1 -> palette.choice1Color
                        2 -> palette.choice2Color
                        else -> palette.choice3Color
                    }
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(1.dp, Color(0xFF222222), CircleShape)
                    )
                }
            }
        }

        if (isHeld) {
            val heldRadius = (3.5f * cardScale).dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-7.5f * cardScale).dp)
                    .shadow((3.5f * cardScale).dp, RoundedCornerShape(heldRadius))
                    .clip(RoundedCornerShape(heldRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(palette.heldTagBgStart, palette.heldTagBgEnd)
                        )
                    )
                    .border((1.1f * cardScale).coerceAtLeast(1f).dp, palette.heldTagBorder, RoundedCornerShape(heldRadius))
                    .padding(horizontal = (7.5f * cardScale).dp, vertical = (1.5f * cardScale).dp)
            ) {
                Text(
                    text = "HELD",
                    color = palette.heldTagTextColor,
                    style = TextStyle(
                        fontSize = (9f * cardScale).sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (0.7f * cardScale).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
private fun BetCoinBadge(
    betCredits: Int,
    size: Dp,
    palette: ThemePalette,
    modifier: Modifier = Modifier
) {
    val fontSize = (size.value * 0.56f).coerceIn(7f, 15f).sp
    Box(
        modifier = modifier
            .size(size)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(palette.coinBgStart, palette.coinBgEnd)))
            .border(1.1.dp, palette.goldColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = betCredits.toString(),
            color = palette.coinTextColor,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

@Composable
private fun UniformWinBadge(
    rankName: String,
    creditsWon: Int,
    width: Dp,
    height: Dp,
    palette: ThemePalette,
    uiScale: Float,
    modifier: Modifier = Modifier
) {
    val corner = (4f * uiScale).coerceIn(3f, 7f).dp
    val textScale = (height.value / 21f).coerceIn(0.82f, 1.55f)
    Row(
        modifier = modifier
            .size(width, height)
            .shadow(4.dp, RoundedCornerShape(corner))
            .clip(RoundedCornerShape(corner))
            .border(1.2.dp, palette.goldColor, RoundedCornerShape(corner)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(0.75f)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(palette.dealButtonBgStart, palette.dealButtonBgEnd)
                    )
                )
                .padding(horizontal = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rankName,
                color = palette.dealButtonTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = (8.8f * textScale).sp,
                    fontWeight = FontWeight.Black,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
        Box(
            modifier = Modifier
                .weight(0.25f)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(palette.splitPayValueBgStart, palette.splitPayValueBgEnd)
                    )
                )
                .border(0.75.dp, palette.goldColor.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = creditsWon.toString(),
                color = palette.splitPayValueTextColor,
                maxLines = 1,
                style = TextStyle(
                    fontSize = (9.8f * textScale).sp,
                    fontWeight = FontWeight.Black,
                    fontFeatureSettings = "tnum",
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@Composable
private fun rememberCardAssetBitmap(card: Card?, cardDeck: CardDeck): ImageBitmap? {
    val context = LocalContext.current
    return remember(card, cardDeck) {
        val fileName = card?.assetFileName ?: "back.png"
        try {
            context.assets.open("${cardDeck.assetFolder}/$fileName").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }
}
