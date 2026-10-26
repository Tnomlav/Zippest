# Zippest for Android

An Android file manager and archive browser for opening and extracting common
archive formats. The product UI follows the finalized prototype in
`C:\Decompress\design`.

## Current build

- Package: `com.decompress.app`
- Version: `0.1.0`
- Minimum Android: 8.0 (API 26)
- UI: Kotlin, Jetpack Compose
- Storage: Android Storage Access Framework, with no broad storage permission
- APK: `app/build/outputs/apk/debug/app-debug.apk`

## Features

- Browse authorized folders, search, sort, create folders, select by long press,
  copy, move, share, and delete.
- Browse and extract ZIP, 7Z, RAR, TAR, TAR.GZ, TAR.BZ2, TAR.XZ, CPIO, GZ,
  BZ2, and XZ archives.
- Open archive files from another app through Android's standard "Open with"
  flow.
- Extract to the archive folder, a new same-name folder, or any folder selected
  through the system picker.
- Extract password-protected ZIP and 7Z/RAR archives. Multi-archive jobs can
  reuse one password or collect a password for every archive.
- Continue extraction in a foreground service with notification progress.
- Refresh the current folder and show an in-app result after a background job
  finishes.
- Guard against path traversal, excessive entry counts, excessive unpacked
  size, abnormal expansion ratios, and insufficient local disk space.

## Build

From PowerShell:

```powershell
cd C:\Decompress\android
$env:JAVA_HOME='C:\Android Studio\jbr'
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
```

The project is also configured to open directly in Android Studio from
`C:\Decompress\android`.

## Run on the emulator

```powershell
$adb='C:\Users\23399\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n com.decompress.app/.MainActivity
```

The development emulator is `Pixel_7_Pro`. The authorized test directory is
`/sdcard/Download/DecompressTest`. Local test fixtures are in
`C:\Decompress\tools`; `encrypted-secret.zip` uses the password `Secret42`.

## Scope

The initial release focuses on opening, browsing, and extracting archives.
Archive creation and persistent background queues are not included yet.
Cloud-backed SAF providers may offer less information or different performance
than local storage.
