# Gola Grocery
**Freshness Delivered to Your Doorstep.** — a complete quick-commerce grocery platform for India (INR, English + हिंदी).

| Folder | What it is | Tech |
|---|---|---|
| `android-app/` | Customer app **and** delivery-partner app (role-based) | Kotlin, Jetpack Compose, Material 3, MVVM, Retrofit, DataStore, Razorpay, FCM |
| `backend/` | REST API, auth, orders, payments, delivery, admin APIs | Spring Boot 3.3, Java 21, Spring Security (JWT), JPA, Flyway |
| `admin-dashboard/` | Store admin console | React 18, TypeScript, Vite, Recharts |
| `database/` | PostgreSQL migrations, catalogue seed generator, seed images | PostgreSQL 16 |
| `documentation/` | Setup, architecture, API, Android build/signing, test report | |
| `scripts/smoke_test.py` | End-to-end API test of the whole order lifecycle | Python 3 (std-lib only) |

## Quick start (local)
1. **Database** – install PostgreSQL 16, then `psql -U postgres -f database/scripts/create_database.sql` (edit the password first).
2. **Backend** – copy `backend/.env.example` to `backend/.env`, fill `DB_PASSWORD`, `JWT_SECRET` (`openssl rand -base64 48`), `ADMIN_PHONE`, `ADMIN_PASSWORD`; then
   ```bash
   cd backend
   set -a; source .env; set +a          # PowerShell: see documentation/SETUP.md
   mvn spring-boot:run                   # Flyway creates the schema + 144 products / 164 SKUs on first start
   ```
   API: http://localhost:8080 · Swagger UI: http://localhost:8080/swagger-ui.html
3. **Admin dashboard** – `cd admin-dashboard && npm install && npm run dev` → http://localhost:5173 (log in with ADMIN_PHONE / ADMIN_PASSWORD).
4. **Android** – open `android-app/` in Android Studio (or `./gradlew assembleDebug`). The emulator reaches your PC at `http://10.0.2.2:8080/` (default). APK: `android-app/app/build/outputs/apk/debug/app-debug.apk`.
5. **Verify everything** – with `OTP_DEV_ECHO=true` and `PAYMENTS_MODE=mock`:
   `python scripts/smoke_test.py --admin-phone <ADMIN_PHONE> --admin-password <ADMIN_PASSWORD>`

Or run DB + API + admin together: `docker compose --env-file backend/.env up --build`.

## Credentials you must supply (nothing secret is committed)
| Purpose | Variable | Without it |
|---|---|---|
| JWT signing | `JWT_SECRET` | backend refuses to start |
| First admin | `ADMIN_PHONE`, `ADMIN_PASSWORD` | no admin account |
| Payments | `PAYMENTS_MODE=razorpay`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` | **mock mode**: payments are clearly labelled TEST and need an explicit "simulate" tap; no money moves |
| Push notifications | `FIREBASE_CREDENTIALS_FILE` (backend) + `android-app/app/google-services.json` | notifications are stored in the in-app inbox only |
| SMS OTP | implement `SmsSender` (MSG91/Twilio/SNS) | OTP is logged as "queued"; use `OTP_DEV_ECHO=true` locally |

See `documentation/` for details, and **`documentation/TEST_REPORT.md` for exactly what was and wasn't verified.**
