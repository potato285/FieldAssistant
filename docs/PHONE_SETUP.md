# Install Field Assistant on another phone

This procedure installs **both the app and the mandatory offline Llama files**.
It does not require Android Studio, an API key or a paid account. The computer installs
an Android app on a phone; it does not run the Android app itself.

## 1. Check the phone and computer

| Item | Requirement / tested status |
| --- | --- |
| Android | 9/API 28 or newer |
| CPU | `arm64-v8a` (64-bit ARM) |
| Page size | 4,096 bytes; installer rejects 16 KB and other sizes |
| RAM | About 8 GB is the tested class; other/lower memory unverified |
| Phone free space | About 3 GB, including working space and speech packs |
| Computer free space | Allow 4–5 GB for downloads/staging; more for a source build |
| USB | Data-capable cable, not a charge-only cable |
| Computer tools | Python 3.8+, Git or extracted source; adb can be downloaded by setup |
| Initial network | Needed for APK, approximately 1.14 GB model, tokenizer, tools and voices |

**Only Nothing A001, Android 16, ARM64, 4 KB pages has been tested.** Requirements are
necessary checks, not a guarantee for every phone. The 0.6.0 native runtime has not
been validated on 16 KB-page devices. iPhones and 32-bit-only phones are unsupported.
macOS installation is tested; Windows/Linux execution is not yet tested.

For manual checks with adb:

```sh
adb shell getprop ro.product.cpu.abilist
adb shell getprop ro.build.version.sdk
adb shell getconf PAGE_SIZE
adb shell cat /proc/meminfo
adb shell df -h /data
```

## 2. Get the public source

