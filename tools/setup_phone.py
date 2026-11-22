#!/usr/bin/env python3
"""Prepare verified APK/Llama files and run the USB installer on Windows/macOS/Linux."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
from download_models import ROOT, download, entries, sha256


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, help='Use your own locally built debug APK instead of the published APK')
    parser.add_argument('--adb', help='Path to an existing Android Platform-Tools adb executable')
    parser.add_argument('--serial', help='Select a specific authorized phone')
    parser.add_argument('--check-only', action='store_true', help='Prepare files and check the phone without installing')
    parser.add_argument('--prepare-only', action='store_true', help='Prepare verified installer files without accessing a phone')
    args = parser.parse_args()
    stage = ROOT / 'artifacts/phone-setup'
    stage.mkdir(parents=True, exist_ok=True)
    for source in (ROOT / 'friend-setup').iterdir():
        if source.is_file(): shutil.copy2(source, stage / source.name)
    if os.name != 'nt': (stage / 'Install-Mac.command').chmod(0o755)
    shutil.copytree(ROOT / 'app/src/main/assets/licenses', stage / 'licenses', dirs_exist_ok=True)
    manifest = {}
    apk_entry = json.loads((ROOT / 'models/release.json').read_text(encoding='utf-8'))
    if args.apk:
        apk = args.apk.resolve()
        if not apk.is_file(): raise RuntimeError('APK not found: ' + str(apk))
        print('Using your own APK; caller is responsible for its origin and debug signing.')
    else:
        apk = download(apk_entry, ROOT / 'artifacts/downloads/field-assistant-debug.apk')
    files = {'field-assistant-debug.apk': apk}
    for entry in entries():
        if entry['group'] == 'llama':
            print('Llama download/use is governed by the included Llama 3.2 license and acceptable-use policy.')
            source = download(entry, ROOT / entry['path'])
            files['models/' + source.name] = source
    for name, source in files.items():
        target = stage / name
        target.parent.mkdir(parents=True, exist_ok=True)
        if source.resolve() != target.resolve():
            target.unlink(missing_ok=True)
            try: os.link(source, target)
            except OSError: shutil.copy2(source, target)
        manifest[name] = {'bytes': target.stat().st_size, 'sha256': sha256(target)}
    (stage / 'files.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    if args.prepare_only:
        print('Prepared ' + str(stage) + '. No phone accessed.')
        return
    command = [sys.executable, str(stage / 'install.py')]
    if args.adb: command += ['--adb', str(Path(args.adb).resolve())]
    if args.serial: command += ['--serial', args.serial]
    if args.check_only: command += ['--check-only']
    subprocess.run(command, check=True)


if __name__ == '__main__':
    try:
        main()
    except (Exception, KeyboardInterrupt) as failure:
        print('SETUP NOT COMPLETED: ' + str(failure), file=sys.stderr)
        print('See docs/PHONE_SETUP.md. Completed downloads can be reused on retry.', file=sys.stderr)
        sys.exit(1)
