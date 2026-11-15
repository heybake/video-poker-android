package com.videopoker.android.ui.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.videopoker.android.ui.components.SoundButton
import com.videopoker.android.ui.state.ActiveOverlay
import com.videopoker.android.ui.state.DealState
import com.videopoker.android.ui.state.GameState
import com.videopoker.android.ui.theme.LocalResponsiveMetrics
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun BottomDeck(
    state: GameState,
    palette: ThemePalette,
    onDealDraw: () -> Unit,
    onBetOne: () -> Unit,
    onBetMax: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleWinCurrency: () -> Unit,
    onToggleBetCurrency: () -> Unit,
    onToggleCreditCurrency: () -> Unit,
    onOpenAddMoney: () -> Unit,
    onCashOut: () -> Unit,
    onToggleVolumePopup: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenOverlay: (ActiveOverlay) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = LocalResponsiveMetrics.current
    val scale = metrics.uiScale

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = metrics.s(4f), vertical = metrics.s(2f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Upper HUD Status Bar anchored to card/control column width so counters stay visually tied to cards
        Row(
            modifier = Modifier
                .width(metrics.hudContainerWidth)
                .height(metrics.s(20f))
                .padding(horizontal = metrics.s(6f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left HUD Cluster: WIN and Sound anchored together
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(metrics.s(10f))
            ) {
                // WIN (Click to toggle WIN independently between dollars and credits)
                val winDisplay = if (state.winAsCash) {
                    String.format(Locale.US, "$%.2f", state.winAmount * state.currentDenom)
                } else {
                    "${state.winAmount}"
                }
                Row(
                    modifier = Modifier.clickable { onToggleWinCurrency() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WIN ",
                        color = palette.statusLabelColor,
                        style = TextStyle(
                            fontSize = (12f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Text(
                        text = winDisplay,
                        color = palette.statusValueColor,
                        style = TextStyle(
                            fontSize = (13.5f * scale).sp,
                            fontWeight = FontWeight.Black,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                // Sound Speaker Button & Recording Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(metrics.s(6f))
                ) {
                    SoundButton(
                        volume = state.soundVolume,
                        isPopupOpen = state.isVolumePopupOpen,
                        palette = palette,
                        onTogglePopup = onToggleVolumePopup,
                        onVolumeChange = onVolumeChange
                    )

                    // Recording Indicator (Visible only when session recording is active)
                    if (state.isRecordingSession) {
                        val recRadius = metrics.s(4f)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(recRadius))
                                .background(Color(0x33D6293B))
                                .border(1.dp, Color(0xFFD6293B), RoundedCornerShape(recRadius))
                                .padding(horizontal = metrics.s(5f), vertical = metrics.s(1.5f)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(metrics.s(6f))
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935))
                            )
                            Spacer(modifier = Modifier.width(metrics.s(3f)))
                            Text(
                                text = "REC",
                                color = Color(0xFFFF8A80),
                                fontSize = (9f * scale).sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (0.8f * scale).sp
                            )
                        }
                    }
                }
            }

            // Right HUD Cluster: BET and Credits anchored together
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(metrics.s(12f))
            ) {
                // BET (Click to toggle BET independently between dollars and credits)
                val betDisplay = if (state.betAsCash) {
                    String.format(Locale.US, "$%.2f", state.currentBet * state.currentDenom)
                } else {
                    "${state.currentBet}"
                }
                Row(
                    modifier = Modifier.clickable { onToggleBetCurrency() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BET ",
                        color = palette.statusLabelColor,
                        style = TextStyle(
                            fontSize = (12f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Text(
                        text = betDisplay,
                        color = palette.statusValueColor,
                        style = TextStyle(
                            fontSize = (13.5f * scale).sp,
                            fontWeight = FontWeight.Black,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                // CASH / CREDIT (Click to toggle CREDIT independently between dollars and credits)
                val cashDisplay = if (state.creditAsCash) {
                    String.format(Locale.US, "$%.2f", state.cashBalance)
                } else {
                    "${(state.cashBalance / state.currentDenom).toInt()}"
                }
                Row(
                    modifier = Modifier.clickable { onToggleCreditCurrency() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.creditAsCash) "CASH " else "CREDIT ",
                        color = palette.statusLabelColor,
                        style = TextStyle(
                            fontSize = (12f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Text(
                        text = cashDisplay,
                        color = palette.statusValueColor,
                        style = TextStyle(
                            fontSize = (13.5f * scale).sp,
                            fontWeight = FontWeight.Black,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }

        // 2. Scaled Action Button Strip centered cleanly
        Row(
            modifier = Modifier
                .width(metrics.actionBarWidth)
                .height(metrics.s(40f)),
            horizontalArrangement = Arrangement.spacedBy(metrics.s(4f), Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HELP
            ActionButton(
                text = "HELP",
                width = 48,
                scale = scale,
                palette = palette,
                onClick = { onOpenOverlay(ActiveOverlay.HELP) }
            )

            // INSERT CASH
            ActionButton(
                text = "INSERT\nCASH",
                width = 78,
                scale = scale,
                palette = palette,
                onClick = onOpenAddMoney
            )

            // CASH OUT
            ActionButton(
                text = "CASH\nOUT",
                width = 72,
                scale = scale,
                palette = palette,
                onClick = onCashOut
            )

            // MORE GAMES
            ActionButton(
                text = "MORE\nGAMES",
                width = 88,
                scale = scale,
                palette = palette,
                onClick = { onOpenOverlay(ActiveOverlay.MORE_GAMES) }
            )

            // OPTIONS
            ActionButton(
                text = "OPTIONS",
                width = 68,
                scale = scale,
                palette = palette,
                onClick = { onOpenOverlay(ActiveOverlay.OPTIONS) }
            )

            // SEE PAYS
            ActionButton(
                text = "SEE PAYS",
                width = 68,
                scale = scale,
                palette = palette,
                onClick = { onOpenOverlay(ActiveOverlay.SEE_PAYS) }
            )

            // SPEED
            val speedArrows = "▶".repeat(state.speedLevel)
            ActionButton(
                text = "SPEED $speedArrows",
                width = 74,
                scale = scale,
                palette = palette,
                onClick = onCycleSpeed
            )

            // BET ONE
            ActionButton(
                text = "BET\nONE",
                width = 58,
                scale = scale,
                palette = palette,
                onClick = onBetOne
            )

            // BET MAX
            ActionButton(
                text = "BET\nMAX",
                width = 58,
                scale = scale,
                palette = palette,
                onClick = onBetMax
            )

            // DENOM COIN
            val denomLabel = if (state.currentDenom < 1.0) {
                "${(state.currentDenom * 100).toInt()}¢"
            } else {
                "$${state.currentDenom.toInt()}"
            }
            Box(
                modifier = Modifier
                    .size(metrics.s(36f))
                    .shadow(metrics.s(4f), CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFF3C0), Color(0xFF7A5A1E))
                        )
                    )
                    .border(2.dp, palette.goldColor, CircleShape)
                    .clickable { onOpenOverlay(ActiveOverlay.DENOM) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = denomLabel,
                    color = Color(0xFF2A1A08),
                    style = TextStyle(
                        fontSize = (10.5f * scale).sp,
                        fontWeight = FontWeight.Black,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }

            // DRAW / DEAL tactile button
            val dealDrawLabel = if (state.dealState == DealState.DEALT_CHOOSING_HOLDS) "DRAW" else "DEAL"
            val dealRadius = metrics.s(6f)
            Box(
                modifier = Modifier
                    .width(metrics.s(98f))
                    .fillMaxHeight()
                    .shadow(metrics.s(6f), RoundedCornerShape(dealRadius))
                    .clip(RoundedCornerShape(dealRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(palette.dealButtonBgStart, palette.dealButtonBgEnd)
                        )
                    )
                    .border(1.5.dp, palette.dealButtonBorderColor, RoundedCornerShape(dealRadius))
                    .clickable(enabled = !state.isAnimating, onClick = onDealDraw),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dealDrawLabel,
                    color = palette.dealButtonTextColor,
                    style = TextStyle(
                        fontSize = (15f * scale).sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (1.2f * scale).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }

        // 3. Footer Bar anchored symmetrically with control/card deck bounds
        Row(
            modifier = Modifier
                .width(metrics.hudContainerWidth)
                .padding(horizontal = metrics.s(6f))
                .padding(top = metrics.s(2f))
                .height(metrics.s(16f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val gameTitle = state.currentGame?.let {
                "${it.displayName} • ${String.format("%.2f", it.returnPercentage)}%"
            } ?: "Select a Game"

            val denomText = if (state.currentDenom < 1.0) "${(state.currentDenom * 100).toInt()}¢" else "$${state.currentDenom.toInt()}"
            val counterText = "Hands: ${state.totalHandsPlayed}  •  Hands at $denomText: ${state.handsAtCurrentDenom}"

            Text(
                text = gameTitle,
                color = palette.footerTextColor,
                style = TextStyle(
                    fontSize = (10f * scale).sp,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier.weight(1.2f),
                textAlign = TextAlign.Start
            )

            Text(
                text = "v1.0.14",
                color = palette.goldColor,
                style = TextStyle(
                    fontSize = (10.5f * scale).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (0.5f * scale).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier.weight(0.6f),
                textAlign = TextAlign.Center
            )

            Text(
                text = counterText,
                color = palette.footerTextColor,
                style = TextStyle(
                    fontSize = (10f * scale).sp,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier.weight(1.2f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    width: Int,
    scale: Float = 1f,
    palette: ThemePalette,
    onClick: () -> Unit
) {
    val radius = (5f * scale).dp
    Box(
        modifier = Modifier
            .width((width * scale).dp)
            .fillMaxHeight()
            .shadow((3f * scale).dp, RoundedCornerShape(radius))
            .clip(RoundedCornerShape(radius))
            .background(
                Brush.verticalGradient(
                    listOf(palette.buttonBgStart, palette.buttonBgEnd)
                )
            )
            .border(1.dp, palette.buttonBorderColor, RoundedCornerShape(radius))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = palette.buttonTextColor,
            style = TextStyle(
                fontSize = (10.5f * scale).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = (12f * scale).sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
