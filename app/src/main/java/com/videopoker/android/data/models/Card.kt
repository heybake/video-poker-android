package com.videopoker.android.data.models

/**
 * Immutable representation of a standard playing card.
 *
 * @property rank Single character rank: '2'..'9', 'T', 'J', 'Q', 'K', 'A'
 * @property suit Single character suit: 's' (spades), 'h' (hearts), 'd' (diamonds), 'c' (clubs)
 */
data class Card(
    val rank: Char,
    val suit: Char
) : Comparable<Card> {

    init {
        require(rank in VALID_RANKS) { "Invalid card rank: '$rank'" }
        require(suit in VALID_SUITS) { "Invalid card suit: '$suit'" }
    }

    /**
     * Standard rank value (2..14, where Ace is 14).
     */
    val rankValue: Int
        get() = RANK_VALUES[rank] ?: 0

    /**
     * Low rank value where Ace is 1 for wheel straights (A-2-3-4-5).
     */
    val lowRankValue: Int
        get() = if (rank == 'A') 1 else rankValue

    /**
     * True if card is an honor rank (J, Q, K, A).
     */
    val isHigh: Boolean
        get() = rank in HIGH_RANKS

    /**
     * Asset filename in assets/cards/ (e.g. "Ah.png", "Ts.png").
     */
    val assetFileName: String
        get() = "${rank.lowercaseChar()}${suit.lowercaseChar()}.png"

    override fun toString(): String = "$rank$suit"

    override fun compareTo(other: Card): Int {
        val rankComp = this.rankValue.compareTo(other.rankValue)
        return if (rankComp != 0) rankComp else this.suit.compareTo(other.suit)
    }

    companion object {
        val VALID_RANKS = setOf('2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A')
        val VALID_SUITS = setOf('s', 'h', 'd', 'c')
        val HIGH_RANKS = setOf('J', 'Q', 'K', 'A')

        val RANK_VALUES = mapOf(
            '2' to 2, '3' to 3, '4' to 4, '5' to 5,
            '6' to 6, '7' to 7, '8' to 8, '9' to 9,
            'T' to 10, 'J' to 11, 'Q' to 12, 'K' to 13, 'A' to 14
        )

        /**
         * Parse a two-character card string (e.g. "Ah", "Ts", "2d").
         */
        fun fromString(token: String): Card {
            require(token.length == 2) { "Invalid card token length: '$token'" }
            val rank = token[0].uppercaseChar()
            val suit = token[1].lowercaseChar()
            return Card(rank, suit)
        }
    }
}
