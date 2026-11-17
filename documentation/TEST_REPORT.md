# Test & verification report

Build environment for this delivery: the build sandbox could reach the **npm registry and GitHub only** — Maven Central,
Google Maven and the Gradle plugin portal were blocked by network policy. That determines what could be executed here.

## Executed and passing
| Area | What was run | Result |
|---|---|---|
| Database | `V1__schema.sql` + `V2__seed_catalog.sql` applied to a real PostgreSQL 16 with `ON_ERROR_STOP` | ✅ 16 top-level + 52 sub-categories, 144 products, 164 variants, 164 inventory rows, 288 images, 74 brands, 3 coupons, 12 pincodes |
| Database ↔ JPA | Script comparing every entity field with the live schema columns | ✅ all mapped columns exist |
| Backend native SQL | Dashboard series, sales report, category sales, product search (lateral join, filters, sorts), low-stock count, brand-by-category, rating refresh, broadcast insert, order-number sequence, atomic stock deduction — executed against the seeded DB | ✅ (one `day` alias bug found and fixed) |
| Backend Java | `javac` parse of all 140 sources | ✅ no syntax errors (type-checking needs the Maven dependencies) |
| Admin dashboard | `tsc --noEmit` (strict), `vitest` (4 tests), `vite build` | ✅ 0 type errors, 4/4 tests, production bundle built |
| Admin dashboard UI | Playwright: login → dashboard → order detail with mocked API responses, console checked | ✅ renders, no runtime errors |
| Android | `kotlinc 2.0.21` parse of all 36 Kotlin sources; XML resources well-formed | ✅ no syntax errors (type-checking needs the Android/Compose dependencies) |

## Written but NOT executed here (run these on your machine)
| Command | Covers |
|---|---|
| `cd backend && mvn test` | JWT round-trip & tamper rejection, Razorpay payment/webhook signature checks, GST/discount/paise maths, order state machine & cancellation window, EN/HI messages, upload magic-byte validation |
| `cd backend && mvn spring-boot:run` then `python scripts/smoke_test.py …` | 45+ end-to-end checks: catalogue, register/OTP, RBAC (403 for customer on admin API), address, cart & server totals, coupon validation, COD checkout, admin workflow (cannot skip states), partner creation/assignment, pickup/out-for-delivery, wrong-OTP rejection, OTP delivery with COD cash, invoice, review, reorder, mock online payment, customer cancellation with automatic refund, reports, notifications, audit log |
| `cd android-app && ./gradlew testDebugUnitTest assembleDebug` | JSON parsing of backend payloads, error-body mapping, network-failure mapping, image URL resolution; builds `app-debug.apk` |

**Until those commands have been run, the backend and Android app should be treated as uncompiled.** If any compile
error appears, send me the output and I'll fix it.

## Known limitations / next steps
* Product images are generated placeholders, not professional photography — upload real images via the admin panel.
* SMS delivery of OTPs needs a provider implementation of `SmsSender` (MSG91, Twilio, AWS SNS…).
* Live delivery-partner tracking is location upload + 20 s order polling; a map view with live rider position is a future enhancement.
* Images are stored on local disk; use S3/GCS (replace `FileStorageService`) for multi-instance deployments.
* No instrumented (on-device) UI tests yet.
