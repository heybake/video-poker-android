package com.videopoker.android.ui.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onToggleCurrency: () -> Unit,
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
        // 1. Status Bar (24dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clickable { onToggleCurrency() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // WIN
            val winDisplay = if (state.displayCurrency) {
                String.format("$%.2f", state.winAmount * state.currentDenom)
            } else {
                "${state.winAmount}"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "WIN ",
                    color = palette.statusLabelColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = winDisplay,
                    color = palette.statusValueColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Sound Speaker Button (Theme Vector SVG)
            SoundButton(
                volume = state.soundVolume,
                isPopupOpen = state.isVolumePopupOpen,
                palette = palette,
                onTogglePopup = onToggleVolumePopup,
                onVolumeChange = onVolumeChange
            )

            // BET
            val betDisplay = if (state.displayCurrency) {
                String.format("$%.2f", state.currentBet * state.currentDenom)
            } else {
                "${state.currentBet}"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "BET ",
                    color = palette.statusLabelColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = betDisplay,
                    color = palette.statusValueColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // CASH / CREDIT
            val cashDisplay = if (state.displayCurrency) {
                String.format("$%.2f", state.cashBalance)
            } else {
                "${(state.cashBalance / state.currentDenom).toInt()}"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.displayCurrency) "CASH " else "CREDIT ",
                    color = palette.statusLabelColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = cashDisplay,
                    color = palette.statusValueColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // 2. Action Button Strip (38dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HELP (72dp)
            ActionButton(
                text = "HELP",
                width = 72,
                palette = palette,
                onClick = { onOpenOverlay(ActiveOverlay.HELP) }
            )

            // MORE GAMES (118dp)
            ActionButton(
                text = "MORE GAMES",
                width = 118,
                palette = palette,
                onClick = { onOpenOverlay(ActiveOverlay.MORE_GAMES) }
            )

            // OPTIONS (94dp)
            ActionButton(
                text = "OPTIONS",
                width = 94,
                palette = palette,
                onClick = { onOpenOverlay(ActiveOverlay.OPTIONS) }
            )

            // SEE PAYS (94dp)
            ActionButton(
                text = "SEE PAYS",
                width = 94,
                palette = palette,
                onClick = { onOpenOverlay(ActiveOverlay.SEE_PAYS) }
            )

            // SPEED (108dp)
            val speedArrows = "▶".repeat(state.speedLevel)
            ActionButton(
                text = "SPEED $speedArrows",
                width = 108,
                palette = palette,
                onClick = onCycleSpeed
            )

            // BET ONE (74dp)
            ActionButton(
                text = "BET\nONE",
                width = 74,
                palette = palette,
                onClick = onBetOne
            )

            // BET MAX (74dp)
            ActionButton(
                text = "BET\nMAX",
                width = 74,
                palette = palette,
                onClick = onBetMax
            )

            // DENOM COIN (42dp circle)
            val denomLabel = if (state.currentDenom < 1.0) {
                "${(state.currentDenom * 100).toInt()}¢"
            } else {
                "$${state.currentDenom.toInt()}"
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // DRAW / DEAL (146dp tactile button for right thumb)
            val dealDrawLabel = if (state.dealState == DealState.DEALT_CHOOSING_HOLDS) "DRAW" else "DEAL"
            Box(
                modifier = Modifier
                    .width(146.dp)
                    .fillMaxHeight()
                    .shadow(6.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(palette.dealButtonBgStart, palette.dealButtonBgEnd)
                        )
                    )
                    .border(1.5.dp, palette.goldColor, RoundedCornerShape(6.dp))
                    .clickable(onClick = onDealDraw),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dealDrawLabel,
                    color = palette.dealButtonTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }
        }

        // 3. Footer Bar (16dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val gameTitle = state.currentGame?.let {
                "${it.displayName} • ${String.format("%.2f", it.returnPercentage)}%"
            } ?: "Double Bonus (10/7) • 100.17%"

            val denomText = if (state.currentDenom < 1.0) "${(state.currentDenom * 100).toInt()}¢" else "$${state.currentDenom.toInt()}"
            val counterText = "Hands: ${state.totalHandsPlayed}  •  Hands at $denomText: ${state.handsAtCurrentDenom}"

            Text(
                text = gameTitle,
                color = palette.footerTextColor,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = counterText,
                color = palette.footerTextColor,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
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
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 11.5.sp
        )
    }
}
