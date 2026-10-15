# 11 — Image handling deep-dive

## What the image layer protects

The image layer turns original image bytes into pixel arrays and saves the engine's result without losing hidden bits. A **pixel** is one image cell; **RGB** means its red, green and blue channel amounts, each0–255. The **least significant bit (LSB)** is a channel's lowest binary digit; the engine stores one bit in each RGB channel. **PNG** is a lossless image encoding, preserving channel values. Ordinary **JPEG** compression is lossy, changing them. **Steganography** is carrying data within a medium such as these image pixels; **ciphertext** is the encrypted data being carried, and **plaintext** is the readable secret.

An Android **Bitmap** stores an image in memory. **ARGB_8888** represents alpha (opacity), red, green and blue using8bits per component; a **bit** is a binary digit and a **byte** is8bits. Alpha255 means opaque (not transparent). **sRGB** is the colour space requested throughout this layer; a colour space defines how numeric values represent colours. **Software bitmap** means a CPU-accessible pixel image, rather than an opaque hardware-backed image. **Mutable** means its pixel storage can be changed. A **thumbnail** is a separate smaller display copy, never a vault extraction input.

Source paths below start at `app/src/main/java/com/example/kineticvault/`. This document covers all seven files in `image/`; `10_CRYPTO_AND_STEGO.md` covers the nine core files. The packages contain synchronous helpers: synchronous means a call finishes before returning. A **worker thread** handles image/disk work separately from the **UI thread**, which handles screens and taps. No class in image/ creates a screen or a task queue. ImageUtils is the public **facade**, a single front door delegating to smaller helpers.

## Files, class purpose, callers and state

| File/class | Purpose in one sentence | Who creates/calls it | Important fields/types |
| --- | --- | --- | --- |
| image/ImageUtils.java / ImageUtils | Exposes shared image sources, preparation, pixel conversion, PNG output and statistics. | HideStore/HideForge, RevealWork, ScannerActivity and other image callers use its static API. | Nested Source interface; no retained bitmap/global state. |
| image/ImageUtils.java / Source | Supplies a newly opened original-byte stream on every request. | source(File) or source(ContentResolver,Uri) returns a lambda implementing it; decoder/PNG guard call open. | open() contract only; captured File or resolver/URI held by returned implementation. |
| image/ImageDecoder.java / ImageDecoder | Separates transformable cover preparation from unchanged decoding. | ImageUtils delegates decodeCover/decodeExact/flatten/thumbnail. | No fields; local BitmapFactory.Options and owned temporary Bitmap. Package-private. |
| image/PngIntegrity.java / PngIntegrity | Rejects broken PNG framing/checksums before BitmapFactory accepts partial pixels. | ImageDecoder.bounds calls read. | SIGNATURE, IHDR, IDAT, IEND; local8192-byte streaming buffer. Package-private. |
| image/ImageMemory.java / ImageMemory | Computes conservative allocation budgets and cover dimensions. | Decoder, facade and CoverGenerator call its helpers. | MIB=1024*1024L; WORK_RESERVE=16*MIB. |
| image/ImageFiles.java / ImageFiles | Writes explicit PNG with temporary-file replacement and computes a file fingerprint. | ImageUtils.savePng/sha256 delegate here. | Local temporary files/streams/digest; no retained state. Package-private. |
| image/ImageStats.java / ImageStats | Calculates exact RGB changed count/percentage, maximum delta and PSNR. | ImageUtils.statistics → compare → private constructor; result views consume fields. | final pixelsChanged, changedPercent, maxChannelChange, psnrDb. |
| image/CoverGenerator.java / CoverGenerator | Generates an opaque plasma/gradient/noise cover offline. | HideStore.generate; public demonstrations also use it as located in D1. | No retained state; local seeded Random and one pixel row. |

All top-level utility classes have private constructors and static methods. ImageStats is created only through compare; its constructor derives final results. **Package-private** means only code in the same package accesses the helper directly. An **interface** specifies callable behavior without choosing an implementation. A **lambda** is a compact supplied implementation; a **callback** is called by its owner when needed. Real image/ImageUtils.java — source(ContentResolver,Uri):

```java
return () -> resolver.openInputStream(uri);
```

## User-visible image paths, data storage and failures

An **Activity** is an Android screen. A **URI** (Android Uri) addresses an item provided by an app; it is not necessarily a disk path. **ContentResolver** opens original streams from that item's provider. An **Intent** requests a screen/external operation; the **Activity Result API** registers its typed contract and callback, then later returns the result. **FileProvider** publishes selected private files through content URIs with temporary access grants.

```text
Device cover:
Hide / CoverStep OpenDocument callback -> CoverStep.load -> HideWork.run [worker]
 -> HideStore.load -> ImageUtils.source(ContentResolver,Uri)
 -> ImageUtils.decodeCover -> ImageDecoder.cover -> adopt -> preview/cover dimensions

Camera cover:
CoverStep.openCamera -> HideStore.prepareCapture [worker] -> FileProvider URI
 -> CoverStep.resume -> TakePicture -> HideStore.finishCapture [worker]
 -> ImageUtils.source(File) -> decodeCover -> temporary JPEG deleted

Generated cover:
CoverStep Generate -> HideWork.run -> HideStore.generate
 -> CoverGenerator.generate(1024,1024,System.nanoTime()) -> HideStore.adopt

Engine/output:
HideActivity.forge -> HideWork.forge -> HideStore.forge -> HideForge.run
 -> ImageUtils.checkMemory/pixels -> VaultEngine.hide -> ImageUtils.fromPixels
 -> HideForge.finish -> ImageUtils.savePng -> ImageFiles.savePng/write
 -> ImageUtils.statistics -> ImageStats.compare
 -> ImageUtils.thumbnail -> ImageDecoder.renderOpaque -> result previews

Original Reveal:
RevealActivity.load -> RevealWork.load [worker]
 -> ImageUtils.source(ContentResolver,Uri) -> ImageUtils.decodeExact
 -> ImageDecoder.exact -> ImageUtils.pixels -> StegoCore.readHeader
 -> later RevealWork.decrypt -> VaultEngine.reveal -> authenticated payload

Original Scanner:
ScannerActivity.scan -> decode [worker] -> ImageUtils.source -> decodeExact -> pixels
 -> StegoCore.readHeader -> found status / absent status + exploratory LSB preview
```

