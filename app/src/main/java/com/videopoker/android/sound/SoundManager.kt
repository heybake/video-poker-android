package com.videopoker.android.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.util.Log
import com.videopoker.android.data.models.HandRank
import kotlin.math.sqrt

class SoundManager(private val context: Context) {

    private data class SoundKey(val pack: SoundPack, val effect: SoundEffect)

    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<SoundKey, Int>()
    private val readyIds = mutableSetOf<Int>()
    private val pendingPlay = mutableMapOf<Int, Boolean>()
    private val durationMsMap = mutableMapOf<SoundKey, Long>()
    private val sampleMixGain = mutableMapOf<Int, Float>()
    private val streamMixGain = mutableMapOf<Int, Float>()
    private val activeStreams = mutableListOf<Int>()
    private var previewStreamId: Int? = null
    private var currentConfig: SoundConfig = SoundConfig()
    private var volume: Float = 1.0f

    init {
        // Boost device media stream volume so playback is never choked
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Could not set stream volume: ${e.message}")
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) {
                synchronized(this) { pendingPlay.remove(sampleId) }
                return@setOnLoadCompleteListener
            }
            val isPreview = synchronized(this) {
                readyIds.add(sampleId)
                pendingPlay.remove(sampleId)
            }
            if (isPreview != null) {
                playLoadedSample(sampleId, isPreview)
            }
        }

        preloadActiveSounds()
    }

    fun applyConfig(config: SoundConfig) {
        if (currentConfig == config && soundMap.isNotEmpty()) return
        currentConfig = config
        preloadActiveSounds()
    }

    private fun activeKeys(): Set<SoundKey> =
        SoundEffect.entries
            .asSequence()
            .map { effect -> SoundKey(currentConfig.packFor(effect), effect) }
            .filter { it.pack != SoundPack.NONE }
            .toCollection(linkedSetOf())

    private fun preloadActiveSounds() {
        val needed = activeKeys()
        val staleKeys = synchronized(this) {
            soundMap.keys.filter { it !in needed }
        }
        for (stale in staleKeys) {
            unloadKey(stale)
        }
        val missing = synchronized(this) {
            needed.filter { it !in soundMap }
        }
        if (missing.isEmpty()) return

        try {
            val retriever = MediaMetadataRetriever()
            try {
                for (key in missing) {
                    loadKey(key, retriever)
                }
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to preload sound effects: ${e.message}")
        }
    }

    private fun loadKey(key: SoundKey, retriever: MediaMetadataRetriever? = null): Int? {
        if (key.pack == SoundPack.NONE) return null
        return try {
            val path = SoundConfig.assetPathFor(key.pack, key.effect)
            context.assets.openFd(path).use { afd ->
                val soundId = soundPool.load(afd, 1)
                synchronized(this) {
                    soundMap[key] = soundId
                    sampleMixGain[soundId] = mixGainFor(key.effect)
                }
                if (retriever != null) {
                    try {
                        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        val dur = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                        )?.toLongOrNull()
                        if (dur != null && dur > 0L) {
                            durationMsMap[key] = dur
                        }
                    } catch (_: Exception) {
                        // Fall back to default duration estimates when metadata cannot be read
                    }
                }
                soundId
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to load ${key.pack.id}/${key.effect.assetName}: ${e.message}")
            null
        }
    }

    private fun unloadKey(key: SoundKey) {
        val soundId = synchronized(this) {
            val id = soundMap.remove(key)
            if (id != null) {
                readyIds.remove(id)
                pendingPlay.remove(id)
                sampleMixGain.remove(id)
            }
            id
        }
        durationMsMap.remove(key)
        if (soundId != null) {
            try {
                soundPool.unload(soundId)
            } catch (_: Exception) {
            }
        }
    }

    private fun trimPreviewCacheIfNeeded(keep: SoundKey) {
        val needed = activeKeys()
        val extras = synchronized(this) {
            if (soundMap.size <= 32) emptyList()
            else soundMap.keys.filter { it !in needed && it != keep }
        }
        for (extra in extras.take((soundMap.size - 26).coerceAtLeast(0))) {
            unloadKey(extra)
        }
    }

    private fun perceivedVolume(vol: Float): Float {
        val clamped = vol.coerceIn(0f, 1f)
        return if (clamped <= 0.01f) 0f else clamped * clamped
    }

    fun setVolume(vol: Float) {
        this.volume = vol.coerceIn(0f, 1f)
        val perceived = perceivedVolume(this.volume)
        val streams = synchronized(activeStreams) {
            activeStreams.map { id -> id to (streamMixGain[id] ?: 1.0f) }
        }
        for ((streamId, mix) in streams) {
            val scaled = perceived * mix
            try {
                soundPool.setVolume(streamId, scaled, scaled)
            } catch (_: Exception) {
            }
        }
    }

    fun play(effect: SoundEffect) {
        playKey(SoundKey(currentConfig.packFor(effect), effect), isPreview = false)
    }

    fun playPreview(pack: SoundPack, effect: SoundEffect) {
        stopPreview()
        val key = SoundKey(pack, effect)
        trimPreviewCacheIfNeeded(key)
        playKey(key, isPreview = true)
    }

    /**
     * Play [effect] and return its expected playback duration in milliseconds so callers
     * can wait until the entire clip finishes before advancing.
     */
    fun playWithDuration(effect: SoundEffect): Long {
        val key = SoundKey(currentConfig.packFor(effect), effect)
        playKey(key, isPreview = false)
        return durationMsMap[key] ?: defaultDurationMs(effect)
    }

    private fun playKey(key: SoundKey, isPreview: Boolean) {
        if (key.pack == SoundPack.NONE || volume <= 0.01f) {
            if (isPreview) stopPreview()
            return
        }
        val existingId = synchronized(this) { soundMap[key] }
        if (existingId != null) {
            val ready = synchronized(this) {
                if (existingId in readyIds) true
                else {
                    pendingPlay[existingId] = isPreview
                    false
                }
            }
            if (ready) {
                playLoadedSample(existingId, isPreview)
            }
            return
        }
        val loadedId = loadKey(key) ?: return
        val ready = synchronized(this) {
            if (loadedId in readyIds) true
            else {
                pendingPlay[loadedId] = isPreview
                false
            }
        }
        if (ready) {
            playLoadedSample(loadedId, isPreview)
        }
    }

    private fun playLoadedSample(soundId: Int, isPreview: Boolean) {
        if (volume <= 0.01f) return
        try {
            // Quadratic perceptual curve provides smooth audible attenuation across the full slider range
            val mix = synchronized(this) { sampleMixGain[soundId] ?: 1.0f }
            val perceivedVol = perceivedVolume(volume) * mix
            val streamId = soundPool.play(soundId, perceivedVol, perceivedVol, 1, 0, 1.0f)
            if (streamId != 0) {
                if (isPreview) {
                    previewStreamId = streamId
                }
                synchronized(activeStreams) {
                    if (activeStreams.size > 24) {
                        val dropped = activeStreams.removeAt(0)
                        streamMixGain.remove(dropped)
                    }
                    activeStreams.add(streamId)
                    streamMixGain[streamId] = mix
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing soundId=$soundId", e)
        }
    }

    fun stopPreview() {
        val streamId = previewStreamId ?: return
        previewStreamId = null
        try {
            soundPool.stop(streamId)
        } catch (_: Exception) {
        }
    }

    fun stopAll() {
        stopPreview()
        synchronized(this) {
            pendingPlay.clear()
        }
        val toStop = synchronized(activeStreams) {
            activeStreams.toList().also {
                activeStreams.clear()
                streamMixGain.clear()
            }
        }
        for (streamId in toStop) {
            try {
                soundPool.stop(streamId)
            } catch (_: Exception) {
            }
        }
    }

    fun winEffectForLine(rank: HandRank, perHandWinCredits: Int, betPerHand: Int): SoundEffect? =
        Companion.winEffectForLine(rank, perHandWinCredits, betPerHand)

    companion object {
        fun mixGainFor(effect: SoundEffect): Float = when (effect) {
            SoundEffect.RELEASE -> 0.22f
            SoundEffect.HOLD -> 0.25f
            SoundEffect.DRAW -> 0.32f
            SoundEffect.DEAL, SoundEffect.BET -> 0.35f
            SoundEffect.COUNT_TICK, SoundEffect.NO_WIN -> 0.45f
            SoundEffect.MADE_HAND -> 0.72f
            SoundEffect.WIN_SMALL -> 0.80f
            SoundEffect.WIN_MEDIUM -> 0.90f
            SoundEffect.CASH_OUT -> 0.95f
            SoundEffect.WIN_BIG, SoundEffect.WIN_ROYAL -> 1.00f
        }

        fun winEffectForLine(rank: HandRank, perHandWinCredits: Int, betPerHand: Int): SoundEffect? {
            if (perHandWinCredits <= 0) return null
            if (rank == HandRank.ROYAL_FLUSH) return SoundEffect.WIN_ROYAL
            val ratio = perHandWinCredits.toDouble() / betPerHand.coerceAtLeast(1)
            return when {
                ratio >= 8.0 -> SoundEffect.WIN_BIG
                ratio >= 3.0 -> SoundEffect.WIN_MEDIUM
                else -> SoundEffect.WIN_SMALL
            }
        }
    }

    private fun defaultDurationMs(effect: SoundEffect): Long = when (effect) {
        SoundEffect.WIN_SMALL -> 1200L
        SoundEffect.WIN_MEDIUM -> 2000L
        SoundEffect.WIN_BIG -> 2900L
        SoundEffect.WIN_ROYAL -> 4700L
        SoundEffect.DRAW -> 500L
        SoundEffect.DEAL -> 950L
        SoundEffect.MADE_HAND -> 850L
        SoundEffect.COUNT_TICK -> 120L
        SoundEffect.NO_WIN -> 900L
        SoundEffect.CASH_OUT -> 3250L
        else -> 220L
    }

    fun playWinByPayout(coinsWon: Int) {
        when {
            coinsWon <= 0 -> play(SoundEffect.NO_WIN)
            coinsWon < 10 -> play(SoundEffect.WIN_SMALL)
            coinsWon < 50 -> play(SoundEffect.WIN_MEDIUM)
            coinsWon < 800 -> play(SoundEffect.WIN_BIG)
            else -> play(SoundEffect.WIN_ROYAL)
        }
    }

    fun release() {
        stopAll()
        soundPool.release()
    }
}
