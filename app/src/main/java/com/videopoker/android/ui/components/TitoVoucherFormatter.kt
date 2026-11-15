package com.videopoker.android.ui.components

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Generates authentic Vegas-style TITO Gaming Voucher metadata, English dollar wording,
 * and scannable Code 128B barcode module runs.
 */
object TitoVoucherFormatter {

    const val CASINO_NAME = "Hold & Draw Casino"
    const val CASINO_ADDRESS_LINE_1 = "The Double-Down Suite, Tilt Street"
    const val CASINO_ADDRESS_LINE_2 = "Bad Beat, NV 89111"
    const val CASINO_URL = "WWW.HOLDANDDRAW.COM"
    const val BARCODE_EASTER_EGG_TEXT = "ROYAL FLUSH NOT FOUND"
    const val ASSET_NUMBER = "2947"

    private val ONES = arrayOf(
        "", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE",
        "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN",
        "SEVENTEEN", "EIGHTEEN", "NINETEEN"
    )
    private val TENS = arrayOf(
        "", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"
    )
    private val SCALES = arrayOf("", "THOUSAND", "MILLION", "BILLION")

    fun numberToWords(n: Long): String {
        if (n == 0L) return "ZERO"
        val parts = mutableListOf<String>()
        var remaining = n
        var scaleIdx = 0
        while (remaining > 0L) {
            val chunk = (remaining % 1000L).toInt()
            if (chunk > 0) {
                val chunkWords = threeDigitWords(chunk)
                val scale = SCALES.getOrElse(scaleIdx) { "" }
                parts.add(0, if (scale.isNotEmpty()) "$chunkWords $scale" else chunkWords)
            }
            remaining /= 1000L
            scaleIdx++
        }
        return parts.joinToString(" ")
    }

    private fun threeDigitWords(num: Int): String {
        val parts = mutableListOf<String>()
        val hundreds = num / 100
        val rem = num % 100
        if (hundreds > 0) {
            parts.add("${ONES[hundreds]} HUNDRED")
        }
        if (rem in 1..19) {
            parts.add(ONES[rem])
        } else if (rem >= 20) {
            val t = TENS[rem / 10]
            val o = rem % 10
            if (o > 0) parts.add("$t ${ONES[o]}") else parts.add(t)
        }
        return parts.joinToString(" ")
    }

    fun currencyToWords(amountDollars: Double): String {
        val totalCents = (amountDollars.coerceAtLeast(0.0) * 100.0).roundToLong()
        val dollars = totalCents / 100L
        val cents = totalCents % 100L

        val dollarLabel = if (dollars == 1L) "DOLLAR" else "DOLLARS"
        val dollarPhrase = "${numberToWords(dollars)} $dollarLabel"

        val centPhrase = when (cents) {
            0L -> "NO CENTS"
            1L -> "ONE CENT"
            else -> "${numberToWords(cents)} CENTS"
        }
        return "$dollarPhrase AND $centPhrase"
    }

    fun generateValidationNumber(random: Random = Random.Default): String {
        fun block(digits: Int): String = buildString {
            repeat(digits) { append(random.nextInt(0, 10)) }
        }
        return "07-${block(4)}-${block(4)}-${block(4)}-${block(4)}"
    }

    data class VoucherDetails(
        val validationNumber: String,
        val dateText: String,
        val timeText: String,
        val ticketNumber: Int,
        val amountText: String,
        val amountWords: String,
        val creditsText: String,
        val assetNumber: String
    )

    fun createDetails(
        cashBalance: Double,
        currentDenom: Double,
        ticketNumber: Int,
        now: LocalDateTime = LocalDateTime.now(),
        validationNumber: String = generateValidationNumber()
    ): VoucherDetails {
        val safeBalance = cashBalance.coerceAtLeast(0.0)
        val credits = if (currentDenom > 0.0) (safeBalance / currentDenom).roundToLong() else 0L
        return VoucherDetails(
            validationNumber = validationNumber,
            dateText = now.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
            timeText = now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            ticketNumber = ticketNumber,
            amountText = String.format(Locale.US, "$%,.2f", safeBalance),
            amountWords = currencyToWords(safeBalance),
            creditsText = "$credits credits",
            assetNumber = ASSET_NUMBER
        )
    }
}

/**
 * Pure-Kotlin Code 128B encoder returning (isBar, moduleUnits) pairs.
 */
object Code128Encoder {

