package com.videopoker.android.ui.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import com.videopoker.android.data.engine.Deck
import com.videopoker.android.data.engine.HandClassifier
import com.videopoker.android.data.engine.StrategyEngine
import com.videopoker.android.data.export.PlayedGameLog
import com.videopoker.android.data.export.SessionExporter
import com.videopoker.android.data.export.SessionVoucher
import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.GameConfig
import com.videopoker.android.data.models.HandRank
import com.videopoker.android.data.models.HoldChoice
import com.videopoker.android.sound.SoundEffect
import com.videopoker.android.sound.SoundManager
import com.videopoker.android.ui.components.TitoVoucherFormatter
import com.videopoker.android.ui.theme.AppTheme
import com.videopoker.android.ui.theme.CardDeck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime
import kotlin.math.roundToLong

class GameViewModel @JvmOverloads constructor(
    initialState: GameState = GameState()
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private var deck: Deck = Deck()
    private var soundManager: SoundManager? = null
    private var prefs: android.content.SharedPreferences? = null
    private var sessionsRoot: File? = null
    private var activePlayedLog: PlayedGameLog? = null
    private var sessionMoneyInCents: Long = 0L
    private var pendingAllHoldChoices: List<HoldChoice> = emptyList()
    private var gameSelectedBannerJob: Job? = null
    private var dealtDockJob: Job? = null
    private var multiRevealJob: Job? = null
    private var pendingMultiSettlement: PendingMultiSettlement? = null

    private data class PendingMultiSettlement(
        val game: GameConfig,
        val betPerHand: Int,
        val denom: Double,
        val dealtCards: List<Card>,
        val heldMask: List<Boolean>,
        val bestDrawnCards: List<Card>,
        val bestFinalCards: List<Card>,
        val bestRank: HandRank,
        val allLines: List<MultiHandLine>,
        val totalWinCredits: Int,
        val totalWonCash: Double,
        var committed: Boolean = false
    )

    fun loadGames(context: Context) {
        if (soundManager == null) {
            soundManager = SoundManager(context.applicationContext)
            soundManager?.setVolume(_uiState.value.soundVolume)
        }
        val appPrefs = prefs ?: context.applicationContext
            .getSharedPreferences("vp_prefs", Context.MODE_PRIVATE)
            .also { prefs = it }
        val savedDeck = CardDeck.fromId(appPrefs.getString("card_deck", null))
        val savedTicketNum = appPrefs.getInt("next_voucher_ticket", 3914).coerceAtLeast(3914)
        val savedTargets = BankrollTargets(
            winGoalEnabled = appPrefs.getBoolean("win_goal_enabled", true),
            winGoalPercent = appPrefs.getInt("win_goal_percent", BankrollTargets.DEFAULT_WIN_GOAL_PERCENT)
                .coerceIn(BankrollTargets.MIN_WIN_GOAL_PERCENT, BankrollTargets.MAX_WIN_GOAL_PERCENT),
            stopLossEnabled = appPrefs.getBoolean("stop_loss_enabled", true),
            stopLossPercent = appPrefs.getInt("stop_loss_percent", BankrollTargets.DEFAULT_STOP_LOSS_PERCENT)
                .coerceIn(BankrollTargets.MIN_STOP_LOSS_PERCENT, BankrollTargets.MAX_STOP_LOSS_PERCENT)
        )
        val root = File(context.applicationContext.filesDir, "recorded_sessions").apply { mkdirs() }
        sessionsRoot = root

        viewModelScope.launch {
            try {
                val (loadedList, saved) = withContext(Dispatchers.IO) {
                    val assetManager = context.applicationContext.assets
                    val files = assetManager.list("games") ?: emptyArray()
                    val games = mutableListOf<GameConfig>()

                    for (file in files) {
                        if (file.endsWith(".json")) {
                            assetManager.open("games/$file").use { stream ->
                                val reader = InputStreamReader(stream)
                                val json = JsonParser.parseReader(reader).asJsonObject
                                val config = StrategyEngine.parseGameConfig(json, file)
                                games.add(config)
                            }
                        }
                    }

                    // Sort games: Double Bonus 10/7 first, then alphabetical
                    games.sortBy { if (it.variant.contains("double_bonus_10_7")) "000" else it.displayName }
                    val recorded = PlayedGameLog.listRecordedSessions(root)
                    games to recorded
                }

                _uiState.update { current ->
                    current.copy(
                        allGames = loadedList,
                        currentCardDeck = savedDeck,
                        nextVoucherTicketNumber = savedTicketNum,
                        bankrollTargets = savedTargets,
                        currentGame = null,
                        cards = emptyList(),
                        heldMask = listOf(false, false, false, false, false),
                        revealedMask = listOf(false, false, false, false, false),
                        holdChoices = emptyList(),
                        handRank = HandRank.NOTHING,
                        winAmount = 0,
                        dealState = DealState.READY_TO_DEAL,
                        recordedSessions = saved
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissSplash() {
        _uiState.update { it.copy(showSplash = false) }
    }

    fun onDealDraw() {
        val state = _uiState.value
        if (state.isRevealingMultiDraw) {
            skipMultiReveal()
            return
        }
        if (state.isAnimating) return
        clearGameSelectedBanner()
        if (state.currentGame == null) {
            openOverlay(ActiveOverlay.MORE_GAMES)
            return
        }
        val game = state.currentGame

        if (state.isMultiHand) {
            handleMultiDealDraw(state, game)
            return
        }

        // Authentic speeds from desktop SPEED_MS = [250, 170, 100]
        val speedDelays = listOf(250L, 170L, 100L)
        val delayMs = speedDelays.getOrElse(state.speedLevel - 1) { 170L }

        when (state.dealState) {
            DealState.READY_TO_DEAL, DealState.SHOWDOWN_WIN, DealState.SHOWDOWN_NO_WIN -> {
                // DEAL NEW HAND
                val cost = state.currentBet * state.currentDenom
                if (state.cashBalance < cost) {
                    // Out of funds: Prompt to add cash!
                    openOverlay(ActiveOverlay.ADD_MONEY)
                    return
                }

                deck = Deck()
                val newDeal = deck.deal(5)
                val payKeys = game.payTable.rows.map { it.key }.toSet()
                val currentRank = HandClassifier.classifyHand(newDeal, payKeys)
                val allChoices = StrategyEngine.evaluateHoldChoices(newDeal, game, maxChoices = Int.MAX_VALUE)
                pendingAllHoldChoices = allChoices
                val choices = allChoices.take(15)

                if (state.isRecordingSession && activePlayedLog == null) {
                    val root = sessionsRoot
                    if (root != null) {
                        val startingCents = (state.cashBalance * 100.0).roundToLong()
                        sessionMoneyInCents = startingCents
                        activePlayedLog = PlayedGameLog(
                            game = game,
                            outputRoot = root,
                            startingCents = startingCents,
                            startedAt = LocalDateTime.now(),
                            startingDenomination = state.currentDenom,
                            startingCredits = state.currentBet,
                            handsPerDeal = 1
                        )
                    }
                }

                // In Learn mode, auto-hold top choice. In Practice/Casino, start empty!
                val autoMask = if (state.currentMode == GameMode.LEARN && choices.isNotEmpty()) {
                    choices.first().holdMask
                } else {
                    listOf(false, false, false, false, false)
                }

                viewModelScope.launch {
                    soundManager?.play(SoundEffect.DEAL)
                    _uiState.update {
                        it.copy(
                            isAnimating = true,
                            cards = newDeal,
                            revealedMask = listOf(false, false, false, false, false),
                            heldMask = listOf(false, false, false, false, false),
                            holdChoices = emptyList(),
                            handRank = HandRank.NOTHING,
                            winAmount = 0,
                            cashBalance = it.cashBalance - cost,
                            dealtDenom = state.currentDenom,
                            dealState = DealState.DEALT_CHOOSING_HOLDS,
                            activeOverlay = if (it.activeOverlay == ActiveOverlay.DENOM) null else it.activeOverlay,
                            practiceFeedback = null,
                            practiceMadeTopChoice = null,
                            showPracticePlot = false
                        )
                    }

                    // Reveal cards sequentially one-by-one from slot 0 to 4
                    for (i in 0 until 5) {
                        delay(delayMs)
                        _uiState.update { s ->
                            val mask = s.revealedMask.toMutableList()
                            mask[i] = true
                            s.copy(revealedMask = mask)
                        }
                    }

                    // Hand complete: apply auto-holds, strategy advisor, and hand rank
                    _uiState.update { s ->
                        s.copy(
                            isAnimating = false,
                            revealedMask = listOf(true, true, true, true, true),
                            heldMask = autoMask,
                            holdChoices = choices,
                            handRank = currentRank
                        )
                    }
                }
            }
            DealState.DEALT_CHOOSING_HOLDS -> {
                // DRAW REPLACEMENTS
                val settlementDenom = resolveSettlementDenom(state)
                val unheldIndices = (0 until 5).filter { !state.heldMask.getOrElse(it) { false } }

                viewModelScope.launch {
                    soundManager?.play(SoundEffect.DRAW)

                    if (unheldIndices.isEmpty()) {
                        // All 5 cards held (pat hand)
                        finalizeHandSettlement(
                            game = game,
                            bet = state.currentBet,
                            denom = settlementDenom,
                            dealtCards = state.cards,
                            heldMask = state.heldMask,
                            drawnCards = emptyList(),
                            finalCardList = state.cards,
                            mode = state.currentMode
                        )
                    } else {
                        _uiState.update { it.copy(isAnimating = true) }

                        val drawnCards = deck.draw(state.cards, state.heldMask)
                        val workingCards = state.cards.toMutableList()

                        // Turn replaced card positions face down
                        _uiState.update { s ->
                            val rMask = s.revealedMask.toMutableList()
                            for (idx in unheldIndices) {
                                rMask[idx] = false
                            }
                            s.copy(revealedMask = rMask)
                        }

                        // Reveal new cards one-by-one with speed delay
                        for (idx in unheldIndices) {
                            delay(delayMs)
                            workingCards[idx] = drawnCards[idx]
                            _uiState.update { s ->
                                val rMask = s.revealedMask.toMutableList()
                                rMask[idx] = true
                                s.copy(cards = workingCards.toList(), revealedMask = rMask)
                            }
                        }

                        val finalCardList = workingCards.toList()
                        val drawnList = unheldIndices.map { finalCardList[it] }
                        finalizeHandSettlement(
                            game = game,
                            bet = state.currentBet,
                            denom = settlementDenom,
                            dealtCards = state.cards,
                            heldMask = state.heldMask,
                            drawnCards = drawnList,
                            finalCardList = finalCardList,
                            mode = state.currentMode
                        )
                    }
                }
            }
        }
    }

    private fun handleMultiDealDraw(state: GameState, game: GameConfig) {
        val hands = state.handsPerDeal
        val dealMs = listOf(190L, 110L, 55L).getOrElse(state.speedLevel - 1) { 110L }
        val drawCardMs = listOf(160L, 95L, 45L).getOrElse(state.speedLevel - 1) { 95L }
        val handStepMs = listOf(240L, 130L, 58L).getOrElse(state.speedLevel - 1) { 130L }

        when (state.dealState) {
            DealState.READY_TO_DEAL, DealState.SHOWDOWN_WIN, DealState.SHOWDOWN_NO_WIN -> {
                val totalBet = state.currentBet * hands
                val cost = totalBet * state.currentDenom
                if (state.cashBalance < cost) {
                    openOverlay(ActiveOverlay.ADD_MONEY)
                    return
                }

                deck = Deck()
                val newDeal = deck.deal(5)
                val payKeys = game.payTable.rows.map { it.key }.toSet()
                val dealtRank = HandClassifier.classifyHand(newDeal, payKeys)
                val dealtWin = if (dealtRank != HandRank.NOTHING) {
                    game.payTable.getPayout(dealtRank.key, state.currentBet)
                } else {
                    0
                }

                if (state.isRecordingSession && activePlayedLog == null) {
                    val root = sessionsRoot
                    if (root != null) {
                        val startingCents = (state.cashBalance * 100.0).roundToLong()
                        sessionMoneyInCents = startingCents
                        activePlayedLog = PlayedGameLog(
                            game = game,
                            outputRoot = root,
                            startingCents = startingCents,
                            startedAt = LocalDateTime.now(),
                            startingDenomination = state.currentDenom,
                            startingCredits = state.currentBet,
                            handsPerDeal = hands
                        )
                    }
                }

                dealtDockJob?.cancel()
                dealtDockJob = null
                multiRevealJob?.cancel()
                pendingMultiSettlement = null
                pendingAllHoldChoices = StrategyEngine.evaluateHoldChoices(newDeal, game, maxChoices = Int.MAX_VALUE)

                viewModelScope.launch {
                    soundManager?.play(SoundEffect.DEAL)
                    _uiState.update {
                        it.copy(
                            isAnimating = true,
                            isRevealingMultiDraw = false,
                            cards = newDeal,
                            revealedMask = listOf(false, false, false, false, false),
                            heldMask = listOf(false, false, false, false, false),
                            holdChoices = emptyList(),
                            handRank = HandRank.NOTHING,
                            dealtPayingRank = HandRank.NOTHING,
                            dealtWinCredits = 0,
                            dealtBadgeDocked = false,
                            upperLines = List(hands - 1) { idx -> MultiHandLine(index = idx) },
                            mainLineWinRank = HandRank.NOTHING,
                            mainLineWinCredits = 0,
                            isMainLineSettled = false,
                            hideMultiWinBadges = false,
                            winAmount = 0,
                            cashBalance = it.cashBalance - cost,
                            dealtDenom = state.currentDenom,
                            dealState = DealState.DEALT_CHOOSING_HOLDS,
                            activeOverlay = if (it.activeOverlay == ActiveOverlay.DENOM) null else it.activeOverlay,
                            practiceFeedback = null,
                            practiceMadeTopChoice = null,
                            showPracticePlot = false
                        )
                    }

                    for (i in 0 until 5) {
                        delay(dealMs)
                        _uiState.update { s ->
                            val mask = s.revealedMask.toMutableList()
                            mask[i] = true
                            s.copy(revealedMask = mask)
                        }
                    }

                    _uiState.update { s ->
                        s.copy(
                            isAnimating = false,
                            revealedMask = listOf(true, true, true, true, true),
                            dealtPayingRank = if (dealtWin > 0) dealtRank else HandRank.NOTHING,
                            dealtWinCredits = dealtWin
                        )
                    }

                    if (dealtWin > 0) {
                        soundManager?.play(SoundEffect.MADE_HAND)
                        dealtDockJob = viewModelScope.launch {
                            delay(1100L)
                            _uiState.update { s ->
                                if (s.dealState == DealState.DEALT_CHOOSING_HOLDS && s.dealtWinCredits > 0) {
                                    s.copy(dealtBadgeDocked = true)
                                } else {
                                    s
                                }
                            }
                        }
                    }
                }
            }
            DealState.DEALT_CHOOSING_HOLDS -> {
                val settlementDenom = resolveSettlementDenom(state)
                val betPerHand = state.currentBet
                val dealtCards = state.cards
                val holdMask = state.heldMask
                val unheldIndices = (0 until 5).filter { !holdMask.getOrElse(it) { false } }
                val payKeys = game.payTable.rows.map { it.key }.toSet()

                dealtDockJob?.cancel()
                dealtDockJob = null

                val allLines = List(hands) { idx ->
                    val lineFinal = deck.draw(dealtCards, holdMask)
                    val lineRank = HandClassifier.classifyHand(lineFinal, payKeys)
                    val lineWin = if (lineRank != HandRank.NOTHING) {
                        game.payTable.getPayout(lineRank.key, betPerHand)
                    } else {
                        0
                    }
                    MultiHandLine(
                        index = idx,
                        cards = lineFinal,
                        rank = lineRank,
                        winCredits = lineWin,
                        isRevealed = false
                    )
                }
                val totalWinCredits = allLines.sumOf { it.winCredits }
                val totalWonCash = totalWinCredits * settlementDenom
                val bestIdx = allLines.indices.maxByOrNull { allLines[it].winCredits } ?: (hands - 1)
                val bestLine = allLines[bestIdx]
                val bestFinalCards = bestLine.cards.filterNotNull()
                val bestDrawnCards = unheldIndices.map { bestFinalCards[it] }

                val settlement = PendingMultiSettlement(
                    game = game,
                    betPerHand = betPerHand,
                    denom = settlementDenom,
                    dealtCards = dealtCards,
                    heldMask = holdMask,
                    bestDrawnCards = bestDrawnCards,
                    bestFinalCards = bestFinalCards,
                    bestRank = bestLine.rank,
                    allLines = allLines,
                    totalWinCredits = totalWinCredits,
                    totalWonCash = totalWonCash
                )
                pendingMultiSettlement = settlement

                multiRevealJob?.cancel()
                multiRevealJob = viewModelScope.launch {
                    soundManager?.play(SoundEffect.DRAW)
                    val mainLine = allLines.last()
                    val mainFinal = mainLine.cards.filterNotNull()
                    val workingMain = dealtCards.toMutableList()
                    val upperTemplate = allLines.dropLast(1).map { line ->
                        line.copy(
                            cards = List(5) { ci -> if (holdMask[ci]) dealtCards[ci] else null },
                            isRevealed = false
                        )
                    }

                    _uiState.update { s ->
                        s.copy(
                            isAnimating = true,
                            isRevealingMultiDraw = true,
                            dealtPayingRank = HandRank.NOTHING,
                            dealtWinCredits = 0,
                            dealtBadgeDocked = false,
                            hideMultiWinBadges = false,
                            winAmount = 0,
                            isMainLineSettled = false,
                            mainLineWinRank = HandRank.NOTHING,
                            mainLineWinCredits = 0,
                            upperLines = upperTemplate,
                            revealedMask = List(5) { idx -> holdMask.getOrElse(idx) { false } }
                        )
                    }

                    if (unheldIndices.isEmpty()) {
                        delay(80L)
                    } else {
                        for (idx in unheldIndices) {
                            delay(drawCardMs)
                            workingMain[idx] = mainFinal[idx]
                            _uiState.update { s ->
                                val rMask = s.revealedMask.toMutableList()
                                rMask[idx] = true
                                s.copy(cards = workingMain.toList(), revealedMask = rMask)
                            }
                        }
                        delay(maxOf(70L, (handStepMs * 0.7f).toLong()))
                    }

                    var runningWin = 0
                    val mainEffect = soundManager?.winEffectForLine(mainLine.rank, mainLine.winCredits, betPerHand)
                    if (mainEffect != null) {
                        runningWin += mainLine.winCredits
                    }
                    _uiState.update { s ->
                        s.copy(
                            cards = mainFinal,
                            revealedMask = listOf(true, true, true, true, true),
                            isMainLineSettled = true,
                            mainLineWinRank = mainLine.rank,
                            mainLineWinCredits = mainLine.winCredits,
                            winAmount = runningWin
                        )
                    }
                    if (mainEffect != null) {
                        val dur = soundManager?.playWithDuration(mainEffect) ?: 650L
                        delay(dur + 70L)
                    } else {
                        delay(handStepMs)
                    }

                    for (li in upperRevealOrder(hands)) {
                        val revealedLine = allLines[li].copy(isRevealed = true)
                        val lineEffect = soundManager?.winEffectForLine(
                            revealedLine.rank,
                            revealedLine.winCredits,
                            betPerHand
                        )
                        if (lineEffect != null) {
                            runningWin += revealedLine.winCredits
                        }
                        _uiState.update { s ->
                            val updatedUpper = s.upperLines.toMutableList()
                            if (li in updatedUpper.indices) {
                                updatedUpper[li] = revealedLine
                            }
                            s.copy(upperLines = updatedUpper, winAmount = runningWin)
                        }
                        if (lineEffect != null) {
                            val dur = soundManager?.playWithDuration(lineEffect) ?: 650L
                            delay(dur + 70L)
                        } else {
                            soundManager?.play(SoundEffect.DRAW)
                            delay(handStepMs)
                        }
                    }

                    if (totalWinCredits == 0) {
                        soundManager?.play(SoundEffect.NO_WIN)
                    }
                    commitPendingMultiSettlement(settlement)
                }
            }
        }
    }

    fun skipMultiReveal() {
        multiRevealJob?.cancel()
        multiRevealJob = null
        soundManager?.stopAll()
        val pending = pendingMultiSettlement ?: return
        viewModelScope.launch {
            commitPendingMultiSettlement(pending)
        }
    }

    private suspend fun commitPendingMultiSettlement(settlement: PendingMultiSettlement) {
        if (settlement.committed) return
        settlement.committed = true
        pendingMultiSettlement = null

        val mainLine = settlement.allLines.last()
        val mainFinal = mainLine.cards.filterNotNull()
        val isWin = settlement.totalWinCredits > 0
        val nextState = if (isWin) DealState.SHOWDOWN_WIN else DealState.SHOWDOWN_NO_WIN
        val currentSnapshot = _uiState.value
        val newCashBalance = currentSnapshot.cashBalance + settlement.totalWonCash
        val minDenom = settlement.game.denominations.minOrNull() ?: settlement.denom
        val minWager = settlement.allLines.size * minDenom
        val wentBroke = (newCashBalance * 100.0).roundToLong() < (minWager * 100.0).roundToLong()

        val topChoice = pendingAllHoldChoices.firstOrNull()
        val isSuboptimal = (topChoice != null && settlement.heldMask != topChoice.holdMask)

        if (currentSnapshot.isRecordingSession) {
            logSessionHand(
                game = settlement.game,
                denom = settlement.denom,
                bet = settlement.betPerHand * settlement.allLines.size,
                dealt = settlement.dealtCards,
                held = settlement.heldMask,
                drawn = settlement.bestDrawnCards,
                finalCards = settlement.bestFinalCards,
                rank = settlement.bestRank,
                payoutCredits = settlement.totalWinCredits,
                mode = GameMode.CASINO,
                wentBroke = wentBroke,
                lineClassifications = settlement.allLines.map { it.rank.key },
                lineCreditsWon = settlement.allLines.map { it.winCredits }
            )
        }

        _uiState.update { s ->
            val settledBalance = s.cashBalance + settlement.totalWonCash
            s.copy(
                isAnimating = false,
                isRevealingMultiDraw = false,
                dealtDenom = null,
                dealState = nextState,
                cards = mainFinal,
                revealedMask = listOf(true, true, true, true, true),
                upperLines = settlement.allLines.dropLast(1).map { it.copy(isRevealed = true) },
                isMainLineSettled = true,
                mainLineWinRank = mainLine.rank,
                mainLineWinCredits = mainLine.winCredits,
                handRank = mainLine.rank,
                winAmount = settlement.totalWinCredits,
                cashBalance = settledBalance,
                totalHandsPlayed = s.totalHandsPlayed + 1,
                handsAtCurrentDenom = s.handsAtCurrentDenom + 1,
                practiceMadeTopChoice = null,
                showPracticePlot = false,
                bankrollHistory = SessionBankrollTracker.onHandSettled(
                    history = s.bankrollHistory,
                    settledBalanceDollars = settledBalance,
                    isSuboptimalHold = isSuboptimal
                )
            )
        }
    }

    private fun applyHoldMaskUpdate(newMask: List<Boolean>) {
        dealtDockJob?.cancel()
        dealtDockJob = null
        _uiState.update { state ->
            val updatedUpper = if (state.isMultiHand && state.cards.size == 5) {
                state.upperLines.map { line ->
                    line.copy(
                        cards = List(5) { ci -> if (newMask[ci]) state.cards[ci] else null }
                    )
                }
            } else {
                state.upperLines
            }
            val shouldDock = state.isMultiHand && state.dealtWinCredits > 0
            state.copy(
                heldMask = newMask,
                upperLines = updatedUpper,
                dealtBadgeDocked = if (shouldDock) true else state.dealtBadgeDocked
            )
        }
    }

    fun onToggleHold(index: Int) {
        val snap = _uiState.value
        if (snap.isAnimating || snap.dealState != DealState.DEALT_CHOOSING_HOLDS) return
        if (index !in 0..4) return
        val currentHeld = snap.heldMask.getOrElse(index) { false }
        if (!currentHeld) {
            soundManager?.play(SoundEffect.HOLD)
        } else {
            soundManager?.play(SoundEffect.RELEASE)
        }
        val newMask = snap.heldMask.toMutableList()
        newMask[index] = !newMask[index]
        applyHoldMaskUpdate(newMask)
    }

    fun onSetHold(index: Int, hold: Boolean) {
        val snap = _uiState.value
        if (snap.isAnimating || snap.dealState != DealState.DEALT_CHOOSING_HOLDS) return
        if (index !in 0..4) return
        val currentHeld = snap.heldMask.getOrElse(index) { false }
        if (currentHeld == hold) return

        if (hold) {
            soundManager?.play(SoundEffect.HOLD)
        } else {
            soundManager?.play(SoundEffect.RELEASE)
        }
        val newMask = snap.heldMask.toMutableList()
        newMask[index] = hold
        applyHoldMaskUpdate(newMask)
    }

    fun onToggleRecordingSession() {
        val turningOn = !_uiState.value.isRecordingSession
        if (turningOn) {
            activePlayedLog = null
            sessionMoneyInCents = 0L
            _uiState.update {
                it.copy(
                    isRecordingSession = true,
                    recordedHandsCount = 0,
                    sessionExportStatus = null
                )
            }
        } else {
            val log = activePlayedLog
            activePlayedLog = null
            sessionMoneyInCents = 0L
            _uiState.update {
                it.copy(
                    isRecordingSession = false,
                    recordedHandsCount = 0
                )
            }
            if (log != null && log.created) {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        log.finish(PlayedGameLog.ENDED_BY_LEFT, LocalDateTime.now())
                    }
                    refreshRecordedSessions()
                }
            }
        }
    }

    fun onBetOne() {
        if (_uiState.value.isAnimating || _uiState.value.dealState == DealState.DEALT_CHOOSING_HOLDS) return
        soundManager?.play(SoundEffect.BET)
        _uiState.update { state ->
            val nextBet = if (state.currentBet >= 5) 1 else state.currentBet + 1
            state.copy(
                currentBet = nextBet,
                hideMultiWinBadges = true,
                winAmount = if (state.isMultiHand) 0 else state.winAmount
            )
        }
    }

    fun onBetMax() {
        if (_uiState.value.isAnimating || _uiState.value.dealState == DealState.DEALT_CHOOSING_HOLDS) return
        _uiState.update { it.copy(currentBet = 5) }
        soundManager?.play(SoundEffect.BET)
        onDealDraw()
    }

    fun onCashOut() {
        if (_uiState.value.isAnimating || _uiState.value.dealState == DealState.DEALT_CHOOSING_HOLDS) return
        soundManager?.play(SoundEffect.CASH_OUT)
        _uiState.update { it.copy(activeVoucherSerial = TitoVoucherFormatter.generateValidationNumber()) }
        openOverlay(ActiveOverlay.CASH_OUT)
    }

    fun onConfirmCashOut() {
        val cashOutCents = (_uiState.value.cashBalance * 100.0).roundToLong()
        val nextTicket = _uiState.value.nextVoucherTicketNumber + 1
        prefs?.edit()?.putInt("next_voucher_ticket", nextTicket)?.apply()
        val log = activePlayedLog
        val moneyIn = sessionMoneyInCents
        activePlayedLog = null
        sessionMoneyInCents = 0L

        if (log != null && log.created) {
            val voucher = SessionVoucher(
                moneyInCents = moneyIn,
                handsPlayed = log.handsLogged,
                cashOutCents = cashOutCents,
                resultCents = cashOutCents - moneyIn
            )
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    log.finish(PlayedGameLog.ENDED_BY_CASHED_OUT, LocalDateTime.now(), voucher)
                }
                refreshRecordedSessions()
            }
        }

        _uiState.update {
            it.copy(
                cashBalance = 0.0,
                cards = emptyList(),
                heldMask = listOf(false, false, false, false, false),
                revealedMask = listOf(false, false, false, false, false),
                handRank = HandRank.NOTHING,
                dealtPayingRank = HandRank.NOTHING,
                dealtWinCredits = 0,
                dealtBadgeDocked = false,
                upperLines = emptyList(),
                mainLineWinRank = HandRank.NOTHING,
                mainLineWinCredits = 0,
                isMainLineSettled = false,
                hideMultiWinBadges = false,
                isRevealingMultiDraw = false,
                winAmount = 0,
                holdChoices = emptyList(),
                dealtDenom = null,
                dealState = DealState.READY_TO_DEAL,
                practiceFeedback = null,
                practiceMadeTopChoice = null,
                totalHandsPlayed = 0,
                handsAtCurrentDenom = 0,
                recordedHandsCount = 0,
                nextVoucherTicketNumber = nextTicket,
                bankrollHistory = emptyList(),
                showPracticePlot = false,
                activeOverlay = null
            )
        }
    }

    fun onToggleMute() {
        val currentVol = _uiState.value.soundVolume
        val newVol = if (currentVol > 0.01f) 0f else 0.8f
        onVolumeChange(newVol)
    }

    fun onAddCash() {
        recordCashAddition(100.0)
    }

    fun onAddCashAmount(amount: Double) {
        recordCashAddition(amount)
    }

    private fun recordCashAddition(amount: Double) {
        if (amount <= 0.0 || !amount.isFinite()) return
        soundManager?.play(SoundEffect.COUNT_TICK)
        if (activePlayedLog != null) {
            sessionMoneyInCents += (amount * 100.0).roundToLong()
        }
        _uiState.update { state ->
            val updatedBalance = state.cashBalance + amount
            state.copy(
                cashBalance = updatedBalance,
                bankrollHistory = SessionBankrollTracker.onCashInserted(
                    history = state.bankrollHistory,
                    addedAmount = amount,
                    newBalanceDollars = updatedBalance
                )
            )
        }
    }

    fun onSetBankrollTargets(
        winGoalEnabled: Boolean = _uiState.value.bankrollTargets.winGoalEnabled,
        winGoalPercent: Int = _uiState.value.bankrollTargets.winGoalPercent,
        stopLossEnabled: Boolean = _uiState.value.bankrollTargets.stopLossEnabled,
        stopLossPercent: Int = _uiState.value.bankrollTargets.stopLossPercent
    ) {
        val clamped = BankrollTargets(
            winGoalEnabled = winGoalEnabled,
            winGoalPercent = winGoalPercent.coerceIn(
                BankrollTargets.MIN_WIN_GOAL_PERCENT,
                BankrollTargets.MAX_WIN_GOAL_PERCENT
            ),
            stopLossEnabled = stopLossEnabled,
            stopLossPercent = stopLossPercent.coerceIn(
                BankrollTargets.MIN_STOP_LOSS_PERCENT,
                BankrollTargets.MAX_STOP_LOSS_PERCENT
            )
        )
        prefs?.edit()
            ?.putBoolean("win_goal_enabled", clamped.winGoalEnabled)
            ?.putInt("win_goal_percent", clamped.winGoalPercent)
            ?.putBoolean("stop_loss_enabled", clamped.stopLossEnabled)
            ?.putInt("stop_loss_percent", clamped.stopLossPercent)
            ?.apply()
        _uiState.update { it.copy(bankrollTargets = clamped) }
    }

    fun onOpenAddMoney() {
        openOverlay(ActiveOverlay.ADD_MONEY)
    }

    fun onDenomSelect(denom: Double) {
        if (!canChangeDenom(_uiState.value)) return
        soundManager?.play(SoundEffect.COUNT_TICK)
        _uiState.update {
            it.copy(
                currentDenom = denom,
                handsAtCurrentDenom = 0,
                activeOverlay = null
            )
        }
    }

    fun onGameSelect(game: GameConfig, handsPerDeal: Int = _uiState.value.handsPerDeal) {
        if (!_uiState.value.canChangeGameOrMode) return
        val normalizedHands = if (handsPerDeal in GameState.SUPPORTED_HANDS_OPTIONS) handsPerDeal else 1
        val log = activePlayedLog
        if (log != null && (_uiState.value.currentGame?.variant != game.variant || _uiState.value.handsPerDeal != normalizedHands)) {
            activePlayedLog = null
            sessionMoneyInCents = 0L
            if (log.created) {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        log.finish(PlayedGameLog.ENDED_BY_LEFT, LocalDateTime.now())
                    }
                    refreshRecordedSessions()
                }
            }
        }
        _uiState.update { state ->
            applyGameSelection(
                state = state,
                game = game,
                keepRecordedCount = (activePlayedLog != null),
                handsPerDeal = normalizedHands
            )
        }
        gameSelectedBannerJob?.cancel()
        gameSelectedBannerJob = viewModelScope.launch {
            delay(GAME_SELECTED_BANNER_DURATION_MS)
            _uiState.update { it.copy(gameSelectedBanner = null) }
        }
    }

    private fun clearGameSelectedBanner() {
        gameSelectedBannerJob?.cancel()
        gameSelectedBannerJob = null
        if (_uiState.value.gameSelectedBanner != null) {
            _uiState.update { it.copy(gameSelectedBanner = null) }
        }
    }

    companion object {
        internal const val GAME_SELECTED_BANNER_DURATION_MS = 3500L

        fun upperRevealOrder(hands: Int): List<Int> = when (hands) {
            3 -> listOf(1, 0)
            5 -> listOf(2, 3, 0, 1)
            10 -> listOf(6, 7, 8, 3, 4, 5, 0, 1, 2)
            else -> (0 until (hands - 1).coerceAtLeast(0)).reversed().toList()
        }

        fun canChangeDenom(state: GameState): Boolean =
            !state.isAnimating && state.dealState != DealState.DEALT_CHOOSING_HOLDS

        fun resolveSettlementDenom(state: GameState): Double =
            state.dealtDenom ?: state.currentDenom

        fun formatGameSelectedBanner(displayName: String, handsPerDeal: Int = 1): String {
            val modeTag = if (handsPerDeal > 1) " ($handsPerDeal-Hand)" else ""
            return "GAME SELECTED: $displayName$modeTag — press DEAL"
        }

        fun applyGameSelection(
            state: GameState,
            game: GameConfig,
            keepRecordedCount: Boolean = false,
            handsPerDeal: Int = state.handsPerDeal
        ): GameState {
            val normalizedHands = if (handsPerDeal in GameState.SUPPORTED_HANDS_OPTIONS) handsPerDeal else 1
            val modeChanged = (normalizedHands != state.handsPerDeal)
            val gameChanged = (game.variant != state.currentGame?.variant)
            val resetBoard = modeChanged || (normalizedHands > 1 && gameChanged)
            val payKeys = game.payTable.rows.map { it.key }.toSet()
            val newRank = if (!resetBoard && state.cards.size == 5) {
                HandClassifier.classifyHand(state.cards, payKeys)
            } else {
                HandRank.NOTHING
            }
            val choices = if (!resetBoard && normalizedHands == 1 && state.cards.size == 5) {
                StrategyEngine.evaluateHoldChoices(state.cards, game)
            } else {
                emptyList()
            }
            return state.copy(
                currentGame = game,
                handsPerDeal = normalizedHands,
                currentBet = if (modeChanged) 1 else state.currentBet,
                cards = if (resetBoard) emptyList() else state.cards,
                heldMask = if (resetBoard) listOf(false, false, false, false, false) else state.heldMask,
                revealedMask = if (resetBoard) listOf(false, false, false, false, false) else state.revealedMask,
                upperLines = if (resetBoard) emptyList() else state.upperLines,
                dealtPayingRank = if (resetBoard) HandRank.NOTHING else state.dealtPayingRank,
                dealtWinCredits = if (resetBoard) 0 else state.dealtWinCredits,
                dealtBadgeDocked = if (resetBoard) false else state.dealtBadgeDocked,
                mainLineWinRank = if (resetBoard) HandRank.NOTHING else state.mainLineWinRank,
                mainLineWinCredits = if (resetBoard) 0 else state.mainLineWinCredits,
                isMainLineSettled = if (resetBoard) false else state.isMainLineSettled,
                hideMultiWinBadges = false,
                winAmount = if (resetBoard) 0 else state.winAmount,
                dealState = if (resetBoard) DealState.READY_TO_DEAL else state.dealState,
                handRank = newRank,
                holdChoices = choices,
                recordedHandsCount = if (keepRecordedCount) state.recordedHandsCount else 0,
                gameSelectedBanner = formatGameSelectedBanner(game.displayName, normalizedHands),
                activeOverlay = null
            )
        }
    }

    fun onThemeSelect(theme: AppTheme) {
        _uiState.update { it.copy(currentTheme = theme) }
    }

    fun onCardDeckSelect(cardDeck: CardDeck) {
        prefs?.edit()?.putString("card_deck", cardDeck.id)?.apply()
        _uiState.update { it.copy(currentCardDeck = cardDeck) }
    }

    fun onModeToggle(mode: GameMode) {
        if (_uiState.value.isMultiHand && mode != GameMode.CASINO) return
        _uiState.update { state ->
            if (state.isMultiHand) return@update state
            val autoMask = if (mode == GameMode.LEARN && state.holdChoices.isNotEmpty() && state.dealState == DealState.DEALT_CHOOSING_HOLDS) {
                state.holdChoices.first().holdMask
            } else {
                state.heldMask
            }
            state.copy(currentMode = mode, heldMask = autoMask, showPracticePlot = false)
        }
    }

    fun onTogglePracticePlot() {
        _uiState.update { it.copy(showPracticePlot = !it.showPracticePlot) }
    }

    fun onCycleSpeed() {
        _uiState.update {
            val nextSpeed = if (it.speedLevel >= 3) 1 else it.speedLevel + 1
            it.copy(speedLevel = nextSpeed)
        }
    }

    fun onToggleCurrencyDisplay() {
        _uiState.update { it.copy(displayCurrency = !it.displayCurrency) }
    }

    fun onToggleWinCurrency() {
        _uiState.update { it.copy(winAsCash = !it.winAsCash) }
    }

    fun onToggleBetCurrency() {
        _uiState.update { it.copy(betAsCash = !it.betAsCash) }
    }

    fun onToggleCreditCurrency() {
        _uiState.update { it.copy(creditAsCash = !it.creditAsCash) }
    }

    fun onToggleVolumePopup() {
        _uiState.update { it.copy(isVolumePopupOpen = !it.isVolumePopupOpen) }
    }

    fun onVolumeChange(vol: Float) {
        soundManager?.setVolume(vol)
        _uiState.update { it.copy(soundVolume = vol) }
    }

    fun openOverlay(overlay: ActiveOverlay) {
        if (overlay == ActiveOverlay.DENOM && !canChangeDenom(_uiState.value)) return
        if (overlay == ActiveOverlay.MORE_GAMES && !_uiState.value.canChangeGameOrMode) return
        clearGameSelectedBanner()
        if (overlay == ActiveOverlay.OPTIONS) {
            refreshRecordedSessions()
        }
        _uiState.update { it.copy(activeOverlay = overlay, isVolumePopupOpen = false, sessionExportStatus = null) }
    }

    fun closeOverlay() {
        _uiState.update { it.copy(activeOverlay = null) }
    }

    fun onSaveSessionToDownloads(context: Context, folderPath: String) {
        val dir = File(folderPath)
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                SessionExporter.saveZipToDownloads(context.applicationContext, dir)
            }
            val msg = res.fold(
                onSuccess = { it },
                onFailure = { "Export failed: ${it.message ?: "unknown error"}" }
            )
            _uiState.update { it.copy(sessionExportStatus = msg) }
        }
    }

    fun onShareSession(context: Context, folderPath: String) {
        val dir = File(folderPath)
        val res = SessionExporter.shareSessionZip(context, dir)
        res.onFailure { err ->
            _uiState.update { it.copy(sessionExportStatus = "Share failed: ${err.message ?: "unknown error"}") }
        }
    }

    private fun refreshRecordedSessions() {
        val root = sessionsRoot ?: return
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                PlayedGameLog.listRecordedSessions(root)
            }
            _uiState.update { it.copy(recordedSessions = saved) }
        }
    }

    private suspend fun finalizeHandSettlement(
        game: GameConfig,
        bet: Int,
        denom: Double,
        dealtCards: List<Card>,
        heldMask: List<Boolean>,
        drawnCards: List<Card>,
        finalCardList: List<Card>,
        mode: GameMode
    ) {
        val payKeys = game.payTable.rows.map { it.key }.toSet()
        val finalRank = HandClassifier.classifyHand(finalCardList, payKeys)
        val payoutCredits = game.payTable.getPayout(finalRank.key, bet)
        val wonCash = payoutCredits * denom
        val isWin = payoutCredits > 0
        val nextState = if (isWin) DealState.SHOWDOWN_WIN else DealState.SHOWDOWN_NO_WIN

        if (isWin) {
            soundManager?.playWinByPayout(payoutCredits)
        } else {
            soundManager?.play(SoundEffect.NO_WIN)
        }

        val currentSnapshot = _uiState.value
        val topChoice = currentSnapshot.holdChoices.firstOrNull()
        val didMakeTopChoice = (topChoice != null && heldMask == topChoice.holdMask)
        val isSuboptimal = (topChoice != null && !didMakeTopChoice)

        val newCashBalance = currentSnapshot.cashBalance + wonCash
        val minDenom = game.denominations.minOrNull() ?: denom
        val wentBroke = (newCashBalance * 100.0).roundToLong() < (minDenom * 100.0).roundToLong()

        if (currentSnapshot.isRecordingSession) {
            logSessionHand(
                game = game,
                denom = denom,
                bet = bet,
                dealt = dealtCards,
                held = heldMask,
                drawn = drawnCards,
                finalCards = finalCardList,
                rank = finalRank,
                payoutCredits = payoutCredits,
                mode = mode,
                wentBroke = wentBroke
            )
        }

        _uiState.update { state ->
            val settledBalance = state.cashBalance + wonCash
            state.copy(
                isAnimating = false,
                dealtDenom = null,
                dealState = nextState,
                cards = finalCardList,
                revealedMask = listOf(true, true, true, true, true),
                handRank = finalRank,
                winAmount = payoutCredits,
                cashBalance = settledBalance,
                totalHandsPlayed = state.totalHandsPlayed + 1,
                handsAtCurrentDenom = state.handsAtCurrentDenom + 1,
                practiceMadeTopChoice = didMakeTopChoice,
                showPracticePlot = false,
                bankrollHistory = SessionBankrollTracker.onHandSettled(
                    history = state.bankrollHistory,
                    settledBalanceDollars = settledBalance,
                    isSuboptimalHold = isSuboptimal
                )
            )
        }
    }

    private suspend fun logSessionHand(
        game: GameConfig,
        denom: Double,
        bet: Int,
        dealt: List<Card>,
        held: List<Boolean>,
        drawn: List<Card>,
        finalCards: List<Card>,
        rank: HandRank,
        payoutCredits: Int,
        mode: GameMode,
        wentBroke: Boolean,
        lineClassifications: List<String>? = null,
        lineCreditsWon: List<Int>? = null
    ) {
        val dealtStr = dealt.joinToString(" ")
        val heldStr = held.mapIndexedNotNull { i, h -> if (h) dealt.getOrNull(i)?.toString() else null }.joinToString(" ")
        val finalStr = finalCards.joinToString(" ")
        val wonCash = payoutCredits * denom
        android.util.Log.i(
            "VP_SESSION",
            "HAND #${_uiState.value.totalHandsPlayed + 1} | Game: ${game.variant} | Denom: $denom | Bet: $bet | Dealt: [$dealtStr] | Held: [$heldStr] | Final: [$finalStr] | Rank: ${rank.displayName} | Won: $payoutCredits cr ($$wonCash)"
        )

        val log = activePlayedLog ?: return
        val choicesSnapshot = pendingAllHoldChoices
        val now = LocalDateTime.now()
        withContext(Dispatchers.IO) {
            log.recordHand(
                dealt = dealt,
                heldMask = held,
                drawn = drawn,
                finalCards = finalCards,
                classification = rank.key,
                creditsPlayed = bet,
                denominationCents = (denom * 100.0).roundToLong(),
                creditsWon = payoutCredits,
                trainingLevel = mode.displayName.lowercase(),
                allHoldChoices = choicesSnapshot,
                finishedAt = now,
                wentBroke = wentBroke,
                lineClassifications = lineClassifications,
                lineCreditsWon = lineCreditsWon
            )
            if (wentBroke) {
                log.finish(PlayedGameLog.ENDED_BY_WENT_BROKE, now)
            }
        }
        val loggedCount = log.handsLogged
        val root = sessionsRoot
        val updatedList = if (root != null) {
            withContext(Dispatchers.IO) { PlayedGameLog.listRecordedSessions(root) }
        } else {
            _uiState.value.recordedSessions
        }
        if (wentBroke) {
            activePlayedLog = null
            sessionMoneyInCents = 0L
        }
        _uiState.update {
            it.copy(
                recordedHandsCount = if (wentBroke) 0 else loggedCount,
                recordedSessions = updatedList
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        multiRevealJob?.cancel()
        dealtDockJob?.cancel()
        gameSelectedBannerJob?.cancel()
        soundManager?.release()
    }
}
