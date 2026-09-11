package com.saavn.music.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.saavn.music.data.model.DynamicUiConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DynamicUiService private constructor(private val context: Context) {
    private val rtdb = FirebaseDatabase.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences("isai_dynamic_ui", Context.MODE_PRIVATE)

    private val _uiConfig = MutableStateFlow(loadCachedConfig())
    val uiConfig: StateFlow<DynamicUiConfig> = _uiConfig.asStateFlow()

    companion object {
        private const val TAG = "DynamicUiService"

        @Volatile
        private var instance: DynamicUiService? = null

        fun getInstance(context: Context): DynamicUiService {
            return instance ?: synchronized(this) {
                instance ?: DynamicUiService(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        startRealtimeListener()
    }

    private fun loadCachedConfig(): DynamicUiConfig {
        return DynamicUiConfig(
            queueActionStyle = prefs.getString("queueActionStyle", "capsule_pill") ?: "capsule_pill",
            enableQueueReorder = prefs.getBoolean("enableQueueReorder", true),
            enableQueueDelete = prefs.getBoolean("enableQueueDelete", true),
            capsulePillSize = prefs.getInt("capsulePillSize", 32),
            accentColorHex = prefs.getString("accentColorHex", "#00F5D4") ?: "#00F5D4",
            deleteColorHex = prefs.getString("deleteColorHex", "#FF007F") ?: "#FF007F",
            queueHeaderSubtitle = prefs.getString("queueHeaderSubtitle", "Reorder with ▲ ▼ • Remove with 🗑") ?: "Reorder with ▲ ▼ • Remove with 🗑",
            showPlayingBadge = prefs.getBoolean("showPlayingBadge", true),
            bannerMessage = prefs.getString("bannerMessage", "") ?: "",
            version = prefs.getLong("version", 1L)
        )
    }

    private fun saveCachedConfig(config: DynamicUiConfig) {
        prefs.edit()
            .putString("queueActionStyle", config.queueActionStyle)
            .putBoolean("enableQueueReorder", config.enableQueueReorder)
            .putBoolean("enableQueueDelete", config.enableQueueDelete)
            .putInt("capsulePillSize", config.capsulePillSize)
            .putString("accentColorHex", config.accentColorHex)
            .putString("deleteColorHex", config.deleteColorHex)
            .putString("queueHeaderSubtitle", config.queueHeaderSubtitle)
            .putBoolean("showPlayingBadge", config.showPlayingBadge)
            .putString("bannerMessage", config.bannerMessage)
            .putLong("version", config.version)
            .apply()
    }

    private fun startRealtimeListener() {
        val ref = rtdb.getReference("app_config/ui_config")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                try {
                    val style = snapshot.child("queueActionStyle").value?.toString() ?: "capsule_pill"
                    val reorder = snapshot.child("enableQueueReorder").value as? Boolean ?: true
                    val delete = snapshot.child("enableQueueDelete").value as? Boolean ?: true
                    val size = (snapshot.child("capsulePillSize").value as? Number)?.toInt() ?: 32
                    val accent = snapshot.child("accentColorHex").value?.toString() ?: "#00F5D4"
                    val delColor = snapshot.child("deleteColorHex").value?.toString() ?: "#FF007F"
                    val subtitle = snapshot.child("queueHeaderSubtitle").value?.toString() ?: "Reorder with ▲ ▼ • Remove with 🗑"
                    val badge = snapshot.child("showPlayingBadge").value as? Boolean ?: true
                    val banner = snapshot.child("bannerMessage").value?.toString() ?: ""
                    val ver = (snapshot.child("version").value as? Number)?.toLong() ?: 1L

                    val newConfig = DynamicUiConfig(
                        queueActionStyle = style,
                        enableQueueReorder = reorder,
                        enableQueueDelete = delete,
                        capsulePillSize = size,
                        accentColorHex = accent,
                        deleteColorHex = delColor,
                        queueHeaderSubtitle = subtitle,
                        showPlayingBadge = badge,
                        bannerMessage = banner,
                        version = ver
                    )

                    saveCachedConfig(newConfig)
                    _uiConfig.value = newConfig
                    Log.d(TAG, "Live Dynamic UI Config applied: style=$style, reorder=$reorder, delete=$delete")
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing dynamic UI config: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Dynamic UI config listener cancelled: ${error.message}")
            }
        })
    }
}
