package com.saavn.music.data.model

data class AppUpdateModel(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val minRequiredVersionCode: Int = 1,
    val updateTitle: String = "New Update Available! 🚀",
    val updateMessage: String = "A fresh version of ISAI is ready with new features and improvements.",
    val releaseNotes: List<String> = emptyList(),
    val downloadUrl: String = "",
    val isForceUpdate: Boolean = false,
    val targetMode: String = "ALL", // "ALL", "TESTERS_ONLY", "SPECIFIC_USERS"
    val targetEmails: List<String> = emptyList(),
    val targetUserIds: List<String> = emptyList()
)
