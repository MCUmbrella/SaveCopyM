# Save Copy M

A simple Android app that lets you select a destination folder (via SAF), then when other apps (e.g. Baidu Netdisk) open a file with this app, it automatically copies the file to the selected folder and shows a success dialog.

## Features
- Select destination folder using Storage Access Framework (works on Android 5.0+)
- Persist the permission so you only select once
- Appears in "Open with" for almost all file types
- Automatic name conflict handling (_1, _2...)
- Dark theme matching the provided screenshots

## How to build
1. Open the project in Android Studio (Hedgehog / Iguana or newer recommended)
2. Let Gradle sync
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. Install the generated APK on your device

Or from command line (if you have Android SDK + Gradle wrapper set up):
```bash
./gradlew assembleDebug
```

## Usage
1. Open **Save Copy M** once, click **Select destination**, choose a folder (e.g. Download or a custom folder).
2. In Baidu Netdisk (or any other app), preview/open a file → choose **Save Copy M** in the open-with dialog.
3. The file will be copied to your selected folder and a "File saved!" dialog will appear.

## Notes
- On first use after install, make sure to select a destination before trying to save files.
- The selected folder permission is persisted.
- Works best with content:// URIs (which most modern apps use).
