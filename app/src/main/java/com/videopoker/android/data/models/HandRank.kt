package com.videopoker.android.data.models

/**
 * Standard video poker hand classifications with user-facing display plaques.
 */
enum class HandRank(val key: String, val displayName: String, val rankOrder: Int) {
    ROYAL_FLUSH("royal_flush", "ROYAL FLUSH", 1),
    STRAIGHT_FLUSH("straight_flush", "STRAIGHT FLUSH", 2),
    FOUR_ACES_WITH_2_3_4("four_aces_with_2_3_4", "4 ACES W/ 2,3,4", 3),
    FOUR_2S_3S_4S_WITH_A_THRU_4("four_2s_3s_4s_with_a_thru_4", "4 2s,3s,4s W/ A-4", 4),
    FOUR_ACES("four_aces", "FOUR ACES", 5),
    FOUR_2S_3S_4S("four_2s_3s_4s", "FOUR 2s, 3s, 4s", 6),
    FOUR_5S_THRU_K("four_5s_thru_k", "FOUR 5s THRU KINGS", 7),
    FOUR_OF_A_KIND("four_of_a_kind", "FOUR OF A KIND", 8),
    FULL_HOUSE("full_house", "FULL HOUSE", 9),
    FLUSH("flush", "FLUSH", 10),
    STRAIGHT("straight", "STRAIGHT", 11),
    THREE_OF_A_KIND("three_of_a_kind", "THREE OF A KIND", 12),
    TWO_PAIR("two_pair", "TWO PAIR", 13),
    JACKS_OR_BETTER("jacks_or_better", "JACKS OR BETTER", 14),
    NOTHING("nothing", "", 15);

    companion object {
        fun fromKey(key: String): HandRank {
            return entries.find { it.key == key } ?: NOTHING
        }
    }
}
