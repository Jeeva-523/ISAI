import { OpenAPIHono } from '@hono/zod-openapi'
import { paymentService } from './services'
import type { Routes } from '#common/types'
import type { CreateOrderRequest, VerifyPaymentRequest } from './types'

export class PaymentController implements Routes {
  public controller: OpenAPIHono

  constructor() {
    this.controller = new OpenAPIHono()
  }

  public initRoutes() {
    // 1. POST /api/payments/razorpay/create-order
    this.controller.post('/payments/razorpay/create-order', async (c) => {
      try {
        const body = (await c.req.json()) as CreateOrderRequest
        if (!body.planType || !body.userId) {
          return c.json({ success: false, message: 'planType and userId are required.' }, 400)
        }

        const res = await paymentService.createOrder(body)
        return c.json(res)
      } catch (e: any) {
        return c.json({ success: false, message: e.message || 'Failed to create payment order.' }, 500)
      }
    })

    // 2. POST /api/payments/razorpay/verify
    this.controller.post('/payments/razorpay/verify', async (c) => {
      try {
        const body = (await c.req.json()) as VerifyPaymentRequest
        if (!body.razorpay_payment_id || !body.userId) {
          return c.json({ success: false, message: 'Missing required payment verification parameters.' }, 400)
        }

        const res = await paymentService.verifyPayment(body)
        return c.json(res, res.success ? 200 : 400)
      } catch (e: any) {
        return c.json({ success: false, message: e.message || 'Verification failed.' }, 500)
      }
    })

    // 3. POST /api/payments/razorpay/webhook
    this.controller.post('/payments/razorpay/webhook', async (c) => {
      try {
        const rawBody = await c.req.text()
        const signature = c.req.header('x-razorpay-signature') || ''

        const res = await paymentService.handleWebhook(rawBody, signature)
        return c.json(res, res.success ? 200 : 400)
      } catch (e: any) {
        return c.json({ success: false, message: e.message || 'Webhook handling failed.' }, 500)
      }
    })

    // 4. GET /api/payments/status/:userId
    this.controller.get('/payments/status/:userId', async (c) => {
      try {
        const userId = c.req.param('userId')
        if (!userId) {
          return c.json({ success: false, message: 'userId is required.' }, 400)
        }

        const status = await paymentService.getSubscriptionStatus(userId)
        return c.json({
          success: true,
          isPremium: status?.subscription_status === 'PREMIUM',
          subscription: status
        })
      } catch (e: any) {
        return c.json({ success: false, message: e.message || 'Failed to retrieve subscription status.' }, 500)
      }
    })

    // 5. POST /api/payments/razorpay/cancel
    this.controller.post('/payments/razorpay/cancel', async (c) => {
      try {
        const body = (await c.req.json()) as { userId: string }
        if (!body.userId) {
          return c.json({ success: false, message: 'userId is required.' }, 400)
        }

        const success = await paymentService.cancelSubscription(body.userId)
        return c.json({ success, message: success ? 'Subscription cancelled.' : 'Subscription not found.' })
      } catch (e: any) {
        return c.json({ success: false, message: e.message || 'Cancellation failed.' }, 500)
      }
    })
  }
}
