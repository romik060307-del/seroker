# RK Calculator Vault — Lite UI (Android 8+)

A native Android calculator/private-file vault with a minimal, dark iPhone-inspired calculator screen. The project uses Android framework widgets only (no third-party UI libraries).

## What changed in this build

- Calculator screen uses a black background, grey number keys, light utility keys and orange operators.
- Enter a PIN of 4–12 digits and press `=` once to open the private space. PIN derivation and verification run on a background thread to avoid freezing older/low-memory phones.
- The private-file screen remains local and uses AES-GCM encryption.
- App list discovery for the app-hider screen runs off the UI thread and is cached during the activity session. Pressing Hide/Restore updates the list without querying every installed app again.
- RK Home uses a recycling `GridView` and queries app shortcuts in the background to reduce launcher stutter.
- Build workflow uses `android-actions/setup-android@v4` and does not request the removed SDK package named `tools`.

## Build APK with GitHub Actions

1. Upload the contents of this project to your repository (keep the `.github/workflows/build-apk.yml` path).
2. Commit and push to `main` or `master`, or run the workflow manually from **Actions**.
3. Open the completed workflow run and download the `RK-Calculator-APK` artifact.

The workflow produces a debug APK. It has not been built on a physical Vivo Y71 from this workspace; test it on the phone before relying on it.

## Important app-hiding limitation

Android does not provide a regular third-party calculator app a supported way to remove another app's icon from the Vivo/Funtouch stock launcher. The included Hide/Restore list filters icons only inside **RK Home**. To use that filtering, open the private space → **Aplikasi** → **Jadikan RK Home sebagai Beranda**, then select RK Home as the default Home app in Android settings.

After that, selected apps no longer appear in RK Home's app grid. They remain installed. To launch one, open the calculator → enter the PIN → **Aplikasi** → tap **Buka** beside that app. To show it in RK Home again, tap **Pulihkan**. Returning to the Vivo launcher will make its own app icons visible again; RK Home cannot modify Vivo's launcher database. Android Settings and other apps may still reveal installed apps, so this is icon filtering rather than full device-level app hiding.

## Notes for an older phone

- `minSdk` is Android 8.0 (API 26), which includes the Vivo Y71's Android 8.1 generation.
- Keep the old app's important vault files exported/backed up before replacing or uninstalling APKs. Uninstalling an app deletes its private encrypted files.
- If Android refuses to install this build as an update because of a signing-certificate mismatch, back up/export vault contents before uninstalling the older APK.
- No software build can honestly be guaranteed “zero bug” without testing on the actual phone. Verify calculator operations, PIN entry, file import/export and RK Home hide/restore on the device.
