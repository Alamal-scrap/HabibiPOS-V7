# Habibi Dessert POS Android – V7

This is an Android project, not a pre-built APK. GitHub Actions builds a test APK automatically on push to `main` or via Actions > Build Habibi POS Android APK > Run workflow.

Download from the successful workflow run's **Artifacts** area, extract `HabibiPOS-V7-APK.zip`, then install `app-debug.apk` on your Samsung tablet.

This test APK uses Android debug signing; future updates should use a stable release signing key. Test a transaction, PayNow, open/close shift, browser/app relaunch, adding items, and JSON backup/restore before relying on the app for sales. Never clear app data without backup. PayNow QR script loads from a CDN and requires network access.

Upload the *folders*, especially the hidden `.github` folder and `app` folder, while preserving paths. GitHub Desktop is recommended.
