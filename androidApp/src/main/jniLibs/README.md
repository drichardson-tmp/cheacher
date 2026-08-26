# libstockfish.so — Stockfish 11, dressed as a library

These are **Stockfish 11** (classical evaluation, no NNUE — ~1MB instead of ~76MB)
compiled for Android and named like a JNI library so the packager ships them and the
app can `exec()` them from its native-library dir (`useLegacyPackaging = true` keeps
them extracted on disk). They are executables, not libraries; nothing links against
them. The app speaks UCI to a spawned process — see
`androidMain/kotlin/com/cheacher/app/engine/CreateSparringEngine.android.kt`.

Why 11: at Cheacher's learner ratings the engine is throttled far below either
version's strength, so the ~350 Elo between Stockfish 11 and current releases is
invisible — but 75MB of embedded neural nets in the APK is not.

Built 2026-08-26 from the official `sf_11` tag with NDK r27c:

```
curl -sL https://github.com/official-stockfish/Stockfish/archive/refs/tags/sf_11.tar.gz | tar xz
cd Stockfish-sf_11/src
$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android26-clang++ \
  -std=c++11 -O3 -flto -DNDEBUG -DIS_64BIT -DUSE_POPCNT -static-libstdc++ \
  -Wl,--strip-all -Wl,-z,max-page-size=16384 \
  *.cpp syzygy/tbprobe.cpp -o libstockfish.so
# x86_64 (emulator): same, with x86_64-linux-android26-clang++
```

`max-page-size=16384` is not optional: Android 15+ devices ship a 16 KB page
size, and the Play packaging check rejects any `lib/**/*.so` whose LOAD segments
are aligned to the old 4 KB. Verify a rebuild with

```
$NDK/toolchains/llvm/prebuilt/*/bin/llvm-readelf -lW libstockfish.so | grep LOAD
# every LOAD must show Align 0x4000
```

Stockfish is GPLv3 — see `STOCKFISH-LICENSE.txt` (its `Copying.txt`), source at
https://github.com/official-stockfish/Stockfish/tree/sf_11.
