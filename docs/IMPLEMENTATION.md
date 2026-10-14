# Kinetic Vault — implementation handoff

## Phase 1B completion

Phase 1B is complete and device-verified. The user confirmed four PASS results:
Standard text, Standard binary file, Ghost text and Ghost binary file.
This confirmation covers the on-device integrity harness; it is not a claim
that the separate instrumentation suite or release/R8 build was verified.
No further builds, tests or web searches were run for this documentation update.

New files: app/src/main/java/com/example/kineticvault/VaultApplication.java; diagnostics/{HarnessReport,HarnessRunner,TestHarnessActivity}.java and image/{ImageMemory,ImageDecoder,ImageStats,ImageFiles,ImageUtils,CoverGenerator}.java under that Java package; app/src/main/res/xml/{file_paths,network_security_config}.xml; res/values/{dimens,styles}.xml; res/values-night/colors.xml; res/drawable/ic_{lock,shield,scan,history,settings,arrow_left,arrow_right,arrow_up,arrow_down,orb}.xml and bg_glow_card_{cyan,violet}.xml; res/anim/{fade_in,slide_up,shake,pulse}.xml; res/layout/activity_test_harness.xml (all res paths are under app/src/main); app/src/test/java/com/example/kineticvault/image/ImagePolicyTest.java; app/src/androidTest/java/com/example/kineticvault/image/{PngFixtures,ImageLayerTest}.java; docs/BUILD_HEALTH.md and docs/PHONE_CHECKLIST.md.

## Scope and authoritative phase map

The user authorized Phase 1 and Phase 1B only. Stop after the verification
checklist and STATE SNAPSHOT. Phase 2 requires an explicit START PHASE 2 line.
The earlier six-step roadmap was incorrect and is replaced by this map:

| Phase | Scope |
| --- | --- |
| 1, including 1B | Core, build health, setup, design resources, image layer, temporary test screen |
| 2 | Motion + Hide + Reveal |
| 3 | Data + Server |
| 4 | Wow layer + Scanner |
| 5 | Harden + Ship |

No later-phase implementation files have been created. Icons and animation XML
are Phase 1 design resources; they do not implement later-phase screens or effects.

## Current project

Package com.example.kineticvault; SDK 37; minSdk 26; AGP 9.4.1; Gradle 9.6.0;
Java 11 source compatibility. The template requests a Java 25 Gradle daemon.
No runtime or test dependencies were added. Core classes and the wire format
are byte-for-byte unchanged from the preceding Phase 1 delivery.

TestHarnessActivity is now the temporary exported launcher. VaultApplication
sets Material 3 DayNight to dark on fresh process start. The test screen has an
in-memory light-theme preview. System-bar and display-cutout insets are applied.
The layout scrolls for small screens and large text. Portrait is requested;
large-screen Android policy may override it, so recreation cancels the test safely.

No CAMERA, storage or INTERNET permission is declared. FileProvider exposes
only cache/shared/ with explicit URI grants. Backup and device-transfer exclusions
are supplied. The network policy denies cleartext globally; Phase 3 may add
explicit development hosts in a debug-only policy.

The nine portable core classes must run on a worker executor. WorkProgress
callbacks also run on the worker. The current harness follows that contract.

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

## Phase 1B image layer

ImageUtils is the public facade. ImageDecoder, ImageFiles, ImageMemory and
ImageStats keep each class small enough to explain in a viva.

| Public API | Purpose |
| --- | --- |
| ImageUtils.source(File) | Reopen original file bytes |
| ImageUtils.source(ContentResolver, Uri) | Reopen original SAF/provider bytes |
| ImageUtils.decodeCover(Source, boolean ghost) | Sample, size-limit and flatten a cover |
| ImageUtils.decodeExact(Source, boolean ghost) | Original dimensions/order, no EXIF or density transform |
| ImageUtils.flattenOpaque(Bitmap) | Composite onto an opaque dark background before embedding |
| ImageUtils.thumbnail(Bitmap, int longSide) | Separate display-only preview, at most 512 px |
| ImageUtils.pixels(Bitmap) | Opaque bitmap to int[] |
| ImageUtils.fromPixels(int[], int width, int height) | int[] to mutable opaque sRGB bitmap |
| ImageUtils.savePng(Bitmap, File) | PNG-only write, sync, atomic replace when supported |
| ImageUtils.sha256(File) | Streaming file digest |
| ImageUtils.statistics(int[], int[]) | RGB changed count, percentage, maximum delta, PSNR |
| ImageUtils.psnr(int[], int[]) | RGB PSNR, positive infinity for identical images |
| ImageUtils.checkMemory(int width, int height, boolean ghost) | Conservative operation budget |
| CoverGenerator.generate(int width, int height, long seed) | Offline plasma/gradient/noise cover |

