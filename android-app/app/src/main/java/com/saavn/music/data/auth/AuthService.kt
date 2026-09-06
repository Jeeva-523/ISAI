package com.saavn.music.data.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

data class AndroidUserProfile(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val language: String = "Tamil"
)

class AuthService private constructor(context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _currentUserProfile = MutableStateFlow<AndroidUserProfile?>(null)
    val currentUserProfile: StateFlow<AndroidUserProfile?> = _currentUserProfile

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                syncProfile(user)
            } else {
                _currentUserProfile.value = null
            }
        }
    }

    companion object {
        @Volatile
        private var instance: AuthService? = null

        fun getInstance(context: Context): AuthService {
            return instance ?: synchronized(this) {
                instance ?: AuthService(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "user_jeeva_123"
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun login(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw Exception("Login failed: empty user")
            syncProfile(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, pass: String, displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw Exception("Registration failed")
            syncProfile(user, displayName)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
        _currentUserProfile.value = null
    }

    private fun syncProfile(user: FirebaseUser, customName: String? = null) {
        val uid = user.uid
        val name = customName ?: user.displayName ?: "JEEVA ⚡"
        val emailStr = user.email ?: "jeeva.google@gmail.com"

        val profile = AndroidUserProfile(
            userId = uid,
            username = emailStr.substringBefore("@"),
            displayName = name,
            email = emailStr,
            avatarUrl = user.photoUrl?.toString() ?: ""
        )

        _currentUserProfile.value = profile

        val data = hashMapOf(
            "userId" to uid,
            "username" to profile.username,
            "displayName" to profile.displayName,
            "email" to profile.email,
            "avatarUrl" to profile.avatarUrl,
            "language" to "Tamil",
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("profiles").document(uid)
            .set(data, SetOptions.merge())
            .addOnFailureListener {
                // Ignore offline sync errors gracefully
            }
    }
}
