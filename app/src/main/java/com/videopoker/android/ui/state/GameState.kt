package com.videopoker.android.ui.state

import com.videopoker.android.data.export.RecordedSessionInfo
import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.GameConfig
import com.videopoker.android.data.models.HandRank
import com.videopoker.android.data.models.HoldChoice
import com.videopoker.android.ui.theme.AppTheme
import com.videopoker.android.ui.theme.CardDeck

enum class GameMode(val displayName: String) {
    LEARN("Learn"),
    PRACTICE("Practice"),
    CASINO("Casino")
}

enum class DealState {
    READY_TO_DEAL,
    DEALT_CHOOSING_HOLDS,
    SHOWDOWN_WIN,
    SHOWDOWN_NO_WIN
}

data class GameState(
    val currentGame: GameConfig? = null,
    val currentMode: GameMode = GameMode.LEARN,
    val currentBet: Int = 5,
    val currentDenom: Double = 0.25,
    val cashBalance: Double = 0.0,
    val dealState: DealState = DealState.READY_TO_DEAL,
    val cards: List<Card> = emptyList(),
    val heldMask: List<Boolean> = listOf(false, false, false, false, false),
    val holdChoices: List<HoldChoice> = emptyList(),
    val handRank: HandRank = HandRank.NOTHING,
    val winAmount: Int = 0,
    val winAsCash: Boolean = false, // false = credits, true = dollars
    val betAsCash: Boolean = false,
    val creditAsCash: Boolean = false,
    val displayCurrency: Boolean = false, // legacy fallback
    val currentTheme: AppTheme = AppTheme.BRASS,
    val currentCardDeck: CardDeck = CardDeck.MODERN,
    val soundVolume: Float = 0.8f,
    val isVolumePopupOpen: Boolean = false,
    val speedLevel: Int = 2, // 1..3, default 2 (matching desktop SPEED_MS = [250, 170, 100])
    val revealedMask: List<Boolean> = listOf(true, true, true, true, true),
    val isAnimating: Boolean = false,
    val totalHandsPlayed: Int = 0,
    val handsAtCurrentDenom: Int = 0,
    val allGames: List<GameConfig> = emptyList(),
    val isRecordingSession: Boolean = false,
    val recordedHandsCount: Int = 0,
    val recordedSessions: List<RecordedSessionInfo> = emptyList(),
    val sessionExportStatus: String? = null,
    val activeOverlay: ActiveOverlay? = null,
    val practiceFeedback: String? = null,
    val practiceMadeTopChoice: Boolean? = null,
    val showSplash: Boolean = true
) {
    companion object {
        const val APP_VERSION = "1.0.18"
    }
}

enum class ActiveOverlay {
    HELP,
    MORE_GAMES,
    OPTIONS,
    DENOM,
    SEE_PAYS,
    ADD_MONEY,
    CASH_OUT,
    COMPLETE_HOLD_LIST
}
