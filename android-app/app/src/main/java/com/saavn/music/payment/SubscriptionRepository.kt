package com.saavn.music.payment

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.saavn.music.data.local.LocalMusicStorage
import com.saavn.music.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SubscriptionRepository(
    private val context: Context,
    private val localStorage: LocalMusicStorage
) {
    private val tag = "ISAI_SubscriptionRepo"
    private val apiService = PaymentApiService.create()
    private val database: FirebaseDatabase =
        FirebaseDatabase.getInstance("https://isai-49b51-default-rtdb.firebaseio.com")
    private var activeListener: ValueEventListener? = null
    private var activeUserId: String? = null

    /**
     * Attaches a real-time listener to Firebase RTDB for the current user's subscription record.
     * Ensures instant account sync across multiple devices and platforms (Android, Web).
     */
    fun attachSubscriptionListener(userId: String) {
        if (userId.isBlank()) return
        if (activeUserId == userId && activeListener != null) return

        detachSubscriptionListener()
        activeUserId = userId

        val ref = database.getReference("subscriptions").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                try {
                    val status = snapshot.child("subscription_status").value?.toString() ?: "FREE"
                    val planType = snapshot.child("plan_type").value?.toString() ?: "FREE"
                    val expiry = snapshot.child("subscription_expiry").value as? Long ?: 0L
                    val start = snapshot.child("subscription_start").value as? Long ?: 0L

                    val isPrem = status.equals("PREMIUM", ignoreCase = true) &&
                            (expiry == 0L || System.currentTimeMillis() <= expiry)

                    val current = localStorage.userProfile.value
                    if (current != null) {
                        val updated = current.copy(
                            isPremium = isPrem,
                            selectedPlan = if (isPrem) planType else "FREE",
                            subscriptionStatus = if (isPrem) "PREMIUM" else if (status.equals("CANCELLED", ignoreCase = true)) "CANCELLED" else "FREE",
                            planType = planType,
                            subscriptionStart = start,
                            subscriptionExpiry = expiry
                        )
                        localStorage.saveUserProfile(updated)
                        Log.d(tag, "Synced subscription from RTDB: isPremium=$isPrem, plan=$planType")
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Error parsing subscription snapshot: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(tag, "Subscription listener cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        activeListener = listener
    }

    fun detachSubscriptionListener() {
        val uid = activeUserId
        val l = activeListener
        if (uid != null && l != null) {
            database.getReference("subscriptions").child(uid).removeEventListener(l)
        }
        activeListener = null
        activeUserId = null
    }

    /**
     * Calls backend to create a Razorpay order.
     */
    suspend fun createOrder(
        planType: String,
        userId: String,
        email: String?,
        name: String?
    ): Result<CreateOrderApiResponse> {
        return try {
            val req = CreateOrderApiRequest(
                planType = planType,
                userId = userId,
                userEmail = email,
                userName = name
            )
            val res = apiService.createOrder(req)
            if (res.success) {
                Result.success(res)
            } else {
                Result.failure(Exception(res.message ?: "Failed to create payment order."))
            }
        } catch (e: Exception) {
            Log.e(tag, "createOrder error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sends payment reference to backend for cryptographic HMAC verification.
     */
    suspend fun verifyPayment(req: VerifyPaymentApiRequest): Result<VerifyPaymentApiResponse> {
        return try {
            val res = apiService.verifyPayment(req)
            if (res.success && res.isPremium) {
                // Update local storage with verified subscription
                val current = localStorage.userProfile.value
                val sub = res.subscription
                if (current != null) {
                    val updated = current.copy(
                        isPremium = true,
                        selectedPlan = req.planType,
                        subscriptionStatus = "PREMIUM",
                        planType = req.planType,
                        subscriptionStart = sub?.subscriptionStart ?: System.currentTimeMillis(),
                        subscriptionExpiry = sub?.subscriptionExpiry ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
                    )
                    localStorage.saveUserProfile(updated)
                }
                Result.success(res)
            } else {
                Result.failure(Exception(res.message))
            }
        } catch (e: Exception) {
            Log.e(tag, "verifyPayment error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Cancels subscription via backend.
     */
    suspend fun cancelSubscription(userId: String): Result<Boolean> {
        return try {
            val res = apiService.cancelSubscription(CancelSubscriptionApiRequest(userId))
            Result.success(res.success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refreshes subscription directly from backend.
     */
    suspend fun refreshSubscription(userId: String) {
        try {
            val res = apiService.getSubscriptionStatus(userId)
            if (res.success && res.subscription != null) {
                val sub = res.subscription
                val isPrem = res.isPremium
                val current = localStorage.userProfile.value
                if (current != null) {
                    val updated = current.copy(
                        isPremium = isPrem,
                        selectedPlan = if (isPrem) sub.planType else "FREE",
                        subscriptionStatus = sub.subscriptionStatus,
                        planType = sub.planType,
                        subscriptionStart = sub.subscriptionStart,
                        subscriptionExpiry = sub.subscriptionExpiry
                    )
                    localStorage.saveUserProfile(updated)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "refreshSubscription error: ${e.message}")
        }
    }
}
