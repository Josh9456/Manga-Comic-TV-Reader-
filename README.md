# Manga TV

<div align="center">

![Android TV](https://img.shields.io/badge/Platform-Android%20TV%20%7C%20Google%20TV-00E5FF?style=for-the-badge&logo=android)
![Minimum SDK](https://img.shields.io/badge/Min%20SDK-29%20(Android%2010+)-brightgreen?style=for-the-badge)
![Jetpack Compose](https://img.shields.io/badge/UI-Compose%20TV%20(Material%203)-4285F4?style=for-the-badge&logo=jetpackcompose)
![Kotlin](https://img.shields.io/badge/Language-Kotlin%20100%25-7F52FF?style=for-the-badge&logo=kotlin)
![License](https://img.shields.io/badge/License-Apache%202.0-orange?style=for-the-badge)

**A high-performance, native 10-foot couch-reading comic and manga reader built specifically for Android TV and Google TV.**

</div>

---

## 📌 Important Notice: Local Files Only

> [!IMPORTANT]  
> **Manga TV is strictly an offline / local media reader.**  
> It is designed to render and read digital comic and manga archives that you already own and have access to on your TV's internal storage, connected USB external drives, or your local home network (SMB shares). The application **does not** host, scrape, provide, or download any copyrighted content from online sources.

---

## 🌟 Features

### 📺 10-Foot Leanback Experience
* **Pure D-Pad Navigation**: Built from scratch for standard TV remote controls (Chromecast with Google TV, Nvidia Shield, Sony / TCL / Hisense / Xiaomi Android TVs, Fire TV). No air mouse or touchscreen required.
* **Living-Room Optimized UI**: High-contrast typography, deep cinema-black backgrounds, active focus scaling rings, and smooth micro-animations.
* **Overscan & Safe Area Calibration**: Adjustable TV margin safety padding (0%, 3%, 5%, 8%) to prevent screen edge cropping on older television sets.

### 📖 Reading Engines & Layouts
* **Smooth Page-Turn Transitions**: Direction-aware, hardware-accelerated animated transitions (`slide + fade`) tailored for Manga (RTL) vs Comic (LTR) reading directions.
* **Smart D-Pad Panning & Navigation**:
  * Seamlessly scroll around zoomed, stretched, or tall pages (`Fit Width`, `Original 1:1`) using D-Pad Up/Down/Left/Right.
  * Boundary-aware clamping keeps pages centered and advances to next/previous pages when horizontal bounds are met.
* **Zoom Multipliers**: Quick zoom presets (`1.0x`, `1.25x`, `1.5x`, `2.0x`, `3.0x`) in the OSD with transient viewport mini-map indicators.
* **Reading Directions**:
  * **Manga Mode (Right-to-Left / RTL)**: Right arrow advances to next page; Left arrow goes back.
  * **Comic Mode (Left-to-Right / LTR)**: Left arrow advances to next page; Right arrow goes back.
  * **Webtoon Mode**: Continuous vertical flow for long-strip manhwa/webtoons.
* **Display & Aspect Ratio Modes**:
  * **Fit Screen (Default)**: Preserves original aspect ratio with clean letterboxing / pillarboxing.
  * **Fit Width**: Scales page width to TV screen; enables smooth D-Pad Up/Down scrolling for tall pages.
  * **Fit Height**: Scales page height to TV screen; enables smooth D-Pad Left/Right panning for wide pages.
  * **Original (1:1)**: Scan resolution display with full 2D D-pad directional panning.
  * **Stretch**: Fills entire 16:9 display with full panning capability.
* **Dual-Page & Spread Modes**: Seamlessly toggle between single-page view and dual-page side-by-side spread layouts.
* **Smart Auto-Crop (Margin Trimming)**: Automatically analyzes and crops blank scanner borders (white/black margins) to maximize artwork real estate on large screens.
* **Slideshow Auto-Advance**: Configurable hands-free automatic page turning (5s, 8s, 10s, 15s intervals).

### 🗂️ Storage, Library & Networking
* **Local Storage Explorer**: Browse internal television storage and directories.
* **USB & External Storage Auto-Detection**: Plug and play support for USB flash drives, SD cards, and external hard drives.
* **Pinned Bookmarks**: Pin frequently used comic folders (e.g. `/USB/Manga/`) to your home sidebar for instant access.
* **SMB (Samba / Windows Share v2/v3)**: Connect to your home NAS (Synology, TrueNAS, Unraid) or PC shared folders to stream archives across your local network without copying them to TV storage.
* **Dynamic Cover Extraction**: Background decoders extract and cache front covers and `ComicInfo.xml` metadata on the fly.
* **Reading Progress Tracking**: Visual state badges (`Unread`, `In Progress (Page X/Y)`, `Completed`) with automatic progress bookmarking and instant "Next Chapter / Volume" prompts when finishing a book.
* **🔄 One-Click In-App Updater**: Check for updates and download & install new releases directly from GitHub Releases without needing a computer or sideloading tools.

### 🎮 On-Screen Display (OSD)
* Press **OK / Select** at any time while reading to open the OSD:
  * **Live Thumbnail Seekbar**: Scrub through pages with real-time thumbnail previews.
  * **Quick Controls**: Instant switching of reading direction, aspect ratio mode, zoom multiplier, spread layouts, and auto-crop.
  * **Metadata Viewer**: View synopsis, series name, issue number, author, artist, and publisher details parsed from `ComicInfo.xml`.

---

## 📂 Supported File Types & Metadata

| Format | Extensions | Description |
| :--- | :--- | :--- |
| **Comic Book Zip** | `.cbz`, `.zip` | Zip-compressed archives containing images. Fast stream decoding. |
| **Comic Book RAR** | `.cbr`, `.rar` | RAR archives (RAR v4 & RAR v5 supported via native archive decoders). |
| **Image Folders** | Directories | Uncompressed local folders containing numbered image files. |
| **Image Formats** | `.jpg`, `.jpeg`, `.png`, `.webp`, `.avif`, `.bmp` | Supported inside archives or standalone folders. |
| **Metadata** | `ComicInfo.xml` | Embedded metadata tags (Series, Volume/Number, Title, Summary, RTL flag, Writer, Penciller, Cover Artist). |

---

## 🔄 Updating Manga TV (In-App)

Once Manga TV is installed on your Android TV or Google TV, you **never need to sideload future updates manually**!

1. Open **Manga TV** on your television.
2. Select the **Settings** tab in the top navigation bar.
3. Under the **App Updates (GitHub)** card, click **Check for Updates**.
4. When a new version is detected, click **Install vX.X.X**.
5. The app will download the release APK with a live progress bar and automatically launch the Android TV package installer.
6. Click **Update** on your TV screen with your remote, and your app will be updated instantly!

---

## 🚀 Initial Installation / Sideloading Guide

### 📥 Step 1: Download the APK
Download the latest pre-compiled APK directly from the **[GitHub Releases Page](https://github.com/Josh9456/Manga-Comic-TV-Reader-/releases)**:
* **`Manga-Comic-TV-Reader-1.0.0-beta.apk`** (or latest release APK)

---

### ⚙️ Step 2: Enable Developer Options & Unknown Sources
1. On your Android TV, go to **Settings** ➔ **System** (or **Device Preferences**) ➔ **About**.
2. Scroll down to **Android TV OS build** and click the **OK/Select button 7 times** until you see the message *"You are now a developer!"*.
3. Go back to **Settings** ➔ **Apps** ➔ **Security & Restrictions** (or **Special app access**) ➔ **Install unknown apps**.
4. Enable permission for whichever app you will use to install the APK (e.g. *Downloader*, *Send Files to TV*, or *File Commander*).

---

### 📲 Method 1: Wireless ADB Sideload (Fastest for PC Users)
If you have a computer on the same Wi-Fi network as your TV:

1. On your TV, enable **USB Debugging / Network Debugging** under **Settings** ➔ **System** ➔ **Developer Options**.
2. Find your TV's IP address under **Settings** ➔ **Network & Internet** (e.g., `192.168.1.50`).
3. Open your terminal in the folder where you downloaded the release APK and run:
   ```bash
   adb connect 192.168.1.50:5555
   adb install Manga-Comic-TV-Reader-1.0.0-beta.apk
   ```
4. Once installed, **Manga TV** will appear directly on your Android TV home screen.

---

### 📲 Method 2: Using the "Send Files to TV" (SFTV) App
1. Install **Send Files to TV** on both your Android TV and your smartphone/PC from Google Play.
2. Install a file manager on your TV (such as **AnExplorer**, **TV File Commander**, or **FX File Explorer**).
3. Download the APK from [GitHub Releases](https://github.com/Josh9456/Manga-Comic-TV-Reader-/releases) on your phone/PC and send it to your TV using SFTV.
4. Open the file manager on your TV, navigate to the `Download` directory, click the APK, and choose **Install**.

---

### 📲 Method 3: USB Flash Drive / External Drive
1. Download the release APK from [GitHub Releases](https://github.com/Josh9456/Manga-Comic-TV-Reader-/releases) and copy it onto a USB flash drive.
2. Insert the USB drive into your TV or streaming box USB port.
3. Open any TV file manager app, locate the USB drive, select the APK, and press **Install**.

---

### 📲 Method 4: Using the "Downloader" App (AFTVnews)
1. Install **Downloader** from the Google Play Store or Amazon Appstore on your TV.
2. Open Downloader and enter the direct APK download link from the [GitHub Releases page](https://github.com/Josh9456/Manga-Comic-TV-Reader-/releases).
3. Once downloaded, Downloader will prompt you to install the APK directly.

---

### 🔑 Storage Permission (First Launch)
On first launch, Manga TV will request **All Files Access** (`MANAGE_EXTERNAL_STORAGE`).
* Click **Grant Permission** to open system settings and toggle **Allow access to manage all files**.
* This is required so the app can browse and read your comics from internal storage, SD cards, and connected USB hard drives.

---

## 🎮 Remote Control & D-Pad Controls

```
                 ┌───────────────┐
                 │    UP (▲)     │  Pan Up / Zoom In / Navigate Up
                 └───────┬───────┘
 ┌───────────────┐       │       ┌───────────────┐
 │   LEFT (◀)    │─── [ OK ] ───│   RIGHT (▶)   │
 │ Prev/Next Pg  │  Toggle OSD   │ Next/Prev Pg  │
 └───────────────┘       │       └───────────────┘
                 ┌───────┴───────┐
                 │   DOWN (▼)    │  Pan Down / Zoom Out / Navigate Down
                 └───────────────┘
```

### In Reader View:
| Button / Key | Manga Mode (RTL) | Comic Mode (LTR) | Webtoon Mode |
| :--- | :--- | :--- | :--- |
| **D-Pad Right (▶)** | Next Page / Pan Right | Previous Page / Pan Right | Fast Scroll Down |
| **D-Pad Left (◀)** | Previous Page / Pan Left | Next Page / Pan Left | Fast Scroll Up |
| **D-Pad Up (▲)** | Pan / Scroll Page Up | Pan / Scroll Page Up | Smooth Scroll Up |
| **D-Pad Down (▼)** | Pan / Scroll Page Down | Pan / Scroll Page Down | Smooth Scroll Down |
| **OK / Center Select** | Toggle On-Screen Display (OSD) | Toggle On-Screen Display (OSD) | Toggle On-Screen Display (OSD) |
| **Back** | Reset Zoom/Pan / Exit to Library | Reset Zoom/Pan / Exit to Library | Reset Zoom/Pan / Exit to Library |
| **Play / Pause** | Toggle Auto-Slideshow | Toggle Auto-Slideshow | Toggle Auto-Slideshow |
| **Fast Forward (>>)** | Skip 10 Pages Forward | Skip 10 Pages Forward | Jump Down |
| **Rewind (<<)** | Skip 10 Pages Backward | Skip 10 Pages Backward | Jump Up |

### In Library & File Explorer:
| Button / Key | Action |
| :--- | :--- |
| **D-Pad (▲/▼/◀/▶)** | Move visual focus card with scale-up highlight |
| **OK / Center Select** | Open comic / Enter directory / Select item |
| **Long Press OK / Info** | Open comic metadata drawer & details |
| **Back** | Navigate up one folder directory level / Exit app |

---

## 🏗️ Architecture & Tech Stack

* **UI Framework**: [Jetpack Compose for TV](https://developer.android.com/tv/compose) (Material 3 TV)
* **Language**: 100% Kotlin with Coroutines & Flow
* **Database**: [Room](https://developer.android.com/training/data-storage/room) for library metadata & reading history persistence
* **Image Loading & Caching**: [Coil Compose](https://coil-kt.github.io/coil/) + Custom Bitmap Page Pre-decoding Engine
* **Archive Decoders**:
  * `java.util.zip` & `org.apache.commons.compress` for `.cbz` / `.zip`
  * `junrar` (RAR v4 & v5 engine) for `.cbr` / `.rar`
* **Networking**: [SMBJ](https://github.com/hierynomus/smbj) for SMB2/SMB3 local network storage streaming
* **Navigation**: Jetpack Navigation Compose

---

## 📄 License

```text
Copyright 2026 Josh Thompson

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
