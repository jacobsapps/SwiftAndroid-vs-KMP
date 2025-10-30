#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"
BUILD_DIR="$PROJECT_DIR/.build"
OUTPUT_DIR="$PROJECT_DIR/build/android/arm64-v8a"
SWIFT_PRODUCT=Core
ANDROID_TRIPLE=aarch64-unknown-linux-android28

swiftly run swift build --product "$SWIFT_PRODUCT" --swift-sdk "$ANDROID_TRIPLE"

SDK_ROOT="${SWIFT_SDK_PATH:-}"
if [[ -z "$SDK_ROOT" ]]; then
  SDK_ROOT=$(ls -1d "$HOME"/Library/org.swift.swiftpm/swift-sdks/* 2>/dev/null | head -n 1)
fi
if [[ -z "$SDK_ROOT" ]]; then
  echo "error: unable to locate Swift SDK. Set SWIFT_SDK_PATH." >&2
  exit 1
fi

ANDROID_BUNDLE="$SDK_ROOT/swift-android"
RUNTIME_DIR="$ANDROID_BUNDLE/swift-resources/usr/lib/swift-aarch64/android"
NDK_LIB="$ANDROID_BUNDLE/ndk-sysroot/usr/lib/aarch64-linux-android/libc++_shared.so"

rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

cp "$BUILD_DIR/$ANDROID_TRIPLE/debug/lib${SWIFT_PRODUCT}.so" "$OUTPUT_DIR/"
cp "$NDK_LIB" "$OUTPUT_DIR/"

RUNTIME_LIBS=(
  swiftCore
  swift_Concurrency
  swift_StringProcessing
  swift_RegexParser
  swift_Builtin_float
  swift_math
  swiftAndroid
  swiftSwiftOnoneSupport
  swiftDispatch
  swiftSynchronization
  Foundation
  FoundationEssentials
  FoundationInternationalization
  FoundationNetworking
  _FoundationICU
  dispatch
  BlocksRuntime
)

for lib in "${RUNTIME_LIBS[@]}"; do
  candidate="$RUNTIME_DIR/lib${lib}.so"
  if [[ -f "$candidate" ]]; then
    cp "$candidate" "$OUTPUT_DIR/"
  else
    echo "warning: missing runtime library $candidate" >&2
  fi
done

echo "Android JNI artifacts are in $OUTPUT_DIR"