| Visible feature | Storage and result | Failure/recovery | Security/limit |
| --- | --- | --- | --- |
| Device/camera cover | Original source remains with provider; prepared opaque cover/preview in HideStore memory; camera JPEG in cache/capture/ until consumed. | Unreadable/memory error -> choose another image or generate; Next disabled until load succeeds. Camera cancellation removes capture and offers another route. | Sampling/flattening happens before embedding. No CAMERA permission is needed for delegated camera capture. |
| Generate cover | 1024x1024 bitmap plus <=512 preview; no network. | Memory/operation failure offers another image/try again. | Seeded artwork is reproducible, not encryption randomness. |
| Reveal/Scanner original decode | Original stream reopened, exact bitmap/pixels; Reveal later logs retained encrypted copy via its separate data helper. | Unsupported/malformed/translucent/oversize images rejected; obtain accessible untouched original or device with enough memory. | Never repair a vault by resizing/flattening/rotating it. A structurally valid header is still unauthenticated. |
| PNG result | HideForge saves cache/shared/kinetic-UUID.png; logging hook retains private encrypted copy; previews only in memory. UUID is a random identifier used in filenames. | Directory/encode/move/sync errors become operation/export recovery; free storage or select another destination. | Output format explicitly PNG; original PNG file is used for export, not a recompressed preview. |
| Stats | Numeric ImageStats in memory. | Null/empty/unequal arrays rejected; outer UI handles operation failure. | Measures exact pixels, not visual proof of payload authenticity or encryption strength. |
| Fingerprint | sha256 returns a64-character fingerprint; data layer uses it for encrypted-file identity. | Missing digest/read error -> IOException to caller. | Fingerprint is unkeyed; not a credential, GCM tag or proof that only the secret-holder made the file. |

The image facade itself does not write SQLite records, settings or network messages. SQLite is the local relational database; those integration hooks live in data/ and are not implemented by the image package. A **lifecycle** is Android's creation/visibility/destruction sequence; hosts must detach displayed bitmaps before recycling them. **Ownership** means responsibility for releasing or wiping a resource. Worker-produced previews transfer display ownership explicitly; callers must not recycle a Bitmap still used by a view.

## ImageUtils: all method contracts

| Method / parameters | Plain-English result | Called by → calls | Edge/failure/ownership |
| --- | --- | --- | --- |
| Source.open() | Newly opened InputStream of original bytes. | PngIntegrity and ImageDecoder → concrete source lambda. | May throw IOException; null stream is rejected by consumer; bytes must stay stable between opens. Each opened stream is closed by consumer. |
| source(File file) | Source reopening that File. | HideStore camera path and file callers → lambda new FileInputStream(file). | Factory itself does not open/read disk or check worker; errors occur when open is invoked. |
| source(ContentResolver resolver,Uri uri) | Source reopening the provider input stream. | Hide/Reveal/Scanner → lambda resolver.openInputStream(uri). | No thumbnail API or copied preview; permission/read failure on open propagates. |
| decodeCover(Source source,boolean ghost) | New prepared opaque cover Bitmap. | HideStore.load/finishCapture → ImageDecoder.cover. | Delegated worker/memory/decode checks. ghost selects memory budget, not a decoding transformation. |
| decodeExact(Source source,boolean ghost) | New unscaled, original-dimension opaque Bitmap. | RevealWork.load/Scanner decode → ImageDecoder.exact. | Rejects unsupported image rather than changing dimensions/alpha; caller recycles when done. |
| flattenOpaque(Bitmap source) | New same-size opaque rendering. | Public facade for pre-embedding flattening → requireWorker, requireSoftware, ImageMemory.check(false), renderOpaque. | Does not mutate/recycle source; allocates new image. Never use to repair encoded transparency. |
| thumbnail(Bitmap source,int longSide) | New opaque display copy, no upscale, longest side<=longSide. | HideStore.adopt/HideForge.finish and display callers → requireWorker, requireSoftware, renderOpaque. | longSide must1..512; source retained, returned preview caller-owned. No extra ImageMemory.check inside this method. |
| pixels(Bitmap bitmap) | New row-major int[] of exact bitmap pixels. Row-major means each row follows the previous. | HideForge, RevealWork, Scanner → requireWorker, requireOpaque, ImageMemory.pixelCount, getPixels. | Opacity/software/dimension checks; can allocate significantly; does not alter bitmap. |
| fromPixels(int[] pixels,int width,int height) | New mutable opaque sRGB ARGB_8888 Bitmap. | HideForge/visual callers → requireWorker, ImageMemory.pixelCount, per-pixel alpha check, Bitmap.createBitmap, setPixels. | Array non-null and exactly width*height, all alpha255; no own available-budget check; callers plan memory. |
| savePng(Bitmap bitmap,File destination) | void; writes complete PNG to selected file. | HideForge.finish → ImageFiles.savePng. | Worker/opaque/path/output errors propagate; source ownership stays with caller. |
| sha256(File file) | Lowercase64-digit hex file fingerprint. | Encrypted-image identity callers → ImageFiles.sha256. | Worker and file IO failures propagated; no source mutation. |
| statistics(int[] before,int[] after) | New ImageStats. | HideForge.finish/visual callers → requireWorker, ImageStats.compare. | Equal nonempty arrays required; no automatic dimension/opacity matching. |
| psnr(int[] before,int[] after) | double psnrDb from statistics. | Public statistics shortcut → statistics. | Infinity if no RGB error; same validation. |
| checkMemory(int width,int height,boolean ghost) | void; rejects unsupported budget plan. | HideForge.run and image callers → ImageMemory.check. | This wrapper does not itself call requireWorker; it computes runtime budget without pixel/file work. |
| requireSoftware(Bitmap bitmap) | void; validates non-null/unrecycled ARGB_8888/sRGB. | Flatten/thumbnail/requireOpaque → bitmap accessors. | IOException on wrong/missing colour space/config/recycled bitmap. Despite its name, it does not separately require isMutable; decode enforces that. |
| requireOpaque(Bitmap bitmap) | void; ensures supported dimensions/software and alpha255. | pixels/save/exact → requireSoftware, ImageMemory.pixelCount, getPixels in chunks. | If !hasAlpha returns without scanning; otherwise scans each row in chunks of min(8192,width). Reject any translucent pixel. Does not flatten. |
| requireWorker() | void; refuses UI-thread image work. | Decode/generate/pixel/file/statistics methods → Looper.myLooper/getMainLooper. | Throws IllegalStateException on main, not IOException; no task is automatically scheduled. |

**InputStream** is sequential byte input; FileInputStream reads a file. **Looper** is Android's message loop; comparing the current loop with main enforces the worker requirement. An **exception** reports failure: IOException represents input/output/format failure; IllegalStateException an invalid calling state; IllegalArgumentException invalid arguments. A **throws** declaration propagates checked errors, while **finally** performs cleanup when an operation returns or fails.

