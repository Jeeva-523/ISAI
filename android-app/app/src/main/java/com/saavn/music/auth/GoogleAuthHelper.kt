package com.saavn.music.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.saavn.music.data.model.UserProfile

class GoogleAuthHelper(private val context: Context) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun getSystemGoogleAccount(): UserProfile? {
        try {
            val manager = android.accounts.AccountManager.get(context)
            val googleAccounts = manager.getAccountsByType("com.google")
            if (googleAccounts.isNotEmpty()) {
                val accountName = googleAccounts[0].name
                val rawName = accountName.substringBefore("@").replace(".", " ")
                val formattedName = rawName.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                return UserProfile(
                    id = "google_" + kotlin.math.abs(accountName.hashCode()),
                    displayName = formattedName,
                    email = accountName,
                    photoUrl = null
                )
            }
        } catch (_: Exception) {}
        return null
    }

    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>): Result<UserProfile> {
        return try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                val profile = UserProfile(
                    id = account.id ?: "google_${System.currentTimeMillis()}",
                    displayName = account.displayName ?: account.givenName ?: "Google User",
                    email = account.email ?: "user@gmail.com",
                    photoUrl = account.photoUrl?.toString()
                )
                Result.success(profile)
            } else {
                val fallback = getLastSignedInAccount() ?: getSystemGoogleAccount()
                if (fallback != null) {
                    Result.success(fallback)
                } else {
                    Result.failure(Exception("No Google Account selected"))
                }
            }
        } catch (e: Exception) {
            val fallback = getLastSignedInAccount() ?: getSystemGoogleAccount()
            if (fallback != null) {
                Result.success(fallback)
            } else {
                Result.failure(e)
            }
        }
    }

    fun getLastSignedInAccount(): UserProfile? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return UserProfile(
            id = account.id ?: "",
            displayName = account.displayName ?: "User",
            email = account.email ?: "",
            photoUrl = account.photoUrl?.toString()
        )
    }

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            onComplete()
        }
    }
}
