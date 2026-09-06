package com.saavn.music.data.analytics

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PlayEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val songId: String,
    val songTitle: String,
    val artistName: String,
    val eventType: String, // "play", "like", "playlist_add", "search"
    val timestamp: Long = System.currentTimeMillis()
)

class AnalyticsService private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("isai_analytics", Context.MODE_PRIVATE)

    private val _recentEvents = MutableStateFlow<List<PlayEvent>>(emptyList())
    val recentEvents: StateFlow<List<PlayEvent>> = _recentEvents

    init {
        loadEvents()
    }

    companion object {
        @Volatile
        private var instance: AnalyticsService? = null

        fun getInstance(context: Context): AnalyticsService {
            return instance ?: synchronized(this) {
                instance ?: AnalyticsService(context.applicationContext).also { instance = it }
            }
        }
    }

    fun trackEvent(songId: String, title: String, artist: String, eventType: String) {
        val event = PlayEvent(
            songId = songId,
            songTitle = title,
            artistName = artist,
            eventType = eventType
        )
        val current = _recentEvents.value.toMutableList()
        current.add(0, event)
        // Keep last 500 events
        if (current.size > 500) {
            current.removeAt(current.size - 1)
        }
        _recentEvents.value = current
        saveEvents(current)
    }

    fun getEventCount(songId: String, eventType: String, timeWindowMs: Long): Int {
        val now = System.currentTimeMillis()
        val cutoff = now - timeWindowMs
        return _recentEvents.value.count {
            it.songId == songId && it.eventType == eventType && it.timestamp >= cutoff
        }
    }

    fun getUniqueListenerCount(songId: String, timeWindowMs: Long): Int {
        val now = System.currentTimeMillis()
        val cutoff = now - timeWindowMs
        // Unique listener counts estimated from plays in time window
        val plays = getEventCount(songId, "play", timeWindowMs)
        return (plays * 0.85).toInt().coerceAtLeast(if (plays > 0) 1 else 0)
    }

    private fun loadEvents() {
        try {
            val jsonStr = prefs.getString("events_data", "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            val list = mutableListOf<PlayEvent>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PlayEvent(
                        eventId = obj.optString("eventId"),
                        songId = obj.optString("songId"),
                        songTitle = obj.optString("songTitle"),
                        artistName = obj.optString("artistName"),
                        eventType = obj.optString("eventType"),
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
            _recentEvents.value = list
        } catch (e: Exception) {
            _recentEvents.value = emptyList()
        }
    }

    private fun saveEvents(events: List<PlayEvent>) {
        try {
            val array = JSONArray()
            events.take(100).forEach { ev ->
                val obj = JSONObject().apply {
                    put("eventId", ev.eventId)
                    put("songId", ev.songId)
                    put("songTitle", ev.songTitle)
                    put("artistName", ev.artistName)
                    put("eventType", ev.eventType)
                    put("timestamp", ev.timestamp)
                }
                array.put(obj)
            }
            prefs.edit().putString("events_data", array.toString()).apply()
        } catch (_: Exception) {}
    }
}
