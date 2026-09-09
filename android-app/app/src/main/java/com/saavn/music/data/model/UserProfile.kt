package com.saavn.music.data.model

data class UserProfile(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val preferredLanguages: List<String> = emptyList()
)
