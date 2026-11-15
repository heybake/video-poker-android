package com.videopoker.android.ui.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.videopoker.android.ui.components.CardSlot
import com.videopoker.android.ui.components.HandPlaque
import com.videopoker.android.ui.state.DealState
import com.videopoker.android.ui.state.GameMode
import com.videopoker.android.ui.state.GameState
import com.videopoker.android.ui.theme.LocalResponsiveMetrics
import com.videopoker.android.ui.theme.ThemePalette
import com.videopoker.android.ui.theme.calculateCardDeckLayout

@Composable
fun MidDeck(
    state: GameState,
    palette: ThemePalette,
    onToggleHold: (Int) -> Unit,
    onSetHold: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHeldMask by rememberUpdatedState(state.heldMask)
    val currentDealState by rememberUpdatedState(state.dealState)
    val currentIsAnimating by rememberUpdatedState(state.isAnimating)
    val currentOnSetHold by rememberUpdatedState(onSetHold)

    val metrics = LocalResponsiveMetrics.current
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = metrics.s(1f)),
        contentAlignment = Alignment.Center
    ) {
        val deckLayout = remember(maxWidth, maxHeight, metrics.uiScale) {
            calculateCardDeckLayout(
                availWidthDp = maxWidth.value,
                availHeightDp = maxHeight.value,
                uiScale = metrics.uiScale
            )
        }
        val cardWidth = deckLayout.cardWidth
        val cardHeight = deckLayout.cardHeight
        val spacing = deckLayout.spacing
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val spacingPx = with(density) { spacing.toPx() }
        val pitchPx = cardWidthPx + spacingPx
        val totalDeckWidthPx = with(density) { (cardWidth * 5 + spacing * 4).toPx() }
        val railRadius = metrics.s(12f)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hand Plaque (TWO PAIR)
            HandPlaque(
                handRank = state.handRank,
                palette = palette
            )

            // Proportional clearance between plaque and felt rail
            Spacer(modifier = Modifier.height(deckLayout.plaqueSpacer))

            // 5 Playing Cards in authentic felt rail scaled proportionally to viewport
            Box(
                modifier = Modifier
                    .shadow(elevation = metrics.s(6f), shape = RoundedCornerShape(railRadius))
                    .clip(RoundedCornerShape(railRadius))
                    .background(
                        Brush.radialGradient(
                            listOf(palette.feltColor, palette.feltColorEnd),
                            radius = 800f * metrics.uiScale
                        )
                    )
                    .border(2.dp, palette.railBorderColor, RoundedCornerShape(railRadius))
                    .padding(
                        start = deckLayout.railPadH,
                        end = deckLayout.railPadH,
                        top = deckLayout.railPadTop,
                        bottom = deckLayout.railPadBottom
                    )
            ) {
                Row(
                    modifier = Modifier.pointerInput(pitchPx, totalDeckWidthPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (currentDealState != DealState.DEALT_CHOOSING_HOLDS || currentIsAnimating) return@awaitEachGesture

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
                    // Dots logic: Dots ONLY appear in LEARN mode while choosing holds (never in Practice or Casino mode, per authentic play.js)
                    val showDots = (state.currentMode == GameMode.LEARN && state.dealState == DealState.DEALT_CHOOSING_HOLDS)

                    for (idx in 0 until 5) {
                        val isRevealed = state.revealedMask.getOrElse(idx) { true }
                        val card = if (isRevealed) state.cards.getOrNull(idx) else null
                        val isHeld = state.heldMask.getOrElse(idx) { false }

                        val choicesHoldingCard = if (showDots) {
                            state.holdChoices.take(3).mapNotNull { choice ->
                                if (choice.holdMask.getOrElse(idx) { false }) choice.row else null
                            }
                        } else {
                            emptyList()
                        }

                        CardSlot(
                            card = card,
                            isHeld = isHeld,
                            cardIndex = idx,
                            choicesHoldingCard = choicesHoldingCard,
                            mode = state.currentMode,
                            palette = palette,
                            cardDeck = state.currentCardDeck,
                            cardWidth = cardWidth,
                            cardHeight = cardHeight,
                            onToggleHold = { onToggleHold(idx) }
                        )
                    }
                }
            }
        }
    }
}