## Original bytes, reopenable sources and exact decoding

### Idea and worked example

Reading original streams preserves the only data the engine can decode. BitmapFactory performs bounds and decode passes; a third initial pass scans PNG framing. **BitmapFactory.Options** describes decoding requests. **Density scaling** changes dimensions based on display density. **EXIF** is photo metadata that can request rotation; applying it would reorder stored bits. Exact path disables scaling/sampling and does not add EXIF rotation, cropping or thumbnail lookup.

Example: a supported opaque3001x1 original contains3001pixels, below9M. Exact has no3000-long-side cover cap, so it keeps3001x1 if budget allows. A4000x2500 original contains10M pixels and exact rejects it. Resizing to4000x2250 would change extraction values/order, so it is not a valid repair. Cover preparation can instead sample/resize such an image because its hidden data has not yet been written.

### Code walkthrough: ImageDecoder.exact

1. requireWorker checks calling thread.
2. bounds calls PngIntegrity.read, then reopens source with inJustDecodeBounds=true. BitmapFactory fills outWidth/outHeight without returning full pixel data; invalid dimensions fail.
3. ImageMemory.check uses original dimensions and selected Standard/Ghost memory budget before full decoding.
4. options requests inPreferredConfig=ARGB_8888, sRGB, inMutable=true, inScaled=false, inSampleSize=1.
5. decode reopens original bytes and rejects null or wrong format/colour-space/nonmutable Bitmap.
6. exact rejects width/height differing from bounds pass (source could have changed), then requireOpaque. On IOException/RuntimeException/OutOfMemoryError after decode, recycle decoded and rethrow.
7. Return same decoded bitmap, not a rendered copy; caller extracts pixels and later recycles it.

A source must supply stable original bytes across opens. Comparing dimensions detects one kind of change; it is not a byte-for-byte snapshot/hash of the source between passes. A provider might change same-size content; code's contract requires stability. Exact requests sRGB and validates the decoder result; it does not promise byte-identical colour values for every arbitrary external colour profile. App-generated opaque sRGB PNG is the intended preserved interchange.

**BitmapFactory** may accept different supported image formats. PngIntegrity.read returns immediately when a signature byte differs, so neither cover nor exact enforces a PNG-only input filename/type. Non-PNG exploration is possible; it does not mean JPEG can preserve an already embedded vault. Transparent original images are rejected even if visually similar. A software bitmap and no display transform do not undo compression already applied by another app.

## Cover preparation: sample, fit and flatten before hiding

### Idea and worked dimensions

A cover can be changed before embedding. **Sampling** decodes fewer pixels early to avoid allocating an oversized original; **compositing** draws a transparent image over an opaque background; **resampling** computes pixel colours at a new size. The code first selects a power-of-two sample to fit the pixel budget, then fits the cover to both pixel and3000long-side limits.

Numeric sample example: width8000,height7000, limit9,000,000. sample1 ->56M; sample2 ->4000*3500=14M; sample4 ->2000*1750=3.5M, so sampleSize returns4. fitCover then uses decoded dimensions, so this route yields approximately2000x1750 (decoder actual sizes are used), not a promise of the largest possible3000-long-side cover. Separate fitCover example: fitCover(4000,2000,9M) uses min(1,3000/4000)=.75, sqrt(9M/8M)>1, so3000x1500.

Transparency example: a nominal half-opacity RGB(100,150,200) is drawn over dark RGB(10,14,26), giving roughly(55,82,113) with opaque alpha. Exact rounding depends on Android compositing; this example illustrates why flattening changes colours and must happen before LSB embedding, never during Reveal.

### Code walkthrough: cover/renderOpaque

1. ImageDecoder.cover checks worker and obtains bounds (including PNG guard).
2. ImageMemory.maximumPixels(ghost,availableBudget()) derives allowed count.
3. sampleSize doubles1,2,4... using ceiling estimates (width+sample−1)/sample and height likewise until pixel estimate<=limit.
4. decode obtains mutable unscaled sRGB ARGB_8888 with chosen sample.
5. ImageMemory.fitCover on decoded dimensions computes target width/height; check target allocation plan.
6. renderOpaque makes a fresh opaque sRGB target, Canvas draws0xff0a0e1a background then Paint(FILTER_BITMAP_FLAG) draws source into Rect(0,0,width,height).
7. Return target; finally recycle intermediate decoded image. Rendering failures recycle target in its catch.

**Canvas** is Android's drawing surface and **Paint** specifies drawing/filter behavior. FILTER_BITMAP_FLAG enables filtered bitmap scaling. **Premultiplied alpha** means colour representation can be scaled by opacity; compositing after embedding can destroy last bits. Opaque target avoids that issue before engine work. Alpha false in Bitmap.createBitmap selects opaque storage metadata; no encryption happens in this drawing step.

### Every ImageDecoder method

| Method / parameters | Return / work | Called by → calls | Edge/failure behavior |
| --- | --- | --- | --- |
| cover(ImageUtils.Source source,boolean ghost) | Prepared new opaque Bitmap. | ImageUtils.decodeCover → requireWorker,bounds,availableBudget,maximumPixels,options,sampleSize,decode,fitCover,check,renderOpaque. | Intermediate decoded always recycled once obtained; errors propagate to worker wrapper. |
| exact(ImageUtils.Source source,boolean ghost) | Original-dimension decoded opaque Bitmap. | ImageUtils.decodeExact → requireWorker,bounds,check,decode(options),requireOpaque. | Same dimension check across passes; catches IO/runtime/OOM to recycle allocated decode. |
| bounds(Source source) | BitmapFactory.Options containing positive outWidth/outHeight. | cover/exact → PngIntegrity.read, source.open, BitmapFactory.decodeStream. | Null stream, unsupported/damaged/zero dimensions fail IOException; stream auto-closes. |
| options() | New configured Options described above. | cover/exact → ColorSpace.get(SRGB). | Default sample1; cover changes only sample afterward. |
| decode(Source source,BitmapFactory.Options options) | New decoder Bitmap with required type/mutability. | cover/exact → source.open, BitmapFactory.decodeStream/config checks. | Null/wrong-config/nonmutable/not-sRGB rejected; wrong-result bitmap recycled. Stream auto-closes. |
| sampleSize(int width,int height,int limit) | Power-of-two int sample, starting1. | cover → ceiling-size arithmetic. | long intermediates prevent width+sample overflow; loop cap1<<30; bounds/limit come from validated caller. |
| renderOpaque(Bitmap source,int width,int height) | New opaque rendering Bitmap. | cover/ImageUtils.flattenOpaque/thumbnail → createBitmap, Canvas/Paint/Rect, drawBitmap. | Input dimensions checked by public/calling paths; target created before try; runtime/OOM during drawing recycles target and rethrows. Does not recycle source. |

