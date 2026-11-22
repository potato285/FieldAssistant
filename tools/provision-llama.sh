#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$FIELD_ROOT"
python3 - <<'PY'
import hashlib
from pathlib import Path
for name, expected in {
    'llama.pte': '8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b',
    'tokenizer.model': '82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55'
}.items():
    with (Path('tools/llama')/name).open('rb') as f:
        value=hashlib.file_digest(f, 'sha256').hexdigest()
    assert value == expected, f'Invalid or incomplete {name}'
    print(name, value)
PY
# Write as the app UID. Android 16 scoped storage can reject shell-created
# external directories even when their apparent Unix permissions are permissive.
# run-as is intentionally available only for this prototype's debug APK.
adb shell run-as org.fieldassistant mkdir -p files/llama
adb shell -T "run-as org.fieldassistant sh -c 'cat > files/llama/llama.pte'" < tools/llama/llama.pte
adb shell -T "run-as org.fieldassistant sh -c 'cat > files/llama/tokenizer.model'" < tools/llama/tokenizer.model
adb shell run-as org.fieldassistant ls -l files/llama
