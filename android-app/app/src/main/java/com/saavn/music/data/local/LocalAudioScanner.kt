package com.saavn.music.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.saavn.music.data.model.YouTubeSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAudioScanner(private val context: Context) {

    suspend fun scanDeviceAudioFiles(): List<YouTubeSong> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<YouTubeSong>()
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val title = c.getString(titleColumn) ?: "Unknown Track"
                    val artist = c.getString(artistColumn) ?: "Local Artist"
                    val durationMs = c.getLong(durationColumn)
                    val albumId = c.getLong(albumIdColumn)

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    // Use the App Logo as the thumbnail for all local songs as requested
                    val appLogoUri = "android.resource://${context.packageName}/${com.saavn.music.R.mipmap.ic_launcher}"

                    val seconds = durationMs / 1000
                    val minutes = seconds / 60
                    val remSeconds = seconds % 60
                    val durationStr = String.format("%d:%02d", minutes, remSeconds)

                    audioList.add(
                        YouTubeSong(
                            videoId = "local_$id",
                            title = title,
                            channelTitle = if (artist.contains("<unknown>", ignoreCase = true)) "Device Audio" else artist,
                            thumbnailUrl = appLogoUri,
                            durationFormatted = durationStr,
                            durationMs = durationMs,
                            viewCountFormatted = "Device Local",
                            audioUrl = contentUri.toString()
                        )
                    )
                }
            }
            Log.i("LocalAudioScanner", "Scanned ${audioList.size} local audio files from device")
        } catch (e: Exception) {
            Log.e("LocalAudioScanner", "Failed to scan local audio files: ${e.message}", e)
        }

        return@withContext audioList
    }
}
