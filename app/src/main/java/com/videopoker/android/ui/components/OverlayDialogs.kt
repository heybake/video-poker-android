package com.videopoker.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.videopoker.android.data.models.GameConfig
import com.videopoker.android.ui.state.ActiveOverlay
import com.videopoker.android.ui.state.GameMode
import com.videopoker.android.ui.state.GameState
import com.videopoker.android.ui.theme.AppTheme
import com.videopoker.android.ui.theme.ThemeManager
import com.videopoker.android.ui.theme.ThemePalette

@Composable
fun OverlayDialogs(
    state: GameState,
    palette: ThemePalette,
    onClose: () -> Unit,
    onModeToggle: (GameMode) -> Unit,
    onThemeSelect: (AppTheme) -> Unit,
    onDenomSelect: (Double) -> Unit,
    onGameSelect: (GameConfig) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    if (state.activeOverlay == null) return

    // Darkened backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(540.dp)
                .height(380.dp)
                .clickable(enabled = false) {} // Prevent backdrop dismissal on dialog click
                .clip(RoundedCornerShape(14.dp))
                .background(palette.panelColor)
                .border(2.dp, palette.goldColor, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            when (state.activeOverlay) {
                ActiveOverlay.HELP -> HelpContent(palette = palette, onClose = onClose)
                ActiveOverlay.MORE_GAMES -> MoreGamesContent(
                    allGames = state.allGames,
                    currentGame = state.currentGame,
                    palette = palette,
                    onGameSelect = onGameSelect
                )
                ActiveOverlay.OPTIONS -> OptionsContent(
                    state = state,
                    palette = palette,
                    onModeToggle = onModeToggle,
                    onThemeSelect = onThemeSelect,
                    onVolumeChange = onVolumeChange,
                    onClose = onClose
                )
                ActiveOverlay.DENOM -> DenomContent(
                    currentDenom = state.currentDenom,
                    palette = palette,
                    onDenomSelect = onDenomSelect
                )
                ActiveOverlay.SEE_PAYS -> SeePaysContent(
                    game = state.currentGame,
                    currentBet = state.currentBet,
                    palette = palette,
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
private fun HelpContent(palette: ThemePalette, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HOW TO PLAY VIDEO POKER",
                color = palette.goldColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            CloseButton(palette = palette, onClose = onClose)
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                HelpSection("Objective", "Get the highest paying 5-card poker hand according to the active game's pay schedule.", palette)
                HelpSection("Deal & Draw", "Tap DEAL to receive 5 cards. Tap any cards you wish to HOLD. Then tap DRAW to replace unheld cards with new cards from the deck.", palette)
                HelpSection("Training Modes", "• LEARN: The top optimal hold is selected automatically.\n• PRACTICE: Compare your holds against the optimal strategy table.\n• CASINO: Pure casino simulation with full pay tables and no hints.", palette)
                HelpSection("Pay Table", "Standard hands range from Royal Flush down to Jacks or Better (pair of Jacks, Queens, Kings, or Aces). Bonus games pay extra for specific 4 of a Kind combinations.", palette)
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, desc: String, palette: ThemePalette) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(text = title, color = palette.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = desc, color = palette.textColor, fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun MoreGamesContent(
    allGames: List<GameConfig>,
    currentGame: GameConfig?,
    palette: ThemePalette,
    onGameSelect: (GameConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "SELECT VIDEO POKER GAME (41 GAMES)",
            color = palette.goldColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(allGames) { game ->
                val isSelected = (game.variant == currentGame?.variant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) palette.goldColor else Color(0x18FFFFFF))
                        .clickable { onGameSelect(game) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = game.displayName,
                            color = if (isSelected) Color(0xFF14100A) else palette.textColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = game.familyName,
                            color = if (isSelected) Color(0xFF3A2808) else palette.textDimColor,
                            fontSize = 9.5.sp
                        )
                    }

                    Text(
                        text = "${String.format("%.2f", game.returnPercentage)}%",
                        color = if (isSelected) Color(0xFF14100A) else palette.goldColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionsContent(
    state: GameState,
    palette: ThemePalette,
    onModeToggle: (GameMode) -> Unit,
    onThemeSelect: (AppTheme) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "OPTIONS & SETTINGS", color = palette.goldColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
            CloseButton(palette = palette, onClose = onClose)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mode Selector Pills
        Text(text = "TRAINING MODE", color = palette.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameMode.entries.forEach { mode ->
                val isCur = (state.currentMode == mode)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCur) palette.goldColor else Color(0x22FFFFFF))
                        .clickable { onModeToggle(mode) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.displayName,
                        color = if (isCur) Color(0xFF14100A) else palette.textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visual Themes (8 Authentic Themes)
        Text(text = "AUTHENTIC THEMES", color = palette.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.height(130.dp)
        ) {
            items(AppTheme.entries) { theme ->
                val isCur = (state.currentTheme == theme)
                val p = ThemeManager.getPalette(theme)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCur) palette.goldColor else Color(0x22FFFFFF))
                        .border(1.dp, if (isCur) Color.White else Color(0x44FFFFFF), RoundedCornerShape(6.dp))
                        .clickable { onThemeSelect(theme) }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(p.swatchColor)
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = theme.displayName,
                        color = if (isCur) Color(0xFF14100A) else palette.textColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Master Volume Slider
        Text(text = "SOUND VOLUME (${(state.soundVolume * 100).toInt()}%)", color = palette.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Slider(
            value = state.soundVolume,
            onValueChange = onVolumeChange,
            colors = SliderDefaults.colors(
                thumbColor = palette.goldColor,
                activeTrackColor = palette.goldColor,
                inactiveTrackColor = Color(0x44FFFFFF)
            )
        )
    }
}

@Composable
private fun DenomContent(
    currentDenom: Double,
    palette: ThemePalette,
    onDenomSelect: (Double) -> Unit
) {
    val denoms = listOf(0.05, 0.10, 0.25, 0.50, 1.0, 5.0, 10.0, 25.0, 100.0)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SELECT COIN DENOMINATION",
            color = palette.goldColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(denoms) { d ->
                val isCur = (currentDenom == d)
                val label = if (d < 1.0) "${(d * 100).toInt()}¢" else "$${d.toInt()}"

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                if (isCur) listOf(Color(0xFFFFF7D6), Color(0xFFC9A24D))
                                else listOf(Color(0xFF4A443E), Color(0xFF1E1B18))
                            )
                        )
                        .border(
                            if (isCur) 3.dp else 1.5.dp,
                            if (isCur) palette.goldColor else Color(0x66FFFFFF),
                            CircleShape
                        )
                        .clickable { onDenomSelect(d) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isCur) Color(0xFF2A1A08) else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun SeePaysContent(
    game: GameConfig?,
    currentBet: Int,
    palette: ThemePalette,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${game?.displayName ?: "PAYOUT SCHEDULE"} (FULL TABLE)",
                color = palette.goldColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            CloseButton(palette = palette, onClose = onClose)
        }

        Spacer(modifier = Modifier.height(8.dp))

        PayTableGrid(
            game = game,
            currentBet = currentBet,
            winningRank = null,
            mode = GameMode.CASINO,
            palette = palette,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CloseButton(palette: ThemePalette, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "✕", color = palette.textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
