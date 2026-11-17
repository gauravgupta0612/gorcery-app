# Local setup (Windows / macOS / Linux)

## Prerequisites
| Tool | Version | Used for |
|---|---|---|
| JDK | 21 (Temurin recommended) | backend; Android Gradle also runs on it |
| Maven | 3.9+ | backend build |
| PostgreSQL | 14+ (16 tested) | database |
| Node.js | 20+ (22 tested) | admin dashboard |
| Android Studio | Koala/Ladybug or newer, SDK 35 | Android app |
| Python | 3.9+ (optional) | smoke test |

## 1. Database
```sql
-- psql -U postgres
CREATE ROLE gola WITH LOGIN PASSWORD '<choose-a-password>';
CREATE DATABASE gola_grocery OWNER gola ENCODING 'UTF8';
```
You don't need to run migrations manually — the backend applies `database/migrations/*.sql` with Flyway at startup.
(Optional manual apply: `database/scripts/apply_migrations.sh`.)

## 2. Backend
```powershell
cd backend
copy .env.example .env      # then edit .env
# PowerShell: load .env into the session
Get-Content .env | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object { $k,$v = $_ -split '=',2; [Environment]::SetEnvironmentVariable($k,$v) }
mvn spring-boot:run
```
bash/zsh: `set -a; source .env; set +a; mvn spring-boot:run`

Check: http://localhost:8095/actuator/health → `{"status":"UP"}`.

Useful development values in `.env`: `OTP_DEV_ECHO=true` (OTP returned in API + shown in the app), `PAYMENTS_MODE=mock`.
Store hours are 06:00–23:30 IST by default (checkout is refused outside them) — change in Admin → Store settings.

Tests: `mvn test` (unit tests: pricing/GST maths, JWT, Razorpay signature verification, order state machine, upload validation).

## 3. Admin dashboard
```bash
cd admin-dashboard
npm install
npm run dev          # http://localhost:5173 (proxies /api and /files to :8095)
npm test             # unit tests
npm run build        # production build in dist/
```

## 4. Android app
See `ANDROID_BUILD.md`. Backend URL is set in `android-app/gradle.properties` (`golaApiBaseUrl`).
- Emulator: `http://10.0.2.2:8095/` (default)
- Phone on same Wi-Fi: `http://<PC-LAN-IP>:8095/` and allow port 8095 in Windows Firewall.

## 5. Accounts for testing
- **Admin**: created from `ADMIN_PHONE` / `ADMIN_PASSWORD` on startup.
- **Customer**: register in the app (OTP shown in a yellow box when `OTP_DEV_ECHO=true`).
- **Delivery partner**: Admin → Delivery partners → Add partner, then sign in to the Android app with that phone + password ("Log in with password").
- Serviceable demo pincodes: 201301, 201303, 201304 (Noida), 110001, 110017 (Delhi), 122001, 122018 (Gurugram), 400001, 400050 (Mumbai), 560001, 560034, 560102 (Bengaluru).
- Demo coupons: `GOLA75` (₹75 off first order ≥ ₹299), `FRESH10` (10% up to ₹100 ≥ ₹499), `SAVE50` (₹50 off ≥ ₹699).
