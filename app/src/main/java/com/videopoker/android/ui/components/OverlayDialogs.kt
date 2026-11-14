package com.videopoker.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import com.videopoker.android.data.models.GameConfig
import com.videopoker.android.data.models.HoldCatalogue
import com.videopoker.android.data.models.HoldChoice
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

    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val compactForIme = isImeVisible && state.activeOverlay == ActiveOverlay.ADD_MONEY

    // Darkened backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(Color(0xCC000000))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(if (state.activeOverlay == ActiveOverlay.COMPLETE_HOLD_LIST) 580.dp else 540.dp)
                .then(
                    if (compactForIme) {
                        Modifier.wrapContentHeight().heightIn(max = 340.dp)
                    } else {
                        Modifier.height(340.dp)
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {} // Absorb clicks inside dialog so they never hit backdrop onClose
                .clip(RoundedCornerShape(14.dp))
                .background(palette.overlayBgColor)
                .border(2.dp, palette.goldColor, RoundedCornerShape(14.dp))
                .padding(if (compactForIme) 12.dp else 16.dp)
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
                    compactForIme = compactForIme,
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
                ActiveOverlay.COMPLETE_HOLD_LIST -> CompleteHoldListContent(
                    game = state.currentGame,
                    holdChoices = state.holdChoices,
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
                HelpSection("Training Modes", "• LEARN: The top optimal hold is selected automatically.\n• PRACTICE: Compare your holds against the optimal strategy table after the draw.\n• CASINO: Pure casino simulation with full pay tables and no hints.", palette)
                HelpSection("The Hold List", "The Hold List (located in the Strategy Advisor panel on the right) evaluates all 32 possible hold combinations for your dealt hand and ranks the best mathematical plays for the active game:\n• Rank (#1, #2, #3): Shows the top choices ranked by expected payout. Choice #1 is marked ✓ TOP and is always the mathematically optimal play.\n• Card Chips: Shows the exact cards to keep for that choice (or DISCARD ALL).\n• Card Indicators: Small numbered dots (1, 2, 3) on the dealt cards indicate which ranked choice keeps each card.", palette)
                HelpSection("What is a Slot?", "Every video poker variant has a master strategy hierarchy of 40 to 60 ranked rules ordered from highest expected value (Slot 1, such as a Royal Flush) down to lowest (Discard All). The Slot number (or Spot) indicates where a particular play ranks on that master chart. When your dealt hand matches multiple possible holds, the rule with the lowest slot number is mathematically superior and takes priority.", palette)
                HelpSection("Complete List", "Tap the COMPLETE LIST button in the Hold Panel to view the entire master strategy chart for your selected game variant. It displays every single rule in priority order from top to bottom. The list automatically scrolls to and highlights the rules matching your current hand, with your top optimal hold highlighted in green.", palette)
                HelpSection("Keyboard Shortcuts", "• Space or 0: Deal / Draw\n• 1, 2, 3, 4, 5: Hold / Release card 1 to 5\n• B: Bet one\n• M: Bet max & deal\n• A: Insert / Add money\n• C: Cash out voucher\n• S: Sound on / off\n• Esc: Close any window", palette)
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
    val scrollState = rememberScrollState()

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

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mode Selector Pills
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "TRAINING MODE", color = palette.goldColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameMode.entries.forEach { mode ->
                            val isCur = (state.currentMode == mode)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCur) palette.goldColor else Color(0x22FFFFFF))
                                    .clickable { onModeToggle(mode) }
                                    .padding(vertical = 7.dp),
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
                }

                // Visual Themes (expandable multi-row grid)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "THEMES", color = palette.goldColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppTheme.entries.chunked(4).forEach { rowThemes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowThemes.forEach { theme ->
                                    val isCur = (state.currentTheme == theme)
                                    val p = ThemeManager.getPalette(theme)
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isCur) palette.goldColor else Color(0x22FFFFFF))
                                            .border(1.dp, if (isCur) Color.White else Color(0x44FFFFFF), RoundedCornerShape(6.dp))
                                            .clickable { onThemeSelect(theme) }
                                            .padding(horizontal = 6.dp, vertical = 7.dp),
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
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                repeat(4 - rowThemes.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Master Volume Slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "SOUND VOLUME (${(state.soundVolume * 100).toInt()}%)", color = palette.goldColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = state.soundVolume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier.height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = palette.goldColor,
                            activeTrackColor = palette.goldColor,
                            inactiveTrackColor = Color(0x44FFFFFF)
                        )
                    )
                }

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
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

            OptionsVerticalScrollbar(
                scrollState = scrollState,
                palette = palette,
                modifier = Modifier
                    .width(12.dp)
                    .fillMaxHeight()
            )
        }
    }
}

