package com.videopoker.android.data.engine

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.GameConfig
import com.videopoker.android.data.models.HoldChoice
import com.videopoker.android.data.models.PayTable
import com.videopoker.android.data.models.PayTableRow
import com.videopoker.android.data.models.StrategyRule

/**
 * Pure Kotlin strategy evaluator translating Query DSL rules and producing optimal hold choices.
 */
object StrategyEngine {

    /**
     * Parse a built game JSON into a [GameConfig].
     */
    fun parseGameConfig(json: JsonObject): GameConfig {
        val variant = json.get("variant")?.asString ?: "unknown"
        val configName = json.get("config_name")?.asString ?: "$variant.json"

        // Theoretical return
        val benchObj = json.getAsJsonObject("theoretical_benchmarks")
        val rtp = benchObj?.get("base_rtp")?.asDouble ?: 99.0

        // Denominations
        val denoms = json.getAsJsonArray("denominations")?.map { it.asDouble } ?: listOf(0.25)
        val defaultDenom = json.get("denomination")?.asDouble ?: denoms.firstOrNull() ?: 0.25

        // Paytable
        val ptObj = json.getAsJsonObject("paytable")
        val payRows = mutableListOf<PayTableRow>()
        ptObj?.entrySet()?.forEach { entry ->
            val key = entry.key
            val baseVal = entry.value.asInt
            val values = (1..5).map { coin ->
                if (key == "royal_flush" && coin == 5) 4000 else baseVal * coin
            }
            val label = formatHandLabel(key)
            payRows.add(PayTableRow(key, label, values))
        }

        // Display name & family
        val (family, display) = getGameTitles(variant)

        // Strategy Rules
        val rules = mutableListOf<StrategyRule>()
        val rulesArr = json.getAsJsonArray("hold_priority_list")
        rulesArr?.forEach { elem ->
            if (elem.isJsonObject) {
                val rObj = elem.asJsonObject
                val id = rObj.get("id")?.asString ?: ""
                val hold = rObj.get("hold")?.asString ?: "matching_group"
                val query = rObj.getAsJsonObject("query") ?: JsonObject()
                rules.add(StrategyRule(id, hold, query))
            }
        }

        return GameConfig(
            configName = configName,
            variant = variant,
            familyName = family,
            displayName = display,
            returnPercentage = rtp,
            denominations = denoms,
            defaultDenomination = defaultDenom,
            payTable = PayTable(payRows),
            strategyRules = rules
        )
    }

    /**
     * Resolve all matching hold choices for a given deal in priority order.
     */
    fun evaluateHoldChoices(deal: List<Card>, gameConfig: GameConfig, maxChoices: Int = 3): List<HoldChoice> {
        require(deal.size == 5) { "Deal must contain exactly 5 cards" }

        val seenMasks = mutableSetOf<List<Boolean>>()
        val choices = mutableListOf<HoldChoice>()

        for (rule in gameConfig.strategyRules) {
            val (matched, matchingCards) = evaluateQuery(deal, rule.query)
            if (matched) {
                val mask = projectHoldMask(deal, rule.hold, matchingCards)
                if (seenMasks.add(mask)) {
                    val row = choices.size + 1
                    val keptIndices = deal.indices.filter { mask[it] }
                    val keptCards = keptIndices.map { deal[it] }
                    val desc = generateDescription(rule.id, mask, keptCards)
                    val isTop = (choices.isEmpty())

                    choices.add(
                        HoldChoice(
                            row = row,
                            ruleId = rule.id,
                            holdMask = mask,
                            keptIndexes = keptIndices,
                            keptCards = keptCards,
                            description = desc,
                            expectedValue = 0.0, // calculated or ranked
                            isTopChoice = isTop
                        )
                    )

                    if (choices.size >= maxChoices) break
                }
            }
        }

        // If no rule matched, fallback to Discard All
        if (choices.isEmpty()) {
            val mask = listOf(false, false, false, false, false)
            choices.add(
                HoldChoice(
                    row = 1,
                    ruleId = "discard_all",
                    holdMask = mask,
                    keptIndexes = emptyList(),
                    keptCards = emptyList(),
                    description = "Discard All",
                    expectedValue = 0.0,
                    isTopChoice = true
                )
            )
        }

        return choices
    }

    private fun projectHoldMask(deal: List<Card>, directive: String, matchingCards: List<Card>): List<Boolean> {
        return when (directive) {
            "all" -> listOf(true, true, true, true, true)
            "none" -> listOf(false, false, false, false, false)
            else -> {
                val matchSet = matchingCards.toSet()
                deal.map { it in matchSet }
            }
        }
    }

