package com.videopoker.android.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import com.videopoker.android.data.export.SavedSoundPreset
import com.videopoker.android.data.models.GameConfig
import com.videopoker.android.data.models.HoldCatalogue
import com.videopoker.android.data.models.HoldChoice
import com.videopoker.android.sound.SoundEffect
import com.videopoker.android.sound.SoundPack
import com.videopoker.android.ui.state.ActiveOverlay
import com.videopoker.android.ui.state.BankrollTargets
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
    onGameSelect: (GameConfig, Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onAddCashAmount: (Double) -> Unit,
    onConfirmCashOut: () -> Unit,
    onSetBankrollTargets: (Boolean, Int, Boolean, Int) -> Unit = { _, _, _, _ -> },
    onToggleRecordingSession: () -> Unit,
    onSaveSessionToDownloads: (String) -> Unit = {},
    onShareSession: (String) -> Unit = {},
    onSingleSoundPackSelect: (SoundPack) -> Unit = {},
    onPerSoundPackSelect: (SoundEffect, SoundPack) -> Unit = { _, _ -> },
    onPreviewSound: (SoundPack, SoundEffect) -> Unit = { _, _ -> },
    onPlaySampleHand: () -> Unit = {},
    onSaveSoundPreset: (String) -> Unit = {},
    onLoadSavedSoundPreset: (SavedSoundPreset) -> Unit = {},
    onExportSoundPresetToDownloads: (String) -> Unit = {},
    onShareSoundPreset: (String) -> Unit = {},
    onImportSoundPresetUri: (Uri) -> Unit = {}
) {
    val activeOverlay = state.activeOverlay ?: return

    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val compactForIme = isImeVisible && activeOverlay == ActiveOverlay.ADD_MONEY
    val shrinkOptionsForIme = isImeVisible && activeOverlay == ActiveOverlay.OPTIONS
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
                    when {
                        compactForIme -> Modifier.wrapContentHeight().heightIn(max = minOf(modal.height, modal.maxHeightForIme))
                        shrinkOptionsForIme -> Modifier.height(minOf(modal.height, modal.maxHeightForIme))
                        else -> Modifier.height(modal.height)
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
                    currentHandsPerDeal = state.handsPerDeal,
                    palette = palette,
                    scale = s,
                    onGameSelect = { selectedGame, hands ->
                        onGameSelect(selectedGame, hands)
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
                    onSetBankrollTargets = onSetBankrollTargets,
                    onVolumeChange = onVolumeChange,
                    onToggleRecordingSession = onToggleRecordingSession,
                    onSaveSessionToDownloads = onSaveSessionToDownloads,
                    onShareSession = onShareSession,
                    onSingleSoundPackSelect = onSingleSoundPackSelect,
                    onPerSoundPackSelect = onPerSoundPackSelect,
                    onPreviewSound = onPreviewSound,
                    onPlaySampleHand = onPlaySampleHand,
                    onSaveSoundPreset = onSaveSoundPreset,
                    onLoadSavedSoundPreset = onLoadSavedSoundPreset,
                    onExportSoundPresetToDownloads = onExportSoundPresetToDownloads,
                    onShareSoundPreset = onShareSoundPreset,
                    onImportSoundPresetUri = onImportSoundPresetUri,
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
                    handsPerDeal = state.handsPerDeal,
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
                    ticketNumber = state.nextVoucherTicketNumber,
                    validationSerial = state.activeVoucherSerial,
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
                HelpSection("Keyboard Shortcuts", "• Space or 0: Deal / Draw (also instant-completes multi-hand draw reveal)\n• 1, 2, 3, 4, 5: Hold / Release card 1 to 5\n• B: Bet one\n• M: Bet max & deal\n• A: Insert / Add money\n• C: Cash out voucher\n• S: Sound on / off\n• Esc: Close any window", palette, scale)
                HelpSection("Pay Table", "Standard hands range from Royal Flush down to Jacks or Better (pair of Jacks, Queens, Kings, or Aces). Bonus games pay extra for specific 4 of a Kind combinations.", palette, scale)
                HelpSection("Multi-Hand Play (3, 5, or 10 Hands)", "• Mode Selection: Choose SINGLE PLAY, TRIPLE PLAY, FIVE PLAY, or TEN PLAY from the top tabs in MORE GAMES.\n• Learn, Practice & Casino: Learn auto-holds and plots mistakes, Practice lets you pick holds and reveals your Practice Review immediately on DRAW, and Casino hides strategy hints while tracking your Session Bankroll.\n• Shared Deal & Individual Draws: Held cards duplicate face-up into every upper hand, then each hand draws independently from its own 47-card stub deck.\n• Swipe or Tap Holds: Tap or swipe across the bottom main hand to toggle HOLD; tap DEAL/DRAW or cards during reveal to jump straight to final results.", palette, scale)
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

private fun handsModeLabel(hands: Int): String = when (hands) {
    3 -> "TRIPLE PLAY"
    5 -> "FIVE PLAY"
    10 -> "TEN PLAY"
    else -> "SINGLE PLAY"
}

@Composable
private fun MoreGamesContent(
    allGames: List<GameConfig>,
    currentGame: GameConfig?,
    currentHandsPerDeal: Int,
    palette: ThemePalette,
    scale: Float,
    onGameSelect: (GameConfig, Int) -> Unit,
    onClose: () -> Unit
) {
    // Stage 1: Family Grid (e.g. 11 Families in 4 columns)
    // Stage 2: Pick Pay Table Grid (e.g. 2 columns showing 9/6, 10/7 with return %)
    var selectedFamily by remember { mutableStateOf<String?>(null) }
    var selectedHandsPerDeal by remember(currentHandsPerDeal) { mutableStateOf(currentHandsPerDeal) }

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

            Spacer(modifier = Modifier.height((8f * scale).dp))

            // Horizontal Mode Selector Tabs (SINGLE PLAY / TRIPLE PLAY / FIVE PLAY / TEN PLAY)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((8f * scale).dp)
            ) {
                val tabCorner = (7f * scale).dp
                GameState.SUPPORTED_HANDS_OPTIONS.forEach { hands ->
                    val isSelectedTab = (selectedHandsPerDeal == hands)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(tabCorner))
                            .background(
                                if (isSelectedTab) {
                                    Brush.verticalGradient(listOf(palette.goldColor, palette.goldColor.copy(alpha = 0.78f)))
                                } else {
                                    Brush.verticalGradient(listOf(palette.panelColor, palette.panelColor.copy(alpha = 0.85f)))
                                }
                            )
                            .border(
                                width = (1.5f * scale).dp,
                                color = if (isSelectedTab) palette.accentColor else palette.goldColor.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(tabCorner)
                            )
                            .clickable { selectedHandsPerDeal = hands }
                            .padding(vertical = (7f * scale).dp, horizontal = (4f * scale).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = handsModeLabel(hands),
                            color = if (isSelectedTab) Color(0xFF14100A) else palette.textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                fontSize = (11f * scale).sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (0.5f * scale).sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height((8f * scale).dp))

            // 4-Column Grid of Authentic Family Tiles matching casino mockup
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = (10f * scale).dp),
                horizontalArrangement = Arrangement.spacedBy((10f * scale).dp),
                verticalArrangement = Arrangement.spacedBy((10f * scale).dp)
            ) {
                items(familyGroups) { (family, games) ->
                    val isCurFamily = (currentGame?.familyName == family && selectedHandsPerDeal == currentHandsPerDeal)
                    val tableCount = games.size
                    val countLabel = if (tableCount == 1) "1 pay table" else "$tableCount pay tables"
                    val tileCorner = (8f * scale).dp

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (tableCount == 1) {
                                    onGameSelect(games.first(), selectedHandsPerDeal)
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
                            color = palette.overlayTextColor,
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
                        text = "${selectedFamily?.uppercase()} • ${handsModeLabel(selectedHandsPerDeal)} — PAY TABLES",
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
                    val isCur = (game.variant == currentGame?.variant && selectedHandsPerDeal == currentHandsPerDeal)
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
                            .clickable { onGameSelect(game, selectedHandsPerDeal) }
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

private enum class OptionsSection(val title: String) {
    PLAY_MODE("PLAY MODE"),
    THEMES_AND_CARDS("THEMES & CARDS"),
    BANKROLL_GOALS("BANKROLL GOALS"),
    SOUNDS("SOUNDS")
}

@Composable
private fun OptionsContent(
    state: GameState,
    palette: ThemePalette,
    scale: Float,
    onModeToggle: (GameMode) -> Unit,
    onThemeSelect: (AppTheme) -> Unit,
    onCardDeckSelect: (CardDeck) -> Unit,
    onSetBankrollTargets: (Boolean, Int, Boolean, Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleRecordingSession: () -> Unit,
    onSaveSessionToDownloads: (String) -> Unit,
    onShareSession: (String) -> Unit,
    onSingleSoundPackSelect: (SoundPack) -> Unit,
    onPerSoundPackSelect: (SoundEffect, SoundPack) -> Unit,
    onPreviewSound: (SoundPack, SoundEffect) -> Unit,
    onPlaySampleHand: () -> Unit,
    onSaveSoundPreset: (String) -> Unit,
    onLoadSavedSoundPreset: (SavedSoundPreset) -> Unit,
    onExportSoundPresetToDownloads: (String) -> Unit,
    onShareSoundPreset: (String) -> Unit,
    onImportSoundPresetUri: (Uri) -> Unit,
    onClose: () -> Unit
) {
    var selectedSection by remember { mutableStateOf(OptionsSection.SOUNDS) }
    var soundSubViewIsMix by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val pillCorner = (6f * scale).dp

    fun sectionSubtitle(section: OptionsSection): String = when (section) {
        OptionsSection.PLAY_MODE -> {
            val rec = if (state.isRecordingSession) "ON" else "OFF"
            "${state.currentMode.displayName} • Rec $rec"
        }
        OptionsSection.THEMES_AND_CARDS ->
            "${state.currentTheme.displayName} • ${if (state.currentCardDeck == CardDeck.MODERN) "Modern" else "Vintage"}"
        OptionsSection.BANKROLL_GOALS -> {
            val w = if (state.bankrollTargets.winGoalEnabled) "+${state.bankrollTargets.winGoalPercent}%" else "Off"
            val l = if (state.bankrollTargets.stopLossEnabled) "-${state.bankrollTargets.stopLossPercent}%" else "Off"
            "$w / $l"
        }
        OptionsSection.SOUNDS -> state.soundConfig.summaryText()
    }

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

        Spacer(modifier = Modifier.height((6f * scale).dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((10f * scale).dp)
        ) {
            // Left Sidebar Navigation (4 Sections)
            Column(
                modifier = Modifier
                    .width((152f * scale).dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(end = (8f * scale).dp),
                verticalArrangement = Arrangement.spacedBy((5f * scale).dp)
            ) {
                OptionsSection.entries.forEach { section ->
                    val isSelected = (selectedSection == section)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(pillCorner))
                            .background(if (isSelected) palette.goldColor else Color(0x1EFFFFFF))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White else Color(0x44FFFFFF),
                                shape = RoundedCornerShape(pillCorner)
                            )
                            .clickable {
                                selectedSection = section
                                coroutineScope.launch { scrollState.scrollTo(0) }
                            }
                            .padding(horizontal = (9f * scale).dp, vertical = (7f * scale).dp),
                        verticalArrangement = Arrangement.spacedBy((1.5f * scale).dp)
                    ) {
                        Text(
                            text = section.title,
                            color = if (isSelected) Color(0xFF14100A) else palette.overlayTextColor,
                            fontSize = (10f * scale).sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = sectionSubtitle(section),
                            color = if (isSelected) Color(0xFF332917) else palette.overlayTextDimColor,
                            fontSize = (8.5f * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Vertical divider between sidebar and content
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0x33FFFFFF))
            )

            // Right Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(bottom = (10f * scale).dp),
                verticalArrangement = Arrangement.spacedBy((10f * scale).dp)
            ) {
                when (selectedSection) {
                    OptionsSection.PLAY_MODE -> OptionsPlayModePane(
                        state = state,
                        palette = palette,
                        scale = scale,
                        onModeToggle = onModeToggle,
                        onToggleRecordingSession = onToggleRecordingSession,
                        onSaveSessionToDownloads = onSaveSessionToDownloads,
                        onShareSession = onShareSession
                    )
                    OptionsSection.THEMES_AND_CARDS -> OptionsLookPane(
                        state = state,
                        palette = palette,
                        scale = scale,
                        onThemeSelect = onThemeSelect,
                        onCardDeckSelect = onCardDeckSelect
                    )
                    OptionsSection.BANKROLL_GOALS -> OptionsTargetsPane(
                        state = state,
                        palette = palette,
                        scale = scale,
                        onSetBankrollTargets = onSetBankrollTargets
                    )
                    OptionsSection.SOUNDS -> OptionsSoundsPane(
                        state = state,
                        palette = palette,
                        scale = scale,
                        isMixView = soundSubViewIsMix,
                        onSelectView = { soundSubViewIsMix = it },
                        onVolumeChange = onVolumeChange,
                        onSingleSoundPackSelect = onSingleSoundPackSelect,
                        onPerSoundPackSelect = onPerSoundPackSelect,
                        onPreviewSound = onPreviewSound,
                        onPlaySampleHand = onPlaySampleHand,
                        onSaveSoundPreset = onSaveSoundPreset,
                        onLoadSavedSoundPreset = onLoadSavedSoundPreset,
                        onExportSoundPresetToDownloads = onExportSoundPresetToDownloads,
                        onShareSoundPreset = onShareSoundPreset,
                        onImportSoundPresetUri = onImportSoundPresetUri
                    )
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

        Spacer(modifier = Modifier.height((5f * scale).dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x28FFFFFF))
        )
        Spacer(modifier = Modifier.height((4f * scale).dp))
        Text(
            text = "Sounds: ${state.soundConfig.summaryText()} • Volume ${(state.soundVolume * 100).toInt()}%",
            color = palette.overlayTextDimColor,
            fontSize = (9.5f * scale).sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OptionsPlayModePane(
    state: GameState,
    palette: ThemePalette,
    scale: Float,
    onModeToggle: (GameMode) -> Unit,
    onToggleRecordingSession: () -> Unit,
    onSaveSessionToDownloads: (String) -> Unit,
    onShareSession: (String) -> Unit
) {
    val pillCorner = (6f * scale).dp

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
        Text(
            text = if (state.isMultiHand) {
                "All training modes (Learn, Practice, Casino) are active in Multi-Hand play."
            } else {
                "All training modes are available in Single Play."
            },
            color = palette.overlayTextDimColor,
            fontSize = (9.5f * scale).sp
        )
    }

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
            .padding(horizontal = (12f * scale).dp, vertical = (9f * scale).dp),
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

@Composable
private fun OptionsLookPane(
    state: GameState,
    palette: ThemePalette,
    scale: Float,
    onThemeSelect: (AppTheme) -> Unit,
    onCardDeckSelect: (CardDeck) -> Unit
) {
    val pillCorner = (6f * scale).dp
    val context = LocalContext.current

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

    Column(verticalArrangement = Arrangement.spacedBy((4f * scale).dp)) {
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
}

@Composable
private fun OptionsTargetsPane(
    state: GameState,
    palette: ThemePalette,
    scale: Float,
    onSetBankrollTargets: (Boolean, Int, Boolean, Int) -> Unit
) {
    val targetPresets = listOf(5, 10, 25, 50, 75, 100)
    Column(verticalArrangement = Arrangement.spacedBy((6f * scale).dp)) {
        Text(
            text = "BANKROLL TARGETS",
            color = palette.goldColor,
            fontSize = (10.5f * scale).sp,
            fontWeight = FontWeight.Bold
        )
        BankrollTargetConfigRow(
            title = "WIN GOAL (+% ABOVE BUY-IN)",
            enabled = state.bankrollTargets.winGoalEnabled,
            currentPercent = state.bankrollTargets.winGoalPercent,
            presets = targetPresets,
            minPercent = BankrollTargets.MIN_WIN_GOAL_PERCENT,
            maxPercent = BankrollTargets.MAX_WIN_GOAL_PERCENT,
            accentColor = Color(0xFF4ADE80),
            signPrefix = "+",
            palette = palette,
            scale = scale,
            onToggleEnabled = { enabled ->
                onSetBankrollTargets(
                    enabled,
                    state.bankrollTargets.winGoalPercent,
                    state.bankrollTargets.stopLossEnabled,
                    state.bankrollTargets.stopLossPercent
                )
            },
            onSelectPercent = { pct ->
                onSetBankrollTargets(
                    true,
                    pct,
                    state.bankrollTargets.stopLossEnabled,
                    state.bankrollTargets.stopLossPercent
                )
            }
        )
        BankrollTargetConfigRow(
            title = "STOP-LOSS (-% BELOW BUY-IN)",
            enabled = state.bankrollTargets.stopLossEnabled,
            currentPercent = state.bankrollTargets.stopLossPercent,
            presets = targetPresets,
            minPercent = BankrollTargets.MIN_STOP_LOSS_PERCENT,
            maxPercent = BankrollTargets.MAX_STOP_LOSS_PERCENT,
            accentColor = Color(0xFFF87171),
            signPrefix = "-",
            palette = palette,
            scale = scale,
            onToggleEnabled = { enabled ->
                onSetBankrollTargets(
                    state.bankrollTargets.winGoalEnabled,
                    state.bankrollTargets.winGoalPercent,
                    enabled,
                    state.bankrollTargets.stopLossPercent
                )
            },
            onSelectPercent = { pct ->
                onSetBankrollTargets(
                    state.bankrollTargets.winGoalEnabled,
                    state.bankrollTargets.winGoalPercent,
                    true,
                    pct
                )
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionsSoundsPane(
    state: GameState,
    palette: ThemePalette,
    scale: Float,
    isMixView: Boolean,
    onSelectView: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSingleSoundPackSelect: (SoundPack) -> Unit,
    onPerSoundPackSelect: (SoundEffect, SoundPack) -> Unit,
    onPreviewSound: (SoundPack, SoundEffect) -> Unit,
    onPlaySampleHand: () -> Unit,
    onSaveSoundPreset: (String) -> Unit,
    onLoadSavedSoundPreset: (SavedSoundPreset) -> Unit,
    onExportSoundPresetToDownloads: (String) -> Unit,
    onShareSoundPreset: (String) -> Unit,
    onImportSoundPresetUri: (Uri) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val pillCorner = (6f * scale).dp
    var presetNameText by remember { mutableStateOf("") }

    val openPresetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportSoundPresetUri(uri)
        }
    }

    val uniPack = state.soundConfig.uniformPack()
    val dominantPack = state.soundConfig.dominantPack()

    // Volume Bar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(pillCorner))
            .background(Color(0x18FFFFFF))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(pillCorner))
            .padding(horizontal = (10f * scale).dp, vertical = (4f * scale).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((8f * scale).dp)
    ) {
        Text(
            text = "VOLUME",
            color = palette.goldColor,
            fontSize = (10f * scale).sp,
            fontWeight = FontWeight.Black
        )
        Slider(
            value = state.soundVolume,
            onValueChange = onVolumeChange,
            modifier = Modifier
                .weight(1f)
                .height((26f * scale).dp),
            colors = SliderDefaults.colors(
                thumbColor = palette.goldColor,
                activeTrackColor = palette.goldColor,
                inactiveTrackColor = Color(0x44FFFFFF)
            )
        )
        Text(
            text = "${(state.soundVolume * 100).toInt()}%",
            color = palette.overlayTextColor,
            fontSize = (10.5f * scale).sp,
            fontWeight = FontWeight.Bold
        )
    }

    // One instrument / Pick each sound subtabs
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((6f * scale).dp)
    ) {
        listOf(false to "One theme for all", true to "Pick each sound").forEach { (mixTab, label) ->
            val selected = (isMixView == mixTab)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(pillCorner))
                    .background(if (selected) palette.goldColor.copy(alpha = 0.22f) else Color(0x18FFFFFF))
                    .border(
                        width = 1.dp,
                        color = if (selected) palette.goldColor else Color(0x33FFFFFF),
                        shape = RoundedCornerShape(pillCorner)
                    )
                    .clickable { onSelectView(mixTab) }
                    .padding(vertical = (6f * scale).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) palette.goldColor else palette.overlayTextColor,
                    fontSize = (10.5f * scale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (!isMixView) {
        Text(
            text = if (uniPack != null) {
                "Pick one theme to set all 16 game sounds at once."
            } else {
                "Currently using a custom mix. Tap any theme below to set all 16 sounds to it."
            },
            color = palette.overlayTextDimColor,
            fontSize = (9.5f * scale).sp
        )
        Column(verticalArrangement = Arrangement.spacedBy((5f * scale).dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((5f * scale).dp)
            ) {
                listOf(SoundPack.CLASSIC, SoundPack.NONE).forEach { pack ->
                    val isCur = (uniPack == pack)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(pillCorner))
                            .background(if (isCur) palette.goldColor else Color(0x1EFFFFFF))
                            .border(
                                width = 1.dp,
                                color = if (isCur) Color.White else Color(0x38FFFFFF),
                                shape = RoundedCornerShape(pillCorner)
                            )
                            .clickable { onSingleSoundPackSelect(pack) }
                            .padding(horizontal = (7f * scale).dp, vertical = (7f * scale).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pack.displayName,
                            color = if (isCur) Color(0xFF14100A) else palette.overlayTextColor,
                            fontSize = (10f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            SoundPack.entries.filter { it != SoundPack.CLASSIC && it != SoundPack.NONE }.chunked(4).forEach { rowPacks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((5f * scale).dp)
                ) {
                    rowPacks.forEach { pack ->
                        val isCur = (uniPack == pack)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(pillCorner))
                                .background(if (isCur) palette.goldColor else Color(0x1EFFFFFF))
                                .border(
                                    width = 1.dp,
                                    color = if (isCur) Color.White else Color(0x38FFFFFF),
                                    shape = RoundedCornerShape(pillCorner)
                                )
                                .clickable { onSingleSoundPackSelect(pack) }
                                .padding(horizontal = (7f * scale).dp, vertical = (7f * scale).dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = pack.displayName,
                                color = if (isCur) Color(0xFF14100A) else palette.overlayTextColor,
                                fontSize = (10f * scale).sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    repeat(4 - rowPacks.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        Text(
            text = "Tap ◀ ▶ to step through themes. Tap a theme name to hear it again.",
            color = palette.overlayTextDimColor,
            fontSize = (9.5f * scale).sp
        )
        Column(verticalArrangement = Arrangement.spacedBy((5f * scale).dp)) {
            SoundEffect.entries.chunked(2).forEach { rowEffects ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((6f * scale).dp)
                ) {
                    rowEffects.forEach { effect ->
                        val activePack = state.soundConfig.packFor(effect)
                        val isChanged = (uniPack == null && activePack != dominantPack)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(pillCorner))
                                .background(if (isChanged) Color(0x1A7BD88F) else Color(0x18FFFFFF))
                                .border(
                                    width = 1.dp,
                                    color = if (isChanged) Color(0x887BD88F) else Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(pillCorner)
                                )
                                .padding(horizontal = (7f * scale).dp, vertical = (4.5f * scale).dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((5f * scale).dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy((1f * scale).dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy((4f * scale).dp)
                                ) {
                                    Text(
                                        text = effect.displayName,
                                        color = palette.overlayTextColor,
                                        fontSize = (10f * scale).sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isChanged) {
                                        Box(
                                            modifier = Modifier
                                                .size((6f * scale).dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF7BD88F))
                                        )
                                    }
                                }
                                Text(
                                    text = effect.group.displayName.uppercase(),
                                    color = palette.overlayTextDimColor,
                                    fontSize = (8f * scale).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }

                            // Stepper: [◀] [Instrument] [▶]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((3f * scale).dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size((24f * scale).dp)
                                        .clip(RoundedCornerShape((4f * scale).dp))
                                        .background(Color(0x28FFFFFF))
                                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape((4f * scale).dp))
                                        .clickable { onPerSoundPackSelect(effect, activePack.previous()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "◀",
                                        color = palette.overlayTextColor,
                                        fontSize = (9.5f * scale).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width((72f * scale).dp)
                                        .height((24f * scale).dp)
                                        .clip(RoundedCornerShape((4f * scale).dp))
                                        .background(Color(0x44000000))
                                        .border(
                                            1.dp,
                                            if (isChanged) palette.goldColor.copy(alpha = 0.6f) else Color(0x33FFFFFF),
                                            RoundedCornerShape((4f * scale).dp)
                                        )
                                        .clickable { onPreviewSound(activePack, effect) }
                                        .padding(horizontal = (4f * scale).dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = activePack.displayName,
                                        color = if (isChanged) palette.goldColor else palette.overlayTextColor,
                                        fontSize = (9.5f * scale).sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size((24f * scale).dp)
                                        .clip(RoundedCornerShape((4f * scale).dp))
                                        .background(Color(0x28FFFFFF))
                                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape((4f * scale).dp))
                                        .clickable { onPerSoundPackSelect(effect, activePack.next()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "▶",
                                        color = palette.overlayTextColor,
                                        fontSize = (9.5f * scale).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    if (rowEffects.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    // Sample hand + JSON Preset Save / Export / Share / Import
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((6f * scale).dp),
        verticalArrangement = Arrangement.spacedBy((5f * scale).dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .alpha(if (state.isPlayingSampleHand) 0.55f else 1f)
                .clip(RoundedCornerShape(pillCorner))
                .background(palette.goldColor)
                .clickable(enabled = !state.isPlayingSampleHand) { onPlaySampleHand() }
                .padding(horizontal = (10f * scale).dp, vertical = (6f * scale).dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▶ Play a sample hand",
                color = Color(0xFF14100A),
                fontSize = (10f * scale).sp,
                fontWeight = FontWeight.Black
            )
        }

        Box(
            modifier = Modifier
                .width((118f * scale).dp)
                .clip(RoundedCornerShape((5f * scale).dp))
                .background(Color(0x2A000000))
                .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape((5f * scale).dp))
                .padding(horizontal = (7f * scale).dp, vertical = (5.5f * scale).dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (presetNameText.isEmpty()) {
                Text(
                    text = "Preset name...",
                    color = Color(0x88F5F0E6),
                    fontSize = (9.5f * scale).sp,
                    maxLines = 1
                )
            }
            BasicTextField(
                value = presetNameText,
                onValueChange = { if (it.length <= 40) presetNameText = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = palette.overlayTextColor,
                    fontSize = (9.5f * scale).sp,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(palette.goldColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val name = presetNameText.trim().ifBlank { state.soundConfig.summaryText() }
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onSaveSoundPreset(name)
                        presetNameText = ""
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(pillCorner))
                .background(Color(0x26FFFFFF))
                .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(pillCorner))
                .clickable {
                    val name = presetNameText.trim().ifBlank { state.soundConfig.summaryText() }
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSaveSoundPreset(name)
                    presetNameText = ""
                }
                .padding(horizontal = (9f * scale).dp, vertical = (6f * scale).dp)
        ) {
            Text(
                text = "SAVE",
                color = palette.overlayTextColor,
                fontSize = (9.5f * scale).sp,
                fontWeight = FontWeight.Black
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(pillCorner))
                .background(Color(0x22FFFFFF))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(pillCorner))
                .clickable {
                    val name = presetNameText.trim().ifBlank { state.soundConfig.summaryText() }
                    onExportSoundPresetToDownloads(name)
                }
                .padding(horizontal = (8f * scale).dp, vertical = (6f * scale).dp)
        ) {
            Text(
                text = "DOWNLOAD .JSON",
                color = palette.overlayTextColor,
                fontSize = (9f * scale).sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(pillCorner))
                .background(Color(0x22FFFFFF))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(pillCorner))
                .clickable {
                    val name = presetNameText.trim().ifBlank { state.soundConfig.summaryText() }
                    onShareSoundPreset(name)
                }
                .padding(horizontal = (8f * scale).dp, vertical = (6f * scale).dp)
        ) {
            Text(
                text = "SHARE .JSON",
                color = palette.overlayTextColor,
                fontSize = (9f * scale).sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(pillCorner))
                .background(Color(0x22FFFFFF))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(pillCorner))
                .clickable { openPresetLauncher.launch(arrayOf("application/json", "*/*")) }
                .padding(horizontal = (8f * scale).dp, vertical = (6f * scale).dp)
        ) {
            Text(
                text = "LOAD .JSON",
                color = palette.overlayTextColor,
                fontSize = (9f * scale).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (state.savedSoundPresets.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy((4f * scale).dp)) {
            Text(
                text = "SAVED SOUND PRESETS",
                color = palette.goldColor,
                fontSize = (9.5f * scale).sp,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy((5f * scale).dp),
                verticalArrangement = Arrangement.spacedBy((4f * scale).dp)
            ) {
                state.savedSoundPresets.forEach { preset ->
                    val isActive = (state.soundConfig.presetName == preset.name)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(pillCorner))
                            .background(if (isActive) palette.goldColor else Color(0x1EFFFFFF))
                            .border(
                                width = 1.dp,
                                color = if (isActive) Color.White else Color(0x38FFFFFF),
                                shape = RoundedCornerShape(pillCorner)
                            )
                            .clickable { onLoadSavedSoundPreset(preset) }
                            .padding(horizontal = (8f * scale).dp, vertical = (4.5f * scale).dp)
                    ) {
                        Text(
                            text = preset.name,
                            color = if (isActive) Color(0xFF14100A) else palette.overlayTextColor,
                            fontSize = (9.5f * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    val statusMsg = state.soundPresetStatus
    if (!statusMsg.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape((5f * scale).dp))
                .background(Color(0x2E2E8540))
                .border(1.dp, Color(0xFF56C470), RoundedCornerShape((5f * scale).dp))
                .padding(horizontal = (8f * scale).dp, vertical = (4.5f * scale).dp)
        ) {
            Text(
                text = statusMsg,
                color = Color(0xFFB8E6C4),
                fontSize = (9.5f * scale).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BankrollTargetConfigRow(
    title: String,
    enabled: Boolean,
    currentPercent: Int,
    presets: List<Int>,
    minPercent: Int,
    maxPercent: Int,
    accentColor: Color,
    signPrefix: String,
    palette: ThemePalette,
    scale: Float,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPercent: (Int) -> Unit
) {
    val pillCorner = (6f * scale).dp
    var customText by remember(currentPercent) { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submitCustomPercent() {
        val parsed = customText.trim().toIntOrNull() ?: return
        val clamped = parsed.coerceIn(minPercent, maxPercent)
        customText = ""
        focusManager.clearFocus()
        keyboardController?.hide()
        onSelectPercent(clamped)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(pillCorner))
            .background(Color(0x1EFFFFFF))
            .border(
                width = 1.dp,
                color = if (enabled) accentColor.copy(alpha = 0.55f) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(pillCorner)
            )
            .padding(horizontal = (10f * scale).dp, vertical = (7f * scale).dp),
        verticalArrangement = Arrangement.spacedBy((6f * scale).dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((6f * scale).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size((8f * scale).dp)
                        .clip(CircleShape)
                        .background(if (enabled) accentColor else Color(0xFF777777))
                )
                Text(
                    text = title,
                    color = Color(0xFFF5F0E6),
                    fontSize = (9.5f * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((6f * scale).dp)
            ) {
                Text(
                    text = if (enabled) "$signPrefix$currentPercent%" else "OFF",
                    color = if (enabled) accentColor else Color(0xFF9E9487),
                    fontSize = (10f * scale).sp,
                    fontWeight = FontWeight.Black
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((4f * scale).dp))
                        .background(if (enabled) accentColor.copy(alpha = 0.25f) else Color(0x33FFFFFF))
                        .border(
                            1.dp,
                            if (enabled) accentColor else Color(0x44FFFFFF),
                            RoundedCornerShape((4f * scale).dp)
                        )
                        .clickable { onToggleEnabled(!enabled) }
                        .padding(horizontal = (8f * scale).dp, vertical = (2.5f * scale).dp)
                ) {
                    Text(
                        text = if (enabled) "ON" else "OFF",
                        color = if (enabled) accentColor else Color(0xFFD6CEC2),
                        fontSize = (9f * scale).sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((5f * scale).dp),
            verticalArrangement = Arrangement.spacedBy((4f * scale).dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { preset ->
                val isSelected = enabled && (currentPercent == preset)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((5f * scale).dp))
                        .background(if (isSelected) palette.goldColor else Color(0x22FFFFFF))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.White else Color(0x33FFFFFF),
                            shape = RoundedCornerShape((5f * scale).dp)
                        )
                        .clickable { onSelectPercent(preset) }
                        .padding(horizontal = (9f * scale).dp, vertical = (4f * scale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$preset%",
                        color = if (isSelected) Color(0xFF14100A) else Color(0xFFF5F0E6),
                        fontSize = (9.5f * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width((60f * scale).dp)
                    .clip(RoundedCornerShape((5f * scale).dp))
                    .background(Color(0x2A000000))
                    .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape((5f * scale).dp))
                    .padding(horizontal = (6f * scale).dp, vertical = (4f * scale).dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (customText.isEmpty()) {
                    Text(
                        text = "Custom %",
                        color = Color(0x88F5F0E6),
                        fontSize = (8.5f * scale).sp,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = customText,
                    onValueChange = { input ->
                        if (input.length <= 3 && input.all { it.isDigit() }) {
                            customText = input
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFFF5F0E6),
                        fontSize = (9.5f * scale).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(palette.goldColor),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitCustomPercent() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape((5f * scale).dp))
                    .background(palette.goldColor.copy(alpha = if (customText.isNotEmpty()) 1f else 0.35f))
                    .clickable(enabled = customText.isNotEmpty()) { submitCustomPercent() }
                    .padding(horizontal = (8f * scale).dp, vertical = (4f * scale).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SET",
                    color = Color(0xFF14100A),
                    fontSize = (9f * scale).sp,
                    fontWeight = FontWeight.Black
                )
            }
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
    handsPerDeal: Int,
    palette: ThemePalette,
    scale: Float,
    onClose: () -> Unit
) {
    val betSummary = if (handsPerDeal > 1) {
        " • BET $currentBet/HAND (${currentBet * handsPerDeal} TOTAL)"
    } else {
        ""
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${game?.displayName ?: "PAYOUT SCHEDULE"} (FULL TABLE)$betSummary",
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
            showFullTable = true,
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
                    color = palette.overlayTextColor,
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
                    color = palette.overlayTextDimColor,
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
                    color = palette.overlayTextColor,
                    fontSize = (19f * effScale).sp,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = "Select a bill or enter an amount:",
                color = palette.overlayTextColor,
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
                            color = palette.overlayTextDimColor,
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

private fun Modifier.verticalTicketRotate(degrees: Float): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth
            )
        )
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2)
            )
        }
    }
    .graphicsLayer { rotationZ = degrees }

@Composable
private fun Code128BarcodeCanvas(
    text: String,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val runs = remember(text) { Code128Encoder.encodeRunsB(text) }
    val totalUnits = remember(runs) { runs.sumOf { it.second }.coerceAtLeast(1) }

    Canvas(modifier = modifier) {
        val unitW = size.width / totalUnits.toFloat()
        var cursor = 0f
        for ((isBar, units) in runs) {
            val w = units * unitW
            if (isBar) {
                drawRect(
                    color = barColor,
                    topLeft = Offset(cursor, 0f),
                    size = Size(w.coerceAtLeast(1f), size.height)
                )
            }
            cursor += w
        }
    }
}

@Composable
private fun CashOutContent(
    cashBalance: Double,
    currentDenom: Double,
    ticketNumber: Int,
    validationSerial: String,
    palette: ThemePalette,
    scale: Float,
    onConfirmCashOut: () -> Unit,
    onClose: () -> Unit
) {
    val voucherCorner = (8f * scale).dp
    val btnCorner = (6f * scale).dp
    val details = remember(cashBalance, currentDenom, ticketNumber, validationSerial) {
        TitoVoucherFormatter.createDetails(
            cashBalance = cashBalance,
            currentDenom = currentDenom,
            ticketNumber = ticketNumber,
            validationNumber = validationSerial
        )
    }

    val inkDark = Color(0xFF0E0E0E)
    val inkSecondary = Color(0xFF24221D)
    val paperBg = Color(0xFFF6F0DE)
    val paperBorder = Color(0xFFC2B79B)

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
                fontSize = (14f * scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (0.8f * scale).sp
            )
            CloseButton(palette = palette, scale = scale, onClose = onClose)
        }

        // Authentic Vegas TITO Thermal Ticket Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(0.985f)
                .padding(top = (2f * scale).dp, bottom = (4f * scale).dp)
                .shadow((6f * scale).dp, RoundedCornerShape(voucherCorner))
                .clip(RoundedCornerShape(voucherCorner))
                .background(paperBg)
                .border((1.5f * scale).dp, paperBorder, RoundedCornerShape(voucherCorner))
                .padding(horizontal = (6f * scale).dp, vertical = (4f * scale).dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side Rail: INSERT THIS SIDE UP
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = (2f * scale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "INSERT THIS SIDE UP",
                        color = inkDark,
                        fontSize = (10f * scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (0.7f * scale).sp,
                        maxLines = 1,
                        modifier = Modifier.verticalTicketRotate(-90f)
                    )
                }

                // Main Center Ticket Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = (6f * scale).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = TitoVoucherFormatter.CASINO_NAME,
                        color = inkDark,
                        fontSize = (21f * scale).sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((1.5f * scale).dp)
                    ) {
                        Text(
                            text = TitoVoucherFormatter.CASINO_ADDRESS_LINE_1,
                            color = inkSecondary,
                            fontSize = (10.5f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                        Text(
                            text = "${TitoVoucherFormatter.CASINO_ADDRESS_LINE_2}  •  ${TitoVoucherFormatter.CASINO_URL}",
                            color = inkSecondary,
                            fontSize = (10.5f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }

                    Text(
                        text = "G A M I N G   V O U C H E R",
                        color = inkDark,
                        fontSize = (15f * scale).sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )

                    // Genuine scannable Code 128B Barcode ("ROYAL FLUSH NOT FOUND")
                    Code128BarcodeCanvas(
                        text = TitoVoucherFormatter.BARCODE_EASTER_EGG_TEXT,
                        barColor = inkDark,
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .height((34f * scale).dp)
                    )

                    Text(
                        text = details.validationNumber,
                        color = inkDark,
                        fontSize = (12f * scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((2f * scale).dp)
                    ) {
                        Text(
                            text = "VALIDATION   ${details.validationNumber}",
                            color = inkDark,
                            fontSize = (11.5f * scale).sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                        Text(
                            text = "${details.dateText}    ${details.timeText}    TICKET # ${details.ticketNumber}",
                            color = inkSecondary,
                            fontSize = (11f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }

                    Text(
                        text = details.amountWords,
                        color = inkDark,
                        fontSize = (11.5f * scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = details.amountText,
                            color = inkDark,
                            fontSize = (27f * scale).sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif,
                            maxLines = 1,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                            modifier = Modifier.align(Alignment.Center)
                        )
                        Text(
                            text = "ASSET# ${details.assetNumber}",
                            color = inkSecondary,
                            fontSize = (10.5f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }

                // Right Side Rails: vertical Validation Serial + INSERT THIS SIDE UP
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((4f * scale).dp)
                ) {
                    Text(
                        text = details.validationNumber,
                        color = inkSecondary,
                        fontSize = (9.5f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier.verticalTicketRotate(90f)
                    )
                    Text(
                        text = "INSERT THIS SIDE UP",
                        color = inkDark,
                        fontSize = (10f * scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (0.7f * scale).sp,
                        maxLines = 1,
                        modifier = Modifier.verticalTicketRotate(90f)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy((16f * scale).dp)) {
            Box(
                modifier = Modifier
                    .width((134f * scale).dp)
                    .height((33f * scale).dp)
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
                    .width((114f * scale).dp)
                    .height((33f * scale).dp)
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
                    color = palette.overlayTextDimColor,
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
                                color = if (isTop) Color.White else palette.overlayTextColor,
                                fontSize = (11.5f * scale).sp,
                                fontWeight = if (isTop || isMatched) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (info.keeps.isNotBlank()) {
                                Text(
                                    text = info.keeps,
                                    color = if (isTop) Color(0xFFD5EFDF) else palette.overlayTextDimColor,
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
                color = palette.overlayTextDimColor,
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
