# Zippest for Android

An Android file manager and archive utility for opening, extracting, and
creating common archive formats. The product UI follows the finalized prototype in
`C:\Decompress\design`.

## Current build

- Package: `com.decompress.app`
- Version: `1.5.5`
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
- Persistent settings for appearance, hidden files, thumbnails, sorting,
  extraction defaults, notifications, permissions, and task maintenance.
- Browse and extract ZIP, 7Z, RAR, TAR, TAR.GZ, TAR.BZ2, TAR.XZ, TAR.ZST,
  TAR.BR, CPIO, GZ, BZ2, XZ, LZMA, LZ4, ZST, and BR archives.
- Create ZIP, 7Z, TAR, TAR.GZ, TAR.BZ2, TAR.XZ, TAR.ZST, CPIO, GZ, BZ2,
  XZ, LZMA, LZ4, and ZST archives with selectable compression levels.
- Create encrypted ZIP and 7Z archives.
- Split created archives into sequential `.001`, `.002`, and later volumes;
  open `.001` in Zippest to combine and extract it.
- Open archive files from another app through Android's standard "Open with"
  flow.
- Extract to the archive folder, a new same-name folder, or any folder selected
  through the system picker.
- Extract password-protected ZIP, 7Z, RAR4, and RAR5 archives. Multi-archive
  jobs can reuse one password or collect a password for every archive.
- Queue extraction and compression through WorkManager with notification progress, encrypted
  password storage, and recovery after a process stop.
- Hold a foreground service and partial CPU wake lock so archive jobs continue when
  Zippest is in the background or the phone screen is off.
- Replace the storage bar with a live task card and progress bar while compression
  or extraction is active.
- Preselect the text after the last dot when renaming and replace it with 7Z, ZIP,
  RAR, or a user-defined suffix.
- Browse copy and move destinations inside Zippest with breadcrumb navigation and
  no system document picker.
- Prefer direct filesystem operations for create, rename, delete, copy, and move
  when all-files access is available, even when browsing through SAF.
- Delete selected files immediately without an additional in-app confirmation.
- Keep deleted files in a recycle bin with restore, permanent deletion, clear-all,
  and configurable retention.
- Render operation notices as compact cards centered in the app window.
- Paste clipboard text directly from the left side of the extraction clipboard row;
  the right-side arrow opens the clipboard list.
- Hide the duplicated internal-storage subtitle and use a quick-location grid with
  dedicated icons for root, downloads, media, and documents.
- Tap an archive row to open extraction directly; use its trailing chevron to preview.
  Preview mode shows an animated border and a clear extraction-required notice.
- Tap any file inside the preview to open extraction settings for the complete archive.
- Create standard ZIP split archives as `.z01/.z02/.../.zip`; open either the first
  volume or the final `.zip`. Numeric `.zip.001` volume sets remain supported.
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

Version `1.5.5` keeps the pull-to-refresh indicator visible for a minimum
duration so fast directory scans still provide visible loading feedback.
Version `1.5.4` moved copy and move into an in-place destination browser and
returns to the source folder after the operation.
Version `1.5.3` added pull-to-refresh to the file browser while retaining the
current list during refresh.
Version `1.5.2` added configurable internal-storage shortcuts, neutral popup
surfaces, and mutual exclusion for browser menus and dialogs. Shortcut paths
are chosen through the system folder picker, and the popup ends with a shared
“Add” action.
Version `1.5.1` moved compression into the selection menu and presents
extraction as a circular action above the selection toolbar.
Version `1.5.0` added archive renaming and suffix shortcuts directly to the
single-archive extraction sheet; the rename is committed before extraction.
Version `1.4.4` added matching linear icons to the selection-toolbar menu.
Version `1.4.3` positioned top and bottom menus directly from the visible
Scaffold bar boundaries instead of popup icon anchors, then lifts and narrows
the resulting compact panels. Version `1.4.0` aligned top-bar and selection-toolbar
menus flush with their adjacent app bars. Version `1.3.8` aligned top-bar dropdowns beneath the anchor
icon, removed menu shadows, and retained the full theme-colored border.
Version `1.3.5` aligned top-bar dropdowns beneath the app bar and used
the active theme background for every menu surface. Version `1.3.2` unified
every dropdown menu with the rounded, elevated panel style used by the
internal-storage chooser. Version `1.3.1` replaced the selected-items
share shortcut with a menu containing Open With and Share. Version `1.3.0` opened indexed media through MediaStore and
includes same-folder media URIs so capable galleries and players can navigate
between siblings; write grants enable direct editing where supported.
Version `1.2.4` grouped Open With targets by file type, prioritized direct
viewers/editors ahead of share and send targets, and remembered always-use
preferences for the selected file family. Version `1.2.2` added the
explicit one-time/always Open With dialog and its reset control. Version `1.2.0`
added system-app opening for regular files, cross-volume file
transfer roots, app-wide archive progress with pause/resume and timing, edge and
bottom navigation gestures, Zstandard creation/opening, and Brotli opening.
Version `1.1.9` showed the current directory path as the top-bar title without a
second subtitle and removed the redundant quick-location heading. Version
`1.1.8` removed the colored background tiles from file and folder icons so file
names remain the visual focus. Version `1.1.7` attached the full-width
internal-storage quick-location panel directly below the top app bar. Version
`1.1.6` added native ZIP split-volume output and reading for `.z01/.zip`
sets while retaining numeric `.001` volume support. It opens whole-archive extraction when a previewed file is tapped. It separates direct extraction from archive preview and marks preview mode with an animated border. It redesigns the internal-storage location picker as a compact grid and removes its duplicate root subtitle. It separates direct clipboard paste from the clipboard expansion control in the extraction sheet. It centers compact operation notices across every screen and includes a persistent recycle bin with restore and retention settings, plus unrestricted direct file modifications across standard internal
storage and removes delete confirmation prompts. It includes an in-app destination browser for copy and move operations.
It includes the dot in automatic suffix selection and one-tap 7Z/ZIP/RAR/custom
extension replacement in the rename dialog. It retains screen-off background execution safeguards and an in-app floating
progress bar for active archive tasks. It retains archive creation, password-protected ZIP/7Z output,
split-volume output and reading, plus LZMA/LZ4 support. It retains persistent settings for appearance, browsing, extraction
defaults, permissions, notifications, and maintenance. Archive creation is
available from the multi-select bottom toolbar.
Cloud-backed SAF providers may offer less information or different performance
than local storage.

For Google Play distribution, the `MANAGE_EXTERNAL_STORAGE` declaration must be
submitted through the File Manager and Archive policy declaration. Direct APK
distribution does not require that review.
