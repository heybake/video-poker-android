package com.videopoker.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.videopoker.android.ui.components.OverlayDialogs
import com.videopoker.android.ui.decks.BottomDeck
import com.videopoker.android.ui.decks.MidDeck
import com.videopoker.android.ui.decks.TopDeck
import com.videopoker.android.ui.state.GameViewModel
import com.videopoker.android.ui.theme.ThemeManager

@Composable
fun MainScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    val palette = ThemeManager.getPalette(state.currentTheme)

    // Outer container fills phone screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .background(
                    Brush.radialGradient(
                        colors = listOf(palette.feltColor, palette.backgroundColor),
                        radius = 1200f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Deck: Pay Table + Hold Panel
                TopDeck(
                    state = state,
                    palette = palette,
                    modifier = Modifier.weight(142f)
                )

                // Mid Deck: Plaque + 5 Cards + Dots
                MidDeck(
                    state = state,
                    palette = palette,
                    onToggleHold = viewModel::onToggleHold,
                    modifier = Modifier.weight(180f)
                )

                // Bottom Deck: Status Row + 9 Action Buttons + Footer
                BottomDeck(
                    state = state,
                    palette = palette,
                    onDealDraw = viewModel::onDealDraw,
                    onBetOne = viewModel::onBetOne,
                    onBetMax = viewModel::onBetMax,
                    onCycleSpeed = viewModel::onCycleSpeed,
                    onToggleCurrency = viewModel::onToggleCurrencyDisplay,
                    onToggleVolumePopup = viewModel::onToggleVolumePopup,
                    onVolumeChange = viewModel::onVolumeChange,
                    onOpenOverlay = viewModel::openOverlay,
                    modifier = Modifier.weight(92f)
                )
            }

            // Modal Overlays (Help, More Games, Options, Denom, See Pays)
            OverlayDialogs(
                state = state,
                palette = palette,
                onClose = viewModel::closeOverlay,
                onModeToggle = viewModel::onModeToggle,
                onThemeSelect = viewModel::onThemeSelect,
                onDenomSelect = viewModel::onDenomSelect,
                onGameSelect = viewModel::onGameSelect,
                onVolumeChange = viewModel::onVolumeChange
            )
        }
    }
}
