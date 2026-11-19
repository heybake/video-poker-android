package com.videopoker.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videopoker.android.ui.components.OverlayDialogs
import com.videopoker.android.ui.components.StartupSplashScreen
import com.videopoker.android.ui.decks.BottomDeck
import com.videopoker.android.ui.decks.MidDeck
import com.videopoker.android.ui.decks.TopDeck
import com.videopoker.android.ui.state.GameState
import com.videopoker.android.ui.state.GameViewModel
import com.videopoker.android.ui.theme.AppTheme
import com.videopoker.android.ui.theme.LocalResponsiveMetrics
import com.videopoker.android.ui.theme.ThemeManager
import com.videopoker.android.ui.theme.calculateResponsiveMetrics

@Composable
fun MainScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    val palette = ThemeManager.getPalette(state.currentTheme)
    val context = LocalContext.current

    // Outer container fills phone or tablet viewport with authentic theme background
    BoxWithConstraints(
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
        val metrics = remember(maxWidth, maxHeight) {
            calculateResponsiveMetrics(
                viewportWidthDp = maxWidth.value,
                viewportHeightDp = maxHeight.value
            )
        }

        CompositionLocalProvider(LocalResponsiveMetrics provides metrics) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = metrics.outerPadH, vertical = metrics.outerPadV),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Deck: Pay Table + Hold Panel scaled proportionally to window height
                TopDeck(
                    state = state,
                    palette = palette,
                    onOpenOverlay = viewModel::openOverlay,
                    onTogglePracticePlot = viewModel::onTogglePracticePlot,
                    modifier = Modifier.weight(metrics.topWeight)
                )

                // Mid Deck: Plaque + Dynamically Sized 5 Cards
                MidDeck(
                    state = state,
                    palette = palette,
                    onToggleHold = viewModel::onToggleHold,
                    onSetHold = viewModel::onSetHold,
                    modifier = Modifier.weight(metrics.midWeight)
                )

                // Bottom Deck: HUD Status Row (anchored to deck width) + Scaled Action Buttons + Footer
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
                    modifier = Modifier.weight(metrics.bottomWeight)
                )
            }

            // Non-blocking transient banner after selecting a game from More Games
            val bannerText = state.gameSelectedBanner
            val bannerAccent = if (state.currentTheme == AppTheme.PARCHMENT) Color(0xFFE8C27A) else palette.goldColor
            val bannerCorner = metrics.s(10f)
            AnimatedVisibility(
                visible = bannerText != null && state.activeOverlay == null && !state.showSplash,
                enter = fadeIn(animationSpec = tween(durationMillis = 160)),
                exit = fadeOut(animationSpec = tween(durationMillis = 240)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (bannerText != null) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = metrics.s(20f), vertical = metrics.s(8f))
                            .widthIn(max = metrics.viewportWidth * 0.85f)
                            .shadow(metrics.s(10f), RoundedCornerShape(bannerCorner))
                            .clip(RoundedCornerShape(bannerCorner))
                            .background(palette.overlayBgColor.copy(alpha = 0.92f))
                            .border(2.dp, bannerAccent, RoundedCornerShape(bannerCorner))
                            .padding(horizontal = metrics.s(20f), vertical = metrics.s(11f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bannerText,
                            color = bannerAccent,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            style = TextStyle(
                                fontSize = (13.5f * metrics.uiScale).sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (0.6f * metrics.uiScale).sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }

            // Modal Overlays (Help, More Games, Options, Denom, See Pays, Add Money, Cash Out)
            OverlayDialogs(
                state = state,
                palette = palette,
                onClose = viewModel::closeOverlay,
                onModeToggle = viewModel::onModeToggle,
                onThemeSelect = viewModel::onThemeSelect,
                onCardDeckSelect = viewModel::onCardDeckSelect,
                onDenomSelect = viewModel::onDenomSelect,
                onGameSelect = viewModel::onGameSelect,
                onVolumeChange = viewModel::onVolumeChange,
                onAddCashAmount = viewModel::onAddCashAmount,
                onConfirmCashOut = viewModel::onConfirmCashOut,
                onSetBankrollTargets = viewModel::onSetBankrollTargets,
                onToggleRecordingSession = viewModel::onToggleRecordingSession,
                onSaveSessionToDownloads = { path -> viewModel.onSaveSessionToDownloads(context, path) },
                onShareSession = { path -> viewModel.onShareSession(context, path) }
            )

            AnimatedVisibility(
                visible = state.showSplash,
                enter = EnterTransition.None,
                exit = fadeOut(animationSpec = tween(durationMillis = 380))
            ) {
                StartupSplashScreen(
                    gamesReady = state.allGames.isNotEmpty(),
                    onDismiss = viewModel::dismissSplash
                )
            }
        }
    }
}