## Image memory: arithmetic rather than a promise against OOM

**Heap** is the runtime's managed memory pool. **Headroom** is remaining allowance after current use/reserves. **OOM** (OutOfMemoryError) means an allocation could not be satisfied. **MiB** means1024*1024bytes. **long** is64-bit integer, used before multiplication to avoid32-bit integer overflow; **double** is floating-point, used for scales/ratios. A conservative estimate leaves room for UI/native decoder work and simultaneous copies; it is not a proof that allocation always succeeds.

### Exact formulas and numeric budget example

```text
used = runtime.totalMemory() - runtime.freeMemory()
headroom = runtime.maxMemory() - used - 24MiB
budget = max(0, min(runtime.maxMemory()/2, headroom))
requiredBytes(P, Standard) = 16MiB + P*28
requiredBytes(P, Ghost)    = 16MiB + P*32
maximumPixels = min(9,000,000, (budget - 16MiB)/(Ghost?32:28))
```

Assume max heap256MiB and current used80MiB. Headroom=256−80−24=152MiB; half heap128MiB, so budget128MiB=134,217,728bytes. After16MiB reserve,117,440,512bytes remain. Standard maximum floor(117,440,512/28)=4,194,304pixels; Ghost=3,670,016pixels. For1M pixels, Standard requirement16,777,216+28M=44,777,216bytes; Ghost48,777,216bytes, exactly4Mbytes more for shuffle allowance. Decode checks selected budget; some callers conservatively choose ghost=true before actual header/mode known.

### fitCover algorithm and edge example

Idea: use the smaller scale demanded by long side and pixel budget, never enlarge a source. Example2500x2500 with limit1M: long-side scale1, pixel scale sqrt(1M/6.25M)=.4 ->1000x1000. For very thin dimensions, rounding short side up to1can violate the count; the final loop reduces the longer side until count fits.

Walkthrough in ImageMemory.fitCover: reject nonpositive width/height/maxPixels; clamp limit to9M; scale=min(1,3000/maxSide); scale=min(scale,sqrt(limit/(double width*height))); floor both target dimensions but clamp each at1; while target product>limit, decrement larger dimension. Uses original aspect ratio within integer rounding, returns new[width,height]. Exact never calls fitCover.

| Method / parameters | Return / work | Called by → calls | Edge/failure behavior |
| --- | --- | --- | --- |
| availableBudget() | long bytes using actual Runtime memory. | cover/check → Runtime.getRuntime,totalMemory,freeMemory,maxMemory. | Clamps negative budget0; no allocation guarantee or fixed device assumption. |
| requiredBytes(int pixels,boolean ghost) | long16MiB+pixels*(32 or28). | Public planning helper → arithmetic only. | No argument validation here; callers must pass supported positive count; formula is estimate. |
| maximumPixels(boolean ghost,long budget) | int allowance<=9M. | cover/check → subtraction/division/Math.min. | If computed count<102 throws IOException; division follows Java integer truncation. Constants reserve prevents treating tiny budgets as useful. |
| pixelCount(int width,int height) | int width*height once safe. | pixels/fromPixels/opacity/check → long multiplication. | Nonpositive or product>9M rejects; does not itself require>=102, which core needs for a header. |
| check(int width,int height,boolean ghost) | void; dimension/count/budget guard. | ImageUtils, decoder,generator → pixelCount,maximumPixels(availableBudget). | Throws if count exceeds device allowance; tiny positive images can pass memory yet later fail core header capacity. |
| fitCover(int width,int height,int maxPixels) | int[2] target dimensions. | cover → Math.max/min/sqrt/floor. | Rejects invalid dimensions/limit; respects both cover caps; no upscale. |

Rationale in code:24MiB remaining headroom,16MiB work reserve and28/32bytes per pixel conservatively account for image/pixel copies, PNG work and Ghost order. UNCLEAR as an exact allocation guarantee: source says `// Includes bitmaps, pixel copies, PNG work and Ghost's int[] shuffle.` but does not provide a measured per-object derivation. Describe it as an estimate; top-level workers catch OOM. The raw secret's live buffers also consume memory outside this image-only plan (HideForge calls SecretInput.ensureRoom).

## PNG framing and CRC: PngIntegrity completely explained

**Framing** is a file's boundaries/structure. A PNG **chunk** contains a big-endian length,4-byte type, data and4-byte checksum. **Big endian** means most significant byte first. **CRC32** is a32-bit cyclic redundancy checksum for accidental corruption, not keyed cryptographic authentication. **IHDR** is PNG's initial image header, **IDAT** compressed image data, **IEND** end marker. These PNG container chunks are separate from core/VaultHeader embedded in decoded RGB bits.

### Idea, numeric structure example and steps

A PNG starting137,80,78,71,13,10,26,10 matches the8-byte signature. Its required first chunk has length13 and type IHDR (hex49484452). Chunk footprint4length+4type+13data+4CRC=25bytes. IEND has length0; footprint12bytes. If its declared type/data checksum differs by any amount from CRC32 result, checksum throws. This is structure arithmetic, not an invented checksum result.

Walkthrough:

1. read opens source in try-with-resources; wraps BufferedInputStream then DataInputStream, which reads binary integers.
2. Read signature bytes one at a time: EOF before an encountered required byte fails; a different byte returns immediately so non-PNG inputs can proceed to BitmapFactory. Once a mismatch is found, remaining signature bytes are not required by this method.
3. chunks allocates8192buffer and loops, checking thread interruption. Read signed int length and type.
4. Reject negative length, first chunk not IHDR13, repeated IHDR, or IEND length!=0. Other chunk semantics are left to decoder; it is not a full PNG specification validator.
5. checksum starts CRC32, feeds type bytes at shifts24/16/8/0, then length bytes of data in bounded chunks via readFully.
6. Compare crc.getValue() with stored readInt()&0xffffffffL, reading the stored value as unsigned32-bit. EOF/broken framing propagates IOException.
7. Track whether any IDAT was seen. At IEND require IDAT and immediate EOF; no trailing bytes accepted. Missing IEND/partial data eventually fails reading. IDAT decompression/pixel correctness belongs to BitmapFactory, not this chunk scanner.

