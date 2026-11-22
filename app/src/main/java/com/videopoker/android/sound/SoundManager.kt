package com.videopoker.android.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.util.Log
import com.videopoker.android.data.models.HandRank

enum class SoundEffect(val assetName: String) {
    DEAL("new_deal.mp3"),
    DRAW("new_draw.mp3"),
    HOLD("new_hold.mp3"),
    RELEASE("new_release.mp3"),
    BET("new_bet.mp3"),
    WIN_SMALL("new_win_small.mp3"),
    WIN_MEDIUM("new_win_medium.mp3"),
    WIN_BIG("new_win_big.mp3"),
    WIN_ROYAL("new_win_royal.mp3"),
    NO_WIN("new_no_win.mp3"),
    COUNT_TICK("new_count_tick.mp3"),
    CASH_OUT("new_cash_out.mp3"),
    MADE_HAND("new_made_hand.mp3")
}

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<SoundEffect, Int>()
    private val durationMsMap = mutableMapOf<SoundEffect, Long>()
    private val activeStreams = mutableListOf<Int>()
    private var volume: Float = 1.0f
    private var isLoaded = false

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

        preloadSounds()
    }

    private fun preloadSounds() {
        try {
            val retriever = MediaMetadataRetriever()
            try {
                for (effect in SoundEffect.values()) {
                    context.assets.openFd("sounds/${effect.assetName}").use { afd ->
                        soundMap[effect] = soundPool.load(afd, 1)
                        try {
                            retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            val dur = retriever.extractMetadata(
                                MediaMetadataRetriever.METADATA_KEY_DURATION
                            )?.toLongOrNull()
                            if (dur != null && dur > 0L) {
                                durationMsMap[effect] = dur
                            }
                        } catch (_: Exception) {
                            // Fall back to default duration estimates when metadata cannot be read
                        }
                    }
                }
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
            isLoaded = true
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to preload sound effects: ${e.message}")
        }
    }

    fun setVolume(vol: Float) {
        this.volume = vol.coerceIn(0f, 1f)
    }

    fun play(effect: SoundEffect) {
        playInternal(effect)
    }

    /**
     * Play [effect] and return its expected playback duration in milliseconds so callers
     * can wait until the entire clip finishes before advancing.
     */
    fun playWithDuration(effect: SoundEffect): Long {
        playInternal(effect)
        return durationMsMap[effect] ?: defaultDurationMs(effect)
    }

    private fun playInternal(effect: SoundEffect) {
        if (volume <= 0.01f) return
        val soundId = soundMap[effect] ?: return
        try {
            // Perceptual audio curve: square root taper ensures 50% slider is clearly and comfortably audible (~71% signal level)
            val perceivedVol = Math.sqrt(volume.toDouble().coerceIn(0.0, 1.0)).toFloat()
            val streamId = soundPool.play(soundId, perceivedVol, perceivedVol, 1, 0, 1.0f)
            if (streamId != 0) {
                synchronized(activeStreams) {
                    if (activeStreams.size > 24) activeStreams.removeAt(0)
                    activeStreams.add(streamId)
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing sound: ${effect.assetName}", e)
        }
    }

    fun stopAll() {
        val toStop = synchronized(activeStreams) {
            activeStreams.toList().also { activeStreams.clear() }
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
        SoundEffect.WIN_SMALL -> 650L
        SoundEffect.WIN_MEDIUM -> 1050L
        SoundEffect.WIN_BIG -> 1500L
        SoundEffect.WIN_ROYAL -> 3100L
        SoundEffect.DRAW -> 150L
        SoundEffect.DEAL -> 200L
        SoundEffect.MADE_HAND -> 500L
        SoundEffect.COUNT_TICK -> 150L
        SoundEffect.NO_WIN -> 400L
        else -> 200L
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
        soundPool.release()
    }
}
