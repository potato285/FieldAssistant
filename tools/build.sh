#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$FIELD_ROOT"
if [[ -x "$FIELD_ROOT/tools/gradle-8.9/bin/gradle" ]]; then
    gradle --no-daemon --console=plain "$@"
elif [[ -x ./gradlew ]]; then
    ./gradlew --no-daemon --console=plain "$@"
else
    echo 'Run bash tools/bootstrap.sh first (requires JDK 17).' >&2
    exit 1
fi
