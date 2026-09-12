export type PlanType = 'MONTHLY' | 'YEARLY' | 'FREE'
export type SubscriptionStatus = 'FREE' | 'PREMIUM' | 'EXPIRED' | 'CANCELLED'
export type PaymentStatus = 'PAID' | 'PENDING' | 'FAILED' | 'CANCELLED'

export interface CreateOrderRequest {
  planType: PlanType
  userId: string
  userEmail?: string
  userName?: string
}

export interface CreateOrderResponse {
  success: boolean
  orderId?: string
  subscriptionId?: string
  keyId: string
  amount: number
  currency: string
  planType: PlanType
  message?: string
}

export interface VerifyPaymentRequest {
  razorpay_payment_id: string
  razorpay_order_id?: string
  razorpay_subscription_id?: string
  razorpay_signature: string
  userId: string
  planType: PlanType
  userEmail?: string
  userName?: string
}

export interface VerifyPaymentResponse {
  success: boolean
  isPremium: boolean
  message: string
  subscription?: UserSubscriptionRecord
}

export interface UserSubscriptionRecord {
  user_id: string
  subscription_status: SubscriptionStatus
  plan_type: PlanType
  razorpay_customer_id?: string
  razorpay_subscription_id?: string
  razorpay_payment_id?: string
  subscription_start: number
  subscription_expiry: number
  payment_status: PaymentStatus
  updated_at: number
}

export interface RazorpayWebhookPayload {
  entity: string
  account_id: string
  event: string
  contains: string[]
  payload: {
    payment?: {
      entity: {
        id: string
        order_id?: string
        amount: number
        currency: string
        status: string
        notes?: Record<string, string>
      }
    }
    subscription?: {
      entity: {
        id: string
        plan_id: string
        status: string
        current_start?: number
        current_end?: number
        notes?: Record<string, string>
      }
    }
  }
  created_at: number
}
