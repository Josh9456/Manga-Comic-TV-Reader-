# **Product Specification & Architecture Blueprint: MangaTV Reader**

**Working Directory:** /home/josh/Documents/scripts/Manga App

**Target Platform:** Android TV / Google TV (Android 10.0+ / API Level 29+)

**Primary Input:** D-Pad Remote Control (Chromecast with Google TV remote, Smart TV remotes)

**Primary Use Case:** Native couch-reading experience for digital comics and manga archives directly on large displays without touch input.

## **1\. Domain Research & Technical Foundations**

### **1.1 Comic & Manga File Format Specifications**

Digital comics and manga are primarily distributed as compressed archives containing ordered image files, or as document formats:

| Format | File Structure / Underlying Container | Parsing & Extraction Strategy | Priority Level |
| :---- | :---- | :---- | :---- |
| **CBZ** | Standard .zip archive containing image files (.jpg, .png, .webp, .avif). | Native java.util.zip or Apache Commons Compress. Fast stream extraction without full unpack. | **P0 (Essential)** |
| **CBR** | .rar archive (RAR v4 or RAR v5 format). | Requires Java junrar library (for v4) or native C++ libunrar via JNI (for v5+ support). | **P0 (Essential)** |
| **CBT** | .tar archive. | Apache Commons Compress TarArchiveInputStream. | **P2 (Nice to Have)** |
| **CB7** | .7z (7-Zip) archive with LZMA/LZMA2 compression. | Apache Commons Compress or native 7z-android bindings. | **P1 (High)** |
| **PDF** | Standard PDF document with embedded page raster images. | Android native PdfRenderer API or Android-Pdfium. | **P1 (High)** |
| **Image Folders** | Uncompressed local directories containing images. | Direct Android File / SAF directory listing and numerical sorting. | **P0 (Essential)** |
| **EPUB** | Reflowable or fixed-layout EPUB containers. | Parsing manifest XML and extracting spine image references. | **P2 (Nice to Have)** |

#### **Metadata Architecture (ComicInfo.xml)**

Many high-quality comic archives include a ComicInfo.xml file at the root. The app must parse this file to extract:

* \<Series\>, \<Number\> (Chapter/Volume number), \<Title\>  
* \<Summary\> (Synopsis)  
* \<Writer\>, \<Penciller\>, \<CoverArtist\>  
* \<PageCount\>, \<BookmarkPage\>  
* \<Manga\> (Yes/No \- automatically toggles Right-to-Left reading mode)

### **1.2 Benchmark Analysis: Top Reader Features (Adapted for 10-Foot TV UI)**

Analyzing industry-standard mobile readers (*Tachiyomi / Mihon*, *Perfect Viewer*, *Kuro Reader*, *Panels*, *YACReader*), the following features are crucial for a TV app:

1. **Reading Direction & Layout Modes:**  
   * **Right-to-Left (RTL):** Standard for Manga. Remote Right button goes to previous page, Left button goes to next page.  
   * **Left-to-Right (LTR):** Standard for Western Comics and Graphic Novels.  
   * **Webtoon / Continuous Vertical:** Infinite D-pad smooth scrolling for long-strip digital manhwa.  
   * **Dual-Page Spread Detection:** Automatically detects wide images (aspect ratio ![][image1]) and displays them across full screen width, or pairs two sequential single pages side-by-side in landscape mode.  
   * **Spread Splitter / Joiner:** Option to split wide spread scans into two single pages or join single pages side-by-side on 16:9 TV screens.  
2. **Image Post-Processing & Rendering Enhancements:**  
   * **Smart Margin Trimming (Auto-Crop):** Automatically removes blank white/black borders around panels to maximize visual real estate on TVs.  
   * **Color Correction & TV Gamma Adjustments:** Contrast/Brightness boost controls to compensate for standard TV panel color profiles.  
   * **Hardware-Accelerated Scaling:** Bilinear/Bicubic filtering for downscaling high-res (4K/8K scan) pages down to 1080p/4K TV output without jagged edges.  
3. **Performance & Memory Optimization:**  
   * **Pre-decoding & RAM Caching Engine:** Android TVs often have limited RAM (1.5GB \- 3GB). The reader must cache ![][image2] (previous), ![][image3] (current), and ![][image4] (upcoming) pages in uncompressed Bitmap memory while streaming the rest on demand.  
4. **Network Storage Integration (Crucial for TV Devices):**  
   * Since Chromecast and Smart TVs often have less than 8GB total storage, local file access must be complemented by **SMB (Samba/Windows Network Shares)**, **WebDAV**, and **LAN file servers** (or Komga/Kavita self-hosted server integrations).

### **1.3 10-Foot UI/UX Living Room Requirements & Screen Safety**

A TV app differs fundamentally from a mobile app due to viewing distance, remote control constraints, and display technologies:

1. **Visual Focus Engine (10-Foot Readability):**  
   * **Active Focus Ring & Scale Effect:** Card components must scale up by ![][image5] to ![][image6] with an outer glow/accent outline when focused by D-pad.  
   * **Focus Memory Restoration:** When returning from Reader View to Library Grid, D-pad focus MUST return to the exact comic card that was open.  
   * **High-Contrast Text:** Font sizing must be calibrated for viewing from 8 to 12 feet away (![][image7] minimum for body text, ![][image8] for headings).  
2. **TV Safe Area Calibration (Overscan Correction):**  
   * Configurable UI padding (![][image9] to ![][image10]) to prevent UI controls from bleeding off-screen on older TV models with hardware overscan.  
3. **OLED Panel Burn-In Protection & Idle Sleep:**  
   * Automatic screen dimming after 3 minutes of inactivity on a single page.  
   * Micro pixel-shifting algorithm for static OSD overlays.  
4. **Seamless Continuous Reading (Auto-Next Volume):**  
   * Reaching the last page of Volume ![][image11] presents an instant TV prompt: *"Press OK / Right to open Volume* ![][image12]*"* or auto-loads the next file in the folder.  
5. **Live Thumbnail Progress Scrubber (OSD):**  
   * Pressing OK opens an interactive seek bar with live thumbnail previews appearing above the slider as you hold Left/Right.