| Method / parameters | Return / work | Called by → calls | Edge/failure behavior |
| --- | --- | --- | --- |
| read(ImageUtils.Source source) | void; checks PNG framing if signature matches. | ImageDecoder.bounds → source.open, input.read, chunks. | Null stream/truncated matched prefix fail; nonmatching signature returns; closes underlying opened stream. No dimension/body-size cap here. |
| chunks(DataInputStream input) | void; traverses PNG to strict end. | read → input.readInt/read, checksum, Thread.isInterrupted. | Checks conditions above; interruptions -> InterruptedIOException; buffer length8192 bounds per-read memory, not total bytes/time. |
| checksum(DataInputStream input,int type,int length,byte[] buffer) | void; consumes exact data+stored CRC. | chunks → CRC32.update/getValue, input.readFully/readInt. | Reject damaged checksum; interruption checked per buffer; negative length rejected by caller. |

Constants: signature bytes as above; IHDR0x49484452, IDAT0x49444154, IEND0x49454e44; required IHDR data13; IEND0; chunk buffer8192; unsigned mask0xffffffffL. Length is not used to allocate a length-sized array. Very large/slow input can still take time; stable/reopenable provider remains a contract. CRC cannot authenticate a vault because an attacker can recalculate an unkeyed checksum; AES-GCM in core handles adversarial authentication.

## PNG saving, temporary replacement and fingerprints

### Why PNG, not JPEG

PNG losslessly preserves the RGB values after the engine modifies their low bits. JPEG's ordinary quantization (rounding transformed information to reduce size) reconstructs different values: red101(LSB1) may become100(LSB0), changing a ciphertext bit. Visual similarity or JPEG quality100 does not promise preservation. Saving must select PNG format explicitly; naming a JPEG file .png would not do it. A JPEG can be the source cover before embedding; after embedding output must remain the untouched PNG. Resizing, colour edits, screenshots or photo-mode sharing can destroy a vault even if the receiving file still uses PNG.

### Save algorithm and concrete file example

Idea: finish a complete encoded PNG in a sibling temporary file, then replace destination, rather than leaving a partly-written destination as the apparent final output. Example destination cache/shared/kinetic-X.png gets a sibling kvlt-[random].part. If encoding/sync succeeds, move .part to kinetic-X.png using atomic replace when supported. **Atomic replacement** means the filesystem swaps destination in one indivisible move; fallback still replaces but does not promise that indivisibility.

Walkthrough in image/ImageFiles.java:

1. savePng checks worker, requires supported opaque bitmap, resolves absolute parent and creates directories if needed.
2. File.createTempFile("kvlt-", ".part",parent) makes a unique sibling.
3. write opens FileOutputStream; bitmap.compress(Bitmap.CompressFormat.PNG,100,output) must return true. PNG selects encoding;100 is not a JPEG-quality workaround.
4. flush sends Java-buffered data down; output.getFD().sync requests filesystem synchronization, then stream auto-closes.
5. Files.move with ATOMIC_MOVE and REPLACE_EXISTING; if AtomicMoveNotSupportedException, retry REPLACE_EXISTING only.
6. finally Files.deleteIfExists(temp). Failed output does not intentionally retain a partial temporary. IO cleanup failures can also propagate; this is not a transaction covering all later UI/logging actions or a universal power-loss guarantee.

### SHA-256 algorithm and example

**SHA-256** is a cryptographic256-bit hash producing a file fingerprint; **MessageDigest** updates the hash incrementally. It is unkeyed: it identifies bytes, not an authorized sender. Example256bits/8=32digest bytes, rendered two hex characters per byte ->64characters. For one illustrative digest byte0xAB (Java signed byte−85), high nibble(value>>>4)&15=10 ->'a'; low(value&15)=11 ->'b'. This explains formatting, not a fabricated whole-file fingerprint.

Walkthrough: sha256 requires worker, requests MessageDigest.getInstance("SHA-256"), reads8192-byte chunks and updates only actual count, digest() finalizes, Character.forDigit writes high/low nibble with radix16, returns lowercase64-character String. It hashes file bytes, not decoded image pixels: re-encoding identical pixels can produce a different file hash. NoSuchAlgorithmException becomes IOException("SHA-256 unavailable",failure).

| Method / parameters | Return / work | Called by → calls | Edge/failure behavior |
| --- | --- | --- | --- |
| savePng(Bitmap bitmap,File destination) | void; PNG staged then moved. | ImageUtils.savePng → requireWorker/requireOpaque, createTempFile, write, Files.move/deleteIfExists. | Bad bitmap/parent/mkdir/encode/move/sync propagate; only atomic-not-supported triggers fallback. Does not recycle input. |
| write(Bitmap bitmap,File file) | void; explicit PNG, flush and sync. | savePng → FileOutputStream, Bitmap.compress, getFD.sync. | false compress result -> IOException; auto-close output. No independent worker/opacity check in private helper. |
| sha256(File file) |64hex chars. | ImageUtils.sha256 → MessageDigest, FileInputStream, Character.forDigit, StringBuilder. | Missing/read/digest failure -> IOException; stream closes, source unchanged. Hash buffer is file input, not raw secret/key. |

ImageFiles does not perform a post-save reload or decrypt. Its saving algorithm is separate from the app's user-invoked public Integrity Lab. A complete PNG write plus CRC checks still does not authenticate a hidden payload; Reveal must run the cryptographic tag check.

## PSNR, changed pixels and maximum channel change

**PSNR** is peak signal-to-noise ratio, a numerical image similarity measure. **MSE** (mean squared error) averages squared RGB channel differences. A **decibel** (dB) is a logarithmic ratio unit. Higher PSNR means less RGB error; it does not measure encryption strength, secrecy or whether an image has hidden data.

### Worked numbers

One pixel beforeRGB(0,0,0), after(1,1,1): differences−1,−1,−1, squared error3. Count1pixel*3channels=3; MSE1. PSNR=10log10(255²/1)=48.1308036dB. changedPixels1,changedPercent100,maxChannelChange1. If two pixels and only one changes that way: error3/(2*3)=.5; PSNR≈51.14dB,changedPercent50. Identical RGB makes error0 ->positive infinity by explicit branch; alpha-only change is ignored by this statistic.

### Step-by-step algorithm

