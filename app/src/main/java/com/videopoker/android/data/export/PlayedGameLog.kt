package com.videopoker.android.data.export

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.videopoker.android.data.models.Card
import com.videopoker.android.data.models.GameConfig
import com.videopoker.android.data.models.HoldChoice
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

data class SessionVoucher(
    val moneyInCents: Long,
    val handsPlayed: Int,
    val cashOutCents: Long,
    val resultCents: Long
)

data class RecordedSessionInfo(
    val folderName: String,
    val folderPath: String,
    val payTableName: String,
    val handsPlayed: Int,
    val netProfitLoss: Double,
    val startedAtDisplay: String,
    val endedBy: String
)

/**
 * Pure Kotlin writer for Workbench-compatible played session folders (`played_<game>_<timestamp>`).
 * Matches `export/played_game_log.py` and `workbench/services/played_run.py` schema cleanly.
 */
class PlayedGameLog(
    private val game: GameConfig,
    outputRoot: File,
    startingCents: Long,
    private val startedAt: LocalDateTime = LocalDateTime.now(),
    private val startingDenomination: Double,
    private val startingCredits: Int
) {
    val runDir: File = File(outputRoot, runFolderName(game.displayName, startedAt))

    private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
    private val paytable: Map<String, Int> = game.baseMultipliers.ifEmpty {
        game.payTable.rows.associate { it.key to (it.values.firstOrNull() ?: 0) }
    }
    private val sortedPaytableKeys: List<String> = paytable.keys.sortedWith(
        compareBy<String>({ -(paytable[it] ?: 0) }, { it })
    )
    private val startingCentsVal: Long = startingCents

    private var balanceCents: Long = startingCents
    private var peakCents: Long = startingCents
    private var troughCents: Long = startingCents
    private var maxDrawdownCents: Long = 0L
    private var wagerCents: Long = 0L
    private var payoutCents: Long = 0L
    private val counts = mutableMapOf<String, Int>()
    private var hands: Int = 0
    private var endedAt: LocalDateTime = startedAt
    private var endedBy: String = ENDED_BY_LEFT
    private var voucher: SessionVoucher? = null

    var created: Boolean = false
        private set

    val handsLogged: Int
        get() = hands

    @Synchronized
    fun recordHand(
        dealt: List<Card>,
        heldMask: List<Boolean>,
        drawn: List<Card>,
        finalCards: List<Card>,
        classification: String,
        creditsPlayed: Int,
        denominationCents: Long,
        creditsWon: Int,
        trainingLevel: String,
        allHoldChoices: List<HoldChoice>,
        finishedAt: LocalDateTime = LocalDateTime.now(),
        wentBroke: Boolean = false
    ) {
        val mask = List(5) { idx -> heldMask.getOrElse(idx) { false } }
        ensureFolder()

        val handWagerCents = creditsPlayed.toLong() * denominationCents
        val handPayoutCents = creditsWon.toLong() * denominationCents
        val handNetCents = handPayoutCents - handWagerCents
        balanceCents += handNetCents

        hands += 1
        wagerCents += handWagerCents
        payoutCents += handPayoutCents
        counts[classification] = (counts[classification] ?: 0) + 1
        if (classification == "nothing") {
            counts["non_paying_hands"] = (counts["non_paying_hands"] ?: 0) + 1
        }

        peakCents = max(peakCents, balanceCents)
        troughCents = min(troughCents, balanceCents)
        maxDrawdownCents = max(maxDrawdownCents, peakCents - balanceCents)

        val topChoice = allHoldChoices.firstOrNull()
        val matchedChoice = allHoldChoices.firstOrNull { it.holdMask == mask }
        val holdSource = matchedChoice?.ruleId ?: PLAYER_CHOICE
        val heldCards = dealt.filterIndexed { i, _ -> mask[i] }

        val simRow = listOf(
            hands.toString(),
            formatCardList(dealt),
            holdSource,
            formatCardList(heldCards),
            formatCardList(drawn),
            formatCardList(finalCards),
            classification,
            creditsPlayed.toString(),
            formatMoney(denominationCents / 100.0),
            formatMoney(handWagerCents / 100.0),
            creditsWon.toString(),
            formatMoney(handPayoutCents / 100.0),
            formatMoney(handNetCents / 100.0),
            formatMoney(balanceCents / 100.0),
            if (wentBroke) "bankroll_exhaustion" else ""
        )
        appendLine(File(File(runDir, "CSV"), "session_0001.csv"), rowToCsvLine(simRow))

        val isTopHeld = if (topChoice != null && topChoice.holdMask == mask) "1" else "0"
        val playedRow = listOf(
            hands.toString(),
            trainingLevel,
            topChoice?.ruleId ?: "",
            isTopHeld,
            matchedChoice?.ruleId ?: "",
            matchedChoice?.slot?.toString() ?: "",
            game.strategyRules.size.toString()
        )
        appendLine(File(runDir, "played_hands.csv"), rowToCsvLine(playedRow))

        endedAt = finishedAt
        endedBy = if (wentBroke) ENDED_BY_WENT_BROKE else ENDED_BY_LEFT
        voucher = null
        rewriteSummaries()
    }

    @Synchronized
    fun finish(
        endedBy: String,
        at: LocalDateTime = LocalDateTime.now(),
        voucher: SessionVoucher? = null
    ) {
        require(endedBy in STOPPING_REASONS) { "Unknown ending: $endedBy" }
        if (!created) return
        this.endedBy = endedBy
        this.endedAt = at
        this.voucher = voucher
        rewriteSummaries()
    }

    private fun ensureFolder() {
        if (created) return
        val csvDir = File(runDir, "CSV")
        csvDir.mkdirs()
        appendLine(File(csvDir, "session_0001.csv"), SIMULATION_CSV_HEADER)
        appendLine(File(runDir, "played_hands.csv"), rowToCsvLine(PLAYED_HANDS_HEADER))
        created = true
    }

    private fun appendLine(target: File, line: String) {
        FileOutputStream(target, true).use { fos ->
            fos.write((line + "\n").toByteArray(StandardCharsets.UTF_8))
            fos.flush()
            fos.fd.sync()
        }
    }

    private fun atomicWrite(target: File, content: String) {
        target.parentFile?.mkdirs()
        val tmp = File.createTempFile(target.name + ".", ".tmp", target.parentFile)
        try {
            FileOutputStream(tmp).use { fos ->
                fos.write(content.toByteArray(StandardCharsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (t: Throwable) {
            if (tmp.exists()) tmp.delete()
            throw t
        }
    }

    private fun rewriteSummaries() {
        atomicWrite(File(runDir, "session_summary.csv"), buildSessionSummaryCsv())
        atomicWrite(File(runDir, "batch_summary.txt"), buildBatchSummaryTxt())
        atomicWrite(File(runDir, "batch_summary.json"), buildBatchSummaryJson())
        atomicWrite(File(runDir, "game_info.json"), buildGameInfoJson())
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0
    private fun round4(value: Double): Double = round(value * 10000.0) / 10000.0

    private fun buildSessionSummaryCsv(): String {
        val stoppingReason = STOPPING_REASONS[endedBy] ?: endedBy
        val wagered = wagerCents / 100.0
        val payout = payoutCents / 100.0
        val netReturn = (balanceCents - startingCentsVal) / 100.0
        val rtp = if (wagerCents > 0L) round2((payout / wagered) * 100.0) else 0.0

        val headers = CORE_SESSION_HEADERS + sortedPaytableKeys + listOf("non_paying_hands")
        val row = mutableListOf(
            "1",
            hands.toString(),
            stoppingReason,
            formatMoney(startingCentsVal / 100.0),
            formatMoney(balanceCents / 100.0),
            formatMoney(netReturn),
            formatMoney(wagered),
            formatMoney(payout),
            formatMoney(rtp),
            formatMoney(peakCents / 100.0),
            formatMoney(troughCents / 100.0),
            formatMoney(maxDrawdownCents / 100.0)
        )
        for (k in sortedPaytableKeys) {
            row.add((counts[k] ?: 0).toString())
        }
        val nonPaying = counts["non_paying_hands"] ?: (counts["nothing"] ?: 0)
        row.add(nonPaying.toString())

        return rowToCsvLine(headers) + "\n" + rowToCsvLine(row) + "\n"
    }

    private fun buildBatchSummaryJson(): String {
        val stoppingReason = STOPPING_REASONS[endedBy] ?: endedBy
        val wagered = round2(wagerCents / 100.0)
        val payout = round2(payoutCents / 100.0)
        val netReturn = round2((balanceCents - startingCentsVal) / 100.0)
        val batchRtp = if (wagerCents > 0L) round2((payout / wagered) * 100.0) else 0.0
        val profitable = if (netReturn > 0.0) 1 else 0
        val losing = if (netReturn < 0.0) 1 else 0
        val breakeven = if (netReturn == 0.0) 1 else 0

        val classCounts = linkedMapOf<String, Int>()
        for (k in sortedPaytableKeys) {
            classCounts[k] = counts[k] ?: 0
        }
        classCounts["non_paying_hands"] = counts["non_paying_hands"] ?: (counts["nothing"] ?: 0)

        val actualPcts = linkedMapOf<String, Double>()
        val expectedPcts = linkedMapOf<String, Double>()
        val deltaPcts = linkedMapOf<String, Double>()
        for ((k, cnt) in classCounts) {
            val act = if (hands > 0) round4(cnt.toDouble() / hands * 100.0) else 0.0
            val exp = game.expectedFrequenciesPct[k] ?: 0.0
            actualPcts[k] = act
            expectedPcts[k] = exp
            deltaPcts[k] = round4(act - exp)
        }

        val metadata = linkedMapOf<String, Any?>(
            "config_name" to game.configName,
            "variant" to game.variant,
            "timestamp" to endedAt.format(TS_SPACE_FMT),
            "session_count" to 1,
            "denomination" to round2(startingDenomination),
            "credits_played" to startingCredits,
            "wager_per_hand" to round2(startingDenomination * startingCredits),
            "starting_balance" to round2(startingCentsVal / 100.0),
            "target_profit" to null,
            "stop_loss" to null,
            "stopping_conditions" to emptyMap<String, Any>()
        )
        val payload = linkedMapOf<String, Any?>(
            "metadata" to metadata,
            "volume" to linkedMapOf(
                "total_sessions" to 1,
                "total_hands" to hands,
                "total_wagered" to wagered,
                "total_payout" to payout,
                "batch_rtp_pct" to batchRtp,
                "expected_base_rtp_pct" to game.returnPercentage
            ),
            "outcome_distribution" to linkedMapOf(
                "profitable_sessions" to profitable,
                "profitable_sessions_pct" to if (profitable == 1) 100.0 else 0.0,
                "losing_sessions" to losing,
                "losing_sessions_pct" to if (losing == 1) 100.0 else 0.0,
                "breakeven_sessions" to breakeven,
                "breakeven_sessions_pct" to if (breakeven == 1) 100.0 else 0.0,
                "stopping_reasons" to linkedMapOf(
                    "counts" to mapOf(stoppingReason to 1),
                    "percentages" to mapOf(stoppingReason to 100.0)
                )
            ),
            "returns" to linkedMapOf(
                "mean" to netReturn,
                "median" to netReturn,
                "stddev" to 0.0,
                "min" to netReturn,
                "max" to netReturn
            ),
            "durations" to linkedMapOf(
                "mean" to hands.toDouble(),
                "median" to hands.toDouble(),
                "min" to hands,
                "max" to hands
            ),
            "classifications" to linkedMapOf(
                "counts" to classCounts,
                "actual_percentages" to actualPcts,
                "expected_percentages" to expectedPcts,
                "delta_percentages" to deltaPcts
            )
        )
        return gson.toJson(payload) + "\n"
    }

    private fun buildBatchSummaryTxt(): String {
        val stoppingReason = STOPPING_REASONS[endedBy] ?: endedBy
        val wagered = round2(wagerCents / 100.0)
        val payout = round2(payoutCents / 100.0)
        val netReturn = round2((balanceCents - startingCentsVal) / 100.0)
        val batchRtp = if (wagerCents > 0L) round2((payout / wagered) * 100.0) else 0.0
        val profitable = if (netReturn > 0.0) 1 else 0
        val losing = if (netReturn < 0.0) 1 else 0
        val breakeven = if (netReturn == 0.0) 1 else 0
        val netDiff = round2(payout - wagered)
        val netSign = if (netDiff >= 0.0) "+" else "-"
        val varDelta = batchRtp - game.returnPercentage
        val varSign = if (varDelta >= 0.0) "+" else ""

        fun fmtSignedCurrency(v: Double, forcePlus: Boolean = false): String {
            val sign = when {
                v > 0.0 && forcePlus -> "+"
                v < 0.0 -> "-"
                else -> " "
            }
            val num = String.format(Locale.US, "%,8.2f", abs(v))
            return "$sign$$num"
        }

        val lines = mutableListOf(
            "=".repeat(80),
            "BATCH SIMULATION SUMMARY REPORT",
            "=".repeat(80),
            "Configuration: ${game.configName}",
            "Variant: ${game.variant}",
            "Run Timestamp: ${endedAt.format(TS_SPACE_FMT)}",
            "Sessions Run: 1",
            String.format(
                Locale.US,
                "Denomination: $%.2f (Wager: $%.2f per hand / %d credits)",
                startingDenomination,
                startingDenomination * startingCredits,
                startingCredits
            ),
            String.format(Locale.US, "Starting Bank: $%.2f per session", startingCentsVal / 100.0),
            "",
            "-".repeat(80),
            "1. SESSION OUTCOME DISTRIBUTION",
            "-".repeat(80),
            String.format(Locale.US, "Profitable Sessions: %6d (%6.2f%%)", profitable, if (profitable == 1) 100.0 else 0.0),
            String.format(Locale.US, "Losing Sessions:     %6d (%6.2f%%)", losing, if (losing == 1) 100.0 else 0.0),
            String.format(Locale.US, "Breakeven Sessions:  %6d (%6.2f%%)", breakeven, if (breakeven == 1) 100.0 else 0.0),
            "",
            "Termination Reasons:",
            String.format(Locale.US, " - %-22s %5d (%6.2f%%)", stoppingReason, 1, 100.0),
            "",
            "-".repeat(80),
            "2. ENSEMBLE FINANCIAL PERFORMANCE",
            "-".repeat(80),
            String.format(Locale.US, "Total Hands Simulated: %,10d", hands),
            String.format(Locale.US, "Total Wagered:         $%,11.2f", wagered),
            String.format(Locale.US, "Total Returned:        $%,11.2f", payout),
            String.format(Locale.US, "Net Return:            %s$%,10.2f", netSign, abs(netDiff)),
            String.format(Locale.US, "Observed Batch RTP:    %9.2f%%", batchRtp),
            String.format(Locale.US, "Expected Base RTP:    %9.2f%% (Variance: %s%.2f%%)", game.returnPercentage, varSign, varDelta),
            "",
            "Per-Session Net Return Distribution:",
            " - Mean:               ${fmtSignedCurrency(netReturn)}",
            " - Median:             ${fmtSignedCurrency(netReturn)}",
            String.format(Locale.US, " - Standard Deviation: $%,8.2f", 0.0),
            " - Maximum Profit:     ${fmtSignedCurrency(netReturn, forcePlus = true)}",
            " - Maximum Loss:       ${fmtSignedCurrency(netReturn)}",
            "",
            "Per-Session Duration (Hands Played):",
            String.format(Locale.US, " - Mean:               %8.1f hands", hands.toDouble()),
            String.format(Locale.US, " - Median:             %8.1f hands", hands.toDouble()),
            String.format(Locale.US, " - Shortest Session:   %,8d hands", hands),
            String.format(Locale.US, " - Longest Session:    %,8d hands", hands),
            "",
            "-".repeat(80),
            "3. SHOWDOWN HAND FREQUENCY & HIT RATES (ALL SESSIONS COMBINED)",
            "-".repeat(80),
            String.format(Locale.US, "%-26s%10s%15s%15s%14s", "Hand Classification", "Count", "Actual Freq", "Expected Freq", "Diff"),
            "-".repeat(80)
        )

        val keysWithNonPaying = sortedPaytableKeys + listOf("non_paying_hands")
        for (k in keysWithNonPaying) {
            val cnt = counts[k] ?: if (k == "non_paying_hands") (counts["nothing"] ?: 0) else 0
            val act = if (hands > 0) round4(cnt.toDouble() / hands * 100.0) else 0.0
            val exp = game.expectedFrequenciesPct[k] ?: 0.0
            val delta = round4(act - exp)
            val cntStr = String.format(Locale.US, "%,d", cnt)
            val actStr = String.format(Locale.US, "%.4f%%", act)
            val expStr = if (exp > 0.0) String.format(Locale.US, "%.4f%%", exp) else "N/A"
            val diffStr = if (exp > 0.0) String.format(Locale.US, "%s%.4f%%", if (delta >= 0.0) "+" else "", delta) else "N/A"
            lines.add(String.format(Locale.US, "%-26s%10s%15s%15s%14s", k, cntStr, actStr, expStr, diffStr))
        }
        lines.add("=".repeat(80))
        return lines.joinToString("\n") + "\n"
    }

    private fun buildGameInfoJson(): String {
        val voucherMap = if (endedBy == ENDED_BY_CASHED_OUT && voucher != null) {
            val v = voucher!!
            linkedMapOf<String, Any>(
                "money_in" to round2(v.moneyInCents / 100.0),
                "hands_played" to v.handsPlayed,
                "cash_out" to round2(v.cashOutCents / 100.0),
                "result" to round2(v.resultCents / 100.0)
            )
        } else {
            null
        }
        val info = linkedMapOf<String, Any?>(
            "schema_version" to SCHEMA_VERSION,
            "kind" to "played",
            "pay_table" to game.displayName,
            "config_file" to game.configFile,
            "starting_denomination" to round2(startingDenomination),
            "starting_credits_played" to startingCredits,
            "started_at" to startedAt.format(ISO_SEC_FMT),
            "ended_at" to endedAt.format(ISO_SEC_FMT),
            "ended_by" to endedBy,
            "voucher" to voucherMap
        )
        return gson.toJson(info) + "\n"
    }

    companion object {
        const val ENDED_BY_CASHED_OUT = "cashed_out"
        const val ENDED_BY_WENT_BROKE = "went_broke"
        const val ENDED_BY_LEFT = "left_without_cashing_out"
        const val PLAYER_CHOICE = "player_choice"
        const val SCHEMA_VERSION = 1

        private val STOPPING_REASONS = mapOf(
            ENDED_BY_CASHED_OUT to "cashed_out",
            ENDED_BY_WENT_BROKE to "bankroll_exhaustion",
            ENDED_BY_LEFT to "left_without_cashing_out"
        )

        val SIMULATION_HEADERS = listOf(
            "hand_number",
            "dealt_hand",
            "hold_source",
            "held_cards",
            "drawn_cards",
            "final_hand",
            "hand_classification",
            "credits_played",
            "denomination",
            "wager_amount",
            "credits_won",
            "payout_amount",
            "net_return",
            "cumulative_balance",
            "stopping_condition_met"
        )
        val SIMULATION_CSV_HEADER = SIMULATION_HEADERS.joinToString(",")

        val PLAYED_HANDS_HEADER = listOf(
            "hand_number",
            "training_level",
            "top_rule",
            "top_choice_held",
            "matched_rule",
            "list_spot_held",
            "list_size"
        )

        val CORE_SESSION_HEADERS = listOf(
            "session_id",
            "hands_played",
            "stopping_reason",
            "starting_balance",
            "final_balance",
            "net_profit_loss",
            "total_wagered",
            "total_payout",
            "session_rtp_pct",
            "peak_balance",
            "trough_balance",
            "max_drawdown"
        )

        private val FOLDER_TS_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        private val ISO_SEC_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        private val TS_SPACE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun runFolderName(payTableName: String, startedAt: LocalDateTime): String {
            val safe = payTableName.replace(" ", "_").replace("/", "-")
            return "played_${safe}_${startedAt.format(FOLDER_TS_FMT)}"
        }

        fun formatCardList(cards: List<Card>): String = cards.joinToString(",") { it.toString() }

        fun rowToCsvLine(fields: List<String>): String = fields.joinToString(",") { field ->
            if (field.contains(',') || field.contains('"') || field.contains('\n') || field.contains('\r')) {
                "\"" + field.replace("\"", "\"\"") + "\""
            } else {
                field
            }
        }

        private fun formatMoney(value: Double): String = String.format(Locale.US, "%.2f", value)

        fun listRecordedSessions(outputRoot: File): List<RecordedSessionInfo> {
            if (!outputRoot.isDirectory) return emptyList()
            val dirs = outputRoot.listFiles { f -> f.isDirectory && File(f, "game_info.json").isFile }
                ?: return emptyList()
            val result = mutableListOf<RecordedSessionInfo>()
            for (dir in dirs) {
                try {
                    val infoJson = JsonParser.parseString(File(dir, "game_info.json").readText(StandardCharsets.UTF_8)).asJsonObject
                    val payTable = infoJson.get("pay_table")?.asString ?: dir.name
                    val startedRaw = infoJson.get("started_at")?.asString ?: ""
                    val endedBy = infoJson.get("ended_by")?.asString ?: ENDED_BY_LEFT
                    var handsPlayed = 0
                    var netProfitLoss = 0.0
                    val sumFile = File(dir, "session_summary.csv")
                    if (sumFile.isFile) {
                        val lines = sumFile.readLines(StandardCharsets.UTF_8)
                        if (lines.size >= 2) {
                            val cols = lines[1].split(",")
                            handsPlayed = cols.getOrNull(1)?.toIntOrNull() ?: 0
                            netProfitLoss = cols.getOrNull(5)?.toDoubleOrNull() ?: 0.0
                        }
                    }
                    result.add(
                        RecordedSessionInfo(
                            folderName = dir.name,
                            folderPath = dir.absolutePath,
                            payTableName = payTable,
                            handsPlayed = handsPlayed,
                            netProfitLoss = netProfitLoss,
                            startedAtDisplay = startedRaw.replace('T', ' '),
                            endedBy = endedBy
                        )
                    )
                } catch (_: Exception) {
                    // Skip malformed session directory cleanly
                }
            }
            return result.sortedByDescending { it.startedAtDisplay }
        }

        fun zipSessionFolder(sessionDir: File, destZip: File) {
            require(sessionDir.isDirectory) { "Session folder missing: ${sessionDir.absolutePath}" }
            destZip.parentFile?.mkdirs()
            val rootName = sessionDir.name
            ZipOutputStream(BufferedOutputStream(FileOutputStream(destZip))).use { zos ->
                sessionDir.walkTopDown()
                    .filter { it.isFile && !it.name.endsWith(".tmp") }
                    .forEach { file ->
                        val rel = file.relativeTo(sessionDir).invariantSeparatorsPath
                        val entry = ZipEntry("$rootName/$rel")
                        zos.putNextEntry(entry)
                        file.inputStream().use { input -> input.copyTo(zos) }
                        zos.closeEntry()
                    }
            }
        }
    }
}
