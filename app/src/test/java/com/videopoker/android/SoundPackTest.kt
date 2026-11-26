package com.videopoker.android

import com.videopoker.android.data.export.SoundPresetStorage
import com.videopoker.android.sound.SoundConfig
import com.videopoker.android.sound.SoundEffect
import com.videopoker.android.sound.SoundManager
import com.videopoker.android.sound.SoundPack
import com.videopoker.android.ui.state.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SoundPackTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun fromIdAndSteppingWrapCleanly() {
        assertEquals(SoundPack.CLASSIC, SoundPack.fromId(null))
        assertEquals(SoundPack.CLASSIC, SoundPack.fromId("unknown_pack"))
        assertEquals(SoundPack.NONE, SoundPack.fromId("none"))
        assertEquals(SoundPack.ACES, SoundPack.fromId("aces"))

        assertEquals(SoundPack.MEMORY, SoundPack.CLASSIC.previous())
        assertEquals(SoundPack.CLASSIC, SoundPack.MEMORY.next())
        assertEquals(SoundPack.NONE, SoundPack.CLASSIC.next())
        assertEquals(SoundPack.KNOCK, SoundPack.NONE.next())
    }

    @Test
    fun everyPackContainsAll13SoundAssetsAndNoLegacyPrefixFiles() {
        val assetsRoot = listOf(
            File("src/main/assets"),
            File("app/src/main/assets")
        ).firstOrNull { it.isDirectory }
            ?: error("Could not locate Android assets directory")

        val soundsRoot = File(assetsRoot, "sounds")
        assertTrue("Sounds folder missing: ${soundsRoot.path}", soundsRoot.isDirectory)

        val legacyFiles = soundsRoot.listFiles { f -> f.isFile && f.name.startsWith("new_") }.orEmpty()
        assertTrue("Legacy new_*.mp3 files still present: ${legacyFiles.map { it.name }}", legacyFiles.isEmpty())

        assertEquals(18, SoundPack.entries.size)
        assertEquals(13, SoundEffect.entries.size)
        assertNull(SoundEffect.fromId("option_button"))
        assertNull(SoundEffect.fromId("add_money"))
        assertNull(SoundEffect.fromId("coin_size"))

        for (pack in SoundPack.entries) {
            if (pack == SoundPack.NONE) continue
            val packDir = File(soundsRoot, pack.id)
            assertTrue("Missing pack folder: ${packDir.path}", packDir.isDirectory)
            for (effect in SoundEffect.entries) {
                val assetFile = File(packDir, effect.assetName)
                assertTrue("Missing asset ${assetFile.path}", assetFile.isFile && assetFile.length() > 0L)
            }
        }
    }

    @Test
    fun unifiedSoundConfigAndJsonRoundTrip() {
        val initial = SoundConfig()
        assertEquals(SoundPack.CLASSIC, initial.uniformPack())
        assertEquals("Classic", initial.summaryText())

        val allAces = initial.withAll(SoundPack.ACES)
        assertEquals(SoundPack.ACES, allAces.uniformPack())
        assertEquals("Aces", allAces.summaryText())
        assertEquals(SoundPack.ACES, allAces.packFor(SoundEffect.DEAL))

        val mix = allAces.withEffect(SoundEffect.WIN_ROYAL, SoundPack.FENWAY)
        assertNull(mix.uniformPack())
        assertEquals(SoundPack.ACES, mix.dominantPack())
        assertEquals("Custom mix", mix.summaryText())
        assertEquals(SoundPack.FENWAY, mix.packFor(SoundEffect.WIN_ROYAL))
        assertEquals(SoundPack.ACES, mix.packFor(SoundEffect.DEAL))

        val json = mix.toJson(pretty = true, overrideName = "Aces Royal")
        val decoded = SoundConfig.fromJson(json)
        assertEquals("Aces Royal", decoded.presetName)
        assertEquals("Aces Royal", decoded.summaryText())
        assertEquals(SoundPack.FENWAY, decoded.packFor(SoundEffect.WIN_ROYAL))
        assertEquals(SoundPack.ACES, decoded.packFor(SoundEffect.DEAL))

        val fallback = SoundConfig.fromJson("""{"sounds":{"deal":"memory","bet":"missing_pack"}}""")
        assertEquals(SoundPack.MEMORY, fallback.packFor(SoundEffect.DEAL))
        assertEquals(SoundPack.CLASSIC, fallback.packFor(SoundEffect.BET))
        assertEquals(SoundPack.MEMORY, fallback.packFor(SoundEffect.DRAW))
    }

    @Test
    fun localPresetSaveAndListRoundTrip() {
        val dir = tempFolder.newFolder("sound_presets")
        val cfg = SoundConfig().withAll(SoundPack.FAITH).withEffect(SoundEffect.DEAL, SoundPack.TEXAS)

        val saved = SoundPresetStorage.saveLocalPreset(dir, "My Faith Mix!", cfg).getOrThrow()
        assertEquals("My_Faith_Mix.json", saved.filename)
        assertEquals("My Faith Mix!", saved.name)

        val listed = SoundPresetStorage.listLocalPresets(dir)
        assertEquals(1, listed.size)
        assertEquals("My Faith Mix!", listed[0].name)
        assertEquals(SoundPack.TEXAS, listed[0].config.packFor(SoundEffect.DEAL))
        assertEquals(SoundPack.FAITH, listed[0].config.packFor(SoundEffect.WIN_MEDIUM))

        val bad = SoundPresetStorage.saveLocalPreset(dir, "   ", cfg)
        assertFalse(bad.isSuccess)
    }

    @Test
    fun viewModelSoundPackAndStepperTransitions() {
        val vm = GameViewModel()
        vm.onSingleSoundPackSelect(SoundPack.ACES)
        assertEquals(SoundPack.ACES, vm.uiState.value.soundConfig.uniformPack())

        vm.onPerSoundPackSelect(SoundEffect.DEAL, SoundPack.MEMORY)
        assertNull(vm.uiState.value.soundConfig.uniformPack())
        assertEquals(SoundPack.MEMORY, vm.uiState.value.soundConfig.packFor(SoundEffect.DEAL))
        assertEquals(SoundPack.ACES, vm.uiState.value.soundConfig.packFor(SoundEffect.BET))

        vm.onSingleSoundPackSelect(SoundPack.CLASSIC)
        assertEquals(SoundPack.CLASSIC, vm.uiState.value.soundConfig.uniformPack())

        vm.onSingleSoundPackSelect(SoundPack.NONE)
        assertEquals(SoundPack.NONE, vm.uiState.value.soundConfig.uniformPack())
        assertEquals("None", vm.uiState.value.soundConfig.summaryText())

        vm.onPerSoundPackSelect(SoundEffect.NO_WIN, SoundPack.NONE)
        vm.onPerSoundPackSelect(SoundEffect.DEAL, SoundPack.ACES)
        assertEquals(SoundPack.NONE, vm.uiState.value.soundConfig.packFor(SoundEffect.NO_WIN))
        assertEquals(SoundPack.ACES, vm.uiState.value.soundConfig.packFor(SoundEffect.DEAL))
    }

    @Test
    fun soundMixGainsKeepEverydaySoundsQuieterThanFanfare() {
        assertTrue(SoundManager.mixGainFor(SoundEffect.DRAW) <= SoundManager.mixGainFor(SoundEffect.DEAL))
        assertTrue(SoundManager.mixGainFor(SoundEffect.DEAL) <= SoundManager.mixGainFor(SoundEffect.HOLD))
        assertTrue(SoundManager.mixGainFor(SoundEffect.HOLD) < SoundManager.mixGainFor(SoundEffect.NO_WIN))
        assertTrue(SoundManager.mixGainFor(SoundEffect.NO_WIN) < SoundManager.mixGainFor(SoundEffect.MADE_HAND))
        assertTrue(SoundManager.mixGainFor(SoundEffect.MADE_HAND) < SoundManager.mixGainFor(SoundEffect.WIN_SMALL))
        assertTrue(SoundManager.mixGainFor(SoundEffect.WIN_SMALL) < SoundManager.mixGainFor(SoundEffect.WIN_MEDIUM))
        assertTrue(SoundManager.mixGainFor(SoundEffect.WIN_MEDIUM) < SoundManager.mixGainFor(SoundEffect.CASH_OUT))
        assertTrue(SoundManager.mixGainFor(SoundEffect.CASH_OUT) < SoundManager.mixGainFor(SoundEffect.WIN_BIG))
        assertEquals(1.0f, SoundManager.mixGainFor(SoundEffect.WIN_ROYAL), 0.0001f)
    }
}
