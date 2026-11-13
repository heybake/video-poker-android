package com.videopoker.android.ui.state

import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.GameConfig
import com.videopoker.android.data.models.HandRank
import com.videopoker.android.data.models.HoldChoice
import com.videopoker.android.ui.theme.AppTheme

enum class GameMode(val displayName: String) {
    LEARN("LEARN"),
    PRACTICE("PRACTICE"),
    CASINO("CASINO")
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
    val cashBalance: Double = 40.0,
    val dealState: DealState = DealState.DEALT_CHOOSING_HOLDS,
    val cards: List<Card> = emptyList(),
    val heldMask: List<Boolean> = listOf(false, false, false, false, false),
    val holdChoices: List<HoldChoice> = emptyList(),
    val handRank: HandRank = HandRank.NOTHING,
    val winAmount: Int = 0,
    val displayCurrency: Boolean = false, // false = credits, true = dollars
    val currentTheme: AppTheme = AppTheme.BRASS,
    val soundVolume: Float = 0.8f,
    val isVolumePopupOpen: Boolean = false,
    val speedLevel: Int = 3, // 1..3
    val totalHandsPlayed: Int = 12,
    val handsAtCurrentDenom: Int = 12,
    val allGames: List<GameConfig> = emptyList(),
    val activeOverlay: ActiveOverlay? = null
)

enum class ActiveOverlay {
    HELP,
    MORE_GAMES,
    OPTIONS,
    DENOM,
    SEE_PAYS
}
