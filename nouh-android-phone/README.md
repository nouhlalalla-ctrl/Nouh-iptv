# NOUH IPTV — Android Phone APK
## How to build the APK

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer — https://developer.android.com/studio
- Android SDK 34
- JDK 11 or 17 (bundled with Android Studio)

---

### Steps

1. **Open project in Android Studio**
   - Launch Android Studio
   - Click **Open** → select this folder (`nouh-android-phone`)
   - Wait for Gradle sync to finish (first run downloads dependencies)

2. **Build the APK**
   - Menu: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
   - Or in Terminal: `./gradlew assembleRelease`
   - APK is output to: `app/build/outputs/apk/release/app-release.apk`

3. **Install on your phone**
   - Enable *Developer Options* → *USB Debugging* on your phone
   - Connect phone via USB
   - In Android Studio: **Run → Run 'app'**  
   - Or via ADB: `adb install app/build/outputs/apk/release/app-release.apk`

4. **Sideload (no PC needed)**
   - Copy the APK to your phone storage
   - On phone: Settings → Apps → Install unknown apps → allow your file manager
   - Open the APK with your file manager to install

---

### Notes
- The app requires internet access (needed for IPTV streams and HLS.js CDN)
- All IPTV data (profiles, sources, channels) is stored locally in WebView localStorage
- HTTP streams are allowed via `network_security_config.xml` (required for most IPTV portals)
- Tested on Android 7.0+ (minSdk 21 = Android 5.0+)
