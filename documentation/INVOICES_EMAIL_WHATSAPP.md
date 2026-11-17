# Order invoices by email and WhatsApp

When an order is **placed** (Cash on Delivery at checkout, online orders once Razorpay confirms the payment)
the backend sends the GST tax invoice automatically:

| Channel  | What the customer gets | Needs |
|----------|------------------------|-------|
| Email    | Email with order summary + **Invoice-GGxxxxxx.pdf** attached + "Download invoice" button | Already working (Gmail relay, `scripts/gmail-otp-relay.gs` v2) |
| WhatsApp | Template message with the **invoice PDF** as a document | WhatsApp Business Cloud API (below) |

Sending happens in the background after the order is saved; if email/WhatsApp fails the order is not affected
(see Render logs: `Invoice ... emailed`, `Invoice ... sent on WhatsApp`, or `... failed: <reason>`).

The PDF link is `https://gola-grocery-api.onrender.com/api/v1/public/invoices/{orderId}/{token}/Invoice-GGxxxxxx.pdf`;
the token is an HMAC signed with `JWT_SECRET`, so links cannot be guessed.

## WhatsApp setup (Meta WhatsApp Business Cloud API)

Cost: Meta charges per **utility** template message (India: roughly ₹0.12-0.15 each; check current Meta pricing).
You need a payment method in Meta Business Manager, and a phone number that is **not** already used in the
WhatsApp / WhatsApp Business app.

1. https://developers.facebook.com -> **Create app** -> type **Business** -> add product **WhatsApp**.
2. WhatsApp -> **API Setup**: add and verify your business phone number. Copy the **Phone number ID**.
3. Business settings -> **System users** -> add a system user (Admin) -> **Generate token** for the app with
   permissions `whatsapp_business_messaging` and `whatsapp_business_management` (choose "Never expire").
4. WhatsApp Manager -> **Message templates** -> **Create template**:
   - Category **Utility**, name **order_invoice**, language **English**
   - Header: **Document**
   - Body: `Hi {{1}}, thank you for shopping with Gola Grocery! Your invoice for order {{2}} ({{3}}) is attached.`
   - Submit and wait for approval (usually minutes).
5. Render -> gola-grocery-api -> Environment:
   - `WHATSAPP_TOKEN` = the system-user token
   - `WHATSAPP_PHONE_NUMBER_ID` = the Phone number ID
   - (optional) `WHATSAPP_INVOICE_TEMPLATE` if you used another template name, `WHATSAPP_TEMPLATE_LANGUAGE` (default `en`)
   - Save, rebuild, and deploy.

Customers' numbers are taken from their account (10-digit Indian numbers get `91` prefixed).
