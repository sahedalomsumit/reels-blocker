# Reels Blocker — Block Reels, Shorts

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-blue?style=for-the-badge)](https://developer.android.com/topic/libraries/architecture)
[![Database](https://img.shields.io/badge/Database-Room-00ACC1?style=for-the-badge)](https://developer.android.com/training/data-storage/room)

Reels Blocker stops short-form video content from Facebook Reels, Instagram Reels, YouTube Shorts, and Snapchat Spotlights — all controlled from one simple place.

---

## ✨ Features

- **🎯 Smart App & Keyword Matching**: Uses a high-performance, low-latency tree traversal algorithm to detect Reels, Shorts, and Spotlight views instantly without sluggishness.
- **📱 Multi-Platform Support**: Granular, per-platform toggle options for:
  - **Instagram**: Reels tab, suggested reels, clips, and reel viewers.
  - **YouTube**: Shorts players, shorts tab, and video shelf elements.
  - **Facebook**: Reels tab, watch reels, and suggested short-form videos.
  - **TikTok**: Complete app-blocking mode (since the FYP is purely short-form).
  - **Snapchat**: Spotlight view and spotlight video players.
- **🕒 Smart Scheduler (With Overnight Support)**: Set active hours where the blocker is enforced (e.g., bedtime block from `22:00` to `06:00` or work block).
- **📊 Real-time Stats & Analytics**: Local logging of block events persisted in a secure Room database. View charts and details of which platforms you spend too much time on.
- **⚡ Battery & Performance Optimized**: Employs structural event throttling (250ms interval validation) ensuring negligible impact on battery life and smooth operation.
- **🔒 Privacy First**: Core blocking is 100% offline. Authentication is handled securely via Google, but no browsing data leaves your device.

---

## 🛠️ Tech Stack & Architecture

- **Core Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for a beautiful, modern, fluid design
- **Architecture**: MVVM (Model-View-ViewModel) pattern with Repository layer
- **Database**: [Room SQLite](https://developer.android.com/training/data-storage/room) for persistent, localized storage of block events and configurations
- **Service Integration**: Android Accessibility Service API (`AccessibilityService`)
- **Coroutines & Flows**: Modern reactive streams for asynchronous settings management and stats tracking

---

## 📂 Directory Structure

```text
reels-blocker/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/
│   │   │   ├── data/                 # Data Layer (Database, Dao, Models, Repository)
│   │   │   ├── ui/                   # Jetpack Compose UI (Screens, ViewModel)
│   │   │   │   ├── screens/          # StatsScreen, PlatformsScreen, HomeScreen, Onboarding
│   │   │   ├── MainActivity.kt       # Application Entry point
│   │   │   ├── OverlayBlockerActivity.kt # Custom Blocking Overlay
│   │   │   └── ReelsBlockerAccessibilityService.kt # Core Accessibility Daemon
│   │   └── res/                      # Resource layouts, XMLs, and drawables
│   └── build.gradle.kts              # Module-level Gradle configuration
└── build.gradle.kts                  # Project-level Gradle configuration
```

---

## 🚀 Setup & Run Locally

### Prerequisites

- [Android Studio Jellyfish+](https://developer.android.com/studio)
- Android device or Emulator running **Android 8.0 (API level 26) or higher**

### Installation Steps

1. **Clone & Open**:
   Open Android Studio, select **Open**, and navigate to the root directory of this project.

2. **Configure Environment Variables**:
   Create a `.env` file in the project's root directory and add your Gemini API Key if required:

   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

   _(Refer to `.env.example` for details)._

3. **Configure Local Properties**:
   Ensure `local.properties` correctly points to your Android SDK path.

4. **Signing configuration (Debug Run)**:
   By default, the project is pre-configured with a signing configuration. If you encounter local build errors related to signing, remove this line from `app/build.gradle.kts`:

   ```kotlin
   signingConfig = signingConfigs.getByName("debugConfig")
   ```

5. **Deploy**:
   Connect your physical device or boot up an emulator and click **Run** (`Shift + F10` or the play button in the toolbar).

---

## ⚙️ Enabling the Accessibility Service

To activate the Reels Blocker on your Android device:

1. **(Android 13+ only)**: If the accessibility setting is restricted, first go to **Settings** > **Apps** > **Reels Blocker**. Tap the 3-dot menu in the top right and select **Allow restricted settings**.
2. Navigate to **Settings** > **Accessibility**.
3. Locate **Reels Blocker** under downloaded/installed services.
4. Toggle **Use Reels Blocker** to **ON**.
5. Confirm the system dialog permission warning (required to analyze active application node structures).

---

## 🤝 Contributing

Contributions are extremely welcome! If you have suggestions for new keyword match terms, optimizations to the accessibility node matching algorithm, or additional feature requests, please open an Issue or submit a Pull Request.

## 📄 License

This project is licensed under the Apache License 2.0. See the LICENSE file for details.