/**
 * Interactive vertical scrollbar synchronized with the Options & Settings ScrollState.
 */
@Composable
private fun OptionsVerticalScrollbar(
    scrollState: ScrollState,
    palette: ThemePalette,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val canScroll = scrollState.maxValue > 0

    fun scrollToFraction(fraction: Float) {
        if (!canScroll) return
        val target = (fraction.coerceIn(0f, 1f) * scrollState.maxValue).roundToInt()
        coroutineScope.launch {
            scrollState.scrollTo(target)
        }
    }

    Box(
        modifier = modifier
            .pointerInput(scrollState.maxValue) {
                detectTapGestures { offset ->
                    if (size.height > 0) {
                        scrollToFraction(offset.y / size.height.toFloat())
                    }
                }
            }
            .pointerInput(scrollState.maxValue) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    if (size.height > 0) {
                        scrollToFraction(change.position.y / size.height.toFloat())
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackWidth = 4.dp.toPx()
            val trackLeft = (size.width - trackWidth) / 2f
            val corner = CornerRadius(trackWidth / 2f, trackWidth / 2f)

            drawRoundRect(
                color = Color(0x28FFFFFF),
                topLeft = Offset(trackLeft, 0f),
                size = Size(trackWidth, size.height),
                cornerRadius = corner
            )

            val totalContent = (scrollState.viewportSize + scrollState.maxValue).toFloat()
            val thumbFraction = if (canScroll && totalContent > 0f) {
                (scrollState.viewportSize.toFloat() / totalContent).coerceIn(0.24f, 0.90f)
            } else 1f
            val scrollProgress = if (canScroll) {
                (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
            } else 0f

            val thumbHeight = size.height * thumbFraction
            val thumbTop = (size.height - thumbHeight) * scrollProgress
            val thumbWidth = 6.dp.toPx()
            val thumbLeft = (size.width - thumbWidth) / 2f
            val thumbColor = if (canScroll) palette.goldColor.copy(alpha = 0.85f) else Color(0x44FFFFFF)

            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(thumbLeft, thumbTop),
                size = Size(thumbWidth, thumbHeight),
                cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
            )
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
    compactForIme: Boolean,
    palette: ThemePalette,
    onAddCashAmount: (Double) -> Unit,
    onClose: () -> Unit
) {
    var customInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submitCustomAmount() {
        val trimmed = customInput.trim()
        val parsed = trimmed.toDoubleOrNull()
        if (parsed == null || !parsed.isFinite() || parsed <= 0.0) {
            inputError = "Enter a positive dollar amount"
            return
        }
        val roundedCents = round(parsed * 100.0)
        if (abs(parsed * 100.0 - roundedCents) > 1e-6) {
            inputError = "Enter whole cents or whole dollars"
            return
        }
        val exactCredits = parsed / currentDenom
        val roundedCredits = round(exactCredits)
        if (roundedCredits < 1.0 || abs(exactCredits - roundedCredits) > 1e-6) {
            val denomLabel = if (currentDenom >= 1.0) {
                "$${currentDenom.toInt()}"
            } else {
                "${round(currentDenom * 100.0).toInt()}¢"
            }
            inputError = "Must convert to whole credits (multiple of $denomLabel)"
            return
        }
        inputError = null
        customInput = ""
        focusManager.clearFocus()
        keyboardController?.hide()
        onAddCashAmount(roundedCents / 100.0)
    }

    Column(
        modifier = if (compactForIme) Modifier.fillMaxWidth().wrapContentHeight() else Modifier.fillMaxSize(),
        verticalArrangement = if (compactForIme) Arrangement.spacedBy(8.dp) else Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (compactForIme) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INSERT CASH",
                    color = palette.goldColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "Balance: $%.2f (%d Credits)",
                        cashBalance,
                        round(cashBalance / currentDenom).toInt()
                    ),
                    color = palette.textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                CloseButton(palette = palette, onClose = {
                    focusManager.clearFocus()
                    onClose()
                })
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INSERT CASH",
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "$%.2f  (%d Credits)",
                        cashBalance,
                        round(cashBalance / currentDenom).toInt()
                    ),
                    color = palette.textColor,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = "Select a bill or enter an amount:",
                color = palette.textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            val billRows = listOf(
                listOf(1.0, 5.0, 10.0),
                listOf(20.0, 50.0, 100.0)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                billRows.forEach { rowBills ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowBills.forEach { amt ->
                            val credits = round(amt / currentDenom).toInt()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .shadow(3.dp, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF2E8540), Color(0xFF144D22))
                                        )
                                    )
                                    .border(1.5.dp, Color(0xFF56C470), RoundedCornerShape(8.dp))
                                    .clickable {
                                        inputError = null
                                        onAddCashAmount(amt)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "+$${amt.toInt()}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                    )
                                    Text(
                                        text = "$credits credits",
                                        color = Color(0xFFB8E6C4),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.62f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$",
                    color = palette.goldColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0A1426))
                        .border(
                            width = 1.dp,
                            color = if (inputError != null) Color(0xFFFF6B6B) else palette.goldColor.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (customInput.isEmpty()) {
                        Text(
                            text = "Amount",
                            color = palette.textDimColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    BasicTextField(
                        value = customInput,
                        onValueChange = {
                            customInput = it
                            if (inputError != null) inputError = null
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(palette.goldColor),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitCustomAmount() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2E8540), Color(0xFF144D22))
                            )
                        )
                        .border(1.2.dp, Color(0xFF56C470), RoundedCornerShape(6.dp))
                        .clickable { submitCustomAmount() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ADD",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Box(
                modifier = Modifier.height(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val err = inputError
                if (err != null) {
                    Text(
                        text = err,
                        color = Color(0xFFFF7A7A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!compactForIme) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x33FFFFFF))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "DONE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
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

@Composable
private fun CompleteHoldListContent(
    game: GameConfig?,
    holdChoices: List<HoldChoice>,
    palette: ThemePalette,
    onClose: () -> Unit
) {
    val rules = game?.strategyRules ?: emptyList()
    val totalRules = rules.size
    val matchedChoicesBySlot = remember(holdChoices) {
        holdChoices.associateBy { it.slot }
    }
    val topMatchedSpot = holdChoices.firstOrNull()?.slot ?: 1
    val initialScrollIndex = (topMatchedSpot - 2).coerceIn(0, (totalRules - 1).coerceAtLeast(0))
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COMPLETE STRATEGY LIST",
                    color = palette.goldColor,
                    style = TextStyle(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Black,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Text(
                    text = "${game?.displayName ?: "Video Poker"} • $totalRules Priority Rules (Top to Bottom)",
                    color = palette.textDimColor,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
            CloseButton(palette = palette, onClose = onClose)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x22FFFFFF))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SPOT",
                color = palette.goldColor,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "STRATEGY RULE & ACTIONS",
                color = palette.goldColor,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "MATCH STATUS",
                color = palette.goldColor,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(90.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Scrollable Rules (Automatically centered on top matching rule)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(rules.size) { idx ->
                val spot = idx + 1
                val rule = rules[idx]
                val info = HoldCatalogue.getInfo(rule.id)
                val matched = matchedChoicesBySlot[spot]
                val isTop = matched?.isTopChoice == true
                val isMatched = matched != null

                val rowBg = when {
                    isTop -> Color(0xFF1B5E20)
                    isMatched -> Color(0x33FFD700)
                    idx % 2 == 0 -> Color(0x10FFFFFF)
                    else -> Color(0x06FFFFFF)
                }

                val rowBorder = when {
                    isTop -> Color(0xFF4CAF50)
                    isMatched -> palette.goldColor.copy(alpha = 0.6f)
                    else -> Color(0x18FFFFFF)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(rowBg)
                        .border(1.dp, rowBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Spot Number (e.g. 1, 2, ..., 45)
                    Text(
                        text = "$spot",
                        color = if (isTop) Color.White else palette.goldColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.width(36.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Rule Name and Keeps text
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = info.label,
                            color = if (isTop) Color.White else palette.payTableTextColor,
                            fontSize = 11.5.sp,
                            fontWeight = if (isTop || isMatched) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (info.keeps.isNotBlank()) {
                            Text(
                                text = info.keeps,
                                color = if (isTop) Color(0xFFD5EFDF) else palette.textDimColor,
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Match Status Pill / Badge
                    Box(
                        modifier = Modifier.width(90.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (isTop) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF2E7D32))
                                    .border(1.dp, Color(0xFF81C784), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "✓ TOP CHOICE",
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        } else if (isMatched) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFB8860B))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#${matched?.row} MATCH",
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Footer with Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rules evaluated in top-to-bottom priority. Highest matching rule is played.",
                color = palette.textDimColor,
                fontSize = 9.5.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33FFFFFF))
                    .border(1.dp, palette.goldColor, RoundedCornerShape(4.dp))
                    .clickable { onClose() }
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CLOSE",
                    color = palette.goldColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
