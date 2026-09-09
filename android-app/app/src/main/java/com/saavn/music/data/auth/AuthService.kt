package com.saavn.music.data.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
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

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: "user_guest"
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified == true
    }

    suspend fun checkEmailVerified(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            user.reload().await()
            user.isEmailVerified
        } catch (e: Exception) {
            user.isEmailVerified
        }
    }

    suspend fun registerUser(displayName: String, emailStr: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(emailStr.trim(), pass.trim()).await()
            val user = result.user ?: throw Exception("Failed to create user account.")
            
            // Immediately send native Firebase verification email safely
            try {
                user.sendEmailVerification().await()
            } catch (e: Exception) {
                android.util.Log.w("ISAI_AUTH", "sendEmailVerification warning: ${e.message}")
            }
            
            // Sync user profile data to Firestore
            syncProfile(user, displayName.trim())
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    suspend fun loginUser(emailStr: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(emailStr.trim(), pass.trim()).await()
            val user = result.user ?: throw Exception("Failed to sign in.")
            
            // Reload user state from Firebase to get latest isEmailVerified flag
            user.reload().await()
            syncProfile(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    suspend fun resendVerificationEmail(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("No active session found. Please sign in again."))
        return try {
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    suspend fun sendPasswordResetEmail(emailStr: String): Result<Unit> {
        val clean = emailStr.trim()
        if (clean.isBlank()) return Result.failure(Exception("Please enter your email address."))
        return try {
            auth.sendPasswordResetEmail(clean).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    fun updateDisplayName(newName: String) {
        val current = _currentUserProfile.value
        if (current != null) {
            _currentUserProfile.value = current.copy(displayName = newName)
            val uid = current.userId
            if (uid.isNotBlank()) {
                firestore.collection("profiles").document(uid)
                    .set(mapOf("displayName" to newName), SetOptions.merge())
            }
        }
    }

    fun logout() {
        auth.signOut()
        _currentUserProfile.value = null
    }

    private fun syncProfile(user: FirebaseUser, customName: String? = null) {
        val uid = user.uid
        val name = customName ?: user.displayName ?: (user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "ISAI Listener")
        val emailStr = user.email ?: ""

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
                // Ignore offline Firestore write issues
            }
    }

    private fun mapFirebaseError(e: Exception): String {
        if (e is FirebaseAuthException) {
            return when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE", "auth/email-already-in-use" ->
                    "An account already exists with this email address. Please sign in."
                "ERROR_INVALID_EMAIL", "auth/invalid-email" ->
                    "Invalid email address format. Please enter a valid email."
                "ERROR_WEAK_PASSWORD", "auth/weak-password" ->
                    "Password is too weak. Please enter at least 6 characters."
                "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "auth/wrong-password", "auth/invalid-credential" ->
                    "Incorrect email or password. Please check and try again."
                "ERROR_USER_NOT_FOUND", "auth/user-not-found" ->
                    "No account found for this email address."
                "ERROR_TOO_MANY_REQUESTS", "auth/too-many-requests" ->
                    "Too many attempts. Please wait a moment and try again."
                else -> e.message ?: "Authentication error occurred."
            }
        }
        val msg = e.message ?: ""
        return when {
            msg.contains("network", ignoreCase = true) || msg.contains("connection", ignoreCase = true) ->
                "Network error. Please check your internet connection."
            msg.isNotBlank() -> msg
            else -> "An error occurred. Please try again."
        }
    }
}


