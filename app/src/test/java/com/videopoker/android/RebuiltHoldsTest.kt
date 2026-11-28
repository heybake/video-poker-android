package com.videopoker.android

import com.google.gson.JsonParser
import com.videopoker.android.data.engine.StrategyEngine
import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.GameConfig
import com.videopoker.android.data.models.HoldCatalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifies Issues #30, #31, #32, and #33:
 * - all 41 built JSON game assets parse cleanly
 * - all hold rule ids (including the 6 new kicker/ace rules) resolve in HoldCatalogue
 * - StrategyEngine evaluates having.extra_card (three-of-a-kind + kicker) and quad kickers accurately
 * - spot checks across updated families (suited_ak_aq_aj, pair_of_aces, kicker holds) match expectations
 */
class RebuiltHoldsTest {

    private val gamesDir = File("src/main/assets/games")

    private fun loadGame(fileName: String): GameConfig {
        val file = File(gamesDir, fileName)
        assertTrue("Missing asset $fileName", file.exists())
        return StrategyEngine.parseGameConfig(JsonParser.parseString(file.readText()).asJsonObject, fileName)
    }

    @Test
    fun all41GameAssetsResolveEveryRuleIdInHoldCatalogue() {
        val files = gamesDir.listFiles { _, name -> name.endsWith("_built.json") }?.sortedBy { it.name }.orEmpty()
        assertEquals(41, files.size)

        for (file in files) {
            val config = loadGame(file.name)
            assertTrue("${file.name} has no rules", config.strategyRules.isNotEmpty())
            for (rule in config.strategyRules) {
                assertTrue("${file.name} missing EV for ${rule.id}", rule.ev != null && rule.ev!! >= 0.0)
                val info = HoldCatalogue.getInfo(rule.id)
                assertTrue("${file.name} missing shortLabel for ${rule.id}", info.shortLabel.isNotBlank())
                if (rule.id != "discard_all") {
                    assertTrue("${file.name} missing keeps for ${rule.id}", info.keeps.isNotBlank())
                    assertNotEquals(
                        "${file.name} fell back to raw rule id ${rule.id}",
                        rule.id.replace("_", " "),
                        info.label
                    )
                }
            }
        }
    }

    @Test
    fun newHoldCatalogueEntriesMatchSpecification() {
        val expected = mapOf(
            "four_aces_with_kicker" to "Four aces + 2-4",
            "four_2s_3s_4s_with_kicker" to "Four 2s-4s + A-4",
            "three_aces_with_kicker" to "Three aces + 2-4",
            "three_2s_3s_4s_with_kicker" to "Three 2s-4s + A-4",
            "pair_of_aces" to "Pair of aces",
            "suited_ak_aq_aj" to "Suited A-K, A-Q or A-J"
        )
        for ((id, shortLabel) in expected) {
            val info = HoldCatalogue.getInfo(id)
            assertEquals(shortLabel, info.shortLabel)
            assertNotEquals(id.replace("_", " "), info.label)
        }
    }

