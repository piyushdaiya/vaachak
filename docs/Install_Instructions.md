# 📲 Installation Instructions

Since Vaachak is currently in **Beta** and not yet on the Google Play Store, you will need to install the APK file manually ("side-load").
You can download latest release from [Releases Page](https://github.com/piyushdaiya/vaachak/releases/)

Follow one of the methods below to install **Vaachak v2.0**.

---

## ⚠️ Prerequisites
1.  **Enable Unknown Sources:** On your Android device, you may need to allow installation from "Unknown Sources" or allow your browser/file manager to install apps.
2.  **Uninstall Old Versions:** If you previously installed a "Debug" version via Android Studio, you **must uninstall it** before installing the "Release" version from GitHub. They use different signing keys, and Android will block the update otherwise.
    * *Note: Uninstalling deletes your local library/highlights database.*

---

## Method 1: Using ADB (Recommended for Developers)
This method requires a PC with [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) installed.

1.  **Connect your device** via USB and ensure **USB Debugging** is ON.
2.  Open your terminal or command prompt.
3.  Navigate to the folder where you downloaded the APK:
    ```bash
    cd path/to/downloaded/apk
    ```
4.  Run the install command:

    **Option A: Clean Install**
    ```bash
    adb install Vaachak-release.apk
    ```

    **Option B: Upgrade (Retain Data)**
    *Use this only if upgrading from a previous **Release** build.*
    ```bash
    adb install -r Vaachak-release.apk
    ```

---

## Method 2: Direct Install (No PC Required)
This is the easiest method for most users.

1.  **Download** the `Vaachak-release.apk` file directly on your phone (or transfer it via USB/Drive).
2.  Open your phone's **File Manager** app.
3.  Navigate to your **Downloads** folder.
4.  Tap on `Vaachak-release.apk`.
5.  If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*:
    * Tap **Settings**.
    * Toggle **Allow from this source**.
    * Go back.
6.  Tap **Install**.

---

## ❓ Troubleshooting

### "App not installed" Error
This usually happens due to a **Signature Conflict**.
* **Cause:** You are trying to install the GitHub Release version on top of a version you built yourself in Android Studio.
* **Fix:** Long-press the existing Vaachak app icon, tap **Uninstall**, and try installing the APK again.

### "Blocked by Play Protect"
Since this is an open-source app not on the Play Store, Google Play Protect might flag it as "Unrecognized."
* **Fix:** Tap **More Details** (or the arrow icon) -> **Install Anyway**.