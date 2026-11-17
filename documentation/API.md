# REST API (v1)
Interactive docs: `http://localhost:8095/swagger-ui.html`. All bodies are JSON; errors are `{ "code", "message", "fields"? }`.

## Public
| Method | Path | Notes |
|---|---|---|
| POST | /api/v1/auth/register | name, phone, email?, password → sends OTP |
| POST | /api/v1/auth/otp/request | phone, purpose LOGIN/REGISTER |
| POST | /api/v1/auth/otp/verify | → access + refresh token |
| POST | /api/v1/auth/login | identifier (phone or email) + password |
| POST | /api/v1/auth/refresh · /logout | refresh-token rotation / revoke |
| GET | /api/v1/catalog/home · /categories · /brands?categoryId | |
| GET | /api/v1/catalog/products | q, categoryId, brandIds, minPrice, maxPrice, inStock, discounted, sort (relevance, price_asc, price_desc, discount, rating, newest), page, size |
| GET | /api/v1/catalog/products/{id} · /{id}/reviews | |
| GET | /api/v1/store/settings · /store/pincodes/{pincode} | |
| POST | /api/v1/payments/razorpay/webhook | signature-verified |

## Customer (Bearer token)
`GET/PUT /me` · `POST /me/password` · `GET/POST/PUT/DELETE /me/addresses` ·
`GET /cart` · `PUT /cart/items` · `DELETE /cart/items/{variantId}` · `DELETE /cart` · `POST/DELETE /cart/coupon` · `GET /cart/coupons` ·
`GET /wishlist` · `POST/DELETE /wishlist/{productId}` ·
`POST /orders/checkout` · `GET /orders` · `GET /orders/{id}` · `POST /orders/{id}/cancel` · `POST /orders/{id}/reorder` · `POST /orders/{id}/retry-payment` · `GET /orders/{id}/invoice` ·
`POST /payments/razorpay/verify` · `POST /payments/mock/complete` (mock mode only) · `POST /payments/failure` ·
`POST /reviews` · `GET /notifications` · `GET /notifications/unread-count` · `POST /notifications/{id}/read` · `POST /notifications/read-all` · `POST /devices` ·
`GET /support/contact` · `GET/POST /support/tickets`

## Delivery partner (role DELIVERY_PARTNER)
`GET /delivery/me` · `PUT /delivery/me/availability` · `PUT /delivery/me/location` · `GET /delivery/assignments` · `GET /delivery/assignments/history` ·
`GET /delivery/assignments/{id}` · `POST …/{id}/pickup` · `POST …/{id}/out-for-delivery` · `POST …/{id}/deliver` (otp, codCollected)

## Admin (ADMIN / STORE_MANAGER; ★ = ADMIN only)
Dashboard `GET /admin/dashboard` · Reports `GET /admin/reports/sales`, `/admin/reports/sales.csv` ·
Products `GET/POST /admin/products`, `GET/PUT/DELETE /admin/products/{id}`, `PATCH …/{id}/active`, `POST/DELETE …/{id}/images`, `POST /admin/uploads`,
`POST /admin/products/import` (CSV/XLSX), `GET /admin/products/import/template` ·
Categories `GET/POST /admin/categories`, `PUT/DELETE /admin/categories/{id}` · Brands, Suppliers CRUD ·
Inventory `GET /admin/inventory?lowStock=true`, `POST /admin/inventory/{variantId}/adjust`, `PUT …/settings`, `GET …/movements` ·
Orders `GET /admin/orders`, `GET /admin/orders/{id}`, `PATCH …/status`, `POST …/assign`, `GET …/invoice`, ★`POST …/refunds` ·
Customers `GET /admin/customers`, `GET /admin/customers/{id}`, ★`PATCH …/enabled` · Delivery partners `GET`, ★`POST`, ★`PUT` ·
Coupons, Banners CRUD · Pincodes `GET`, `PUT/DELETE /admin/pincodes/{pincode}` · Settings `GET`, ★`PUT /admin/settings` ·
Refunds `GET /admin/refunds`, ★`POST …/{id}/process`, ★`POST …/{id}/reject` · Reviews `GET`, `PATCH /admin/reviews/{id}` ·
Support `GET /admin/support/tickets`, `PATCH …/{id}` · ★`POST /admin/notifications/broadcast` · ★`GET /admin/audit-logs`
