#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${VERSION:?Pass an immutable release candidate version}"
[[ "$VERSION" =~ ^[0-9][0-9A-Za-z._-]*$ ]] || { echo 'Invalid release version' >&2; exit 1; }
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export GROUP=com.github.gycrosskit.storage
mkdir -p build/prebuilt
# 空 staging 避免把旧版本混入候选，并保留失败原件。
export STORAGE_STAGING_DIR="$(mktemp -d "$PWD/build/staging.XXXXXX")"
bash gradlew --no-daemon --max-workers=1 publishAllPublicationsToStagingRepository
python3 scripts/jitpack-metadata.py "$STORAGE_STAGING_DIR/com/github/gycrosskit/storage"
publications=storage-core,storage-core-android,storage-core-jvm,storage-core-iosarm64,storage-core-iossimulatorarm64,storage-core-iosx64,storage-core-ohosarm64,storage-mmkv,storage-mmkv-android
python3 scripts/check-maven.py "$STORAGE_STAGING_DIR" "$GROUP" "$VERSION" storage-core,storage-mmkv ios_arm64,ios_x64,ios_simulator_arm64,ohos_arm64 "$publications" --max-jvm-major 61
COPYFILE_DISABLE=1 tar --no-xattrs -czf build/prebuilt/storage-maven.tar.gz -C "$STORAGE_STAGING_DIR" com
(cd build/prebuilt && shasum -a 256 storage-maven.tar.gz) > build/prebuilt/SHA256SUMS
cat build/prebuilt/SHA256SUMS