### **1.4 Android TV Platform Requirements & Constraints**

To pass Google Play Android TV guidelines and function seamlessly on television hardware:

1. **Manifest Declarations:**  
   \<\!-- Declare app as TV launcher compliant \--\>  
   \<uses-feature android:name="android.hardware.touchscreen" android:required="false" /\>  
   \<uses-feature android:name="android.software.leanback" android:required="false" /\>

   \<application  
       android:banner="@drawable/tv\_banner"  
       android:icon="@mipmap/ic\_launcher"\>

       \<activity android:name=".MainActivity"\>  
           \<intent-filter\>  
               \<action android:name="android.intent.action.MAIN" /\>  
               \<category android:name="android.intent.category.LEANBACK\_LAUNCHER" /\>  
           \</intent-filter\>  
       \</activity\>  
   \</application\>

2. **Assets & Branding:**  
   * **Home Screen Banner:** 320x180 px (xhdpi) banner resource required for TV launcher apps.  
3. **D-Pad Focus Engine:**  
   * **No Touch Screen Reliance:** All interactive UI elements must have clear, high-contrast visual focus indicators.  
4. **Storage Access (Android 10 \- 14+):**  
   * Requires MANAGE\_EXTERNAL\_STORAGE permission for direct access to connected USB flash drives, external hard drives, and OTG storage attached to Chromecast/TV devices.  
   * Storage Access Framework (SAF) fallbacks for restricted external drives.

## **2\. Core Feature Specifications**

### **Feature 1: TV Storage Manager & Network Browser**

* **Local Storage Explorer:** Browse internal storage and dynamically mounted USB/OTG storage drives.  
* **Network File Sharing (SMB v2/v3):** Connect to home NAS (Network Attached Storage) or PC network shares to stream CBR/CBZ files without copying them to local TV storage.  
* **Directory Filtering:** Show only compatible archive extensions (.cbz, .cbr, .cb7, .pdf) and image folders.  
* **Bookmark Directories:** Ability to pin favorite folders (e.g., /USB/Manga/) directly to the main menu.

### **Feature 2: Visual Media Library & Covers Engine**

* **Dynamic Cover Extraction:** Background thread worker extracts the first valid image (or ComicInfo.xml cover path) from each .cbz / .cbr archive in a folder to generate thumbnail covers.  
* **Grid Showcase UI:** Configurable 4 to 6 column TV poster grid with animated scale-up effect on D-pad focus.  
* **Reading Progress Badges:** Visual indicators showing "Unread", "In Progress (Page X/Y)", or "Completed".  
* **Metadata Drawer:** Pressing Info or holding OK on the remote opens a side drawer displaying comic synopsis, author, page count, and format details.

### **Feature 3: Immersive TV Reader Engine**

* **Aspect Ratio & Display Modes:**  
  * **Fit Screen (Default):** Fits image to TV bounds while maintaining exact aspect ratio (letterboxed or pillarboxed).  
  * **Fit Width:** Fits image width to screen width. Allows smooth D-pad Up/Down panning for tall pages.  
  * **Fit Height:** Fits image height to screen height. Allows D-pad Left/Right panning for wide pages.  
  * **Original (1:1):** Displays image at native scan resolution with 2D D-pad panning enabled.  
  * **Stretch:** Stretches image to fill entire 16:9 screen (ignores aspect ratio).  
* **D-Pad Remote Controls:**

                  ┌───────────────┐  
                  │    UP (▲)     │  Pan Up / Zoom In  
                  └───────┬───────┘  
  ┌───────────────┐       │       ┌───────────────┐  
  │   LEFT (◀)    │─── \[ OK \] ───│   RIGHT (▶)   │  
  │ Prev Page/RTL │  Toggle Menu  │ Next Page/RTL │  
  └───────────────┘       │       └───────────────┘  
                  ┌───────┴───────┐  
                  │   DOWN (▼)    │  Pan Down / Zoom Out  
                  └───────────────┘

* **Additional Remote Buttons:**  
  * BACK: Exit reader and return to Library grid (preserves reading progress).  
  * PLAY/PAUSE: Toggle automatic page slideshow mode (configurable timer: 5s, 10s, 15s).  
  * FAST\_FORWARD / REWIND: Jump 10 pages forward/backward.

### **Feature 4: On-Screen Display (OSD) & Control Overlay**

* Activated by pressing OK (Center Select) during reading.  
* **Elements:**  
  * Bottom Scrub Bar with live thumbnail page previews.  
  * Quick Aspect Ratio Switcher widget.  
  * Reading Direction Switcher (RTL / LTR / Webtoon).  
  * OLED Saver / Brightness Control.  
  * Page Jump Dialog (jump directly to any page number).  
  * Safe Area Padding Adjuster (![][image9] to ![][image10]).

## **3\. Architecture & Tech Stack**

