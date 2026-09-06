package com.saavn.music.data.search

import android.content.Context
import android.content.SharedPreferences
import com.saavn.music.data.model.YouTubeSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

data class SearchResultEntities(
    val query: String,
    val topResult: SearchEntityItem?,
    val songs: List<YouTubeSong>,
    val artists: List<SearchEntityItem>,
    val albums: List<SearchEntityItem>,
    val movies: List<SearchEntityItem>,
    val playlists: List<SearchEntityItem>,
    val didYouMean: String? = null
)

data class SearchEntityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: String, // "artist", "album", "movie", "playlist", "mood"
    val imageUrl: String = ""
)

class SearchService private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("isai_search_history", Context.MODE_PRIVATE)

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory

    init {
        loadHistory()
    }

    companion object {
        @Volatile
        private var instance: SearchService? = null

        fun getInstance(context: Context): SearchService {
            return instance ?: synchronized(this) {
                instance ?: SearchService(context.applicationContext).also { instance = it }
            }
        }

        // Tanglish & Tamil Transliteration dictionary
        val TRANSLITERATION_MAP = mapOf(
            "anirud" to "Anirudh Ravichander",
            "aniruth" to "Anirudh Ravichander",
            "anirudh" to "Anirudh Ravichander",
            "kadhal" to "காதல் (Love Songs)",
            "kaadhal" to "காதல் (Love Songs)",
            "kathal" to "காதல் (Love Songs)",
            "vaathi" to "வாத்தி (Vaathi Coming)",
            "ar rahman" to "A.R. Rahman",
            "arr" to "A.R. Rahman",
            "yuvan" to "Yuvan Shankar Raja",
            "harris" to "Harris Jayaraj",
            "vikram" to "🎬 Vikram (Movie Soundtrack)",
            "leo" to "🎬 Leo (Movie Soundtrack)",
            "jailer" to "🎬 Jailer (Movie Soundtrack)",
            "sad" to "💔 Sad Songs",
            "party" to "💃 Party Kuthu Hits",
            "melody" to "🌙 Tamil Melodies"
        )
    }

    fun getSuggestedQuery(rawQuery: String): String {
        val q = rawQuery.trim().lowercase()
        return TRANSLITERATION_MAP[q] ?: rawQuery
    }

    fun addSearchQuery(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        val current = _searchHistory.value.toMutableList()
        current.remove(q)
        current.add(0, q)
        if (current.size > 20) {
            current.removeAt(current.size - 1)
        }
        _searchHistory.value = current
        saveHistory(current)
    }

    fun removeSearchQuery(query: String) {
        val current = _searchHistory.value.toMutableList()
        current.remove(query)
        _searchHistory.value = current
        saveHistory(current)
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        saveHistory(emptyList())
    }

    private fun loadHistory() {
        try {
            val jsonStr = prefs.getString("history", "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            _searchHistory.value = list
        } catch (e: Exception) {
            _searchHistory.value = emptyList()
        }
    }

    private fun saveHistory(list: List<String>) {
        try {
            val array = JSONArray()
            list.forEach { array.put(it) }
            prefs.edit().putString("history", array.toString()).apply()
        } catch (_: Exception) {}
    }
}
