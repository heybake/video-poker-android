package com.videopoker.android.ui.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videopoker.android.ui.components.CardSlot
import com.videopoker.android.ui.components.HandPlaque
import com.videopoker.android.ui.state.GameState
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun MidDeck(
    state: GameState,
    palette: ThemePalette,
    onToggleHold: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hand Plaque (TWO PAIR)
        HandPlaque(
            handRank = state.handRank,
            palette = palette
        )

        // Dedicated 14dp clearance above cards preventing badge collision
        Spacer(modifier = Modifier.height(14.dp))

        // 5 Playing Cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (idx in 0 until 5) {
                val card = state.cards.getOrNull(idx)
                val isHeld = state.heldMask.getOrElse(idx) { false }

                // Find which top choices hold this card (1-based, e.g. [1, 2])
                val choicesHoldingCard = state.holdChoices.take(3).mapNotNull { choice ->
                    if (choice.holdMask.getOrElse(idx) { false }) choice.row else null
                }

                CardSlot(
                    card = card,
                    isHeld = isHeld,
                    cardIndex = idx,
                    choicesHoldingCard = choicesHoldingCard,
                    mode = state.currentMode,
                    palette = palette,
                    onToggleHold = { onToggleHold(idx) }
                )
            }
        }
    }
}
