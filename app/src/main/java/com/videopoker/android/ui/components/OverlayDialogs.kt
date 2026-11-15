package com.videopoker.android.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.videopoker.android.ui.theme.CardDeck
import com.videopoker.android.ui.theme.LocalResponsiveMetrics
import com.videopoker.android.ui.theme.ThemeManager
import com.videopoker.android.ui.theme.ThemePalette
import com.videopoker.android.ui.theme.calculateModalLayout

@Composable
fun OverlayDialogs(
    state: GameState,
    palette: ThemePalette,
    onClose: () -> Unit,
    onModeToggle: (GameMode) -> Unit,
    onThemeSelect: (AppTheme) -> Unit,
    onCardDeckSelect: (CardDeck) -> Unit,
    onDenomSelect: (Double) -> Unit,
    onGameSelect: (GameConfig) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onAddCashAmount: (Double) -> Unit,
    onConfirmCashOut: () -> Unit,
    onToggleRecordingSession: () -> Unit,
    onSaveSessionToDownloads: (String) -> Unit = {},
    onShareSession: (String) -> Unit = {}
) {
    val activeOverlay = state.activeOverlay ?: return

    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val compactForIme = isImeVisible && activeOverlay == ActiveOverlay.ADD_MONEY
    val metrics = LocalResponsiveMetrics.current
    val isWideList = activeOverlay == ActiveOverlay.COMPLETE_HOLD_LIST
    val modal = remember(metrics, isWideList) {
        calculateModalLayout(metrics = metrics, isWideList = isWideList)
    }
    val s = modal.scale

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
                .width(modal.width)
                .then(
                    if (compactForIme) {
                        Modifier.wrapContentHeight().heightIn(max = minOf(modal.height, modal.maxHeightForIme))
                    } else {
                        Modifier.height(modal.height)
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {} // Absorb clicks inside dialog so they never hit backdrop onClose
                .clip(RoundedCornerShape(modal.cornerRadius))
                .background(palette.overlayBgColor)
                .border(modal.borderWidth, palette.goldColor, RoundedCornerShape(modal.cornerRadius))
                .padding(if (compactForIme) (12f * minOf(s, 1.0f)).dp else modal.contentPadding)
        ) {
            when (activeOverlay) {
                ActiveOverlay.HELP -> HelpContent(palette = palette, scale = s, onClose = onClose)
                ActiveOverlay.MORE_GAMES -> MoreGamesContent(
                    allGames = state.allGames,
                    currentGame = state.currentGame,
                    palette = palette,
                    scale = s,
                    onGameSelect = { selectedGame ->
                        onGameSelect(selectedGame)
                        onClose()
                    },
                    onClose = onClose
                )
                ActiveOverlay.OPTIONS -> OptionsContent(
                    state = state,
                    palette = palette,
                    scale = s,
                    onModeToggle = onModeToggle,
                    onThemeSelect = onThemeSelect,
                    onCardDeckSelect = onCardDeckSelect,
                    onVolumeChange = onVolumeChange,
                    onToggleRecordingSession = onToggleRecordingSession,
                    onSaveSessionToDownloads = onSaveSessionToDownloads,
                    onShareSession = onShareSession,
                    onClose = onClose
                )
                ActiveOverlay.DENOM -> DenomContent(
                    currentDenom = state.currentDenom,
                    palette = palette,
                    scale = s,
                    onDenomSelect = onDenomSelect,
                    onClose = onClose
                )
                ActiveOverlay.SEE_PAYS -> SeePaysContent(
                    game = state.currentGame,
                    currentBet = state.currentBet,
                    palette = palette,
                    scale = s,
                    onClose = onClose
                )
                ActiveOverlay.ADD_MONEY -> AddMoneyContent(
                    cashBalance = state.cashBalance,
                    currentDenom = state.currentDenom,
                    compactForIme = compactForIme,
                    palette = palette,
                    scale = s,
                    onAddCashAmount = onAddCashAmount,
                    onClose = onClose
                )
                ActiveOverlay.CASH_OUT -> CashOutContent(
                    cashBalance = state.cashBalance,
                    currentDenom = state.currentDenom,
                    palette = palette,
                    scale = s,
                    onConfirmCashOut = onConfirmCashOut,
                    onClose = onClose
                )
                ActiveOverlay.COMPLETE_HOLD_LIST -> CompleteHoldListContent(
                    game = state.currentGame,
                    holdChoices = state.holdChoices,
                    palette = palette,
                    scale = s,
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
private fun HelpContent(palette: ThemePalette, scale: Float, onClose: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HOW TO PLAY VIDEO POKER",
                color = palette.goldColor,
                fontSize = (15f * scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (0.8f * scale).sp
            )
            CloseButton(palette = palette, scale = scale, onClose = onClose)
        }

        Spacer(modifier = Modifier.height((10f * scale).dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((8f * scale).dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(end = (4f * scale).dp, bottom = (18f * scale).dp),
                verticalArrangement = Arrangement.spacedBy((8f * scale).dp)
            ) {
                HelpSection("Objective", "Get the highest paying 5-card poker hand according to the active game's pay schedule.", palette, scale)
                HelpSection("Deal & Draw", "Tap DEAL to receive 5 cards. Tap any cards you wish to HOLD. Then tap DRAW to replace unheld cards with new cards from the deck.", palette, scale)
                HelpSection("Training Modes", "• LEARN: The top optimal hold is selected automatically.\n• PRACTICE: Compare your holds against the optimal strategy table after the draw.\n• CASINO: Pure casino simulation with full pay tables and no hints.", palette, scale)
                HelpSection("The Hold List", "The Hold List (located in the Strategy Advisor panel on the right) evaluates all 32 possible hold combinations for your dealt hand and ranks the best mathematical plays for the active game:\n• Rank (#1, #2, #3): Shows the top choices ranked by expected payout. Choice #1 is marked ✓ TOP and is always the mathematically optimal play.\n• Card Chips: Shows the exact cards to keep for that choice (or DISCARD ALL).\n• Card Indicators: Small numbered dots (1, 2, 3) on the dealt cards indicate which ranked choice keeps each card.", palette, scale)
                HelpSection("What is a Slot?", "Every video poker variant has a master strategy hierarchy of 40 to 60 ranked rules ordered from highest expected value (Slot 1, such as a Royal Flush) down to lowest (Discard All). The Slot number (or Spot) indicates where a particular play ranks on that master chart. When your dealt hand matches multiple possible holds, the rule with the lowest slot number is mathematically superior and takes priority.", palette, scale)
                HelpSection("Complete List", "Tap the COMPLETE LIST button in the Hold Panel to view the entire master strategy chart for your selected game variant. It displays every single rule in priority order from top to bottom. The list automatically scrolls to and highlights the rules matching your current hand, with your top optimal hold highlighted in green.", palette, scale)
                HelpSection("Keyboard Shortcuts", "• Space or 0: Deal / Draw\n• 1, 2, 3, 4, 5: Hold / Release card 1 to 5\n• B: Bet one\n• M: Bet max & deal\n• A: Insert / Add money\n• C: Cash out voucher\n• S: Sound on / off\n• Esc: Close any window", palette, scale)
                HelpSection("Pay Table", "Standard hands range from Royal Flush down to Jacks or Better (pair of Jacks, Queens, Kings, or Aces). Bonus games pay extra for specific 4 of a Kind combinations.", palette, scale)
            }

            OptionsVerticalScrollbar(
                scrollState = scrollState,
                palette = palette,
                scale = scale,
                modifier = Modifier
                    .width((12f * scale).dp)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun HelpSection(title: String, desc: String, palette: ThemePalette, scale: Float) {
    Column(modifier = Modifier.padding(bottom = (4f * scale).dp)) {
        Text(text = title, color = palette.goldColor, fontSize = (12f * scale).sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height((2f * scale).dp))
        Text(
            text = desc,
            color = Color(0xFFF5F0E6),
            fontSize = (11.5f * scale).sp,
            lineHeight = (15.5f * scale).sp
        )
    }
}

@Composable
private fun MoreGamesContent(
    allGames: List<GameConfig>,
    currentGame: GameConfig?,
    palette: ThemePalette,
    scale: Float,
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
                        fontSize = (15f * scale).sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (1.2f * scale).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                CloseButton(palette = palette, scale = scale, onClose = onClose)
            }

            Spacer(modifier = Modifier.height((10f * scale).dp))

            // 4-Column Grid of Authentic Family Tiles matching casino mockup
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = (10f * scale).dp),
                horizontalArrangement = Arrangement.spacedBy((10f * scale).dp),
                verticalArrangement = Arrangement.spacedBy((10f * scale).dp)
            ) {
                items(familyGroups) { (family, games) ->
                    val isCurFamily = (currentGame?.familyName == family)
                    val tableCount = games.size
                    val countLabel = if (tableCount == 1) "1 pay table" else "$tableCount pay tables"
                    val tileCorner = (8f * scale).dp

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
                                .height((46f * scale).dp)
                                .shadow((4f * scale).dp, RoundedCornerShape(tileCorner))
                                .clip(RoundedCornerShape(tileCorner))
                                .background(palette.feltColor)
                                .then(
                                    if (isCurFamily) {
                                        Modifier.border((2.5f * scale).dp, Color(0xFF3DDC52), RoundedCornerShape(tileCorner))
                                    } else {
                                        Modifier.border((2f * scale).dp, palette.goldColor, RoundedCornerShape(tileCorner))
                                    }
                                )
                                .padding(horizontal = (6f * scale).dp, vertical = (4f * scale).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = family.uppercase(),
                                color = palette.accentColor,
                                style = TextStyle(
                                    fontSize = (10.5f * scale).sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    lineHeight = (12f * scale).sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height((3f * scale).dp))

                        // Subtitle: "X pay tables"
                        Text(
                            text = countLabel,
                            color = palette.payTableTextColor,
                            style = TextStyle(
                                fontSize = (10.5f * scale).sp,
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
                Row(
                    modifier = Modifier.weight(1f).padding(end = (8f * scale).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape((6f * scale).dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(palette.buttonBgStart, palette.buttonBgEnd)
                                )
                            )
                            .border((1f * scale).dp, palette.buttonBorderColor, RoundedCornerShape((6f * scale).dp))
                            .clickable { selectedFamily = null }
                            .padding(horizontal = (10f * scale).dp, vertical = (5f * scale).dp)
                    ) {
                        Text(
                            text = "◀ ALL GAMES",
                            color = palette.buttonTextColor,
                            style = TextStyle(
                                fontSize = (11f * scale).sp,
                                fontWeight = FontWeight.Black,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width((12f * scale).dp))

                    Text(
                        text = "${selectedFamily?.uppercase()} — PAY TABLES",
                        color = palette.goldColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            fontSize = (14f * scale).sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (1f * scale).sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                CloseButton(palette = palette, scale = scale, onClose = onClose)
            }

            Spacer(modifier = Modifier.height((10f * scale).dp))

            // 2-Column Grid of Pay Table Options
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = (10f * scale).dp),
                horizontalArrangement = Arrangement.spacedBy((10f * scale).dp),
                verticalArrangement = Arrangement.spacedBy((8f * scale).dp)
            ) {
                items(gamesInFamily) { game ->
                    val isCur = (game.variant == currentGame?.variant)
                    val retText = String.format(java.util.Locale.US, "%.2f%%", game.returnPercentage)
                    val optCorner = (6f * scale).dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((44f * scale).dp)
                            .shadow((3f * scale).dp, RoundedCornerShape(optCorner))
                            .clip(RoundedCornerShape(optCorner))
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
                                width = if (isCur) (2f * scale).dp else (1f * scale).dp,
                                color = if (isCur) Color(0xFF3DDC52) else palette.goldColor.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(optCorner)
                            )
                            .clickable { onGameSelect(game) }
                            .padding(horizontal = (14f * scale).dp),
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
                                    fontSize = (14f * scale).sp,
                                    fontWeight = FontWeight.Black,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )

                            Text(
                                text = retText,
                                color = if (isCur) Color(0xFFFFEA00) else palette.goldColor,
                                style = TextStyle(
                                    fontSize = (13.5f * scale).sp,
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
    scale: Float,
    onModeToggle: (GameMode) -> Unit,
    onThemeSelect: (AppTheme) -> Unit,
    onCardDeckSelect: (CardDeck) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleRecordingSession: () -> Unit,
    onSaveSessionToDownloads: (String) -> Unit,
    onShareSession: (String) -> Unit,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    val pillCorner = (6f * scale).dp

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OPTIONS & SETTINGS",
                color = palette.goldColor,
                fontSize = (14f * scale).sp,
                fontWeight = FontWeight.Black
            )
            CloseButton(palette = palette, scale = scale, onClose = onClose)
        }

        Spacer(modifier = Modifier.height((8f * scale).dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((8f * scale).dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(bottom = (16f * scale).dp),
                verticalArrangement = Arrangement.spacedBy((10f * scale).dp)
            ) {
                // Mode Selector Pills
                Column(verticalArrangement = Arrangement.spacedBy((4f * scale).dp)) {
                    Text(
                        text = "TRAINING MODE",
                        color = palette.goldColor,
                        fontSize = (10.5f * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy((8f * scale).dp)) {
                        GameMode.entries.forEach { mode ->
                            val isCur = (state.currentMode == mode)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(pillCorner))
                                    .background(if (isCur) palette.goldColor else Color(0x22FFFFFF))
                                    .clickable { onModeToggle(mode) }
                                    .padding(vertical = (7f * scale).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.displayName,
                                    color = if (isCur) Color(0xFF14100A) else Color(0xFFF5F0E6),
                                    fontSize = (11f * scale).sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // Visual Themes (expandable multi-row grid)
                Column(verticalArrangement = Arrangement.spacedBy((4f * scale).dp)) {
                    Text(
                        text = "THEMES",
                        color = palette.goldColor,
                        fontSize = (10.5f * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy((6f * scale).dp)
                    ) {
                        AppTheme.entries.chunked(4).forEach { rowThemes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy((6f * scale).dp)
                            ) {
                                rowThemes.forEach { theme ->
                                    val isCur = (state.currentTheme == theme)
                                    val p = ThemeManager.getPalette(theme)
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(pillCorner))
                                            .background(if (isCur) palette.goldColor else Color(0x22FFFFFF))
                                            .border(1.dp, if (isCur) Color.White else Color(0x44FFFFFF), RoundedCornerShape(pillCorner))
                                            .clickable { onThemeSelect(theme) }
                                            .padding(horizontal = (6f * scale).dp, vertical = (7f * scale).dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size((11f * scale).dp)
                                                .clip(CircleShape)
                                                .background(p.swatchColor)
                                                .border(1.dp, Color.White, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width((4f * scale).dp))
                                        Text(
                                            text = theme.displayName,
                                            color = if (isCur) Color(0xFF14100A) else Color(0xFFF5F0E6),
                                            fontSize = (9.5f * scale).sp,
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

                // Selectable Card Deck (Modern Crest vs Classic IGT)
                Column(verticalArrangement = Arrangement.spacedBy((4f * scale).dp)) {
                    val context = LocalContext.current
                    Text(
                        text = "CARD DECK",
                        color = palette.goldColor,
                        fontSize = (10.5f * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy((8f * scale).dp)
                    ) {
                        CardDeck.entries.forEach { cardDeck ->
                            val isCur = (state.currentCardDeck == cardDeck)
                            val backBmp = remember(cardDeck) {
                                try {
                                    context.assets.open("${cardDeck.assetFolder}/back.png").use {
                                        BitmapFactory.decodeStream(it)?.asImageBitmap()
                                    }
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            val aceBmp = remember(cardDeck) {
                                try {
                                    context.assets.open("${cardDeck.assetFolder}/As.png").use {
                                        BitmapFactory.decodeStream(it)?.asImageBitmap()
                                    }
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(pillCorner))
                                    .background(if (isCur) palette.goldColor else Color(0x22FFFFFF))
                                    .border(
                                        width = 1.dp,
                                        color = if (isCur) Color.White else Color(0x44FFFFFF),
                                        shape = RoundedCornerShape(pillCorner)
                                    )
                                    .clickable { onCardDeckSelect(cardDeck) }
                                    .padding(horizontal = (8f * scale).dp, vertical = (6f * scale).dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((6f * scale).dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy((3f * scale).dp)) {
                                    if (backBmp != null) {
                                        Image(
                                            bitmap = backBmp,
                                            contentDescription = "${cardDeck.displayName} back",
                                            modifier = Modifier
                                                .size(width = (22f * scale).dp, height = (31f * scale).dp)
                                                .clip(RoundedCornerShape((3f * scale).dp))
                                                .border(0.75.dp, Color(0xFF222222), RoundedCornerShape((3f * scale).dp))
                                        )
                                    }
                                    if (aceBmp != null) {
                                        Image(
                                            bitmap = aceBmp,
                                            contentDescription = "${cardDeck.displayName} Ace of Spades",
                                            modifier = Modifier
                                                .size(width = (22f * scale).dp, height = (31f * scale).dp)
                                                .clip(RoundedCornerShape((3f * scale).dp))
                                                .border(0.75.dp, Color(0xFF222222), RoundedCornerShape((3f * scale).dp))
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy((1f * scale).dp)) {
                                    Text(
                                        text = cardDeck.displayName,
                                        color = if (isCur) Color(0xFF14100A) else Color(0xFFF5F0E6),
                                        fontSize = (10.5f * scale).sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = cardDeck.subtitle,
                                        color = if (isCur) Color(0xFF332917) else Color(0xCCDFD8CC),
                                        fontSize = (8.5f * scale).sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Master Volume Slider
                Column(verticalArrangement = Arrangement.spacedBy((2f * scale).dp)) {
                    Text(
                        text = "SOUND VOLUME (${(state.soundVolume * 100).toInt()}%)",
                        color = palette.goldColor,
                        fontSize = (10.5f * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = state.soundVolume,
                        onValueChange = onVolumeChange,
                        modifier = Modifier.height((28f * scale).dp),
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
                        .clip(RoundedCornerShape(pillCorner))
                        .background(if (state.isRecordingSession) Color(0x33D6293B) else Color(0x22FFFFFF))
                        .border(
                            width = 1.dp,
                            color = if (state.isRecordingSession) Color(0xFFD6293B) else Color(0x44FFFFFF),
                            shape = RoundedCornerShape(pillCorner)
                        )
                        .clickable { onToggleRecordingSession() }
                        .padding(horizontal = (12f * scale).dp, vertical = (10f * scale).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size((10f * scale).dp)
                                .clip(CircleShape)
                                .background(if (state.isRecordingSession) Color(0xFFE53935) else Color(0xFF888888))
                        )
                        Spacer(modifier = Modifier.width((8f * scale).dp))
                        Column(verticalArrangement = Arrangement.spacedBy((2f * scale).dp)) {
                            Text(
                                text = "RECORD PLAY SESSION",
                                color = Color(0xFFF5F0E6),
                                fontSize = (11f * scale).sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    state.isRecordingSession && state.recordedHandsCount > 0 ->
                                        "Recording Workbench session (${state.recordedHandsCount} hands)"
                                    state.isRecordingSession ->
                                        "Ready — next finished hand starts a Workbench folder"
                                    else ->
                                        "Tap to record Workbench-compatible session logs"
                                },
                                color = Color(0xFFB0A495),
                                fontSize = (9.5f * scale).sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape((4f * scale).dp))
                            .background(if (state.isRecordingSession) Color(0xFFD6293B) else Color(0x44FFFFFF))
                            .padding(horizontal = (10f * scale).dp, vertical = (3f * scale).dp)
                    ) {
                        Text(
                            text = if (state.isRecordingSession) "ON" else "OFF",
                            color = Color.White,
                            fontSize = (10.5f * scale).sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Saved Workbench Sessions & Export
                Column(verticalArrangement = Arrangement.spacedBy((5f * scale).dp)) {
                    Text(
                        text = "SAVED SESSIONS FOR WORKBENCH (${state.recordedSessions.size})",
                        color = palette.goldColor,
                        fontSize = (10.5f * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Save a .zip to Downloads/VideoPokerSessions or Share to PC, then unzip into Workbench output.",
                        color = Color(0xFFB0A495),
                        fontSize = (9f * scale).sp
                    )
                    val exportMsg = state.sessionExportStatus
                    if (exportMsg != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape((5f * scale).dp))
                                .background(Color(0x332E8540))
                                .border(1.dp, Color(0xFF56C470), RoundedCornerShape((5f * scale).dp))
                                .padding(horizontal = (8f * scale).dp, vertical = (5f * scale).dp)
                        ) {
                            Text(
                                text = exportMsg,
                                color = Color(0xFFB8E6C4),
                                fontSize = (9.5f * scale).sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (state.recordedSessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(pillCorner))
                                .background(Color(0x18FFFFFF))
                                .padding(horizontal = (10f * scale).dp, vertical = (8f * scale).dp)
                        ) {
                            Text(
                                text = "No recorded sessions yet. Turn RECORD PLAY SESSION ON and play a hand.",
                                color = Color(0xFF999084),
                                fontSize = (9.5f * scale).sp
                            )
                        }
                    } else {
                        state.recordedSessions.forEach { session ->
                            val netStr = if (session.netProfitLoss >= 0) {
                                String.format(Locale.US, "+$%.2f", session.netProfitLoss)
                            } else {
                                String.format(Locale.US, "-$%.2f", abs(session.netProfitLoss))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(pillCorner))
                                    .background(Color(0x1EFFFFFF))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(pillCorner))
                                    .padding(horizontal = (10f * scale).dp, vertical = (7f * scale).dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy((2f * scale).dp)
                                ) {
                                    Text(
                                        text = session.payTableName,
                                        color = Color(0xFFF5F0E6),
                                        fontSize = (10.5f * scale).sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${session.handsPlayed} hands  •  $netStr  •  ${session.startedAtDisplay}",
                                        color = Color(0xFFB0A495),
                                        fontSize = (9f * scale).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width((8f * scale).dp))
                                Row(horizontalArrangement = Arrangement.spacedBy((6f * scale).dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape((5f * scale).dp))
                                            .background(Color(0xFF1E6E35))
                                            .border(1.dp, Color(0xFF56C470), RoundedCornerShape((5f * scale).dp))
                                            .clickable { onSaveSessionToDownloads(session.folderPath) }
                                            .padding(horizontal = (8f * scale).dp, vertical = (5f * scale).dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "DOWNLOAD",
                                            color = Color.White,
                                            fontSize = (9f * scale).sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape((5f * scale).dp))
                                            .background(palette.goldColor)
                                            .clickable { onShareSession(session.folderPath) }
                                            .padding(horizontal = (8f * scale).dp, vertical = (5f * scale).dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "SHARE",
                                            color = Color(0xFF14100A),
                                            fontSize = (9f * scale).sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OptionsVerticalScrollbar(
                scrollState = scrollState,
                palette = palette,
                scale = scale,
                modifier = Modifier
                    .width((12f * scale).dp)
                    .fillMaxHeight()
            )
        }
    }
}

/**
 * Interactive vertical scrollbar synchronized with a Compose ScrollState.
 */
@Composable
private fun OptionsVerticalScrollbar(
    scrollState: ScrollState,
    palette: ThemePalette,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val canScroll = scrollState.maxValue > 0

    val totalContent = (scrollState.viewportSize + scrollState.maxValue).toFloat()
    val thumbFraction = if (canScroll && totalContent > 0f) {
        (scrollState.viewportSize.toFloat() / totalContent).coerceIn(0.24f, 0.90f)
    } else 1f
    val scrollProgress = if (canScroll) {
        (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
    } else 0f

    VerticalScrollbarTrack(
        canScroll = canScroll,
        thumbFraction = thumbFraction,
        scrollProgress = scrollProgress,
        palette = palette,
        scale = scale,
        pointerKey = scrollState.maxValue,
        onFractionSelect = { fraction ->
            val target = (fraction.coerceIn(0f, 1f) * scrollState.maxValue).roundToInt()
            coroutineScope.launch { scrollState.scrollTo(target) }
        },
        modifier = modifier
    )
}

/**
 * Interactive vertical scrollbar synchronized with a LazyListState.
 */
@Composable
private fun LazyListVerticalScrollbar(
    listState: LazyListState,
    totalItems: Int,
    palette: ThemePalette,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val layoutInfo = listState.layoutInfo
    val visibleCount = layoutInfo.visibleItemsInfo.size
    val canScroll = totalItems > visibleCount && visibleCount > 0

    val thumbFraction = if (canScroll) {
        (visibleCount.toFloat() / totalItems.toFloat()).coerceIn(0.18f, 0.90f)
    } else 1f
    val maxFirstIndex = (totalItems - visibleCount).coerceAtLeast(1)
    val scrollProgress = if (canScroll) {
        (listState.firstVisibleItemIndex.toFloat() / maxFirstIndex.toFloat()).coerceIn(0f, 1f)
    } else 0f

    VerticalScrollbarTrack(
        canScroll = canScroll,
        thumbFraction = thumbFraction,
        scrollProgress = scrollProgress,
        palette = palette,
        scale = scale,
        pointerKey = totalItems,
        onFractionSelect = { fraction ->
            val targetIndex = (fraction.coerceIn(0f, 1f) * maxFirstIndex).roundToInt()
                .coerceIn(0, (totalItems - 1).coerceAtLeast(0))
            coroutineScope.launch { listState.scrollToItem(targetIndex) }
        },
        modifier = modifier
    )
}

@Composable
private fun VerticalScrollbarTrack(
    canScroll: Boolean,
    thumbFraction: Float,
    scrollProgress: Float,
    palette: ThemePalette,
    scale: Float,
    pointerKey: Any,
    onFractionSelect: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(pointerKey) {
                detectTapGestures { offset ->
                    if (canScroll && size.height > 0) {
                        onFractionSelect(offset.y / size.height.toFloat())
                    }
                }
            }
            .pointerInput(pointerKey) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    if (canScroll && size.height > 0) {
                        onFractionSelect(change.position.y / size.height.toFloat())
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackWidth = (4f * scale).dp.toPx()
            val trackLeft = (size.width - trackWidth) / 2f
            val corner = CornerRadius(trackWidth / 2f, trackWidth / 2f)

            drawRoundRect(
                color = Color(0x28FFFFFF),
                topLeft = Offset(trackLeft, 0f),
                size = Size(trackWidth, size.height),
                cornerRadius = corner
            )

            val thumbHeight = size.height * thumbFraction
            val thumbTop = (size.height - thumbHeight) * scrollProgress
            val thumbWidth = (6f * scale).dp.toPx()
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
    scale: Float,
    onDenomSelect: (Double) -> Unit,
    onClose: () -> Unit
) {
    val denoms = listOf(0.05, 0.10, 0.25, 0.50, 1.0, 5.0, 10.0, 25.0, 100.0)
    val coinSize = (66f * scale).dp
    val gridGap = (14f * scale).dp

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SELECT COIN DENOMINATION",
                color = palette.goldColor,
                fontSize = (14f * scale).sp,
                fontWeight = FontWeight.Black
            )
            CloseButton(palette = palette, scale = scale, onClose = onClose)
        }

        Spacer(modifier = Modifier.height((10f * scale).dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(gridGap),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                denoms.chunked(3).forEach { rowDenoms ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gridGap)) {
                        rowDenoms.forEach { d ->
                            val isCur = (currentDenom == d)
                            val label = if (d < 1.0) "${(d * 100).toInt()}¢" else "$${d.toInt()}"

                            Box(
                                modifier = Modifier
                                    .size(coinSize)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            if (isCur) listOf(Color(0xFFFFF7D6), Color(0xFFC9A24D))
                                            else listOf(Color(0xFF4A443E), Color(0xFF1E1B18))
                                        )
                                    )
                                    .border(
                                        if (isCur) (3f * scale).dp else (1.5f * scale).dp,
                                        if (isCur) palette.goldColor else Color(0x66FFFFFF),
                                        CircleShape
                                    )
                                    .clickable { onDenomSelect(d) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isCur) Color(0xFF2A1A08) else Color.White,
                                    fontSize = (18f * scale).sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
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
    scale: Float,
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
                fontSize = (13f * scale).sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = (8f * scale).dp)
            )
            CloseButton(palette = palette, scale = scale, onClose = onClose)
        }

        Spacer(modifier = Modifier.height((8f * scale).dp))

        PayTableGrid(
            game = game,
            currentBet = currentBet,
            winningRank = null,
            mode = GameMode.CASINO,
            palette = palette,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
}

@Composable
private fun CloseButton(palette: ThemePalette, scale: Float, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .size((24f * scale).dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "✕", color = Color.White, fontSize = (11f * scale).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AddMoneyContent(
    cashBalance: Double,
    currentDenom: Double,
    compactForIme: Boolean,
    palette: ThemePalette,
    scale: Float,
    onAddCashAmount: (Double) -> Unit,
    onClose: () -> Unit
) {
    val effScale = if (compactForIme) minOf(scale, 1.0f) else scale
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
        verticalArrangement = if (compactForIme) Arrangement.spacedBy((8f * effScale).dp) else Arrangement.SpaceBetween,
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
                    fontSize = (14f * effScale).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (1f * effScale).sp
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "Balance: $%.2f (%d Credits)",
                        cashBalance,
                        round(cashBalance / currentDenom).toInt()
                    ),
                    color = palette.textColor,
                    fontSize = (13f * effScale).sp,
                    fontWeight = FontWeight.Bold
                )
                CloseButton(palette = palette, scale = effScale, onClose = {
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
                    fontSize = (15f * effScale).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (1f * effScale).sp
                )
                CloseButton(palette = palette, scale = effScale, onClose = onClose)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Current Balance",
                    color = palette.textDimColor,
                    fontSize = (11f * effScale).sp,
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
                    fontSize = (19f * effScale).sp,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = "Select a bill or enter an amount:",
                color = palette.textColor,
                fontSize = (12f * effScale).sp,
                fontWeight = FontWeight.Medium
            )

            val billRows = listOf(
                listOf(1.0, 5.0, 10.0),
                listOf(20.0, 50.0, 100.0)
            )
            val billCorner = (8f * effScale).dp
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy((6f * effScale).dp)
            ) {
                billRows.forEach { rowBills ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy((8f * effScale).dp)
                    ) {
                        rowBills.forEach { amt ->
                            val credits = round(amt / currentDenom).toInt()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height((42f * effScale).dp)
                                    .shadow((3f * effScale).dp, RoundedCornerShape(billCorner))
                                    .clip(RoundedCornerShape(billCorner))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF2E8540), Color(0xFF144D22))
                                        )
                                    )
                                    .border((1.5f * effScale).dp, Color(0xFF56C470), RoundedCornerShape(billCorner))
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
                                        fontSize = (14f * effScale).sp,
                                        fontWeight = FontWeight.Black,
                                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                    )
                                    Text(
                                        text = "$credits credits",
                                        color = Color(0xFFB8E6C4),
                                        fontSize = (9f * effScale).sp,
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

        val fieldCorner = (6f * effScale).dp
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((2f * effScale).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.62f),
                horizontalArrangement = Arrangement.spacedBy((8f * effScale).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$",
                    color = palette.goldColor,
                    fontSize = (16f * effScale).sp,
                    fontWeight = FontWeight.Black
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((34f * effScale).dp)
                        .clip(RoundedCornerShape(fieldCorner))
                        .background(Color(0xFF0A1426))
                        .border(
                            width = 1.dp,
                            color = if (inputError != null) Color(0xFFFF6B6B) else palette.goldColor.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(fieldCorner)
                        )
                        .padding(horizontal = (10f * effScale).dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (customInput.isEmpty()) {
                        Text(
                            text = "Amount",
                            color = palette.textDimColor,
                            fontSize = (13f * effScale).sp,
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
                            fontSize = (14f * effScale).sp,
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
                        .width((72f * effScale).dp)
                        .height((34f * effScale).dp)
                        .clip(RoundedCornerShape(fieldCorner))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2E8540), Color(0xFF144D22))
                            )
                        )
                        .border((1.2f * effScale).dp, Color(0xFF56C470), RoundedCornerShape(fieldCorner))
                        .clickable { submitCustomAmount() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ADD",
                        color = Color.White,
                        fontSize = (12f * effScale).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Box(
                modifier = Modifier.height((16f * effScale).dp),
                contentAlignment = Alignment.Center
            ) {
                val err = inputError
                if (err != null) {
                    Text(
                        text = err,
                        color = Color(0xFFFF7A7A),
                        fontSize = (10f * effScale).sp,
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
                    .width((120f * effScale).dp)
                    .height((32f * effScale).dp)
                    .clip(RoundedCornerShape((6f * effScale).dp))
                    .background(Color(0x33FFFFFF))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "DONE", color = Color.White, fontSize = (12f * effScale).sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CashOutContent(
    cashBalance: Double,
    currentDenom: Double,
    palette: ThemePalette,
    scale: Float,
    onConfirmCashOut: () -> Unit,
    onClose: () -> Unit
) {
    val voucherCorner = (8f * scale).dp
    val btnCorner = (6f * scale).dp

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
                fontSize = (15f * scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (1f * scale).sp
            )
            CloseButton(palette = palette, scale = scale, onClose = onClose)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(voucherCorner))
                .background(Color(0xFFFFFDF0))
                .border((2f * scale).dp, Color(0xFF333333), RoundedCornerShape(voucherCorner))
                .padding((14f * scale).dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CASINO CASHIER VOUCHER",
                    color = Color(0xFF111111),
                    fontSize = (13f * scale).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (1f * scale).sp
                )
                Spacer(modifier = Modifier.height((4f * scale).dp))
                Text(
                    text = "VALIDATION TICKET #849201",
                    color = Color(0xFF666666),
                    fontSize = (9f * scale).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height((8f * scale).dp))
                Text(
                    text = String.format("$%.2f", cashBalance),
                    color = Color(0xFF007A33),
                    fontSize = (24f * scale).sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${(cashBalance / currentDenom).toInt()} credits cashed out",
                    color = Color(0xFF444444),
                    fontSize = (10f * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy((16f * scale).dp)) {
            Box(
                modifier = Modifier
                    .width((130f * scale).dp)
                    .height((38f * scale).dp)
                    .shadow((4f * scale).dp, RoundedCornerShape(btnCorner))
                    .clip(RoundedCornerShape(btnCorner))
                    .background(Color(0xFFD6293B))
                    .clickable { onConfirmCashOut() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "TAKE VOUCHER", color = Color.White, fontSize = (12f * scale).sp, fontWeight = FontWeight.Black)
            }

            Box(
                modifier = Modifier
                    .width((110f * scale).dp)
                    .height((38f * scale).dp)
                    .clip(RoundedCornerShape(btnCorner))
                    .background(Color(0x33FFFFFF))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "KEEP PLAYING", color = Color.White, fontSize = (12f * scale).sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompleteHoldListContent(
    game: GameConfig?,
    holdChoices: List<HoldChoice>,
    palette: ThemePalette,
    scale: Float,
    onClose: () -> Unit
) {
    val rules = game?.strategyRules ?: emptyList()
    val totalRules = rules.size
    val matchedChoicesBySlot = remember(holdChoices) {
        holdChoices.associateBy { it.slot }
    }
    val topMatchedSpot = holdChoices.firstOrNull()?.slot ?: 1
    val initialScrollIndex = (topMatchedSpot - 2).coerceIn(0, (totalRules - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)
    val rowCorner = (4f * scale).dp
    val spotColW = (40f * scale).dp
    val statusColW = (106f * scale).dp

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = (8f * scale).dp)) {
                Text(
                    text = "COMPLETE STRATEGY LIST",
                    color = palette.goldColor,
                    style = TextStyle(
                        fontSize = (14.5f * scale).sp,
                        fontWeight = FontWeight.Black,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Text(
                    text = "${game?.displayName ?: "Video Poker"} • $totalRules Priority Rules (Top to Bottom)",
                    color = palette.textDimColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontSize = (10f * scale).sp,
                        fontWeight = FontWeight.Medium,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
            CloseButton(palette = palette, scale = scale, onClose = onClose)
        }

        Spacer(modifier = Modifier.height((8f * scale).dp))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(rowCorner))
                .background(Color(0x22FFFFFF))
                .padding(horizontal = (8f * scale).dp, vertical = (4f * scale).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SPOT",
                color = palette.goldColor,
                fontSize = (9.5f * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(spotColW)
            )
            Spacer(modifier = Modifier.width((4f * scale).dp))
            Text(
                text = "STRATEGY RULE & ACTIONS",
                color = palette.goldColor,
                fontSize = (9.5f * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "MATCH STATUS",
                color = palette.goldColor,
                fontSize = (9.5f * scale).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(statusColW)
            )
        }

        Spacer(modifier = Modifier.height((4f * scale).dp))

        // Scrollable Rules (Automatically centered on top matching rule)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((6f * scale).dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = (10f * scale).dp),
                verticalArrangement = Arrangement.spacedBy((2.5f * scale).dp)
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
                            .clip(RoundedCornerShape(rowCorner))
                            .background(rowBg)
                            .border(1.dp, rowBorder, RoundedCornerShape(rowCorner))
                            .padding(horizontal = (8f * scale).dp, vertical = (3.5f * scale).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Spot Number (e.g. 1, 2, ..., 45)
                        Text(
                            text = "$spot",
                            color = if (isTop) Color.White else palette.goldColor,
                            fontSize = (11f * scale).sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.width(spotColW)
                        )

                        Spacer(modifier = Modifier.width((4f * scale).dp))

                        // Rule Name and Keeps text
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = info.label,
                                color = if (isTop) Color.White else palette.payTableTextColor,
                                fontSize = (11.5f * scale).sp,
                                fontWeight = if (isTop || isMatched) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (info.keeps.isNotBlank()) {
                                Text(
                                    text = info.keeps,
                                    color = if (isTop) Color(0xFFD5EFDF) else palette.textDimColor,
                                    fontSize = (9.5f * scale).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Match Status Pill / Badge
                        Box(
                            modifier = Modifier.width(statusColW),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            val badgeCorner = (3f * scale).dp
                            if (isTop) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(badgeCorner))
                                        .background(Color(0xFF2E7D32))
                                        .border(1.dp, Color(0xFF81C784), RoundedCornerShape(badgeCorner))
                                        .padding(horizontal = (5f * scale).dp, vertical = (2f * scale).dp)
                                ) {
                                    Text(
                                        text = "✓ TOP CHOICE",
                                        color = Color.White,
                                        fontSize = (8.5f * scale).sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            } else if (isMatched) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(badgeCorner))
                                        .background(Color(0xFFB8860B))
                                        .padding(horizontal = (5f * scale).dp, vertical = (2f * scale).dp)
                                ) {
                                    Text(
                                        text = "#${matched?.row} MATCH",
                                        color = Color.White,
                                        fontSize = (8.5f * scale).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LazyListVerticalScrollbar(
                listState = listState,
                totalItems = totalRules,
                palette = palette,
                scale = scale,
                modifier = Modifier
                    .width((12f * scale).dp)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height((6f * scale).dp))

        // Footer with Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rules evaluated in top-to-bottom priority. Highest matching rule is played.",
                color = palette.textDimColor,
                fontSize = (9.5f * scale).sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.weight(1f).padding(end = (8f * scale).dp)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(rowCorner))
                    .background(Color(0x33FFFFFF))
                    .border(1.dp, palette.goldColor, RoundedCornerShape(rowCorner))
                    .clickable { onClose() }
                    .padding(horizontal = (14f * scale).dp, vertical = (5f * scale).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CLOSE",
                    color = palette.goldColor,
                    fontSize = (11f * scale).sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
