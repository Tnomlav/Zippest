# Zippest for Android

An Android file manager and archive browser for opening and extracting common
archive formats. The product UI follows the finalized prototype in
`C:\Decompress\design`.

## Current build

- Package: `com.decompress.app`
- Version: `1.0.3`
- Minimum Android: 8.0 (API 26)
- UI: Kotlin, Jetpack Compose
- Storage: Android Storage Access Framework, plus all-files access for direct
  internal storage on supported releases
- APK: `app/build/outputs/apk/release/app-release.apk`

## Features

- Browse authorized folders, search, sort, create folders, select by long press,
  copy, move, rename, share, and delete.
- Use all-files access to browse the internal storage directly on Android 11+,
  with scoped SAF folder access as an explicit fallback. Android 10 uses the
  legacy storage bridge so read/write access works across shared storage.
- Browse and extract ZIP, 7Z, RAR, TAR, TAR.GZ, TAR.BZ2, TAR.XZ, CPIO, GZ,
  BZ2, and XZ archives.
- Open archive files from another app through Android's standard "Open with"
  flow.
- Extract to the archive folder, a new same-name folder, or any folder selected
  through the system picker.
- Extract password-protected ZIP, 7Z, RAR4, and RAR5 archives. Multi-archive
  jobs can reuse one password or collect a password for every archive.
- Queue extraction through WorkManager with notification progress, encrypted
  password storage, and recovery after a process stop.
- Refresh Android's media index for files extracted into direct shared storage.
- Inspect active, queued, completed, failed, and canceled jobs from the in-app
  background task dialog.
- Unlock archives with encrypted headers directly from the archive browser.
- Run a real integrity test that reads every archive entry instead of reporting
  only the header count.
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
.\gradlew.bat assembleRelease
.\gradlew.bat bundleRelease
```

The project is also configured to open directly in Android Studio from
`C:\Decompress\android`.

## Run on the emulator

```powershell
$adb='C:\Users\23399\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb install -r app\build\outputs\apk\release\app-release.apk
& $adb shell am start -n com.decompress.app/.MainActivity
```

The local signing files in `.signing/` are intentionally excluded from Git and
must be backed up separately.

The development emulator is `Pixel_7_Pro`. The authorized test directory is
`/sdcard/Download/DecompressTest`. Local test fixtures are in
`C:\Decompress\tools`; `encrypted-secret.zip` uses the password `Secret42`.
The encrypted RAR fixtures use the password `junrar`, and the encrypted-header
7Z fixture uses `Secret42`.

## Scope

Version `1.0.3` focuses on reliable on-device file modification for direct
storage and SAF providers, including rename and safe move/copy outcomes.
Archive creation is
not included yet.
Cloud-backed SAF providers may offer less information or different performance
than local storage.

For Google Play distribution, the `MANAGE_EXTERNAL_STORAGE` declaration must be
submitted through the File Manager and Archive policy declaration. Direct APK
distribution does not require that review.
