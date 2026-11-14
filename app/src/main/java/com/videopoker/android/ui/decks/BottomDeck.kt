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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Compact Upper/Side HUD Status Bar (20dp) - Win + Sound on Left, Bet + Credits on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left HUD Cluster: WIN and Sound shifted close together
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Text(
                        text = winDisplay,
                        color = palette.statusValueColor,
                        style = TextStyle(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                // Sound Speaker Button & Recording Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x33D6293B))
                                .border(1.dp, Color(0xFFD6293B), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "REC",
                                color = Color(0xFFFF8A80),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }

            // Right HUD Cluster: BET and Credits shifted close together
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Text(
                        text = betDisplay,
                        color = palette.statusValueColor,
                        style = TextStyle(
                            fontSize = 13.5.sp,
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Text(
                        text = cashDisplay,
                        color = palette.statusValueColor,
                        style = TextStyle(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }

        // 2. Action Button Strip (40dp) - Perfectly centered on screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HELP (48dp)
            ActionButton(
                text = "HELP",
                width = 48,
                palette = palette,
                onClick = { onOpenOverlay(ActiveOverlay.HELP) }
            )

            // INSERT CASH (78dp)
            ActionButton(
                text = "INSERT\nCASH",
                width = 78,
                palette = palette,
                onClick = onOpenAddMoney
            )

            // CASH OUT (72dp)
            ActionButton(
                text = "CASH\nOUT",
                width = 72,
                palette = palette,
                onClick = onCashOut
            )

            // MORE GAMES (88dp)
            ActionButton(
                text = "MORE\nGAMES",
                width = 88,
                palette = palette,
                onClick = { onOpenOverlay(ActiveOverlay.MORE_GAMES) }
            )

            // OPTIONS (68dp)
            ActionButton(
                text = "OPTIONS",
                width = 68,
                palette = palette,
                onClick = { onOpenOverlay(ActiveOverlay.OPTIONS) }
            )

            // SEE PAYS (68dp)
            ActionButton(
                text = "SEE PAYS",
                width = 68,
                palette = palette,
                onClick = { onOpenOverlay(ActiveOverlay.SEE_PAYS) }
            )

            // SPEED (74dp)
            val speedArrows = "▶".repeat(state.speedLevel)
            ActionButton(
                text = "SPEED $speedArrows",
                width = 74,
                palette = palette,
                onClick = onCycleSpeed
            )

            // BET ONE (58dp)
            ActionButton(
                text = "BET\nONE",
                width = 58,
                palette = palette,
                onClick = onBetOne
            )

            // BET MAX (58dp)
            ActionButton(
                text = "BET\nMAX",
                width = 58,
                palette = palette,
                onClick = onBetMax
            )

            // DENOM COIN (36dp circle)
            val denomLabel = if (state.currentDenom < 1.0) {
                "${(state.currentDenom * 100).toInt()}¢"
            } else {
                "$${state.currentDenom.toInt()}"
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(4.dp, CircleShape)
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
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }

            // DRAW / DEAL (98dp tactile button)
            val dealDrawLabel = if (state.dealState == DealState.DEALT_CHOOSING_HOLDS) "DRAW" else "DEAL"
            Box(
                modifier = Modifier
                    .width(98.dp)
                    .fillMaxHeight()
                    .shadow(6.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(palette.dealButtonBgStart, palette.dealButtonBgEnd)
                        )
                    )
                    .border(1.5.dp, palette.dealButtonBorderColor, RoundedCornerShape(6.dp))
                    .clickable(enabled = !state.isAnimating, onClick = onDealDraw),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dealDrawLabel,
                    color = palette.dealButtonTextColor,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }

        // 3. Footer Bar: Game Title, Centered Version Number & Hands Counters (Safe from phone corner curves)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 52.dp)
                .padding(top = 2.dp)
                .height(16.dp),
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier.weight(1.2f),
                textAlign = TextAlign.Start
            )

            Text(
                text = "v1.0.9",
                color = palette.goldColor,
                style = TextStyle(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier.weight(0.6f),
                textAlign = TextAlign.Center
            )

            Text(
                text = counterText,
                color = palette.footerTextColor,
                style = TextStyle(
                    fontSize = 10.sp,
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
    palette: ThemePalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .fillMaxHeight()
            .shadow(3.dp, RoundedCornerShape(5.dp))
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.verticalGradient(
                    listOf(palette.buttonBgStart, palette.buttonBgEnd)
                )
            )
            .border(1.dp, palette.buttonBorderColor, RoundedCornerShape(5.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = palette.buttonTextColor,
            style = TextStyle(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
