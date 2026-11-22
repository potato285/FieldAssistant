#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$FIELD_ROOT"
mkdir -p tools "$ANDROID_HOME/cmdline-tools" evidence
if [[ ! -x tools/gradle-8.9/bin/gradle ]]; then
    [[ -f tools/gradle-8.9-bin.zip ]] || curl -fL --retry 2 https://services.gradle.org/distributions/gradle-8.9-bin.zip -o tools/gradle-8.9-bin.zip
    unzip -q tools/gradle-8.9-bin.zip -d tools
fi
if [[ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
    [[ -f tools/android-commandline.zip ]] || curl -fL --retry 2 https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip -o tools/android-commandline.zip
    unzip -q tools/android-commandline.zip -d "$ANDROID_HOME/cmdline-tools"
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
set +o pipefail
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses
set -o pipefail
sdkmanager --sdk_root="$ANDROID_HOME" 'platform-tools' 'platforms;android-35' 'build-tools;34.0.0'
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
bash tools/build.sh wrapper --gradle-version 8.9 --distribution-type bin --no-validate-url