1. ImageStats.compare rejects null/equal-empty/unequal-length arrays. It compares same indexes, so caller ensures same dimensions/orientation and intended pair.
2. For each pixel, ((before^after)&0x00ffffff)!=0 counts that pixel once, even if several channels changed; alpha is masked out.
3. For shifts0,8,16, extract(unsigned channel)&255, subtract and update max(abs(difference)). Order B/G/R does not matter for sum.
4. Add (long)difference*difference so total does not overflow32bits. Worst per-channel difference255;9M*3*255²=1,755,675,000,000, beyond32-bit range.
5. Private constructor sets changedPercent=100.0*changed/count; MSE=squaredError/(count*3.0); if error0 set POSITIVE_INFINITY, otherwise10log10(255²/MSE).
6. ImageUtils.statistics guards worker then delegates; psnr returns only psnrDb. Result UI formats numeric/infinity output (see D1 map).

| Method / parameters | Return / work | Called by → calls | Edge/failure behavior |
| --- | --- | --- | --- |
| compare(int[] before,int[] after) | New ImageStats with all four values. | ImageUtils.statistics → Math.max/abs, private constructor. | Requires non-null equal nonzero lengths; no alpha/dimension/max-size check here. It does not change source arrays. |
| private ImageStats(int changed,int count,int maximum,long squaredError) | New immutable numeric result. | compare only → Math.log10 for nonzero error. | count>0 guaranteed by compare; infinity for identical RGB, avoiding zero division/log failure. |

Exact constants:3channels, peak255, mask0x00ffffff,8-bit shift stride up to16,10log10 power ratio. These follow RGB byte representation and the PSNR formula. A low/high changed percentage depends on actual ciphertext/cover bits, not simply the number of embedded bits: writing a matching LSB changes nothing. Replacing one LSB guarantees per-channel delta<=1, but comparing unrelated/resized arrays can produce larger changes.

## CoverGenerator: all formulas and methods

### Idea and numerical example

**Procedural generation** creates pixels from formulas rather than loading a photograph. A sine **wave** varies smoothly; an exponential **halo** adds a bright region; small seeded **noise** perturbs colours. **Pseudorandom** means repeatable values produced from a seed. java.util.Random here creates artwork; cryptographic salt/IV use SecureRandom in core.

The generator passes normalized x=column/width and y=row/height, each starting0 and staying<1. Atx0,y0:

```text
wave = sin(0*7 + sin(0*5))*.5+.5 = .5
halo = exp(-9*((0-.7)^2+(0-.3)^2)) = exp(-5.22) ≈ .0054
Assume this pixel's noise draw yields0 (nextInt(13)==6):
red   = clamp((int)(18 + 83*.5*(1-0) + 20*.0054)+0) = 59
green = clamp((int)(25 + 95*(1-.5) + 90*.0054)+0) = 72
blue  = clamp((int)(65 + 98*.5 + 70*.0054)+0) = 114
ARGB = 0xff3b4872, alpha255
```

This is a conditional worked colour calculation, not a claim that a particular supplied seed draws noise0. Cast to int truncates these positive channel expressions before noise; clamp then confines0..255. The same noise value affects all three channels at one pixel. No default production encryption credential is generated by this class.

### Code walkthrough and exact constants

1. generate requires worker and ImageMemory.check(width,height,true), conservatively choosing Ghost allowance even though it only generates a cover.
2. Allocate opaque sRGB ARGB_8888 Bitmap; create Random(seed); allocate one int[width] row.
3. At each row check Thread.currentThread().isInterrupted; if interrupted throw CancellationException. For each column call color(x/(double)width,y/(double)height,random), then setPixels one row.
4. color computes wave=sin(x*7+sin(y*5))*.5+.5. Frequencies7/5 and scaling.5 shape the smooth pattern.
5. halo=exp(−9*((x−.7)^2+(y−.3)^2)); centre(.7,.3), falloff9.
6. noise=random.nextInt(13)−6 gives integers−6..6. Channel bases/weights: red18+83*wave*(1−y)+20*halo; green25+95*(1−wave)+90*halo; blue65+98*wave+70*halo, each truncated to int, noise added and clamped.
7. Pack0xff000000 | red<<16 | green<<8 | blue; setPixels copies row values. Return bitmap after all rows.

These numbers are artwork coefficients, not frozen crypto parameters. Source does not claim a perceptual/security optimum for them. **clamp** limits an input to a range: Math.max(0,Math.min(255,value)). For example−2→0,260→255. The public generate dimensions are caller-provided; HideStore specifically requests1024x1024 with System.nanoTime seed. The core capacity for this cover is393,177ciphertext bytes, including tag and GZIP/payload overhead, not393,177raw secret bytes.

| Method / parameters | Return / work | Called by → calls | Edge/failure/cleanup |
| --- | --- | --- | --- |
| generate(int width,int height,long seed) | New generated opaque Bitmap. | HideStore.generate → requireWorker, ImageMemory.check(true), createBitmap, Random, color, setPixels. | RuntimeException during row loop recycles bitmap; CancellationException is one such runtime exception. Explicit catch covers RuntimeException only, not OutOfMemoryError. Allocations of Random/row occur before its try; do not claim all possible allocation failures are locally recycled. Outer worker catches OOM. |
| color(double x,double y,Random random) | Packed opaque ARGB int. | generate for every pixel → Math.sin/exp, Random.nextInt, clamp. | Private caller supplies normalized coordinates/non-null Random; no own checks. |
| clamp(int value) | int bounded0..255. | color three channels → Math.max/min. | Handles both underflow/overflow range values by saturation. |

## Capacity bridge between dimensions and engine

A valid image bitmap is not automatically a usable vault cover. core/StegoCore.java — capacityBytes reserves102header pixels and uses3RGB bits/pixel:

```text
P = width*height
capacity = max(0, floor((P-102)*3/8)) for supported nonnegative P
needed = actual packed-and-GZIP byte length +16-byte GCM tag
```

For640x360: P230,400; P−102=230,298; *3=690,894; floor(/8)=86,361bytes. GZIP size86,345fits exactly after16tag;86,346does not. For the edge P<102, actual capacityBytes returns0 directly. Core stores38header bytes=304bits in102pixels=306channel positions, leaving two untouched bits; subtracting only304bits can overstate byte capacity. ImageMemory.pixelCount enforces positive dimensions and9M max, not the102minimum; core later rejects a tiny pixel array.

**PBKDF2** is password-based key derivation; **salt** is its public random16-byte input; **IV** is the fresh12-byte AES-GCM initialization value; **GCM tag** is16bytes authenticating encryption; **AAD** is all38public header bytes authenticated alongside ciphertext. The image package never derives a key, interprets the phrase or checks GCM. Its file SHA-256 and PNG CRC are not substitutes for this authentication. Complete header byte offsets and one-byte LSB worked example are in10_CRYPTO_AND_STEGO.md.

## Library types from security/crypto/compression namespaces

