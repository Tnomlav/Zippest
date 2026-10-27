# Zippest

Zippest is an Android file manager and archive utility for browsing, opening,
and extracting common compressed formats. Its interaction model follows the
familiar flow of ZArchiver and RAR for Android while using Android's Storage
Access Framework for scoped folder access.

![Zippest file browser](design/screens/01-root.png)

## Repository layout

- `android/` - Kotlin and Jetpack Compose Android application
- `design/` - frozen interactive product prototype and reference screenshots
- `tools/` - local test fixtures and automation scripts

## Highlights

- Browse authorized folders, search, sort, create folders, copy, move, share,
  and delete.
- Browse ZIP, 7Z, RAR, TAR, TAR.GZ, TAR.BZ2, TAR.XZ, CPIO, GZ, BZ2, and XZ.
- Extract to the archive folder, a new same-name folder, or any SAF folder.
- Open password-protected ZIP, 7Z, and RAR archives.
- Reuse one password across a multi-archive job or enter each password
  separately.
- Queue extraction through WorkManager with persistent notification progress
  and recovery after the app process is stopped.
- Encrypt queued archive passwords with an Android Keystore-managed key.
- Inspect active, queued, and completed jobs from the in-app task center.
- Open archives through Android's standard "Open with" flow.
- Guard against path traversal, archive bombs, excessive entry counts, and
  insufficient disk space.

## Build

```powershell
cd android
$env:JAVA_HOME='C:\Android Studio\jbr'
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
```

The debug APK is generated at:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

Android Studio can open `android/` directly.

## Status

The package name remains `com.decompress.app` for upgrade compatibility.
Version `0.2.0` adds the persistent extraction queue, RAR5 support, task status
UI, and resumable checkpoints. Archive creation is not included yet.