    @Test
    fun extraCardKickerRulesAndQuadKickersEvaluateAccurately() {
        val tdb96 = loadGame("triple_double_bonus_9_6_built.json")
        val ddb96 = loadGame("double_double_bonus_9_6_built.json")

        // 1. A A A 3 9 -> three_aces_with_kicker keeps 4 cards (three aces + 3)
        val aaa39 = listOf("As", "Ah", "Ad", "3c", "9s").map { Card.fromString(it) }
        val topAaa39 = StrategyEngine.evaluateHoldChoices(aaa39, tdb96).first()
        assertEquals("three_aces_with_kicker", topAaa39.ruleId)
        assertEquals(listOf(true, true, true, true, false), topAaa39.holdMask)

        // 2. A A A K 9 -> falls through to plain three_aces (keeps 3 aces only)
        val aaaK9 = listOf("As", "Ah", "Ad", "Kc", "9s").map { Card.fromString(it) }
        val topAaaK9 = StrategyEngine.evaluateHoldChoices(aaaK9, tdb96).first()
        assertEquals("three_aces", topAaaK9.ruleId)
        assertEquals(listOf(true, true, true, false, false), topAaaK9.holdMask)

        // 3. 3 3 3 A 9 -> three_2s_3s_4s_with_kicker keeps 4 cards (three 3s + A)
        val threeA9 = listOf("3s", "3h", "3d", "Ac", "9s").map { Card.fromString(it) }
        val topThreeA9 = StrategyEngine.evaluateHoldChoices(threeA9, tdb96).first()
        assertEquals("three_2s_3s_4s_with_kicker", topThreeA9.ruleId)
        assertEquals(listOf(true, true, true, true, false), topThreeA9.holdMask)

        // 4. 3 3 3 K 9 -> falls through to plain three_of_a_kind (keeps 3s only)
        val threeK9 = listOf("3s", "3h", "3d", "Kc", "9s").map { Card.fromString(it) }
        val topThreeK9 = StrategyEngine.evaluateHoldChoices(threeK9, tdb96).first()
        assertEquals("three_of_a_kind", topThreeK9.ruleId)
        assertEquals(listOf(true, true, true, false, false), topThreeK9.holdMask)

        // 5. A A A 2 3 -> holds three aces plus ONLY the FIRST kicker in deal order (2, not 3)
        val aaa23 = listOf("As", "Ah", "Ad", "2c", "3s").map { Card.fromString(it) }
        val topAaa23 = StrategyEngine.evaluateHoldChoices(aaa23, tdb96).first()
        assertEquals("three_aces_with_kicker", topAaa23.ruleId)
        assertEquals(listOf(true, true, true, true, false), topAaa23.holdMask)

        // 6. 4 4 4 4 A -> four_2s_3s_4s_with_kicker holds all 5
        val quads4A = listOf("4s", "4h", "4d", "4c", "As").map { Card.fromString(it) }
        val topQuads4A = StrategyEngine.evaluateHoldChoices(quads4A, ddb96).first()
        assertEquals("four_2s_3s_4s_with_kicker", topQuads4A.ruleId)
        assertEquals(listOf(true, true, true, true, true), topQuads4A.holdMask)

        // 7. A A A A 3 -> four_aces_with_kicker holds all 5
        val quadAces3 = listOf("As", "Ah", "Ad", "Ac", "3s").map { Card.fromString(it) }
        val topQuadAces3 = StrategyEngine.evaluateHoldChoices(quadAces3, ddb96).first()
        assertEquals("four_aces_with_kicker", topQuadAces3.ruleId)
        assertEquals(listOf(true, true, true, true, true), topQuadAces3.holdMask)
    }

    @Test
    fun rebuiltFamiliesSpotCheckSuitedAceAndPairOfAces() {
        val sampleSuitedAK = listOf("Ah", "Kh", "2c", "7d", "9s").map { Card.fromString(it) }
        val gamesWithSuitedAK = listOf(
            "jacks_or_better_9_6_built.json",
            "bonus_poker_8_5_built.json",
            "bonus_poker_deluxe_9_6_built.json",
            "double_bonus_10_7_built.json",
            "double_double_bonus_9_6_built.json",
            "super_aces_bonus_8_5_built.json",
            "triple_bonus_plus_9_5_built.json",
            "white_hot_aces_9_5_built.json",
            "usa_poker_35_built.json",
            "hybrid_db_10_8_5_bp_quads_built.json"
        )
        for (name in gamesWithSuitedAK) {
            val cfg = loadGame(name)
            val top = StrategyEngine.evaluateHoldChoices(sampleSuitedAK, cfg).first()
            assertEquals("$name top hold for Ah Kh 2c 7d 9s", "suited_ak_aq_aj", top.ruleId)
            assertEquals("$name mask for Ah Kh 2c 7d 9s", listOf(true, true, false, false, false), top.holdMask)
        }

        // Pair of aces over two pair (A A 5 5 9) matches file rule priority order
        val aa559 = listOf("As", "Ah", "5d", "5c", "9s").map { Card.fromString(it) }
        val db965 = loadGame("double_bonus_9_6_5_built.json")
        val twoPairIdx = db965.strategyRules.indexOfFirst { it.id == "two_pair" }
        val pairAcesIdx = db965.strategyRules.indexOfFirst { it.id == "pair_of_aces" }
        val expectedId = if (pairAcesIdx in 0 until twoPairIdx) "pair_of_aces" else "two_pair"
        assertEquals(expectedId, StrategyEngine.evaluateHoldChoices(aa559, db965).first().ruleId)
    }
}
