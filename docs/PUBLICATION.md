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

## CI setup correction

The first GitHub Actions run stopped before compilation because the setup action
requested the obsolete SDK package `tools`. The workflow now explicitly requests
`platform-tools` plus the pinned platform/build-tools packages. The initial failure
is retained in Actions history rather than reported as an application build failure.

The corrected [Linux GitHub Actions run](https://github.com/potato285/FieldAssistant/actions/runs/45006911065)
passed: SDK setup, a fresh upstream vision download/checksum verification, Gradle build,
unit tests and lint. [Machine-readable run status](verification/github-ci.json).
This adds an independent Linux **build** result; it is not a Windows/Linux phone-setup
or Android-device inference test.

## Published download and public access checks

The repository is public at https://github.com/potato285/FieldAssistant and the
complete release is https://github.com/potato285/FieldAssistant/releases/tag/v0.4.0.

- A fresh unauthenticated clone from GitHub succeeded; local documentation links resolve.
- All four APK/model release assets have GitHub-reported SHA-256 digests and sizes
  matching the committed manifests. [Digest record](verification/release-digests.json).
- `python3 tools/setup_phone.py --adb PATH_TO_ADB --check-only` downloaded the public
  APK, verified its hash, reused/re-hashed existing Llama files, prepared the installer
  and passed the real phone's compatibility check without installing or changing radios.
  [Output](verification/public-setup.log).
- A fresh vision-model download subsequently succeeded from upstream as connectivity
  recovered, and passed the original hash. [Output](verification/public-model-download.log).
- A controlled primary-URL 404 exercised the actual GitHub fallback: the entire vision
  file downloaded from the public release and passed the original SHA-256.
  [Output](verification/public-fallback-download.log).
- An unauthenticated 4 KiB range download of the public Llama mirror matched the same
  bytes of the original model. Its full uploaded-file digest was verified against
  GitHub's server digest. A new full 1.14 GB download was not repeated; existing local
  bytes were fully re-hashed. First-time model transfer to another phone remains untested.

The repository stores source/metadata; release assets carry binaries, full Llama
terms, model attribution and checksums. Native voice packs are supplied by the phone's
speech engine and still require a one-time setup download where missing.
