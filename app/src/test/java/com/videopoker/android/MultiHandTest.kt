package com.videopoker.android

import com.google.gson.JsonParser
import com.videopoker.android.data.engine.StrategyEngine
import com.videopoker.android.data.export.PlayedGameLog
import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.HandRank
import com.videopoker.android.sound.SoundEffect
import com.videopoker.android.sound.SoundManager
import com.videopoker.android.ui.decks.shouldShowMainDrawBadge
import com.videopoker.android.ui.state.DealState
import com.videopoker.android.ui.state.GameMode
import com.videopoker.android.ui.state.GameState
import com.videopoker.android.ui.state.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDateTime

class MultiHandTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun loadDb107() = File("src/main/assets/games/double_bonus_10_7_built.json").let { f ->
        StrategyEngine.parseGameConfig(JsonParser.parseString(f.readText()).asJsonObject, f.name)
    }

    @Test
    fun testMultiHandSupportsLearnPracticeAndCasino() {
        val db = loadDb107()
        val initial = GameState(
            currentGame = db,
            currentMode = GameMode.LEARN,
            handsPerDeal = 1,
            currentBet = 5,
            dealState = DealState.SHOWDOWN_WIN
        )
        assertFalse(initial.isMultiHand)
        assertEquals(GameMode.LEARN, initial.effectiveMode)
        assertEquals(5, initial.totalBetCredits)

        // Switching to Triple Play (3 hands) resets bet to 1 and preserves user's chosen LEARN mode.
        val triple = GameViewModel.applyGameSelection(initial, db, handsPerDeal = 3)
        assertTrue(triple.isMultiHand)
        assertEquals(3, triple.handsPerDeal)
        assertEquals(1, triple.currentBet)
        assertEquals(3, triple.totalBetCredits)
        assertEquals(GameMode.LEARN, triple.effectiveMode)
        assertEquals(GameMode.LEARN, triple.currentMode)
        assertEquals(DealState.READY_TO_DEAL, triple.dealState)

        val tenPractice = GameViewModel.applyGameSelection(
            triple.copy(currentBet = 5, currentMode = GameMode.PRACTICE),
            db,
            handsPerDeal = 10
        )
        assertEquals(10, tenPractice.handsPerDeal)
        assertEquals(1, tenPractice.currentBet)
        assertEquals(10, tenPractice.totalBetCredits)
        assertEquals(GameMode.PRACTICE, tenPractice.effectiveMode)

        val fiveCasino = GameViewModel.applyGameSelection(
            tenPractice.copy(currentMode = GameMode.CASINO),
            db,
            handsPerDeal = 5
        )
        assertEquals(5, fiveCasino.handsPerDeal)
        assertEquals(GameMode.CASINO, fiveCasino.effectiveMode)

        // Tapping mode toggle in ViewModel switches Multi-Hand between Learn, Practice, and Casino
        // and mirrors Learn auto-holds to upper hands when a dealt hand is active.
        val dealt = listOf(Card('A', 's'), Card('A', 'h'), Card('K', 'd'), Card('7', 'c'), Card('2', 's'))
        val choices = StrategyEngine.evaluateHoldChoices(dealt, db)
        val vm = GameViewModel(
            fiveCasino.copy(
                cards = dealt,
                revealedMask = listOf(true, true, true, true, true),
                holdChoices = choices,
                dealState = DealState.DEALT_CHOOSING_HOLDS,
                upperLines = List(4) { idx -> com.videopoker.android.ui.state.MultiHandLine(index = idx) }
            )
        )
        vm.onModeToggle(GameMode.LEARN)
        val afterLearn = vm.uiState.value
        assertEquals(GameMode.LEARN, afterLearn.effectiveMode)
        assertEquals(choices.first().holdMask, afterLearn.heldMask)
        assertEquals(dealt[0], afterLearn.upperLines.first().cards[0])
        assertEquals(dealt[1], afterLearn.upperLines.first().cards[1])
        assertEquals(null, afterLearn.upperLines.first().cards[2])

        vm.onModeToggle(GameMode.PRACTICE)
        assertEquals(GameMode.PRACTICE, vm.uiState.value.effectiveMode)
    }

    @Test
    fun testUpperRevealOrdersBottomUpLeftToRight() {
        assertEquals(listOf(1, 0), GameViewModel.upperRevealOrder(3))
        assertEquals(listOf(2, 3, 0, 1), GameViewModel.upperRevealOrder(5))
        assertEquals(listOf(6, 7, 8, 3, 4, 5, 0, 1, 2), GameViewModel.upperRevealOrder(10))
    }

    @Test
    fun testWinEffectTiersForMultiHandLines() {
        assertEquals(null, SoundManager.winEffectForLine(HandRank.NOTHING, 0, 5))
        assertEquals(SoundEffect.WIN_SMALL, SoundManager.winEffectForLine(HandRank.JACKS_OR_BETTER, 5, 5))
        assertEquals(SoundEffect.WIN_MEDIUM, SoundManager.winEffectForLine(HandRank.THREE_OF_A_KIND, 15, 5))
        assertEquals(SoundEffect.WIN_BIG, SoundManager.winEffectForLine(HandRank.FOUR_ACES, 800, 5))
        assertEquals(SoundEffect.WIN_ROYAL, SoundManager.winEffectForLine(HandRank.ROYAL_FLUSH, 4000, 5))
    }

    @Test
    fun testMultiHandWorkbenchRecordingExport() {
        val game = loadDb107()
        val root = tempFolder.newFolder("recorded_multi")
        val startedAt = LocalDateTime.of(2026, 11, 22, 13, 45, 0)
        val log = PlayedGameLog(
            game = game,
            outputRoot = root,
            startingCents = 10000L, // $100.00
            startedAt = startedAt,
            startingDenomination = 0.25,
            startingCredits = 5,
            handsPerDeal = 5
        )

        val dealt = listOf(Card('A', 's'), Card('A', 'h'), Card('K', 'd'), Card('7', 'c'), Card('2', 's'))
        val choices = StrategyEngine.evaluateHoldChoices(dealt, game, maxChoices = Int.MAX_VALUE)
        val top = choices.first()
        val finalMain = listOf(Card('A', 's'), Card('A', 'h'), Card('K', 'c'), Card('K', 'd'), Card('3', 's'))

        val lineRanks = listOf("three_of_a_kind", "nothing", "two_pair", "nothing", "two_pair")
        val lineWins = listOf(15, 0, 5, 0, 5)
        log.recordHand(
            dealt = dealt,
            heldMask = top.holdMask,
            drawn = listOf(Card('K', 'c'), Card('K', 'd'), Card('3', 's')),
            finalCards = finalMain,
            classification = "two_pair",
            creditsPlayed = 25,
            denominationCents = 25L,
            creditsWon = 25,
            trainingLevel = "casino",
            allHoldChoices = choices,
            finishedAt = startedAt.plusSeconds(12),
            wentBroke = false,
            lineClassifications = lineRanks,
            lineCreditsWon = lineWins
        )
        log.finish(PlayedGameLog.ENDED_BY_LEFT, startedAt.plusSeconds(15))

        val csvLines = File(File(log.runDir, "CSV"), "session_0001.csv").readLines()
        assertEquals(2, csvLines.size)
        val headers = csvLines[0].split(",")
        assertEquals(19, headers.size)
        assertEquals("fsm_state", headers[15])
        assertEquals("hands_per_deal", headers[16])
        assertEquals("line_classifications", headers[17])
        assertEquals("line_credits_won", headers[18])

        assertTrue(csvLines[1].endsWith(",5,three_of_a_kind|nothing|two_pair|nothing|two_pair,15|0|5|0|5"))

        val gameInfo = JsonParser.parseString(File(log.runDir, "game_info.json").readText()).asJsonObject
        assertEquals(5, gameInfo.get("hands_per_deal").asInt)

        val summary = JsonParser.parseString(File(log.runDir, "batch_summary.json").readText()).asJsonObject
        val meta = summary.getAsJsonObject("metadata")
        assertEquals(5, meta.get("hands_per_deal").asInt)
        assertEquals(6.25, meta.get("wager_per_deal").asDouble, 0.01)
        assertTrue(meta.getAsJsonObject("game_modifiers").getAsJsonObject("multi_hand").get("enabled").asBoolean)

        val volume = summary.getAsJsonObject("volume")
        assertEquals(1, volume.get("total_hands").asInt)
        assertEquals(5, volume.get("total_poker_hands").asInt)

        val counts = summary.getAsJsonObject("classifications").getAsJsonObject("counts")
        assertEquals(2, counts.get("two_pair").asInt)
        assertEquals(1, counts.get("three_of_a_kind").asInt)
        assertEquals(2, counts.get("non_paying_hands").asInt)
    }

    @Test
    fun testMainHandWinBadgeAppearsFirstDuringMultiReveal() {
        val preDrawDealtPaying = GameState(
            handsPerDeal = 5,
            dealState = DealState.DEALT_CHOOSING_HOLDS,
            isRevealingMultiDraw = false,
            isMainLineSettled = false,
            dealtPayingRank = HandRank.THREE_OF_A_KIND,
            dealtWinCredits = 15
        )
        assertFalse(shouldShowMainDrawBadge(preDrawDealtPaying))

        val midMainCardFlip = preDrawDealtPaying.copy(
            isRevealingMultiDraw = true,
            isMainLineSettled = false,
            dealtPayingRank = HandRank.NOTHING,
            dealtWinCredits = 0
        )
        assertFalse(shouldShowMainDrawBadge(midMainCardFlip))

        // Hand 1 finishes drawing while dealState is still DEALT_CHOOSING_HOLDS and upper hands 2..5
        // have not yet revealed: Hand 1 win badge must already be visible FIRST!
        val mainSettledDuringReveal = midMainCardFlip.copy(
            isMainLineSettled = true,
            mainLineWinRank = HandRank.THREE_OF_A_KIND,
            mainLineWinCredits = 15
        )
        assertTrue(shouldShowMainDrawBadge(mainSettledDuringReveal))

        val settledShowdown = mainSettledDuringReveal.copy(
            isRevealingMultiDraw = false,
            dealState = DealState.SHOWDOWN_WIN
        )
        assertTrue(shouldShowMainDrawBadge(settledShowdown))

        val clearedByBetOne = settledShowdown.copy(hideMultiWinBadges = true)
        assertFalse(shouldShowMainDrawBadge(clearedByBetOne))
    }
}
