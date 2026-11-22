#!/usr/bin/env python3
"""Download pinned external models; Python 3.8+, no third-party Python packages."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parent.parent


def sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def download(entry, target):
    target = Path(target)
    if target.is_file() and target.stat().st_size == entry['bytes'] and sha256(target) == entry['sha256']:
        print('Verified existing ' + target.name, flush=True)
        return target
    target.parent.mkdir(parents=True, exist_ok=True)
    partial = target.with_name(target.name + '.part')
    print('Downloading ' + target.name + ' (' + str(entry['bytes']) + ' bytes)', flush=True)
    # System curl also avoids macOS Python certificate-store setup issues.
    if shutil.which('curl'):
        subprocess.run(['curl', '-fL', '--retry', '3', '--connect-timeout', '30',
                        '--speed-time', '120', '--speed-limit', '1024', '-C', '-',
                        entry['url'], '-o', str(partial)], check=True)
    else:
        offset = partial.stat().st_size if partial.exists() else 0
        headers = {'Range': 'bytes=%d-' % offset} if offset else {}
        request = urllib.request.Request(entry['url'], headers=headers)
        with urllib.request.urlopen(request, timeout=120) as response:
            resuming = offset > 0 and response.status == 206
            if resuming and not response.headers.get('Content-Range', '').startswith('bytes %d-' % offset):
                raise RuntimeError('Server returned an unexpected range; download not installed.')
            with partial.open('ab' if resuming else 'wb') as output:
                shutil.copyfileobj(response, output)
    if partial.stat().st_size != entry['bytes'] or sha256(partial) != entry['sha256']:
        partial.unlink(missing_ok=True)
        raise RuntimeError('Checksum/size mismatch for ' + target.name + '; rejected. Retry the download.')
    partial.replace(target)
    print('Verified downloaded ' + target.name, flush=True)
    return target


def entries():
    return json.loads((ROOT / 'models/manifest.json').read_text(encoding='utf-8'))['models']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('group', choices=['vision', 'llama', 'all'])
    args = parser.parse_args()
    print('External pretrained models: read THIRD_PARTY_NOTICES.md and the linked licenses before use.')
    for entry in entries():
        if args.group in ('all', entry['group']):
            download(entry, ROOT / entry['path'])


if __name__ == '__main__':
    main()
