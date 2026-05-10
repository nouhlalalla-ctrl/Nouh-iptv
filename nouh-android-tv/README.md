# NOUH IPTV — Android TV APK
## How to build the APK

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer — https://developer.android.com/studio
- Android SDK 34
- JDK 11 or 17 (bundled with Android Studio)

---

### Steps

1. **Open project in Android Studio**
   - Launch Android Studio
   - Click **Open** → select this folder (`nouh-android-tv`)
   - Wait for Gradle sync to finish

2. **Build the APK**
   - Menu: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
   - Or in Terminal: `./gradlew assembleRelease`
   - APK output: `app/build/outputs/apk/release/app-release.apk`

3. **Install on Android TV / Fire TV / TV Box**

   **Via ADB (recommended):**
   ```bash
   # Find TV IP in Settings → About → Network
   adb connect 192.168.1.XXX:5555
   adb install app/build/outputs/apk/release/app-release.apk
   ```

   **Via USB:**
   - Enable Developer Options on TV: Settings → About → click Build Number 7x
   - Enable USB Debugging
   - Connect USB and run: `adb install app-release.apk`

   **Via sideload apps:**
   - Copy APK to USB stick → plug into TV → use file manager app
   - Or use Downloader app (Firestick) to load APK from URL

4. **App appears on TV home screen** under the Leanback launcher banner

---

### TV Remote Controls
| Button | Action |
|--------|--------|
| ↑ ↓ ← → | Navigate menus & channels |
| OK / Enter | Select / Play |
| Back | Go back / Close player |
| Play/Pause | Toggle playback |

### Notes
- Designed for 1080p landscape (forced landscape orientation)
- No touch required — full D-pad / remote navigation
- HTTP streams allowed for all IPTV portals
- Works on: Android TV, Google TV, Fire TV (4.x+), Nvidia Shield, TV boxes (Android 5+)
