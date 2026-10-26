# BOOTV ⚡

> **Ad-Free Native Android Video Player with Biometric Lock & Gesture Controls**  
> *Created by BOOTLOOP*

---

## 🌟 Features

- **Ad-Free Streaming:** Built-in ad-blocking heuristics, popunder neutralization, and aggressive tracking script suppression.
- **Gesture Video Player:**
  - **Brightness Control:** Vertical scroll on left 40% of screen.
  - **Volume Control:** Vertical scroll on right 40% of screen.
  - **Fast Forward / Rewind:** Horizontal swipe or double-tap left/right for ±10s seeking.
  - **Pause / Resume:** Top-bar controls or double-tap center.
  - **Playback Speed:** 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x cycle button.
  - **Aspect Ratio:** Fit, Fill, Stretch, and Zoom modes.
- **Biometric & PIN Security:**
  - Fingerprint / Biometric authentication.
  - Custom in-app security keypad.
  - Instant lock toggle and password removal.
- **Full Edge-to-Edge Display:** Supports display cutout and camera notch areas with immersive sticky fullscreen.
- **Direct Developer Contact:** Integrated Telegram pill redirecting to [t.me/BOOTLOOP1](https://t.me/BOOTLOOP1).

---

## 🚀 Build Instructions

This project is built using native Android userspace build tools on ARM64 / Termux / Antigravity:

```bash
# Build the APK
bash build.sh

# Clean build artifacts
bash build.sh clean
```

The output APK will be generated at `BOOTV.apk` and copied to `/storage/emulated/0/Download/BOOTV.apk`.

---

## 📱 Tech Stack & Specifications

- **Package Name:** `tv.bootv.app`
- **Target SDK:** Android 14 (API 34)
- **Minimum SDK:** Android 7.0 (API 24)
- **Language:** Java 8 / Android SDK
- **Architecture:** ARM64 (`aarch64`)
- **Signing:** APK Signature Scheme v2 & v3
