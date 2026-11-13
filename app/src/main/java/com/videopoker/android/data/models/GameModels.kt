package com.videopoker.android.data.models

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Parsed game specification matching built JSON files from the workbench.
 */
data class GameConfig(
    val configName: String,
    val variant: String,
    val familyName: String,
    val displayName: String,
    val returnPercentage: Double,
    val denominations: List<Double>,
    val defaultDenomination: Double,
    val payTable: PayTable,
    val strategyRules: List<StrategyRule>
)

/**
 * Pay schedule for 1..5 coins.
 */
data class PayTable(
    val rows: List<PayTableRow>
) {
    fun getPayout(handKey: String, coins: Int): Int {
        val row = rows.find { it.key == handKey } ?: return 0
        val idx = (coins - 1).coerceIn(0, 4)
        return row.values.getOrElse(idx) { 0 }
    }
}

data class PayTableRow(
    val key: String,
    val label: String,
    val values: List<Int>
)

/**
 * Single strategy rule in the priority list.
 */
data class StrategyRule(
    val id: String,
    val hold: String,
    val query: JsonObject
)

/**
 * Evaluated hold choice for display in the hold panel.
 */
data class HoldChoice(
    val row: Int,
    val ruleId: String,
    val holdMask: List<Boolean>,
    val keptIndexes: List<Int>,
    val keptCards: List<Card>,
    val description: String,
    val expectedValue: Double,
    val isTopChoice: Boolean
)
