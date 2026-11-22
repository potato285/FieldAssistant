# Build and test from source

## Toolchain

- JDK **17** (set `JAVA_HOME` to that JDK).
- Android SDK Platform **35**, Build Tools **34.0.0**, Android Platform-Tools.
- Set `ANDROID_HOME` to your SDK directory, or create ignored `local.properties` with
  `sdk.dir=/your/absolute/sdk/path` (Windows: use forward slashes, e.g. `C:/Android/Sdk`).
- Python **3.8+** for downloads/setup; Git; internet for first dependency/model downloads.
- Gradle **8.9** via included checksum-pinned wrapper; AGP **8.7.3**; Kotlin **2.0.21**;
  Compose BOM **2024.10.01**. No separate Gradle installation is required.

Android Studio can install the SDK/JDK, but is optional. Using SDK command-line tools:

```sh
sdkmanager "platform-tools" "platforms;android-35" "build-tools;34.0.0"
sdkmanager --licenses
```

Review/accept the Android SDK licenses. On Windows use `sdkmanager.bat`. Set environment
variables through your OS/terminal and include the SDK's platform-tools directory in PATH.
Check `java -version` reports 17 and `adb version` works. Allow several additional GB
for SDK, Gradle caches and build outputs beyond phone-setup requirements.

## Build

```sh
git clone https://github.com/potato285/FieldAssistant.git
cd FieldAssistant
python3 tools/download_models.py vision
python3 tools/verify-assets.py
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Windows PowerShell:

```powershell
py -3 tools/download_models.py vision
py -3 tools/verify-assets.py
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

The vision download is mandatory before building an app that can analyze photographs.
Large model binaries are ignored by Git; ordered labels/preprocessing metadata are
committed and hash-checked. The downloader uses pinned source URLs and SHA-256.

Output: `app/build/outputs/apk/debug/app-debug.apk`. The debug APK includes the vision
model, native runtimes, saved guidance and all UI language text. It does not contain
the large Llama model or phone speech-engine voice packs.

Install and provision using the [phone requirements/procedure](PHONE_SETUP.md):

```sh
python3 tools/setup_phone.py --apk app/build/outputs/apk/debug/app-debug.apk
```

Only ARM64 is packaged. Runtime minimum Android 9/API 28; compile/target 35. Other
phones, especially 16 KB-page/lower-memory phones, require separate validation.

## Physical device tests

First install/provision and enable the local voices. Keep the phone unlocked. Build
the instrumentation APK:

```sh
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r -e class org.fieldassistant.GalleryAdvisoryFlowTest org.fieldassistant.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -r -e class org.fieldassistant.SpeechPlaybackTest org.fieldassistant.test/androidx.test.runner.AndroidJUnitRunner
```

Other classes: `VisionSmokeTest`, `LlamaSmokeTest`, `PotatoAdvisoryTest`, `FarmerFlowTest`,
`InputAndSpeechTest`, `ExternalInputTest`, `SpeechSynthesisTest`, `TeluguSpeechTest`.
`SpeechPlaybackTest` plays speech and requires installed English/Tamil/Telugu offline
voices. Real photos are test assets, not a feature in the farmer APK.

Read instrumentation output for `OK (N tests)`; adb may exit 0 even after a test failure.
The Bash helper `tools/run-phone-test.sh CLASS` enforces this success check and preserves
app data with `install -r`. Avoid `connectedDebugAndroidTest` for a provisioned phone:
its runner removed the app/model data during earlier testing.

The original development-machine helpers (`tools/bootstrap.sh`, `tools/env.sh`,
`tools/build.sh`) configure repository-local tools on macOS. For other machines use
this standard wrapper procedure, not the helper's machine-specific default JDK path.

## Offline tests

After downloading dependencies once, `./gradlew --offline assembleDebug` can use its
cache. This concerns building; it is separate from the Android application's offline
runtime. The app has no INTERNET permission and does no model downloads.

`tools/offline-check.sh --allow-network-interruption` changes phone radios and restores
them; run only with an independent computer network and deliberate authorization to
interrupt the phone's connection. Do not use it while tethering through that phone.
Recorded automated airplane-mode evidence and the project owner's manual offline
verification are described in [VERIFICATION.md](VERIFICATION.md).

## Release signing and reproducibility limits

`./gradlew assembleRelease` compiles an **unsigned** release APK with the developer
panel removed. It is not the published/provisioned build. The **Phase 1 prototype
installer requires a debuggable APK** because Llama provisioning uses `adb run-as`.
This is not production distribution. General-user model importing, production
signing/distribution and 16 KB compatibility are Phase 2 objectives.

The published v0.4.0 APK uses the original development debug certificate. Keys,
keystores and local SDK paths are excluded from Git. A new machine/CI uses its own
debug key; APK bytes and signatures will differ even with the same source. Pinned
model hashes and dependency versions are reproducible, not a bit-identical APK claim.
Do not replace/uninstall an existing differently signed app without understanding
that uninstalling deletes its model and data.

GitHub Actions builds/tests the source on Linux with pinned model download checks.
Its result is a host build check, not a phone/ARM inference test. Consult the actual
Actions run status; adding a workflow does not itself establish a passing run.
