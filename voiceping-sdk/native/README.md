# Native Opus codec (`libmyopus.so`)

The SDK ships a prebuilt Opus codec, `libmyopus.so`, loaded by
[`Opus.java`](../src/main/java/com/media2359/voiceping/codec/Opus.java) via
`System.loadLibrary("myopus")`. This folder contains the source and build recipe
so the binaries in [`../src/main/jniLibs`](../src/main/jniLibs) are reproducible.

## Contents

- **`opus_jni.c`** — the JNI wrapper. It is the Opus wrapper from
  [Jitsi's libjitsi](https://github.com/jitsi/libjitsi) (Apache-2.0), with the JNI
  symbol prefix changed to `com.media2359.voiceping.codec.Opus`. It exports the
  same 29 `Java_com_media2359_voiceping_codec_Opus_*` functions the SDK expects.
- **`CMakeLists.txt`** — builds fixed-point Opus as a static library and links it
  into a single self-contained `libmyopus.so`, forcing 16 KB ELF segment alignment.
- **`build.sh`** — clones Opus and cross-compiles for the target ABIs with the NDK.

## 16 KB page-size support

Android 15+ can use a 16 KB memory page size, and Google Play requires apps
targeting Android 15+ to be compatible. Compatibility requires every 64-bit native
library's ELF `PT_LOAD` segments to be aligned to at least 16 KB (`0x4000`).

- `arm64-v8a` has historically been built with 64 KB (`0x10000`) alignment, so it
  was already compliant and is shipped **unchanged**.
- `x86_64` was previously 4 KB-aligned and has been **rebuilt** here with
  `-Wl,-z,max-page-size=16384` (see `CMakeLists.txt`).

32-bit ABIs are exempt from the 16 KB requirement and are no longer shipped.

## Rebuilding

```bash
export ANDROID_SDK_ROOT=/path/to/Android/Sdk   # contains ndk/ and cmake/
./build.sh                 # builds arm64-v8a + x86_64 into ./out/<abi>/
# ./build.sh x86_64        # a single ABI
```

Then copy the result over the shipped binary:

```bash
cp out/x86_64/libmyopus.so ../src/main/jniLibs/x86_64/libmyopus.so
```

Use **NDK r27 or newer** (r28+ makes 16 KB the default). The `out/` and `opus/`
and `cmake-build/` directories are build artifacts and should not be committed.

## Verifying alignment

```bash
# 16 KB-aligned segments if every LOAD p_align is >= 0x4000:
llvm-readelf -l src/main/jniLibs/x86_64/libmyopus.so | grep LOAD
```