    /**
     * Evaluates a Query DSL block against a 5-card deal.
     */
    fun evaluateQuery(deal: List<Card>, query: JsonObject): Pair<Boolean, List<Card>> {
        if (query.get("any")?.asBoolean == true) {
            return Pair(true, emptyList())
        }

        if (query.has("group_by")) {
            val groupBy = query.get("group_by").asString
            val having = query.getAsJsonObject("having")
            val havingCounts = query.getAsJsonArray("having_counts")?.map { it.asInt }

            val (matched, cards) = matchGroupBy(deal, groupBy, having, havingCounts)
            if (!matched) return Pair(false, emptyList())

            if (query.has("kicker")) {
                val (kMatched, _) = matchKicker(deal, query.getAsJsonObject("kicker"))
                if (!kMatched) return Pair(false, emptyList())
                return Pair(true, deal)
            }
            return Pair(true, cards)
        }

        if (query.has("sequence")) {
            val (matched, cards) = matchSequence(deal, query.getAsJsonObject("sequence"))
            if (matched) return Pair(true, cards)
            return Pair(false, emptyList())
        }

        if (query.has("kicker")) {
            return matchKicker(deal, query.getAsJsonObject("kicker"))
        }

        if (query.has("exact_ranks") || query.has("match_any")) {
            val exactRanks = query.getAsJsonArray("exact_ranks")?.map { it.asString }
            val matchAny = query.getAsJsonArray("match_any")?.map { inner ->
                inner.asJsonArray.map { it.asString }
            }
            val suited = query.get("suited")?.let { if (it.isJsonNull) null else it.asBoolean }
            return matchRankSets(deal, exactRanks, matchAny, suited)
        }

        if (query.has("having_counts")) {
            val havingCounts = query.getAsJsonArray("having_counts").map { it.asInt }
            return matchGroupBy(deal, "rank", null, havingCounts)
        }

        return Pair(false, emptyList())
    }

    private fun matchGroupBy(
        deal: List<Card>,
        groupBy: String,
        having: JsonObject?,
        havingCounts: List<Int>?
    ): Pair<Boolean, List<Card>> {
        val groups = mutableMapOf<String, MutableList<Card>>()
        for (c in deal) {
            val key = if (groupBy == "rank") c.rank.toString() else c.suit.toString()
            groups.getOrPut(key) { mutableListOf() }.add(c)
        }

        var effHavingCounts = havingCounts
        if (effHavingCounts == null && having != null) {
            if (having.has("counts")) {
                effHavingCounts = having.getAsJsonArray("counts").map { it.asInt }
            } else if (having.has("having_counts")) {
                effHavingCounts = having.getAsJsonArray("having_counts").map { it.asInt }
            }
        }

        if (effHavingCounts != null) {
            val sortedGroups = groups.values.sortedByDescending { it.size }
            val reqLen = effHavingCounts.size
            if (sortedGroups.size < reqLen) return Pair(false, emptyList())
            val actualSizes = sortedGroups.take(reqLen).map { it.size }
            if (actualSizes != effHavingCounts) return Pair(false, emptyList())

            val matchedSet = sortedGroups.take(reqLen).flatten().toSet()
            return Pair(true, deal.filter { it in matchedSet })
        }

        if (having == null || having.entrySet().isEmpty()) {
            return Pair(false, emptyList())
        }

        for ((key, bCards) in groups) {
            var candidateCards: List<Card> = bCards.toList()

            if (having.has("in_ranks")) {
                val inRanks = having.getAsJsonArray("in_ranks").map { it.asString }
                if (groupBy == "rank") {
                    if (key !in inRanks) continue
                } else {
                    candidateCards = candidateCards.filter { it.rank.toString() in inRanks }
                }
            }

            if (having.has("must_include")) {
                val must = having.get("must_include").asString
                if (candidateCards.none { it.rank.toString() == must }) continue
            }

            if (having.has("exact_ranks")) {
                val target = having.getAsJsonArray("exact_ranks").map { it.asString }.toSet()
                val sub = candidateCards.filter { it.rank.toString() in target }
                if (sub.map { it.rank.toString() }.toSet() != target) continue
                candidateCards = sub
            }

            if (having.has("match_any")) {
                var anyMatched = false
                val matchAny = having.getAsJsonArray("match_any")
                for (elem in matchAny) {
                    val target = elem.asJsonArray.map { it.asString }.toSet()
                    val sub = candidateCards.filter { it.rank.toString() in target }
                    if (sub.map { it.rank.toString() }.toSet() == target) {
                        candidateCards = sub
                        anyMatched = true
                        break
                    }
                }
                if (!anyMatched) continue
            }

            if (having.has("sequence")) {
                val (seqMatched, seqCards) = matchSequence(candidateCards, having.getAsJsonObject("sequence"))
                if (!seqMatched) continue
                candidateCards = seqCards
            }

            if (having.has("count")) {
                val targetCount = having.get("count").asInt
                if (candidateCards.size != targetCount) continue
            }

            val highCount = candidateCards.count { it.isHigh }
            if (having.has("min_high_cards") && highCount < having.get("min_high_cards").asInt) continue
            if (having.has("max_high_cards") && highCount > having.get("max_high_cards").asInt) continue
            if (having.has("high_cards") && highCount != having.get("high_cards").asInt) continue

            val matchedInDeal = deal.filter { it in candidateCards }
            return Pair(true, matchedInDeal)
        }

        return Pair(false, emptyList())
    }

