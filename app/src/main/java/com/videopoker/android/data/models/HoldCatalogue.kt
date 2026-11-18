package com.videopoker.android.data.models

data class CatalogueRuleInfo(
    val label: String,
    val shortLabel: String,
    val keeps: String
)

object HoldCatalogue {
    private val rules = mapOf<String, CatalogueRuleInfo>(
        "royal_flush" to CatalogueRuleInfo("Royal flush", "Royal flush", "Keep all 5"),
        "straight_flush" to CatalogueRuleInfo("Straight flush", "Straight flush", "Keep all 5"),
        "four_of_a_kind" to CatalogueRuleInfo("Four of a kind (any)", "Four of a kind", "Keep the four"),
        "four_aces" to CatalogueRuleInfo("Four aces", "Four aces", "Keep the four"),
        "four_aces_with_kicker" to CatalogueRuleInfo("Four aces with a 2, 3 or 4 kicker", "Four aces + 2-4", "Keep all 5"),
        "four_2s_3s_4s" to CatalogueRuleInfo("Four 2s, 3s or 4s", "Four 2s-4s", "Keep the four"),
        "four_2s_3s_4s_with_kicker" to CatalogueRuleInfo("Four 2s, 3s or 4s with an ace, 2, 3 or 4 kicker", "Four 2s-4s + A-4", "Keep all 5"),
        "four_5s_through_kings" to CatalogueRuleInfo("Four 5s through Kings", "Four 5s-Kings", "Keep the four"),
        "full_house" to CatalogueRuleInfo("Full house", "Full house", "Keep all 5"),
        "flush" to CatalogueRuleInfo("Flush", "Flush", "Keep all 5"),
        "straight" to CatalogueRuleInfo("Straight", "Straight", "Keep all 5"),
        "three_aces_with_kicker" to CatalogueRuleInfo("Three aces with a 2, 3 or 4 kicker", "Three aces + 2-4", "Keep the three aces and the kicker"),
        "three_aces" to CatalogueRuleInfo("Three aces", "Three aces", "Keep the three"),
        "three_2s_3s_4s_with_kicker" to CatalogueRuleInfo("Three 2s, 3s or 4s with an ace, 2, 3 or 4 kicker", "Three 2s-4s + A-4", "Keep the three and the kicker"),
        "three_of_a_kind" to CatalogueRuleInfo("Three of a kind", "Three of a kind", "Keep the three"),
        "two_pair" to CatalogueRuleInfo("Two pair", "Two pair", "Keep both pairs"),
        "pair_of_aces" to CatalogueRuleInfo("Pair of aces", "Pair of aces", "Keep the pair"),
        "high_pair" to CatalogueRuleInfo("Pair of Jacks, Queens, Kings or Aces", "High pair", "Keep the pair"),
        "low_pair" to CatalogueRuleInfo("Pair of 2s through 10s", "Low pair", "Keep the pair"),
        "4_to_royal_flush" to CatalogueRuleInfo("4 to a royal flush", "4 to a royal", "Keep the 4"),
        "4_to_straight_flush_open" to CatalogueRuleInfo("4 to an open-ended straight flush", "4 to a straight flush (open-ended)", "Keep the 4"),
        "4_to_straight_flush_inside" to CatalogueRuleInfo("4 to an inside straight flush", "4 to a straight flush (inside)", "Keep the 4"),
        "3_to_royal_flush" to CatalogueRuleInfo("3 to a royal flush", "3 to a royal", "Keep the 3"),
        "4_to_flush" to CatalogueRuleInfo("4 to a flush", "4 to a flush", "Keep the 4"),
        "4_to_outside_straight" to CatalogueRuleInfo("4 to an open-ended straight", "4 to an open-ended straight", "Keep the 4"),
        "3_to_straight_flush_type_1" to CatalogueRuleInfo("3 to a straight flush, no gaps", "3 to a straight flush (connected)", "Keep the 3"),
        "3_to_straight_flush_type_1_gap" to CatalogueRuleInfo("3 to a straight flush, one gap, 1+ high cards", "3 to a straight flush (1 gap, high card)", "Keep the 3"),
        "3_to_straight_flush_type_2_low" to CatalogueRuleInfo("3 to a straight flush, one gap, no high cards", "3 to a straight flush (1 gap, no high card)", "Keep the 3"),
        "3_to_straight_flush_type_2_high" to CatalogueRuleInfo("3 to a straight flush, two gaps, 2+ high cards", "3 to a straight flush (2 gaps, 2 high cards)", "Keep the 3"),
        "3_to_straight_flush_type_3_low" to CatalogueRuleInfo("3 to a straight flush, two gaps, no high cards", "3 to a straight flush (2 gaps, no high card)", "Keep the 3"),
        "3_to_straight_flush_type_3_ace" to CatalogueRuleInfo("3 to a straight flush, Ace-low (Ace with 2 to 5)", "3 to a straight flush (Ace-low)", "Keep the 3"),
        "4_to_inside_straight_3_high" to CatalogueRuleInfo("4 to an inside straight, 3 high cards", "4 to an inside straight (3 high)", "Keep the 4"),
        "4_to_inside_straight_2_high" to CatalogueRuleInfo("4 to an inside straight, 2 high cards", "4 to an inside straight (2 high)", "Keep the 4"),
        "4_to_inside_straight_1_high" to CatalogueRuleInfo("4 to an inside straight, 1 high card", "4 to an inside straight (1 high)", "Keep the 4"),
        "4_to_inside_straight_0_high" to CatalogueRuleInfo("4 to an inside straight, no high cards", "4 to an inside straight (no high)", "Keep the 4"),
        "suited_qj" to CatalogueRuleInfo("Suited Queen-Jack", "Suited Q-J", "Keep the 2"),
        "suited_kq_kj" to CatalogueRuleInfo("Suited King-Queen or King-Jack", "Suited K-Q or K-J", "Keep the 2"),
        "suited_jt" to CatalogueRuleInfo("Suited Jack-Ten", "Suited J-10", "Keep the 2"),
        "suited_qt" to CatalogueRuleInfo("Suited Queen-Ten", "Suited Q-10", "Keep the 2"),
        "suited_kt" to CatalogueRuleInfo("Suited King-Ten", "Suited K-10", "Keep the 2"),
        "suited_ak_aq_aj" to CatalogueRuleInfo("Suited Ace-King, Ace-Queen or Ace-Jack", "Suited A-K, A-Q or A-J", "Keep the 2"),
        "suited_at" to CatalogueRuleInfo("Suited Ace-Ten", "Suited A-10", "Keep the 2"),
        "qj_unsuited" to CatalogueRuleInfo("Queen-Jack, different suits", "Q-J unsuited", "Keep the 2"),
        "kqj_unsuited" to CatalogueRuleInfo("King-Queen-Jack, different suits", "K-Q-J unsuited", "Keep the 3"),
        "kq_kj_unsuited" to CatalogueRuleInfo("King-Queen or King-Jack, different suits", "K-Q or K-J unsuited", "Keep the 2"),
        "ak_aq_aj_unsuited" to CatalogueRuleInfo("Ace with King, Queen or Jack, different suits", "Ace with K, Q or J (unsuited)", "Keep the 2"),
        "akqj_unsuited" to CatalogueRuleInfo("Ace-King-Queen-Jack, different suits", "A-K-Q-J unsuited", "Keep the 4"),
        "single_jack" to CatalogueRuleInfo("Single Jack", "Single Jack", "Keep the card"),
        "single_queen" to CatalogueRuleInfo("Single Queen", "Single Queen", "Keep the card"),
        "single_king" to CatalogueRuleInfo("Single King", "Single King", "Keep the card"),
        "single_ace" to CatalogueRuleInfo("Single Ace", "Single Ace", "Keep the card"),
    )

    fun getInfo(ruleId: String): CatalogueRuleInfo {
        if (ruleId == "discard_all") return CatalogueRuleInfo("Discard all", "Discard all", "Discard all five")
        return rules[ruleId] ?: CatalogueRuleInfo(ruleId.replace("_", " "), ruleId.replace("_", " "), "")
    }
}
