#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${VERSION:?Pass an immutable release candidate version}"
[[ "$VERSION" =~ ^[0-9][0-9A-Za-z._-]*$ ]] || { echo 'Invalid release version' >&2; exit 1; }
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export GROUP=com.github.gycrosskit.storage
# 不复用或清理已有 staging，保留旧候选与失败证据。
[[ ! -e build/maven && ! -L build/maven ]] || { echo 'build/maven already exists; use a fresh candidate workspace' >&2; exit 1; }
[[ ! -e build/prebuilt/storage-maven.tar.gz && ! -L build/prebuilt/storage-maven.tar.gz ]] || { echo 'Candidate archive already exists; use a fresh candidate workspace' >&2; exit 1; }
mkdir -p build/prebuilt
storage_staging_dir="$PWD/build/maven"
bash gradlew --no-daemon --max-workers=1 publishAllPublicationsToStagingRepository
python3 scripts/jitpack-metadata.py "$storage_staging_dir/com/github/gycrosskit/storage"
publications=storage-core,storage-core-android,storage-core-jvm,storage-core-iosarm64,storage-core-iossimulatorarm64,storage-core-iosx64,storage-core-ohosarm64,storage-mmkv,storage-mmkv-android
python3 scripts/check-maven.py "$storage_staging_dir" "$GROUP" "$VERSION" storage-core,storage-mmkv ios_arm64,ios_x64,ios_simulator_arm64,ohos_arm64 "$publications" --max-jvm-major 55
COPYFILE_DISABLE=1 tar --no-xattrs -czf build/prebuilt/storage-maven.tar.gz -C "$storage_staging_dir" com
(cd build/prebuilt && shasum -a 256 storage-maven.tar.gz) > build/prebuilt/SHA256SUMS
cat build/prebuilt/SHA256SUMS
