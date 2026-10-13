# Kinetic Vault — implementation handoff

## Scope gate

Build ONLY Phase 1. Stop after its verification checklist and STATE SNAPSHOT.
Do not begin Phase 2 until the user sends START PHASE 2.
The remaining-phase list below is a roadmap, not authorization to implement it.
No later-phase implementation files have been written.

## Phase 1: portable core

Implemented Java-only payload packing, GZIP, gesture credential encoding,
PBKDF2-HMAC-SHA256, AES-256-GCM, the 38-byte image header, sequential LSB
embedding, salt-seeded Fisher-Yates scattering, extraction and bounded decoding.
No Android types or extra runtime dependencies are used by the core.
All core operations are synchronous and MUST run on the Android worker executor.
Progress callbacks run on that worker; UI updates must use the main-thread handler.

The existing project uses package com.example.kineticvault, AGP 9.4.1,
Gradle 9.6.0, Java 11 source compatibility and compile/target SDK 37.
Only minSdk changes, from 24 to 26. Existing template dependencies are retained.
The template has no launcher activity yet. This phase is NOT a usable phone app.

## Version 1 wire format

All integer fields use big endian. Bits are written most significant first,
in R, G, B order. Alpha must be 255. Header bytes occupy bit positions 0..303
in pixels 0..101; the final two channel bits of pixel 101 remain untouched.
Ciphertext starts at pixel 102, including in ghost mode.

| Byte offsets | Value |
| --- | --- |
| 0..3 | ASCII KVLT |
| 4 | Version 1 |
| 5 | Bit 0: passphrase; bit 1: ghost; other bits rejected |
| 6..21 | Random 16-byte salt |
| 22..33 | Random 12-byte IV |
| 34..37 | Ciphertext length, including the 16-byte GCM tag |

The entire 38-byte header is supplied as GCM additional authenticated data.
Recognizing a header is only structural detection; authenticity is established
only by successful GCM decryption. Wrong credentials and damaged authenticated
data can both cause AEADBadTagException. The UI must not claim it can distinguish them.

The safe capacity is floor((pixelCount - 102) * 3 / 8), clamped to zero.
The requested expression using 304 header bits can overstate capacity by one byte,
because the payload starts after 306 reserved bits. Capacity includes the GCM tag,
GZIP envelope and packed metadata; it is not the maximum raw file size.
The live capacity meter must measure the actual packed/compressed data off-thread.

Payload layout: one type byte (1=text, 2=file), unsigned two-byte UTF-8 filename
length, filename bytes, four-byte content length, exact content bytes.
Text uses an empty filename. Files use a basename, never a path.
Limits: 240 filename bytes, 16 MiB raw payload, 9 million image pixels.
Allocation limits are applied before decoding. Future ImageUtils must also use
a device memory budget, since several pixel arrays can coexist in ghost mode.

Both compression settings use a GZIP envelope. Compression off selects Deflater
NO_COMPRESSION; it does not introduce an undocumented header flag.
The pipeline always packs, writes GZIP, encrypts, then embeds.

Token IDs are LEFT=0, RIGHT=1, FORWARD=2, BACK=3, SNAP=4, FLAT=5.
Accept 4..8 tokens. Canonical password characters are:
KVLT1|<single-digit token count>|<token digits>|<passphrase UTF-16 hex>.
Each passphrase UTF-16 code unit becomes four lowercase ASCII hex digits.
Do not trim, case-fold or normalize. Keep credentials in erasable arrays.
For example, the public test credential LEFT LEFT SNAP FLAT plus A|é becomes
KVLT1|4|0045|0041007c00e9. This is test data, never a production default credential.

Version 1 fixes PBKDF2 at 120,000 iterations and 256 output bits, AES/GCM/NoPadding,
a 128-bit tag and the sizes above. Any future KDF change needs a new format version.

