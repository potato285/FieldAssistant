#!/usr/bin/env python3
"""Package existing build, source, notices and selected evidence; no downloads."""
from pathlib import Path
import hashlib
import shutil
import zipfile

root = Path(__file__).resolve().parent.parent
output = root / "artifacts"
output.mkdir(exist_ok=True)
apk = output / "field-assistant-debug.apk"
shutil.copy2(root / "app/build/outputs/apk/debug/app-debug.apk", apk)

paths = set()
for name in ["README.md", "TECHNICAL_REPORT.md", "DEMO_WALKTHROUGH.md", "AGENTS.md",
             "PROJECT_CONTEXT.md", "START_CODEX_HERE.md", "MENTORING_TRANSCRIPT.txt",
             "build.gradle.kts", "settings.gradle.kts", "gradle.properties",
             "gradlew", "gradlew.bat", ".gitignore", "app/build.gradle.kts"]:
    paths.add(root / name)
for folder in ["app/src", "gradle/wrapper", "evidence/phone-ui", "evidence/v3", "evidence/v4", "friend-setup"]:
    paths.update(p for p in (root / folder).rglob("*") if p.is_file() and "__pycache__" not in p.parts)
for extension in ["*.sh", "*.py"]:
    paths.update((root / "tools").glob(extension))
for extension in ["*.md", "*.json", "*.wav"]:
    paths.update((root / "evidence").glob(extension))
for name in ["build-milestone-a.log", "build-inference-b.log", "build-final-check.log",
             "install-milestone-a.log", "install-final.log", "final-phone-flow.log",
             "external-input-test.log", "final-ui-test.log", "airplane-mode-test.log",
             "airplane-mode-attempt-1.log", "offline-connectivity.txt", "offline-instrumentation.txt",
             "unit-test-summary.json", "lint-final.txt", "offline-audio-validation.json",
             "build-v2.log", "build-v2-final.log", "build-potato-reproduction.log",
             "potato-before-fix.log", "potato-after-parser-fix.log", "phone-v2.log",
             "ui-v2-retry.log", "gallery-v2-final.log", "install-v2.log", "lint-v2.txt"]:
    paths.add(root / "evidence" / name)

archive = output / "field-assistant-source.zip"
with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as bundle:
    for path in sorted(paths):
        if path.is_file():
            bundle.write(path, "FieldAssistant/" + str(path.relative_to(root)))
with zipfile.ZipFile(archive) as bundle:
    assert bundle.testzip() is None
    assert not any("tools/llama/" in n or "local.properties" in n for n in bundle.namelist())

lines = []
for path in [apk, archive]:
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    lines.append(f"{digest}  {path.name}")
    print(f"{path.relative_to(root)}: {path.stat().st_size:,} bytes; SHA256 {digest}")
(output / "SHA256SUMS.txt").write_text("\n".join(lines) + "\n")
