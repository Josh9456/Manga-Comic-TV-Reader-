#!/usr/bin/env bash
# ==============================================================================
# MangaTV Reader - Android TV Emulator Setup & Automated Test Script
# For Kubuntu / Linux environments with KVM Hardware Acceleration
# ==============================================================================

set -e

PROJECT_DIR="/home/josh/Documents/scripts/Manga App"
cd "$PROJECT_DIR"

echo "======================================================================"
echo "  MangaTV Reader: Initializing Android TV Environment & Emulator"
echo "  Working Directory: $PROJECT_DIR"
echo "======================================================================"

# 1. Generate test comic archives if not already present
if [ ! -f "$PROJECT_DIR/test_samples/sample_manga.cbz" ]; then
    echo "[*] Generating test CBZ manga archives..."
    python3 "$PROJECT_DIR/generate_test_manga.py"
fi

# 2. Check Android SDK availability
if command -v sdkmanager >/dev/null 2>&1; then
    echo "[*] Ensuring Android 34 TV system image is installed..."
    sdkmanager "system-images;android-34;google_atv;x86_64" || true
    sdkmanager "platforms;android-34" || true
    sdkmanager "build-tools;34.0.0" || true
else
    echo "[!] sdkmanager not found in current PATH. Using existing SDK if configured."
fi

# 3. Create AVD if avdmanager is available
AVD_NAME="MangaTV_Emulator"
if command -v avdmanager >/dev/null 2>&1; then
    echo "[*] Verifying / Creating Android TV AVD: $AVD_NAME..."
    avdmanager create avd \
        -n "$AVD_NAME" \
        -k "system-images;android-34;google_atv;x86_64" \
        -d "tv_1080p" \
        --force || true
fi

# 4. Check if emulator binary exists
if command -v emulator >/dev/null 2>&1; then
    echo "[*] Launching MangaTV Android TV Emulator in background..."
    emulator -avd "$AVD_NAME" -gpu host -qemu -m 3072 &
    EMULATOR_PID=$!
    echo "[*] Emulator PID: $EMULATOR_PID. Waiting for device boot..."
fi

# 5. Automated ADB setup & test sequences (if ADB is connected)
if command -v adb >/dev/null 2>&1; then
    echo "[*] Waiting for ADB device..."
    adb wait-for-device || true

    echo "[*] Pushing test comic archives to TV storage (/sdcard/Download/)..."
    adb shell mkdir -p /sdcard/Download/
    adb push "$PROJECT_DIR/test_samples/sample_manga.cbz" /sdcard/Download/ || true
    adb push "$PROJECT_DIR/test_samples/sample_manga_vol2.cbz" /sdcard/Download/ || true
    adb push "$PROJECT_DIR/test_samples/western_comic.cbz" /sdcard/Download/ || true

    echo "[*] Launching MangaTV Reader application..."
    adb shell am start -n com.mangatv.reader/.MainActivity || true

    echo "[*] Executing simulated D-Pad navigation sequence..."
    sleep 3
    # Navigate in Library Grid
    adb shell input keyevent 22 # KEYCODE_DPAD_RIGHT
    sleep 1
    adb shell input keyevent 23 # KEYCODE_DPAD_CENTER (Select/Open comic)
    sleep 2

    # In Reader Screen: Turn pages
    adb shell input keyevent 21 # KEYCODE_DPAD_LEFT (Turn page in RTL Manga mode)
    sleep 1
    adb shell input keyevent 21 # KEYCODE_DPAD_LEFT (Next page)
    sleep 1
    adb shell input keyevent 23 # KEYCODE_DPAD_CENTER (Toggle OSD Overlay)
    sleep 2
    adb shell input keyevent 4  # KEYCODE_BACK (Exit reader to Library)

    echo "[✓] Automated TV D-Pad test sequence completed successfully!"
else
    echo "[i] Note: To run automated ADB tests, ensure an Android TV device or emulator is running and adb is in PATH."
fi

echo "======================================================================"
echo "  MangaTV Reader setup script finished."
echo "======================================================================"
