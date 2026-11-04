#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
mkdir -p build/ios-contract
swiftc -swift-version 5 -emit-module -emit-library -module-name MMKV verification/ios/MMKV.swift -o build/ios-contract/libMMKV.dylib -emit-module-path build/ios-contract/MMKV.swiftmodule
swiftc -swift-version 5 -I build/ios-contract -L build/ios-contract -lMMKV -emit-module -emit-library -module-name GycStorage ios/Sources/GycStorage/*.swift -o build/ios-contract/libGycStorage.dylib -emit-module-path build/ios-contract/GycStorage.swiftmodule
swiftc -swift-version 5 -parse-as-library -I build/ios-contract -L build/ios-contract -lMMKV -lGycStorage verification/NativeStorageCompatibilityMain.swift -o build/ios-contract/probe
DYLD_LIBRARY_PATH="$PWD/build/ios-contract${DYLD_LIBRARY_PATH:+:$DYLD_LIBRARY_PATH}" build/ios-contract/probe
