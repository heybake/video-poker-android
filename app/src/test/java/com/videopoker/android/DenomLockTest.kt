package com.videopoker.android

import com.videopoker.android.ui.state.ActiveOverlay
import com.videopoker.android.ui.state.DealState
import com.videopoker.android.ui.state.GameState
import com.videopoker.android.ui.state.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies Issue #36: coin denomination cannot be changed mid-hand and settlement uses the dealt coin.
 */
class DenomLockTest {

    @Test
    fun `canChangeDenom only permits changes when idle and not animating`() {
        assertTrue(GameViewModel.canChangeDenom(GameState(dealState = DealState.READY_TO_DEAL)))
        assertTrue(GameViewModel.canChangeDenom(GameState(dealState = DealState.SHOWDOWN_WIN)))
        assertTrue(GameViewModel.canChangeDenom(GameState(dealState = DealState.SHOWDOWN_NO_WIN)))

        assertFalse(GameViewModel.canChangeDenom(GameState(dealState = DealState.DEALT_CHOOSING_HOLDS)))
        assertFalse(GameViewModel.canChangeDenom(GameState(dealState = DealState.READY_TO_DEAL, isAnimating = true)))
        assertFalse(GameViewModel.canChangeDenom(GameState(dealState = DealState.DEALT_CHOOSING_HOLDS, isAnimating = true)))
    }

    @Test
    fun `resolveSettlementDenom prefers dealtDenom over currentDenom`() {
        val drifted = GameState(
            currentDenom = 100.0,
            dealtDenom = 0.25,
            dealState = DealState.DEALT_CHOOSING_HOLDS
        )
        assertEquals(0.25, GameViewModel.resolveSettlementDenom(drifted), 0.0)

        val fallback = GameState(
            currentDenom = 1.0,
            dealtDenom = null,
            dealState = DealState.READY_TO_DEAL
        )
        assertEquals(1.0, GameViewModel.resolveSettlementDenom(fallback), 0.0)
    }

    @Test
    fun `GameViewModel allows denom change when ready and blocks nothing outside a hand`() {
        val vm = GameViewModel()
        assertEquals(0.25, vm.uiState.value.currentDenom, 0.0)

        vm.openOverlay(ActiveOverlay.DENOM)
        assertEquals(ActiveOverlay.DENOM, vm.uiState.value.activeOverlay)

        vm.onDenomSelect(100.0)
        assertEquals(100.0, vm.uiState.value.currentDenom, 0.0)
        assertNull(vm.uiState.value.activeOverlay)
    }

    @Test
    fun `GameViewModel rejects denom overlay and denom changes during a dealt hand`() {
        val vm = GameViewModel(
            GameState(
                currentDenom = 0.25,
                dealtDenom = 0.25,
                dealState = DealState.DEALT_CHOOSING_HOLDS
            )
        )

        vm.openOverlay(ActiveOverlay.DENOM)
        assertNull(vm.uiState.value.activeOverlay)

        vm.onDenomSelect(100.0)
        assertEquals(0.25, vm.uiState.value.currentDenom, 0.0)
        assertEquals(0.25, GameViewModel.resolveSettlementDenom(vm.uiState.value), 0.0)
    }
}
