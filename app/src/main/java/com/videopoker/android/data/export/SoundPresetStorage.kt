package com.videopoker.android.data.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.gson.JsonParser
import com.videopoker.android.sound.SoundConfig
import java.io.File
import java.io.FileOutputStream

data class SavedSoundPreset(
    val filename: String,
    val name: String,
    val config: SoundConfig
)

/**
 * Saves, lists, exports, shares, and imports portable `vp_sound_preset` JSON files
 * compatible with `VP_Player`'s `Notebook/Options/Sounds/` format.
 */
object SoundPresetStorage {

    private val UNSAFE_FILENAME = Regex("[^A-Za-z0-9_-]+")

    fun sanitizeFilename(name: String): String {
        val stem = UNSAFE_FILENAME.replace(name.trim(), "_").trim('_').ifBlank { "vp_sounds" }
        return "$stem.json"
    }

    fun saveLocalPreset(
        presetsDir: File,
        name: String,
        config: SoundConfig
    ): Result<SavedSoundPreset> = runCatching {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Enter a preset name before saving." }
        val filename = sanitizeFilename(cleanName)
        presetsDir.mkdirs()
        val storedConfig = config.copy(presetName = cleanName)
        val jsonText = storedConfig.toJson(pretty = true, overrideName = cleanName)
        File(presetsDir, filename).writeText(jsonText, Charsets.UTF_8)
        SavedSoundPreset(filename = filename, name = cleanName, config = storedConfig)
    }

    fun listLocalPresets(presetsDir: File): List<SavedSoundPreset> {
        if (!presetsDir.isDirectory) return emptyList()
        val files = presetsDir.listFiles { f -> f.isFile && f.extension.equals("json", ignoreCase = true) }
            ?: return emptyList()
        return files.sortedBy { it.name.lowercase() }.mapNotNull { file ->
            try {
                val raw = file.readText(Charsets.UTF_8)
                val obj = JsonParser.parseString(raw).asJsonObject
                if (obj.get("sounds")?.isJsonObject != true && obj.get("map")?.isJsonObject != true) {
                    return@mapNotNull null
                }
                val fallback = file.nameWithoutExtension
                val cfg = SoundConfig.fromJson(raw, fallbackName = fallback)
                val label = cfg.presetName.ifBlank { fallback }
                SavedSoundPreset(filename = file.name, name = label, config = cfg.copy(presetName = label))
            } catch (_: Exception) {
                null
            }
        }
    }

    fun exportToDownloads(
        context: Context,
        name: String,
        config: SoundConfig
    ): Result<String> = runCatching {
        val cleanName = name.trim().ifBlank { config.summaryText() }
        val filename = sanitizeFilename(cleanName)
        val jsonBytes = config.toJson(pretty = true, overrideName = cleanName).toByteArray(Charsets.UTF_8)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/VideoPokerOptions/Sounds")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create Downloads entry for $filename")
            resolver.openOutputStream(uri)?.use { out ->
                out.write(jsonBytes)
            } ?: error("Unable to write Downloads stream for $filename")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Saved to Downloads/VideoPokerOptions/Sounds/$filename"
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "VideoPokerOptions/Sounds"
            ).apply { mkdirs() }
            val outFile = File(dir, filename)
            FileOutputStream(outFile).use { out -> out.write(jsonBytes) }
            "Saved to Downloads/VideoPokerOptions/Sounds/$filename"
        }
    }

    fun sharePresetJson(
        context: Context,
        name: String,
        config: SoundConfig
    ): Result<Unit> = runCatching {
        val cleanName = name.trim().ifBlank { config.summaryText() }
        val filename = sanitizeFilename(cleanName)
        val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
        val outFile = File(exportDir, filename)
        outFile.writeText(config.toJson(pretty = true, overrideName = cleanName), Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Video Poker Sound Preset: $cleanName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Sound Preset").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun importFromUri(context: Context, uri: Uri): Result<SavedSoundPreset> = runCatching {
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("Could not read selected file.")
        val obj = JsonParser.parseString(raw).asJsonObject
        require(obj.get("sounds")?.isJsonObject == true || obj.get("map")?.isJsonObject == true) {
            "File is not a valid VP Sounds preset."
        }
        val cfg = SoundConfig.fromJson(raw, fallbackName = "Imported Mix")
        val label = cfg.presetName.ifBlank { "Imported Mix" }
        SavedSoundPreset(
            filename = sanitizeFilename(label),
            name = label,
            config = cfg.copy(presetName = label)
        )
    }
}
