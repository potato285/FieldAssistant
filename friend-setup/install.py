#!/usr/bin/env python3
"""Field Assistant USB installer. Python 3.8+, standard library only."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import sys
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent
PACKAGE = "org.fieldassistant"


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def run(command, **kwargs):
    result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            timeout=kwargs.pop("timeout", 180), **kwargs)
    if result.returncode:
        message = result.stderr.decode(errors="replace") + result.stdout.decode(errors="replace")
        raise RuntimeError(message.strip() or "Command failed: " + str(command))
    return result.stdout.decode(errors="replace").strip()


def locate_adb(explicit):
    executable = "adb.exe" if os.name == "nt" else "adb"
    local = ROOT / "platform-tools" / executable
    if explicit:
        return str(Path(explicit).resolve())
    if local.exists():
        if os.name != "nt": local.chmod(0o755)
        return str(local)
    existing = shutil.which("adb")
    if existing:
        return existing
    system = {"Windows": "windows", "Darwin": "darwin", "Linux": "linux"}.get(platform.system())
    if system is None:
        raise RuntimeError("Unsupported computer OS. Use Windows, macOS or Linux.")
    print("Downloading Google's Android Platform-Tools (computer setup only)...", flush=True)
    print("Terms/source: https://developer.android.com/tools/releases/platform-tools", flush=True)
    url = "https://dl.google.com/android/repository/platform-tools-latest-" + system + ".zip"
    archive = ROOT / "platform-tools-download.zip"
    partial = archive.with_suffix(".part")
    try:
        # System curl uses the macOS trust store, avoiding Python certificate-store issues.
        if shutil.which("curl"):
            run(["curl", "-fL", "--retry", "2", "--max-time", "300", url, "-o", str(partial)], timeout=330)
        else:
            with urllib.request.urlopen(url, timeout=120) as response, partial.open("wb") as output:
                shutil.copyfileobj(response, output)
        partial.replace(archive)
        with zipfile.ZipFile(archive) as bundle:
            for entry in bundle.infolist():
                destination = (ROOT / entry.filename).resolve()
                if ROOT not in destination.parents or not entry.filename.startswith("platform-tools/"):
                    raise RuntimeError("Unexpected platform-tools archive path")
            bundle.extractall(ROOT)
    finally:
        partial.unlink(missing_ok=True)
    if not local.is_file():
        raise RuntimeError("adb not found after extraction. Download Platform-Tools manually; see START-HERE.txt.")
    if os.name != "nt": local.chmod(0o755)
    return str(local)


def main():
    parser = argparse.ArgumentParser(description="Install Field Assistant and its offline Llama model over USB.")
    parser.add_argument("--adb", help="Optional path to an existing adb executable")
    parser.add_argument("--serial", help="Explicit device serial when more than one phone is connected")
    parser.add_argument("--check-only", action="store_true", help="Verify local files and device compatibility without installing")
    args = parser.parse_args()
    manifest = json.loads((ROOT / "files.json").read_text())
    print("Field Assistant 0.4.0 - Built with Llama", flush=True)
    print("Checking the APK and model files. The large model may take a moment...", flush=True)
    for name, expected in manifest.items():
        path = ROOT / name
        if not path.is_file() or path.stat().st_size != expected["bytes"] or sha256(path) != expected["sha256"]:
            raise RuntimeError("Missing/damaged file: " + name + ". Extract the complete ZIP again.")
        print("Verified " + name, flush=True)
    adb = locate_adb(args.adb)
    print(run([adb, "version"]), flush=True)
    devices = [line.split()[:2] for line in run([adb, "devices"]).splitlines()[1:] if line.strip()]
    if args.serial:
        selected = next((row for row in devices if row[0] == args.serial), None)
        if selected is None or selected[1] != "device":
            raise RuntimeError("Selected phone is not authorized. Unlock it and accept Allow USB debugging, then rerun.")
        serial = args.serial
    else:
        if len(devices) != 1 or devices[0][1] != "device":
            raise RuntimeError("Connect exactly ONE phone, unlock it, enable USB debugging and accept its Allow prompt.\n"
                               "Device states: " + str(devices) + "\nIf none appear, try a data-capable cable or Windows OEM USB driver.")
        serial = devices[0][0]
    base = [adb, "-s", serial]

    def device(*arguments, **kwargs):
        return run(base + list(arguments), **kwargs)

    abi = device("shell", "getprop", "ro.product.cpu.abilist")
    api = int(device("shell", "getprop", "ro.build.version.sdk"))
    if "arm64-v8a" not in abi.split(",") or api < 28:
        raise RuntimeError("This APK needs ARM64 Android 9 or newer. Device ABI/API: " + abi + "/" + str(api))
    print("Phone: " + device("shell", "getprop", "ro.product.model") + "; Android API " + str(api), flush=True)
    page_size = device("shell", "getconf", "PAGE_SIZE")
    if page_size != "4096":
        raise RuntimeError("This prototype is validated only on 4 KB memory-page phones. Detected page size: " + page_size + ". Ask the sender for a compatible native-runtime build.")
    memory = device("shell", "cat", "/proc/meminfo").splitlines()[0]
    print("Memory: " + memory + "; page size: " + page_size, flush=True)
    print("Basic ARM/API/page checks passed. Only Nothing A001 (~8 GB RAM class) has been validated; other phones may run out of memory or fail.", flush=True)
    if args.check_only:
        print("CHECK PASSED. No APK installed or phone settings changed.")
        return
    print("Installing the debug app (existing app data is retained)...", flush=True)
    print(device("install", "-r", str(ROOT / "field-assistant-debug.apk"), timeout=300), flush=True)
    device("shell", "run-as", PACKAGE, "mkdir", "-p", "files/llama")
    for name in ["llama.pte", "tokenizer.model"]:
        relative = "models/" + name
        expected = manifest[relative]["sha256"]
        remote = "files/llama/" + name
        try:
            current = device("shell", "run-as", PACKAGE, "sha256sum", remote).split()[0]
        except RuntimeError:
            current = ""
        if current == expected:
            print(name + " already installed and verified.", flush=True)
            continue
        print("Copying " + name + " over USB. Keep the cable connected...", flush=True)
        with (ROOT / relative).open("rb") as stream:
            # Direct binary stdin works on Windows too; PowerShell text pipes would corrupt it.
            device("shell", "-T", "run-as " + PACKAGE + " sh -c 'cat > " + remote + ".part'", stdin=stream, timeout=900)
        actual = device("shell", "run-as", PACKAGE, "sha256sum", remote + ".part", timeout=120).split()[0]
        if actual != expected:
            device("shell", "run-as", PACKAGE, "rm", "-f", remote + ".part")
            raise RuntimeError("Phone checksum mismatch for " + name + ". Check the cable and rerun.")
        device("shell", "run-as", PACKAGE, "mv", remote + ".part", remote)
        print(name + " installed and phone checksum verified.", flush=True)
    # Restart so an app already open during provisioning notices the new files.
    device("shell", "am", "force-stop", PACKAGE)
    print(device("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity"), flush=True)
    print("\nSETUP COMPLETE. Choose your crop and a clear leaf photo from the gallery; wait for 'Your next steps are ready'.\n"
          "Open Developer details to confirm Llama and Fallback: false.\n"
          "For speech, choose each language and use Set up offline voices if needed. Voice packs are installed by your phone's speech engine, not included in this ZIP.\n"
          "The app now works offline. You can disconnect USB and turn USB debugging off.\n"
          "Do not uninstall or clear app data: that deletes the installed Llama model.", flush=True)


if __name__ == "__main__":
    try:
        main()
    except (Exception, KeyboardInterrupt) as failure:
        print("\nSETUP NOT COMPLETED: " + str(failure), file=sys.stderr)
        print("Read START-HERE.txt. Keep all extracted files together and rerun after fixing the issue.", file=sys.stderr)
        sys.exit(1)