Ghost mode makes an array containing pixels 102..last. Starting with long seed=0,
fold each unsigned salt byte using seed=seed*31+byte, with Java long overflow.
Use java.util.Random(seed); descend i from array.length-1 to 1, select j with
nextInt(i+1), and swap array[i] with array[j]. Each selected pixel supplies R,G,B.
This algorithm is part of the wire format and must not change for version 1.

## Security boundaries

Ghost mode is obscurity; AES-GCM provides confidentiality and authentication.
Four uniformly random tokens offer at most 10.34 bits; eight offer 20.68 bits.
Human-chosen gestures may be weaker. A strong passphrase is necessary for
meaningful protection against offline guessing; PBKDF2 does not add entropy.
The public KVLT signature makes vaults detectable. Ordinary appearance provides
visual discretion, not cryptographic plausible deniability or undetectability.

The core wipes derived key bytes, canonical password arrays, PBEKeySpec passwords,
intermediate plaintext arrays and its GZIP buffers, including replaced buffers.
JCA/native implementations and Java Strings may retain copies beyond our control.
Do not promise guaranteed memory erasure on Android.
Payload owns its supplied data array: call close() when done. Callers own and must
wipe gesture/passphrase arrays. Never log or persist those values or plaintext.
Filenames belong only in the encrypted payload and temporary UI, never cloud metadata.
Salt and IV are intentionally public in the image, never database fields.

## Remaining phases, in order

1. ImageUtils, generated covers, PNG export, sensor fusion, token detector,
   enrollment/repeat, pad fallback, lifecycle-safe executor and auto-lock sessions.
2. Material 3 dark/light resources, portrait activities, edge-to-edge, accessible
   controls, splash, home, hide, reveal, scanner and a zero-setup demonstration.
3. Orb HUD, matrix work animation, diff/wipe/loupe, statistics, vault-door reveal,
   text scramble, denial feedback, calculator disguise and Nerd Mode.
4. SQLite CRUD, image/text ListView adapter, metadata queue with retries and delete
   tombstones, Settings, CSV export, expiry pickers, static/remote Learn WebView,
   About, screen-off/airplane receivers and optional coarse location off by default.
5. PHP 8 PDO action API, MySQL schema/config, metadata-only sync, connection test,
   dev-host-only cleartext configuration, XAMPP instructions and offline behavior.
6. Real-phone PNG interchange and sensor tuning, failure paths, R8 release/AAB,
   rubric evidence, contribution template, personas/survey, demo/viva/test docs,
   privacy/store kit and up to three small inventions only after required work passes.

Use Java/XML, findViewById, Activity Result APIs, HttpURLConnection, org.json,
Canvas/Paint/ValueAnimator and hand-written vectors. No extra runtime libraries.
Do not declare CAMERA. Receive original image bytes; never resize or rotate reveal
images. Hide may resize and flatten transparency before embedding. Always save PNG.
Sharing must use FileProvider with the Document/File warning. Database and cloud
availability must never be required to open a self-contained vault image.

## Verification on 2026-10-14

- All nine production core classes compile cleanly with javac --release 11.
- JUnit 4.13.2: 35 tests pass, including an independent Node crypto fixture.
- The JBR 25 test compilation emitted classes and exited 0, but also reported a
  ZipFileSystem cleanup AccessDeniedException. This is a local toolchain limitation,
  not a clean Android build result. Test execution itself exited 0 without stderr.
- The Android debug/release Gradle build was attempted. Initial dependency
  resolution failed; using the existing read-only cache progressed to a Gradle
  GeneratedClassCompilationException before app compilation. No APK, release R8
  result or device verification is claimed.
- Local diagnostic logs and test output are under build/core-verification/.

Normal Android Studio verification command, once its Gradle environment works:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

## Sources

- Android build compatibility: https://developer.android.com/build/releases/agp-9-4-0-release-notes
- GCM AAD and authentication failures: https://developer.android.com/reference/javax/crypto/Cipher
- PBEKeySpec password clearing: https://developer.android.com/reference/javax/crypto/spec/PBEKeySpec
- Specified Random algorithm: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Random.html