┌─────────────────────────────────────────────────────────────────┐  
│                    PRESENTATION LAYER                          │  
│        Android TV Jetpack Compose (\`androidx.tv.material3\`)     │  
│       LibraryGridScreen  │  FileManagerScreen  │  ReaderScreen  │  
└────────────────────────────────┬────────────────────────────────┘  
                                 │ ViewModel Flow  
┌────────────────────────────────▼────────────────────────────────┐  
│                      DOMAIN & LOGIC LAYER                       │  
│  ReaderEngineUseCase  │  ArchiveDecoder  │  StorageScannerUseCase│  
└────────────────────────────────┬────────────────────────────────┘  
                                 │  
┌────────────────────────────────▼────────────────────────────────┐  
│                       DATA & STORAGE LAYER                      │  
│   Room Database   │   SAF / USB Manager  │  SMBJ Network Client │  
│   (Progress/Meta) │   (Local Files)      │  (NAS Integration)   │  
└─────────────────────────────────────────────────────────────────┘

### **Core Technologies:**

* **Language:** Kotlin 1.9+  
* **UI Framework:** Jetpack Compose for TV (androidx.tv.material3) \+ TV Foundation.  
* **Image Rendering:** Coil 3.0 (with custom Archive Fetcher/Decoder) combined with hardware-accelerated Canvas tiling / SubsamplingScaleImageView for high-res rendering.  
* **Archive Parsing:**  
  * java.util.zip for CBZ.  
  * com.github.junrar:junrar or custom JNI unrar for CBR.  
  * org.apache.commons:commons-compress for 7z / TAR.  
* **Database:** Room DB (tracks local paths, metadata, reading history, page bookmarks).  
* **Dependency Injection:** Hilt.

## **4\. Kubuntu Development & Android TV Emulator Environment Guide**

All development, repository assets, scripts, and test data are stored inside the working directory:

/home/josh/Documents/scripts/Manga App

### **4.1 Host Environment & KVM Acceleration**

Ensure Kernel-based Virtual Machine (KVM) is enabled on your Kubuntu machine:

\# Navigate to the project directory  
cd "/home/josh/Documents/scripts/Manga App"

\# Install KVM packages  
sudo apt update  
sudo apt install \-y qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils cpu-checker

\# Verify virtualization support  
kvm-ok

\# Add user to KVM group (if not already added)  
sudo usermod \-aG kvm $USER

### **4.2 Automated Setup & Emulator Launcher Script**

Create a shell script named setup\_tv\_emulator.sh inside /home/josh/Documents/scripts/Manga App to manage emulator creation and startup:

\#\!/usr/bin/env bash  
\# File: /home/josh/Documents/scripts/Manga App/setup\_tv\_emulator.sh  
set \-e

PROJECT\_DIR="/home/josh/Documents/scripts/Manga App"  
cd "$PROJECT\_DIR"

echo "=== Initializing Android TV Environment in: $PROJECT\_DIR \==="

\# Download SDK system images and build tools  
sdkmanager "system-images;android-34;google\_atv;x86\_64"  
sdkmanager "platforms;android-34"  
sdkmanager "build-tools;34.0.0"

\# Create AVD named 'MangaTV\_Emulator'  
avdmanager create avd \\  
  \-n "MangaTV\_Emulator" \\  
  \-k "system-images;android-34;google\_atv;x86\_64" \\  
  \-d "tv\_1080p" \\  
  \--force

echo "=== Launching MangaTV Emulator \==="  
emulator \-avd MangaTV\_Emulator \-gpu host \-qemu \-m 3072

Make the script executable:

chmod \+x "/home/josh/Documents/scripts/Manga App/setup\_tv\_emulator.sh"

### **4.3 Development Keyboard Shortcuts (Simulating D-Pad Remote)**

When testing inside the Kubuntu emulator, map standard desktop keyboard controls to remote hardware actions:

| Keyboard Key | TV Remote Function | Android Keycode |
| :---- | :---- | :---- |
| Up Arrow / Keypad 8 | D-Pad UP | KEYCODE\_DPAD\_UP (19) |
| Down Arrow / Keypad 2 | D-Pad DOWN | KEYCODE\_DPAD\_DOWN (20) |
| Left Arrow / Keypad 4 | D-Pad LEFT | KEYCODE\_DPAD\_LEFT (21) |
| Right Arrow / Keypad 6 | D-Pad RIGHT | KEYCODE\_DPAD\_RIGHT (22) |
| Enter / Keypad 5 | D-Pad Center / OK | KEYCODE\_DPAD\_CENTER (23) |
| Escape / Backspace | Back Button | KEYCODE\_BACK (4) |
| Spacebar / F2 | Menu / Toggle OSD | KEYCODE\_MENU (82) |
| Media Play/Pause | Slideshow Toggle | KEYCODE\_MEDIA\_PLAY\_PAUSE (85) |

### **4.4 Automated Testing via ADB Commands (For AI Agents / Antigravity)**

Antigravity or automated test scripts can execute tests using local files within /home/josh/Documents/scripts/Manga App:

\# Working Directory Context  
cd "/home/josh/Documents/scripts/Manga App"

\# Push local sample comic archive to emulator internal storage  
adb push "./test\_samples/sample\_manga.cbz" /sdcard/Download/

\# Launch app directly on emulator  
adb shell am start \-n com.mangatv.reader/.MainActivity

\# Simulate D-Pad remote keypresses  
adb shell input keyevent KEYCODE\_DPAD\_RIGHT  \# Next Page / Select Right  
adb shell input keyevent KEYCODE\_DPAD\_LEFT   \# Previous Page / Select Left  
adb shell input keyevent KEYCODE\_DPAD\_CENTER \# Select item / Toggle OSD  
adb shell input keyevent KEYCODE\_BACK        \# Go back

## **5\. Step-by-Step Implementation Plan**

graph TD  
    A\[Phase 1: Project Setup & TV Foundation\] \--\> B\[Phase 2: Storage & Archive Engine\]  
    B \--\> C\[Phase 3: D-Pad File Manager UI\]  
    C \--\> D\[Phase 4: Library & Cover Extractor\]  
    D \--\> E\[Phase 5: TV Reader & Scaling Engine\]  
    E \--\> F\[Phase 6: Network Shares & Polishing\]

### **Phase 1: Project Setup & Android TV Base**

* Initialize Android project root directly inside /home/josh/Documents/scripts/Manga App.  
* Configure build.gradle.kts with Compose TV dependencies (androidx.tv.material3) and Hilt.  
* Set up AndroidManifest.xml with LEANBACK\_LAUNCHER intent filter, non-touchscreen declarations, and MANAGE\_EXTERNAL\_STORAGE permissions.  
* Create D-pad focusable base wrappers with visible, animated focus rings (![][image6] scale effect).

### **Phase 2: Archive & Storage Abstraction Engine**

* Build unified abstraction interface ComicArchiveReader:  
  * fun getPageCount(): Int  
  * fun getPageBitmap(index: Int): Bitmap  
  * fun getComicInfo(): ComicInfoMetaData?  
* Implement concrete decoders: CbzReader, CbrReader, FolderReader.  
* Implement PageCacheManager (LruCache to keep maximum 4 decoded pages in memory).

### **Phase 3: TV File Manager & Storage Explorer**

* Build D-pad navigable file tree browser with overscan padding options.  
* Implement drive detection (internal vs USB/OTG storage).  
* Request and handle storage permissions gracefully via TV remote prompts.

### **Phase 4: Media Library & Cover Art Grid**

* Build background worker to parse directories and extract cover thumbnails.  
* Build TVLazyVerticalGrid displaying comic covers with focus scaling animations and focus restoration.  
* Integrate Room DB to save reading progress per file hash/path.

### **Phase 5: High-Performance TV Reader Screen**

* Implement canvas/image viewing engine with D-pad navigation bindings.  
* Implement aspect ratio transformation matrix (Fit Screen, Fit Width, Fit Height, Original, Stretch).  
* Implement RTL, LTR, and Webtoon scroll modes.  
* Build OSD (On-Screen Display) overlay triggered by OK button with live thumbnail seekbar.  
* Implement Continuous Next-Volume prompt and OLED screen dimming logic.

### **Phase 6: Network Support (SMB) & Refinements**

* Integrate smbj library for LAN / NAS network share browsing.  
* Add auto-crop white margin algorithm.  
* Add customizable slideshow mode for hands-free TV reading.

## **6\. Prompts for AI Development Agents (Google Antigravity / Cursor)**

When executing code generation tasks, ensure all agents are instructed to operate inside /home/josh/Documents/scripts/Manga App:

### **Prompt Module 1: Project Architecture & Manifest**

> "Act as a Senior Android TV Developer. You are working inside /home/josh/Documents/scripts/Manga App. Set up an Android TV project using Kotlin and Jetpack Compose for TV (androidx.tv.material3). Configure AndroidManifest.xml to follow all Google Play TV guidelines (declare touchscreen not required, Leanback launcher activity, xhdpi TV banner placeholder, and MANAGE\_EXTERNAL\_STORAGE permission). Create a clean architecture package structure with UI, Domain, and Data layers."

### **Prompt Module 2: Archive Extractor & Memory Caching Engine**

> "Working in /home/josh/Documents/scripts/Manga App, create a Kotlin interface ComicArchiveDecoder that abstracts extracting page images from standard comic formats. Implement concrete classes for .cbz (using Java Zip File) and .cbr (using Junrar). Include a ComicInfo.xml parser. Implement an LruCache manager (PageCacheEngine) that pre-decodes pages ![][image2], ![][image3], and ![][image13] into memory while preventing TV Out-Of-Memory (OOM) errors on large images."

### **Prompt Module 3: TV Focus System & Library Grid UI**

> "Build a native Jetpack Compose for TV component named TvLibraryGridScreen inside /home/josh/Documents/scripts/Manga App. Use androidx.tv.material3 components. Every comic item card must scale up by ![][image6] and display a glowing accent focus border when focused by D-pad navigation. Implement focus restoration so when navigating back from reading, the focused item position is preserved."

### **Prompt Module 4: TV Reader View with Aspect Ratio & D-Pad Control**

> "Inside /home/josh/Documents/scripts/Manga App, build a native Jetpack Compose for TV UI component named TvComicReaderScreen. It must listen to D-pad key events:

* LEFT/RIGHT arrows turn pages (swappable based on RTL manga vs LTR comic mode).  
* UP/DOWN arrows pan zoomed images or scroll in Webtoon mode.  
* OK/CENTER button toggles an On-Screen Display (OSD) overlay with live thumbnail scrubbing bar.  
  Implement aspect ratio modes: FitScreen, FitWidth, FitHeight, Original, and Stretch. Include auto-transition to next chapter when reaching the final page. Ensure rendering handles 4K images smoothly on TV hardware."

### **Prompt Module 5: Environment & Emulator Test Scripting**

> "Write a Bash automation script located at /home/josh/Documents/scripts/Manga App/setup\_tv\_emulator.sh for Kubuntu. It must use sdkmanager and avdmanager to set up an Android TV emulator (google\_atv;x86\_64), launch it with host GPU acceleration, push a sample .cbz comic file from /home/josh/Documents/scripts/Manga App/test\_samples/sample\_manga.cbz to /sdcard/Download/, and execute ADB input keyevent sequences to verify D-pad navigation."

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAZCAYAAAB3oa15AAACOUlEQVR4Xu2VTUhUURTHtSEriEhjgpiPN19gzCKQ0dr0SW5EgrZpKxPBlkEFFX4sRMEW7dpF0SIIpoGIFomIuHHRoiCYloGLICRom0T9ju8+uXN8z5k3kyDy/vDnvnf+/3vuOfe+mdvWFiHC3kcul0szxHQ8CIlEIuk4znv4C1bT6fRsMpk8on27imKx2MHiPZlMZopxPZVK9WqPH+Lx+FH8X+EojZyg+Ps8/yXPW+3dVbDomtnFBSmg0QYoeAL/jB3j/ZXkQLtmx7eAOAhX6PKS1loFi94L0wDed/An/gtWbNicwgvbWwPEy5iWxAQzWm8WTTRQET8c8WKUc93EXtteX2AagqtwXr5BrYdF2Aby+fxJCr7B4wEvxvxH5gSmLevOYOHzTHoGH8u/gtYbRdgGfBBj/he4Dk9psS6Y1APL8GU2mz2j9XpotQHmjsIN8vRrLRQovptEb2CFozys9SBYDfRprR5Y57Tj3gXDWgsFiugk2RyJfjjuX1y79gSh2QZkTcf9dG5prWHI7UeCu/CTXCi8d2lPPXgNcIJntRaEUql0kA37IGt7MWlI7gjbFwiTYJwEn+GdVq5xrwHyndOa2SDJn7DjeJ8w76kdYwMu4p23Y9tQKBQOOe6P5iN8yPsx7QkL2TVpgPGqj7bZHCxbsZsmtmgom/gd/kEbs+fXAMMIrMIH/6nw5+T6ZooR/ia2zHjb8/A8YArb+jQc9w7y5tTQbxM2gXgFcZKjO661CBEiRIiwr/EPWzGfWBNSbyUAAAAASUVORK5CYII=>

[image2]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADYAAAAaCAYAAAD8K6+QAAABw0lEQVR4Xu2Vv0vDUBSFK4iLPxChRLA0DRQinZwEi4g4ODg5O+niH9CuLi4ubsXBQXAQBHFwEEFx0kmkIDhUN7soCGK1ootD/R591XgxlkZ8KLwPDk3OvY+8k5smsZjFYvkJrut60vu3+L7fnUqlxgm1hiqyHgrNWXSIrlANncieZDK5j/+q6zdcaFH2/AZcZ4jr3aJNVEIPsqcpbP6AhWW1ec/zhmUdP0fPuvRNwfV3Ww7mOE4niy64Q7N6KjuyB28FTUjfFJGCMaFJFhUymUxHYGp+sIfQp6oe9EwSKRgLltn4tD7O66kVGnVCcurufawwT9RgxXQ63aOO4/F4F+d3qErYXuXx35rnPPd5lVlaDsY0HAIcBz2CLOmp5dU59S31hgr2mEYHe5R+KDTPoIWgR4h+vBd0rb4j/F4G69/BTVl165+PplJPglwfhhshmPrwZaWvN1hDG0qybhodrCr9UGgu8dMufaY2qIPVOJ6TddPoYE/S/xIap9AZh22ypqC2rYIlEokBWTONW398n9U3V9be4TEbpanYmAgq8xIZk334I0zrXPqm4Ib2sdcj9lEJ7PVehVQZZL/FYrFYLJY/yBuRWoYsyJCcxgAAAABJRU5ErkJggg==>

[image3]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAaCAYAAABVX2cEAAABRUlEQVR4Xu2Rr0sDYRjHJ4jFOSzHCR73Aw5OLpkGDjEYFkzmpVn2B2zVYllZG4aFgcEkCyZBsS2JDATDZnPFpSWDxTA/L7zTd8+d5gn3hS/3vN/v93mf5+5yuQzLA8/zSvAevsIZfJAZ13Vv0T+1P/F9/0xmFkDDHcGxagiCoCh99DqZC6knYNv2OuERE6t6+rXMoJ3DQ6knwCZlgu04jteM7SIzw6BH5ZtaKmhuET7WdUNv1577XMzRu/np+AMEB2EYFlRtWVae8xS+M2BTaXyrGuf6YlcKmGrT1Dc1mpt6u4Y641/BXTOTChoq8NTUaNxC+4BvURRt8Hwx/V9BsAtLUme7jt7uUlH6qSA45LEqdbbb0ZfNqE+knwDBI/hEuSI9BbyeusxxnG3pfYNX2Cc0mE+GY37Egcyh77HVs9QzZPjX+AL7dk/I+L9wDAAAAABJRU5ErkJggg==>

[image4]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHcAAAAaCAYAAACNU8MOAAAEs0lEQVR4Xu2Zf2hVZRjHbzb8QaUWzAnb7nv3QyZTTFtJjX44MxNF8J8USyhR+kNMQZsUilhaIIaQDOmPUEsKZTADFX/cP7TQiKVMClRUUIKEVFxuaKiIfh7ue9jbs3t33nN3nZucLzyc+36fH+/znOe857zn3EQiRowYMfotUqnU1yUlJU9pPkY/hDGmQnO5QGOHYn9U8wMB5P4G0kr+nfa4SNs8FqipqXmGAqdQ4LdIu9bnQjKZnIvfp5rv7yDvOvI+JzWXlpaWMd5F3feRj7VtKHCqR9LIRRvkN23DBAfh71r9ZSb+TNv0BHwaKyoqJmg+DMwzEd9/kB+R08i/2iYXsN0jJ0fzxcXFTxP3MPo2W8+t6urqYtcGbj1yw+o7kH2uPgzEfxmfJZr3AX5p/N8OxuQ2xGR60y1Pb9DAQwS4JAXRiMlaD78Cm+2a9wG+n0vBmo8CYuw1ns0lz2exPaJ5F+hXB/WaLKuivLz8Rfg2uXNoXRiYfxqySvMeGMScncj5ysrKEQFJrG9sntFvz7LpwPEMDfjABtmjbeCakKma9wF+G/qyuXISOCHLNO8CmyNcxK9zvIdclhXi6sl3HrLO5XxB3Ol5NreIXK5KD8rKyqoDkvFm4Yj5kWvsBUkG5y21tbWDTdfqrXFtKLRV9C7nC+J90cfNTVdVVY3SfAB7MZ+U3xx/kHqRxa4N4yZpvsv5Qm6reTZXevG8UYvIPkakuW+6vBdw3ESAOfb3SlvslkDPhAzN/i6PaDB92Fx5zmJ3QPMuOEmzsdksvzm+YOs95dow/pNDkcv5ojfN1SAPOfm3kWMMn9D6UOB4gtvScPktGw7G15AOkhwpHIl+yHjF/738Yfqwudg0MtdCzbswmdvczGBs9xuyMqbJGP+xJsujyRcFbu73xPor2+YwFKzKEpL5xeUI9qUUi6yUMfrdyETXJhvwe8f6+coduZh0nGwwmebe0LwGNr+GbYKwaXPnpbYZkg/5/2T1S8Oe2QL8RmepqUch7ls6Ti4Qfz4+19ncjdc6L+D8LrLG5WzSt5C/5URxPOvqo8IUbuX22FzZhGDTrHkX2JRik9Y83Al78us4tnAcp218UYiVa/O4SGNf0jpvmMzHgXrNJ7u23ztFtD4KCtjcDs27IOe1cvfQvAvyeB/5RPPEfs/W24Kc1voo6G1z2QyWk8MFGvtawDFuSNl9kTdsId02DqnMc0eKvZ8KeYaFoYDN7dS8C/QnmWeo5l1gszPbe3wi8xpywda8TSujoDfNlfyRVmS+y5PTamLOdrkegcNMpC2RYxeGrlmKlVuZ1kVBgZorX9Fu5vojAN0k5vhO8y7siriS60uPPGelXo4LtC4KetNc/HaQQ7ut9yhyxmQ2uLLIxmr7biDAq8Y+Y6xcyvZOB/8KAf/QfFSYPJvLRfUcuf5siw1yvY6kpQbXNpnZBM5yuQCyb7Bx/rMx2hkv13b2/fcKuY7WuijIt7nyxuLU2U3y/c7wUGHybG4UMMfvHJ7U/KNAvs0dkKDQcazCYZovFOTCoblbNf+oIN+FqXeM5mPkARr7FdKg+RiPAWjs8USOTWGMAQz57s1teaPmY8SIESNGjBgxHiYeAJnrdIeBSMePAAAAAElFTkSuQmCC>

[image5]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACoAAAAZCAYAAABHLbxYAAABsklEQVR4Xu2Wvy8DYRjHr1qxCCG6NG2vLrWxqIGEgdEkFsEmsQmizCJ2k4FZCJNfY9NJBwmThMTgDxCjiEQi9XnSnrx9NNc2ega5T/LN236f573ne+3l7iwrIOCbkG3bvdqsB8dxOuPxeLf2m0oikYilUqkpQubQqa57ECJcXzKZXGTfA1rTDU2DIcuEvGPILvpoJCj7TugvoANU9DWoCYPeGgnqQuDhfxOU2jRLSPsu1CczmUyr9qvic9AltK19AT/LMfYtjxOpwM+gAvUdtKC8CXTJx7Dpe+J3UGih5wjNyxfWEZRLp9MdutETCcpd4Ez7tTCCZnWtChFmHKNN+vOsXbqhJn8UVOaMoxdmrehaXUhQhp5rvxZG0HVd0xBulL6bWCzWw3qI5nRPTcpBL7Qv8PQaozarfcEIuqFrJnbpmnwkrFO2wnbpYVF/WC7oNja8o7z18zYh7wCv5TCDqub+lUUCb+maCyc6JCFZ+1UpIv8imlF+JXbp9nBdDilBRE8oF41G242+K/Rsvnhw8FW8e/Rp7L3lF9tze1zw8+wd0L4gc6gX5MVG1wICAgICfs8XubmAhtobB+YAAAAASUVORK5CYII=>

[image6]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADUAAAAZCAYAAACRiGY9AAACeklEQVR4Xu2WT4hNURzH35uhSZGSN5n37973R2/DYuZZ2EgmpUhmSdaaxYjJn4UpyU4WkhALEStjwSiryUgUiymlWBAbUbYkpcTn272nTj9v3txXc0Xdb33rnO/ve+453/PuPe/kchky/FPIB0FQs2IS1Ov11eVyeY3VBZ55slQqlV2/VqshBdO+Z8lRqVSKYRiOMdEsvGvrXZAnyPpqtXqAca/hEWsA/ei/DH/CCWtcMrCgQwR6ySQX4Y9eQjHuNv6n8Fa82E6hlqF/i+f4BKdp77Cm1KDJewnlwCI3dwnVR/2NFf8aUgqleqJQvDXbW63WKqs78Pxhvt2q1bsirVDob+EEfAA/4J9pNBqD1qfnqNZut5fbGt/9FkI/1oFka12RYqgvcJfaHCwraD9TQOsTeNYYi79OM+9p6/A/h0OeNRnSCsUub/D7+MflDxc4MKgdhJdp5pvNZoH2HJux0foSQaHYpXtWXwxeqKO21gn4dsb+S7bmwDMn4VU8j+CwrSdGGqGonUH/yE43PW1r7F/wD5jaUBD9992h22friaFQTDhj9cXghTpma2hzcW3Uafj3xNpZ3+sQRt/QPByhPc5G38h531hPiEPdt7qg04faPqsLXqjjtoZ2jkUd9jX8J+TXL+brgk5Fai8Ys9tpQXRy6lXtLRgf5AADv8OHuT8H6074NV74iKlp0m3xIk93qOk1mtVVTP1isbiW/nt403p1d1QguN/W0KbgBat3BMbRIDouFUiLFt9pIYVCYaXnewI/+5dWQkyivQqiu5wbO88uX3EegUCb9AYE0auo+inkft8j4DkP91rdgbHX9Adt9QwZMmTIkOF/wW/3Bb6ajoWqFQAAAABJRU5ErkJggg==>

[image7]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAC0AAAAZCAYAAACl8achAAACzUlEQVR4Xu2WTaiMURjH57qJfJfGZL7OzNzRyFcxkthR1jYsbGwkG5TiUhSZhcItXRsfGyxuXPJZYq6V1KVcRSyIZKXmxt3chCZ+z7zndI/H+05ZGFPmX//Oef7P857zf8+cc96JxTro4I/QZYzJa9FiErmD8C68nsvlLmSz2cW6qGXIZDJJTGzETFUM6byA/BF4gG6XxBheS20tnU4vUKV/H0y+CzPPMXAafgsznUgkpqN/0jraRVjRekuBgfEw07Ka6D/4RZb4Oi97Cr3P11qOKNPJZHIa+lc4ms/nN4iG4TnE7+AKXd9SRJkWoJ+V1YZ1WIFDGN+h63ykUqm51Bw2wVl5Cm8T35Qc7X5ZBBmTLbqb9ji8AmtwkGfTerxQmCam7b6+KpNYvoHLdZ0P8ofgCRdjbhPxiPQxvZB4px3rhTNJPw/fw1fxeHyGezYSpolpJlhkghXbYoLVkMnG2C7LdK2DCVb3l4NKPOT6ckZkHLkMVM02q/f6eijENIU3tN7T0zMP/QMGSxKzSlOp7bPGh3W9A7nzdvLHtIO0m/283PNhppkvY8e+4+uhiDKNfhReC9H74XfZOjonKBQKs8lfsgYc+11etoho2jTotrWNrdQUFI27g6L0AfRjWreT1kul0kydE5Df6vpcm0upPSdm6BdtPtQ0ccGarvp6KKzpWyG6rPTDmP0aOjD4evRHvuaDsV6z2lkXl8vlySa4OtfYfMM03DvxVGPc7Vbf4+u/oVgsTqHoC3wQU+ZkT6OPMMll9rURjXaV1Jom97SYJj9At1tiDt5K4lHmmmXzzvRn+qtFk4NNXJNzIJ684SZAwTo4bA3LAMK3sOpfOXZ/nmTwJyYwKwdxvj+WBhPfk5+eZ+5LX17aeC/pTNOeoX0Jn8GPsBK15f45nGm9p9saUVdeW8OeC9ke8pe3/cHq7sPwmJiG9bDvQwcd/A/4Ceus49kJwbTHAAAAAElFTkSuQmCC>

[image8]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAD0AAAAZCAYAAACCXybJAAADGElEQVR4Xu2WS2jTQRDGq/UJPg6aS17bpIFIfIFREUF8gYIgeFHwJKh4s4JaH4ggpR5E8SKCooei4KMVVBTBxoMHQVpQD6JCFSqehIoVioJC0d8ku2aZpib8LTQp+WDYnW9nZ+fL7n83DQ111DHx0dTUtBbrNcYM2XaPjtEgpi0ej3doviZA4VmE9mHrIpFIFP8Wgn5jh3WsA2MJ7Dt2XY/VBCg8h+DNzk+lUtPh+rEf9EN+rANjd7Hh8RbN+q2JRGKJ5sthMhOHsPfJZHKuI9ntS3a3RxxzFtkEf7Eadpr129iwVZovhylMHBCB0Wg05Uj888Ihfr8fDBrhn8sJqBLR7UFEy84tZfIGnyNRtxW90efx98Efkn4lorkj5pHrFHE57AX2AP++jNEew/9i1zlAexbrNIVN6JL7RefTIO50INEapnBJ/cSe4U5yPLs7h+J6stnsVBtXVjTjJ7Fzzmf+dvyX0qfYBXKSRDT22om063/E3oZCoVlubimMpehrFPNJ/9IkP4Nt8+IqES272664J64fi8UWiWjWa1Exey1/1Oc1xkQ0CXaS6KsU4/N8AtDmkc9VKPqqLb6Htot2hz+Ov7CU6Obm5pjw2EMbJydE/ErtV7lTkgeJswT3I3iFHoPvZDyjuLKi5UWQGFXQBTcuR1w4LbqhcGFKbP5TGA3mf3ba/rIfELzGcfjr3XE2BYF+4dp2F7MVwfxdrs/rsJi4KxLvXorRROMnbd6cz2uYoKKZNAPrlaPt8yQ8weJbfc4hHA7Pt0X9c6fJ2cdux50vl6ApXJKr7XheNNZanPX3lRD+oM9rBBbNAh1MHpRfFXuKvTP2KZGidLyAExG2Rd3QYz5ENDE36TaKz7zlklteAjvuRA+64uUfFv6A3APy79BLNwKBRMvidtGSlslkpuk58HdM4UdycXJDb9FxAgp/LEeXwrqlT3ub2GVu3ImmvUz7BnuFfcba0+n0bD9XKZggoscbTrT+pitFTYoe7cmqFDKfS3Gm5qsafL8r7fE+rscmJNilIwj+Zu+FYfx7OqaOOuqooxz+ABjgGbn0O2LlAAAAAElFTkSuQmCC>

[image9]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABsAAAAZCAYAAADAHFVeAAACcklEQVR4Xu2UO4yMURiGx1q3ZMUGw2Zu/9wYOy7NEBHZbLKNkBAqjUSFtb24ZFkFViMKFCQbxHWLXRIJYSUKRBQaNNhCp1ErNON51/nGmfOPCJ1k3uTLOe973u9833/+//yJRBugVqvNiaLoGHErn89vCdcN2Wx2B+u7Q70BFnvYZIx4lsvlXhP7Qo+KEMPpdHoJ4xQ515lnbL1QKKxDO8vadKVSWejnNpBKpZZieotpSLxUKmWZf0I7aZ5isbgSrV4ul5PizAeISZoaJa4xf0E8JN7Dt1teDBjOqZivwQfRv+roxNlgj4rZOsUXwR/8ypjx1PS0vhYDSZ8xjQeaOq9z/n3iegeuWId4Mpnsgt8zf7VanQt/w1EuNy0G967qjJd9XV1KJ4adb5W4jlxczeA5Y374afh+4y1B5+vdphd9PZPJrJXOBldNg09Q9CjTTuaP8KyQjmc1/CnTWeZtCZL7WxVjg6rTb3rebvht1h4zDpiuwkSv8d8C06ZWxZQs/U8v3L3L48bVBM3cdyfQ/KS80IordsnXOd410kk87+s+dJfY9AnXYZ44/q3EK67OMsYhck80JeiFu2Jjvk6xDU5vdB2CtQtsuNnj+iEc9vhLmzdAdx9ZmPA116WKbfN1Azkbo+bTmA3/jn7ABPid2FVAPEV8SHhnDD9EfLFLHUBf45Tumqd1oH2j2KAJKgbv8TwzXc7XIzPuFOeTXqCn1V+jyejA2hHz+mCP5+7DMK5jjF8HCizWJrmf/7krxK7QI3AsEZ67oS6oAfLeMXYTe/GNhJ6/ApvdoLF0qBsochDPNIVGE+7X9s/QDzjU2mjj/8MPmdCfEAZBI10AAAAASUVORK5CYII=>

[image10]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACYAAAAZCAYAAABdEVzWAAACz0lEQVR4Xu2WW4hNURjH94y7yPW4nNs+N52cpDjiUSF5lCgllElCoSle3B6loYY5efHEuD/MYNTIDKnhNKWMPPAkDygPnkya8nL8vs5aZ75Z9p5LUdT519dZ///3rbX+a6299j6eV8c/gEQiscb3/XvEqWg0utDNCyKRyCzyV2hOcnNhaKBD2hUtksnkbvLdRFkmT6fTvpMvor/LZDLJVCq1lfYnYptnDIgharajvSZ/TPcNBKuMmoF6iE43L0A/RLzJZrOLhDPBGfhXy03Ndca5pHiv6ddOdBD95O/yW/bG2i0mOELxW4pLxM8gY+zMYvQhancoudGv7shxK9B+T5xW/AJjr7PcaLcYb6XWxgSdfgQZQ2siKu6AaL2YfWy5LBB+VuUvyjOneBP5c5aPG6MYk92scGwJR5fjGcTQdMPbabfaPO0nPG9zTHsJ+VfxeHyGzY8boxjrFGMyuNZZ/R3R2ZWscDk2+MdYLLaA3EbaV22tmEbbNNx7AggzJisPMoZ2U3RiuardCX9OlOxO8rueuDbcc4IIM4b2KMTYDdE5nmVadzCZmmf2nZbP52fDW8xur3KLAyHG6HA/QJfrLjuz1NFviy5Hp3UN8s3y/jNU3pOym/vMTe8jN29EhyCEGWOnWo2xES9fqRWdZqPWLaSe6LBcnjH4oGfqxTBjH6x1CIMYo/CBq6PtNcZWax3+gnipNQ1yXfrrAD9JfLacS7MC3mZ5KIyxh64u203uO797rFYoFKaifUM7qmstyO0imrVG7Qm0L5YbYyVd8xtyudw0ioaIp9AGN5+qfrJqnxIm2Q8fsDdPA20uuW7P+eygb5HFW10WCj+sa2ogsYHoN6YqJj4QPfLR1bUcy2b0Fr96GS5zG+frvAW5NiYtunqxWJxCrozBA9zSmbT7wsb448D8WiY+7+oW8s8DQ13EQJD5vwY5WnksXL2OOur4n/ELhqzI8wjSxc0AAAAASUVORK5CYII=>

[image11]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAaCAYAAABVX2cEAAABGklEQVR4XmNgGAWDBygqKsrLyckdlJeX/wrE/6H4rIyMjCpIHsiejST+HqQWKMyCZgwqkJWVVYZquA3kMiGJSwHFngANiUMWJwiAmtaBDFRQUAgA8VVUVNiB7I1AbI6uliAAGuQEdd0+KSkpLiC9Bugia3R1RAOgAVehrjsENMgVXZ4kADQgF+q69ehyJAEtLS02oCFrgfgTEP8ExqgQuhqiAMwgoPfiga7rh7ouB10dQQCKNaDG1UAcDOIDDdSAGnaZgZTkYGxszAoKH6ABHsjiQNftBBkIFI9AFscJlJWVxUAGAXEvuhxQLAjquvPocigAaGsIUNEZIP4F1fASKGYDkwfyc4D4PVTuP1DuJJCeimzGKBgFQxYAAO3nTEBzF9vzAAAAAElFTkSuQmCC>

[image12]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADYAAAAaCAYAAAD8K6+QAAABz0lEQVR4Xu2WO0sDQRSFg/jAVyPEQEiyWRKJRBAhhYIWIgiW4gOs9A9o6R8QLcXKSmsbH0GsRBC0ExQL0cbWxsZoJIVp9BucyHJR446BVZgPDtk9d+7snBkYEgpZLJbf4DiOK71/SyaTaU8mk8OE2kQFWf8S13WdRCJxQlMJvWpdxGKxLlXnecPjF9RY7HoxzbfQt8h3eqVfDQL10XuPttANepRjqhKPx1N68be81nn8KN4dgWa9vh/oX2KRA9L3A3McGAVT0LinwrGIcfWeTqebeN5H/XKsH5hzOehgI/rUjqPRaAu/O5zUoBznF+ZZCTSYguZrfWqnhBqVdRP+RDDCLOhTy8uaKYEHy2azjTTvoiJ64WbskGO+g02Z1pvyU5XD4XCbnOcznPdgT9KvSiUUOzvHAtf0h+flOBOc2p2Yv2Dq9qNpG02qdxbRrYNdhQyveC81DFaU/pfkcrkGGvJ8eMzrc2qHKhz+jNc3oYbBnqX/KalUqlOFQquyhjehT+1S1vxSo2BHqBSJRFpl7QNOY4pB56isF3+PN1Sp8z6PCrr2Su2M33XvHH4wDaYuL/1372Mt6AEdedcbGKbB/jzsbg+73yx9i8VisVgMeANzEJnGrGLCkQAAAABJRU5ErkJggg==>

[image13]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADYAAAAaCAYAAAD8K6+QAAAB8ElEQVR4Xu2WzytEURTHKSn5kdT0lGnevJp6GiUrIUkWFlY2NlZs/AEjOxvCxm6ysFAWSsnCQoqsWElTygI7NpSS8SMWLPjcPDynGfPumAx1P3Wa977nnHfP994xT0mJwWD4CbZtO1L7t7iuWx2NRrsxtUCkZT4rFHcQ28Qp8ULsyZpIJLKJ/uzlL1hoQtZ8Bz1jjuM0Sz0XrNNC7yWxTBwRN7ImJwy/ReOZGp4hWmUePUHNotSDQO8kQ7ZJXQeesa5tzLKsSpqOWXzIO5U1WYM2R/RIPQj0TRXFGCfUS1MyHo+X+07N9dcw2L7K+7Wg8LzpohijYZaF+73rUe/Uku95THJrb3x26FFMY6lYLFajrkOhUBX3V8Qdw9Qqjb+tEe4TX7uCUxRjnIbFort+DSMz3qmNqnvyK+oXyl+TCfoGvL6g8aQ2Uj4nE/absVupZ4XiQWLcr2GiHu2ROFfvET5P/Hld7MKdmJYx9eLrkDq7P+/t6pIKmdehgMbupJ4Vio/4KJM6gzR6xl64HpZ5HQpo7F7qGaGwjzjgslTmFORWlbFwONwgczoUyJj67+hBvXNl7gO+Zp0Upd5PhDjjR6RL1qG3M9Ch1HXJ1xgbWsesO/SnfbNeK5PKg6z/dfI19udhd5vY/QqpGwwGg8GQB68u8p1OqfLI+gAAAABJRU5ErkJggg==>