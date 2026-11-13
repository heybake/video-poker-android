package com.videopoker.android.data.engine

import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.HandRank

/**
 * 5-card showdown evaluator and classification engine matching the desktop player.
 */
object HandClassifier {

    private val HIGH_PAIR_RANKS = setOf('J', 'Q', 'K', 'A')

    /**
     * Classify a 5-card hand against standard video poker rules and paytable bonus tiers.
     *
     * @param hand Exactly 5 distinct cards.
     * @param activePayTableKeys Optional set of hand keys supported in the active pay table.
     * @return Evaluated [HandRank]
     */
    fun classifyHand(hand: List<Card>, activePayTableKeys: Set<String>? = null): HandRank {
        require(hand.size == 5) { "Hand must contain exactly 5 cards, got ${hand.size}" }
        require(hand.distinct().size == 5) { "Hand contains duplicate cards" }

        val ranks = hand.map { it.rank }
        val suits = hand.map { it.suit }
        val isFlush = suits.distinct().size == 1

        val rankCounts = ranks.groupingBy { it }.eachCount()
        val sortedCounts = rankCounts.values.sortedDescending()

        // Sequence / Straight detection
        var isStraight = false
        if (rankCounts.size == 5) {
            val highValues = hand.map { it.rankValue }.sorted()
            val isHighStraight = (highValues.last() - highValues.first() == 4)

            val lowValues = hand.map { it.lowRankValue }.sorted()
            val isWheelStraight = (lowValues == listOf(1, 2, 3, 4, 5))

            isStraight = isHighStraight || isWheelStraight
        }

        // 1. Royal Flush
        if (isFlush && ranks.toSet() == setOf('T', 'J', 'Q', 'K', 'A')) {
            return HandRank.ROYAL_FLUSH
        }

        // 2. Straight Flush (including Wheel Straight Flush A-2-3-4-5)
        if (isFlush && isStraight) {
            return HandRank.STRAIGHT_FLUSH
        }

        // 3. Four of a Kind (with bonus quad / kicker resolution)
        if (sortedCounts == listOf(4, 1)) {
            val quadRank = rankCounts.entries.first { it.value == 4 }.key
            val kickerRank = rankCounts.entries.first { it.value == 1 }.key

            val candidates = mutableListOf<HandRank>()
            if (quadRank == 'A') {
                if (kickerRank in setOf('2', '3', '4')) {
                    candidates.add(HandRank.FOUR_ACES_WITH_2_3_4)
                }
                candidates.add(HandRank.FOUR_ACES)
            } else if (quadRank in setOf('2', '3', '4')) {
                if (kickerRank in setOf('A', '2', '3', '4')) {
                    candidates.add(HandRank.FOUR_2S_3S_4S_WITH_A_THRU_4)
                }
                candidates.add(HandRank.FOUR_2S_3S_4S)
            } else {
                candidates.add(HandRank.FOUR_5S_THRU_K)
            }
            candidates.add(HandRank.FOUR_OF_A_KIND)

            if (activePayTableKeys != null) {
                for (cand in candidates) {
                    if (cand.key in activePayTableKeys) {
                        return cand
                    }
                }
            }
            return candidates.first()
        }

        // 4. Full House
        if (sortedCounts == listOf(3, 2)) {
            return HandRank.FULL_HOUSE
        }

        // 5. Flush
        if (isFlush) {
            return HandRank.FLUSH
        }

        // 6. Straight
        if (isStraight) {
            return HandRank.STRAIGHT
        }

        // 7. Three of a Kind
        if (sortedCounts == listOf(3, 1, 1)) {
            return HandRank.THREE_OF_A_KIND
        }

        // 8. Two Pair
        if (sortedCounts == listOf(2, 2, 1)) {
            return HandRank.TWO_PAIR
        }

        // 9. Jacks or Better (Pair of J, Q, K, A)
        if (sortedCounts == listOf(2, 1, 1, 1)) {
            val pairRank = rankCounts.entries.first { it.value == 2 }.key
            if (pairRank in HIGH_PAIR_RANKS) {
                return HandRank.JACKS_OR_BETTER
            }
            return HandRank.NOTHING
        }

        // 10. Non-Paying Showdown
        return HandRank.NOTHING
    }
}
