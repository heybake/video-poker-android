package com.videopoker.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
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

    // Outer container fills phone screen with authentic theme background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(
                Brush.verticalGradient(
                    listOf(palette.backgroundColor, palette.backgroundColorEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Deck: Pay Table + Hold Panel (Generous 158f weight so pay table NEVER cuts off)
            TopDeck(
                state = state,
                palette = palette,
                onOpenOverlay = viewModel::openOverlay,
                modifier = Modifier.weight(158f)
            )

            // Mid Deck: Plaque + 5 Cards + Dots (160f weight for roomy card display)
            MidDeck(
                state = state,
                palette = palette,
                onToggleHold = viewModel::onToggleHold,
                onSetHold = viewModel::onSetHold,
                modifier = Modifier.weight(160f)
            )

            // Bottom Deck: Status Row + 10 Action Buttons + Footer (Centered buttons)
            BottomDeck(
                state = state,
                palette = palette,
                onDealDraw = viewModel::onDealDraw,
                onBetOne = viewModel::onBetOne,
                onBetMax = viewModel::onBetMax,
                onCycleSpeed = viewModel::onCycleSpeed,
                onToggleWinCurrency = viewModel::onToggleWinCurrency,
                onToggleBetCurrency = viewModel::onToggleBetCurrency,
                onToggleCreditCurrency = viewModel::onToggleCreditCurrency,
                onOpenAddMoney = viewModel::onOpenAddMoney,
                onCashOut = viewModel::onCashOut,
                onToggleVolumePopup = viewModel::onToggleVolumePopup,
                onVolumeChange = viewModel::onVolumeChange,
                onOpenOverlay = viewModel::openOverlay,
                modifier = Modifier.weight(96f)
            )
        }

        // Modal Overlays (Help, More Games, Options, Denom, See Pays, Add Money, Cash Out)
        OverlayDialogs(
            state = state,
            palette = palette,
            onClose = viewModel::closeOverlay,
            onModeToggle = viewModel::onModeToggle,
            onThemeSelect = viewModel::onThemeSelect,
            onDenomSelect = viewModel::onDenomSelect,
            onGameSelect = viewModel::onGameSelect,
            onVolumeChange = viewModel::onVolumeChange,
            onAddCashAmount = viewModel::onAddCashAmount,
            onConfirmCashOut = viewModel::onConfirmCashOut,
            onToggleRecordingSession = viewModel::onToggleRecordingSession
        )
    }
}
