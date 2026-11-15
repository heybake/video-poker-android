package com.videopoker.android

import com.videopoker.android.ui.components.Code128Encoder
import com.videopoker.android.ui.components.TitoVoucherFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TitoVoucherFormatterTest {

    @Test
    fun code128BEncodesAndDecodesRoyalFlushNotFound() {
        val text = TitoVoucherFormatter.BARCODE_EASTER_EGG_TEXT
        val symbols = Code128Encoder.encodeSymbolsB(text)

        assertEquals(104, symbols.first())
        assertEquals(106, symbols.last())
        assertEquals(66, symbols[symbols.size - 2]) // Verified weighted mod-103 checksum

        val runs = Code128Encoder.encodeRunsB(text)
        assertEquals(266, runs.sumOf { it.second })

        // Verify exact pattern round-trip back to characters
        val reverseMap = Code128Encoder.PATTERNS
            .take(106)
            .mapIndexed { idx, arr -> arr.toList() to idx }
            .toMap()

        val decodedSymbols = mutableListOf<Int>()
        var idx = 0
        while (idx + 7 < runs.size) {
            val chunk = runs.subList(idx, idx + 6).map { it.second }
            decodedSymbols.add(reverseMap[chunk] ?: error("Invalid Code128 chunk: $chunk"))
            idx += 6
        }
        assertEquals(Code128Encoder.PATTERNS[106].toList(), runs.subList(idx, runs.size).map { it.second })
        val decodedText = decodedSymbols
            .drop(1) // Start B
            .dropLast(1) // Checksum
            .map { (it + 32).toChar() }
            .joinToString("")

        assertEquals("ROYAL FLUSH NOT FOUND", decodedText)
    }

    @Test
    fun currencyToWordsFormatsCentsAndDollarsCleanly() {
        assertEquals("ZERO DOLLARS AND THIRTEEN CENTS", TitoVoucherFormatter.currencyToWords(0.13))
        assertEquals("ZERO DOLLARS AND NO CENTS", TitoVoucherFormatter.currencyToWords(0.00))
        assertEquals("ONE DOLLAR AND ONE CENT", TitoVoucherFormatter.currencyToWords(1.01))
        assertEquals("ONE HUNDRED EIGHTY FIVE DOLLARS AND NO CENTS", TitoVoucherFormatter.currencyToWords(185.00))
        assertEquals("ONE THOUSAND FIVE HUNDRED FIFTY DOLLARS AND NINETY NINE CENTS", TitoVoucherFormatter.currencyToWords(1550.99))
    }

    @Test
    fun createDetailsPopulatesExpectedFields() {
        val now = LocalDateTime.of(2026, 11, 14, 23, 5, 30)
        val details = TitoVoucherFormatter.createDetails(
            cashBalance = 0.13,
            currentDenom = 0.01,
            ticketNumber = 3914,
            now = now,
            validationNumber = "07-4931-8205-9764-1183"
        )
        assertEquals("$0.13", details.amountText)
        assertEquals("ZERO DOLLARS AND THIRTEEN CENTS", details.amountWords)
        assertEquals("11/14/2026", details.dateText)
        assertEquals("23:05:30", details.timeText)
        assertEquals(3914, details.ticketNumber)
        assertEquals("07-4931-8205-9764-1183", details.validationNumber)
        assertTrue(details.creditsText.contains("13"))
    }
}
