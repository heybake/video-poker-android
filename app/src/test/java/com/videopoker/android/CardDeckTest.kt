package com.videopoker.android

import com.videopoker.android.ui.theme.CardDeck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

class CardDeckTest {

    @Test
    fun fromIdResolvesKnownAndFallsBackToModern() {
        assertEquals(CardDeck.MODERN, CardDeck.fromId("modern"))
        assertEquals(CardDeck.VINTAGE_CREST, CardDeck.fromId("vintage_crest"))
        assertEquals(CardDeck.MODERN, CardDeck.fromId(null))
        assertEquals(CardDeck.MODERN, CardDeck.fromId("unknown_deck"))
    }

    @Test
    fun everyDeckContainsAll53CardAssets() {
        val ranks = listOf("2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A")
        val suits = listOf("c", "d", "h", "s")
        val expectedFiles = listOf("back.png") + ranks.flatMap { r -> suits.map { s -> "$r$s.png" } }

        val assetsRoot = listOf(
            File("src/main/assets"),
            File("app/src/main/assets")
        ).firstOrNull { it.isDirectory }
            ?: error("Could not locate Android assets directory")

        for (deck in CardDeck.entries) {
            val deckDir = File(assetsRoot, deck.assetFolder)
            assertTrue("Deck folder missing: ${deckDir.path}", deckDir.isDirectory)
            for (fileName in expectedFiles) {
                val assetFile = File(deckDir, fileName)
                assertTrue("Missing asset ${assetFile.path}", assetFile.isFile && assetFile.length() > 0L)
            }
        }
    }

    @Test
    fun vintageCrestRedKingsMatchEachOtherAndDifferFromBlackKings() {
        val assetsRoot = listOf(
            File("src/main/assets"),
            File("app/src/main/assets")
        ).firstOrNull { it.isDirectory }
            ?: error("Could not locate Android assets directory")

        val classicDir = File(assetsRoot, CardDeck.VINTAGE_CREST.assetFolder)
        val kc = ImageIO.read(File(classicDir, "Kc.png"))
        val ks = ImageIO.read(File(classicDir, "Ks.png"))
        val kh = ImageIO.read(File(classicDir, "Kh.png"))
        val kd = ImageIO.read(File(classicDir, "Kd.png"))

        assertEquals(450, kh.width)
        assertEquals(630, kh.height)
        assertEquals(450, kd.width)
        assertEquals(630, kd.height)

        fun portraitDiffCount(a: java.awt.image.BufferedImage, b: java.awt.image.BufferedImage): Int {
            var changed = 0
            for (y in 25 until 302) {
                for (x in 162 until 432) {
                    val pa = a.getRGB(x, y)
                    val pb = b.getRGB(x, y)
                    val dr = abs(((pa ushr 16) and 0xFF) - ((pb ushr 16) and 0xFF))
                    val dg = abs(((pa ushr 8) and 0xFF) - ((pb ushr 8) and 0xFF))
                    val db = abs((pa and 0xFF) - (pb and 0xFF))
                    if (maxOf(dr, dg, db) > 5) changed++
                }
            }
            return changed
        }

        // Red Kings match each other, Black Kings match each other, Red & Black differ substantially
        assertEquals(0, portraitDiffCount(kc, ks))
        assertEquals(0, portraitDiffCount(kh, kd))
        assertTrue(
            "Expected red and black Vintage Crest King portraits to differ cleanly",
            portraitDiffCount(kc, kh) > 20_000
        )
    }

    @Test
    fun modernCrestCardsAndBackHaveStandardSizeAndSuitDifferentiation() {
        val assetsRoot = listOf(
            File("src/main/assets"),
            File("app/src/main/assets")
        ).firstOrNull { it.isDirectory }
            ?: error("Could not locate Android assets directory")

        val modernDir = File(assetsRoot, CardDeck.MODERN.assetFolder)
        val back = ImageIO.read(File(modernDir, "back.png"))
        assertEquals(450, back.width)
        assertEquals(630, back.height)

        fun artBayDiffCount(a: java.awt.image.BufferedImage, b: java.awt.image.BufferedImage): Int {
            var changed = 0
            for (y in 20 until 326) {
                for (x in 140 until 430) {
                    val pa = a.getRGB(x, y)
                    val pb = b.getRGB(x, y)
                    val dr = abs(((pa ushr 16) and 0xFF) - ((pb ushr 16) and 0xFF))
                    val dg = abs(((pa ushr 8) and 0xFF) - ((pb ushr 8) and 0xFF))
                    val db = abs((pa and 0xFF) - (pb and 0xFF))
                    if (maxOf(dr, dg, db) > 5) changed++
                }
            }
            return changed
        }

        for (rank in listOf("A", "K", "Q", "J")) {
            val club = ImageIO.read(File(modernDir, "${rank}c.png"))
            val spade = ImageIO.read(File(modernDir, "${rank}s.png"))
            val heart = ImageIO.read(File(modernDir, "${rank}h.png"))
            val diamond = ImageIO.read(File(modernDir, "${rank}d.png"))
            for (img in listOf(club, spade, heart, diamond)) {
                assertEquals(450, img.width)
                assertEquals(630, img.height)
            }
            assertEquals(0, artBayDiffCount(club, spade))
            assertEquals(0, artBayDiffCount(heart, diamond))
            assertTrue(
                "Expected red and black Modern Crest $rank artwork to differ cleanly",
                artBayDiffCount(club, heart) > 5_000
            )
        }
    }
}
