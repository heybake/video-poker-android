package com.videopoker.android.ui.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import com.videopoker.android.data.engine.Deck
import com.videopoker.android.data.engine.HandClassifier
import com.videopoker.android.data.engine.StrategyEngine
import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.GameConfig
import com.videopoker.android.data.models.HandRank
import com.videopoker.android.sound.SoundEffect
import com.videopoker.android.sound.SoundManager
import com.videopoker.android.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStreamReader

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private var deck: Deck = Deck()
    private var soundManager: SoundManager? = null

    fun loadGames(context: Context) {
        if (soundManager == null) {
            soundManager = SoundManager(context.applicationContext)
            soundManager?.setVolume(_uiState.value.soundVolume)
        }

        viewModelScope.launch {
            try {
                val assetManager = context.assets
                val files = assetManager.list("games") ?: emptyArray()
                val loadedList = mutableListOf<GameConfig>()

                for (file in files) {
                    if (file.endsWith(".json")) {
                        assetManager.open("games/$file").use { stream ->
                            val reader = InputStreamReader(stream)
                            val json = JsonParser.parseReader(reader).asJsonObject
                            val config = StrategyEngine.parseGameConfig(json)
                            loadedList.add(config)
                        }
                    }
                }

                // Sort games: Double Bonus 10/7 first, then alphabetical
                loadedList.sortBy { if (it.variant.contains("double_bonus_10_7")) "000" else it.displayName }

                _uiState.update { current ->
                    current.copy(
                        allGames = loadedList,
                        currentGame = null,
                        cards = emptyList(),
                        heldMask = listOf(false, false, false, false, false),
                        revealedMask = listOf(false, false, false, false, false),
                        holdChoices = emptyList(),
                        handRank = HandRank.NOTHING,
                        winAmount = 0,
                        dealState = DealState.READY_TO_DEAL
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onDealDraw() {
        val state = _uiState.value
        if (state.isAnimating) return
        if (state.currentGame == null) {
            openOverlay(ActiveOverlay.MORE_GAMES)
            return
        }
        val game = state.currentGame

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
                val choices = StrategyEngine.evaluateHoldChoices(newDeal, game)

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
                            dealState = DealState.DEALT_CHOOSING_HOLDS,
                            practiceFeedback = null,
                            practiceMadeTopChoice = null
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
                val unheldIndices = (0 until 5).filter { !state.heldMask.getOrElse(it) { false } }

                viewModelScope.launch {
                    soundManager?.play(SoundEffect.DRAW)

                    if (unheldIndices.isEmpty()) {
                        // All 5 cards held (pat hand)
                        val payKeys = game.payTable.rows.map { it.key }.toSet()
                        val finalRank = HandClassifier.classifyHand(state.cards, payKeys)
                        val payoutCredits = game.payTable.getPayout(finalRank.key, state.currentBet)
                        val wonCash = payoutCredits * state.currentDenom
                        val isWin = payoutCredits > 0
                        val nextState = if (isWin) DealState.SHOWDOWN_WIN else DealState.SHOWDOWN_NO_WIN

                        if (isWin) {
                            soundManager?.playWinByPayout(payoutCredits)
                        } else {
                            soundManager?.play(SoundEffect.NO_WIN)
                        }

                        val topChoice = state.holdChoices.firstOrNull()
                        val didMakeTopChoice = (topChoice != null && state.heldMask == topChoice.holdMask)

                        if (_uiState.value.isRecordingSession) {
                            logSessionHand(game, state.currentDenom, state.currentBet, state.cards, state.heldMask, state.cards, finalRank, payoutCredits)
                        }

                        _uiState.update {
                            it.copy(
                                dealState = nextState,
                                handRank = finalRank,
                                winAmount = payoutCredits,
                                cashBalance = it.cashBalance + wonCash,
                                totalHandsPlayed = it.totalHandsPlayed + 1,
                                handsAtCurrentDenom = it.handsAtCurrentDenom + 1,
                                practiceMadeTopChoice = didMakeTopChoice
                            )
                        }
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

                        val payKeys = game.payTable.rows.map { it.key }.toSet()
                        val finalRank = HandClassifier.classifyHand(workingCards, payKeys)
                        val payoutCredits = game.payTable.getPayout(finalRank.key, state.currentBet)
                        val wonCash = payoutCredits * state.currentDenom
                        val isWin = payoutCredits > 0
                        val nextState = if (isWin) DealState.SHOWDOWN_WIN else DealState.SHOWDOWN_NO_WIN

                        if (isWin) {
                            soundManager?.playWinByPayout(payoutCredits)
                        } else {
                            soundManager?.play(SoundEffect.NO_WIN)
                        }

                        val topChoice = state.holdChoices.firstOrNull()
                        val didMakeTopChoice = (topChoice != null && state.heldMask == topChoice.holdMask)

                        if (_uiState.value.isRecordingSession) {
                            logSessionHand(game, state.currentDenom, state.currentBet, state.cards, state.heldMask, state.cards, finalRank, payoutCredits)
                        }

                        _uiState.update {
                            it.copy(
                                isAnimating = false,
                                dealState = nextState,
                                cards = workingCards,
                                revealedMask = listOf(true, true, true, true, true),
                                handRank = finalRank,
                                winAmount = payoutCredits,
                                cashBalance = it.cashBalance + wonCash,
                                totalHandsPlayed = it.totalHandsPlayed + 1,
                                handsAtCurrentDenom = it.handsAtCurrentDenom + 1,
                                practiceMadeTopChoice = didMakeTopChoice
                            )
                        }
                    }
                }
            }
        }
    }

    fun onToggleHold(index: Int) {
        if (_uiState.value.isAnimating || _uiState.value.dealState != DealState.DEALT_CHOOSING_HOLDS) return
        val currentHeld = _uiState.value.heldMask.getOrElse(index) { false }
        if (!currentHeld) {
            soundManager?.play(SoundEffect.HOLD)
        } else {
            soundManager?.play(SoundEffect.RELEASE)
        }

        _uiState.update { state ->
            val newMask = state.heldMask.toMutableList()
            newMask[index] = !newMask[index]
            state.copy(heldMask = newMask)
        }
    }

    fun onSetHold(index: Int, hold: Boolean) {
        if (_uiState.value.isAnimating || _uiState.value.dealState != DealState.DEALT_CHOOSING_HOLDS) return
        if (index !in 0..4) return
        val currentHeld = _uiState.value.heldMask.getOrElse(index) { false }
        if (currentHeld == hold) return

        if (hold) {
            soundManager?.play(SoundEffect.HOLD)
        } else {
            soundManager?.play(SoundEffect.RELEASE)
        }

        _uiState.update { state ->
            val newMask = state.heldMask.toMutableList()
            newMask[index] = hold
            state.copy(heldMask = newMask)
        }
    }

    fun onToggleRecordingSession() {
        _uiState.update { it.copy(isRecordingSession = !it.isRecordingSession) }
    }

    fun onBetOne() {
        if (_uiState.value.isAnimating || _uiState.value.dealState == DealState.DEALT_CHOOSING_HOLDS) return
        soundManager?.play(SoundEffect.BET)
        _uiState.update { state ->
            val nextBet = if (state.currentBet >= 5) 1 else state.currentBet + 1
            state.copy(currentBet = nextBet)
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
        openOverlay(ActiveOverlay.CASH_OUT)
    }

    fun onConfirmCashOut() {
        _uiState.update {
            it.copy(
                cashBalance = 0.0,
                cards = emptyList(),
                heldMask = listOf(false, false, false, false, false),
                revealedMask = listOf(false, false, false, false, false),
                handRank = HandRank.NOTHING,
                winAmount = 0,
                holdChoices = emptyList(),
                dealState = DealState.READY_TO_DEAL,
                practiceFeedback = null,
                practiceMadeTopChoice = null,
                totalHandsPlayed = 0,
                handsAtCurrentDenom = 0,
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
        soundManager?.play(SoundEffect.COUNT_TICK)
        _uiState.update { it.copy(cashBalance = it.cashBalance + 100.0) }
    }

    fun onAddCashAmount(amount: Double) {
        soundManager?.play(SoundEffect.COUNT_TICK)
        _uiState.update { it.copy(cashBalance = it.cashBalance + amount, activeOverlay = null) }
    }

    fun onOpenAddMoney() {
        openOverlay(ActiveOverlay.ADD_MONEY)
    }

    fun onDenomSelect(denom: Double) {
        soundManager?.play(SoundEffect.COUNT_TICK)
        _uiState.update {
            it.copy(
                currentDenom = denom,
                handsAtCurrentDenom = 0,
                activeOverlay = null
            )
        }
    }

    fun onGameSelect(game: GameConfig) {
        _uiState.update { state ->
            val payKeys = game.payTable.rows.map { it.key }.toSet()
            val newRank = if (state.cards.size == 5) HandClassifier.classifyHand(state.cards, payKeys) else HandRank.NOTHING
            val choices = if (state.cards.size == 5) StrategyEngine.evaluateHoldChoices(state.cards, game) else emptyList()

            state.copy(
                currentGame = game,
                handRank = newRank,
                holdChoices = choices,
                activeOverlay = null
            )
        }
    }

    fun onThemeSelect(theme: AppTheme) {
        _uiState.update { it.copy(currentTheme = theme) }
    }

    fun onModeToggle(mode: GameMode) {
        _uiState.update { state ->
            val autoMask = if (mode == GameMode.LEARN && state.holdChoices.isNotEmpty() && state.dealState == DealState.DEALT_CHOOSING_HOLDS) {
                state.holdChoices.first().holdMask
            } else {
                state.heldMask
            }
            state.copy(currentMode = mode, heldMask = autoMask)
        }
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
        _uiState.update { it.copy(activeOverlay = overlay, isVolumePopupOpen = false) }
    }

    fun closeOverlay() {
        _uiState.update { it.copy(activeOverlay = null) }
    }

    private fun logSessionHand(
        game: GameConfig,
        denom: Double,
        bet: Int,
        dealt: List<Card>,
        held: List<Boolean>,
        finalCards: List<Card>,
        rank: HandRank,
        payoutCredits: Int
    ) {
        val dealtStr = dealt.joinToString(" ")
        val heldStr = held.mapIndexedNotNull { i, h -> if (h) dealt.getOrNull(i)?.toString() else null }.joinToString(" ")
        val finalStr = finalCards.joinToString(" ")
        val wonCash = payoutCredits * denom
        android.util.Log.i(
            "VP_SESSION",
            "HAND #${_uiState.value.totalHandsPlayed + 1} | Game: ${game.variant} | Denom: $denom | Bet: $bet | Dealt: [$dealtStr] | Held: [$heldStr] | Final: [$finalStr] | Rank: ${rank.displayName} | Won: $payoutCredits cr ($$wonCash)"
        )
    }

    override fun onCleared() {
        super.onCleared()
        soundManager?.release()
    }
}
