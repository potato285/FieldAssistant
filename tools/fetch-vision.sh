#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
BASE=https://huggingface.co/KumarjitC/plantulus-doctor/resolve/ee2cb607a33fefa85ccc567988c98fc605fa36bd
mkdir -p app/src/main/assets/vision
for file in plantulus_doctor_fp16.tflite classes.json class_info.json preprocessing.json; do
    curl -fL --retry 2 "$BASE/$file" -o "app/src/main/assets/vision/$file"
done
python3 - <<'PY'
from pathlib import Path
import hashlib
p = Path('app/src/main/assets/vision/plantulus_doctor_fp16.tflite')
assert hashlib.sha256(p.read_bytes()).hexdigest() == '3f9b1ef6e7bcc603c6844429ae4ac28607cbfb48fd7b1f856362c824a29a3e65'
print('Pinned vision model checksum verified')
PY
