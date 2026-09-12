package com.saavn.music.data.model

data class UserProfile(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val preferredLanguages: List<String> = emptyList(),
    val isPremium: Boolean = false,
    val selectedPlan: String = "FREE", // "FREE", "MONTHLY", "YEARLY"
    val subscriptionStatus: String = "FREE", // "FREE", "PREMIUM", "EXPIRED", "CANCELLED"
    val planType: String = "FREE", // "MONTHLY", "YEARLY", "FREE"
    val subscriptionStart: Long = 0L,
    val subscriptionExpiry: Long = 0L,
    val isTester: Boolean = false,
    val updateChannel: String = "STABLE" // "STABLE", "BETA"
)
