package com.videopoker.android.ui.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.videopoker.android.ui.components.HoldPanel
import com.videopoker.android.ui.components.PayTableGrid
import com.videopoker.android.ui.state.ActiveOverlay
import com.videopoker.android.ui.state.GameMode
import com.videopoker.android.ui.state.GameState
import com.videopoker.android.ui.theme.LocalResponsiveMetrics
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun TopDeck(
    state: GameState,
    palette: ThemePalette,
    onOpenOverlay: (ActiveOverlay) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val metrics = LocalResponsiveMetrics.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = metrics.s(4f), vertical = metrics.s(2f)),
        horizontalArrangement = Arrangement.spacedBy(metrics.s(8f))
    ) {
        if (state.currentMode == GameMode.CASINO) {
            // Full-width 5-coin pay table
            PayTableGrid(
                game = state.currentGame,
                currentBet = state.currentBet,
                winningRank = state.handRank,
                mode = state.currentMode,
                palette = palette,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Proportional Split View (~41% Pay Table / ~59% Hold Choices Panel across phone & tablet)
            PayTableGrid(
                game = state.currentGame,
                currentBet = state.currentBet,
                winningRank = state.handRank,
                mode = state.currentMode,
                palette = palette,
                modifier = Modifier.weight(0.41f)
            )

            HoldPanel(
                choices = state.holdChoices,
                playerHeldMask = state.heldMask,
                dealtCards = state.cards,
                mode = state.currentMode,
                dealState = state.dealState,
                practiceFeedback = state.practiceFeedback,
                practiceMadeTopChoice = state.practiceMadeTopChoice,
                palette = palette,
                onOpenCompleteList = { onOpenOverlay(ActiveOverlay.COMPLETE_HOLD_LIST) },
                modifier = Modifier.weight(0.59f)
            )
        }
    }
}
