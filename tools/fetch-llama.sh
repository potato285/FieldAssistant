#!/usr/bin/env bash
# Development provisioning only; the Android app has no network/download code.
set -euo pipefail
cd "$(dirname "$0")/.."
BASE=https://huggingface.co/executorch-community/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET/resolve/911221e0f49d3354cbd4697a9c09feee25cadec7
mkdir -p tools/llama
curl -fL --retry 2 -C - "$BASE/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte" -o tools/llama/llama.pte
curl -fL --retry 2 "$BASE/tokenizer.model" -o tools/llama/tokenizer.model
python3 - <<'PY'
from pathlib import Path
import hashlib
for name, expected in {
    'llama.pte': '8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b',
    'tokenizer.model': '82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55'
}.items():
    h=hashlib.sha256()
    with (Path('tools/llama')/name).open('rb') as f:
        for chunk in iter(lambda:f.read(1024*1024),b''): h.update(chunk)
    assert h.hexdigest() == expected, f'Checksum mismatch: {name}'
    print(name, h.hexdigest())
PY
