package com.videopoker.android.data.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Exports recorded Workbench session folders as `.zip` archives to the device's Downloads folder
 * or via the system share sheet (`FileProvider`).
 */
object SessionExporter {

    fun saveZipToDownloads(context: Context, sessionDir: File): Result<String> = runCatching {
        val zipName = "${sessionDir.name}.zip"
        val tempZip = File(File(context.cacheDir, "shared_exports").apply { mkdirs() }, zipName)
        PlayedGameLog.zipSessionFolder(sessionDir, tempZip)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, zipName)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/VideoPokerSessions")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create Downloads entry for $zipName")
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(tempZip).use { input -> input.copyTo(out) }
            } ?: error("Unable to write Downloads stream for $zipName")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Saved to Downloads/VideoPokerSessions/$zipName"
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "VideoPokerSessions"
            ).apply { mkdirs() }
            val outFile = File(downloadsDir, zipName)
            FileInputStream(tempZip).use { input ->
                FileOutputStream(outFile).use { out -> input.copyTo(out) }
            }
            "Saved to Downloads/VideoPokerSessions/$zipName"
        }
    }

    fun shareSessionZip(context: Context, sessionDir: File): Result<Unit> = runCatching {
        val zipName = "${sessionDir.name}.zip"
        val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
        val zipFile = File(exportDir, zipName)
        PlayedGameLog.zipSessionFolder(sessionDir, zipFile)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Video Poker Session: ${sessionDir.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Export Recorded Session").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