All image/pixel/file operations are worker-only. Source must return stable original
bytes across the bounds and decode passes. No thumbnail, EXIF rotation or resizing
is allowed on the exact path. Exact decode rejects translucent images instead of
silently flattening them and destroying their LSBs. Covers are flattened before
embedding. Decoders request and verify mutable ARGB_8888 and sRGB; hardware bitmaps
are never accepted. Recycle worker-owned bitmaps when finished. UI preview ownership
is transferred explicitly; never recycle a bitmap still displayed by an ImageView.

Covers obey BOTH the 3,000-pixel long-side cap and the 9,000,000-pixel core cap,
plus the device memory budget. Power-of-two sampling happens before full decoding.
Pixel-count arithmetic uses long/double to prevent integer overflow.
Exact decoding rejects oversize images rather than altering their dimensions.

The image allowance is the lesser of half the VM heap limit and remaining VM heap
minus 24 MiB. Estimated work reserves another 16 MiB plus 28 bytes/pixel for
Standard or 32 bytes/pixel for Ghost. This conservatively includes bitmap/pixel
copies and Ghost's shuffle array; it is an estimate, not an OOM guarantee.
When the mode is not known yet, request the Ghost budget. A top-level worker
catches allocation failures and the screen offers a retry. Future real-secret
flows must also account for their live payload buffers.

## Temporary integrity screen

HarnessRunner.run(Context, Listener) generates a 640x360 cover and tests:
1. Standard text.
2. Standard binary file.
3. Ghost text.
4. Ghost binary file.

Every case packs, writes GZIP, derives a key, encrypts, embeds, saves PNG, reloads
from disk, compares every encoded pixel, extracts, decrypts and compares type,
filename and payload bytes. It also checks the RGB delta is at most one and records
PSNR, changed percentage, SHA-256 and real timings. PASS is shown only after all
four cases succeed. Key derivation/crypto use an indeterminate spinner, not fake
percentage progress. The public test credential is not a production credential.

Test PNG names are unique per run and deleted in finally blocks. Secret fixtures
and credential arrays are wiped. Closing/recreating the activity interrupts its
single worker at safe boundaries, discards stale UI callbacks and releases unused
previews. PBKDF2 itself may finish before cancellation takes effect. No real secrets,
gestures, history or network metadata are collected by this screen.

## Verification and delivery

Phase 1B is complete. User-reported device verification confirms all four harness
cases show PASS: Standard text/file and Ghost text/file. The core and wire format
remain unchanged. Earlier local evidence: 47 targeted JVM tests passed, AAPT2
compile/link passed, and Android app/device-test sources emitted bytecode without
source or deprecation diagnostics. The agent environment blocked full Gradle
verification with an archive-close access error. Separate instrumentation-suite
execution and release/R8 verification are not established by the four harness PASS results.

BUILD_HEALTH.md and PHONE_CHECKLIST.md retain the earlier diagnostic instructions
and checklist. This completion record supersedes their earlier pending-device status.
No PHASE_1B_FILES.md bundle was created before the user stopped further work.
Stop here. Phase 2 = Motion + Hide + Reveal, and remains gated on START PHASE 2.

## Sources

- [Android build compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes)
- [GCM AAD and authentication failures](https://developer.android.com/reference/javax/crypto/Cipher)
- [PBEKeySpec clearing](https://developer.android.com/reference/javax/crypto/spec/PBEKeySpec)
- [Specified Random algorithm](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Random.html)
- [Bitmap decoding options](https://developer.android.com/reference/android/graphics/BitmapFactory.Options)