| Type used in image/ | Meaning / real code usage |
| --- | --- |
| java.security.MessageDigest | Streaming cryptographic fingerprint; `MessageDigest digest = MessageDigest.getInstance("SHA-256");` — ImageFiles.sha256. update feeds chunks; digest finalizes. |
| java.security.NoSuchAlgorithmException | Missing requested hash implementation; caught in ImageFiles.sha256 and wrapped IOException. |
| java.util.zip.CRC32 | PNG chunk error-detection checksum; `CRC32 crc = new CRC32();` then update/getValue — PngIntegrity.checksum. Unkeyed and forgeable. |

No javax.crypto type is used by image/. Core's Cipher/SecretKey/SecretKeyFactory/PBEKeySpec/SecretKeySpec/GCMParameterSpec, SecureRandom/GeneralSecurityException, and GZIPInputStream/GZIPOutputStream/Deflater are fully defined in10. Image decoding may internally involve compression, but no direct inflater/deflater object is constructed by image/. java.util.Random and CancellationException are outside java.util.zip and are explicitly used for artwork/interrupt handling.

## Ownership, cancellation and failure statements

- Source factories only make reopenable callbacks; actual IO happens in worker consumers. Keep original data stable/permission-accessible during the operation.
- ImageDecoder.cover owns its intermediate decode and recycles it after rendering; exact transfers the returned decode to caller. flatten/thumbnail keep caller's source and return a new bitmap.
- pixels/fromPixels allocate copies; image statistics read arrays without mutating them. No automatic bitmap/pixel owner is supplied by Java garbage collection for timely release.
- Core/image helpers are synchronous; ExecutorService/Handler in app wrappers determine execution/delivery. A generation number marks obsolete requests and suppresses stale callbacks. Main-thread requireWorker failures do not schedule work for you.
- PNG guard/generator check thread interruption at boundaries; not every decoder/native/write operation supports instant cancellation. Rendering or provider errors propagate; recovery occurs at the screen layer.
- Rejecting an oversized/transparent encoded original is intentional: transformation would change hidden bits. A smaller pre-embedding cover is safe, while a resized vault is not equivalent to the original.

## Java/Android reading aid used by this guide

These are real excerpts from the files named in each row. Each excerpt is at most ten lines. Context-only rows connect the engine/image code to the app; they do not move database, network or motion implementation into these packages.

| Construct, defined before its example | Tiny real example and source |
| --- | --- |
| A static utility class exposes class-level operations; final prevents subclassing; a private constructor prevents ordinary callers constructing it. | `public final class VaultEngine` and `private VaultEngine() { }` — core/VaultEngine.java. |
| An interface is a required set of methods; an enum is a fixed set of named choices. A callback is a method supplied for its owner to invoke later. | `void onProgress(Stage stage, int completed, int total);` — core/WorkProgress.java; Stage includes PACK, COMPRESS, KEY, ENCRYPT, EMBED, EXTRACT, DECRYPT, UNPACK. |
| A lambda is a compact implementation of a callback; a method reference (::) passes an existing method. | `return () -> resolver.openInputStream(uri);` — image/ImageUtils.java, source(ContentResolver,Uri); `worker.execute(store::clearPrepared);` — ui/hide/HideWork.java, clearSensitive. |
| Generics attach a type parameter to a reusable container/callback. Callable<T> returns a T; Runnable returns no value. | `interface Callback<T> { void success(T value); void failure(int message); }` — ui/hide/HideWork.java. T can be a cover/capacity/result type. |
| A nested class is declared inside another class; static nested classes do not capture an outer instance. Inheritance (extends) reuses parent behavior. An inner class has an outer instance. An anonymous class is an unnamed inline implementation. | `private static final class WipingBuffer extends ByteArrayOutputStream` — core/GzipCodec.java. `new Callback<HideForge.Result>() {` — ui/hide/HideWork.java, forge, supplies success/failure. The complete core/image packages use named static nested classes, not named non-static inner classes. |
| A thread is one sequence of execution; the main/UI thread handles screens. ExecutorService queues background work; a single-thread executor runs queued work one at a time. Handler posts to a queue serviced by a Looper. | `private final ExecutorService worker = Executors.newSingleThreadExecutor();` and `private final Handler main = new Handler(Looper.getMainLooper());` — ui/hide/HideWork.java. Its run uses `main.post(() -> { if (!closed) callback.success(result); });`. |
| Lifecycle methods are Android's visibility/creation/destruction callbacks. @Override marks an inherited/interface implementation; super invokes the parent. A Bundle is saved screen state. | `@Override protected void onPause() { foreground = false; super.onPause(); }` — ui/ScannerActivity.java. RevealActivity.onSaveInstanceState saves only the selected source URI. |
| An Intent is an Android action request; explicit requests name the destination class. Uri is an address, often to provider content rather than a filesystem path. ContentResolver opens that content. FileProvider exposes selected private files with access grants. | `Intent intent = new Intent(this, RevealActivity.class).setData(selected);` — ui/ScannerActivity.java, reveal. `return () -> resolver.openInputStream(uri);` — image/ImageUtils.java, source. |
| The Activity Result API registers a typed operation contract and a result callback before launching another app's operation. | `private final ActivityResultLauncher<String[]> picker = registerForActivityResult(`<br>`        new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) load(uri); });` — ui/reveal/RevealActivity.java. |
| BroadcastReceiver handles Android event messages; IntentFilter selects an action; registration bounds its lifetime. | `ContextCompat.registerReceiver(this, screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF),`<br>`        ContextCompat.RECEIVER_NOT_EXPORTED);` — ui/reveal/RevealActivity.java, onStart; onStop unregisters. |
| Canvas is a drawing surface; Paint contains drawing style; Rect is an integer rectangle. A bitmap Canvas can draw prepared image pixels on a worker. | `Canvas canvas = new Canvas(target);`<br>`canvas.drawColor(0xff0a0e1a);`<br>`Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);` — image/ImageDecoder.java, renderOpaque. |
| ValueAnimator interpolates a numeric value over time and calls an update callback; invalidate requests redraw. It belongs to UI decoration, not the engine. | `sweep = ValueAnimator.ofFloat(0, 1);`<br>`sweep.setDuration(1400);`<br>`sweep.addUpdateListener(a -> { reveal = (float) a.getAnimatedValue(); invalidate(); });` — ui/DiffMapView.java, startSweep. |
| SQLite is the device's relational database. SQLiteOpenHelper manages its schema; SQLiteDatabase executes SQL; Cursor iterates rows; ContentValues maps column names to typed values. | `SQLiteDatabase db = helper.getWritableDatabase();` and `ContentValues values = new ContentValues();` — data/VaultRepository.java, record; `public final class VaultDatabaseHelper extends SQLiteOpenHelper` — data/VaultDatabaseHelper.java; repository findByHash uses `try (Cursor cursor = ... )` around its actual database query. |
| HTTP is a web request protocol; HTTPS adds transport encryption. HttpURLConnection sends a request. JSON is named-field structured text; JSONObject represents it in Java. These are outside the offline engine. | `HttpURLConnection c = (HttpURLConnection) url.openConnection();` and `c.setRequestMethod("POST");` — data/SyncClient.java, post; `JSONObject result = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));` — response. |
| An exception represents failure. throws propagates checked failures; catch handles them; finally cleans up on every exit. try-with-resources automatically calls close on AutoCloseable resources. | `try (WipingBuffer output = new WipingBuffer()) {` — core/GzipCodec.java, compress. `@Override public void close() { Arrays.fill(data, (byte) 0); }` — core/PayloadCodec.java, Payload. |
| synchronized serializes access using an object's monitor; volatile publishes a field between threads; AtomicInteger performs atomic integer updates. They are not substitutes for posting UI work to main. | `@Override public synchronized void write(int value)` — core/GzipCodec.java, WipingBuffer. `private final AtomicInteger secretGeneration = new AtomicInteger();` and `private volatile boolean closed;` — ui/hide/HideWork.java. |
| Bitwise & selects bits, OR (vertical bar) combines them, ^ finds differences, ~ inverts, << shifts left and >>> shifts right with zero fill. Java promotes byte/char operands to int. | `int value = (bytes[bit / 8] >>> (7 - bit % 8)) & 1;` — core/StegoCore.java, writeBytes. `(value & 255)` in pixelOrder reads a byte as unsigned; `(char) ('0' + tokens[i])` in GestureCredential.canonical makes an ASCII digit. |
| byte is signed 8-bit (-128..127); char is an unsigned UTF-16 code unit; int is signed 32-bit; long is signed 64-bit; double is floating point. A cast requests a conversion. | `int nameLength = input.getShort() & 65535;` — core/PayloadCodec.java, unpack. `long count = (long) width * height;` — image/ImageMemory.java, pixelCount, widens before multiplying. |
| ByteBuffer manages a byte array with typed fields; ByteOrder.BIG_ENDIAN writes high-order bytes first. StandardCharsets.UTF_8 converts text to bytes; CharBuffer wraps characters; CodingErrorAction.REPORT rejects malformed input. | `ByteBuffer input = ByteBuffer.wrap(packed).order(ByteOrder.BIG_ENDIAN);` — core/PayloadCodec.java, unpack; `.onMalformedInput(CodingErrorAction.REPORT)` — decodeName. |
| Streams read/write bytes sequentially. ByteArrayInputStream reads a memory array; ByteArrayOutputStream grows a memory output buffer; DataInputStream reads multibyte fields; BufferedInputStream batches underlying reads. Files.move replaces filesystem entries; atomic means one indivisible replacement when supported. | `DataInputStream input = new DataInputStream(new BufferedInputStream(opened));` — image/PngIntegrity.java, read; `Files.move(temporary.toPath(), destination.toPath(),`<br>`        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);` — image/ImageFiles.java, savePng. |

