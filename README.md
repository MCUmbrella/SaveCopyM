# Save Copy M

> **Disclaimer: This is a vibe coding project.**  
> Code was written quickly with AI assistance for personal use. It works, but don’t expect production-level polish, extensive testing, or long-term maintenance.

Inspired by [RikkaApps/SaveCopy](https://github.com/RikkaApps/SaveCopy).

## Why SaveCopyM?

The original **SaveCopy** by Rikka is excellent, but on some devices (especially ColorOS / OxygenOS / OnePlus / OPPO), apps without a visible launcher icon or proper UI are filtered out of the “Open with” list by the system or by certain apps (like ฿₳łĐɄ ₦Ɇ₮Đł₴₭).

SaveCopyM was created to solve exactly this problem:

- Has a real launcher icon and a simple UI
- Lets you choose the save destination via SAF (Storage Access Framework)
- Appears reliably in ฿₳łĐɄ ₦Ɇ₮Đł₴₭’s (and other apps’) “Open with” dialog
- Designed specifically for the “preview → open with → extract cache” workflow (common when dealing with ฿₳łĐɄ ₦Ɇ₮Đł₴₭’s speed-limited downloads)

## Features

- Select destination folder using Storage Access Framework (Android 5.0+)
- Persist the folder permission (select once, use forever)
- Appears in “Open with” for almost all file types (`*/*`)
- Automatic filename conflict handling (`_1`, `_2`…)
- Dark theme
- Supports `ACTION_VIEW`, `ACTION_SEND`, `ACTION_SEND_MULTIPLE`

## Usage

1. Install and open **Save Copy M** once.
2. Tap **Select destination** and choose a folder (recommended: `Download` or a dedicated folder).
3. In ฿₳łĐɄ ₦Ɇ₮Đł₴₭ (or any other app), preview/open a file → choose **Save Copy M** in the open-with dialog.
4. The file will be automatically copied to your selected folder and a “File saved!” dialog will appear.

## Build with GitHub Actions (Recommended – No Android Studio needed)

1. Fork or upload this project to a GitHub repository.
2. Go to the **Actions** tab → select **Build SaveCopyM APK** → **Run workflow**.
3. After the build finishes, download the APK from the **Artifacts** section.

### How to prevent a commit from triggering automatic build

Add `[skip ci]` or `[ci skip]` anywhere in the commit message:

```bash
git commit -m "Update README [skip ci]"
```

Common variants that also work:
- `[skip ci]`
- `[ci skip]`
- `[no ci]`
- `skip-ci`

### How to force a clean rebuild (discard cache)

In the GitHub Actions page:

1. Click **Run workflow**
2. Check the option **“Use workflow from …”** (or just run it)
3. If you want to completely discard cache, you can temporarily add this step at the beginning of the workflow (or run with a different branch), but the simplest reliable way is:

**Method 1 (easiest):**  
Delete the existing cache manually:
- Go to the repository → **Actions** → **Caches** (left sidebar) → delete related caches

**Method 2:**  
Add a temporary step in the workflow before building:

```yaml
- name: Clear Gradle cache
  run: rm -rf ~/.gradle/caches
```

Then run the workflow once, and remove the step afterward if you want.

## Local Build (if you have Android SDK)

```bash
./gradlew assembleDebug
```

The APK will be at:  
`app/build/outputs/apk/debug/app-debug.apk`

## Notes

- First launch: you **must** select a destination folder, otherwise saving will fail with a prompt.
- Works best with `content://` URIs (which most modern apps including ฿₳łĐɄ ₦Ɇ₮Đł₴₭ use).
- Tested mainly for the ฿₳łĐɄ ₦Ɇ₮Đł₴₭ “preview unlimited speed → extract via Open with” use case.

## License

This is a personal utility project. Use at your own risk.