    // Official Code 128 patterns (indices 0..106)
    val PATTERNS: List<IntArray> = listOf(
        intArrayOf(2,1,2,2,2,2), intArrayOf(2,2,2,1,2,2), intArrayOf(2,2,2,2,2,1), intArrayOf(1,2,1,2,2,3), intArrayOf(1,2,1,3,2,2),
        intArrayOf(1,3,1,2,2,2), intArrayOf(1,2,2,2,1,3), intArrayOf(1,2,2,3,1,2), intArrayOf(1,3,2,2,1,2), intArrayOf(2,2,1,2,1,3),
        intArrayOf(2,2,1,3,1,2), intArrayOf(2,3,1,2,1,2), intArrayOf(1,1,2,2,3,2), intArrayOf(1,2,2,1,3,2), intArrayOf(1,2,2,2,3,1),
        intArrayOf(1,1,3,2,2,2), intArrayOf(1,2,3,1,2,2), intArrayOf(1,2,3,2,2,1), intArrayOf(2,2,3,2,1,1), intArrayOf(2,2,1,1,3,2),
        intArrayOf(2,2,1,2,3,1), intArrayOf(2,1,3,2,1,2), intArrayOf(2,2,3,1,1,2), intArrayOf(3,1,2,1,3,1), intArrayOf(3,1,1,2,2,2),
        intArrayOf(3,2,1,1,2,2), intArrayOf(3,2,1,2,2,1), intArrayOf(3,1,2,2,1,2), intArrayOf(3,2,2,1,1,2), intArrayOf(3,2,2,2,1,1),
        intArrayOf(2,1,2,1,2,3), intArrayOf(2,1,2,3,2,1), intArrayOf(2,3,2,1,2,1), intArrayOf(1,1,1,3,2,3), intArrayOf(1,3,1,1,2,3),
        intArrayOf(1,3,1,3,2,1), intArrayOf(1,1,2,3,1,3), intArrayOf(1,3,2,1,1,3), intArrayOf(1,3,2,3,1,1), intArrayOf(2,1,1,3,1,3),
        intArrayOf(2,3,1,1,1,3), intArrayOf(2,3,1,3,1,1), intArrayOf(1,1,2,1,3,3), intArrayOf(1,1,2,3,3,1), intArrayOf(1,3,2,1,3,1),
        intArrayOf(1,1,3,1,2,3), intArrayOf(1,1,3,3,2,1), intArrayOf(1,3,3,1,2,1), intArrayOf(3,1,3,1,2,1), intArrayOf(2,1,1,3,3,1),
        intArrayOf(2,3,1,1,3,1), intArrayOf(2,1,3,1,1,3), intArrayOf(2,1,3,3,1,1), intArrayOf(2,1,3,1,3,1), intArrayOf(3,1,1,1,2,3),
        intArrayOf(3,1,1,3,2,1), intArrayOf(3,3,1,1,2,1), intArrayOf(3,1,2,1,1,3), intArrayOf(3,1,2,3,1,1), intArrayOf(3,3,2,1,1,1),
        intArrayOf(3,1,4,1,1,1), intArrayOf(2,2,1,4,1,1), intArrayOf(4,3,1,1,1,1), intArrayOf(1,1,1,2,2,4), intArrayOf(1,1,1,4,2,2),
        intArrayOf(1,2,1,1,2,4), intArrayOf(1,2,1,4,2,1), intArrayOf(1,4,1,1,2,2), intArrayOf(1,4,1,2,2,1), intArrayOf(1,1,2,2,1,4),
        intArrayOf(1,1,2,4,1,2), intArrayOf(1,2,2,1,1,4), intArrayOf(1,2,2,4,1,1), intArrayOf(1,4,2,1,1,2), intArrayOf(1,4,2,2,1,1),
        intArrayOf(2,4,1,2,1,1), intArrayOf(2,2,1,1,1,4), intArrayOf(4,1,3,1,1,1), intArrayOf(2,4,1,1,1,2), intArrayOf(1,3,4,1,1,1),
        intArrayOf(1,1,1,2,4,2), intArrayOf(1,2,1,1,4,2), intArrayOf(1,2,1,2,4,1), intArrayOf(1,1,4,2,1,2), intArrayOf(1,2,4,1,1,2),
        intArrayOf(1,2,4,2,1,1), intArrayOf(4,1,1,2,1,2), intArrayOf(4,2,1,1,1,2), intArrayOf(4,2,1,2,1,1), intArrayOf(2,1,2,1,4,1),
        intArrayOf(2,1,4,1,2,1), intArrayOf(4,1,2,1,2,1), intArrayOf(1,1,1,1,4,3), intArrayOf(1,1,1,3,4,1), intArrayOf(1,3,1,1,4,1),
        intArrayOf(1,1,4,1,1,3), intArrayOf(1,1,4,3,1,1), intArrayOf(4,1,1,1,1,3), intArrayOf(4,1,1,3,1,1), intArrayOf(1,1,3,1,4,1),
        intArrayOf(1,1,4,1,3,1), intArrayOf(3,1,1,1,4,1), intArrayOf(4,1,1,1,3,1), intArrayOf(2,1,1,4,1,2), intArrayOf(2,1,1,2,1,4),
        intArrayOf(2,1,1,2,3,2), intArrayOf(2,3,3,1,1,1,2)
    )

    private const val START_B = 104
    private const val STOP = 106

    fun encodeSymbolsB(text: String): List<Int> {
        val codes = mutableListOf(START_B)
        for (ch in text) {
            val code = ch.code
            val value = if (code in 32..126) code - 32 else 0 // space fallback for safety
            codes.add(value)
        }
        var checksum = codes[0]
        for (i in 1 until codes.size) {
            checksum += i * codes[i]
        }
        codes.add(checksum % 103)
        codes.add(STOP)
        return codes
    }

    fun encodeRunsB(text: String): List<Pair<Boolean, Int>> {
        val runs = mutableListOf<Pair<Boolean, Int>>()
        for (symbol in encodeSymbolsB(text)) {
            val pat = PATTERNS[symbol]
            var isBar = true
            for (units in pat) {
                runs.add(isBar to units)
                isBar = !isBar
            }
        }
        return runs
    }
}