Install [Python](https://www.python.org/downloads/) and [Git](https://git-scm.com/).
On Windows enable Python's PATH option or use `py -3` in the commands below.

```sh
git clone https://github.com/potato285/FieldAssistant.git
cd FieldAssistant
```

Alternatively, GitHub's **Code → Download ZIP** supplies the source; extract all files
before running commands from that folder. It contains no large model weights.
Read [third-party model terms](../THIRD_PARTY_NOTICES.md), including the Llama agreement
and acceptable-use policy, before downloading or using the models.

## 3. Enable USB debugging

1. On the phone open Settings → About phone and tap **Build number** seven times.
2. Open Developer options (often Settings → System) and enable **USB debugging**.
3. Connect one phone, unlock it, and accept **Allow USB debugging?** for your computer.
4. Keep the cable connected and the screen unlocked during setup.

Names vary by manufacturer. Search Settings for Build number or Developer options.
Leave mobile data/hotspot enabled if your computer uses this phone's network. The
installer never changes airplane mode, mobile data, Wi-Fi or hotspot settings.

## 4. Run the setup command

macOS/Linux:

```sh
python3 tools/setup_phone.py
```

Windows PowerShell/Command Prompt:

```powershell
py -3 tools/setup_phone.py
```

The command:

1. Downloads the v0.4.0 APK from this repository's GitHub Release.
2. Downloads Llama `.pte` and tokenizer from a pinned upstream revision, falling
   back to byte-identical GitHub Release mirrors if the upstream CDN is unreachable.
3. Verifies each size and SHA-256 against committed manifests. Existing valid files
   are reused; interrupted model downloads can resume. Never bypass a checksum failure.
4. Prepares `artifacts/phone-setup/` with APK, models, licenses and an installer manifest.
5. Locates adb or downloads Google's official Android Platform-Tools for your computer.
6. Checks phone authorization, Android/API, ARM64 and memory page size.
7. Uses `adb install -r`, then copies model files into private app storage using
   `run-as org.fieldassistant`; checksums are checked on the phone too.
8. Opens Field Assistant and prints **SETUP COMPLETE**.

The largest transfer can take several minutes. Wait for completion. Do not assume
installing or opening only the APK means Llama has been provisioned.

Optional commands:

```sh
# Select an existing adb or a specific phone:
python3 tools/setup_phone.py --adb /path/to/adb --serial PHONE_SERIAL
# Download/verify and check the phone without installing:
python3 tools/setup_phone.py --check-only
# Prepare installer files without accessing a phone:
python3 tools/setup_phone.py --prepare-only
# Install a debug APK you built yourself:
python3 tools/setup_phone.py --apk app/build/outputs/apk/debug/app-debug.apk
```

`--apk` deliberately accepts your own APK rather than checking the published APK's
hash; use only a trusted build. Model hashes remain mandatory. The prepared folder
can be transferred to another computer and run with `python3 install.py` (or the
included Windows/Mac launcher). This is optional; the public repository is the
primary source distribution.

## 5. Install offline speech voices once

1. In Field Assistant select English, Tamil or Telugu.
2. Add a real leaf photo and wait for advice; beneath the advice tap **Set up offline voices**.
3. In your speech engine install the corresponding offline language voice. The tested
   Google engine used English (India), Tamil (India) and Telugu (India).
4. Return to Field Assistant. If the voice is still unavailable, close/reopen the app.
5. Tap **Listen to these steps**, then test **Pause reading**, **Resume reading**,
   **Read from the start** and **Stop**. Check the phone's media volume.
6. Repeat for all languages you want to use offline.

These are speech-engine voice packs, not a translation model or files we redistribute.
A phone engine without an offline Tamil/Telugu voice cannot provide those speech
options. Text and model inference still work. A one-time voice download needs internet.

## 6. Verify actual Llama operation

1. Choose the correct crop and a clear, locally stored leaf photo. Potato early/late
   blight has the most complete demonstration guidance.
2. Wait for image analysis and advice. On the tested phone Llama commonly took
   approximately 20–25 seconds for the expanded potato path; other phones vary.
3. Look for **Your next steps are ready**. In the debug build expand **Developer details**:
   check the Llama/ExecuTorch engine identity and **Fallback: false**.
4. English can display a separately generated explanation. Tamil/Telugu display saved
   translations of the sourced instructions, not a live translation of that explanation.
5. **Saved tips · AI advice unavailable** means fallback guidance; it does not confirm
   a successful Llama call. Use retry and read developer diagnostics if needed.
6. Collapse Developer details to return to the farmer view.

No built-in sample image exists. Unsupported crops, uncertain findings or crop
conflicts may receive only general advice. Do not change to an incorrect crop to
force a treatment result. These checks prove execution, not diagnostic accuracy.

After setup you can disconnect USB and disable USB debugging. To demonstrate offline
operation, first ensure local photos and voices are installed. Turn off networking
only when it will not disconnect your computer or interrupt another task. The app
itself has no INTERNET permission; the earlier recorded offline test is documented
in [verification](VERIFICATION.md).

## 7. Updates and removal

Use the same setup command for this pinned version; matching model files are reused.
`adb install -r` preserves data when the package/signing key matches. Uninstalling or
clearing app storage deletes the private Llama model and requires provisioning again.

A source-built or CI APK may use a different debug signing key from the published
APK. If Android reports a signature mismatch, do not expect `-r` to bypass it. Either
keep the existing app, use a build signed by the same key, or deliberately uninstall
and repeat full setup after accepting the data loss. No signing keys are published.
This prototype is not a Play Store/production release.

## Troubleshooting

| Symptom | What to do |
| --- | --- |
| No device / unauthorized | Unlock phone, accept USB prompt, try data cable/port and File transfer mode |
| Windows cannot see phone | Install the manufacturer's official [OEM driver](https://developer.android.com/studio/run/oem-usb) |
| Linux USB permission denied | Install distribution Android udev rules; reconnect phone |
| Download or Python certificate error | Install/use system curl, check network and retry; do not disable TLS verification |
| Repeated download failure | Check storage; remove only the named `.part` file if corrupt and retry |
| adb download blocked | Download [Google Platform-Tools](https://developer.android.com/tools/releases/platform-tools) and pass `--adb` |
| Unsupported ABI/API/page size | Current native build is incompatible/unvalidated; use a compatible phone |
| `run-as: package not debuggable` | Use the supplied debug APK, or your own debug build |
| Signature mismatch | Follow update section above; installer never uninstalls automatically |
| Llama unavailable after setup | Inspect Developer details; rerun installer to verify model checksums; check free RAM/storage |
| Native crash / memory pressure | Close other apps and retry; lower-RAM/other-vendor compatibility is not established |
| No speech | Install offline voice for that language; check media volume and restart app |
| Crop mismatch | Keep correct crop, use a clear close-up leaf photo; model can still be wrong |

Report phone model, Android version, RAM, page size and the specific error when asking
for help. Avoid sharing personal photos or full device logs unnecessarily.
