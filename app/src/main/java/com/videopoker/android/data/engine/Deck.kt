package com.videopoker.android.data.engine

import com.videopoker.android.data.models.Card
import java.security.SecureRandom
import java.util.Random

/**
 * 52-card deck with non-replacement PRNG sampling matching the workbench engine.
 */
class Deck(seed: Long? = null) {

    private val rng: Random = if (seed != null) Random(seed) else SecureRandom()

    val fullDeck: List<Card> = FULL_DECK

    /**
     * Deal [count] cards without replacement from the full deck.
     */
    fun deal(count: Int = 5): List<Card> {
        require(count in 1..52) { "Deal count must be between 1 and 52, got $count" }
        val deckCopy = fullDeck.toMutableList()
        // Fisher-Yates shuffle
        for (i in deckCopy.indices.reversed()) {
            val j = rng.nextInt(i + 1)
            val tmp = deckCopy[i]
            deckCopy[i] = deckCopy[j]
            deckCopy[j] = tmp
        }
        return deckCopy.take(count)
    }

    /**
     * Draw replacement cards for the unheld positions from the unseen pool.
     */
    fun draw(currentHand: List<Card>, holdMask: List<Boolean>): List<Card> {
        require(currentHand.size == 5) { "Current hand must have 5 cards" }
        require(holdMask.size == 5) { "Hold mask must have 5 booleans" }

        val unseenPool = fullDeck.filter { it !in currentHand }.toMutableList()

        // Shuffle unseen pool
        for (i in unseenPool.indices.reversed()) {
            val j = rng.nextInt(i + 1)
            val tmp = unseenPool[i]
            unseenPool[i] = unseenPool[j]
            unseenPool[j] = tmp
        }

        var drawIdx = 0
        return currentHand.mapIndexed { idx, card ->
            if (holdMask[idx]) {
                card
            } else {
                unseenPool[drawIdx++]
            }
        }
    }

    companion object {
        val FULL_DECK: List<Card> = buildList {
            for (rank in Card.VALID_RANKS) {
                for (suit in Card.VALID_SUITS) {
                    add(Card(rank, suit))
                }
            }
        }
    }
}
