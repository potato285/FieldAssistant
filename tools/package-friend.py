#!/usr/bin/env python3
"""Create a self-contained app/model handoff, excluding personal development files."""
from pathlib import Path
import hashlib
import json
import os
import shutil
import zipfile

root = Path(__file__).resolve().parent.parent
stage = root / "artifacts/FieldAssistant-Friend-Setup"
stage.mkdir(parents=True, exist_ok=True)
for source in (root / "friend-setup").iterdir():
    if source.is_file(): shutil.copy2(source, stage / source.name)
for name, source in {
    "field-assistant-debug.apk": root / "artifacts/field-assistant-debug.apk",
    "models/llama.pte": root / "tools/llama/llama.pte",
    "models/tokenizer.model": root / "tools/llama/tokenizer.model",
}.items():
    destination = stage / name
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.unlink(missing_ok=True)
    os.link(source, destination)
shutil.copytree(root / "app/src/main/assets/licenses", stage / "licenses", dirs_exist_ok=True)
manifest = {}
for name in ["field-assistant-debug.apk", "models/llama.pte", "models/tokenizer.model"]:
    file = stage / name
    digest = hashlib.sha256()
    with file.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""): digest.update(chunk)
    manifest[name] = {"bytes": file.stat().st_size, "sha256": digest.hexdigest()}
assert manifest['models/llama.pte']['sha256'] == '8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b'
assert manifest['models/tokenizer.model']['sha256'] == '82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55'
(stage / "files.json").write_text(json.dumps(manifest, indent=2) + "\n")
(stage / "Install-Mac.command").chmod(0o755)
archive = root / "artifacts/FieldAssistant-Friend-Setup.zip"
with zipfile.ZipFile(archive, "w", zipfile.ZIP_STORED) as bundle:
    for file in sorted(stage.rglob("*")):
        if file.is_file() and "__pycache__" not in file.parts and "platform-tools" not in file.parts:
            bundle.write(file, str(file.relative_to(stage.parent)))
with zipfile.ZipFile(archive) as bundle:
    assert bundle.testzip() is None
digest = hashlib.sha256()
with archive.open('rb') as stream:
    for chunk in iter(lambda: stream.read(1024 * 1024), b''): digest.update(chunk)
(archive.parent / "FieldAssistant-Friend-Setup.sha256.txt").write_text(digest.hexdigest() + "  " + archive.name + "\n")
print(f"Created and verified {archive}: {archive.stat().st_size:,} bytes")
