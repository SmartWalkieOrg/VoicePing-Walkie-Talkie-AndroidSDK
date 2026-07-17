#!/usr/bin/env bash
#
# Reproducible build of libmyopus.so (the Opus JNI codec) with 16 KB-aligned ELF
# segments for Android 15+ / Google Play 16 KB page-size compliance.
#
# Usage:
#   ./build.sh [ABI ...]        # default: arm64-v8a x86_64
#
# Requirements (override via env vars if needed):
#   ANDROID_SDK_ROOT  Android SDK location (contains ndk/ and cmake/)
#   NDK_VERSION       NDK to use (default 27.2.12479018; r27+ / r28 recommended)
#
# The output libmyopus.so files are written to ./out/<abi>/ and are meant to be
# copied over voiceping-sdk/src/main/jniLibs/<abi>/libmyopus.so.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
NDK_VERSION="${NDK_VERSION:-27.2.12479018}"
NDK="$SDK/ndk/$NDK_VERSION"
CMAKE_VERSION="${CMAKE_VERSION:-3.22.1}"
CMAKE_DIR="$SDK/cmake/$CMAKE_VERSION/bin"
OPUS_TAG="${OPUS_TAG:-v1.4}"          # fixed-point Opus, matches the original binaries
OPUS_DIR="$HERE/opus"

# CMake/ninja may be on PATH; fall back to the SDK-bundled copies.
CMAKE_BIN="$(command -v cmake || echo "$CMAKE_DIR/cmake")"
NINJA_BIN="$(command -v ninja || echo "$CMAKE_DIR/ninja")"
TOOLCHAIN="$NDK/build/cmake/android.toolchain.cmake"

[ -f "$TOOLCHAIN" ] || { echo "NDK toolchain not found at $TOOLCHAIN"; exit 1; }

if [ ! -d "$OPUS_DIR" ]; then
  echo "=== cloning Opus $OPUS_TAG ==="
  git clone --depth 1 --branch "$OPUS_TAG" https://github.com/xiph/opus.git "$OPUS_DIR"
fi

ABIS=("$@"); [ ${#ABIS[@]} -eq 0 ] && ABIS=(arm64-v8a x86_64)
STRIP="$(ls "$NDK"/toolchains/llvm/prebuilt/*/bin/llvm-strip* | head -1)"

for ABI in "${ABIS[@]}"; do
  BUILD="$HERE/cmake-build/$ABI"; OUT="$HERE/out/$ABI"
  rm -rf "$BUILD"; mkdir -p "$BUILD" "$OUT"
  echo "=== configure/build $ABI ==="
  "$CMAKE_BIN" -S "$HERE" -B "$BUILD" -G Ninja \
      -DCMAKE_MAKE_PROGRAM="$NINJA_BIN" \
      -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN" \
      -DANDROID_ABI="$ABI" \
      -DANDROID_PLATFORM=android-21 \
      -DOPUS_DIR="$OPUS_DIR" \
      -DCMAKE_BUILD_TYPE=Release
  "$CMAKE_BIN" --build "$BUILD" --target myopus -j
  cp "$BUILD/libmyopus.so" "$OUT/libmyopus.so"
  "$STRIP" --strip-unneeded "$OUT/libmyopus.so"
  echo "built $OUT/libmyopus.so"
done
