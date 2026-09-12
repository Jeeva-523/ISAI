package com.saavn.music.payment

import android.app.Activity
import android.content.Context
import android.util.Log
import com.razorpay.Checkout
import org.json.JSONObject

object RazorpayManager {
    private const val TAG = "ISAI_RazorpayManager"

    fun init(context: Context) {
        try {
            Checkout.preload(context.applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Checkout.preload warning: ${e.message}")
        }
    }

    /**
     * Launches Razorpay Standard Checkout UI using the server-generated order ID and safe public Key ID.
     */
    fun openCheckout(
        activity: Activity,
        keyId: String,
        orderId: String,
        amount: Int,
        planType: String,
        userEmail: String?,
        userName: String?
    ) {
        val checkout = Checkout()
        checkout.setKeyID(keyId)

        try {
            val options = JSONObject().apply {
                put("name", "ISAI Music")
                put("description", if (planType == "YEARLY") "ISAI Premium Yearly (₹399/yr)" else "ISAI Premium Monthly (₹49/mo)")
                put("theme.color", "#06B6D4")
                put("currency", "INR")
                put("amount", amount)
                put("order_id", orderId)

                val prefill = JSONObject().apply {
                    put("email", userEmail ?: "user@isaimusic.com")
                    put("name", userName ?: "ISAI Listener")
                }
                put("prefill", prefill)

                val retryObj = JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 2)
                }
                put("retry", retryObj)
            }

            checkout.open(activity, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Razorpay checkout: ${e.message}", e)
        }
    }
}
