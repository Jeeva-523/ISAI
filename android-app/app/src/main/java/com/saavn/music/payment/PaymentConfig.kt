package com.saavn.music.payment

import com.saavn.music.BuildConfig

/**
 * Centralized Razorpay Payment Configuration for ISAI Android App.
 *
 * SECURITY GUARANTEE:
 * - RAZORPAY_KEY_SECRET is NEVER stored in Android app, APK, or client repositories.
 * - Key Secret resides exclusively on the backend server for cryptographic HMAC-SHA256 verification.
 */
object PaymentConfig {
    /**
     * Razorpay Public Key ID.
     * Safe to bundle in client app.
     */
    val RAZORPAY_KEY_ID: String = if (BuildConfig.DEBUG) {
        "rzp_test_ISAI49b51music"
    } else {
        "rzp_test_ISAI49b51music"
    }

    /**
     * Backend Base URL for payment order creation, verification, and subscription sync.
     * Default uses 10.0.2.2 for Android emulator or localhost.
     */
    const val BACKEND_BASE_URL = "http://10.0.2.2:3000/"

    // Plan pricing in INR
    const val PLAN_MONTHLY_PRICE_INR = 49
    const val PLAN_YEARLY_PRICE_INR = 399

    const val PLAN_MONTHLY_AMOUNT_PAISE = 4900
    const val PLAN_YEARLY_AMOUNT_PAISE = 39900
}
