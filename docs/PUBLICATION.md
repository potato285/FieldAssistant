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

Fresh source build and published-download validation are recorded after completion.
No Windows/Linux phone-setup success or first-time installation on another phone is
inferred from these checks. No signing keys, account credentials, personal photos,
SDK/tool caches or private project notes are published.
