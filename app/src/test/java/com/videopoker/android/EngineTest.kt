package com.videopoker.android

import com.google.gson.JsonParser
import com.videopoker.android.data.engine.Deck
import com.videopoker.android.data.engine.HandClassifier
import com.videopoker.android.data.engine.StrategyEngine
import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.HandRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EngineTest {

    @Test
    fun testCardProperties() {
        val aceSpades = Card.fromString("As")
        assertEquals('A', aceSpades.rank)
        assertEquals('s', aceSpades.suit)
        assertEquals(14, aceSpades.rankValue)
        assertEquals(1, aceSpades.lowRankValue)
        assertTrue(aceSpades.isHigh)
        assertEquals("As.png", aceSpades.assetFileName)

        val twoDiamonds = Card.fromString("2d")
        assertEquals('2', twoDiamonds.rank)
        assertEquals('d', twoDiamonds.suit)
        assertEquals(2, twoDiamonds.rankValue)
        assertEquals(2, twoDiamonds.lowRankValue)
        assertFalse(twoDiamonds.isHigh)
        assertEquals("2d.png", twoDiamonds.assetFileName)

        assertTrue(aceSpades > twoDiamonds)
    }

    @Test
    fun testDeckDealAndDraw() {
        val deck = Deck(seed = 42L)
        val deal = deck.deal(5)
        assertEquals(5, deal.size)
        assertEquals(5, deal.distinct().size)

        // Hold first 3 cards, draw 2 replacement cards
        val holdMask = listOf(true, true, true, false, false)
        val drawn = deck.draw(deal, holdMask)
        assertEquals(5, drawn.size)
        assertEquals(5, drawn.distinct().size)

        // Held cards must match original deal
        assertEquals(deal[0], drawn[0])
        assertEquals(deal[1], drawn[1])
        assertEquals(deal[2], drawn[2])
    }

    @Test
    fun testHandClassifierStandardRanks() {
        // Royal Flush
        val royal = listOf("Ah", "Kh", "Qh", "Jh", "Th").map { Card.fromString(it) }
        assertEquals(HandRank.ROYAL_FLUSH, HandClassifier.classifyHand(royal))

        // Straight Flush
        val strFlush = listOf("9s", "8s", "7s", "6s", "5s").map { Card.fromString(it) }
        assertEquals(HandRank.STRAIGHT_FLUSH, HandClassifier.classifyHand(strFlush))

        // Wheel Straight Flush
        val wheelStrFlush = listOf("As", "2s", "3s", "4s", "5s").map { Card.fromString(it) }
        assertEquals(HandRank.STRAIGHT_FLUSH, HandClassifier.classifyHand(wheelStrFlush))

        // Full House
        val fullHouse = listOf("Kh", "Kd", "Ks", "8c", "8h").map { Card.fromString(it) }
        assertEquals(HandRank.FULL_HOUSE, HandClassifier.classifyHand(fullHouse))

        // Flush
        val flush = listOf("2d", "5d", "8d", "Td", "Kd").map { Card.fromString(it) }
        assertEquals(HandRank.FLUSH, HandClassifier.classifyHand(flush))

        // Straight
        val straight = listOf("9c", "8d", "7s", "6h", "5c").map { Card.fromString(it) }
        assertEquals(HandRank.STRAIGHT, HandClassifier.classifyHand(straight))

        // Three of a Kind
        val trips = listOf("7c", "7d", "7h", "2s", "9c").map { Card.fromString(it) }
        assertEquals(HandRank.THREE_OF_A_KIND, HandClassifier.classifyHand(trips))

        // Two Pair
        val twoPair = listOf("Ks", "Kh", "5d", "5c", "2h").map { Card.fromString(it) }
        assertEquals(HandRank.TWO_PAIR, HandClassifier.classifyHand(twoPair))

        // Jacks or Better
        val job = listOf("Jh", "Jd", "4s", "7c", "9h").map { Card.fromString(it) }
        assertEquals(HandRank.JACKS_OR_BETTER, HandClassifier.classifyHand(job))

        // Low Pair (Nothing)
        val lowPair = listOf("8c", "8s", "2d", "4h", "9s").map { Card.fromString(it) }
        assertEquals(HandRank.NOTHING, HandClassifier.classifyHand(lowPair))
    }

    @Test
    fun testBonusQuadClassification() {
        val bonusPayKeys = setOf("four_aces", "four_2s_3s_4s", "four_5s_thru_k", "four_aces_with_2_3_4")

        // 4 Aces with 2 kicker
        val acesWith2 = listOf("Ah", "As", "Ad", "Ac", "2d").map { Card.fromString(it) }
        assertEquals(HandRank.FOUR_ACES_WITH_2_3_4, HandClassifier.classifyHand(acesWith2, bonusPayKeys))

        // 4 3s with King kicker
        val quads3s = listOf("3h", "3s", "3d", "3c", "Kd").map { Card.fromString(it) }
        assertEquals(HandRank.FOUR_2S_3S_4S, HandClassifier.classifyHand(quads3s, bonusPayKeys))

        // 4 8s
        val quads8s = listOf("8h", "8s", "8d", "8c", "Qd").map { Card.fromString(it) }
        assertEquals(HandRank.FOUR_5S_THRU_K, HandClassifier.classifyHand(quads8s, bonusPayKeys))
    }

    @Test
    fun testStrategyEngineWithGameJson() {
        // Read Double Bonus 10/7 JSON directly from assets
        val jsonFile = File("src/main/assets/games/double_bonus_10_7_built.json")
        assertTrue(jsonFile.exists())

        val json = JsonParser.parseString(jsonFile.readText()).asJsonObject
        val config = StrategyEngine.parseGameConfig(json)

        assertEquals("double_bonus_10_7", config.variant)
        assertEquals(100.1725, config.returnPercentage, 0.001)
        assertTrue(config.strategyRules.isNotEmpty())

        // Test Deal with Two Pair: Ks, Kh, 5d, 5c, 2h
        // Optimal hold should be [true, true, true, true, false] holding Ks, Kh, 5d, 5c
        val deal = listOf("Ks", "Kh", "5d", "5c", "2h").map { Card.fromString(it) }
        val choices = StrategyEngine.evaluateHoldChoices(deal, config)

        assertTrue(choices.isNotEmpty())
        val topChoice = choices.first()
        assertTrue(topChoice.isTopChoice)
        assertEquals(listOf(true, true, true, true, false), topChoice.holdMask)
        assertEquals(listOf(0, 1, 2, 3), topChoice.keptIndexes)
        assertTrue(topChoice.description.contains("Two pair"))
    }
}
