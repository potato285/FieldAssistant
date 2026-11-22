# Public repository validation

The GitHub handoff adds a pinned APK/model download wrapper to the existing phone
installer. Application inference code and the tested v0.4.0 APK are unchanged.

```sh
python3 tools/setup_phone.py --apk artifacts/field-assistant-debug.apk --prepare-only
python3 tools/setup_phone.py --apk artifacts/field-assistant-debug.apk --adb tools/android-sdk/platform-tools/adb --check-only
```

Both passed. The existing model and tokenizer were re-hashed, installer files were
prepared, and physical phone compatibility checks passed without changing phone apps
or network settings. Logs: [preparation](verification/repository-setup-prepare.log),
[phone check](verification/repository-setup-check.log).

## Clean source build

A clean Git clone passed the documented Gradle wrapper path:

```sh
python3 tools/verify-assets.py
./gradlew --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
```

JDK 17, SDK 35/Build Tools 34.0.0 and the existing dependency cache were supplied.
All 80 Gradle tasks executed; build succeeded in about one minute, 19 unit tests passed,
lint had 0 errors/9 warnings. [Build output](verification/clean-clone-build.log).
This is a clean source/output check, not a clean SDK/dependency installation.

The original upstream model CDN reset TLS connections on this network. The first two
fresh-download attempts and an HTTP/1.1 retry failed without disabling verification.
The clean build therefore used a copy of the previously verified pinned vision model,
then reran all asset checks. Byte-identical GitHub Release mirrors were added to the
downloader as a fallback; published-download checks are recorded separately below.

No Windows/Linux phone-setup success or first-time installation on another phone is
inferred from these checks. No signing keys, account credentials, personal photos,
SDK/tool caches or private project notes are published.
