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
import com.videopoker.android.ui.theme.AppTheme
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

    fun loadGames(context: Context) {
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

                val defaultGame = loadedList.firstOrNull { it.variant.contains("double_bonus_10_7") }
                    ?: loadedList.firstOrNull()

                // Initial approved mock hand: Two Pair [Ks, Kh, 5d, 5c, 2h]
                val initialDeal = listOf(
                    Card.fromString("Ks"),
                    Card.fromString("Kh"),
                    Card.fromString("5d"),
                    Card.fromString("5c"),
                    Card.fromString("2h")
                )

                val payKeys = defaultGame?.payTable?.rows?.map { it.key }?.toSet()
                val initialRank = HandClassifier.classifyHand(initialDeal, payKeys)
                val initialChoices = defaultGame?.let {
                    StrategyEngine.evaluateHoldChoices(initialDeal, it)
                } ?: emptyList()

                val initialMask = listOf(true, true, true, true, false)

                _uiState.update { current ->
                    current.copy(
                        allGames = loadedList,
                        currentGame = defaultGame,
                        cards = initialDeal,
                        heldMask = initialMask,
                        holdChoices = initialChoices,
                        handRank = initialRank,
                        winAmount = 10,
                        dealState = DealState.DEALT_CHOOSING_HOLDS
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onDealDraw() {
        val state = _uiState.value
        val game = state.currentGame ?: return

        when (state.dealState) {
            DealState.READY_TO_DEAL, DealState.SHOWDOWN_WIN, DealState.SHOWDOWN_NO_WIN -> {
                // DEAL NEW HAND
                val cost = state.currentBet * state.currentDenom
                if (state.cashBalance < cost) return

                deck = Deck()
                val newDeal = deck.deal(5)
                val payKeys = game.payTable.rows.map { it.key }.toSet()
                val currentRank = HandClassifier.classifyHand(newDeal, payKeys)
                val choices = StrategyEngine.evaluateHoldChoices(newDeal, game)

                // In Learn mode, auto-hold top choice
                val autoMask = if (state.currentMode == GameMode.LEARN && choices.isNotEmpty()) {
                    choices.first().holdMask
                } else {
                    listOf(false, false, false, false, false)
                }

                _uiState.update {
                    it.copy(
                        dealState = DealState.DEALT_CHOOSING_HOLDS,
                        cards = newDeal,
                        heldMask = autoMask,
                        holdChoices = choices,
                        handRank = currentRank,
                        winAmount = 0,
                        cashBalance = it.cashBalance - cost
                    )
                }
            }
            DealState.DEALT_CHOOSING_HOLDS -> {
                // DRAW REPLACEMENTS
                val drawnCards = deck.draw(state.cards, state.heldMask)
                val payKeys = game.payTable.rows.map { it.key }.toSet()
                val finalRank = HandClassifier.classifyHand(drawnCards, payKeys)
                val payoutCredits = game.payTable.getPayout(finalRank.key, state.currentBet)
                val wonCash = payoutCredits * state.currentDenom

                val isWin = payoutCredits > 0
                val nextState = if (isWin) DealState.SHOWDOWN_WIN else DealState.SHOWDOWN_NO_WIN

                _uiState.update {
                    it.copy(
                        dealState = nextState,
                        cards = drawnCards,
                        handRank = finalRank,
                        winAmount = payoutCredits,
                        cashBalance = it.cashBalance + wonCash,
                        totalHandsPlayed = it.totalHandsPlayed + 1,
                        handsAtCurrentDenom = it.handsAtCurrentDenom + 1
                    )
                }
            }
        }
    }

    fun onToggleHold(index: Int) {
        if (_uiState.value.dealState != DealState.DEALT_CHOOSING_HOLDS) return
        _uiState.update { state ->
            val newMask = state.heldMask.toMutableList()
            newMask[index] = !newMask[index]
            state.copy(heldMask = newMask)
        }
    }

    fun onBetOne() {
        if (_uiState.value.dealState == DealState.DEALT_CHOOSING_HOLDS) return
        _uiState.update { state ->
            val nextBet = if (state.currentBet >= 5) 1 else state.currentBet + 1
            state.copy(currentBet = nextBet)
        }
    }

    fun onBetMax() {
        if (_uiState.value.dealState == DealState.DEALT_CHOOSING_HOLDS) return
        _uiState.update { it.copy(currentBet = 5) }
    }

    fun onDenomSelect(denom: Double) {
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
            val newMask = if (mode == GameMode.LEARN && state.holdChoices.isNotEmpty() && state.dealState == DealState.DEALT_CHOOSING_HOLDS) {
                state.holdChoices.first().holdMask
            } else {
                state.heldMask
            }
            state.copy(currentMode = mode, heldMask = newMask)
        }
    }

    fun onToggleCurrencyDisplay() {
        _uiState.update { it.copy(displayCurrency = !it.displayCurrency) }
    }

    fun onCycleSpeed() {
        _uiState.update {
            val nextSpeed = if (it.speedLevel >= 3) 1 else it.speedLevel + 1
            it.copy(speedLevel = nextSpeed)
        }
    }

    fun onToggleVolumePopup() {
        _uiState.update { it.copy(isVolumePopupOpen = !it.isVolumePopupOpen) }
    }

    fun onVolumeChange(vol: Float) {
        _uiState.update { it.copy(soundVolume = vol) }
    }

    fun openOverlay(overlay: ActiveOverlay) {
        _uiState.update { it.copy(activeOverlay = overlay, isVolumePopupOpen = false) }
    }

    fun closeOverlay() {
        _uiState.update { it.copy(activeOverlay = null) }
    }
}