    private fun matchSequence(cards: List<Card>, seqConfig: JsonObject): Pair<Boolean, List<Card>> {
        val targetLen = seqConfig.get("length").asInt
        if (cards.size < targetLen) return Pair(false, emptyList())

        val targetGaps = seqConfig.get("gaps")?.let { if (it.isJsonNull) null else it.asInt }
        val targetSpan = seqConfig.get("span")?.let { if (it.isJsonNull) null else it.asInt }
        val targetOutside = seqConfig.get("outside")?.let { if (it.isJsonNull) null else it.asBoolean }
        val minHigh = seqConfig.get("min_high_cards")?.let { if (it.isJsonNull) null else it.asInt }
        val maxHigh = seqConfig.get("max_high_cards")?.let { if (it.isJsonNull) null else it.asInt }
        val highExact = seqConfig.get("high_cards")?.let { if (it.isJsonNull) null else it.asInt }

        val combos = getCombinations(cards, targetLen)
        for (combo in combos) {
            val ranks = combo.map { it.rank }
            if (ranks.distinct().size < targetLen) continue

            val hasAce = combo.any { it.rank == 'A' }
            val candidateVals = mutableListOf<List<Int>>()
            candidateVals.add(combo.map { it.rankValue }.sorted())
            if (hasAce) {
                candidateVals.add(combo.map { it.lowRankValue }.sorted())
            }

            for (rankVals in candidateVals) {
                val span = rankVals.last() - rankVals.first() + 1
                val gaps = span - targetLen
                val isAceTerminalInside = (targetLen == 4 && span == 4 && gaps == 0 && (rankVals.first() == 1 || rankVals.last() == 14))

                var seqMatches = false
                if (targetSpan != null && targetGaps != null) {
                    if (gaps == targetGaps && span == targetSpan) {
                        seqMatches = true
                    } else if (targetGaps == 1 && targetSpan == 5 && isAceTerminalInside) {
                        seqMatches = true
                    }
                } else if (targetGaps != null) {
                    if (gaps == targetGaps) {
                        seqMatches = true
                    } else if (targetGaps == 1 && isAceTerminalInside) {
                        seqMatches = true
                    }
                } else if (targetSpan != null) {
                    if (span == targetSpan) seqMatches = true
                }

                if (!seqMatches) continue

                val isOpenEnded = (gaps == 0 && !hasAce && rankVals.first() >= 2 && rankVals.last() <= 13)
                if (targetOutside == true && !isOpenEnded) continue
                if (targetOutside == false && isOpenEnded) continue

                val highCount = combo.count { it.isHigh }
                if (minHigh != null && highCount < minHigh) continue
                if (maxHigh != null && highCount > maxHigh) continue
                if (highExact != null && highCount != highExact) continue

                return Pair(true, cards.filter { it in combo })
            }
        }

        return Pair(false, emptyList())
    }

    private fun matchRankSets(
        deal: List<Card>,
        exactRanks: List<String>?,
        matchAny: List<List<String>>?,
        suited: Boolean?
    ): Pair<Boolean, List<Card>> {
        val candidateRankLists = mutableListOf<List<String>>()
        if (exactRanks != null) {
            candidateRankLists.add(exactRanks)
        } else if (matchAny != null) {
            candidateRankLists.addAll(matchAny)
        }

        for (targetRanks in candidateRankLists) {
            val targetCount = targetRanks.size
            val combos = getCombinations(deal, targetCount)
            for (combo in combos) {
                val comboRanks = combo.map { it.rank.toString() }.sorted()
                if (comboRanks != targetRanks.sorted()) continue

                val uniqueSuits = combo.map { it.suit }.distinct().size
                if (suited == true && uniqueSuits != 1) continue
                if (suited == false && uniqueSuits < 2) continue

                return Pair(true, deal.filter { it in combo })
            }
        }

        return Pair(false, emptyList())
    }

