package com.videopoker.android.ui.state

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * A settled point on the Session Bankroll timeline at [handNumber].
 *
 * When cash is added mid-session at [handNumber] (>= 1), [balanceBeforeInsert] and
 * [buyInBeforeInsert] preserve the pre-insert values so the chart renders a clean
 * vertical step at that exact hand index before continuing to the next hand.
 */
data class BankrollPoint(
    val handNumber: Int,
    val balanceDollars: Double,
    val totalBuyInDollars: Double,
    val balanceBeforeInsert: Double? = null,
    val buyInBeforeInsert: Double? = null,
    val isSuboptimalHold: Boolean = false
)

/**
 * Configurable session Win Goal and Stop-Loss percentages relative to Total Buy-In.
 */
data class BankrollTargets(
    val winGoalEnabled: Boolean = true,
    val winGoalPercent: Int = DEFAULT_WIN_GOAL_PERCENT,
    val stopLossEnabled: Boolean = true,
    val stopLossPercent: Int = DEFAULT_STOP_LOSS_PERCENT
) {
    fun winGoalDollars(totalBuyIn: Double): Double =
        (totalBuyIn * (1.0 + winGoalPercent / 100.0)).coerceAtLeast(0.0)

    fun stopLossDollars(totalBuyIn: Double): Double =
        (totalBuyIn * (1.0 - stopLossPercent / 100.0)).coerceAtLeast(0.0)

    companion object {
        const val DEFAULT_WIN_GOAL_PERCENT = 50
        const val DEFAULT_STOP_LOSS_PERCENT = 50
        const val MIN_WIN_GOAL_PERCENT = 1
        const val MAX_WIN_GOAL_PERCENT = 500
        const val MIN_STOP_LOSS_PERCENT = 1
        const val MAX_STOP_LOSS_PERCENT = 100
    }
}

data class BankrollViewport(
    val startHand: Float,
    val endHand: Float,
    val maxHand: Int,
    val windowHands: Int,
    val canPan: Boolean,
    val isLiveEdge: Boolean,
    val minY: Double,
    val maxY: Double
)

object SessionBankrollTracker {

    const val DEFAULT_WINDOW_HANDS = 20

    fun onCashInserted(
        history: List<BankrollPoint>,
        addedAmount: Double,
        newBalanceDollars: Double
    ): List<BankrollPoint> {
        if (addedAmount <= 0.0 || !addedAmount.isFinite()) return history
        if (history.isEmpty()) {
            return listOf(
                BankrollPoint(
                    handNumber = 0,
                    balanceDollars = newBalanceDollars.coerceAtLeast(0.0),
                    totalBuyInDollars = addedAmount
                )
            )
        }
        val last = history.last()
        val updatedLast = if (last.handNumber == 0) {
            last.copy(
                balanceDollars = (last.balanceDollars + addedAmount).coerceAtLeast(0.0),
                totalBuyInDollars = last.totalBuyInDollars + addedAmount
            )
        } else {
            last.copy(
                balanceBeforeInsert = last.balanceBeforeInsert ?: last.balanceDollars,
                buyInBeforeInsert = last.buyInBeforeInsert ?: last.totalBuyInDollars,
                balanceDollars = (last.balanceDollars + addedAmount).coerceAtLeast(0.0),
                totalBuyInDollars = last.totalBuyInDollars + addedAmount
            )
        }
        return history.dropLast(1) + updatedLast
    }

    fun onHandSettled(
        history: List<BankrollPoint>,
        settledBalanceDollars: Double,
        isSuboptimalHold: Boolean
    ): List<BankrollPoint> {
        val clampedBalance = settledBalanceDollars.coerceAtLeast(0.0)
        if (history.isEmpty()) {
            return listOf(
                BankrollPoint(
                    handNumber = 1,
                    balanceDollars = clampedBalance,
                    totalBuyInDollars = clampedBalance,
                    isSuboptimalHold = isSuboptimalHold
                )
            )
        }
        val last = history.last()
        return history + BankrollPoint(
            handNumber = last.handNumber + 1,
            balanceDollars = clampedBalance,
            totalBuyInDollars = last.totalBuyInDollars,
            isSuboptimalHold = isSuboptimalHold
        )
    }

    fun calculateViewport(
        points: List<BankrollPoint>,
        targets: BankrollTargets,
        requestedEndHand: Float?,
        windowHands: Int = DEFAULT_WINDOW_HANDS
    ): BankrollViewport {
        val safeWindow = windowHands.coerceAtLeast(5)
        val latestHand = points.lastOrNull()?.handNumber ?: 0
        val maxEnd = max(safeWindow.toFloat(), latestHand.toFloat())
        val canPan = latestHand > safeWindow

        val endHand = if (!canPan) {
            safeWindow.toFloat()
        } else {
            (requestedEndHand ?: maxEnd).coerceIn(safeWindow.toFloat(), maxEnd)
        }
        val startHand = (endHand - safeWindow.toFloat()).coerceAtLeast(0f)
        val isLiveEdge = !canPan || (maxEnd - endHand) <= 0.15f

        val minVisibleIdx = max(0, floor(startHand.toDouble()).toInt() - 1)
        val maxVisibleIdx = ceil(endHand.toDouble()).toInt() + 1
        val visiblePoints = points.filter { it.handNumber in minVisibleIdx..maxVisibleIdx }
            .ifEmpty { points }

        var rawMin = Double.POSITIVE_INFINITY
        var rawMax = Double.NEGATIVE_INFINITY

        for (pt in visiblePoints) {
            val vals = buildList {
                add(pt.balanceDollars)
                pt.balanceBeforeInsert?.let { add(it) }
                add(pt.totalBuyInDollars)
                pt.buyInBeforeInsert?.let { add(it) }
                if (targets.winGoalEnabled) {
                    add(targets.winGoalDollars(pt.totalBuyInDollars))
                    pt.buyInBeforeInsert?.let { add(targets.winGoalDollars(it)) }
                }
                if (targets.stopLossEnabled) {
                    add(targets.stopLossDollars(pt.totalBuyInDollars))
                    pt.buyInBeforeInsert?.let { add(targets.stopLossDollars(it)) }
                }
            }
            for (v in vals) {
                if (v < rawMin) rawMin = v
                if (v > rawMax) rawMax = v
            }
        }

        if (!rawMin.isFinite() || !rawMax.isFinite()) {
            rawMin = 0.0
            rawMax = 100.0
        }

        val span = (rawMax - rawMin).let { if (it < 4.0) max(4.0, rawMax * 0.25) else it }
        val pad = span * 0.14
        val minY = max(0.0, rawMin - pad)
        val maxY = max(minY + 1.0, rawMax + pad)

        return BankrollViewport(
            startHand = startHand,
            endHand = endHand,
            maxHand = latestHand,
            windowHands = safeWindow,
            canPan = canPan,
            isLiveEdge = isLiveEdge,
            minY = minY,
            maxY = maxY
        )
    }

    fun formatDollarAxis(value: Double): String {
        val safe = value.coerceAtLeast(0.0)
        return when {
            safe >= 1000.0 -> String.format(Locale.US, "$%.0f", safe)
            kotlin.math.abs(safe - kotlin.math.round(safe)) < 0.05 -> String.format(Locale.US, "$%.0f", kotlin.math.round(safe))
            else -> String.format(Locale.US, "$%.2f", safe)
        }
    }
}
