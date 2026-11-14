package com.videopoker.android

import com.google.gson.JsonParser
import com.videopoker.android.data.engine.StrategyEngine
import com.videopoker.android.data.export.PlayedGameLog
import com.videopoker.android.data.export.SessionVoucher
import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.HoldChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDateTime
import java.util.zip.ZipFile

class PlayedGameLogTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testFolderNameAndWorkbenchSchemaExport() {
        val jsonFile = File("src/main/assets/games/double_bonus_10_7_built.json")
        val jsonObj = JsonParser.parseString(jsonFile.readText()).asJsonObject
        val game = StrategyEngine.parseGameConfig(jsonObj, jsonFile.name)

        val root = tempFolder.newFolder("recorded_sessions")
        val startedAt = LocalDateTime.of(2026, 11, 14, 10, 5, 0)
        val log = PlayedGameLog(
            game = game,
            outputRoot = root,
            startingCents = 2000L, // $20.00
            startedAt = startedAt,
            startingDenomination = 0.25,
            startingCredits = 5
        )

        assertEquals("played_Double_Bonus_(10-7)_20261114_100500", log.runDir.name)
        assertFalse(log.created)
        assertEquals(0, root.listFiles()?.size ?: 0)

        // Nothing created on early finish with 0 hands
        log.finish(PlayedGameLog.ENDED_BY_LEFT, startedAt)
        assertFalse(log.created)

        val dealt1 = listOf(Card('A', 's'), Card('A', 'h'), Card('K', 'd'), Card('7', 'c'), Card('2', 's'))
        val choices1 = StrategyEngine.evaluateHoldChoices(dealt1, game, maxChoices = Int.MAX_VALUE)
        val top1 = choices1.first()
        val final1 = listOf(Card('A', 's'), Card('A', 'h'), Card('A', 'd'), Card('K', 'c'), Card('K', 's'))
        log.recordHand(
            dealt = dealt1,
            heldMask = top1.holdMask,
            drawn = listOf(Card('A', 'd'), Card('K', 'c'), Card('K', 's')),
            finalCards = final1,
            classification = "full_house",
            creditsPlayed = 5,
            denominationCents = 25L,
            creditsWon = 50,
            trainingLevel = "learn",
            allHoldChoices = choices1,
            finishedAt = startedAt.plusSeconds(15),
            wentBroke = false
        )

        assertTrue(log.created)
        assertEquals(1, log.handsLogged)

        // Second hand with non-matching player_choice hold
        val dealt2 = listOf(Card('9', 'h'), Card('8', 'd'), Card('5', 'c'), Card('3', 's'), Card('2', 'h'))
        val customMask = listOf(false, false, false, true, false)
        val final2 = listOf(Card('Q', 'c'), Card('J', 's'), Card('4', 'd'), Card('3', 's'), Card('6', 'c'))
        val patChoices = listOf(
            HoldChoice(1, "discard_all", listOf(false, false, false, false, false), emptyList(), emptyList(), "Discard All", 0.0, true, game.strategyRules.size)
        )
        log.recordHand(
            dealt = dealt2,
            heldMask = customMask,
            drawn = listOf(Card('Q', 'c'), Card('J', 's'), Card('4', 'd'), Card('6', 'c')),
            finalCards = final2,
            classification = "nothing",
            creditsPlayed = 5,
            denominationCents = 25L,
            creditsWon = 0,
            trainingLevel = "casino",
            allHoldChoices = patChoices,
            finishedAt = startedAt.plusSeconds(40),
            wentBroke = false
        )

        log.finish(
            endedBy = PlayedGameLog.ENDED_BY_CASHED_OUT,
            at = startedAt.plusSeconds(45),
            voucher = SessionVoucher(
                moneyInCents = 2000L,
                handsPlayed = 2,
                cashOutCents = 3000L,
                resultCents = 1000L
            )
        )

        val sessionCsv = File(File(log.runDir, "CSV"), "session_0001.csv").readLines()
        assertEquals(3, sessionCsv.size)
        assertEquals(PlayedGameLog.SIMULATION_CSV_HEADER, sessionCsv[0])
        assertTrue(sessionCsv[1].startsWith("1,\"As,Ah,Kd,7c,2s\","))
        assertTrue(sessionCsv[1].contains(",full_house,5,0.25,1.25,50,12.50,11.25,31.25,"))
        assertTrue(sessionCsv[2].contains(",player_choice,3s,"))
        assertTrue(sessionCsv[2].endsWith(",30.00,"))

        val playedHands = File(log.runDir, "played_hands.csv").readLines()
        assertEquals(3, playedHands.size)
        assertEquals("hand_number,training_level,top_rule,top_choice_held,matched_rule,list_spot_held,list_size", playedHands[0])
        assertTrue(playedHands[1].startsWith("1,learn,${top1.ruleId},1,${top1.ruleId},${top1.slot},${game.strategyRules.size}"))
        assertEquals("2,casino,discard_all,0,,,${game.strategyRules.size}", playedHands[2])

        val gameInfo = JsonParser.parseString(File(log.runDir, "game_info.json").readText()).asJsonObject
        assertEquals(1, gameInfo.get("schema_version").asInt)
        assertEquals("played", gameInfo.get("kind").asString)
        assertEquals("cashed_out", gameInfo.get("ended_by").asString)
        val voucherObj = gameInfo.getAsJsonObject("voucher")
        assertNotNull(voucherObj)
        assertEquals(20.0, voucherObj.get("money_in").asDouble, 0.001)
        assertEquals(30.0, voucherObj.get("cash_out").asDouble, 0.001)

        val listed = PlayedGameLog.listRecordedSessions(root)
        assertEquals(1, listed.size)
        assertEquals(2, listed[0].handsPlayed)
        assertEquals(10.0, listed[0].netProfitLoss, 0.001)

        val zipDest = File(tempFolder.root, "${log.runDir.name}.zip")
        PlayedGameLog.zipSessionFolder(log.runDir, zipDest)
        ZipFile(zipDest).use { zf ->
            val names = zf.entries().toList().map { it.name }.toSet()
            assertTrue("${log.runDir.name}/CSV/session_0001.csv" in names)
            assertTrue("${log.runDir.name}/played_hands.csv" in names)
            assertTrue("${log.runDir.name}/game_info.json" in names)
            assertTrue("${log.runDir.name}/session_summary.csv" in names)
            assertTrue("${log.runDir.name}/batch_summary.txt" in names)
            assertTrue("${log.runDir.name}/batch_summary.json" in names)
        }
    }
}