    private fun matchKicker(deal: List<Card>, kickerConfig: JsonObject): Pair<Boolean, List<Card>> {
        val counts = deal.groupingBy { it.rank }.eachCount()
        val quadRank = counts.entries.firstOrNull { it.value == 4 }?.key ?: return Pair(false, emptyList())
        val kickerCard = deal.firstOrNull { it.rank != quadRank } ?: return Pair(false, emptyList())

        val exactRank = (kickerConfig.get("exact_rank") ?: kickerConfig.get("rank"))?.asString
        if (exactRank != null && kickerCard.rank.toString() != exactRank) {
            return Pair(false, emptyList())
        }

        val inRanks = kickerConfig.getAsJsonArray("in_ranks")?.map { it.asString }
        if (inRanks != null && kickerCard.rank.toString() !in inRanks) {
            return Pair(false, emptyList())
        }

        return Pair(true, deal)
    }

    private fun <T> getCombinations(list: List<T>, k: Int): List<List<T>> {
        val result = mutableListOf<List<T>>()
        fun backtrack(start: Int, current: MutableList<T>) {
            if (current.size == k) {
                result.add(ArrayList(current))
                return
            }
            for (i in start until list.size) {
                current.add(list[i])
                backtrack(i + 1, current)
                current.removeAt(current.size - 1)
            }
        }
        backtrack(0, mutableListOf())
        return result
    }

    private fun generateDescription(ruleId: String, mask: List<Boolean>, keptCards: List<Card>): String {
        val count = keptCards.size
        if (count == 0) return "Discard All"
        if (count == 5) return "Keep Pat Hand (${keptCards.joinToString(", ") { it.rank.toString() }})"

        val cleanId = ruleId.replace("_", " ")
        val cardList = keptCards.joinToString(", ") { it.rank.toString() }

        return when {
            cleanId.contains("two pair") -> "Keep 2 Pair ($cardList)"
            cleanId.contains("three") -> "Keep 3 of a Kind ($cardList)"
            cleanId.contains("pair") -> "Keep Pair of ${keptCards.first().rank}s"
            cleanId.contains("flush") -> "Keep $count to a Flush ($cardList)"
            cleanId.contains("straight") -> "Keep $count to a Straight ($cardList)"
            else -> "Hold $cardList ($cleanId)"
        }
    }

    private fun formatHandLabel(key: String): String {
        return when (key) {
            "royal_flush" -> "ROYAL FLUSH"
            "straight_flush" -> "STRAIGHT FLUSH"
            "four_aces_with_2_3_4" -> "4 ACES W/ 2,3,4"
            "four_2s_3s_4s_with_a_thru_4" -> "4 2,3,4s W/ A-4"
            "four_aces" -> "FOUR ACES"
            "four_2s_3s_4s" -> "FOUR 2s, 3s, 4s"
            "four_5s_thru_k" -> "FOUR 5s THRU Ks"
            "four_of_a_kind" -> "FOUR OF A KIND"
            "full_house" -> "FULL HOUSE"
            "flush" -> "FLUSH"
            "straight" -> "STRAIGHT"
            "three_of_a_kind" -> "THREE OF A KIND"
            "two_pair" -> "TWO PAIR"
            "jacks_or_better" -> "JACKS OR BETTER"
            else -> key.replace("_", " ").uppercase()
        }
    }

    private fun getGameTitles(variant: String): Pair<String, String> {
        val clean = variant.replace("_built", "").replace("_", " ")
        val words = clean.split(" ").map { it.replaceFirstChar { c -> c.uppercaseChar() } }
        val title = words.joinToString(" ")
        val family = when {
            variant.contains("double_double") -> "Double Double Bonus"
            variant.contains("triple_double") -> "Triple Double Bonus"
            variant.contains("double_bonus") -> "Double Bonus Poker"
            variant.contains("bonus_poker") -> "Bonus Poker"
            variant.contains("deuces_wild") -> "Deuces Wild"
            variant.contains("joker") -> "Joker Poker"
            variant.contains("all_american") -> "All American"
            variant.contains("white_hot") -> "White Hot Aces"
            else -> "Jacks or Better"
        }
        return Pair(family, title)
    }
}
