# Deploy Gola Grocery online for free (Neon + Render)

Result: database on **Neon** (always on), API + admin dashboard on **Render**, Android APK that works on any network.
Never paste passwords into chats or commit them to git - type them only into the Neon/Render dashboards.

## 1. Database - Neon
1. Go to https://neon.tech -> **Sign up with GitHub**.
2. Create project: name `gola-grocery`, Postgres **16**, region **AWS Asia Pacific (Singapore)**, database name `gola_grocery`.
3. On the project dashboard click **Connect**. Turn **Connection pooling OFF** (Flyway migrations need a direct connection).
4. You'll see a string like `postgresql://USER:PASSWORD@ep-xxxx.ap-southeast-1.aws.neon.tech/gola_grocery?sslmode=require`.
   Split it into three values (keep the window open for step 2):
   - `DB_URL`      = `jdbc:postgresql://ep-xxxx.ap-southeast-1.aws.neon.tech/gola_grocery?sslmode=require`
   - `DB_USERNAME` = `USER`
   - `DB_PASSWORD` = `PASSWORD`

## 2. API + admin - Render
1. Push the latest code (`push-to-github.bat`) - it includes `render.yaml`.
2. Go to https://render.com -> **Sign in with GitHub** -> allow access to `gorcery-app`.
3. **New + -> Blueprint** -> choose `gauravgupta0612/gorcery-app` -> Render reads `render.yaml` and shows two services.
4. Fill the requested values:
   | Key | Value |
   |---|---|
   | DB_URL / DB_USERNAME / DB_PASSWORD | from Neon step 4 |
   | ADMIN_PHONE | your 10-digit admin login number |
   | ADMIN_PASSWORD | a strong password (10+ characters) |
   | ADMIN_EMAIL | optional |
   | CORS_ORIGINS | `https://gola-grocery-admin.onrender.com` |
   | VITE_API_BASE_URL | `https://gola-grocery-api.onrender.com` (no trailing `/`) |
5. **Apply**. First API build takes ~8-12 minutes (Docker + Maven). The first start creates all tables and loads the catalogue into Neon.
6. Check: open `https://gola-grocery-api.onrender.com/actuator/health` -> `{"status":"UP"}`.
   If Render gave your services different URLs (name already taken), use those URLs instead and update
   `CORS_ORIGINS` / `VITE_API_BASE_URL` in each service's **Environment** tab (the admin site needs a **Manual Deploy** after changing it).

## 3. APK that works everywhere
1. GitHub -> `gorcery-app` -> **Settings -> Secrets and variables -> Actions -> Variables -> New repository variable**
   Name `GOLA_API_BASE_URL`, value `https://gola-grocery-api.onrender.com/` (with trailing `/`).
2. **Actions -> Android APK -> Run workflow**.
3. Download https://github.com/gauravgupta0612/gorcery-app/releases/download/apk-latest/GolaGrocery-debug.apk on the phone and install.

## Free-plan behaviour
* The API sleeps after ~15 minutes without traffic; the next request wakes it in ~30-60 s (the app shows a loading state). Data is never lost.
  A paid Render instance removes the sleep.
* Render's free disk is temporary: product images **uploaded** from the admin panel disappear on redeploy (the built-in catalogue images do not).
  For permanent uploads use object storage (Cloudinary / S3) - ask Claude to wire it in.
* Every push to `main` redeploys the API and admin automatically.
