package com.saavn.music.payment

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class CreateOrderApiRequest(
    @SerializedName("planType") val planType: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("userEmail") val userEmail: String? = null,
    @SerializedName("userName") val userName: String? = null
)

data class CreateOrderApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("orderId") val orderId: String? = null,
    @SerializedName("subscriptionId") val subscriptionId: String? = null,
    @SerializedName("keyId") val keyId: String? = null,
    @SerializedName("amount") val amount: Int = 0,
    @SerializedName("currency") val currency: String = "INR",
    @SerializedName("planType") val planType: String = "MONTHLY",
    @SerializedName("message") val message: String? = null
)

data class VerifyPaymentApiRequest(
    @SerializedName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerializedName("razorpay_order_id") val razorpayOrderId: String? = null,
    @SerializedName("razorpay_subscription_id") val razorpaySubscriptionId: String? = null,
    @SerializedName("razorpay_signature") val razorpaySignature: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("planType") val planType: String,
    @SerializedName("userEmail") val userEmail: String? = null,
    @SerializedName("userName") val userName: String? = null
)

data class SubscriptionRecordDto(
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("subscription_status") val subscriptionStatus: String = "FREE",
    @SerializedName("plan_type") val planType: String = "FREE",
    @SerializedName("subscription_start") val subscriptionStart: Long = 0L,
    @SerializedName("subscription_expiry") val subscriptionExpiry: Long = 0L,
    @SerializedName("payment_status") val paymentStatus: String = "PAID",
    @SerializedName("updated_at") val updatedAt: Long = 0L
)

data class VerifyPaymentApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("isPremium") val isPremium: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("subscription") val subscription: SubscriptionRecordDto? = null
)

data class SubscriptionStatusApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("isPremium") val isPremium: Boolean,
    @SerializedName("subscription") val subscription: SubscriptionRecordDto? = null
)

data class CancelSubscriptionApiRequest(
    @SerializedName("userId") val userId: String
)

data class CancelSubscriptionApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)

interface PaymentApiService {
    @POST("api/payments/razorpay/create-order")
    suspend fun createOrder(@Body req: CreateOrderApiRequest): CreateOrderApiResponse

    @POST("api/payments/razorpay/verify")
    suspend fun verifyPayment(@Body req: VerifyPaymentApiRequest): VerifyPaymentApiResponse

    @GET("api/payments/status/{userId}")
    suspend fun getSubscriptionStatus(@Path("userId") userId: String): SubscriptionStatusApiResponse

    @POST("api/payments/razorpay/cancel")
    suspend fun cancelSubscription(@Body req: CancelSubscriptionApiRequest): CancelSubscriptionApiResponse

    companion object {
        fun create(baseUrl: String = PaymentConfig.BACKEND_BASE_URL): PaymentApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PaymentApiService::class.java)
        }
    }
}
