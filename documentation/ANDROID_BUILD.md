# Building the Android app

## Debug APK
```bash
cd android-app
./gradlew assembleDebug            # Windows: gradlew.bat assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk   (applicationId com.golagrocery.app.debug)
./gradlew testDebugUnitTest        # JVM unit tests (JSON parsing, error mapping, image URLs)
```
Install on a device/emulator: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Point the app at your backend without editing files:
`./gradlew assembleDebug -PgolaApiBaseUrl=http://192.168.1.20:8080/` (must end with `/`).
Debug builds allow plain HTTP; **release builds require HTTPS** (`res/xml/network_security_config.xml`).

## Signed release APK / AAB
1. Create an upload keystore (once — keep it and its passwords safe and **out of git**):
   ```bash
   keytool -genkeypair -v -keystore gola-upload.jks -alias gola -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Create `android-app/keystore.properties` (already git-ignored):
   ```properties
   storeFile=../gola-upload.jks
   storePassword=<your store password>
   keyAlias=gola
   keyPassword=<your key password>
   ```
3. Build against your production HTTPS API:
   ```bash
   ./gradlew assembleRelease -PgolaApiBaseUrl=https://api.your-domain.in/   # → app/build/outputs/apk/release/app-release.apk
   ./gradlew bundleRelease   -PgolaApiBaseUrl=https://api.your-domain.in/   # → AAB for Google Play
   ```
4. Verify the signature: `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`.

Release builds are minified with R8 (`proguard-rules.pro` keeps kotlinx-serialization, Retrofit and Razorpay classes).

## Firebase Cloud Messaging (optional)
1. Create a Firebase project, add an Android app with package `com.golagrocery.app` (and `com.golagrocery.app.debug` for debug).
2. Download `google-services.json` into `android-app/app/`. The Gradle build detects it and enables the Google Services plugin automatically.
3. Backend: create a service-account key (Project settings → Service accounts) and set `FIREBASE_CREDENTIALS_FILE=/path/key.json`.

## Razorpay (optional)
Set `PAYMENTS_MODE=razorpay` and the keys on the **backend** only. The app receives the public key id from the API; the key secret
never leaves the server. Use `rzp_test_…` keys for testing; configure a webhook to `https://<api>/api/v1/payments/razorpay/webhook`
with events `payment.captured` and `payment.failed`.
