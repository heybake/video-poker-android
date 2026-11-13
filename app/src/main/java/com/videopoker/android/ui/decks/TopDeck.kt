package com.videopoker.android.ui.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videopoker.android.ui.components.HoldPanel
import com.videopoker.android.ui.components.PayTableGrid
import com.videopoker.android.ui.state.GameMode
import com.videopoker.android.ui.state.GameState
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun TopDeck(
    state: GameState,
    palette: ThemePalette,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(156.dp)
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            // Split View: 380dp Pay Table + Hold Choices Panel
            PayTableGrid(
                game = state.currentGame,
                currentBet = state.currentBet,
                winningRank = state.handRank,
                mode = state.currentMode,
                palette = palette,
                modifier = Modifier.width(380.dp)
            )

            HoldPanel(
                choices = state.holdChoices,
                palette = palette,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
