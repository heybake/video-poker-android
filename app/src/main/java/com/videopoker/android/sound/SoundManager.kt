package com.videopoker.android.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.util.Log

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
            for (effect in SoundEffect.values()) {
                val assetDescriptor = context.assets.openFd("sounds/${effect.assetName}")
                val soundId = soundPool.load(assetDescriptor, 1)
                soundMap[effect] = soundId
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
        if (volume <= 0.01f) return
        val soundId = soundMap[effect] ?: return
        try {
            // Perceptual audio curve: square root taper ensures 50% slider is clearly and comfortably audible (~71% signal level)
            val perceivedVol = Math.sqrt(volume.toDouble().coerceIn(0.0, 1.0)).toFloat()
            soundPool.play(soundId, perceivedVol, perceivedVol, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing sound: ${effect.assetName}", e)
        }
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
