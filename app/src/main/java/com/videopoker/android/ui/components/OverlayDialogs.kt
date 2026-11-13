package com.videopoker.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
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
    onVolumeChange: (Float) -> Unit,
    onAddCashAmount: (Double) -> Unit,
    onConfirmCashOut: () -> Unit,
    onToggleRecordingSession: () -> Unit
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
                .height(340.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {} // Absorb clicks inside dialog so they never hit backdrop onClose
                .clip(RoundedCornerShape(14.dp))
                .background(palette.overlayBgColor)
                .border(2.dp, palette.goldColor, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            when (state.activeOverlay) {
                ActiveOverlay.HELP -> HelpContent(palette = palette, onClose = onClose)
                ActiveOverlay.MORE_GAMES -> MoreGamesContent(
                    allGames = state.allGames,
                    currentGame = state.currentGame,
                    palette = palette,
                    onGameSelect = { selectedGame ->
                        onGameSelect(selectedGame)
                        onClose()
                    },
                    onClose = onClose
                )
                ActiveOverlay.OPTIONS -> OptionsContent(
                    state = state,
                    palette = palette,
                    onModeToggle = onModeToggle,
                    onThemeSelect = onThemeSelect,
                    onVolumeChange = onVolumeChange,
                    onToggleRecordingSession = onToggleRecordingSession,
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
                ActiveOverlay.ADD_MONEY -> AddMoneyContent(
                    cashBalance = state.cashBalance,
                    currentDenom = state.currentDenom,
                    palette = palette,
                    onAddCashAmount = onAddCashAmount,
                    onClose = onClose
                )
                ActiveOverlay.CASH_OUT -> CashOutContent(
                    cashBalance = state.cashBalance,
                    currentDenom = state.currentDenom,
                    palette = palette,
                    onConfirmCashOut = onConfirmCashOut,
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
                HelpSection("Training Modes", "• LEARN: The top optimal hold is selected automatically.\n• PRACTICE: Compare your holds against the optimal strategy table after the draw.\n• CASINO: Pure casino simulation with full pay tables and no hints.", palette)
                HelpSection("Bluetooth Keyboard Shortcuts", "• Space or 0: Deal / Draw\n• 1, 2, 3, 4, 5: Hold / Release card 1 to 5\n• B: Bet one\n• M: Bet max & deal\n• A: Insert / Add money\n• C: Cash out voucher\n• S: Sound on / off\n• Esc: Close any window", palette)
                HelpSection("Pay Table", "Standard hands range from Royal Flush down to Jacks or Better (pair of Jacks, Queens, Kings, or Aces). Bonus games pay extra for specific 4 of a Kind combinations.", palette)
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, desc: String, palette: ThemePalette) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(text = title, color = palette.goldColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = desc, color = Color(0xFFF5F0E6), fontSize = 11.5.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun MoreGamesContent(
    allGames: List<GameConfig>,
    currentGame: GameConfig?,
    palette: ThemePalette,
    onGameSelect: (GameConfig) -> Unit,
    onClose: () -> Unit
) {
    // Stage 1: Family Grid (e.g. 11 Families in 4 columns)
    // Stage 2: Pick Pay Table Grid (e.g. 2 columns showing 9/6, 10/7 with return %)
    var selectedFamily by remember { mutableStateOf<String?>(null) }

    val familyOrder = listOf(
        "Jacks or Better",
        "Bonus Poker",
        "Bonus Poker Deluxe",
        "Double Bonus",
        "Double Double Bonus",
        "Triple Bonus Plus",
        "Triple Double Bonus",
        "Super Aces Bonus",
        "White Hot Aces",
        "USA Poker",
        "Hybrid Db Bp Quads"
    )

    val familyGroups = remember(allGames) {
        val map = allGames.groupBy { it.familyName }
        familyOrder.mapNotNull { fam ->
            map[fam]?.let { fam to it }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedFamily == null) {
            // Stage 1 Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELECT A GAME",
                    color = palette.goldColor,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                CloseButton(palette = palette, onClose = onClose)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Column Grid of Authentic Family Tiles matching casino mockup
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(familyGroups) { (family, games) ->
                    val isCurFamily = (currentGame?.familyName == family)
                    val tableCount = games.size
                    val countLabel = if (tableCount == 1) "1 pay table" else "$tableCount pay tables"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (tableCount == 1) {
                                    onGameSelect(games.first())
                                } else {
                                    selectedFamily = family
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Tile Art Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .shadow(4.dp, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.feltColor)
                                .then(
                                    if (isCurFamily) {
                                        Modifier.border(2.5.dp, Color(0xFF3DDC52), RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier.border(2.dp, palette.goldColor, RoundedCornerShape(8.dp))
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = family.uppercase(),
                                color = palette.accentColor,
                                style = TextStyle(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 12.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Subtitle: "X pay tables"
                        Text(
                            text = countLabel,
                            color = palette.payTableTextColor,
                            style = TextStyle(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }
        } else {
            // Stage 2: Pick Pay Table for Selected Family
            val gamesInFamily = remember(selectedFamily, allGames) {
                allGames.filter { it.familyName == selectedFamily }
                    .sortedByDescending { it.returnPercentage }
            }

            // Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(palette.buttonBgStart, palette.buttonBgEnd)
                                )
                            )
                            .border(1.dp, palette.buttonBorderColor, RoundedCornerShape(6.dp))
                            .clickable { selectedFamily = null }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "◀ ALL GAMES",
                            color = palette.buttonTextColor,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "${selectedFamily?.uppercase()} — PAY TABLES",
                        color = palette.goldColor,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                CloseButton(palette = palette, onClose = onClose)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2-Column Grid of Pay Table Options
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gamesInFamily) { game ->
                    val isCur = (game.variant == currentGame?.variant)
                    val retText = String.format(java.util.Locale.US, "%.2f%%", game.returnPercentage)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .shadow(3.dp, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isCur) {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF1B6E32), Color(0xFF0F441E))
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(palette.panelColor, palette.panelColor.copy(alpha = 0.85f))
                                    )
                                }
                            )
                            .border(
                                width = if (isCur) 2.dp else 1.dp,
                                color = if (isCur) Color(0xFF3DDC52) else palette.goldColor.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onGameSelect(game) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = game.payTableLabel,
                                color = if (isCur) Color.White else palette.textColor,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )

                            Text(
                                text = retText,
                                color = if (isCur) Color(0xFFFFEA00) else palette.goldColor,
                                style = TextStyle(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }
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
    onToggleRecordingSession: () -> Unit,
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

        Spacer(modifier = Modifier.height(6.dp))

        // Mode Selector Pills
        Text(text = "TRAINING MODE", color = palette.goldColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameMode.entries.forEach { mode ->
                val isCur = (state.currentMode == mode)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCur) palette.goldColor else Color(0x22FFFFFF))
                        .clickable { onModeToggle(mode) }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.displayName,
                        color = if (isCur) Color(0xFF14100A) else Color(0xFFF5F0E6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Visual Themes (8 Authentic Themes)
        Text(text = "AUTHENTIC THEMES", color = palette.goldColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.height(76.dp)
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
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(p.swatchColor)
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = theme.displayName,
                        color = if (isCur) Color(0xFF14100A) else Color(0xFFF5F0E6),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Master Volume Slider
        Text(text = "SOUND VOLUME (${(state.soundVolume * 100).toInt()}%)", color = palette.goldColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        Slider(
            value = state.soundVolume,
            onValueChange = onVolumeChange,
            modifier = Modifier.height(26.dp),
            colors = SliderDefaults.colors(
                thumbColor = palette.goldColor,
                activeTrackColor = palette.goldColor,
                inactiveTrackColor = Color(0x44FFFFFF)
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Session Recording Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (state.isRecordingSession) Color(0x33D6293B) else Color(0x22FFFFFF))
                .border(
                    width = 1.dp,
                    color = if (state.isRecordingSession) Color(0xFFD6293B) else Color(0x44FFFFFF),
                    shape = RoundedCornerShape(6.dp)
                )
                .clickable { onToggleRecordingSession() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (state.isRecordingSession) Color(0xFFE53935) else Color(0xFF888888))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "RECORD PLAY SESSION",
                        color = Color(0xFFF5F0E6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (state.isRecordingSession) "Logging all hands to session history" else "Session logging is disabled",
                        color = Color(0xFFB0A495),
                        fontSize = 9.5.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (state.isRecordingSession) Color(0xFFD6293B) else Color(0x44FFFFFF))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (state.isRecordingSession) "ON" else "OFF",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
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
        Text(text = "✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AddMoneyContent(
    cashBalance: Double,
    currentDenom: Double,
    palette: ThemePalette,
    onAddCashAmount: (Double) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INSERT CASH / ADD MONEY",
                color = palette.goldColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            CloseButton(palette = palette, onClose = onClose)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Current Balance",
                color = palette.textDimColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format("$%.2f  (%d Credits)", cashBalance, (cashBalance / currentDenom).toInt()),
                color = palette.textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }

        Text(
            text = "Select a bill to insert into the machine:",
            color = palette.textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val bills = listOf(20.0, 50.0, 100.0, 500.0)
            bills.forEach { amt ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2E8540), Color(0xFF144D22))
                            )
                        )
                        .border(1.5.dp, Color(0xFF56C470), RoundedCornerShape(8.dp))
                        .clickable { onAddCashAmount(amt) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "+$${amt.toInt()}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${(amt / currentDenom).toInt()} credits",
                            color = Color(0xFFB8E6C4),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .width(120.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x33FFFFFF))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "DONE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CashOutContent(
    cashBalance: Double,
    currentDenom: Double,
    palette: ThemePalette,
    onConfirmCashOut: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CASH OUT / TICKET VOUCHER",
                color = palette.goldColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            CloseButton(palette = palette, onClose = onClose)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFFFDF0))
                .border(2.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CASINO CASHIER VOUCHER",
                    color = Color(0xFF111111),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "VALIDATION TICKET #849201",
                    color = Color(0xFF666666),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = String.format("$%.2f", cashBalance),
                    color = Color(0xFF007A33),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${(cashBalance / currentDenom).toInt()} credits cashed out",
                    color = Color(0xFF444444),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(38.dp)
                    .shadow(4.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFD6293B))
                    .clickable { onConfirmCashOut() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "TAKE VOUCHER", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }

            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x33FFFFFF))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "KEEP PLAYING", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
