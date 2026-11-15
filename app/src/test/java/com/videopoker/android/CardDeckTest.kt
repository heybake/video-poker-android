package com.videopoker.android

import com.videopoker.android.ui.theme.CardDeck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
}