A Java array clone is a separate array; final makes its reference fixed, not its contents immutable. Arrays.fill clears a caller-owned array; it does not erase copies held elsewhere. String is immutable text, so secret arrays are preferred where this code controls ownership. null means no object; in StegoCore's private order parameter it deliberately means sequential traversal. Package-private classes/helpers have no public access modifier, keeping callers behind the public facade.

## Likely viva questions

1. **Why two decoding paths?** Covers may be sampled/resized/flattened before embedding. Reveal/Scan need original dimensions/order/values and reject unsupported originals rather than transform them.
2. **Does saving .png or JPEG quality100 preserve the vault?** Only explicitly lossless PNG encoding and preserving its pixels does; extension or JPEG quality cannot guarantee original low bits.
3. **What does PSNR48.13dB mean in the one-pixel example?** RGB all changing by1 gives MSE1 and10log10(255²)=48.13dB; it is image similarity, not encryption strength.
4. **Why check CRC before decoding?** BitmapFactory may accept partial pixels from broken PNG. The scanner rejects structural/checksum damage, while GCM separately authenticates the hidden payload.
5. **Does the memory estimate prevent every crash?** No. It reserves conservative space and rejects oversized plans, but native allocations/current use can still fail; outer workers handle OOM and offer recovery.

## Covers these files

- `app/src/main/java/com/example/kineticvault/core/Constants.java`
- `app/src/main/java/com/example/kineticvault/core/CryptoCore.java`
- `app/src/main/java/com/example/kineticvault/core/GestureCredential.java`
- `app/src/main/java/com/example/kineticvault/core/GzipCodec.java`
- `app/src/main/java/com/example/kineticvault/core/PayloadCodec.java`
- `app/src/main/java/com/example/kineticvault/core/StegoCore.java`
- `app/src/main/java/com/example/kineticvault/core/VaultEngine.java`
- `app/src/main/java/com/example/kineticvault/core/VaultHeader.java`
- `app/src/main/java/com/example/kineticvault/core/WorkProgress.java`
- `app/src/main/java/com/example/kineticvault/image/CoverGenerator.java`
- `app/src/main/java/com/example/kineticvault/image/ImageDecoder.java`
- `app/src/main/java/com/example/kineticvault/image/ImageFiles.java`
- `app/src/main/java/com/example/kineticvault/image/ImageMemory.java`
- `app/src/main/java/com/example/kineticvault/image/ImageStats.java`
- `app/src/main/java/com/example/kineticvault/image/ImageUtils.java`
- `app/src/main/java/com/example/kineticvault/image/PngIntegrity.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/HideWork.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/HideStore.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/HideForge.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/CoverStep.java`
- `app/src/main/java/com/example/kineticvault/ui/reveal/RevealWork.java`
- `app/src/main/java/com/example/kineticvault/ui/reveal/RevealActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/ScannerActivity.java`
- `app/src/main/java/com/example/kineticvault/data/VaultRepository.java`
- `app/src/main/java/com/example/kineticvault/data/VaultDatabaseHelper.java`
- `app/src/main/java/com/example/kineticvault/data/SyncClient.java`
- `app/src/main/java/com/example/kineticvault/ui/DiffMapView.java`
- `docs/viva/_RULES.md`
- `docs/viva/00_PROJECT_MAP.md`
