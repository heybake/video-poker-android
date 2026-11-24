package com.videopoker.android

import com.videopoker.android.data.export.SoundPresetStorage
import com.videopoker.android.sound.SoundConfig
import com.videopoker.android.sound.SoundEffect
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
        assertEquals(SoundPack.BANJO, SoundPack.fromId("banjo"))

        assertEquals(SoundPack.HELICOPTER, SoundPack.CLASSIC.previous())
        assertEquals(SoundPack.CLASSIC, SoundPack.HELICOPTER.next())
        assertEquals(SoundPack.DRUMS, SoundPack.CLASSIC.next())
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

        assertEquals(16, SoundPack.entries.size)
        assertEquals(13, SoundEffect.entries.size)

        for (pack in SoundPack.entries) {
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
        assertEquals("Classic casino", initial.summaryText())

        val allBanjo = initial.withAll(SoundPack.BANJO)
        assertEquals(SoundPack.BANJO, allBanjo.uniformPack())
        assertEquals("Banjo", allBanjo.summaryText())
        assertEquals(SoundPack.BANJO, allBanjo.packFor(SoundEffect.DEAL))

        val mix = allBanjo.withEffect(SoundEffect.WIN_ROYAL, SoundPack.CHOIR)
        assertNull(mix.uniformPack())
        assertEquals(SoundPack.BANJO, mix.dominantPack())
        assertEquals("Custom mix", mix.summaryText())
        assertEquals(SoundPack.CHOIR, mix.packFor(SoundEffect.WIN_ROYAL))
        assertEquals(SoundPack.BANJO, mix.packFor(SoundEffect.DEAL))

        val json = mix.toJson(pretty = true, overrideName = "Banjo Royal")
        val decoded = SoundConfig.fromJson(json)
        assertEquals("Banjo Royal", decoded.presetName)
        assertEquals("Banjo Royal", decoded.summaryText())
        assertEquals(SoundPack.CHOIR, decoded.packFor(SoundEffect.WIN_ROYAL))
        assertEquals(SoundPack.BANJO, decoded.packFor(SoundEffect.DEAL))

        val fallback = SoundConfig.fromJson("""{"sounds":{"deal":"helicopter","bet":"missing_pack"}}""")
        assertEquals(SoundPack.HELICOPTER, fallback.packFor(SoundEffect.DEAL))
        assertEquals(SoundPack.CLASSIC, fallback.packFor(SoundEffect.BET))
    }

    @Test
    fun localPresetSaveAndListRoundTrip() {
        val dir = tempFolder.newFolder("sound_presets")
        val cfg = SoundConfig().withAll(SoundPack.PIANO).withEffect(SoundEffect.DEAL, SoundPack.DRUMS)

        val saved = SoundPresetStorage.saveLocalPreset(dir, "My Piano Mix!", cfg).getOrThrow()
        assertEquals("My_Piano_Mix.json", saved.filename)
        assertEquals("My Piano Mix!", saved.name)

        val listed = SoundPresetStorage.listLocalPresets(dir)
        assertEquals(1, listed.size)
        assertEquals("My Piano Mix!", listed[0].name)
        assertEquals(SoundPack.DRUMS, listed[0].config.packFor(SoundEffect.DEAL))
        assertEquals(SoundPack.PIANO, listed[0].config.packFor(SoundEffect.WIN_MEDIUM))

        val bad = SoundPresetStorage.saveLocalPreset(dir, "   ", cfg)
        assertFalse(bad.isSuccess)
    }

    @Test
    fun viewModelSoundPackAndStepperTransitions() {
        val vm = GameViewModel()
        vm.onSingleSoundPackSelect(SoundPack.BANJO)
        assertEquals(SoundPack.BANJO, vm.uiState.value.soundConfig.uniformPack())

        vm.onPerSoundPackSelect(SoundEffect.DEAL, SoundPack.HELICOPTER)
        assertNull(vm.uiState.value.soundConfig.uniformPack())
        assertEquals(SoundPack.HELICOPTER, vm.uiState.value.soundConfig.packFor(SoundEffect.DEAL))
        assertEquals(SoundPack.BANJO, vm.uiState.value.soundConfig.packFor(SoundEffect.BET))

        vm.onSingleSoundPackSelect(SoundPack.CLASSIC)
        assertEquals(SoundPack.CLASSIC, vm.uiState.value.soundConfig.uniformPack())
    }
}
