# Architecture

```
 Android app (customer + delivery partner)        Admin dashboard (React)
        │  HTTPS / JSON, Bearer JWT                      │
        └──────────────────┬─────────────────────────────┘
                   Spring Boot REST API  ──► Razorpay (orders, refunds, webhooks)
                           │             ──► Firebase Cloud Messaging
                      PostgreSQL 16      ──► SMS provider (pluggable SmsSender)
```

## Backend packages (`com.golagrocery`)
| Package | Responsibility |
|---|---|
| `config` | typed configuration (`AppProperties`), security filter chain, CORS, OpenAPI, admin bootstrap |
| `security` | JWT creation/validation (HS256, issuer check), request filter, 401/403 JSON handlers |
| `user` | registration, OTP (hashed, 5-min TTL, 5 attempts, 3 sends / 10 min), login, refresh-token rotation with reuse detection, profile, addresses |
| `catalog` | categories (unlimited nesting), brands, suppliers, products, variants, images, inventory, search/filter (parameterised SQL, whitelisted sorts) |
| `cart` | server-side cart and wishlist |
| `promo` | coupons (flat/percent, caps, min order, per-user & global limits, first-order), banners |
| `order` | pricing (single source of truth, GST-inclusive), checkout, state machine, cancellation, reorder, invoices, unpaid-order expiry job |
| `payment` | gateway abstraction (Razorpay / Mock), signature + webhook verification, refunds |
| `delivery` | partner profile/availability/location, assignments, pickup → out for delivery → OTP-verified delivery, COD collection |
| `notification` | in-app inbox + FCM push (token & topic), EN/HI order messages |
| `review`, `support`, `audit`, `files`, `admin` | ratings, tickets, audit trail, image storage, admin APIs (dashboard, reports, CSV/XLSX import) |

## Order state machine
```
PENDING_PAYMENT ──paid──► PLACED ─► CONFIRMED ─► PREPARING ─► PACKED ─► ASSIGNED ─► OUT_FOR_DELIVERY ─► DELIVERED
      │ 30 min unpaid                │ admin       │ admin        │ admin    │ admin      │ partner          │ partner + customer OTP
      ▼                              └──────── CANCELLED (customer until PREPARING, admin until ASSIGNED) ◄────┘
PAYMENT_FAILED
```
* COD orders start at `PLACED`; online orders start at `PENDING_PAYMENT` and become `PLACED` **only** after the backend verifies the
  Razorpay signature (or a verified webhook). Mock mode requires an explicit "simulate" action and is labelled TEST everywhere.
* Stock is deducted atomically at checkout (`UPDATE … WHERE quantity >= :qty`) and restored on cancellation/expiry.
* Cancelling a paid order creates and processes a refund through the gateway; COD refunds are recorded as MANUAL.
* Delivery requires the 4-digit OTP shown in the customer's app; COD requires the exact cash amount.

## Android app (`com.golagrocery.app`)
* **MVVM + repositories**: `ui/screens/*` (Compose screens + ViewModels) → `data/repository` → `data/remote` (Retrofit, kotlinx-serialization).
* `di/AppContainer` – manual dependency injection (no annotation processing).
* `data/local/SessionStore` – DataStore persistence of tokens, user, delivery location, onboarding flag.
* OkHttp `AuthInterceptor` + `TokenRefreshAuthenticator` (single-flight refresh, logout on failure).
* `CartStore` – app-wide cart state from the server (badges, steppers). Prices are never computed on the device.
* Role-based start: delivery partners land on the delivery console; customers on Home.
* Localisation: `values/strings.xml` + `values-hi/strings.xml`; per-app language switch (Account → Language), also saved to the server so push messages arrive in the same language.

## Security summary
BCrypt (cost 12) passwords and OTP hashes · SHA-256 hashed refresh tokens with rotation · stateless JWT with role claims ·
`/api/v1/admin/**` → ADMIN/STORE_MANAGER, sensitive admin actions (settings, refunds, partners, broadcasts, audit) → ADMIN only ·
`/api/v1/delivery/**` → DELIVERY_PARTNER · Bean Validation on every request body · parameterised SQL only ·
image uploads checked by MIME type **and** magic bytes, path-traversal-safe file serving · CORS allow-list ·
HMAC-SHA256 constant-time verification for Razorpay payments & webhooks · audit log of logins, payments, order changes, catalogue/settings edits ·
no secrets in source (all from environment) · release app is HTTPS-only.
