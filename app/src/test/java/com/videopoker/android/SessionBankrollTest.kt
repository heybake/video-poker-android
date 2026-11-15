package com.videopoker.android

import com.videopoker.android.ui.state.BankrollTargets
import com.videopoker.android.ui.state.SessionBankrollTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionBankrollTest {

    @Test
    fun `initial cash inserts accumulate cleanly at Hand 0`() {
        var history = SessionBankrollTracker.onCashInserted(emptyList(), 20.0, 20.0)
        history = SessionBankrollTracker.onCashInserted(history, 5.0, 25.0)

        assertEquals(1, history.size)
        val pt0 = history.single()
        assertEquals(0, pt0.handNumber)
        assertEquals(25.0, pt0.balanceDollars, 1e-6)
        assertEquals(25.0, pt0.totalBuyInDollars, 1e-6)
        assertNull(pt0.balanceBeforeInsert)
        assertNull(pt0.buyInBeforeInsert)
    }

    @Test
    fun `mid-session cash insert creates vertical step at active hand index`() {
        var history = SessionBankrollTracker.onCashInserted(emptyList(), 20.0, 20.0)
        history = SessionBankrollTracker.onHandSettled(history, 18.75, isSuboptimalHold = false)
        history = SessionBankrollTracker.onHandSettled(history, 17.50, isSuboptimalHold = true)
        // Re-buy $20 after Hand 2
        history = SessionBankrollTracker.onCashInserted(history, 20.0, 37.50)

        assertEquals(3, history.size)
        val hand2 = history[2]
        assertEquals(2, hand2.handNumber)
        assertEquals(17.50, hand2.balanceBeforeInsert ?: -1.0, 1e-6)
        assertEquals(37.50, hand2.balanceDollars, 1e-6)
        assertEquals(20.0, hand2.buyInBeforeInsert ?: -1.0, 1e-6)
        assertEquals(40.0, hand2.totalBuyInDollars, 1e-6)
        assertTrue(hand2.isSuboptimalHold)

        // Hand 3 continues from $40 buy-in
        history = SessionBankrollTracker.onHandSettled(history, 42.50, isSuboptimalHold = false)
        assertEquals(4, history.size)
        assertEquals(3, history[3].handNumber)
        assertEquals(40.0, history[3].totalBuyInDollars, 1e-6)
        assertFalse(history[3].isSuboptimalHold)
    }

    @Test
    fun `win goal and stop loss step up with total buy in`() {
        val targets = BankrollTargets(
            winGoalEnabled = true,
            winGoalPercent = 50,
            stopLossEnabled = true,
            stopLossPercent = 25
        )
        assertEquals(30.0, targets.winGoalDollars(20.0), 1e-6)
        assertEquals(15.0, targets.stopLossDollars(20.0), 1e-6)
        assertEquals(60.0, targets.winGoalDollars(40.0), 1e-6)
        assertEquals(30.0, targets.stopLossDollars(40.0), 1e-6)
    }

    @Test
    fun `viewport auto-shifts right edge after window fills and supports panning`() {
        var history = SessionBankrollTracker.onCashInserted(emptyList(), 50.0, 50.0)
        repeat(25) { idx ->
            history = SessionBankrollTracker.onHandSettled(
                history = history,
                settledBalanceDollars = 50.0 + (idx % 5),
                isSuboptimalHold = (idx % 7 == 0)
            )
        }
        val targets = BankrollTargets()
        val live = SessionBankrollTracker.calculateViewport(history, targets, requestedEndHand = null)
        assertEquals(5f, live.startHand, 1e-4f)
        assertEquals(25f, live.endHand, 1e-4f)
        assertTrue(live.canPan)
        assertTrue(live.isLiveEdge)

        val panned = SessionBankrollTracker.calculateViewport(history, targets, requestedEndHand = 20f)
        assertEquals(0f, panned.startHand, 1e-4f)
        assertEquals(20f, panned.endHand, 1e-4f)
        assertFalse(panned.isLiveEdge)
    }

    @Test
    fun `mistake stats track count percentage and session reset`() {
        // Empty history and Hand 0 cash-in both start at 0 (0.0%)
        assertEquals("Mistakes: 0 (0.0%)", SessionBankrollTracker.formatMistakesLabel(emptyList()))

        var history = SessionBankrollTracker.onCashInserted(emptyList(), 20.0, 20.0)
        assertEquals("Mistakes: 0 (0.0%)", SessionBankrollTracker.formatMistakesLabel(history))

        // 2 optimal + 1 suboptimal => 1 of 3 = 33.3%
        history = SessionBankrollTracker.onHandSettled(history, 18.75, isSuboptimalHold = false)
        history = SessionBankrollTracker.onHandSettled(history, 17.50, isSuboptimalHold = true)
        history = SessionBankrollTracker.onHandSettled(history, 22.50, isSuboptimalHold = false)

        val stats = SessionBankrollTracker.calculateMistakeStats(history)
        assertEquals(1, stats.mistakeCount)
        assertEquals(3, stats.completedHands)
        assertEquals(33.333333, stats.mistakePercent, 1e-3)
        assertEquals("Mistakes: 1 (33.3%)", SessionBankrollTracker.formatMistakesLabel(history))

        // Cash out clears history -> back to 0 (0.0%)
        assertEquals("Mistakes: 0 (0.0%)", SessionBankrollTracker.formatMistakesLabel(emptyList()))
    }

    @Test
    fun `mistake stats show zero percent when all completed hands are optimal`() {
        var history = SessionBankrollTracker.onCashInserted(emptyList(), 20.0, 20.0)
        history = SessionBankrollTracker.onHandSettled(history, 18.75, isSuboptimalHold = false)
        history = SessionBankrollTracker.onHandSettled(history, 17.50, isSuboptimalHold = false)

        val stats = SessionBankrollTracker.calculateMistakeStats(history)
        assertEquals(0, stats.mistakeCount)
        assertEquals(2, stats.completedHands)
        assertEquals(0.0, stats.mistakePercent, 1e-6)
        assertEquals("Mistakes: 0 (0.0%)", SessionBankrollTracker.formatMistakesLabel(history))
    }
}
