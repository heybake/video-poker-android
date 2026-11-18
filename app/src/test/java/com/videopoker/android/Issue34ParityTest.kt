package com.videopoker.android

import com.videopoker.android.data.models.GameConfig
import com.videopoker.android.data.models.PayTable
import com.videopoker.android.ui.decks.shouldShowBankrollMistakes
import com.videopoker.android.ui.state.ActiveOverlay
import com.videopoker.android.ui.state.GameMode
import com.videopoker.android.ui.state.GameState
import com.videopoker.android.ui.state.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies Issue #34 parity items:
 * - Item 1: GAME SELECTED banner appears on game selection and closes More Games
 * - Item 2: Mistakes label is hidden in Casino mode and shown in Learn & Practice modes
 */
class Issue34ParityTest {

    private fun sampleGame(displayName: String = "Double Bonus 10/7"): GameConfig = GameConfig(
        configName = "double_bonus_10_7_built.json",
        variant = "double_bonus_10_7",
        familyName = "Double Bonus",
        payTableLabel = "10/7",
        displayName = displayName,
        returnPercentage = 100.17,
        denominations = listOf(0.25),
        defaultDenomination = 0.25,
        payTable = PayTable(emptyList()),
        strategyRules = emptyList()
    )

    @Test
    fun `mistakes label is hidden in Casino mode and shown in Learn and Practice`() {
        assertTrue(shouldShowBankrollMistakes(GameMode.LEARN))
        assertTrue(shouldShowBankrollMistakes(GameMode.PRACTICE))
        assertFalse(shouldShowBankrollMistakes(GameMode.CASINO))
    }

    @Test
    fun `selecting a game sets non-blocking GAME SELECTED banner and closes overlay`() {
        val initial = GameState(activeOverlay = ActiveOverlay.MORE_GAMES)
        assertNull(initial.gameSelectedBanner)

        val updated = GameViewModel.applyGameSelection(initial, sampleGame("Jacks or Better 9/6"))
        assertEquals(
            "GAME SELECTED: Jacks or Better 9/6 — press DEAL",
            updated.gameSelectedBanner
        )
        assertNull(updated.activeOverlay)
        assertEquals("Jacks or Better 9/6", updated.currentGame?.displayName)

        // Tapping Deal directly via ViewModel clears the banner cleanly and prompts Add Money when cash is 0
        val vm = GameViewModel()
        vm.onDealDraw()
        assertNull(vm.uiState.value.gameSelectedBanner)
        assertEquals(ActiveOverlay.MORE_GAMES, vm.uiState.value.activeOverlay)
    }
}
