# 00 — Project map

## How to use this map

Kinetic Vault is an Android app that encrypts text or one file and stores the encrypted bytes in a PNG image. Encryption makes the content unreadable without credentials; steganography means carrying those encrypted bytes inside ordinary image colour values. PNG is a lossless image format: its encoding preserves pixel values. A pixel is one image cell; RGB means its red, green and blue amounts. An LSB (least significant bit) is the lowest binary digit in one amount.

A package groups Java classes by responsibility. A class defines an object or static operations. UI means user interface (screens and controls); engine means the reusable motion/cryptography/bit-processing logic; data means local records/settings/synchronization; visuals means drawing/image assets; server means the optional PHP/MySQL endpoint. An Activity is an Android screen. XML is the markup used for layouts/resources; a resource is a named Android asset compiled into R identifiers. Build files configure compilation, not app screens. Test files below are mapped as source artifacts, without claiming execution outcomes.

Paths in the tree are relative to the project root. Java paths in the class atlas are relative to app/src/main/java/com/example/kineticvault/; resource paths are relative to app/src/main/res/. Entries describe project inputs, excluding Git internals, IDE caches and generated build outputs. Binary launcher/wrapper assets are identified by their asset role, not decompiled. local.properties values are machine-specific and are not reproduced.

## Shared vocabulary for the code references

A worker thread executes background tasks; the main/UI thread handles taps, screen state and drawing. ExecutorService queues managed tasks, Executors.newSingleThreadExecutor serializes them, Handler posts results to a message queue and Looper services that queue; Looper.getMainLooper is Android's UI loop. Synchronous means a method completes before returning; asynchronous means a callback receives the result later. A lifecycle method is called by Android when a screen is created, started, resumed, paused, stopped or destroyed. A generation counter discards obsolete results. A callback is a supplied function invoked by its owner; lambda arrow syntax and method-reference :: are compact ways to supply one.

Canvas is a drawing surface, Paint its style, and ValueAnimator an animated value with update callbacks. A Bitmap stores image pixels; ARGB_8888 means8-bit opacity/red/green/blue; sRGB is the requested colour space; a thumbnail is a separate display copy. An enum is a fixed named choice, an interface defines required methods, and a generic type such as List<VaultEntry> specifies collection element/result type. An anonymous class supplies one implementation inline; a nested class groups related state. A Bundle stores Activity recreation state. A ViewFlipper switches between child views, ListView displays adapter-provided rows, and WebView embeds web content. An AlertDialog is a modal confirmation/input window; a bottom sheet presents actions from the lower edge. A BroadcastReceiver handles an Android event message; its IntentFilter chooses matching actions. An Intent requests navigation or an external operation. A Uri addresses provider content; ContentResolver opens it. FileProvider exposes only allowed private-file subtrees with temporary URI grants. Activity Result API registers a typed external-operation contract and callback.

SQLite is the on-device relational database, SQLiteOpenHelper manages its schema, SQLiteDatabase executes SQL, Cursor iterates result rows and ContentValues maps values to columns. SharedPreferences is a private named key/value settings store. MySQL is the separate server database, accessed here by PHP using PDO prepared statements. A transaction commits related changes together; a trigger runs automatic SQL on a matching change; an outbox is a durable upload queue; a tombstone is a retained deletion marker. CRUD means create/read/update/delete. A UUID is a record identifier; a BLOB is stored bytes. JSON is a named-field text format; HttpURLConnection sends HTTP requests, HTTPS encrypts transport, MIME identifies content type, and an API key authenticates the metadata service.

Plaintext is readable secret content; ciphertext is encrypted bytes. PBKDF2 derives a key from password characters and random salt; AES-GCM encrypts with an authentication tag, and AAD is public metadata included in authentication. IV means initialization vector, a per-encryption input. SHA-256 makes a fingerprint; Base64 encodes bytes as text and is not encryption. GZIP is a compression envelope. UTF-8 encodes text bytes, UTF-16 is Java char representation, big endian writes most significant bytes first, and a byte contains8bits. LSB means least significant bit; RGB is red/green/blue. PSNR compares image quality in decibels using mean squared error. FLAG_SECURE asks Android to protect a window from capture. dp measures density-independent size and sp additionally follows font scaling. R is the generated resource identifier class. SDK is the Android software development kit; Gradle configures the build and R8 optimizes/shrinks release code.

The real excerpts below explain the mechanisms referenced by the map/index. For operation-specific ownership, failure paths and constants, continue to the class atlas/feature rows and 02_ARCHITECTURE.md.

## Java and Android constructs: tiny real examples

The examples below are source excerpts, each no longer than10 lines. They explain language/framework mechanisms used by the actual flows rather than giving replacement implementations.

### Interfaces, enums, callbacks, generics and method references

An interface specifies callable behavior without fixing the implementation. An enum is a fixed set of named values. A method reference uses :: to pass an existing operation. Generic types put the element/result type between angle brackets. From core/WorkProgress.java:

```java
public interface WorkProgress {
    enum Stage { PACK, COMPRESS, KEY, ENCRYPT, EMBED, EXTRACT, DECRYPT, UNPACK }
    void onProgress(Stage stage, int completed, int total);
```

From ui/hide/HideWork.java:

```java
interface Callback<T> { void success(T value); void failure(int message); }
private final ExecutorService worker = Executors.newSingleThreadExecutor();
private final Handler main = new Handler(Looper.getMainLooper());
```

From ui/vault/VaultActivity.java, field initialization:

```java
private final Runnable search = this::load;
```

Runnable represents an action with no returned value; Callable<T> returns T; Consumer<T> accepts one value; BooleanSupplier returns a boolean decision (used for network pause). List<VaultEntry>/ArrayList<> are typed growable collections. A wildcard ? in Future<?> means the result type is not needed. Class<?> in Home holds a screen class reference. Annotations such as @Test identify runner methods, @RunWith chooses a runner and @SuppressLint suppresses a named static-analysis warning. extends inherits class behavior; implements supplies an interface contract; static members belong to the class; final references cannot be reassigned. varargs float... in GestureRecognizer.finite accepts multiple floats as an array.

### Anonymous and nested/inner classes

An anonymous class is a one-off implementation created inline. A static nested class groups a related record without holding its parent's object; an inner class can access its containing object. From ui/hide/HideActivity.java:

```java
getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
    @Override public void handleOnBackPressed() { back(); }
});
```

CryptoCore.Sealed, PayloadCodec.Payload, HideForge.Result, SyncManager.Status and HarnessReport.Entry are nested data holders. GesturePadView.PadAccessibility is an inner AccessibilityDelegate that uses its host's selected token and labels; accessibility means presenting actions/content for assistive tools. Anonymous GestureSession.Listener and GestureRecognizer.Listener implement event callbacks. VaultApplication's anonymous ActivityLifecycleCallbacks tracks screen count. VaultAdapter's anonymous LruCache overrides sizeOf to measure bitmap bytes; an LRU cache discards least recently used entries when its4MiB budget is exceeded.

### Resource ownership, try-with-resources and exceptions

try-with-resources closes AutoCloseable objects on every exit. AutoCloseable is a close() contract, which Payload also uses to wipe its owned byte array. From image/ImageFiles.java, write:

```java
try (FileOutputStream output = new FileOutputStream(file)) {
    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
        throw new IOException("PNG encoding failed");
    }
    output.flush();
    output.getFD().sync();
}
```

Multi-catch (IOException | RuntimeException) handles alternative failures in one block. A throws declaration propagates a checked error for the caller to handle; IOException is an input/output failure. finally does cleanup after success/error/cancellation. CancellationException marks obsolete work, InterruptedIOException marks interrupted IO; Future.cancel(true)/shutdownNow request interruption. shutdown drains submitted work. Bitmap.recycle releases owned bitmap allocation; calling it while still displayed would violate ownership, so hosts detach first.

synchronized serializes shared-state access; volatile publishes flags/references across threads; AtomicInteger performs thread-safe generation increments. These do not make arbitrary whole UI operations thread-safe; Android UI changes still return to main. Java try/finally array fills cannot guarantee erasure of platform/native copies.

### Byte/character conversions and bit operations

From core/GestureCredential.java, writePassphrase:

```java
for (char character : passphrase) {
    for (int shift = 12; shift >= 0; shift -= 4) {
        out[offset++] = digits[(character >>> shift) & 15];
    }
}
```

From core/StegoCore.java, writeBytes:

```java
int shift = 16 - (bit % 3) * 8;
int value = (bytes[bit / 8] >>> (7 - bit % 8)) & 1;
pixels[index] = (pixels[index] & ~(1 << shift)) | (value << shift);
```

Java promotes byte/char to int for arithmetic; signed byte right-shift effects are masked before use. (byte) casts low8 bits back; &65535 reads an unsigned two-byte filename length; &0xffffffffL reads an unsigned CRC32. ByteBuffer/ByteOrder read/write exact big-endian fields; StandardCharsets.UTF_8 encoders/decoders and CodingErrorAction.REPORT reject malformed filenames/imported text. CharBuffer wraps/transfers characters without requiring a permanent secret String. Arrays.fill clears owned arrays, Arrays.copyOf/cloning duplicate ownership explicitly. StringBuilder is mutable text for ordinary labels/calculator display; BigDecimal/MathContext.DECIMAL64 supply decimal arithmetic and bounded precision in Calculator.

### Intent, Uri, ContentResolver and Activity Result API

From ui/reveal/RevealActivity.java:

```java
private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) load(uri); });
```

From image/ImageUtils.java, source(ContentResolver,Uri):

```java
return () -> resolver.openInputStream(uri);
```

From ui/ScannerActivity.java, reveal:

```java
Intent intent = new Intent(this, RevealActivity.class).setData(selected);
intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(intent);
```

OpenDocument selects provider content; CreateDocument returns a writable destination; TakePicture receives a pre-created output URI; StartActivityForResult handles external viewer return. FileProvider content URIs and ClipData carry grantable file references. MIME type selects compatible handlers; chooser lets user choose a receiver. No URI thumbnail shortcut or EXIF transform is used for Reveal/Scan.

### BroadcastReceiver and lifecycle

From ui/reveal/RevealActivity.java, onStart:

```java
ContextCompat.registerReceiver(this, screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF),
        ContextCompat.RECEIVER_NOT_EXPORTED);
registered = true;
```

Its anonymous BroadcastReceiver.onReceive calls lock/discardExport/finish only when autoLock is enabled. onStop unregisters. SyncManager has a different receiver and IntentFilter for AIRPLANE_MODE_CHANGED; app lifecycle starts/stops it. IntentFilter specifies which broadcast action is relevant; ContextCompat supplies compatible registration flags. No manifest receiver is hidden elsewhere.

### Canvas, Paint, ValueAnimator and touch

From ui/DiffMapView.java, startSweep:

```java
sweep = ValueAnimator.ofFloat(0, 1);
sweep.setDuration(1400);
sweep.addUpdateListener(a -> { reveal = (float) a.getAnimatedValue(); invalidate(); });
sweep.start();
```

From ui/XRayView.java, onDraw:

```java
canvas.drawBitmap(plane, null, area, paint);
```

invalidate requests redraw; postInvalidateOnAnimation schedules the next display frame. Rect/RectF are integer/floating drawing rectangles; Path/PathMeasure define and measure the splash outline. Canvas.save/clipRect/restoreToCount limits the wipe/sweep drawing region without changing the source bitmap. MotionEvent supplies touch action/coordinates/pointer IDs; onTouchListener in DiffMapView maps that position to exact pixel indexes. performClick preserves accessible click semantics; GesturePad accessibility actions and keyboard keys emit the same tokens. HapticFeedbackConstants request system touch/vibration feedback.

### SQLite classes

From data/VaultRepository.java, search (excerpt):

```java
try (Cursor cursor = helper.getReadableDatabase().query("vaults", null, selection.toString(),
        args.toArray(new String[0]), null, null, "created_at DESC, id DESC")) {
    while (cursor.moveToNext()) rows.add(VaultEntry.read(cursor));
}
```

SQLiteOpenHelper creates/upgrades; SQLiteDatabase executes queries/updates/transactions; Cursor owns row iteration; ContentValues supplies typed update fields. Placeholders ? bind values separately from SQL syntax. VaultRepository.escape also escapes LIKE wildcard characters so title search is literal. Readable/writable database handles do not make DB calls suitable for main; repository enforces worker access. getColumnIndexOrThrow names schema columns explicitly; isNull preserves nullable expiry/coordinates. Triggers/outbox/revision logic are described above.

### HttpURLConnection and JSON

From data/SyncClient.java, post (excerpt):

```java
c.setConnectTimeout(5000); c.setReadTimeout(5000); c.setInstanceFollowRedirects(false);
c.setRequestMethod("POST"); c.setDoOutput(true); c.setUseCaches(false);
c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
c.setRequestProperty("X-API-KEY", key);
byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
```

JSONObject.put creates named fields; optBoolean/optLong read optional values; JSONObject.NULL represents JSON null rather than Java missing references. URL validates address components, NetworkSecurityPolicy enforces Android cleartext policy, and HttpURLConnection must disconnect in finally. java.util.function.BooleanSupplier supplies active/airplane cancellation checks. The app constructs an explicit allowlist in SyncStore rather than serializing the entire VaultEntry.

### All javax.crypto, java.security and java.util.zip types used

| Type and real usage | What it means / tiny real source example |
| --- | --- |
| java.security.SecureRandom; CryptoCore, DisguiseCode, GhostComparisonWork | Cryptographic random generator; `new SecureRandom().nextBytes(salt);` from DisguiseCode.create. |
| java.security.MessageDigest; ImageFiles, DisguiseCode, SyncManager | SHA-256 file/destination fingerprint and comparison helper; `MessageDigest.getInstance("SHA-256");` from ImageFiles.sha256; `MessageDigest.isEqual(expected, actual)` from DisguiseCode.matches. |
| java.security.GeneralSecurityException; core/helpers/demo | Checked superclass for cryptographic failures; `throws GeneralSecurityException` on CryptoCore transformation path. |
| java.security.NoSuchAlgorithmException; ImageFiles | Requested digest missing; `catch (NoSuchAlgorithmException failure)` maps to IOException. |
| javax.crypto.SecretKeyFactory; CryptoCore, DisguiseCode | Runs PBKDF2 key derivation; `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");` in CryptoCore.derive. |
| javax.crypto.SecretKey; CryptoCore | Provider key object returned by derivation; `generated = factory.generateSecret(specification);` then getEncoded/destroy attempt. |
| javax.crypto.spec.PBEKeySpec; CryptoCore, DisguiseCode | Password/salt/iteration/key-length request; `specification.clearPassword();` clears its password copy. |
| javax.crypto.spec.SecretKeySpec; CryptoCore | Wraps derived bytes as an AES key; `key = new SecretKeySpec(keyBytes, "AES");` in transform. |
| javax.crypto.Cipher; CryptoCore | Performs AES-GCM operation; `Cipher.getInstance("AES/GCM/NoPadding");` and `return cipher.doFinal(input);`. |
| javax.crypto.spec.GCMParameterSpec; CryptoCore | Tag length in bits and IV; `new GCMParameterSpec(128, header.iv())` in cipher.init. |
| javax.crypto.AEADBadTagException; VaultEngineTest import, core comment/UI boundary | Authenticated-encryption tag failure subtype; test `assertThrows(AEADBadTagException.class,` wraps a wrong-credential reveal call. Core propagates it rather than claiming its cause is uniquely known. |
| javax.security.auth.DestroyFailedException; CryptoCore (related cleanup type) | A key provider may not support destroy; `catch (DestroyFailedException ignored)` avoids logging secrets or promising erasure. |
| java.util.zip.GZIPOutputStream; GzipCodec, PayloadCodecTest | Writes GZIP envelope; `private static final class ConfiguredGzip extends GZIPOutputStream`. |
| java.util.zip.Deflater; GzipCodec | Compression level constants; `def.setLevel(compress ? Deflater.DEFAULT_COMPRESSION : Deflater.NO_COMPRESSION);`. |
| java.util.zip.GZIPInputStream; GzipCodec | Reads bounded GZIP bytes; `new GZIPInputStream(new ByteArrayInputStream(compressed))` in decompress. |
| java.util.zip.CRC32; PngIntegrity, PngFixtures | Noncryptographic PNG chunk checksum; `CRC32 crc = new CRC32();` then update/getValue. Detects framing/data corruption, not adversarial authenticity. |
| java.util.zip.DeflaterOutputStream; androidTest/image/PngFixtures | Writes zlib-compressed PNG image rows without allocating huge bitmaps; `new DeflaterOutputStream(compressed)` in opaque. Only fixture code uses this stream directly. |

java.util.Random (StegoCore/CoverGenerator/MatrixRainView) is separate from these namespaces and is deterministic/presentation randomness, not suitable for generating cryptographic secrets. UUID.randomUUID creates identities for images/events/installations; identity randomness is distinct from key material. Base64 text encodes salt/hash/preference signature; encoding is not encryption.


## Complete project-input tree

Every file shown has its purpose and role on its own line. Group directories show the containing package/resource family.

```text
├── .gitignore — Root Git exclusions [docs/build]
├── AGENTS.md — Repository working constraints [docs]
├── app/
│   ├── .gitignore — Module Git exclusions [docs/build]
│   ├── build.gradle.kts — Android app SDK37/min26/Java11 and dependency setup [UI/build]
│   └── src/
│       ├── androidTest/
│       │   └── java/
│       │       └── com/
│       │           └── example/
│       │               └── kineticvault/
│       │                   ├── ExampleInstrumentedTest.java — Contains the template Android package-context example. [engine/data/visuals; non-user test source]
│       │                   └── image/
│       │                       ├── ImageLayerTest.java — Describes intended Android image, PNG, FileProvider and worker-thread behavior. [engine/data/visuals; non-user test source]
│       │                       └── PngFixtures.java — Creates small-memory synthetic PNG inputs for ImageLayerTest. [engine/data/visuals; non-user test source]
│       ├── debug/
│       │   └── res/
│       │       └── xml/
│       │           └── network_security_config.xml — Debug-only exact-host HTTP exceptions over a deny-cleartext base [data]
│       ├── main/
│       │   ├── AndroidManifest.xml — App components, launch/share filters, permission and provider declaration [UI/data]
│       │   ├── assets/
│       │   │   └── how_it_works.html — Offline interactive pixel/LSB/encryption lesson [UI/visuals]
│       │   ├── java/
│       │   │   └── com/
│       │   │       └── example/
│       │   │           └── kineticvault/
│       │   │               ├── core/
│       │   │               │   ├── Constants.java — Keeps format sizes, allocation limits, permanent tokens and motion thresholds in one place. [engine]
│       │   │               │   ├── CryptoCore.java — Derives an AES key and authenticates or encrypts the compressed payload. [engine]
│       │   │               │   ├── GestureCredential.java — Encodes tokens and passphrase into the exact password characters and estimates token entropy. [engine]
│       │   │               │   ├── GzipCodec.java — Wraps every packed payload in bounded GZIP, with optional compression. [engine]
│       │   │               │   ├── PayloadCodec.java — Turns text/file metadata and exact content into a portable byte sequence and back. [engine]
│       │   │               │   ├── StegoCore.java — Embeds and extracts encrypted bits in opaque RGB pixel arrays. [engine]
│       │   │               │   ├── VaultEngine.java — Combines packing, GZIP, encryption and embedding into the frozen synchronous engine. [engine]
│       │   │               │   ├── VaultHeader.java — Owns an immutable defensive copy of the public 38-byte version-1 header. [engine]
│       │   │               │   └── WorkProgress.java — Defines the engine progress callback contract. [engine]
│       │   │               ├── data/
│       │   │               │   ├── DisguiseCode.java — Hashes and compares the calculator code independently of vault encryption. [data]
│       │   │               │   ├── HistoryCsv.java — Exports only the local event ledger as CSV. [data]
│       │   │               │   ├── SyncClient.java — Sends bounded JSON POST requests to the configured ledger. [data]
│       │   │               │   ├── SyncManager.java — Coordinates durable metadata uploads while an app screen is visible. [data]
│       │   │               │   ├── SyncStore.java — Reads the local metadata outbox and acknowledges uploads. [data]
│       │   │               │   ├── VaultDatabaseHelper.java — Creates and upgrades the private SQLite schema and queue triggers. [data]
│       │   │               │   ├── VaultEntry.java — Carries local encrypted-image metadata between database and screens. [data]
│       │   │               │   ├── VaultLogger.java — Keeps a private byte-for-byte encrypted-image copy and records metadata events. [data]
│       │   │               │   ├── VaultPreferences.java — Loads/saves settings off-thread and exposes an immutable in-memory snapshot. [data]
│       │   │               │   └── VaultRepository.java — Provides synchronous SQLite creation, searches, updates and event transactions. [data]
│       │   │               ├── diagnostics/
│       │   │               │   ├── HarnessReport.java — Owns public-demo timings, image statistics and an optional preview. [UI]
│       │   │               │   ├── HarnessRunner.java — Runs the four public-data PNG round-trip demonstrations when invoked by the user. [UI]
│       │   │               │   └── TestHarnessActivity.java — Displays the public integrity demo and temporary light-theme preview. [UI]
│       │   │               ├── gesture/
│       │   │               │   ├── GesturePadView.java — Draws six accessible controls that emit the same named tokens as motion. [visuals]
│       │   │               │   ├── GestureRecognizer.java — Converts fused motion samples into six repeatable tokens. [engine]
│       │   │               │   ├── GestureSession.java — Owns a transient 4..8-token recording and enrollment comparison. [engine]
│       │   │               │   └── StrengthMeter.java — Labels the uniform-token entropy upper bound and passphrase advice. [engine]
│       │   │               ├── image/
│       │   │               │   ├── CoverGenerator.java — Creates an opaque procedural gradient/plasma/noise cover offline. [visuals]
│       │   │               │   ├── ImageDecoder.java — Separates resizable covers from unchanged exact image decoding. [visuals]
│       │   │               │   ├── ImageFiles.java — Saves PNG safely and streams file fingerprints. [visuals]
│       │   │               │   ├── ImageMemory.java — Computes conservative image allocation limits and cover dimensions. [visuals]
│       │   │               │   ├── ImageStats.java — Computes exact RGB differences and peak signal-to-noise ratio. [visuals]
│       │   │               │   ├── ImageUtils.java — Offers the shared public image/file/pixel facade. [visuals]
│       │   │               │   └── PngIntegrity.java — Rejects broken PNG framing and checksums before BitmapFactory accepts partial pixels. [visuals]
│       │   │               ├── sensor/
│       │   │               │   └── SensorFusionManager.java — Combines accelerometer and gyro orientation and feeds the recognizer. [engine]
│       │   │               ├── ui/
│       │   │               │   ├── AboutActivity.java — Shows the static app and workstream description. [UI]
│       │   │               │   ├── CalculatorActivity.java — Offers arithmetic and optional launcher-disguise code entry. [UI]
│       │   │               │   ├── DiffMapData.java — Owns exact source arrays and worker-built difference/X-ray previews. [visuals]
│       │   │               │   ├── DiffMapView.java — Draws animated difference maps and a touch pixel inspector. [visuals]
│       │   │               │   ├── DiffStats.java — Formats exact image statistics for a TextView. [visuals]
│       │   │               │   ├── ExportSheet.java — Creates a reusable Material bottom sheet with supplied export actions. [UI]
│       │   │               │   ├── GhostComparisonActivity.java — Shows Standard/Ghost public-demo difference maps side by side. [UI]
│       │   │               │   ├── GhostComparisonWork.java — Builds two placement demonstrations using public content and ephemeral credentials. [visuals]
│       │   │               │   ├── hide/
│       │   │               │   │   ├── CoverStep.java — Controls gallery/camera/generated cover input in Hide. [UI]
│       │   │               │   │   ├── GestureReplay.java — Privately replays only the current matched enrollment. [UI]
│       │   │               │   │   ├── GestureStep.java — Collects and compares two Hide gesture performances. [UI]
│       │   │               │   │   ├── HideActivity.java — Coordinates the five-step Hide screen and its lifecycle. [UI]
│       │   │               │   │   ├── HideExporter.java — Copies original encrypted PNG bytes to gallery/document or prepares granted share URI. [UI]
│       │   │               │   │   ├── HideForge.java — Runs engine, PNG rendering and result asset creation. [UI]
│       │   │               │   │   ├── HideStore.java — Owns cover, prepared payload and rendered output on one worker. [UI]
│       │   │               │   │   ├── HideWork.java — Serializes Hide disk/image/crypto work and marshals callbacks to main. [UI]
│       │   │               │   │   ├── ProofSheet.java — Renders a disclosure-limited comparison PNG for presentation. [UI]
│       │   │               │   │   ├── ResultStep.java — Presents result visuals and encrypted-PNG/proof export actions. [UI]
│       │   │               │   │   ├── SecretInput.java — Owns an erasable snapshot of text or a selected file for payload preparation. [UI]
│       │   │               │   │   ├── SecretStep.java — Controls text/file, UTF-8 import, compression, Ghost, passphrase and measured capacity. [UI]
│       │   │               │   │   └── TextImport.java — Imports bounded strict UTF-8 text without retaining an immutable plaintext String. [UI]
│       │   │               │   ├── HomeActivity.java — Provides main feature navigation and foreground Cloud Ledger status. [UI]
│       │   │               │   ├── InfoActivity.java — Shares safe insets and Back controls for small information screens. [UI]
│       │   │               │   ├── KineticOrbView.java — Draws angle-following orb, fading trail and accepted-token dial. [visuals]
│       │   │               │   ├── LearnActivity.java — Shows bundled interactive education and restricted online reading. [UI]
│       │   │               │   ├── MatrixRainView.java — Draws decorative binary rain responsive to real forge progress. [visuals]
│       │   │               │   ├── NerdOverlay.java — Shows transient actual sensor samples and token names during recording. [UI]
│       │   │               │   ├── reveal/
│       │   │               │   │   ├── RevealActivity.java — Coordinates original-image selection, credential capture, authenticated reveal and plaintext export. [UI]
│       │   │               │   │   ├── RevealExpiry.java — Enforces a reversible local device-clock expiry policy for new unlocks. [UI]
│       │   │               │   │   ├── RevealExport.java — Owns temporary decrypted export and destination copying. [UI]
│       │   │               │   │   ├── RevealGesture.java — Collects one transient gesture for Reveal. [UI]
│       │   │               │   │   ├── RevealLedger.java — Keeps encrypted-image identity and best-effort event logging separate from secrets. [UI]
│       │   │               │   │   └── RevealWork.java — Owns Reveal worker, encrypted pixels and erasable decrypted state. [UI]
│       │   │               │   ├── ScannerActivity.java — Detects structurally valid vault headers in original incoming/selected images. [UI]
│       │   │               │   ├── SettingsActivity.java — Edits settings, calculator code and ledger connection details. [UI]
│       │   │               │   ├── SplashActivity.java — Waits for preferences and routes normal launch to Home or Calculator. [UI]
│       │   │               │   ├── SplashLogoView.java — Draws a shield path progressively during normal launch. [visuals]
│       │   │               │   ├── TextScrambler.java — Animates revealed text while owning erasable character arrays. [UI]
│       │   │               │   ├── UiPolish.java — Applies shared entrance motion, haptics and accessible notices. [UI]
│       │   │               │   ├── vault/
│       │   │               │   │   ├── VaultActivity.java — Displays saved vaults with title/date filters and context actions. [UI]
│       │   │               │   │   ├── VaultAdapter.java — Adapts encrypted-image metadata into image-and-text ListView rows. [UI]
│       │   │               │   │   └── VaultDialogs.java — Builds rename, expiry and creation-date filter dialogs. [UI]
│       │   │               │   ├── VaultDoorView.java — Visualizes authenticated opening or denied unlock. [visuals]
│       │   │               │   ├── WipeComparisonView.java — Renders before/after display thumbnails with a movable divider. [visuals]
│       │   │               │   ├── WipeSliderView.java — Combines image comparison with an accessible SeekBar. [visuals]
│       │   │               │   └── XRayView.java — Displays a prebuilt amplified RGB least-bit plane. [visuals]
│       │   │               └── VaultApplication.java — Initializes the process, loads preferences, forces the initial dark theme and starts or stops foreground sync. [UI]
│       │   ├── keepRules/
│       │   │   └── rules.keep — Comment-only R8 shrinker-rule template [UI/build]
│       │   └── res/
│       │       ├── anim/
│       │       │   ├── fade_in.xml — 240ms opacity entrance [visuals]
│       │       │   ├── pulse.xml — 900ms reversing repeating opacity pulse [visuals]
│       │       │   ├── shake.xml — 60ms repeating horizontal denial shake [visuals]
│       │       │   └── slide_up.xml — 280ms upward/opacity entrance [visuals]
│       │       ├── drawable/
│       │       │   ├── bg_glow_card_cyan.xml — Layered cyan glow card background [visuals]
│       │       │   ├── bg_glow_card_violet.xml — Layered violet glow card background [visuals]
│       │       │   ├── ic_arrow_down.xml — Vector arrow down icon [visuals]
│       │       │   ├── ic_arrow_left.xml — Vector arrow left icon [visuals]
│       │       │   ├── ic_arrow_right.xml — Vector arrow right icon [visuals]
│       │       │   ├── ic_arrow_up.xml — Vector arrow up icon [visuals]
│       │       │   ├── ic_history.xml — Vector history icon [visuals]
│       │       │   ├── ic_launcher_background.xml — Fixed dark launcher background [visuals]
│       │       │   ├── ic_launcher_foreground.xml — Shield-and-lock launcher foreground [visuals]
│       │       │   ├── ic_lock.xml — Vector lock icon [visuals]
│       │       │   ├── ic_orb.xml — Vector orb icon [visuals]
│       │       │   ├── ic_scan.xml — Vector scan icon [visuals]
│       │       │   ├── ic_settings.xml — Vector settings icon [visuals]
│       │       │   └── ic_shield.xml — Vector shield icon [visuals]
│       │       ├── layout/
│       │       │   ├── activity_about.xml — About text and three workstream labels [UI]
│       │       │   ├── activity_calculator.xml — Calculator display and tagged digit/operator controls [UI]
│       │       │   ├── activity_ghost_comparison.xml — Two diff maps, exact pixel inspectors, statistics, regenerate and replay [UI]
│       │       │   ├── activity_hide.xml — Five-step ViewFlipper shell with progress, notice and Back [UI]
│       │       │   ├── activity_home.xml — Navigation tiles, decorative orb and Cloud Ledger status [UI]
│       │       │   ├── activity_learn.xml — Local/remote WebView controls and recovery panel [UI]
│       │       │   ├── activity_reveal.xml — Vault door, image picker, credentials, transient text and file export [UI]
│       │       │   ├── activity_scanner.xml — Original-image input, detection notice, Reveal action and absent-header X-ray [UI]
│       │       │   ├── activity_settings.xml — Sensitivity spinner, preference checkboxes, server credentials and disguise code [UI]
│       │       │   ├── activity_test_harness.xml — Public integrity demo, progress, results, preview and light switch [UI]
│       │       │   ├── activity_vault.xml — Title/date search, saved-image list, CSV, empty and error states [UI]
│       │       │   ├── hide_step_cover.xml — Device/camera/generated cover preview and continue [UI]
│       │       │   ├── hide_step_forge.xml — Matrix rain, actual stage progress and cancellation [UI]
│       │       │   ├── hide_step_gesture.xml — Orb, pad, two-recording enrollment, strength and private replay [UI]
│       │       │   ├── hide_step_result.xml — Wipe/diff/loupe/X-ray/statistics and PNG/proof exports [UI]
│       │       │   ├── hide_step_secret.xml — Text/file radio choice, import, compression/Ghost/passphrase and measured capacity [UI]
│       │       │   ├── sheet_export.xml — Shared export bottom-sheet title, note, generated actions and Cancel [UI]
│       │       │   └── vault_row.xml — Encrypted-image thumbnail, neutral title, creation time, size and expiry [UI]
│       │       ├── mipmap-anydpi-v26/
│       │       │   ├── ic_launcher_round.xml — Launcher round icon for this Android resource configuration [visuals]
│       │       │   └── ic_launcher.xml — Launcher icon for this Android resource configuration [visuals]
│       │       ├── mipmap-hdpi/
│       │       │   ├── ic_launcher_round.webp — Launcher round icon for this Android resource configuration [visuals]
│       │       │   └── ic_launcher.webp — Launcher icon for this Android resource configuration [visuals]
│       │       ├── mipmap-mdpi/
│       │       │   ├── ic_launcher_round.webp — Launcher round icon for this Android resource configuration [visuals]
│       │       │   └── ic_launcher.webp — Launcher icon for this Android resource configuration [visuals]
│       │       ├── mipmap-xhdpi/
│       │       │   ├── ic_launcher_round.webp — Launcher round icon for this Android resource configuration [visuals]
│       │       │   └── ic_launcher.webp — Launcher icon for this Android resource configuration [visuals]
│       │       ├── mipmap-xxhdpi/
│       │       │   ├── ic_launcher_round.webp — Launcher round icon for this Android resource configuration [visuals]
│       │       │   └── ic_launcher.webp — Launcher icon for this Android resource configuration [visuals]
│       │       ├── mipmap-xxxhdpi/
│       │       │   ├── ic_launcher_round.webp — Launcher round icon for this Android resource configuration [visuals]
│       │       │   └── ic_launcher.webp — Launcher icon for this Android resource configuration [visuals]
│       │       ├── values/
│       │       │   ├── colors.xml — Semantic light/dark colors and glow tokens [UI/visuals]
│       │       │   ├── diff_strings.xml — Diff, exact pixel/LSB, X-ray and demo explanations [UI/visuals]
│       │       │   ├── dimens.xml — Spacing, typography, touch-size and shape dimensions [UI/visuals]
│       │       │   ├── gesture_ids.xml — Six accessibility action IDs for Gesture Pad [UI/visuals]
│       │       │   ├── scanner_strings.xml — Scanner and WebView labels, recovery and remote URL [UI/visuals]
│       │       │   ├── settings_strings.xml — Settings/About/Learn/import/export labels and sensitivity choices [UI/visuals]
│       │       │   ├── strings.xml — Main labels, recovery messages, accessibility text and feature explanations [UI/visuals]
│       │       │   ├── styles.xml — Reusable button/text appearance rules [UI/visuals]
│       │       │   ├── sync_strings.xml — Cloud Ledger state, queue and connection recovery [UI/visuals]
│       │       │   ├── themes.xml — Material 3 DayNight theme and semantic token mapping [UI/visuals]
│       │       │   └── vault_strings.xml — Local library, metadata-policy, CRUD and error copy [UI/visuals]
│       │       ├── values-night/
│       │       │   ├── colors.xml — Semantic light/dark colors and glow tokens [UI/visuals]
│       │       │   └── themes.xml — Material 3 DayNight theme and semantic token mapping [UI/visuals]
│       │       └── xml/
│       │           ├── backup_rules.xml — Legacy backup exclusions for app data [data]
│       │           ├── data_extraction_rules.xml — Cloud-backup and device-transfer exclusions [data]
│       │           ├── file_paths.xml — FileProvider allowlist for vaults/, shared/, capture/ and reveal/ [data]
│       │           └── network_security_config.xml — Release/main deny-cleartext policy with system trust anchors [data]
│       └── test/
│           └── java/
│               └── com/
│                   └── example/
│                       └── kineticvault/
│                           ├── core/
│                           │   ├── PayloadCodecTest.java — Describes intended payload validation and GZIP-envelope behavior. [engine/data/visuals; non-user test source]
│                           │   ├── StegoCoreTest.java — Describes intended header, capacity, LSB and Ghost behavior. [engine/data/visuals; non-user test source]
│                           │   └── VaultEngineTest.java — Describes intended engine round trips, credential canonicalization and authentication failures. [engine/data/visuals; non-user test source]
│                           ├── ExampleUnitTest.java — Contains the template addition example. [engine/data/visuals; non-user test source]
│                           └── image/
│                               └── ImagePolicyTest.java — Describes intended image sizing, memory and statistics behavior. [engine/data/visuals; non-user test source]
├── build.gradle.kts — Root Android plugin alias [UI/build]
├── design.md — Source-backed design resources and appearance guide [docs]
├── docs/
│   ├── AUDIT.md — Existing hardening-change notes [docs]
│   ├── BUILD_HEALTH.md — Historical build-health notes [docs]
│   ├── IMPLEMENTATION.md — Frozen wire-format authority plus historical Phase1B handoff [engine/docs]
│   ├── INVENTIONS.md — Local expiry, private replay and comparison-sheet concept notes [docs]
│   ├── PHONE_CHECKLIST.md — Historical phone demonstration checklist [docs]
│   ├── STATE_SNAPSHOT.md — Existing feature/signature state record, unchanged by D1 [docs]
│   └── viva/
│       ├── _PROGRESS.md — D2–D6 pending checklist [docs]
│       ├── _RULES.md — Verbatim supplied documentation rules [docs]
│       ├── 00_PROJECT_MAP.md — D1 package/file/component map and class/method atlas [docs]
│       ├── 01_FEATURE_INDEX.md — D1 user-visible feature-to-source index [docs]
│       └── 02_ARCHITECTURE.md — D1 flows, threads, algorithms, constructs and stores [docs]
├── gradle/
│   ├── gradle-daemon-jvm.properties — Java25 daemon toolchain selection [UI/build]
│   ├── libs.versions.toml — Version catalog for AGP, appcompat, Material and test libraries [UI/build]
│   └── wrapper/
│       ├── gradle-wrapper.jar — Binary wrapper bootstrap [UI/build]
│       └── gradle-wrapper.properties — Gradle9.6 distribution and checksum configuration [UI/build]
├── gradle.properties — Gradle memory/UTF-8/configuration-cache settings [UI/build]
├── gradlew — Unix Gradle wrapper entry script [UI/build]
├── gradlew.bat — Windows Gradle wrapper entry script [UI/build]
├── local.properties — Machine-local Android SDK configuration; values intentionally omitted [UI/build]
├── README.md — Project user manual and implementation references [docs]
├── server/
│   ├── .gitignore — Excludes deployment config.php from Git [server]
│   ├── .htaccess — Apache denies config/schema/setup downloads [server]
│   ├── api.php — Validated JSON/PDO ledger endpoint and action dispatch [server]
│   ├── config.sample.php — Database/API-key configuration template; no real deployment credentials [server]
│   ├── README_XAMPP.md — PHP/MySQL/XAMPP setup and metadata API contract [server]
│   └── schema.sql — MySQL vault/event tables and device-scoped keys [server]
├── settings.gradle.kts — Project/module names and dependency/plugin repositories [UI/build]
└── User Manual.md — User-facing manual [docs]
```

## Entry point and navigation

Android first creates VaultApplication and calls onCreate. The launcher intent filter then selects ui/SplashActivity. An Intent is an Android message requesting an operation; an explicit intent names the destination class. startActivity sends that request, and finish removes the current Activity from the back stack (the screen history).

Real example from ui/HomeActivity.java, onCreate:

```java
findViewById(R.id.home_hide).setOnClickListener(v -> startActivity(new Intent(this, HideActivity.class)));
```

The arrow is a lambda: a short function supplied for later invocation. The click listener is a callback: Android calls it when the user taps. findViewById retrieves a resource-ID-labelled control from the current layout. The R identifiers are generated from XML names.

| Source screen / method | Destination and how | Return/state behavior |
| --- | --- | --- |
| Android launcher | SplashActivity via MAIN + LAUNCHER | Exported public launch entrance. |
| SplashActivity.route | HomeActivity normally; CalculatorActivity when saved disguise is true; explicit Intent | Waits for preferences and foreground; normal logo delay 1200ms; finishes Splash. |
| CalculatorActivity.equalsPressed callback | HomeActivity via explicit Intent after code match | Finishes Calculator; ordinary calculation otherwise. |
| HomeActivity.onCreate | HideActivity, RevealActivity, VaultActivity, SettingsActivity, ScannerActivity, LearnActivity, TestHarnessActivity | Each named tile starts an explicit Activity; Home remains below it. |
| HomeActivity.overflow | SettingsActivity, LearnActivity, AboutActivity | PopupMenu builds runtime options; no res/menu files. |
| VaultActivity.bindList / open | RevealActivity with stored original image URI | Empty list action opens HideActivity, or clears filters if filtering. |
| ScannerActivity.reveal | RevealActivity with original selected URI and read grant | Header discovery enables this action; no plaintext is passed. |
| ResultStep constructor | GhostComparisonActivity via explicit Intent | Comparison generates new public demo data, not the user's secret. |
| Scanner manifest + receive/onNewIntent | ScannerActivity from other apps' image SEND/VIEW | singleTop reuses top instance; original content URI drives scan. |
| Small InfoActivity screens / Back buttons / Hide back / system Back | finish or existing back-stack navigation | Hide intermediate steps call showStep; forge cancellation uses a dialog. |
| CoverStep / Scanner launchers | External document picker or camera app | Activity Result API returns URI/success through registered callbacks. |
| ResultStep.prepareShare | System chooser then selected receiving app, ACTION_SEND image/png | FileProvider URI/read grant/ClipData; Document/File warning first. |
| RevealActivity.export | External document save picker or ACTION_VIEW viewer | Explicit plaintext-export step; temp file survives intended external viewer until return/cleanup. |

A URI (Uniform Resource Identifier, Android Uri) is an address such as content://provider/item, not necessarily a filesystem path. ContentResolver asks its provider for original streams. FileProvider publishes selected private files as content URIs, with temporary grants instead of exposing broad storage. Activity Result API means registering an operation contract and a result callback before launching it. See 02_ARCHITECTURE.md for real examples and lifecycle details.

## Manifest explained

The manifest is Android's component declaration. exported=true means other apps may start the component; false restricts it to this application. An intent filter declares matching external requests. Portrait fixes device axes for motion screens. adjustResize asks the window to fit above the keyboard; Java also applies system/keyboard insets (safe edge padding). launchMode=singleTop delivers a new request to onNewIntent if Scanner is already topmost.

| Declaration in app/src/main/AndroidManifest.xml | Why it exists |
| --- | --- |
| INTERNET | Optional Cloud Ledger HTTP(S) requests and remote Learn WebView. No CAMERA, storage, location or sensor permission is declared. External TakePicture delegates capture to a camera app. |
| application name=.VaultApplication | Process initialization and ActivityLifecycleCallbacks (Android notifications when screens start/stop/resume/pause). |
| allowBackup=false plus backup/dataExtraction XML | Disable app backups and exclude app files/database/preferences from legacy backup, cloud backup and device transfer rules. |
| Theme.KineticVault, icon/roundIcon, app label, supportsRtl | App-wide Material 3 DayNight appearance, launcher identity and right-to-left support declaration. |
| networkSecurityConfig | Main resource denies unencrypted HTTP; debug overlay allows exact development hosts only. |
| SplashActivity exported=true, portrait, adjustResize | Sole MAIN/LAUNCHER Activity and settings-dependent route. |
| ScannerActivity exported=true, singleTop | External image receiver and ordinary in-app Scanner. SEND filter: DEFAULT + image/*; VIEW filter: DEFAULT/BROWSABLE + content scheme + image/*. BROWSABLE permits matching external open links; this is not a universal URL handler. |
| HideActivity and RevealActivity exported=false, portrait, adjustResize | Internal gesture workflows and keyboard-safe secret/passphrase input. |
| TestHarnessActivity exported=false, portrait, adjustResize | Internal public-data Integrity Lab; it is no longer the launcher. |
| HomeActivity, CalculatorActivity, SettingsActivity, AboutActivity, LearnActivity, VaultActivity, GhostComparisonActivity exported=false | Internal feature/navigation screens. Calculator has Calculator label. Settings/Vault request adjustResize for inputs. |
| androidx.core.content.FileProvider, authority=${applicationId}.files | Runtime authority is com.example.kineticvault.files; maps allowlisted files to content URIs. exported=false, grantUriPermissions=true permit per-request grants. Meta-data android.support.FILE_PROVIDER_PATHS links res/xml/file_paths.xml. |

No receiver is declared in the manifest. A BroadcastReceiver is an object Android calls when an event message arrives. The app registers them dynamically for the needed lifetime: RevealActivity.screenOff listens for ACTION_SCREEN_OFF between onStart/onStop with RECEIVER_NOT_EXPORTED; SyncManager.receiver listens for AIRPLANE_MODE_CHANGED while any Activity is started, using RECEIVER_EXPORTED for the system broadcast. Reveal screen-off clearing respects the autoLock setting; recording credentials still clear when paused. These registrations are not permissions or persistent background services.

### Provider and network XML

- files/vaults/ is the app's retained encrypted library; cache/shared/ contains forged PNG and proof images; cache/capture/ holds camera input; cache/reveal/ holds deliberately exported plaintext. Only these subtrees appear in file_paths.xml.
- Main network_security_config.xml trusts system certificates and denies cleartext. Debug permits HTTP only to 10.0.2.2, 127.0.0.1, localhost and the sample 192.168.1.100. This policy does not configure a live endpoint by itself.
- Legacy backup_rules.xml and modern data_extraction_rules.xml exclude root/file/database/sharedpref/external and device-protected counterparts. They do not delete manually saved PNG/plaintext copies.

## Resources and supporting files

| Folder | Meaning and actual use |
| --- | --- |
| res/layout | XML screen/control trees inflated by setContentView or LayoutInflater. activity_hide includes five hide_step layouts. vault_row is the ListView image/text row; sheet_export is the shared bottom sheet. Splash and replay are built in Java, not separate layout files. |
| res/drawable | Vector line icons and layered glow-card shape backgrounds. A vector is shape/path instructions rather than a bitmap; the theme supplies colours. |
| res/anim | fade_in=240ms; slide_up=280ms; shake=60ms/repeat5; pulse=900ms/reverse/infinite. Runtime ValueAnimator/custom views supply additional motion; a resource being present does not prove it is used on a screen. |
| res/values | Named colours, dimensions, strings, styles, theme and accessibility IDs. dp is density-independent size; sp also follows text scaling. touch_min=48dp, button_height=56dp. |
| res/values-night | Night-qualified colour/theme overrides. The initial process forces night; the lab can temporarily choose light. DayNight is the theme family that supports both palettes. |
| res/mipmap-* | Density-specific launcher assets plus API26 adaptive-icon XML layers; these are not cover images. |
| res/xml | Provider paths, backup exclusions and main network policy. Debug source set overrides network policy only in debug variants. |
| assets | how_it_works.html bundles an offline lesson, styling, inline JavaScript bit toggle and reduced-motion CSS. Assets retain their file paths rather than R.layout IDs. |
| main/keepRules | R8 shrinker rules template; current file contains comments, no enabled custom keep rules. |
| test / androidTest | JVM/device test source inputs; JUnit annotations identify callable test methods; PngFixtures synthesizes PNG data. They are distinct from the user-visible Integrity Lab. |
| server | Optional PHP API and MySQL schema/config template/setup docs, independent of the Android APK. |

app/build.gradle.kts sets namespace/applicationId com.example.kineticvault, compile/target SDK37, minimum26, Java11 compatibility, version1/1.0, AndroidJUnitRunner and release optimization. Root build.gradle.kts declares the application plugin alias without applying it at root; settings.gradle.kts includes :app and repositories; gradle/libs.versions.toml pins existing dependencies. The wrapper selects Gradle9.6 and daemon configuration Java25. Java11 source compatibility and the Gradle daemon's Java25 runtime are different settings.

## Source-reading conflicts to explain accurately

UNCLEAR as a current-state statement in docs/IMPLEMENTATION.md: “TestHarnessActivity is now the temporary exported launcher.” Actual manifest declares SplashActivity MAIN/LAUNCHER and TestHarnessActivity exported=false. Treat the former as historical handoff text. Likewise the handoff's “No CAMERA, storage or INTERNET permission is declared” is historical: current manifest declares INTERNET. The version-1 wire-format specification remains useful and agrees with the core implementations described in 02_ARCHITECTURE.md.

The image instrumentation method named fileProviderOnlyExposesSharedSubdirectory tests a denied private.bin path; current file_paths.xml additionally allows dedicated capture/reveal/vaults subtrees. Do not expand that method name into a claim that shared/ is the only current allowed subtree.

## Class atlas

For each Java source below, the owner paragraph identifies who creates/calls it; fields list the important state; operation notes describe its non-trivial methods. The following declaration table records exact parameter/return types and direct calls from each method body. void means no returned value. Constructors create objects and have no declared return type. Calls includes constructors/helpers, not a claim that every branch executes. Android onCreate/onStart/onResume/onPause/onStop/onDestroy are lifecycle callbacks, invoked by Android; overridden drawing/input/database callbacks are similarly framework-driven. Other callbacks are invoked by their registered owner. Local callers are recorded by matching actual references inside the containing source, including method references (::).

02_ARCHITECTURE.md defines interfaces, generics, inner/anonymous classes, resource ownership, exceptions, threads, crypto and drawing constructs with small real examples. The atlas uses their exact source names to remain searchable.

### app/src/main/java/com/example/kineticvault/VaultApplication.java

**Purpose:** Initializes the process, loads preferences, forces the initial dark theme and starts or stops foreground sync.

**Created/called by:** Android creates it from the manifest.

**Key fields / nested records:** ActivityLifecycleCallbacks.visible counts started screens.

**Operations and failure handling:** onCreate -> VaultPreferences.load, registerActivityLifecycleCallbacks, AppCompatDelegate.setDefaultNightMode; callbacks start/stop SyncManager and install/pause UiPolish.

### app/src/main/java/com/example/kineticvault/core/Constants.java

**Purpose:** Keeps format sizes, allocation limits, permanent tokens and motion thresholds in one place.

**Created/called by:** Core, sensors, gesture and image classes call its constants/static helpers.

**Key fields / nested records:** All fields are public static constants.

**Operations and failure handling:** tiltEnterDegrees(sensitivity) and tiltExitDegrees(sensitivity) return relaxed/normal/strict angles; unknown values use normal.

### app/src/main/java/com/example/kineticvault/core/CryptoCore.java

**Purpose:** Derives an AES key and authenticates or encrypts the compressed payload.

**Created/called by:** VaultEngine calls encrypt/decrypt.

**Key fields / nested records:** Sealed holds header and ciphertext; key material is method-local.

**Operations and failure handling:** encrypt -> SecureRandom, VaultHeader.create, transform; decrypt validates exact length then transform; transform -> derive, Cipher.init/updateAAD/doFinal; derive -> GestureCredential.canonical and SecretKeyFactory; destroy tolerates unsupported key erasure.

### app/src/main/java/com/example/kineticvault/core/GestureCredential.java

**Purpose:** Encodes tokens and passphrase into the exact password characters and estimates token entropy.

**Created/called by:** CryptoCore derives with canonical; StrengthMeter calls entropyBits.

**Key fields / nested records:** No persistent state.

**Operations and failure handling:** canonical validates and writes prefix/count/token digits/hex passphrase; validate rejects invalid count/token/passphrase; writePassphrase encodes each UTF-16 code unit with four hex digits; entropyBits returns tokenCount*log2(6), rejects counts outside 0..8.

### app/src/main/java/com/example/kineticvault/core/GzipCodec.java

**Purpose:** Wraps every packed payload in bounded GZIP, with optional compression.

**Created/called by:** VaultEngine and HideStore call it.

**Key fields / nested records:** WipingBuffer owns erasable backing bytes; ConfiguredGzip sets Deflater level.

**Operations and failure handling:** compress checks packed size and returns GZIP bytes; decompress reads in 8192-byte chunks and rejects expansion beyond MAX_PACKED_BYTES; WipingBuffer.write wipes replaced buffers and close wipes current buffer.

### app/src/main/java/com/example/kineticvault/core/PayloadCodec.java

**Purpose:** Turns text/file metadata and exact content into a portable byte sequence and back.

**Created/called by:** SecretInput constructs Payload; VaultEngine and HideStore pack; VaultEngine unpacks.

**Key fields / nested records:** TEXT=1, FILE=2; nested Payload owns type, filename, data.

**Operations and failure handling:** Payload validates ownership and close wipes data; pack writes type/name length/name/content length/content; unpack validates exact sizes and strict UTF-8 filename; validate checks size; validateName rejects traversal/control characters/unknown types; decodeName reports malformed UTF-8.

### app/src/main/java/com/example/kineticvault/core/StegoCore.java

**Purpose:** Embeds and extracts encrypted bits in opaque RGB pixel arrays.

**Created/called by:** VaultEngine, scanner, RevealWork and HideStore call it.

**Key fields / nested records:** No retained state; Ghost order is temporary int[].

**Operations and failure handling:** capacityBytes reserves 102 header pixels; readHeader validates pixels/header/capacity; embed clones cover, writes header then ciphertext; extract rebuilds order and reads bytes; pixelOrder creates salt-seeded Fisher-Yates order; validatePixels rejects alpha/dimension errors; pixelIndex maps bit to pixel; writeBytes/readBytes manipulate channel LSBs and report bit progress.

### app/src/main/java/com/example/kineticvault/core/VaultEngine.java

**Purpose:** Combines packing, GZIP, encryption and embedding into the frozen synchronous engine.

**Created/called by:** HideForge, RevealWork, HarnessRunner and GhostComparisonWork call it.

**Key fields / nested records:** Only local intermediate plaintext arrays.

**Operations and failure handling:** hide -> PayloadCodec.pack -> GzipCodec.compress -> capacity check -> CryptoCore.encrypt -> StegoCore.embed; reveal -> readHeader/extract -> CryptoCore.decrypt -> GzipCodec.decompress -> PayloadCodec.unpack; wipe clears temporary arrays in finally.

### app/src/main/java/com/example/kineticvault/core/VaultHeader.java

**Purpose:** Owns an immutable defensive copy of the public 38-byte version-1 header.

**Created/called by:** CryptoCore creates it; StegoCore parses it.

**Key fields / nested records:** encoded stores cloned bytes.

**Operations and failure handling:** create writes big-endian fields then parse; parse rejects magic/version/flags/length errors; bytes/salt/iv return copies; flag queries and ciphertextLength decode public metadata.

### app/src/main/java/com/example/kineticvault/core/WorkProgress.java

**Purpose:** Defines the engine progress callback contract.

**Created/called by:** Core reports to UI-worker callbacks.

**Key fields / nested records:** Stage enum identifies PACK, COMPRESS, KEY, ENCRYPT, EMBED, EXTRACT, DECRYPT, UNPACK.

**Operations and failure handling:** onProgress receives stage/completed/total on worker; report calls it if non-null, with total=0 meaning indeterminate.

### app/src/main/java/com/example/kineticvault/data/DisguiseCode.java

**Purpose:** Hashes and compares the calculator code independently of vault encryption.

**Created/called by:** VaultPreferences calls create/matches/valid.

**Key fields / nested records:** No stored mutable credential fields.

**Operations and failure handling:** valid accepts 6..12 ASCII digits; create generates 16-byte salt and stores Base64 salt:hash; matches parses lengths and compares with MessageDigest.isEqual; derive uses PBKDF2WithHmacSHA256/120000/256 and clears PBEKeySpec; errors return false or propagate during creation.

### app/src/main/java/com/example/kineticvault/data/HistoryCsv.java

**Purpose:** Exports only the local event ledger as CSV.

**Created/called by:** VaultActivity export callback calls export.

**Key fields / nested records:** No retained state.

**Operations and failure handling:** export requires worker, queries events ordered by ts,id, writes UTF-8 CRLF CSV through ContentResolver; cell quotes/escapes and prefixes formula-like cells; output/query errors propagate.

### app/src/main/java/com/example/kineticvault/data/SyncClient.java

**Purpose:** Sends bounded JSON POST requests to the configured ledger.

**Created/called by:** SyncManager calls post.

**Key fields / nested records:** No retained connection field.

**Operations and failure handling:** post validates HTTP(S), host, URL/key shape and network policy; sets 5-second timeouts, no redirects, API header; response reads at most 65536 bytes and requires ok=true; pause/non-200/malformed response fails; finally disconnects.

### app/src/main/java/com/example/kineticvault/data/SyncManager.java

**Purpose:** Coordinates durable metadata uploads while an app screen is visible.

**Created/called by:** VaultApplication manages singleton lifetime; Home/Settings/repository request work.

**Key fields / nested records:** worker/main, listeners, active/airplane, busy/rerun, pending/stats/status, poll, receiver.

**Operations and failure handling:** get constructs singleton; start registers airplane receiver and reads airplane state off-thread; stop unregisters/removes poll; request serializes sync; configurationChanged resets retry state; testConnection calls health with entered URL/key; deviceId persists UUID; destination hashes URL/key and resets queue on change; sync batches uploads/stats/backoff; upload acknowledges each item; send adds device_id; finish returns status to main; publish notifies listeners; addListener sends current status, removeListener detaches.

### app/src/main/java/com/example/kineticvault/data/SyncStore.java

**Purpose:** Reads the local metadata outbox and acknowledges uploads.

**Created/called by:** SyncManager creates it in worker try-with-resources.

**Key fields / nested records:** helper owns VaultDatabaseHelper.

**Operations and failure handling:** vaults and events return at most 50 queued JSON records; vault maps allowlisted columns only; acknowledge marks success/failed and matches vault revision; pending counts queue; airplane inserts global event; resetDestination requeues all; close closes DB helper.

### app/src/main/java/com/example/kineticvault/data/VaultDatabaseHelper.java

**Purpose:** Creates and upgrades the private SQLite schema and queue triggers.

**Created/called by:** VaultRepository, SyncStore and HistoryCsv construct it.

**Key fields / nested records:** VERSION=2, database kinetic_vault.db.

**Operations and failure handling:** onConfigure enables foreign keys; onCreate creates vaults/events/indexes and syncSchema; onUpgrade adds missing metadata in place then onCreate; syncSchema adds revisions/outbox/triggers; addColumn reads PRAGMA before ALTER TABLE.

### app/src/main/java/com/example/kineticvault/data/VaultEntry.java

**Purpose:** Carries local encrypted-image metadata between database and screens.

**Created/called by:** VaultLogger creates records; VaultEntry.read maps query rows.

**Key fields / nested records:** IDs/title/image URI/hash, payload/capacity sizes, dates, flags, thumbnail, nullable coordinates.

**Operations and failure handling:** read maps Cursor fields; string/number/coordinate read typed values, keeping SQL NULL as null for coordinates.

### app/src/main/java/com/example/kineticvault/data/VaultLogger.java

**Purpose:** Keeps a private byte-for-byte encrypted-image copy and records metadata events.

**Created/called by:** HideForge, RevealLedger and exporters call its worker hooks.

**Key fields / nested records:** LOCK serializes image capture and deduplication.

**Operations and failure handling:** capture locks captureLocked; captureLocked copies to UUID PNG, hashes, reuses hash match or inserts entry; entry creates neutral title/FileProvider URI/96px thumbnail; copy streams and syncs output; logHide/logReveal/logExport/logAutoLock delegate to record; record skips null entries and opens repository; failed captures delete unused copy.

### app/src/main/java/com/example/kineticvault/data/VaultPreferences.java

**Purpose:** Loads/saves settings off-thread and exposes an immutable in-memory snapshot.

**Created/called by:** Application, Splash, Settings, sensor screens and Calculator use it.

**Key fields / nested records:** WORKER, MAIN, volatile current; Values holds sensitivity/pad/protection/autoLock/url/apiKey/disguise/nerd/codeHash.

**Operations and failure handling:** load reads private preferences then main callback; save optionally hashes owned code, commits, replaces current only on success and wipes code; persist writes values; unlockDisguise compares on worker and wipes supplied code; current is memory-only; preferences resolves named store.

### app/src/main/java/com/example/kineticvault/data/VaultRepository.java

**Purpose:** Provides synchronous SQLite creation, searches, updates and event transactions.

**Created/called by:** Logger, VaultActivity, expiry policy and exports create it.

**Key fields / nested records:** helper and application context.

**Operations and failure handling:** requireWorker rejects main thread; insert returns row ID; search filters undeleted literal title LIKE/date range and returns entries; findByHash includes deleted records for identity; rename validates trimmed 1..80 chars; setExpiry updates nullable time; softDelete marks deleted; record transactionally inserts identity, optional size/type and event; update requires one live row; dirty marks queue pending; values maps fields; escape protects literal LIKE; close worker-only.

### app/src/main/java/com/example/kineticvault/diagnostics/HarnessReport.java

**Purpose:** Owns public-demo timings, image statistics and an optional preview.

**Created/called by:** HarnessRunner constructs; TestHarnessActivity consumes.

**Key fields / nested records:** entries list, preview, generationMs, totalMs; Entry stores mode/type/timings/hash/stats.

**Operations and failure handling:** close recycles an untransferred preview; Entry is a plain result record.

### app/src/main/java/com/example/kineticvault/diagnostics/HarnessRunner.java

**Purpose:** Runs the four public-data PNG round-trip demonstrations when invoked by the user.

**Created/called by:** TestHarnessActivity and ImageLayerTest call run.

**Key fields / nested records:** WIDTH=640, HEIGHT=360.

**Operations and failure handling:** run generates cover and loops four cases; generate returns exact pixels and preview; runCase sets text/file and Standard/Ghost plus public fixture credentials; roundTrip calls hide/save/reload/unlock/statistics/hash; save/reload convert PNG/pixels; unlock compares type/name/bytes; payload makes UTF-8 text or 4096-byte binary fixture; temporaryPng creates unique file; stage/checkCancelled support interruption; finally deletes PNG and wipes fixture credentials.

### app/src/main/java/com/example/kineticvault/diagnostics/TestHarnessActivity.java

**Purpose:** Displays the public integrity demo and temporary light-theme preview.

**Created/called by:** HomeActivity opens it; Android invokes lifecycle.

**Key fields / nested records:** worker/main/task, status/result views, current stage/index, busy.

**Operations and failure handling:** onCreate binds layout/insets/listeners; startTest disables controls and submits executeTest; executeTest invokes HarnessRunner with main-posted progress and maps error types; showStage/showReport update labels and transfer preview; modeText formats entries; fail posts failure stage; setBusy gates controls; onDestroy cancels task and worker.

### app/src/main/java/com/example/kineticvault/gesture/GesturePadView.java

**Purpose:** Draws six accessible controls that emit the same named tokens as motion.

**Created/called by:** Hide/Reveal XML creates it; gesture controllers set listener.

**Key fields / nested records:** TOKENS/LABELS/ACTIONS, Paint/cells/labels, dimensions, selected/pressed/pointer/lastMove.

**Operations and failure handling:** onMeasure/onSizeChanged size 2x3 cells; onDraw/drawCell render states; onTouchEvent tracks one press and cancels moved/multi-touch presses; performClick enforces 300ms and listener acceptance before haptic; onKeyDown moves focus; hit refuses cells below 48dp; PadAccessibility adds named actions and activates selected token; detach clears timing/press.

### app/src/main/java/com/example/kineticvault/gesture/GestureRecognizer.java

**Purpose:** Converts fused motion samples into six repeatable tokens.

**Created/called by:** SensorFusionManager constructs it with listener and serialized callback executor.

**Key fields / nested records:** thresholds, generation, sample/move/flat/snap times, latch/rejection flags.

**Operations and failure handling:** setSensitivity updates thresholds/reset; update rejects stale/nonfinite samples, computes stillness and queues deliver; recognize applies neutral rearm, SNAP then tilt then FLAT priority; detectFlat requires level still hold; detectSnap requires bounded downward pulse/recovery; deliver ignores stale generation; finite rejects NaN/infinity; reset invalidates queued events.

### app/src/main/java/com/example/kineticvault/gesture/GestureSession.java

**Purpose:** Owns a transient 4..8-token recording and enrollment comparison.

**Created/called by:** GestureStep and RevealGesture construct sessions.

**Key fields / nested records:** 8-byte tokens, listener, count/times, finished/closed.

**Operations and failure handling:** addToken validates token/time/gap and reports progress; onMotion finishes after continuous stillness; done rejects too-short without closing; matches requires two distinct finished sessions and compares all slots; takeTokens transfers sized copy and closes; reset/clear/close wipe and reset state.

### app/src/main/java/com/example/kineticvault/gesture/StrengthMeter.java

**Purpose:** Labels the uniform-token entropy upper bound and passphrase advice.

**Created/called by:** GestureStep uses label; bits delegates to GestureCredential.

**Key fields / nested records:** No retained state.

**Operations and failure handling:** bits returns entropyBits; label chooses incomplete, passphrase-present or passphrase-advice string without claiming passphrase entropy.

### app/src/main/java/com/example/kineticvault/image/CoverGenerator.java

**Purpose:** Creates an opaque procedural gradient/plasma/noise cover offline.

**Created/called by:** HideStore, harness and comparison worker call generate.

**Key fields / nested records:** No retained state.

**Operations and failure handling:** generate checks worker/memory then fills rows from seeded Random and color; color combines sine wave/halo/noise into ARGB; clamp limits 0..255; interruption recycles failed bitmap.

### app/src/main/java/com/example/kineticvault/image/ImageDecoder.java

**Purpose:** Separates resizable covers from unchanged exact image decoding.

**Created/called by:** ImageUtils delegates to package-private methods.

**Key fields / nested records:** Only local BitmapFactory.Options and bitmap ownership.

**Operations and failure handling:** cover bounds-checks, samples, fits and flattens; exact checks budget and exact size then opacity without transforms; bounds invokes PngIntegrity then bounds decode; options requests mutable unscaled ARGB_8888/sRGB; decode enforces result config; sampleSize grows powers of two; renderOpaque composites onto dark background before embedding; failures recycle allocated bitmaps.

### app/src/main/java/com/example/kineticvault/image/ImageFiles.java

**Purpose:** Saves PNG safely and streams file fingerprints.

**Created/called by:** ImageUtils delegates.

**Key fields / nested records:** No retained state.

**Operations and failure handling:** savePng validates worker/opacity, writes sibling temporary then atomic replace with fallback; write compresses explicitly PNG and syncs FD; sha256 streams 8192-byte blocks into MessageDigest and returns 64 hex characters; failures propagate IOException and temporary files are removed.

### app/src/main/java/com/example/kineticvault/image/ImageMemory.java

**Purpose:** Computes conservative image allocation limits and cover dimensions.

**Created/called by:** ImageDecoder/ImageUtils/CoverGenerator call helpers.

**Key fields / nested records:** MIB and 16MiB WORK_RESERVE.

**Operations and failure handling:** availableBudget leaves 24MiB headroom and caps at half heap; requiredBytes uses 28/32 bytes per pixel; maximumPixels clips to 9M and rejects tiny budget; pixelCount uses long arithmetic; check compares dimensions with budget; fitCover keeps aspect ratio, <=3000 long side and <=budget without upscale.

### app/src/main/java/com/example/kineticvault/image/ImageStats.java

**Purpose:** Computes exact RGB differences and peak signal-to-noise ratio.

**Created/called by:** ImageUtils.statistics delegates; diff views/results consume it.

**Key fields / nested records:** pixelsChanged, changedPercent, maxChannelChange, psnrDb.

**Operations and failure handling:** compare validates equal nonempty arrays, counts changed pixels once and accumulates RGB squared error in long; constructor derives percentage and PSNR, infinity when identical.

### app/src/main/java/com/example/kineticvault/image/ImageUtils.java

**Purpose:** Offers the shared public image/file/pixel facade.

**Created/called by:** Hide, Reveal, Scanner, visual/demo workers and logger call it.

**Key fields / nested records:** Source interface reopens original bytes.

**Operations and failure handling:** source returns file/resolver stream callbacks; decodeCover/decodeExact delegate to ImageDecoder; flattenOpaque/thumbnail render only prepared/display images; pixels/fromPixels enforce shape/opaque ARGB; savePng/sha256 delegate to ImageFiles; statistics/psnr delegate to ImageStats; checkMemory delegates; requireSoftware/requireOpaque validate bitmaps; requireWorker rejects UI-thread image work.

### app/src/main/java/com/example/kineticvault/image/PngIntegrity.java

**Purpose:** Rejects broken PNG framing and checksums before BitmapFactory accepts partial pixels.

**Created/called by:** ImageDecoder.bounds calls read.

**Key fields / nested records:** PNG SIGNATURE and IHDR/IDAT/IEND integer tags.

**Operations and failure handling:** read opens original stream, returns for non-PNG signature; chunks enforces initial IHDR, IDAT and terminating IEND/no trailing bytes; checksum streams CRC32 over type/data and compares stored unsigned CRC; interruption/truncation/damage fails IOException.

### app/src/main/java/com/example/kineticvault/sensor/SensorFusionManager.java

**Purpose:** Combines accelerometer and gyro orientation and feeds the recognizer.

**Created/called by:** GestureStep/RevealGesture construct and start it only during recording.

**Key fields / nested records:** SensorManager/main/recognizer, gravity/acceleration/gyro arrays, pitch/roll, mode/running/timestamps, telemetry.

**Operations and failure handling:** start registers accelerometer and optional gyro on main; stop unregisters and wipes motion state; resetGesture clears recognizer; onSensorChanged rejects invalid samples and dispatches; accelerometer smooths gravity/angles and emits; useAccelerometer estimates fallback speed; gyroscope transforms device rates and blends angles; blend wraps seam-safe correction; emit removes gravity, sends telemetry and recognizer.update; helpers magnitude/seconds/degrees/wrap convert units; requireMainThread enforces lifecycle ownership.

### app/src/main/java/com/example/kineticvault/ui/AboutActivity.java

**Purpose:** Shows the static app and workstream description.

**Created/called by:** Home overflow opens it; extends InfoActivity.

**Key fields / nested records:** No own state.

**Operations and failure handling:** onCreate calls showPage(activity_about).

### app/src/main/java/com/example/kineticvault/ui/CalculatorActivity.java

**Purpose:** Offers arithmetic and optional launcher-disguise code entry.

**Created/called by:** SplashActivity opens it when enabled.

**Key fields / nested records:** input, display, accumulator, operation, fresh/busy/generation.

**Operations and failure handling:** onCreate installs secure layout/insets and bind; bind attaches tagged keys recursively; press routes digits/operators/clear/backspace/equals with 16-character cap; equalsPressed tries saved 6..12-digit code only for stand-alone input; operator/calculate/apply use DECIMAL64 immediate arithmetic; number/showNumber/render manage display; reset/wipeInput clear input; fail handles invalid/divide-by-zero; onPause invalidates pending result and resets.

### app/src/main/java/com/example/kineticvault/ui/DiffMapData.java

**Purpose:** Owns exact source arrays and worker-built difference/X-ray previews.

**Created/called by:** HideForge and GhostComparisonWork create; ResultStep/comparison views consume.

**Key fields / nested records:** width/height, diff/xray, stats, before/after arrays.

**Operations and failure handling:** build OR-bins changed channels into <=512px preview and calculates stats; buildXRay samples each RGB LSB and amplifies to 255; original/encoded return exact pixel values; close recycles previews and zeros arrays.

### app/src/main/java/com/example/kineticvault/ui/DiffMapView.java

**Purpose:** Draws animated difference maps and a touch pixel inspector.

**Created/called by:** Result/comparison XML creates; hosts bind data/loupe.

**Key fields / nested records:** paint/area/data/sweep/reveal/pixelX/pixelY/loupe.

**Operations and failure handling:** setData stops old sweep; startSweep animates 0..1 in 1400ms; stopSweep restores full map; bindLoupe installs touch and center-click; touch maps/clamps coordinates; inspect/rgb format exact pixel/channel LSB values; onDraw clips sweep and calls drawLoupe; drawLoupe magnifies preview bins plus exact position crosshair; performClick preserves touch vs accessibility behavior; detach/hidden stop animation.

### app/src/main/java/com/example/kineticvault/ui/DiffStats.java

**Purpose:** Formats exact image statistics for a TextView.

**Created/called by:** ResultStep and GhostComparisonActivity call show.

**Key fields / nested records:** No retained state.

**Operations and failure handling:** show selects infinity or formatted dB and displays changed count/percent/max delta/PSNR.

### app/src/main/java/com/example/kineticvault/ui/ExportSheet.java

**Purpose:** Creates a reusable Material bottom sheet with supplied export actions.

**Created/called by:** ResultStep, RevealActivity and VaultActivity call show.

**Key fields / nested records:** No static mutable state; returned dialog owned by screen.

**Operations and failure handling:** show inflates sheet_export, adds minimum-height buttons and callbacks, preserves secure-window flag and safe insets, runs action after dismiss if host alive; dp converts density-independent size.

### app/src/main/java/com/example/kineticvault/ui/GhostComparisonActivity.java

**Purpose:** Shows Standard/Ghost public-demo difference maps side by side.

**Created/called by:** ResultStep opens it.

**Key fields / nested records:** worker/main, pair, standard/ghost views, closed/busy.

**Operations and failure handling:** onCreate binds layout/insets/loupes/actions and generate; generate queues GhostComparisonWork.create; show installs results/stats and disposes old pair; setBusy gates controls; dispose closes pair on worker even after shutdown; onPause stops sweeps; onDestroy detaches data and shuts down.

### app/src/main/java/com/example/kineticvault/ui/GhostComparisonWork.java

**Purpose:** Builds two placement demonstrations using public content and ephemeral credentials.

**Created/called by:** GhostComparisonActivity calls create.

**Key fields / nested records:** Pair owns standard/ghost DiffMapData.

**Operations and failure handling:** create generates 320x320 cover, 2048-byte repeated public text, random 8-token/32-char credentials; encode calls VaultEngine.hide with compression off and chosen mode then DiffMapData.build; guard checks interruption; finally wipes buffers/credentials and recycles cover; Pair.close disposes both maps.

### app/src/main/java/com/example/kineticvault/ui/HomeActivity.java

**Purpose:** Provides main feature navigation and foreground Cloud Ledger status.

**Created/called by:** Splash or Calculator opens it.

**Key fields / nested records:** pulse animator and ledger callback.

**Operations and failure handling:** onCreate binds explicit intents/tiles/ledger retry/insets; overflow builds Settings/Learn/About PopupMenu; showLedger formats offline/pending/synced/cached counts/airplane banner; onStart/onStop attach/detach sync listener; onResume/onPause start/cancel orb pulse.

### app/src/main/java/com/example/kineticvault/ui/InfoActivity.java

**Purpose:** Shares safe insets and Back controls for small information screens.

**Created/called by:** About/Learn/Settings/Scanner inherit it.

**Key fields / nested records:** No retained fields.

**Operations and failure handling:** showPage(layout) enables edge-to-edge, inflates layout, pads system/keyboard cutouts and binds finish.

### app/src/main/java/com/example/kineticvault/ui/KineticOrbView.java

**Purpose:** Draws angle-following orb, fading trail and accepted-token dial.

**Created/called by:** Home/Hide XML and GestureReplay create it.

**Key fields / nested records:** Paint/dial, 24-element trails, target/current coordinates, token count/animation flags.

**Operations and failure handling:** setAngles clamps tilt; setTokenCount clamps 0..8 and optionally haptics; setAnimating schedules frames; reset wipes trails/count; onDraw smooths positions and records trail while running; drawDial/drawOrb render arcs/glow; detach stops/reset.

### app/src/main/java/com/example/kineticvault/ui/LearnActivity.java

**Purpose:** Shows bundled interactive education and restricted online reading.

**Created/called by:** Home or overflow opens it; extends InfoActivity.

**Key fields / nested records:** WebView web, remote/failed.

**Operations and failure handling:** onCreate configures WebSettings and WebViewClient callbacks; load switches asset/HTTPS source and local-only JS; showError offers retry/offline route; lifecycle saves only remote choice, pauses/resumes and destroys WebView.

### app/src/main/java/com/example/kineticvault/ui/MatrixRainView.java

**Purpose:** Draws decorative binary rain responsive to real forge progress.

**Created/called by:** Hide XML creates it.

**Key fields / nested records:** Paint/Random, 24 drops, progress/lastFrame/running.

**Operations and failure handling:** setProgress stores ratio or -1; setRunning controls frames; onDraw advances drops with bounded delta-time and draws nine-character tails; detach stops animation.

### app/src/main/java/com/example/kineticvault/ui/NerdOverlay.java

**Purpose:** Shows transient actual sensor samples and token names during recording.

**Created/called by:** GestureStep/RevealGesture construct it and register telemetry.

**Key fields / nested records:** TextView, 8-byte token history, count/render time/sample text.

**Operations and failure handling:** clear wipes history and resets visibility; begin identifies pad; token appends bounded history; onSample throttles display to 120ms and formats units/fusion/fallback; render combines labels without persistence.

### app/src/main/java/com/example/kineticvault/ui/ScannerActivity.java

**Purpose:** Detects structurally valid vault headers in original incoming/selected images.

**Created/called by:** Home or exported SEND/VIEW intent opens it; extends InfoActivity.

**Key fields / nested records:** worker/main, XRayView/plane/status, selected/camera URIs, generation/destroyed/capture/foreground flags.

**Operations and failure handling:** onCreate binds launchers/restores URI-only state; onNewIntent/receive handles SEND/VIEW; capture prepares file on worker; launchCamera waits foreground; scan accepts content URI and invalidates old state; decode uses decodeExact/readHeader or absent-header X-ray; showResult delivers current generation; reveal opens RevealActivity with selected URI/read grant; clearPlane recycles; discardCamera/deleteCapture clean file off-thread; lifecycle retains capture state and drains cleanup.

### app/src/main/java/com/example/kineticvault/ui/SettingsActivity.java

**Purpose:** Edits settings, calculator code and ledger connection details.

**Created/called by:** Home tile/overflow opens it; extends InfoActivity.

**Key fields / nested records:** Spinner, URL/key/code inputs, connectionRequest generation.

**Operations and failure handling:** onCreate secures window, populates sensitivity adapter and loads prefs; populate renders memory snapshot; testConnection uses entered values without saving and ignores stale result; save validates URL/code and calls VaultPreferences.save then SyncManager.configurationChanged; onPause clears code; onStop invalidates connection UI callback.

### app/src/main/java/com/example/kineticvault/ui/SplashActivity.java

**Purpose:** Waits for preferences and routes normal launch to Home or Calculator.

**Created/called by:** Android launcher opens it.

**Key fields / nested records:** main/animator, elapsed/loaded/resumed/routed flags, delay Runnable.

**Operations and failure handling:** onCreate loads settings with blank screen; showLogo animates SplashLogoView for 1000ms, delay 1200ms; route requires loaded/elapsed/resumed and starts selected destination then finish; lifecycle starts/cancels animation and removes delay.

### app/src/main/java/com/example/kineticvault/ui/SplashLogoView.java

**Purpose:** Draws a shield path progressively during normal launch.

**Created/called by:** SplashActivity constructs it.

**Key fields / nested records:** Paint, logo/drawn Path, PathMeasure, progress.

**Operations and failure handling:** setProgress invalidates; onSizeChanged builds shield; onDraw takes path segment and draws central mark with progress opacity.

### app/src/main/java/com/example/kineticvault/ui/TextScrambler.java

**Purpose:** Animates revealed text while owning erasable character arrays.

**Created/called by:** RevealActivity constructs with TextView.

**Key fields / nested records:** main, target, secret/frame, tick/next.

**Operations and failure handling:** show takes char[] ownership; advance reveals over 18 ticks at 35ms, avoids half surrogate, caps intermediate frame at 4096 then displays full text and wipes arrays; clear cancels callbacks and clears target/arrays.

### app/src/main/java/com/example/kineticvault/ui/UiPolish.java

**Purpose:** Applies shared entrance motion, haptics and accessible notices.

**Created/called by:** VaultApplication lifecycle and screen actions call helpers.

**Key fields / nested records:** No retained global state; tagged views prevent repeated install.

**Operations and failure handling:** install finds content, tags once, binds button touch feedback and enters most screens; buttons traverses children without consuming click; enter/notice check animation setting; feedback/failure haptic; pause clears content animation.

### app/src/main/java/com/example/kineticvault/ui/VaultDoorView.java

**Purpose:** Visualizes authenticated opening or denied unlock.

**Created/called by:** Reveal XML creates; RevealActivity drives it.

**Key fields / nested records:** Paint, animator, opening/denial.

**Operations and failure handling:** onDraw renders rings and spokes; open animates 700ms and calls completed only if not cancelled; deny animates 650ms; reset cancels, clears states and restores locked description; detach resets.

### app/src/main/java/com/example/kineticvault/ui/WipeComparisonView.java

**Purpose:** Renders before/after display thumbnails with a movable divider.

**Created/called by:** WipeSliderView constructs it.

**Key fields / nested records:** Paint/RectF, before/after, fraction.

**Operations and failure handling:** setImages updates references; setFraction clamps 0..1; onDraw fits images, clips encoded side and draws divider; null inputs draw nothing.

### app/src/main/java/com/example/kineticvault/ui/WipeSliderView.java

**Purpose:** Combines image comparison with an accessible SeekBar.

**Created/called by:** Result XML creates it.

**Key fields / nested records:** comparison child.

**Operations and failure handling:** constructor builds comparison/SeekBar; anonymous onProgressChanged maps percent to fraction, onStopTrackingTouch haptics; setImages delegates.

### app/src/main/java/com/example/kineticvault/ui/XRayView.java

**Purpose:** Displays a prebuilt amplified RGB least-bit plane.

**Created/called by:** Scanner/Hide result XML creates it.

**Key fields / nested records:** Paint/area/plane.

**Operations and failure handling:** setPlane updates bitmap reference; onDraw fits it over black; null plane draws only background.

### app/src/main/java/com/example/kineticvault/ui/hide/CoverStep.java

**Purpose:** Controls gallery/camera/generated cover input in Hide.

**Created/called by:** HideActivity constructs it.

**Key fields / nested records:** host/work/preview/info, gallery/camera launchers, ready/pendingCamera.

**Operations and failure handling:** openGallery launches OpenDocument; openCamera prepares capture through worker; load renders CoverInfo or recovery; setBusy gates controls; resume launches pending camera only foreground; deleteCapture queues removal; close detaches preview; CameraContract.createIntent adds read/write grants and ClipData.

### app/src/main/java/com/example/kineticvault/ui/hide/GestureReplay.java

**Purpose:** Privately replays only the current matched enrollment.

**Created/called by:** GestureStep constructs and calls show/clear.

**Key fields / nested records:** main/host/dialog/orb/label/moves/index/tick.

**Operations and failure handling:** show clones moves and secures host/dialog, next names/visualizes each move at 900ms and wipes completed clone; clear cancels callbacks, wipes and dismisses then restores configured secure flag.

### app/src/main/java/com/example/kineticvault/ui/hide/GestureStep.java

**Purpose:** Collects and compares two Hide gesture performances.

**Created/called by:** HideActivity constructs it.

**Key fields / nested records:** host/secret/orb/pad/checkbox/status/buttons/sensors/nerd, first/current/enrolled/replay.

**Operations and failure handling:** bind wires controls; begin creates GestureSession and starts motion if requested; startSensors reports fallback; finishAttempt holds first then compares second, resets mismatch or transfers enrolled tokens; takeTokens clears replay and transfers ownership; reset stops sensors/visuals and wipes all sessions/enrollment.

### app/src/main/java/com/example/kineticvault/ui/hide/HideActivity.java

**Purpose:** Coordinates the five-step Hide screen and its lifecycle.

**Created/called by:** Home/Vault empty-state explicit intent opens it.

**Key fields / nested records:** work, cover/secret/gesture/result controllers, rain, step/resumed.

**Operations and failure handling:** onCreate creates controllers and back callback; showStep switches ViewFlipper and activation; forge transfers credentials and calls work.forge; progress displays actual work; message shows notice/Snackbar; back finishes, steps back or confirms cancellation; onPause clears sensitive work and enrollment; onStop clears text; onDestroy closes controllers/worker.

### app/src/main/java/com/example/kineticvault/ui/hide/HideExporter.java

**Purpose:** Copies original encrypted PNG bytes to gallery/document or prepares granted share URI.

**Created/called by:** ResultStep calls static methods.

**Key fields / nested records:** No retained state.

**Operations and failure handling:** saveGallery uses MediaStore pending row on API29+, removes incomplete output; saveDocument streams bytes; recordExport reports ledger success independently; shareUri requires existing file and FileProvider; copy uses ContentResolver stream and never recompresses PNG.

### app/src/main/java/com/example/kineticvault/ui/hide/HideForge.java

**Purpose:** Runs engine, PNG rendering and result asset creation.

**Created/called by:** HideStore.forge invokes run on HideWork executor.

**Key fields / nested records:** Result holds output file, before/after thumbnails, ImageStats, DiffMapData.

**Operations and failure handling:** run guards generation/memory, reads cover pixels, VaultEngine.hide, fromPixels; finish saves PNG, builds stats/thumbnails/maps, logs Hide and returns Result; failed completion deletes file/recycles temporary assets.

### app/src/main/java/com/example/kineticvault/ui/hide/HideStore.java

**Purpose:** Owns cover, prepared payload and rendered output on one worker.

**Created/called by:** HideWork constructs store.

**Key fields / nested records:** context/cover/coverPreview/prepared/compress/ghost/capture/output/rendered/proof.

**Operations and failure handling:** load/generate adopt cover; adopt creates preview; prepareCapture creates FileProvider URI; finishCapture decodes then deletes JPEG; measure reads SecretInput, packs/GZIPs and compares exact overhead, optionally retains fitting payload; forge invokes HideForge and clears payload; proofUri lazily creates ProofSheet; clearPrepared/clearCover/releaseRendered/close wipe/recycle owned assets.

### app/src/main/java/com/example/kineticvault/ui/hide/HideWork.java

**Purpose:** Serializes Hide disk/image/crypto work and marshals callbacks to main.

**Created/called by:** HideActivity constructs it; step controllers submit work.

**Key fields / nested records:** store, worker/main, AtomicInteger secretGeneration, volatile closed.

**Operations and failure handling:** run executes Callable with cleanup and maps exceptions/OOM; measure guards generation and closes snapshot; forge guards progress/results and wipes credentials in cleanup; clearSensitive increments generation and queues clearPrepared; guard throws cancellation; fail posts error; stageLabel maps core enum to resource; close drains credential cleanup before shutdown.

### app/src/main/java/com/example/kineticvault/ui/hide/ProofSheet.java

**Purpose:** Renders a disclosure-limited comparison PNG for presentation.

**Created/called by:** HideStore.proofUri calls create.

**Key fields / nested records:** No retained state.

**Operations and failure handling:** create worker-renders 1080x660 sheet; panel strips RGB lowest bits using 0x00fefefe; density draws 16x16 binned occupancy; savePng writes display artifact, finally recycles/wipes and deletes failed file.

### app/src/main/java/com/example/kineticvault/ui/hide/ResultStep.java

**Purpose:** Presents result visuals and encrypted-PNG/proof export actions.

**Created/called by:** HideActivity constructs and calls show.

**Key fields / nested records:** host/work, sheet, WipeSliderView, CreateDocument launcher.

**Operations and failure handling:** show binds comparison/diff/X-ray/stats; exportOptions selects API-specific actions; saveDocument/savePng/save run worker exporters; warnShare/warnProof confirm disclosure; prepareShare requests URI then ACTION_SEND with read grant/ClipData; logShare records event; busy gates controls; pause dismisses sheet/stops sweep; close detaches images.

### app/src/main/java/com/example/kineticvault/ui/hide/SecretInput.java

**Purpose:** Owns an erasable snapshot of text or a selected file for payload preparation.

**Created/called by:** SecretStep creates snapshots; HideStore.read consumes them.

**Key fields / nested records:** compress/ghost, char[] text, Uri file.

**Operations and failure handling:** read builds Payload and transfers byte ownership; encodeText strict UTF-8 with memory cap; readFile streams bounded chunks into WipingBuffer; filename sanitizes basename and limits UTF-8 length; ensureRoom reserves six payload copies plus 24MiB; close wipes text; nested WipingBuffer wipes replacement/current backing storage.

### app/src/main/java/com/example/kineticvault/ui/hide/SecretStep.java

**Purpose:** Controls text/file, UTF-8 import, compression, Ghost, passphrase and measured capacity.

**Created/called by:** HideActivity constructs it.

**Key fields / nested records:** host/work/main/debounce, inputs/toggles/type/meter, launchers/importer, file/version/active/busy/fits.

**Operations and failure handling:** bind attaches text/option callbacks; activate/deactivate controls measuring; schedule debounces 300ms; snapshot copies erasable inputs; measure/showCapacity display exact bytes; prepare remeasures with keep=true then opens enrollment; validPassphrase rejects checked-empty; takePassphrase copies then clears input; clearSensitiveUi cancels import/clears fields; setBusy gates editing; close closes importer.

### app/src/main/java/com/example/kineticvault/ui/hide/TextImport.java

**Purpose:** Imports bounded strict UTF-8 text without retaining an immutable plaintext String.

**Created/called by:** SecretStep constructs and uses read.

**Key fields / nested records:** host/worker/main, generation/closed.

**Operations and failure handling:** read grows wiped char buffer from4096 up to MAX_PAYLOAD_BYTES/4, ignores cancelled results, calls main consumer and wipes result afterward; cancel invalidates generation; close invalidates and drains worker.

### app/src/main/java/com/example/kineticvault/ui/reveal/RevealActivity.java

**Purpose:** Coordinates original-image selection, credential capture, authenticated reveal and plaintext export.

**Created/called by:** Home/Vault/Scanner explicitly open it.

**Key fields / nested records:** work/gesture/door/scrambler/password/header/selected, busy/registered/revealed/external/attempts, screenOff and launchers.

**Operations and failure handling:** onCreate restores only source URI and binds; load resets then work.load; unlock transfers credentials to work.unlock; unlocked opens door then text/file controls; denied increments attempts and feedback; exportOptions/export prepare plaintext and launch save/view; lock clears UI/work; onStart registers screen-off and resolves protection; onPause clears active recording/busy attempt; onStop optional auto-lock/unregister; destruction clears all secrets/worker.

### app/src/main/java/com/example/kineticvault/ui/reveal/RevealExpiry.java

**Purpose:** Enforces a reversible local device-clock expiry policy for new unlocks.

**Created/called by:** RevealWork.decrypt calls requireAllowed before and after engine.

**Key fields / nested records:** Expired is a marker IOException subclass.

**Operations and failure handling:** requireAllowed reloads record by hash and throws Expired when now>=expiresAt; missing identity is IOException; no image modification or remote revocation.

### app/src/main/java/com/example/kineticvault/ui/reveal/RevealExport.java

**Purpose:** Owns temporary decrypted export and destination copying.

**Created/called by:** RevealWork creates/saves/discards it.

**Key fields / nested records:** process cleanup flag, file/URI/name/MIME/vault metadata.

**Operations and failure handling:** create cleans prior-process orphans then writes UUID cache file and display-name FileProvider URI; constructor derives MIME extension fallback; cleanupOrphans removes old cache files once per process; save copies to destination and wipes chunk; failures delete incomplete temporary file.

### app/src/main/java/com/example/kineticvault/ui/reveal/RevealGesture.java

**Purpose:** Collects one transient gesture for Reveal.

**Created/called by:** RevealActivity constructs it.

**Key fields / nested records:** host/sensors/nerd/pad/checkbox/status/session/tokens.

**Operations and failure handling:** begin clears old input and starts session/pad or sensor fallback; callbacks enable unlock only on completed session; takeTokens transfers ownership; clear stops sensors and wipes session/tokens, disables unlock.

### app/src/main/java/com/example/kineticvault/ui/reveal/RevealLedger.java

**Purpose:** Keeps encrypted-image identity and best-effort event logging separate from secrets.

**Created/called by:** RevealWork constructs it.

**Key fields / nested records:** application context and synchronized current VaultEntry.

**Operations and failure handling:** capture delegates VaultLogger.capture; adopt/current synchronize identity; revealed/exported return logging success without undoing completed operation; autoLock queues event and tolerates failure so wiping is not delayed.

### app/src/main/java/com/example/kineticvault/ui/reveal/RevealWork.java

**Purpose:** Owns Reveal worker, encrypted pixels and erasable decrypted state.

**Created/called by:** RevealActivity constructs and submits work.

**Key fields / nested records:** context/ledger/worker/main, generation/closed, pixels/payload/tokens/passphrase/text/exported.

**Operations and failure handling:** load exact-decodes, reads header and captures identity; unlock queues decrypt; decrypt applies expiry, engine and text decode, adopts result and logs, always wipes credentials; prepareExport clones bytes on worker and wipes clone; save/opened log exports; protection reads preference off-thread; takeText transfers chars; clearSensitive/discardResult wipe; deliver/requireCurrent suppress stale work; discardExport deletes temp; close clears and shuts down.

### app/src/main/java/com/example/kineticvault/ui/vault/VaultActivity.java

**Purpose:** Displays saved vaults with title/date filters and context actions.

**Created/called by:** Home opens it.

**Key fields / nested records:** worker/main/search/repository/adapter/query, from/to/selected/generation/closed/sheet/csv.

**Operations and failure handling:** bindList opens originals and context menu; bindFilters installs date selection and 250ms debounce; load queues search; show/loadFailed design empty/error states; clearFilters/rangeLabel manage dates; open explicit Reveal intent; onCreateContextMenu/onContextItemSelected route rename/expiry/delete; change queues mutations/reloads; failure offers retry; CSV callback exports; lifecycle reloads/saves only filters and closes resources.

### app/src/main/java/com/example/kineticvault/ui/vault/VaultAdapter.java

**Purpose:** Adapts encrypted-image metadata into image-and-text ListView rows.

**Created/called by:** VaultActivity constructs it.

**Key fields / nested records:** entries, worker/main, 4MiB LruCache, closed.

**Operations and failure handling:** replace updates rows; getView inflates/reuses vault_row and binds title/date/size/expiry; date/expiry format state; thumbnail decodes blob on worker, caches and checks UUID view tag before delivery; close stops worker and evicts cache.

### app/src/main/java/com/example/kineticvault/ui/vault/VaultDialogs.java

**Purpose:** Builds rename, expiry and creation-date filter dialogs.

**Created/called by:** VaultActivity invokes static methods.

**Key fields / nested records:** No retained state; callbacks own chosen result.

**Operations and failure handling:** rename validates neutral 1..80-char title; expiry offers clear/change; expiryDate chains date then time; range returns inclusive-start/exclusive-next-day-end; chooseDate/dateDialog normalize dates to midnight and dispatch callback.

### app/src/androidTest/java/com/example/kineticvault/ExampleInstrumentedTest.java

Contains the template Android package-context example. The JUnit runner creates/calls the test class; ImageLayerTest calls PngFixtures. These methods describe assertions/fixtures, not observed results.

### app/src/androidTest/java/com/example/kineticvault/image/ImageLayerTest.java

Describes intended Android image, PNG, FileProvider and worker-thread behavior. The JUnit runner creates/calls the test class; ImageLayerTest calls PngFixtures. These methods describe assertions/fixtures, not observed results.

### app/src/androidTest/java/com/example/kineticvault/image/PngFixtures.java

Creates small-memory synthetic PNG inputs for ImageLayerTest. The JUnit runner creates/calls the test class; ImageLayerTest calls PngFixtures. These methods describe assertions/fixtures, not observed results.

### app/src/test/java/com/example/kineticvault/core/PayloadCodecTest.java

Describes intended payload validation and GZIP-envelope behavior. The JUnit runner creates/calls the test class; ImageLayerTest calls PngFixtures. These methods describe assertions/fixtures, not observed results.

### app/src/test/java/com/example/kineticvault/core/StegoCoreTest.java

Describes intended header, capacity, LSB and Ghost behavior. The JUnit runner creates/calls the test class; ImageLayerTest calls PngFixtures. These methods describe assertions/fixtures, not observed results.

### app/src/test/java/com/example/kineticvault/core/VaultEngineTest.java

Describes intended engine round trips, credential canonicalization and authentication failures. The JUnit runner creates/calls the test class; ImageLayerTest calls PngFixtures. These methods describe assertions/fixtures, not observed results.

### app/src/test/java/com/example/kineticvault/ExampleUnitTest.java

Contains the template addition example. The JUnit runner creates/calls the test class; ImageLayerTest calls PngFixtures. These methods describe assertions/fixtures, not observed results.

### app/src/test/java/com/example/kineticvault/image/ImagePolicyTest.java

Describes intended image sizing, memory and statistics behavior. The JUnit runner creates/calls the test class; ImageLayerTest calls PngFixtures. These methods describe assertions/fixtures, not observed results.

## Exact method declarations and body-call index

This is a declaration index accompanying each class's operation/failure paragraph above. Parameters are shown in the signature as real Java type/name pairs. Return column names the declared type and representative direct return expressions; void has no value, constructors create the containing type. Expressions are quoted from source and can depend on the branch. Calls are syntactic method/constructor or :: callback references inside the body, including nested callback code; they are not a dynamic execution trace or an execution claim. The local-caller column names operations in the same file whose bodies reference that name; cross-file callers and framework-created callbacks are identified in each class's Created/called by paragraph and the feature chains in 01. Overloads share a name: use the full signature to distinguish parameters. Nested callback methods are called by the interface owner (Android, gesture session, worker wrapper or animator), as explained in the class atlas.


### app/src/androidTest/java/com/example/kineticvault/ExampleInstrumentedTest.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public void useAppContext()` | void — no value | External owner / framework / callback in class atlas | `InstrumentationRegistry.getInstrumentation`, `getTargetContext`, `assertEquals`, `appContext.getPackageName` |

### app/src/androidTest/java/com/example/kineticvault/image/ImageLayerTest.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private Context context()` | Context — `InstrumentationRegistry.getInstrumentation().getTargetContext()` | allFourPngRoundTripsPass, sha256MatchesKnownBytes, fileProviderOnlyExposesSharedSubdirectory | `InstrumentationRegistry.getInstrumentation`, `getTargetContext` |
| `@Test public void allFourPngRoundTripsPass() throws Exception` | void — no value | External owner / framework / callback in class atlas | `HarnessRunner.run`, `context`, `assertEquals`, `report.entries.size`, `entry.sha256.length`, `assertTrue` |
| `@Test public void alphaIsFlattenedOnlyForCovers() throws Exception` | void — no value | External owner / framework / callback in class atlas | `Bitmap.createBitmap`, `transparent.eraseColor`, `encode`, `transparent.recycle`, `ByteArrayInputStream`, `ImageUtils.decodeCover`, `assertEquals`, `cover.getConfig`, `ColorSpace.get`, `cover.getColorSpace`, `assertTrue`, `cover.isMutable`, `ImageUtils.pixels`, `cover.recycle`, `assertThrows`, `ImageUtils.decodeExact` |
| `@Test public void oversizedCoverScalesButExactDecodeRejectsIt() throws Exception` | void — no value | External owner / framework / callback in class atlas | `PngFixtures.opaque`, `ByteArrayInputStream`, `ImageUtils.decodeCover`, `assertTrue`, `cover.getWidth`, `cover.getHeight`, `Math.max`, `cover.recycle`, `assertThrows`, `ImageUtils.decodeExact` |
| `@Test public void exactDecodeDoesNotApplyCoverLongSideLimit() throws Exception` | void — no value | External owner / framework / callback in class atlas | `PngFixtures.opaque`, `ImageUtils.decodeExact`, `ByteArrayInputStream`, `assertEquals`, `exact.getWidth`, `exact.getHeight`, `exact.recycle` |
| `@Test public void thumbnailIsBoundedAndSoftware() throws Exception` | void — no value | External owner / framework / callback in class atlas | `CoverGenerator.generate`, `ImageUtils.thumbnail`, `assertEquals`, `thumb.getWidth`, `thumb.getHeight`, `thumb.getConfig`, `cover.recycle`, `thumb.recycle` |
| `@Test public void sha256MatchesKnownBytes() throws Exception` | void — no value | External owner / framework / callback in class atlas | `File.createTempFile`, `context`, `getCacheDir`, `Files.write`, `file.toPath`, `assertEquals`, `ImageUtils.sha256`, `Files.deleteIfExists` |
| `@Test public void fileProviderOnlyExposesSharedSubdirectory() throws Exception` | void — no value | External owner / framework / callback in class atlas | `File`, `context`, `getCacheDir`, `assertTrue`, `folder.isDirectory`, `folder.mkdirs`, `File.createTempFile`, `assertEquals`, `FileProvider.getUriForFile`, `getPackageName`, `getScheme`, `getFilesDir`, `assertThrows`, `Files.deleteIfExists`, `file.toPath` |
| `@Test public void jpegExifOrientationIsNotApplied() throws Exception` | void — no value | External owner / framework / callback in class atlas | `Bitmap.createBitmap`, `bitmap.eraseColor`, `encode`, `bitmap.recycle`, `ByteArrayOutputStream`, `encoded.write`, `encoded.toByteArray`, `ImageUtils.decodeExact`, `ByteArrayInputStream`, `assertEquals`, `decoded.getWidth`, `decoded.getHeight`, `decoded.recycle` |
| `@Test public void malformedInputFailsCleanly()` | void — no value | External owner / framework / callback in class atlas | `assertThrows`, `ImageUtils.decodeExact`, `ByteArrayInputStream` |
| `@Test public void mainThreadImageWorkIsRejected()` | void — no value | External owner / framework / callback in class atlas | `InstrumentationRegistry.getInstrumentation`, `runOnMainSync`, `ImageUtils.requireWorker`, `result.set`, `assertTrue`, `result.get` |
| `private byte[] encode(Bitmap bitmap, Bitmap.CompressFormat format) throws IOException` | byte[] — `output.toByteArray()` | alphaIsFlattenedOnlyForCovers, jpegExifOrientationIsNotApplied | `ByteArrayOutputStream`, `bitmap.compress`, `IOException`, `output.toByteArray` |

### app/src/androidTest/java/com/example/kineticvault/image/PngFixtures.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private PngFixtures()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `static byte[] opaque(int width, int height) throws IOException` | byte[] — `result.toByteArray()` | External owner / framework / callback in class atlas | `ByteArrayOutputStream`, `DataOutputStream`, `output.write`, `header.writeInt`, `header.write`, `chunk`, `headerBytes.toByteArray`, `DeflaterOutputStream`, `deflater.write`, `compressed.toByteArray`, `result.toByteArray` |
| `private static void chunk(DataOutputStream output, String type, byte[] data) throws IOException` | void — no value | opaque | `type.getBytes`, `output.writeInt`, `output.write`, `CRC32`, `crc.update`, `crc.getValue` |

### app/src/main/java/com/example/kineticvault/core/Constants.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private Constants()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static float tiltEnterDegrees(int sensitivity)` | float — `sensitivity == 0 ? 20f : sensitivity == 2 ? 30f : TILT_ENTER_DEGREES` | External owner / framework / callback in class atlas | No further method call in body |
| `public static float tiltExitDegrees(int sensitivity)` | float — `sensitivity == 0 ? 10f : sensitivity == 2 ? 14f : TILT_EXIT_DEGREES` | External owner / framework / callback in class atlas | No further method call in body |

### app/src/main/java/com/example/kineticvault/core/CryptoCore.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private CryptoCore()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `private Sealed(VaultHeader header, byte[] ciphertext)` | New containing-class object | encrypt | No further method call in body |
| `public static Sealed encrypt(byte[] compressed, byte[] tokens, char[] passphrase, boolean ghost, WorkProgress progress) throws GeneralSecurityException, IOException` | Sealed — `new Sealed(header, ciphertext)` | External owner / framework / callback in class atlas | `IOException`, `IllegalArgumentException`, `SecureRandom`, `random.nextBytes`, `VaultHeader.create`, `transform`, `Sealed` |
| `public static byte[] decrypt(VaultHeader header, byte[] ciphertext, byte[] tokens, char[] passphrase, WorkProgress progress) throws GeneralSecurityException, IOException` | byte[] — `transform(Cipher.DECRYPT_MODE, ciphertext, header, tokens, passphrase, progress)` | External owner / framework / callback in class atlas | `header.ciphertextLength`, `IOException`, `transform` |
| `private static byte[] transform(int mode, byte[] input, VaultHeader header, byte[] tokens, char[] passphrase, WorkProgress progress) throws GeneralSecurityException` | byte[] — `cipher.doFinal(input)` | encrypt, decrypt | `WorkProgress.report`, `derive`, `header.salt`, `SecretKeySpec`, `Cipher.getInstance`, `cipher.init`, `GCMParameterSpec`, `header.iv`, `cipher.updateAAD`, `header.bytes`, `cipher.doFinal`, `Arrays.fill`, `destroy` |
| `private static byte[] derive(byte[] tokens, char[] passphrase, byte[] salt) throws GeneralSecurityException` | byte[] — `generated.getEncoded()` | transform | `GestureCredential.canonical`, `PBEKeySpec`, `SecretKeyFactory.getInstance`, `factory.generateSecret`, `generated.getEncoded`, `Arrays.fill`, `specification.clearPassword`, `destroy` |
| `private static void destroy(SecretKey key)` | void — no value | transform, derive | `key.destroy` |

### app/src/main/java/com/example/kineticvault/core/GestureCredential.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private GestureCredential()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static char[] canonical(byte[] tokens, char[] passphrase)` | char[] — `result` | External owner / framework / callback in class atlas | `validate`, `System.arraycopy`, `writePassphrase` |
| `private static void validate(byte[] tokens, char[] passphrase)` | void — no value | canonical | `IllegalArgumentException` |
| `private static void writePassphrase(char[] out, int offset, char[] passphrase)` | void — no value | canonical | `toCharArray` |
| `public static double entropyBits(int tokenCount)` | double — `tokenCount * Math.log(6.0) / Math.log(2.0)` | External owner / framework / callback in class atlas | `IllegalArgumentException`, `Math.log` |

### app/src/main/java/com/example/kineticvault/core/GzipCodec.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private GzipCodec()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `@Override public synchronized void write(byte[] bytes, int offset, int length)` | void — no value | write, compress, decompress | `super.write`, `Arrays.fill` |
| `@Override public synchronized void write(int value)` | void — no value | write, compress, decompress | `super.write`, `Arrays.fill` |
| `@Override public void close()` | void — no value | External owner / framework / callback in class atlas | `Arrays.fill`, `reset` |
| `ConfiguredGzip(ByteArrayOutputStream output, boolean compress) throws IOException` | New containing-class object | compress | `def.setLevel` |
| `public static byte[] compress(byte[] packed, boolean compress) throws IOException` | byte[] — `output.toByteArray()` | External owner / framework / callback in class atlas | `IOException`, `WipingBuffer`, `ConfiguredGzip`, `gzip.write`, `gzip.finish`, `output.toByteArray` |
| `public static byte[] decompress(byte[] compressed) throws IOException` | byte[] — `output.toByteArray()` | External owner / framework / callback in class atlas | `IOException`, `GZIPInputStream`, `ByteArrayInputStream`, `WipingBuffer`, `gzip.read`, `output.size`, `output.write`, `output.toByteArray`, `Arrays.fill` |

### app/src/main/java/com/example/kineticvault/core/PayloadCodec.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private PayloadCodec()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public Payload(byte type, String filename, byte[] data) throws IOException` | New containing-class object | unpack | `validate` |
| `@Override public void close()` | void — no value | External owner / framework / callback in class atlas | `Arrays.fill` |
| `public static byte[] pack(Payload payload) throws IOException` | byte[] — `out.array()` | External owner / framework / callback in class atlas | `IOException`, `validate`, `payload.filename.getBytes`, `ByteBuffer.allocate`, `out.order`, `out.put`, `putShort`, `putInt`, `put`, `out.array` |
| `public static Payload unpack(byte[] packed) throws IOException` | Payload — `new Payload(type, filename, data)` | External owner / framework / callback in class atlas | `IOException`, `ByteBuffer.wrap`, `order`, `input.get`, `input.getShort`, `input.remaining`, `decodeName`, `input.getInt`, `validateName`, `Payload` |
| `private static void validate(byte type, String filename, byte[] data) throws IOException` | void — no value | Payload, pack | `validateName`, `IOException` |
| `private static void validateName(byte type, String filename) throws IOException` | void — no value | unpack, validate | `IOException`, `filename.isEmpty`, `filename.getBytes`, `filename.equals`, `filename.length`, `filename.charAt`, `StandardCharsets.UTF_8.newEncoder`, `encode`, `java.nio.CharBuffer.wrap` |
| `private static String decodeName(byte[] name) throws CharacterCodingException` | String — `StandardCharsets.UTF_8.newDecoder() .onMalformedInput(CodingErrorAction.REPORT) .onUnmappableCharacter(CodingErrorAction.REPORT) .decode(ByteBuffer.wr` | unpack | `StandardCharsets.UTF_8.newDecoder`, `onMalformedInput`, `onUnmappableCharacter`, `decode`, `ByteBuffer.wrap`, `toString` |

### app/src/main/java/com/example/kineticvault/core/StegoCore.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private StegoCore()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static int capacityBytes(int pixelCount)` | int — `0`; `(int) (bits / 8)` | readHeader, embed | No further method call in body |
| `public static VaultHeader readHeader(int[] pixels) throws IOException` | VaultHeader — `header` | extract | `validatePixels`, `readBytes`, `VaultHeader.parse`, `header.ciphertextLength`, `capacityBytes`, `IOException` |
| `public static int[] embed(int[] cover, VaultHeader header, byte[] ciphertext, WorkProgress progress) throws IOException` | int[] — `result` | External owner / framework / callback in class atlas | `validatePixels`, `header.ciphertextLength`, `IOException`, `capacityBytes`, `pixelOrder`, `cover.clone`, `writeBytes`, `header.bytes` |
| `public static byte[] extract(int[] pixels, WorkProgress progress) throws IOException` | byte[] — `readBytes(pixels, header.ciphertextLength(), Constants.HEADER_PIXELS, order, progress)` | External owner / framework / callback in class atlas | `readHeader`, `pixelOrder`, `readBytes`, `header.ciphertextLength` |
| `private static void validatePixels(int[] pixels) throws IOException` | void — no value | readHeader, embed | `IOException` |
| `private static int[] pixelOrder(int count, VaultHeader header)` | int[] — `null`; `order` | embed, extract | `header.isGhost`, `header.salt`, `Random`, `random.nextInt` |
| `private static int pixelIndex(int bitIndex, int startPixel, int[] order)` | int — `order == null ? startPixel + position : order[position]` | writeBytes, readBytes | No further method call in body |
| `private static void writeBytes(int[] pixels, byte[] bytes, int startPixel, int[] order, WorkProgress progress)` | void — no value | embed | `pixelIndex`, `WorkProgress.report` |
| `private static byte[] readBytes(int[] pixels, int count, int startPixel, int[] order, WorkProgress progress)` | byte[] — `bytes` | readHeader, extract | `pixelIndex`, `WorkProgress.report` |

### app/src/main/java/com/example/kineticvault/core/VaultEngine.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private VaultEngine()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static int[] hide(int[] cover, PayloadCodec.Payload payload, byte[] tokens, char[] passphrase, boolean compress, boolean ghost, WorkProgress progress) throws IOException, GeneralSecurityException` | int[] — `StegoCore.embed(cover, sealed.header, sealed.ciphertext, progress)` | External owner / framework / callback in class atlas | `WorkProgress.report`, `PayloadCodec.pack`, `GzipCodec.compress`, `StegoCore.capacityBytes`, `IOException`, `CryptoCore.encrypt`, `StegoCore.embed`, `wipe` |
| `public static PayloadCodec.Payload reveal(int[] pixels, byte[] tokens, char[] passphrase, WorkProgress progress) throws IOException, GeneralSecurityException` | PayloadCodec.Payload — `PayloadCodec.unpack(packed)` | External owner / framework / callback in class atlas | `StegoCore.readHeader`, `StegoCore.extract`, `CryptoCore.decrypt`, `WorkProgress.report`, `GzipCodec.decompress`, `PayloadCodec.unpack`, `wipe` |
| `private static void wipe(byte[] bytes)` | void — no value | hide, reveal | `Arrays.fill` |

### app/src/main/java/com/example/kineticvault/core/VaultHeader.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private VaultHeader(byte[] encoded)` | New containing-class object | parse | `encoded.clone` |
| `public static VaultHeader create(byte[] salt, byte[] iv, int flags, int length) throws IOException` | VaultHeader — `parse(out.array())` | External owner / framework / callback in class atlas | `IOException`, `ByteBuffer.allocate`, `out.order`, `out.put`, `put`, `putInt`, `parse`, `out.array` |
| `public static VaultHeader parse(byte[] bytes) throws IOException` | VaultHeader — `new VaultHeader(bytes)` | create | `IOException`, `ByteBuffer.wrap`, `order`, `getInt`, `VaultHeader` |
| `public byte[] bytes()` | byte[] — `encoded.clone()` | External owner / framework / callback in class atlas | `encoded.clone` |
| `public byte[] salt()` | byte[] — `Arrays.copyOfRange(encoded, 6, 22)` | External owner / framework / callback in class atlas | `Arrays.copyOfRange` |
| `public byte[] iv()` | byte[] — `Arrays.copyOfRange(encoded, 22, 34)` | External owner / framework / callback in class atlas | `Arrays.copyOfRange` |
| `public boolean usesPassphrase()` | boolean — `(encoded[5] & 1) != 0` | External owner / framework / callback in class atlas | No further method call in body |
| `public boolean isGhost()` | boolean — `(encoded[5] & 2) != 0` | External owner / framework / callback in class atlas | No further method call in body |
| `public int ciphertextLength()` | int — `ByteBuffer.wrap(encoded).order(ByteOrder.BIG_ENDIAN).getInt(34)` | External owner / framework / callback in class atlas | `ByteBuffer.wrap`, `order`, `getInt` |

### app/src/main/java/com/example/kineticvault/core/WorkProgress.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `static void report(WorkProgress listener, Stage stage, int done, int total)` | void — no value | External owner / framework / callback in class atlas | `listener.onProgress` |

### app/src/main/java/com/example/kineticvault/data/DisguiseCode.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private DisguiseCode()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static boolean valid(char[] code)` | boolean — `false`; `false`; `true` | External owner / framework / callback in class atlas | No further method call in body |
| `public static String create(char[] code) throws java.security.GeneralSecurityException` | String — `Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + Base64.encodeToString(hash, Base64.NO_WRAP)` | External owner / framework / callback in class atlas | `SecureRandom`, `nextBytes`, `derive`, `Base64.encodeToString`, `Arrays.fill` |
| `public static boolean matches(char[] code, String stored)` | boolean — `false`; `false`; `MessageDigest.isEqual(expected, actual)` | External owner / framework / callback in class atlas | `stored.split`, `Base64.decode`, `derive`, `MessageDigest.isEqual`, `Arrays.fill` |
| `private static byte[] derive(char[] code, byte[] salt) throws java.security.GeneralSecurityException` | byte[] — `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded()` | create, matches | `PBEKeySpec`, `SecretKeyFactory.getInstance`, `generateSecret`, `getEncoded`, `spec.clearPassword` |

### app/src/main/java/com/example/kineticvault/data/HistoryCsv.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private HistoryCsv()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static void export(Context context, Uri destination) throws IOException` | void — no value | External owner / framework / callback in class atlas | `VaultRepository.requireWorker`, `VaultDatabaseHelper`, `helper.getReadableDatabase`, `rawQuery`, `context.getContentResolver`, `openOutputStream`, `IOException`, `OutputStreamWriter`, `out.write`, `context.getString`, `rows.moveToNext`, `cell`, `rows.getString` |
| `private static String cell(String value)` | String — `"\"" + text.replace("\"", "\"\"") + "\""` | export | `text.isEmpty`, `indexOf`, `text.charAt`, `text.replace` |

### app/src/main/java/com/example/kineticvault/data/SyncClient.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private SyncClient()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `static JSONObject post(String address, String key, JSONObject body, BooleanSupplier paused) throws Exception` | JSONObject — `response(in, paused)` | External owner / framework / callback in class atlas | `VaultRepository.requireWorker`, `URL`, `equals`, `url.getProtocol`, `url.getHost`, `isEmpty`, `url.getUserInfo`, `url.getRef`, `key.isEmpty`, `key.chars`, `anyMatch`, `IOException`, `NetworkSecurityPolicy.getInstance`, `isCleartextTrafficPermitted`, `paused.getAsBoolean`, `url.openConnection`, `c.setConnectTimeout`, `c.setReadTimeout`, `c.setInstanceFollowRedirects`, `c.setRequestMethod`, `c.setDoOutput`, `c.setUseCaches`, `c.setRequestProperty`, `body.toString`, `getBytes`, `c.setFixedLengthStreamingMode`, `c.getOutputStream`, `out.write`, `c.getResponseCode`, `c.getInputStream`, `response`, `c.disconnect` |
| `private static JSONObject response(InputStream in, BooleanSupplier paused) throws Exception` | JSONObject — `result` | post | `ByteArrayOutputStream`, `in.read`, `paused.getAsBoolean`, `out.size`, `IOException`, `out.write`, `JSONObject`, `out.toString`, `StandardCharsets.UTF_8.name`, `result.optBoolean` |

### app/src/main/java/com/example/kineticvault/data/SyncManager.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `Status(boolean offline, boolean airplane, int pending, JSONObject stats)` | New containing-class object | onReceive, finish | `stats.optLong` |
| `@Override public void onReceive(Context context, Intent intent)` | void — no value | External owner / framework / callback in class atlas | `Intent.ACTION_AIRPLANE_MODE_CHANGED.equals`, `intent.getAction`, `intent.getBooleanExtra`, `publish`, `Status`, `worker.execute`, `SyncStore`, `store.airplane`, `main.post`, `SyncManager.this::request` |
| `private SyncManager(Context context)` | New containing-class object | get | `context.getApplicationContext` |
| `public static synchronized SyncManager get(Context context)` | SyncManager — `instance` | External owner / framework / callback in class atlas | `SyncManager` |
| `public void addListener(Listener listener)` | void — no value | External owner / framework / callback in class atlas | `listeners.add`, `listener.onSync` |
| `public void removeListener(Listener listener)` | void — no value | External owner / framework / callback in class atlas | `listeners.remove` |
| `public void start()` | void — no value | External owner / framework / callback in class atlas | `ContextCompat.registerReceiver`, `IntentFilter`, `worker.execute`, `Settings.Global.getInt`, `app.getContentResolver`, `main.post`, `this::request` |
| `public void stop()` | void — no value | External owner / framework / callback in class atlas | `main.removeCallbacks`, `app.unregisterReceiver` |
| `public void request()` | void — no value | onReceive, start, configurationChanged | `Looper.myLooper`, `Looper.getMainLooper`, `main.post`, `main.removeCallbacks`, `worker.execute`, `this::request`, `this::sync` |
| `public void configurationChanged()` | void — no value | External owner / framework / callback in class atlas | `worker.execute`, `preferences`, `edit`, `putLong`, `putInt`, `commit`, `request` |
| `public void testConnection(String url, String key, Consumer<Boolean> done)` | void — no value | External owner / framework / callback in class atlas | `worker.execute`, `SyncClient.post`, `JSONObject`, `put`, `deviceId`, `equals`, `result.optString`, `result.optInt`, `main.post`, `done.accept` |
| `private SharedPreferences preferences()` | SharedPreferences — `app.getSharedPreferences("metadata_sync", Context.MODE_PRIVATE)` | configurationChanged, deviceId, destination, sync | `app.getSharedPreferences` |
| `private String deviceId()` | String — `id` | testConnection, send | `preferences`, `p.getString`, `UUID.randomUUID`, `toString`, `p.edit`, `putString`, `commit`, `IllegalStateException` |
| `private void destination(SyncStore store, VaultPreferences.Values config) throws Exception` | void — no value | sync | `MessageDigest.getInstance`, `digest`, `getBytes`, `android.util.Base64.encodeToString`, `preferences`, `signature.equals`, `p.getString`, `store.resetDestination`, `p.edit`, `putString`, `putLong`, `putInt`, `commit`, `IllegalStateException` |
| `private void sync()` | void — no value | request | `SyncStore`, `VaultPreferences.current`, `destination`, `store.pending`, `preferences`, `getLong`, `System.currentTimeMillis`, `config.url.isEmpty`, `config.apiKey.isEmpty`, `Math.min`, `upload`, `store.vaults`, `store.events`, `send`, `JSONObject`, `put`, `edit`, `putInt`, `putLong`, `commit`, `getInt`, `finish` |
| `private void upload(SyncStore store, List<JSONObject> items, VaultPreferences.Values config) throws Exception` | void — no value | sync | `java.io.IOException`, `send`, `store.acknowledge` |
| `private JSONObject send(VaultPreferences.Values config, JSONObject item) throws Exception` | JSONObject — `SyncClient.post(config.url, config.apiKey, item.put("device_id", deviceId()), () -> airplane \|\| !active)` | sync, upload | `SyncClient.post`, `item.put`, `deviceId` |
| `private void finish(boolean offline, long delay)` | void — no value | sync | `Status`, `main.post`, `publish`, `main.removeCallbacks`, `main.postDelayed` |
| `private void publish(Status next)` | void — no value | onReceive, finish | `listener.onSync` |

### app/src/main/java/com/example/kineticvault/data/SyncStore.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `SyncStore(Context context)` | New containing-class object | External owner / framework / callback in class atlas | `VaultDatabaseHelper` |
| `List<JSONObject> vaults() throws Exception` | List<JSONObject> — `result` | External owner / framework / callback in class atlas | `VaultRepository.requireWorker`, `helper.getReadableDatabase`, `query`, `c.moveToNext`, `result.add`, `vault` |
| `private JSONObject vault(Cursor c) throws Exception` | JSONObject — `new JSONObject().put("uuid", c.getString(0)).put("sha256", c.getString(1)) .put("payload_bytes", c.getLong(2)).put("capacity_bytes", c.getLong(3)) .pu` | vaults | `JSONObject`, `put`, `c.getString`, `c.getLong`, `c.getInt`, `c.isNull`, `Math.max` |
| `List<JSONObject> events() throws Exception` | List<JSONObject> — `result` | External owner / framework / callback in class atlas | `VaultRepository.requireWorker`, `helper.getReadableDatabase`, `query`, `c.moveToNext`, `result.add`, `JSONObject`, `put`, `c.getString`, `c.getColumnIndexOrThrow`, `c.isNull`, `c.getLong` |
| `void acknowledge(JSONObject item, boolean success) throws Exception` | void — no value | External owner / framework / callback in class atlas | `VaultRepository.requireWorker`, `ContentValues`, `values.put`, `helper.getWritableDatabase`, `item.has`, `db.update`, `item.getString` |
| `int pending()` | int — `c.getInt(0)` | External owner / framework / callback in class atlas | `VaultRepository.requireWorker`, `helper.getReadableDatabase`, `rawQuery`, `c.moveToFirst`, `c.getInt` |
| `void airplane(boolean enabled)` | void — no value | External owner / framework / callback in class atlas | `VaultRepository.requireWorker`, `ContentValues`, `v.put`, `UUID.randomUUID`, `toString`, `v.putNull`, `System.currentTimeMillis`, `helper.getWritableDatabase`, `insertOrThrow` |
| `void resetDestination()` | void — no value | External owner / framework / callback in class atlas | `VaultRepository.requireWorker`, `ContentValues`, `v.put`, `helper.getWritableDatabase`, `update` |
| `@Override public void close()` | void — no value | External owner / framework / callback in class atlas | `VaultRepository.requireWorker`, `helper.close` |

### app/src/main/java/com/example/kineticvault/data/VaultDatabaseHelper.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public VaultDatabaseHelper(Context context)` | New containing-class object | External owner / framework / callback in class atlas | `context.getApplicationContext` |
| `@Override public void onConfigure(SQLiteDatabase db)` | void — no value | External owner / framework / callback in class atlas | `db.setForeignKeyConstraintsEnabled` |
| `@Override public void onCreate(SQLiteDatabase db)` | void — no value | onUpgrade | `db.execSQL`, `syncSchema` |
| `@Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)` | void — no value | External owner / framework / callback in class atlas | `addColumn`, `onCreate` |
| `private static void syncSchema(SQLiteDatabase db)` | void — no value | onCreate | `addColumn`, `db.execSQL` |
| `private static void addColumn(SQLiteDatabase db, String name, String definition)` | void — no value | onUpgrade, syncSchema | `db.rawQuery`, `columns.moveToNext`, `name.equals`, `columns.getString`, `columns.getColumnIndexOrThrow`, `db.execSQL` |

### app/src/main/java/com/example/kineticvault/data/VaultEntry.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public VaultEntry()` | New containing-class object | read | No further method call in body |
| `static VaultEntry read(Cursor cursor)` | VaultEntry — `entry` | External owner / framework / callback in class atlas | `VaultEntry`, `cursor.getLong`, `cursor.getColumnIndexOrThrow`, `string`, `number`, `cursor.isNull`, `cursor.getBlob`, `coordinate` |
| `private static String string(Cursor cursor, String name)` | String — `cursor.getString(cursor.getColumnIndexOrThrow(name))` | read | `cursor.getString`, `cursor.getColumnIndexOrThrow` |
| `private static long number(Cursor cursor, String name)` | long — `cursor.getLong(cursor.getColumnIndexOrThrow(name))` | read | `cursor.getLong`, `cursor.getColumnIndexOrThrow` |
| `private static Double coordinate(Cursor cursor, String name)` | Double — `cursor.isNull(column) ? null : cursor.getDouble(column)` | read | `cursor.getColumnIndexOrThrow`, `cursor.isNull`, `cursor.getDouble` |

### app/src/main/java/com/example/kineticvault/data/VaultLogger.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private VaultLogger()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static VaultEntry capture(Context context, Uri source, Bitmap image, boolean ghost, boolean usesPassphrase) throws IOException` | VaultEntry — `captureLocked(context, source, image, ghost, usesPassphrase)` | logHide | `VaultRepository.requireWorker`, `captureLocked` |
| `private static VaultEntry captureLocked(Context context, Uri source, Bitmap image, boolean ghost, boolean usesPassphrase) throws IOException` | VaultEntry — `existing`; `entry` | capture | `File`, `context.getFilesDir`, `directory.isDirectory`, `directory.mkdirs`, `IOException`, `UUID.randomUUID`, `toString`, `VaultRepository`, `copy`, `ImageUtils.sha256`, `repository.findByHash`, `entry`, `repository.insert`, `copy.delete` |
| `private static VaultEntry entry(Context context, Bitmap image, String uuid, File file, String hash, boolean ghost, boolean usesPassphrase) throws IOException` | VaultEntry — `entry` | captureLocked | `VaultEntry`, `context.getString`, `FileProvider.getUriForFile`, `context.getPackageName`, `toString`, `StegoCore.capacityBytes`, `image.getWidth`, `image.getHeight`, `System.currentTimeMillis`, `ImageUtils.thumbnail`, `ByteArrayOutputStream`, `thumbnail.compress`, `IOException`, `bytes.toByteArray`, `thumbnail.recycle` |
| `private static void copy(Context context, Uri source, File destination) throws IOException` | void — no value | captureLocked | `context.getContentResolver`, `openInputStream`, `FileOutputStream`, `IOException`, `input.read`, `output.write`, `output.getFD`, `sync` |
| `public static void logHide(Context context, File source, Bitmap image, int type, long bytes, boolean ghost, boolean usesPassphrase) throws IOException` | void — no value | External owner / framework / callback in class atlas | `capture`, `Uri.fromFile`, `record` |
| `public static void logReveal(Context context, VaultEntry entry, boolean success, int type, long bytes)` | void — no value | External owner / framework / callback in class atlas | `record` |
| `public static void logExport(Context context, VaultEntry entry)` | void — no value | External owner / framework / callback in class atlas | `record` |
| `public static void logExport(Context context, File image) throws IOException` | void — no value | External owner / framework / callback in class atlas | `VaultRepository.requireWorker`, `VaultRepository`, `repository.findByHash`, `ImageUtils.sha256`, `repository.record` |
| `public static void logAutoLock(Context context, VaultEntry entry)` | void — no value | External owner / framework / callback in class atlas | `record` |
| `private static void record(Context context, VaultEntry entry, String event, Integer type, Long bytes)` | void — no value | logHide, logReveal, logExport, logAutoLock | `VaultRepository.requireWorker`, `VaultRepository`, `repository.record` |

### app/src/main/java/com/example/kineticvault/data/VaultPreferences.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private VaultPreferences()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static Values current()` | Values — `current` | External owner / framework / callback in class atlas | No further method call in body |
| `public static void load(Context context, Runnable ready)` | void — no value | External owner / framework / callback in class atlas | `context.getApplicationContext`, `WORKER.execute`, `preferences`, `Values`, `p.getInt`, `p.getBoolean`, `p.getString`, `MAIN.post` |
| `public static void save(Context context, Values values, Consumer<Boolean> done)` | void — no value | External owner / framework / callback in class atlas | `save` |
| `public static void save(Context context, Values values, char[] code, Consumer<Boolean> done)` | void — no value | save | `context.getApplicationContext`, `WORKER.execute`, `DisguiseCode.create`, `Values`, `hash.isEmpty`, `DisguiseCode.valid`, `persist`, `Arrays.fill`, `MAIN.post`, `done.accept` |
| `private static boolean persist(Context app, Values v)` | boolean — `preferences(app).edit().putInt("sensitivity", v.sensitivity) .putBoolean("gesture_pad", v.pad).putBoolean("screenshot_protection", v.protection) .putB` | save | `preferences`, `edit`, `putInt`, `putBoolean`, `putString`, `commit` |
| `public static void unlockDisguise(char[] code, Consumer<Boolean> done)` | void — no value | External owner / framework / callback in class atlas | `WORKER.execute`, `DisguiseCode.valid`, `DisguiseCode.matches`, `Arrays.fill`, `MAIN.post`, `done.accept` |
| `private static SharedPreferences preferences(Context context)` | SharedPreferences — `context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE)` | load, persist | `context.getSharedPreferences`, `context.getPackageName` |
| `public Values(int sensitivity, boolean pad, boolean protection, boolean autoLock, String url, String apiKey)` | New containing-class object | load, save | No further method call in body |
| `public Values(int sensitivity, boolean pad, boolean protection, boolean autoLock, String url, String apiKey, boolean disguise, boolean nerd, String codeHash)` | New containing-class object | load, save | `Math.max`, `Math.min` |

### app/src/main/java/com/example/kineticvault/data/VaultRepository.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public VaultRepository(Context context)` | New containing-class object | External owner / framework / callback in class atlas | `context.getApplicationContext`, `VaultDatabaseHelper` |
| `public static void requireWorker()` | void — no value | insert, search, findByHash, rename, setExpiry, softDelete, record, close | `Looper.myLooper`, `Looper.getMainLooper`, `IllegalStateException` |
| `public long insert(VaultEntry entry)` | long — `id` | External owner / framework / callback in class atlas | `requireWorker`, `helper.getWritableDatabase`, `insertOrThrow`, `values`, `SyncManager.get`, `request` |
| `public List<VaultEntry> search(String query, Long fromInclusive, Long toExclusive)` | List<VaultEntry> — `rows` | External owner / framework / callback in class atlas | `requireWorker`, `StringBuilder`, `args.add`, `escape`, `selection.append`, `fromInclusive.toString`, `toExclusive.toString`, `helper.getReadableDatabase`, `query`, `selection.toString`, `args.toArray`, `cursor.moveToNext`, `rows.add`, `VaultEntry.read` |
| `public VaultEntry findByHash(String hash)` | VaultEntry — `cursor.moveToFirst() ? VaultEntry.read(cursor) : null` | External owner / framework / callback in class atlas | `requireWorker`, `helper.getReadableDatabase`, `query`, `cursor.moveToFirst`, `VaultEntry.read` |
| `public void rename(String uuid, String title)` | void — no value | External owner / framework / callback in class atlas | `requireWorker`, `title.trim`, `label.isEmpty`, `label.length`, `IllegalArgumentException`, `dirty`, `values.put`, `update` |
| `public void setExpiry(String uuid, Long expiresAt)` | void — no value | External owner / framework / callback in class atlas | `requireWorker`, `dirty`, `values.putNull`, `values.put`, `update` |
| `public void softDelete(String uuid)` | void — no value | External owner / framework / callback in class atlas | `requireWorker`, `dirty`, `values.put`, `update` |
| `public void record(VaultEntry entry, String event, Integer payloadType, Long payloadBytes)` | void — no value | External owner / framework / callback in class atlas | `requireWorker`, `helper.getWritableDatabase`, `db.beginTransaction`, `db.insertWithOnConflict`, `values`, `dirty`, `data.put`, `db.update`, `ContentValues`, `values.put`, `System.currentTimeMillis`, `db.insertOrThrow`, `db.setTransactionSuccessful`, `db.endTransaction`, `SyncManager.get`, `request` |
| `private void update(String uuid, ContentValues values)` | void — no value | rename, setExpiry, softDelete, record | `helper.getWritableDatabase`, `update`, `IllegalStateException`, `SyncManager.get`, `request` |
| `private static ContentValues dirty()` | ContentValues — `values` | rename, setExpiry, softDelete, record, values | `ContentValues`, `values.put` |
| `private static ContentValues values(VaultEntry entry)` | ContentValues — `values` | insert, record | `dirty`, `values.put` |
| `private static String escape(String value)` | String — `value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")` | search | `value.replace`, `replace` |
| `@Override public void close()` | void — no value | External owner / framework / callback in class atlas | `requireWorker`, `helper.close` |

### app/src/main/java/com/example/kineticvault/diagnostics/HarnessReport.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override public void close()` | void — no value | External owner / framework / callback in class atlas | `preview.recycle` |

### app/src/main/java/com/example/kineticvault/diagnostics/HarnessRunner.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private HarnessRunner()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static HarnessReport run(Context context, Listener listener) throws IOException, GeneralSecurityException` | HarnessReport — `report` | External owner / framework / callback in class atlas | `ImageUtils.requireWorker`, `HarnessReport`, `SystemClock.elapsedRealtime`, `stage`, `generate`, `report.entries.add`, `runCase`, `report.close` |
| `private static int[] generate(HarnessReport report) throws IOException` | int[] — `ImageUtils.pixels(bitmap)` | run | `CoverGenerator.generate`, `ImageUtils.thumbnail`, `ImageUtils.pixels`, `bitmap.recycle` |
| `private static HarnessReport.Entry runCase(Context context, int[] cover, int index, Listener listener) throws IOException, GeneralSecurityException` | HarnessReport.Entry — `entry` | run | `HarnessReport.Entry`, `ImageUtils.checkMemory`, `toCharArray`, `SystemClock.elapsedRealtime`, `payload`, `temporaryPng`, `roundTrip`, `Arrays.fill`, `Files.deleteIfExists`, `file.toPath` |
| `private static void roundTrip(int[] cover, PayloadCodec.Payload payload, byte[] tokens, char[] passphrase, File file, HarnessReport.Entry entry, int index, Listener listener) throws IOException, GeneralSecurityException` | void — no value | runCase | `stage`, `checkCancelled`, `SystemClock.elapsedRealtime`, `VaultEngine.hide`, `save`, `reload`, `Arrays.equals`, `IOException`, `unlock`, `ImageUtils.statistics`, `ImageUtils.sha256` |
| `private static void save(int[] encoded, File file, HarnessReport.Entry entry, int index, Listener listener) throws IOException` | void — no value | roundTrip | `stage`, `SystemClock.elapsedRealtime`, `ImageUtils.fromPixels`, `ImageUtils.savePng`, `bitmap.recycle` |
| `private static int[] reload(File file, HarnessReport.Entry entry, int index, Listener listener) throws IOException` | int[] — `pixels` | roundTrip | `stage`, `SystemClock.elapsedRealtime`, `ImageUtils.decodeExact`, `ImageUtils.source`, `ImageUtils.pixels`, `bitmap.recycle` |
| `private static void unlock(int[] pixels, PayloadCodec.Payload expected, byte[] tokens, char[] passphrase, HarnessReport.Entry entry, int index, Listener listener) throws IOException, GeneralSecurityException` | void — no value | roundTrip | `stage`, `SystemClock.elapsedRealtime`, `checkCancelled`, `VaultEngine.reveal`, `actual.filename.equals`, `Arrays.equals`, `IOException` |
| `private static PayloadCodec.Payload payload(Context context, boolean file) throws IOException` | PayloadCodec.Payload — `new PayloadCodec.Payload(PayloadCodec.TEXT, "", text)`; `new PayloadCodec.Payload(PayloadCodec.FILE, "fixture.bin", bytes)` | runCase | `context.getString`, `getBytes`, `PayloadCodec.Payload` |
| `private static File temporaryPng(Context context) throws IOException` | File — `File.createTempFile("phase1b-", ".png", directory)` | runCase | `File`, `context.getCacheDir`, `directory.isDirectory`, `directory.mkdirs`, `IOException`, `File.createTempFile` |
| `private static void stage(Listener listener, int index, int resource)` | void — no value | run, roundTrip, save, reload, unlock | `checkCancelled`, `listener.onStage` |
| `private static void checkCancelled()` | void — no value | roundTrip, unlock, stage | `Thread.currentThread`, `isInterrupted`, `CancellationException` |

### app/src/main/java/com/example/kineticvault/diagnostics/TestHarnessActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override protected void onCreate(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `EdgeToEdge.enable`, `setContentView`, `bindViews`, `installInsets`, `light.setChecked`, `getResources`, `getConfiguration`, `light.setOnCheckedChangeListener`, `AppCompatDelegate.setDefaultNightMode`, `run.setOnClickListener`, `startTest` |
| `private void bindViews()` | void — no value | onCreate | `findViewById` |
| `private void installInsets()` | void — no value | onCreate | `findViewById`, `getResources`, `getDimensionPixelSize`, `ViewCompat.setOnApplyWindowInsetsListener`, `insets.getInsets`, `WindowInsetsCompat.Type.systemBars`, `WindowInsetsCompat.Type.displayCutout`, `view.setPadding`, `ViewCompat.requestApplyInsets` |
| `private void startTest()` | void — no value | onCreate | `setBusy`, `run.performHapticFeedback`, `status.setText`, `status.setTextColor`, `getColor`, `standard.setText`, `ghost.setText`, `summary.setText`, `worker.submit`, `this::executeTest` |
| `private void executeTest()` | void — no value | startTest | `HarnessRunner.run`, `getApplicationContext`, `main.post`, `showStage`, `showReport`, `fail` |
| `private void showStage(int index, int resource)` | void — no value | executeTest | `isDestroyed`, `isFinishing`, `summary.setText`, `getString` |
| `private void showReport(HarnessReport report)` | void — no value | executeTest | `isDestroyed`, `isFinishing`, `report.close`, `setBusy`, `status.setText`, `status.setTextColor`, `getColor`, `summary.setText`, `getString`, `standard.setText`, `modeText`, `ghost.setText`, `preview.setScaleType`, `preview.setImageBitmap`, `preview.setContentDescription` |
| `private String modeText(HarnessReport report, boolean ghostMode)` | String — `text.toString()` | showReport | `StringBuilder`, `text.length`, `text.append`, `getString`, `entry.sha256.substring`, `text.toString` |
| `private void fail(int message)` | void — no value | executeTest | `main.post`, `isDestroyed`, `isFinishing`, `setBusy`, `status.setText`, `status.setTextColor`, `getColor`, `summary.setText`, `getString` |
| `private void setBusy(boolean value)` | void — no value | startTest, showReport, fail | `run.setEnabled`, `light.setEnabled`, `run.setText`, `progress.setVisibility` |
| `@Override protected void onDestroy()` | void — no value | External owner / framework / callback in class atlas | `task.cancel`, `worker.shutdownNow`, `preview.setImageDrawable`, `super.onDestroy` |

### app/src/main/java/com/example/kineticvault/gesture/GesturePadView.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public GesturePadView(Context context)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public GesturePadView(Context context, AttributeSet attrs)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public GesturePadView(Context context, AttributeSet attrs, int defStyleAttr)` | New containing-class object | External owner / framework / callback in class atlas | `dimension`, `RectF`, `context.getString`, `setMinimumWidth`, `Math.ceil`, `setMinimumHeight`, `setFocusable`, `setClickable`, `setContentDescription`, `setAccessibilityDelegate`, `PadAccessibility` |
| `public void setOnTokenListener(OnTokenListener listener)` | void — no value | External owner / framework / callback in class atlas | No further method call in body |
| `@Override protected void onMeasure(int widthSpec, int heightSpec)` | void — no value | External owner / framework / callback in class atlas | `dimension`, `getPaddingLeft`, `getPaddingRight`, `getMinimumHeight`, `getPaddingTop`, `getPaddingBottom`, `setMeasuredDimension`, `resolveSize`, `Math.max`, `getMinimumWidth` |
| `@Override protected void onSizeChanged(int w, int h, int oldw, int oldh)` | void — no value | External owner / framework / callback in class atlas | `getPaddingLeft`, `getPaddingRight`, `getPaddingTop`, `getPaddingBottom`, `set`, `Math.max` |
| `@Override protected void onDraw(Canvas canvas)` | void — no value | External owner / framework / callback in class atlas | `super.onDraw`, `drawCell` |
| `private void drawCell(Canvas canvas, int index)` | void — no value | onDraw | `paint.setStyle`, `paint.setColor`, `getContext`, `getColor`, `canvas.drawRoundRect`, `paint.setStrokeWidth`, `hasFocus`, `isEnabled`, `paint.setTextSize`, `Math.max`, `cell.width`, `paint.measureText`, `paint.setTextAlign`, `canvas.drawText`, `cell.centerX`, `cell.centerY`, `paint.ascent`, `paint.descent` |
| `@Override public boolean onTouchEvent(MotionEvent event)` | boolean — `false`; `false`; `true` | External owner / framework / callback in class atlas | `isEnabled`, `event.getActionMasked`, `hit`, `event.getX`, `event.getY`, `event.getPointerId`, `requestFocus`, `event.findPointerIndex`, `clearPress`, `performClick`, `invalidate` |
| `@Override public boolean performClick()` | boolean — `false`; `false`; `false` | onTouchEvent, performAccessibilityAction | `isEnabled`, `SystemClock.elapsedRealtime`, `listener.onToken`, `performHapticFeedback`, `super.performClick` |
| `@Override public boolean onKeyDown(int keyCode, KeyEvent event)` | boolean — `super.onKeyDown(keyCode, event)`; `super.onKeyDown(keyCode, event)`; `true` | External owner / framework / callback in class atlas | `isEnabled`, `super.onKeyDown`, `Math.max`, `Math.min`, `setContentDescription`, `getContext`, `getString`, `invalidate` |
| `private int hit(float x, float y)` | int — `i`; `-1` | onTouchEvent | `width`, `height`, `contains` |
| `private void clearPress()` | void — no value | onTouchEvent, onDetachedFromWindow | No further method call in body |
| `private float dimension(int resource)` | float — `getResources().getDimension(resource)` | GesturePadView, onMeasure | `getResources`, `getDimension` |
| `@Override protected void onDetachedFromWindow()` | void — no value | External owner / framework / callback in class atlas | `clearPress`, `super.onDetachedFromWindow` |
| `@Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info)` | void — no value | External owner / framework / callback in class atlas | `super.onInitializeAccessibilityNodeInfo`, `info.addAction`, `AccessibilityNodeInfo.AccessibilityAction` |
| `@Override public boolean performAccessibilityAction(View host, int action, Bundle args)` | boolean — `performClick()`; `super.performAccessibilityAction(host, action, args)` | External owner / framework / callback in class atlas | `invalidate`, `performClick`, `super.performAccessibilityAction` |

### app/src/main/java/com/example/kineticvault/gesture/GestureRecognizer.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public void setSensitivity(int sensitivity)` | void — no value | External owner / framework / callback in class atlas | `Constants.tiltEnterDegrees`, `Constants.tiltExitDegrees`, `reset` |
| `public GestureRecognizer(Executor mainThreadExecutor, Listener listener)` | New containing-class object | External owner / framework / callback in class atlas | `Objects.requireNonNull` |
| `public void update(long time, float pitch, float roll, float ax, float ay, float az, float angularSpeed)` | void — no value | External owner / framework / callback in class atlas | `finite`, `Math.sqrt`, `Math.abs`, `recognize`, `callbackExecutor.execute`, `deliver` |
| `private void deliver(long stamp, byte token, long time, float pitch, float roll, boolean still)` | void — no value | update | `listener.onToken`, `listener.onMotion` |
| `private byte recognize(long time, float pitch, float roll, float az, boolean still)` | byte — `-1`; `token` | update | `Math.abs`, `detectSnap`, `detectFlat`, `Math.max` |
| `private boolean detectFlat(long time, float pitch, float roll, boolean still)` | boolean — `false`; `time - flatSince >= Constants.FLAT_HOLD_MS` | recognize | `Math.abs` |
| `private boolean detectSnap(long time, float pitch, float roll, float az)` | boolean — `false`; `pulse`; `false` | recognize | `Math.max`, `Math.abs` |
| `private static boolean finite(float... values)` | boolean — `false`; `true` | update | `Float.isFinite` |
| `public void reset()` | void — no value | setSensitivity | No further method call in body |

### app/src/main/java/com/example/kineticvault/gesture/GestureSession.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public GestureSession(Listener listener)` | New containing-class object | External owner / framework / callback in class atlas | `Objects.requireNonNull` |
| `public boolean addToken(byte token, long timestampMillis)` | boolean — `false`; `false`; `true` | External owner / framework / callback in class atlas | `IllegalArgumentException`, `listener.onProgress` |
| `public void onMotion(long timestampMillis, boolean still)` | void — no value | External owner / framework / callback in class atlas | `done` |
| `public boolean done()` | boolean — `false`; `true`; `false` | onMotion | `listener.onTooShort`, `listener.onFinished` |
| `public int size()` | int — `count` | External owner / framework / callback in class atlas | No further method call in body |
| `public boolean isFinished()` | boolean — `finished && !closed` | matches, takeTokens | No further method call in body |
| `public static boolean matches(GestureSession first, GestureSession second)` | boolean — `false`; `difference == 0` | External owner / framework / callback in class atlas | `first.isFinished`, `second.isFinished` |
| `public byte[] takeTokens()` | byte[] — `result` | External owner / framework / callback in class atlas | `isFinished`, `IllegalStateException`, `Arrays.copyOf`, `close` |
| `public void reset()` | void — no value | External owner / framework / callback in class atlas | `IllegalStateException`, `clear`, `listener.onProgress` |
| `private void clear()` | void — no value | reset, close | `Arrays.fill` |
| `@Override public void close()` | void — no value | takeTokens | `clear` |

### app/src/main/java/com/example/kineticvault/gesture/StrengthMeter.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private StrengthMeter()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static double bits(int tokenCount)` | double — `GestureCredential.entropyBits(tokenCount)` | label | `GestureCredential.entropyBits` |
| `public static String label(Context context, int tokenCount, boolean hasPassphrase)` | String — `context.getString(R.string.gesture_strength_incomplete, upperBound, Constants.MIN_TOKENS - tokenCount)`; `context.getString(hasPassphrase ? R.string.gesture_strength_with_passphrase : R.string.gesture_strength_add_passphrase, upperBound)` | External owner / framework / callback in class atlas | `bits`, `context.getString` |

### app/src/main/java/com/example/kineticvault/image/CoverGenerator.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private CoverGenerator()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static Bitmap generate(int width, int height, long seed) throws IOException` | Bitmap — `bitmap` | External owner / framework / callback in class atlas | `ImageUtils.requireWorker`, `ImageMemory.check`, `Bitmap.createBitmap`, `ColorSpace.get`, `Random`, `Thread.currentThread`, `isInterrupted`, `CancellationException`, `color`, `bitmap.setPixels`, `bitmap.recycle` |
| `private static int color(double x, double y, Random random)` | int — `0xff000000 \| (red << 16) \| (green << 8) \| blue` | generate | `Math.sin`, `Math.exp`, `random.nextInt`, `clamp` |
| `private static int clamp(int value)` | int — `Math.max(0, Math.min(255, value))` | color | `Math.max`, `Math.min` |

### app/src/main/java/com/example/kineticvault/image/ImageDecoder.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private ImageDecoder()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `static Bitmap cover(ImageUtils.Source source, boolean ghost) throws IOException` | Bitmap — `renderOpaque(decoded, size[0], size[1])` | External owner / framework / callback in class atlas | `ImageUtils.requireWorker`, `bounds`, `ImageMemory.maximumPixels`, `ImageMemory.availableBudget`, `options`, `sampleSize`, `decode`, `ImageMemory.fitCover`, `decoded.getWidth`, `decoded.getHeight`, `ImageMemory.check`, `renderOpaque`, `decoded.recycle` |
| `static Bitmap exact(ImageUtils.Source source, boolean ghost) throws IOException` | Bitmap — `decoded` | External owner / framework / callback in class atlas | `ImageUtils.requireWorker`, `bounds`, `ImageMemory.check`, `decode`, `options`, `decoded.getWidth`, `decoded.getHeight`, `IOException`, `ImageUtils.requireOpaque`, `decoded.recycle` |
| `private static BitmapFactory.Options bounds(ImageUtils.Source source) throws IOException` | BitmapFactory.Options — `options` | cover, exact | `PngIntegrity.read`, `BitmapFactory.Options`, `source.open`, `IOException`, `BitmapFactory.decodeStream` |
| `private static BitmapFactory.Options options()` | BitmapFactory.Options — `options` | cover, exact | `BitmapFactory.Options`, `ColorSpace.get` |
| `private static Bitmap decode(ImageUtils.Source source, BitmapFactory.Options options) throws IOException` | Bitmap — `bitmap` | cover, exact | `source.open`, `IOException`, `BitmapFactory.decodeStream`, `bitmap.getConfig`, `bitmap.isMutable`, `bitmap.getColorSpace`, `isSrgb`, `bitmap.recycle` |
| `private static int sampleSize(int width, int height, int limit)` | int — `sample` | cover | No further method call in body |
| `static Bitmap renderOpaque(Bitmap source, int width, int height)` | Bitmap — `target` | cover | `Bitmap.createBitmap`, `ColorSpace.get`, `Canvas`, `canvas.drawColor`, `Paint`, `canvas.drawBitmap`, `Rect`, `target.recycle` |

### app/src/main/java/com/example/kineticvault/image/ImageFiles.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private ImageFiles()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `static void savePng(Bitmap bitmap, File destination) throws IOException` | void — no value | External owner / framework / callback in class atlas | `ImageUtils.requireWorker`, `ImageUtils.requireOpaque`, `destination.getAbsoluteFile`, `getParentFile`, `parent.isDirectory`, `parent.mkdirs`, `IOException`, `File.createTempFile`, `write`, `Files.move`, `temporary.toPath`, `destination.toPath`, `Files.deleteIfExists` |
| `private static void write(Bitmap bitmap, File file) throws IOException` | void — no value | savePng | `FileOutputStream`, `bitmap.compress`, `IOException`, `output.flush`, `output.getFD`, `sync` |
| `static String sha256(File file) throws IOException` | String — `text.toString()` | External owner / framework / callback in class atlas | `ImageUtils.requireWorker`, `MessageDigest.getInstance`, `FileInputStream`, `input.read`, `digest.update`, `StringBuilder`, `digest.digest`, `text.append`, `Character.forDigit`, `text.toString`, `IOException` |

### app/src/main/java/com/example/kineticvault/image/ImageMemory.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private ImageMemory()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static long availableBudget()` | long — `Math.max(0, Math.min(runtime.maxMemory() / 2, headroom))` | check | `Runtime.getRuntime`, `runtime.totalMemory`, `runtime.freeMemory`, `runtime.maxMemory`, `Math.max`, `Math.min` |
| `public static long requiredBytes(int pixels, boolean ghost)` | long — `WORK_RESERVE + (long) pixels * (ghost ? 32 : 28)` | External owner / framework / callback in class atlas | No further method call in body |
| `public static int maximumPixels(boolean ghost, long budget) throws IOException` | int — `(int) Math.min(Constants.MAX_PIXELS, count)` | check | `IOException`, `Math.min` |
| `public static int pixelCount(int width, int height) throws IOException` | int — `(int) count` | check | `IOException` |
| `public static void check(int width, int height, boolean ghost) throws IOException` | void — no value | External owner / framework / callback in class atlas | `pixelCount`, `maximumPixels`, `availableBudget`, `IOException` |
| `public static int[] fitCover(int width, int height, int maxPixels) throws IOException` | int[] — `new int[]{targetWidth, targetHeight}` | External owner / framework / callback in class atlas | `IOException`, `Math.min`, `Math.max`, `Math.sqrt`, `Math.floor` |

### app/src/main/java/com/example/kineticvault/image/ImageStats.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private ImageStats(int changed, int count, int maximum, long squaredError)` | New containing-class object | compare | `Math.log10` |
| `public static ImageStats compare(int[] before, int[] after)` | ImageStats — `new ImageStats(changed, before.length, maximum, squaredError)` | External owner / framework / callback in class atlas | `IllegalArgumentException`, `Math.max`, `Math.abs`, `ImageStats` |

### app/src/main/java/com/example/kineticvault/image/ImageUtils.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private ImageUtils()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static Source source(File file)` | Source — `() -> new FileInputStream(file)` | External owner / framework / callback in class atlas | `FileInputStream` |
| `public static Source source(ContentResolver resolver, Uri uri)` | Source — `() -> resolver.openInputStream(uri)` | External owner / framework / callback in class atlas | `resolver.openInputStream` |
| `public static Bitmap decodeCover(Source source, boolean ghost) throws IOException` | Bitmap — `ImageDecoder.cover(source, ghost)` | External owner / framework / callback in class atlas | `ImageDecoder.cover` |
| `public static Bitmap decodeExact(Source source, boolean ghost) throws IOException` | Bitmap — `ImageDecoder.exact(source, ghost)` | External owner / framework / callback in class atlas | `ImageDecoder.exact` |
| `public static Bitmap flattenOpaque(Bitmap source) throws IOException` | Bitmap — `ImageDecoder.renderOpaque(source, source.getWidth(), source.getHeight())` | External owner / framework / callback in class atlas | `requireWorker`, `requireSoftware`, `ImageMemory.check`, `source.getWidth`, `source.getHeight`, `ImageDecoder.renderOpaque` |
| `public static Bitmap thumbnail(Bitmap source, int longSide) throws IOException` | Bitmap — `ImageDecoder.renderOpaque(source, width, height)` | External owner / framework / callback in class atlas | `requireWorker`, `requireSoftware`, `IOException`, `Math.min`, `Math.max`, `source.getWidth`, `source.getHeight`, `Math.floor`, `ImageDecoder.renderOpaque` |
| `public static int[] pixels(Bitmap bitmap) throws IOException` | int[] — `pixels` | External owner / framework / callback in class atlas | `requireWorker`, `requireOpaque`, `ImageMemory.pixelCount`, `bitmap.getWidth`, `bitmap.getHeight`, `bitmap.getPixels` |
| `public static Bitmap fromPixels(int[] pixels, int width, int height) throws IOException` | Bitmap — `bitmap` | External owner / framework / callback in class atlas | `requireWorker`, `ImageMemory.pixelCount`, `IOException`, `Bitmap.createBitmap`, `ColorSpace.get`, `bitmap.setPixels` |
| `public static void savePng(Bitmap bitmap, File destination) throws IOException` | void — no value | External owner / framework / callback in class atlas | `ImageFiles.savePng` |
| `public static String sha256(File file) throws IOException` | String — `ImageFiles.sha256(file)` | External owner / framework / callback in class atlas | `ImageFiles.sha256` |
| `public static ImageStats statistics(int[] before, int[] after)` | ImageStats — `ImageStats.compare(before, after)` | psnr | `requireWorker`, `ImageStats.compare` |
| `public static double psnr(int[] before, int[] after)` | double — `statistics(before, after).psnrDb` | External owner / framework / callback in class atlas | `statistics` |
| `public static void checkMemory(int width, int height, boolean ghost) throws IOException` | void — no value | External owner / framework / callback in class atlas | `ImageMemory.check` |
| `static void requireSoftware(Bitmap bitmap) throws IOException` | void — no value | flattenOpaque, thumbnail, requireOpaque | `bitmap.isRecycled`, `bitmap.getConfig`, `bitmap.getColorSpace`, `isSrgb`, `IOException` |
| `static void requireOpaque(Bitmap bitmap) throws IOException` | void — no value | pixels | `requireSoftware`, `ImageMemory.pixelCount`, `bitmap.getWidth`, `bitmap.getHeight`, `bitmap.hasAlpha`, `Math.min`, `bitmap.getPixels`, `IOException` |
| `public static void requireWorker()` | void — no value | flattenOpaque, thumbnail, pixels, fromPixels, statistics | `Looper.myLooper`, `Looper.getMainLooper`, `IllegalStateException` |

### app/src/main/java/com/example/kineticvault/image/PngIntegrity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private PngIntegrity()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `static void read(ImageUtils.Source source) throws IOException` | void — no value | chunks | `source.open`, `IOException`, `DataInputStream`, `BufferedInputStream`, `input.read`, `chunks` |
| `private static void chunks(DataInputStream input) throws IOException` | void — no value | read | `Thread.currentThread`, `isInterrupted`, `InterruptedIOException`, `input.readInt`, `IOException`, `checksum`, `input.read` |
| `private static void checksum(DataInputStream input, int type, int length, byte[] buffer) throws IOException` | void — no value | chunks | `CRC32`, `crc.update`, `Thread.currentThread`, `isInterrupted`, `InterruptedIOException`, `Math.min`, `input.readFully`, `crc.getValue`, `input.readInt`, `IOException` |

### app/src/main/java/com/example/kineticvault/sensor/SensorFusionManager.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public void setTelemetryListener(TelemetryListener listener)` | void — no value | External owner / framework / callback in class atlas | No further method call in body |
| `public SensorFusionManager(Context context, GestureRecognizer.Listener listener)` | New containing-class object | External owner / framework / callback in class atlas | `context.getApplicationContext`, `getSystemService`, `GestureRecognizer`, `Looper.myLooper`, `Looper.getMainLooper`, `command.run`, `main.post` |
| `public Mode start()` | Mode — `mode`; `mode`; `mode` | External owner / framework / callback in class atlas | `requireMainThread`, `stop`, `recognizer.setSensitivity`, `com.example.kineticvault.data.VaultPreferences.current`, `sensors.getDefaultSensor`, `sensors.registerListener` |
| `public void stop()` | void — no value | start | `requireMainThread`, `sensors.unregisterListener`, `recognizer.reset`, `Arrays.fill` |
| `public void resetGesture()` | void — no value | External owner / framework / callback in class atlas | `requireMainThread`, `recognizer.reset` |
| `@Override public void onSensorChanged(SensorEvent event)` | void — no value | External owner / framework / callback in class atlas | `Float.isFinite`, `event.sensor.getType`, `accelerometer`, `System.arraycopy`, `gyroscope` |
| `private void accelerometer(SensorEvent event)` | void — no value | onSensorChanged | `seconds`, `magnitude`, `degrees`, `Math.atan2`, `Math.hypot`, `useAccelerometer`, `emit` |
| `private void useAccelerometer(float dt, boolean restart)` | void — no value | accelerometer | `wrap`, `Math.toRadians`, `Math.hypot` |
| `private void gyroscope(SensorEvent event)` | void — no value | onSensorChanged | `seconds`, `magnitude`, `Math.toRadians`, `Math.abs`, `Math.cos`, `Math.sin`, `Math.tan`, `blend`, `degrees` |
| `private static float blend(float theta, float omega, float dt, float accelAngle)` | float — `wrap(predicted + (1 - Constants.FILTER_ALPHA) * wrap(accelAngle - predicted))` | gyroscope | `wrap` |
| `private void emit(long timestampNanos)` | void — no value | accelerometer | `Math.toRadians`, `Math.sin`, `Math.cos`, `telemetry.onSample`, `recognizer.update` |
| `private static float magnitude(float[] values)` | float — `(float) Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])` | accelerometer, gyroscope | `Math.sqrt` |
| `private static float seconds(long now, long before)` | float — `before < 0 ? 0f : (now - before) / 1_000_000_000f` | accelerometer, gyroscope | No further method call in body |
| `private static float degrees(double radians)` | float — `(float) Math.toDegrees(radians)` | accelerometer, gyroscope | `Math.toDegrees` |
| `private static float wrap(float degrees)` | float — `(degrees + 180f - (float) Math.floor((degrees + 180f) / 360f) * 360f) - 180f` | useAccelerometer, blend | `Math.floor` |
| `private static void requireMainThread()` | void — no value | start, stop, resetGesture | `Looper.myLooper`, `Looper.getMainLooper`, `IllegalStateException` |
| `@Override public void onAccuracyChanged(Sensor sensor, int accuracy)` | void — no value | External owner / framework / callback in class atlas | No further method call in body |

### app/src/main/java/com/example/kineticvault/ui/AboutActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override protected void onCreate(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `showPage` |

### app/src/main/java/com/example/kineticvault/ui/CalculatorActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override protected void onCreate(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `EdgeToEdge.enable`, `getWindow`, `addFlags`, `setContentView`, `findViewById`, `getResources`, `getDimensionPixelSize`, `ViewCompat.setOnApplyWindowInsetsListener`, `insets.getInsets`, `WindowInsetsCompat.Type.systemBars`, `WindowInsetsCompat.Type.displayCutout`, `v.setPadding`, `ViewCompat.requestApplyInsets`, `bind`, `reset` |
| `private void bind(android.view.ViewGroup group)` | void — no value | onCreate | `group.getChildCount`, `group.getChildAt`, `bind`, `child.getTag`, `child.setOnClickListener`, `press`, `v.getTag`, `toString` |
| `private void press(String key)` | void — no value | bind | `equals`, `reset`, `equalsPressed`, `contains`, `operator`, `input.length`, `input.deleteCharAt`, `render`, `wipeInput`, `input.indexOf`, `input.append` |
| `private void equalsPressed()` | void — no value | press | `input.length`, `input.indexOf`, `VaultPreferences.current`, `calculate`, `input.getChars`, `VaultPreferences.unlockDisguise`, `isDestroyed`, `isFinishing`, `reset`, `display.performHapticFeedback`, `startActivity`, `Intent`, `finish` |
| `private void operator(String next)` | void — no value | press | `number`, `apply`, `showNumber`, `fail` |
| `private void calculate()` | void — no value | equalsPressed | `number`, `apply`, `showNumber`, `fail` |
| `private BigDecimal apply(BigDecimal left, BigDecimal right)` | BigDecimal — `left.add(right, MathContext.DECIMAL64)`; `left.subtract(right, MathContext.DECIMAL64)`; `left.multiply(right, MathContext.DECIMAL64)` | operator, calculate | `left.add`, `left.subtract`, `left.multiply`, `left.divide` |
| `private BigDecimal number()` | BigDecimal — `input.length() == 0 ? BigDecimal.ZERO : new BigDecimal(input.toString())` | operator, calculate | `input.length`, `BigDecimal`, `input.toString` |
| `private void showNumber(BigDecimal value)` | void — no value | operator, calculate | `wipeInput`, `input.append`, `value.stripTrailingZeros`, `toPlainString`, `render` |
| `private void render()` | void — no value | press, showNumber, reset | `display.setText`, `input.length`, `getString` |
| `private void wipeInput()` | void — no value | press, showNumber, reset | `input.length`, `input.setCharAt`, `input.setLength` |
| `private void reset()` | void — no value | onCreate, press, equalsPressed, fail, onPause | `wipeInput`, `render` |
| `private void fail()` | void — no value | operator, calculate | `reset`, `display.setText`, `display.performHapticFeedback` |
| `@Override protected void onPause()` | void — no value | External owner / framework / callback in class atlas | `reset`, `super.onPause` |

### app/src/main/java/com/example/kineticvault/ui/DiffMapData.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private DiffMapData(int[] before, int[] after, int width, int height, Bitmap diff, Bitmap xray, ImageStats stats)` | New containing-class object | build | No further method call in body |
| `public static DiffMapData build(int[] before, int[] after, int width, int height) throws IOException` | DiffMapData — `new DiffMapData(before, after, width, height, diff, xray, ImageUtils.statistics(before, after))` | External owner / framework / callback in class atlas | `ImageUtils.requireWorker`, `IOException`, `Math.min`, `Math.max`, `Arrays.fill`, `ImageUtils.fromPixels`, `buildXRay`, `DiffMapData`, `ImageUtils.statistics`, `diff.recycle`, `xray.recycle` |
| `public static Bitmap buildXRay(int[] pixels, int width, int height) throws IOException` | Bitmap — `ImageUtils.fromPixels(plane, w, h)` | build | `ImageUtils.requireWorker`, `IOException`, `Math.min`, `Math.max`, `ImageUtils.fromPixels` |
| `public int original(int x, int y)` | int — `before[y * width + x]` | External owner / framework / callback in class atlas | No further method call in body |
| `public int encoded(int x, int y)` | int — `after[y * width + x]` | External owner / framework / callback in class atlas | No further method call in body |
| `@Override public void close()` | void — no value | External owner / framework / callback in class atlas | `diff.recycle`, `xray.recycle`, `Arrays.fill` |

### app/src/main/java/com/example/kineticvault/ui/DiffMapView.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public DiffMapView(Context context)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public DiffMapView(Context context, AttributeSet attrs)` | New containing-class object | External owner / framework / callback in class atlas | `setContentDescription`, `context.getString`, `setFocusable`, `setClickable` |
| `public void setData(DiffMapData value)` | void — no value | External owner / framework / callback in class atlas | `stopSweep`, `invalidate` |
| `public void startSweep()` | void — no value | External owner / framework / callback in class atlas | `stopSweep`, `ValueAnimator.ofFloat`, `sweep.setDuration`, `sweep.addUpdateListener`, `a.getAnimatedValue`, `invalidate`, `sweep.start` |
| `public void stopSweep()` | void — no value | setData, startSweep, onDetachedFromWindow, onWindowVisibilityChanged | `sweep.cancel`, `invalidate` |
| `public void bindLoupe(TextView label)` | void — no value | External owner / framework / callback in class atlas | `setOnTouchListener`, `touch`, `setOnClickListener`, `inspect` |
| `private boolean touch(MotionEvent event, TextView label)` | boolean — `false`; `false`; `true` | bindLoupe | `event.getActionMasked`, `area.contains`, `event.getX`, `event.getY`, `getParent`, `requestDisallowInterceptTouchEvent`, `Math.max`, `Math.min`, `area.width`, `area.height`, `inspect`, `performHapticFeedback`, `performClick` |
| `private void inspect(int x, int y, TextView label)` | void — no value | bindLoupe, touch | `label.setText`, `getContext`, `getString`, `rgb`, `data.original`, `data.encoded`, `invalidate` |
| `private String rgb(int color)` | String — `getContext().getString(R.string.diff_rgb, r, g, b, r & 1, g & 1, b & 1)` | inspect | `getContext`, `getString` |
| `@Override public boolean performClick()` | boolean — `true`; `super.performClick()` | touch | `sendAccessibilityEvent`, `super.performClick` |
| `@Override protected void onDraw(Canvas canvas)` | void — no value | External owner / framework / callback in class atlas | `super.onDraw`, `canvas.drawColor`, `Math.min`, `getWidth`, `getHeight`, `area.set`, `canvas.save`, `canvas.clipRect`, `canvas.drawBitmap`, `canvas.restoreToCount`, `paint.setColor`, `getContext`, `getColor`, `paint.setStrokeWidth`, `canvas.drawLine`, `drawLoupe` |
| `private void drawLoupe(Canvas canvas)` | void — no value | onDraw | `data.diff.getWidth`, `data.diff.getHeight`, `Rect`, `Math.max`, `Math.min`, `getWidth`, `getResources`, `getDisplayMetrics`, `RectF`, `canvas.drawBitmap`, `paint.setStyle`, `canvas.drawRect`, `area.width`, `area.height`, `canvas.drawLine` |
| `@Override protected void onDetachedFromWindow()` | void — no value | External owner / framework / callback in class atlas | `stopSweep`, `super.onDetachedFromWindow` |
| `@Override protected void onWindowVisibilityChanged(int visibility)` | void — no value | External owner / framework / callback in class atlas | `super.onWindowVisibilityChanged`, `stopSweep` |

### app/src/main/java/com/example/kineticvault/ui/DiffStats.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private DiffStats()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static void show(TextView view, ImageStats stats)` | void — no value | External owner / framework / callback in class atlas | `Double.isInfinite`, `view.getContext`, `getString`, `view.setText` |

### app/src/main/java/com/example/kineticvault/ui/ExportSheet.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private ExportSheet()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static BottomSheetDialog show(Activity host, int note, int[] labels, Runnable[] actions)` | BottomSheetDialog — `insets`; `sheet` | External owner / framework / callback in class atlas | `BottomSheetDialog`, `host.getLayoutInflater`, `inflate`, `content.findViewById`, `setText`, `MaterialButton`, `button.setText`, `button.setMinHeight`, `dp`, `button.setOnClickListener`, `UiPolish.feedback`, `sheet.dismiss`, `host.isFinishing`, `host.isDestroyed`, `action.run`, `options.addView`, `LinearLayout.LayoutParams`, `setOnClickListener`, `sheet.setContentView`, `sheet.show`, `host.getWindow`, `getAttributes`, `sheet.getWindow`, `addFlags`, `ViewCompat.setOnApplyWindowInsetsListener`, `insets.getInsets`, `WindowInsetsCompat.Type.systemBars`, `WindowInsetsCompat.Type.displayCutout`, `v.setPadding`, `ViewCompat.requestApplyInsets` |
| `private static int dp(Activity host, int value)` | int — `Math.round(value * host.getResources().getDisplayMetrics().density)` | show | `Math.round`, `host.getResources`, `getDisplayMetrics` |

### app/src/main/java/com/example/kineticvault/ui/GhostComparisonActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override protected void onCreate(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `EdgeToEdge.enable`, `setContentView`, `findViewById`, `getResources`, `getDimensionPixelSize`, `ViewCompat.setOnApplyWindowInsetsListener`, `insets.getInsets`, `WindowInsetsCompat.Type.systemBars`, `WindowInsetsCompat.Type.displayCutout`, `view.setPadding`, `ViewCompat.requestApplyInsets`, `standard.bindLoupe`, `ghost.bindLoupe`, `setOnClickListener`, `finish`, `generate`, `standard.startSweep`, `ghost.startSweep` |
| `private void generate()` | void — no value | onCreate | `setBusy`, `findViewById`, `setText`, `getString`, `worker.execute`, `GhostComparisonWork.create`, `main.post`, `dispose`, `show`, `UiPolish.failure` |
| `private void show(GhostComparisonWork.Pair value)` | void — no value | generate | `standard.setData`, `ghost.setData`, `DiffStats.show`, `findViewById`, `setText`, `setBusy`, `getLifecycle`, `getCurrentState`, `isAtLeast`, `standard.startSweep`, `ghost.startSweep`, `standard.performHapticFeedback`, `dispose` |
| `private void setBusy(boolean value)` | void — no value | generate, show | `findViewById`, `setVisibility`, `setEnabled` |
| `private void dispose(GhostComparisonWork.Pair value)` | void — no value | generate, show, onDestroy | `worker.isShutdown`, `worker.execute`, `Executors.newSingleThreadExecutor`, `cleanup.execute`, `cleanup.shutdown`, `value::close`, `value::close` |
| `@Override protected void onPause()` | void — no value | External owner / framework / callback in class atlas | `standard.stopSweep`, `ghost.stopSweep`, `super.onPause` |
| `@Override protected void onDestroy()` | void — no value | External owner / framework / callback in class atlas | `standard.setData`, `ghost.setData`, `dispose`, `worker.shutdown`, `super.onDestroy` |

### app/src/main/java/com/example/kineticvault/ui/GhostComparisonWork.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `Pair(DiffMapData standard, DiffMapData ghost)` | New containing-class object | create | No further method call in body |
| `@Override public void close()` | void — no value | create | `standard.close`, `ghost.close` |
| `private GhostComparisonWork()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `static Pair create(String publicText) throws Exception` | Pair — `new Pair(standard, ghost)` | External owner / framework / callback in class atlas | `ImageUtils.requireWorker`, `CoverGenerator.generate`, `System.nanoTime`, `publicText.getBytes`, `SecureRandom`, `random.nextInt`, `ImageUtils.pixels`, `guard`, `PayloadCodec.Payload`, `encode`, `Pair`, `standard.close`, `ghost.close`, `cover.recycle`, `Arrays.fill` |
| `private static DiffMapData encode(int[] original, PayloadCodec.Payload payload, byte[] tokens, char[] phrase, boolean ghost, WorkProgress progress) throws Exception` | DiffMapData — `DiffMapData.build(original.clone(), encoded, 320, 320)` | create | `guard`, `VaultEngine.hide`, `DiffMapData.build`, `original.clone` |
| `private static void guard()` | void — no value | create, encode | `Thread.currentThread`, `isInterrupted`, `CancellationException` |

### app/src/main/java/com/example/kineticvault/ui/hide/CoverStep.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `CoverStep(HideActivity host, HideWork work)` | New containing-class object | External owner / framework / callback in class atlas | `host.findViewById`, `host.registerForActivityResult`, `ActivityResultContracts.OpenDocument`, `load`, `work.store.load`, `CameraContract`, `setBusy`, `deleteCapture`, `host.message`, `setOnClickListener`, `openGallery`, `openCamera`, `host.showStep`, `work.store::finishCapture`, `work.store::generate` |
| `private void openGallery()` | void — no value | CoverStep | `gallery.launch`, `host.message` |
| `private void openCamera()` | void — no value | CoverStep | `setBusy`, `work.run`, `success`, `host.isForeground`, `resume`, `failure`, `host.message`, `work.store::prepareCapture` |
| `public void success(Uri uri)` | void — no value | openCamera, deleteCapture, load | `host.isForeground`, `resume` |
| `public void failure(int message)` | void — no value | openCamera, deleteCapture, load | `setBusy`, `host.message` |
| `private void deleteCapture()` | void — no value | CoverStep, resume | `work.run`, `work.store.deleteCapture`, `success`, `failure`, `host.message` |
| `public void success(Boolean ignored)` | void — no value | openCamera, deleteCapture, load | No further method call in body |
| `public void failure(int message)` | void — no value | openCamera, deleteCapture, load | `host.message` |
| `private void load(Callable<HideStore.CoverInfo> action)` | void — no value | CoverStep | `preview.setImageDrawable`, `setBusy`, `info.setText`, `work.run`, `success`, `preview.setImageBitmap`, `host.getString`, `failure`, `host.message` |
| `public void success(HideStore.CoverInfo cover)` | void — no value | openCamera, deleteCapture, load | `preview.setImageBitmap`, `info.setText`, `host.getString`, `setBusy` |
| `public void failure(int message)` | void — no value | openCamera, deleteCapture, load | `setBusy`, `info.setText`, `host.message` |
| `private void setBusy(boolean busy)` | void — no value | CoverStep, openCamera, failure, load, success, resume | `host.findViewById`, `setEnabled`, `setVisibility` |
| `void resume()` | void — no value | openCamera, success | `camera.launch`, `deleteCapture`, `setBusy`, `host.message` |
| `void close()` | void — no value | External owner / framework / callback in class atlas | `preview.setImageDrawable` |
| `@Override public Intent createIntent(Context context, Uri input)` | Intent — `intent` | External owner / framework / callback in class atlas | `super.createIntent`, `intent.addFlags`, `intent.setClipData`, `ClipData.newRawUri`, `context.getString` |

### app/src/main/java/com/example/kineticvault/ui/hide/GestureReplay.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `GestureReplay(HideActivity host)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `void show(byte[] enrolled)` | void — no value | External owner / framework / callback in class atlas | `clear`, `enrolled.clone`, `host.getWindow`, `addFlags`, `LinearLayout`, `panel.setOrientation`, `host.getResources`, `getDimensionPixelSize`, `panel.setPadding`, `TextView`, `label.setSaveEnabled`, `label.setAccessibilityLiveRegion`, `KineticOrbView`, `orb.setSaveEnabled`, `panel.addView`, `LinearLayout.LayoutParams`, `MaterialAlertDialogBuilder`, `setTitle`, `setMessage`, `setView`, `setPositiveButton`, `create`, `dialog.setOnDismissListener`, `dialog.show`, `dialog.getWindow`, `orb.setAnimating`, `ValueAnimator.areAnimatorsEnabled`, `next` |
| `private void next()` | void — no value | show | `label.setText`, `host.getString`, `orb.setAngles`, `orb.setTokenCount`, `main.postDelayed`, `Arrays.fill` |
| `void clear()` | void — no value | show | `main.removeCallbacks`, `Arrays.fill`, `label.setText`, `orb.setAnimating`, `orb.reset`, `old.setOnDismissListener`, `old.dismiss`, `com.example.kineticvault.data.VaultPreferences.current`, `host.getWindow`, `clearFlags` |

### app/src/main/java/com/example/kineticvault/ui/hide/GestureStep.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `GestureStep(HideActivity host, SecretStep secret)` | New containing-class object | External owner / framework / callback in class atlas | `GestureReplay`, `host.findViewById`, `com.example.kineticvault.ui.NerdOverlay`, `SensorFusionManager`, `GestureRecognizer.Listener`, `onToken`, `nerd.token`, `current.addToken`, `onMotion`, `orb.setAngles`, `current.onMotion`, `sensors.setTelemetryListener`, `bind`, `usePad.setChecked`, `com.example.kineticvault.data.VaultPreferences.current`, `pad.setVisibility`, `usePad.isChecked` |
| `public void onToken(byte token, long time)` | void — no value | GestureStep | `nerd.token`, `current.addToken` |
| `public void onMotion(float pitch, float roll, boolean still, long time)` | void — no value | GestureStep | `orb.setAngles`, `current.onMotion` |
| `private void bind()` | void — no value | GestureStep | `pad.setOnTokenListener`, `nerd.token`, `current.addToken`, `usePad.setOnCheckedChangeListener`, `reset`, `pad.setVisibility`, `record.setOnClickListener`, `begin`, `done.setOnClickListener`, `current.done`, `forge.setOnClickListener`, `host.forge`, `host.findViewById`, `setOnClickListener`, `replay.show` |
| `private void begin()` | void — no value | bind | `nerd.begin`, `usePad.isChecked`, `orb.reset`, `orb.setAnimating`, `strength.setText`, `StrengthMeter.label`, `secret.hasPassphrase`, `status.setText`, `GestureSession`, `GestureSession.Listener`, `onProgress`, `orb.setTokenCount`, `onFinished`, `finishAttempt`, `onTooShort`, `host.message`, `record.setEnabled`, `done.setEnabled`, `pad.setEnabled`, `startSensors` |
| `public void onProgress(int count)` | void — no value | begin | `orb.setTokenCount`, `usePad.isChecked`, `strength.setText`, `StrengthMeter.label`, `secret.hasPassphrase` |
| `public void onFinished()` | void — no value | begin | `finishAttempt` |
| `public void onTooShort(int count)` | void — no value | begin | `host.message` |
| `private void startSensors()` | void — no value | begin | `sensors.start`, `usePad.setChecked`, `host.message` |
| `private void finishAttempt()` | void — no value | begin, onFinished | `sensors.stop`, `nerd.clear`, `orb.setAnimating`, `done.setEnabled`, `pad.setEnabled`, `record.setText`, `record.setEnabled`, `status.setText`, `GestureSession.matches`, `reset`, `host.message`, `first.takeTokens`, `current.close`, `status.performHapticFeedback`, `forge.setEnabled`, `host.findViewById`, `setEnabled` |
| `byte[] takeTokens()` | byte[] — `null`; `result` | finishAttempt | `replay.clear`, `forge.setEnabled` |
| `void reset()` | void — no value | bind, begin, finishAttempt | `replay.clear`, `host.findViewById`, `setEnabled`, `sensors.stop`, `nerd.clear`, `orb.setAnimating`, `orb.reset`, `current.close`, `first.close`, `Arrays.fill`, `record.setText`, `record.setEnabled`, `done.setEnabled`, `forge.setEnabled`, `pad.setEnabled`, `status.setText`, `strength.setText`, `StrengthMeter.label`, `secret.hasPassphrase` |

### app/src/main/java/com/example/kineticvault/ui/hide/HideActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override protected void onCreate(Bundle savedState)` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `com.example.kineticvault.data.VaultPreferences.current`, `getWindow`, `addFlags`, `EdgeToEdge.enable`, `setContentView`, `findViewById`, `setSaveFromParentEnabled`, `installInsets`, `HideWork`, `getApplicationContext`, `CoverStep`, `SecretStep`, `GestureStep`, `ResultStep`, `setOnClickListener`, `back`, `getOnBackPressedDispatcher`, `addCallback`, `OnBackPressedCallback`, `handleOnBackPressed`, `showStep`, `message` |
| `getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true)` | New containing-class object | onCreate | `handleOnBackPressed`, `back` |
| `@Override public void handleOnBackPressed()` | void — no value | onCreate, getOnBackPressedDispatcher | `back` |
| `private void installInsets()` | void — no value | onCreate | `findViewById`, `getResources`, `getDimensionPixelSize`, `ViewCompat.setOnApplyWindowInsetsListener`, `insets.getInsets`, `WindowInsetsCompat.Type.systemBars`, `WindowInsetsCompat.Type.displayCutout`, `WindowInsetsCompat.Type.ime`, `view.setPadding`, `ViewCompat.requestApplyInsets` |
| `void showStep(int next)` | void — no value | onCreate, forge, success, failure, back, onPause | `secret.deactivate`, `gesture.reset`, `findViewById`, `setVisibility`, `setDisplayedChild`, `setText`, `getString`, `setProgressCompat`, `rain.setRunning`, `hideKeyboard`, `secret.activate`, `startAnimation`, `android.view.animation.AnimationUtils.loadAnimation` |
| `void forge()` | void — no value | External owner / framework / callback in class atlas | `secret.validPassphrase`, `work.clearSensitive`, `showStep`, `gesture.takeTokens`, `secret.takePassphrase`, `secret.clearSensitiveUi`, `progress`, `work.forge`, `success`, `result.show`, `findViewById`, `performHapticFeedback`, `failure`, `HideActivity.this.message`, `this::progress` |
| `public void success(HideForge.Result value)` | void — no value | forge | `result.show`, `showStep`, `findViewById`, `performHapticFeedback` |
| `public void failure(int message)` | void — no value | forge | `showStep`, `HideActivity.this.message` |
| `private void progress(int label, int done, int total)` | void — no value | forge | `rain.setProgress`, `findViewById`, `text.setText`, `getString`, `bar.isIndeterminate`, `bar.setVisibility`, `bar.setIndeterminate`, `bar.setProgressCompat` |
| `void message(int resource)` | void — no value | onCreate, forge, failure, back, onPause | `isFinishing`, `isDestroyed`, `findViewById`, `notice.setText`, `notice.setVisibility`, `com.example.kineticvault.ui.UiPolish.notice`, `Snackbar.make`, `show` |
| `private void back()` | void — no value | onCreate, getOnBackPressedDispatcher, handleOnBackPressed | `finish`, `MaterialAlertDialogBuilder`, `setTitle`, `setMessage`, `setNegativeButton`, `setPositiveButton`, `work.clearSensitive`, `showStep`, `message`, `show` |
| `private void hideKeyboard()` | void — no value | showStep | `getSystemService`, `keyboard.hideSoftInputFromWindow`, `findViewById`, `getWindowToken`, `getCurrentFocus`, `focused.clearFocus` |
| `@Override protected void onResume()` | void — no value | External owner / framework / callback in class atlas | `super.onResume`, `cover.resume`, `com.example.kineticvault.data.VaultPreferences.current`, `getWindow`, `addFlags`, `clearFlags`, `secret.activate` |
| `boolean isForeground()` | boolean — `resumed` | External owner / framework / callback in class atlas | No further method call in body |
| `@Override protected void onPause()` | void — no value | External owner / framework / callback in class atlas | `result.pause`, `gesture.reset`, `rain.setRunning`, `secret.deactivate`, `work.clearSensitive`, `showStep`, `secret.clearSensitiveUi`, `message`, `super.onPause` |
| `@Override protected void onStop()` | void — no value | External owner / framework / callback in class atlas | `secret.clearSensitiveUi`, `super.onStop` |
| `@Override protected void onDestroy()` | void — no value | External owner / framework / callback in class atlas | `cover.close`, `secret.deactivate`, `secret.close`, `result.close`, `work.close`, `super.onDestroy` |

### app/src/main/java/com/example/kineticvault/ui/hide/HideExporter.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private HideExporter()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `static boolean saveGallery(Context context, File source) throws IOException` | boolean — `recordExport(context, source)` | External owner / framework / callback in class atlas | `IOException`, `context.getContentResolver`, `ContentValues`, `values.put`, `source.getName`, `resolver.insert`, `copy`, `values.clear`, `resolver.update`, `recordExport`, `resolver.delete` |
| `static boolean saveDocument(Context context, File source, Uri destination) throws IOException` | boolean — `recordExport(context, source)` | External owner / framework / callback in class atlas | `copy`, `context.getContentResolver`, `recordExport` |
| `private static boolean recordExport(Context context, File source)` | boolean — `true`; `false` | saveGallery, saveDocument | `VaultLogger.logExport` |
| `static Uri shareUri(Context context, File source) throws IOException` | Uri — `FileProvider.getUriForFile(context, context.getPackageName() + ".files", source)` | External owner / framework / callback in class atlas | `source.isFile`, `IOException`, `FileProvider.getUriForFile`, `context.getPackageName` |
| `private static void copy(ContentResolver resolver, File source, Uri destination) throws IOException` | void — no value | saveGallery, saveDocument | `IOException`, `FileInputStream`, `resolver.openOutputStream`, `input.read`, `output.write`, `output.flush` |

### app/src/main/java/com/example/kineticvault/ui/hide/HideForge.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `Result(File file, Bitmap before, Bitmap after, ImageStats stats, DiffMapData visuals)` | New containing-class object | finish | No further method call in body |
| `private HideForge()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `static Result run(Context context, Bitmap cover, PayloadCodec.Payload payload, byte[] tokens, char[] passphrase, boolean compress, boolean ghost, HideWork.Progress progress, Runnable guard) throws Exception` | Result — `finish(context, cover, bitmap, original, encoded, progress, guard, payload.type, payload.data.length, ghost, passphrase.length > 0)` | finish | `guard.run`, `SecretInput.ensureRoom`, `ImageUtils.checkMemory`, `cover.getWidth`, `cover.getHeight`, `ImageUtils.pixels`, `VaultEngine.hide`, `progress.update`, `HideWork.stageLabel`, `ImageUtils.fromPixels`, `finish`, `bitmap.recycle` |
| `private static Result finish(Context context, Bitmap cover, Bitmap encoded, int[] original, int[] pixels, HideWork.Progress progress, Runnable guard, int type, long bytes, boolean ghost, boolean usesPassphrase) throws IOException` | Result — `new Result(file, before, after, stats, visuals)` | run | `File`, `context.getCacheDir`, `directory.isDirectory`, `directory.mkdirs`, `IOException`, `UUID.randomUUID`, `progress.update`, `ImageUtils.savePng`, `guard.run`, `ImageUtils.statistics`, `ImageUtils.thumbnail`, `DiffMapData.build`, `cover.getWidth`, `cover.getHeight`, `VaultLogger.logHide`, `Result`, `file.delete`, `visuals.close`, `before.recycle`, `after.recycle` |

### app/src/main/java/com/example/kineticvault/ui/hide/HideStore.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `CoverInfo(Bitmap preview, int width, int height)` | New containing-class object | adopt | `StegoCore.capacityBytes` |
| `Capacity(int needed, int available)` | New containing-class object | measure | No further method call in body |
| `boolean fits()` | boolean — `needed <= available` | measure | No further method call in body |
| `HideStore(Context context)` | New containing-class object | External owner / framework / callback in class atlas | `context.getApplicationContext` |
| `CoverInfo load(Uri uri) throws IOException` | CoverInfo — `adopt(ImageUtils.decodeCover(ImageUtils.source(context.getContentResolver(), uri), true))` | External owner / framework / callback in class atlas | `clearCover`, `adopt`, `ImageUtils.decodeCover`, `ImageUtils.source`, `context.getContentResolver` |
| `CoverInfo generate() throws IOException` | CoverInfo — `adopt(CoverGenerator.generate(1024, 1024, System.nanoTime()))` | External owner / framework / callback in class atlas | `clearCover`, `adopt`, `CoverGenerator.generate`, `System.nanoTime` |
| `private CoverInfo adopt(Bitmap bitmap) throws IOException` | CoverInfo — `new CoverInfo(thumbnail, bitmap.getWidth(), bitmap.getHeight())` | load, generate, finishCapture | `ImageUtils.thumbnail`, `CoverInfo`, `bitmap.getWidth`, `bitmap.getHeight`, `bitmap.recycle` |
| `Uri prepareCapture() throws IOException` | Uri — `FileProvider.getUriForFile(context, context.getPackageName() + ".files", capture)` | External owner / framework / callback in class atlas | `deleteCapture`, `File`, `context.getCacheDir`, `directory.isDirectory`, `directory.mkdirs`, `IOException`, `UUID.randomUUID`, `capture.createNewFile`, `FileProvider.getUriForFile`, `context.getPackageName` |
| `CoverInfo finishCapture() throws IOException` | CoverInfo — `adopt(ImageUtils.decodeCover(ImageUtils.source(capture), true))` | External owner / framework / callback in class atlas | `IOException`, `clearCover`, `adopt`, `ImageUtils.decodeCover`, `ImageUtils.source`, `deleteCapture` |
| `void deleteCapture()` | void — no value | prepareCapture, finishCapture, close | `capture.delete` |
| `Capacity measure(SecretInput input, boolean keep) throws IOException` | Capacity — `result` | External owner / framework / callback in class atlas | `IOException`, `clearPrepared`, `input.read`, `context.getContentResolver`, `PayloadCodec.pack`, `GzipCodec.compress`, `Capacity`, `StegoCore.capacityBytes`, `cover.getWidth`, `cover.getHeight`, `result.fits`, `payload.close`, `Arrays.fill` |
| `HideForge.Result forge(byte[] tokens, char[] passphrase, HideWork.Progress progress, Runnable guard) throws Exception` | HideForge.Result — `result` | External owner / framework / callback in class atlas | `IOException`, `releaseRendered`, `HideForge.run`, `clearPrepared` |
| `void clearPrepared()` | void — no value | measure, forge, clearCover | `prepared.close` |
| `private void clearCover()` | void — no value | load, generate, finishCapture, close | `clearPrepared`, `cover.recycle`, `coverPreview.recycle` |
| `Uri proofUri() throws IOException` | Uri — `HideExporter.shareUri(context, proof)` | External owner / framework / callback in class atlas | `ProofSheet.create`, `HideExporter.shareUri` |
| `private void releaseRendered()` | void — no value | forge, close | `proof.delete`, `rendered.before.recycle`, `rendered.after.recycle`, `rendered.visuals.close` |
| `@Override public void close()` | void — no value | measure, clearPrepared, releaseRendered | `clearCover`, `deleteCapture`, `releaseRendered` |

### app/src/main/java/com/example/kineticvault/ui/hide/HideWork.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `HideWork(Context context)` | New containing-class object | External owner / framework / callback in class atlas | `HideStore` |
| `<T> void run(Callable<T> action, Callback<T> callback)` | void — no value | run, measure, forge | `run` |
| `private <T> void run(Callable<T> action, Runnable cleanup, Callback<T> callback)` | void — no value | run, measure, forge | `cleanup.run`, `worker.execute`, `action.call`, `main.post`, `callback.success`, `fail` |
| `void measure(SecretInput input, boolean keep, Callback<HideStore.Capacity> callback)` | void — no value | External owner / framework / callback in class atlas | `secretGeneration.get`, `run`, `guard`, `store.measure`, `store.clearPrepared`, `input::close` |
| `void forge(byte[] tokens, char[] passphrase, Progress progress, Callback<HideForge.Result> callback)` | void — no value | External owner / framework / callback in class atlas | `secretGeneration.get`, `run`, `guard`, `store.forge`, `main.post`, `progress.update`, `Arrays.fill`, `success`, `callback.success`, `failure`, `callback.failure` |
| `public void success(HideForge.Result result)` | void — no value | run, forge | `secretGeneration.get`, `callback.success` |
| `public void failure(int message)` | void — no value | forge, fail | `secretGeneration.get`, `callback.failure` |
| `void clearSensitive()` | void — no value | External owner / framework / callback in class atlas | `secretGeneration.incrementAndGet`, `worker.execute`, `store::clearPrepared` |
| `private void guard(int generation)` | void — no value | measure, forge | `secretGeneration.get`, `CancellationException` |
| `private <T> void fail(Callback<T> callback, int message)` | void — no value | run | `main.post`, `callback.failure` |
| `static int stageLabel(WorkProgress.Stage stage)` | int — `R.string.hide_stage_pack`; `R.string.hide_stage_compress`; `R.string.hide_stage_key` | External owner / framework / callback in class atlas | No further method call in body |
| `@Override public void close()` | void — no value | measure | `secretGeneration.incrementAndGet`, `worker.execute`, `worker.shutdown`, `store::close` |

### app/src/main/java/com/example/kineticvault/ui/hide/ProofSheet.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private ProofSheet()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `static File create(Context context, HideForge.Result result) throws IOException` | File — `file` | External owner / framework / callback in class atlas | `ImageUtils.requireWorker`, `IOException`, `File`, `context.getCacheDir`, `directory.isDirectory`, `directory.mkdirs`, `UUID.randomUUID`, `Bitmap.createBitmap`, `Canvas`, `canvas.drawColor`, `Color.rgb`, `Paint`, `paint.setColor`, `context.getColor`, `paint.setTextSize`, `canvas.drawText`, `context.getString`, `panel`, `density`, `ImageUtils.savePng`, `sheet.recycle`, `file.delete` |
| `private static void panel(Canvas canvas, Paint paint, Bitmap source, int left, String title)` | void — no value | create | `canvas.drawText`, `source.getWidth`, `source.getHeight`, `source.getPixels`, `Bitmap.createBitmap`, `Math.min`, `clean.getWidth`, `clean.getHeight`, `canvas.drawBitmap`, `RectF`, `Arrays.fill`, `clean.recycle` |
| `private static void density(Canvas canvas, Paint paint, Bitmap map, int color)` | void — no value | create | `map.getWidth`, `map.getHeight`, `map.getPixels`, `paint.setColor`, `paint.setAlpha`, `canvas.drawRect`, `Arrays.fill` |

### app/src/main/java/com/example/kineticvault/ui/hide/ResultStep.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `ResultStep(HideActivity host, HideWork work)` | New containing-class object | External owner / framework / callback in class atlas | `host.findViewById`, `host.registerForActivityResult`, `ActivityResultContracts.CreateDocument`, `busy`, `save`, `HideExporter.saveDocument`, `bindLoupe`, `setOnClickListener`, `startSweep`, `host.startActivity`, `Intent`, `exportOptions`, `warnShare`, `host.finish` |
| `void show(HideForge.Result result)` | void — no value | exportOptions, warnShare, warnProof | `comparison.setImages`, `host.findViewById`, `diff.setData`, `diff.startSweep`, `setPlane`, `DiffStats.show`, `busy` |
| `void pause()` | void — no value | External owner / framework / callback in class atlas | `sheet.dismiss`, `host.findViewById`, `stopSweep` |
| `void close()` | void — no value | External owner / framework / callback in class atlas | `comparison.setImages`, `host.findViewById`, `setData`, `setPlane` |
| `private void exportOptions()` | void — no value | ResultStep | `sheet.dismiss`, `com.example.kineticvault.ui.ExportSheet.show`, `this::savePng`, `this::saveDocument`, `this::warnShare`, `this::warnProof`, `this::saveDocument`, `this::warnShare`, `this::warnProof` |
| `private void saveDocument()` | void — no value | ResultStep, exportOptions | `busy`, `document.launch`, `host.getString`, `host.message` |
| `private void savePng()` | void — no value | exportOptions | `busy`, `save`, `HideExporter.saveGallery`, `document.launch`, `host.getString`, `host.message` |
| `private void save(Callable<Boolean> action)` | void — no value | ResultStep, savePng | `busy`, `work.run`, `success`, `host.message`, `failure` |
| `public void success(Boolean logged)` | void — no value | save, prepareShare, logShare | `busy`, `host.message` |
| `public void failure(int message)` | void — no value | save, prepareShare, logShare | `busy`, `host.message` |
| `private void warnShare()` | void — no value | ResultStep, exportOptions | `MaterialAlertDialogBuilder`, `setTitle`, `setMessage`, `setNegativeButton`, `setPositiveButton`, `prepareShare`, `show` |
| `private void warnProof()` | void — no value | exportOptions | `MaterialAlertDialogBuilder`, `setTitle`, `setMessage`, `setNegativeButton`, `setPositiveButton`, `prepareShare`, `show` |
| `private void prepareShare()` | void — no value | warnShare, warnProof | `prepareShare` |
| `private void prepareShare(boolean proof)` | void — no value | warnShare, warnProof, prepareShare | `busy`, `work.run`, `work.store.proofUri`, `HideExporter.shareUri`, `success`, `Intent`, `setType`, `putExtra`, `addFlags`, `intent.setClipData`, `ClipData.newRawUri`, `host.getString`, `host.startActivity`, `Intent.createChooser`, `logShare`, `host.message`, `failure` |
| `public void success(Uri uri)` | void — no value | save, prepareShare, logShare | `busy`, `Intent`, `setType`, `putExtra`, `addFlags`, `intent.setClipData`, `ClipData.newRawUri`, `host.getString`, `host.startActivity`, `Intent.createChooser`, `logShare`, `host.message` |
| `public void failure(int message)` | void — no value | save, prepareShare, logShare | `busy`, `host.message` |
| `private void logShare()` | void — no value | prepareShare, success | `work.run`, `VaultLogger.logExport`, `success`, `failure`, `host.message` |
| `public void success(Boolean ignored)` | void — no value | save, prepareShare, logShare | No further method call in body |
| `public void failure(int message)` | void — no value | save, prepareShare, logShare | `host.message` |
| `private void busy(boolean value)` | void — no value | ResultStep, show, saveDocument, savePng, save, success, failure, prepareShare | `host.findViewById`, `setEnabled` |

### app/src/main/java/com/example/kineticvault/ui/hide/SecretInput.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public SecretInput(char[] text, Uri file, boolean compress, boolean ghost)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public PayloadCodec.Payload read(ContentResolver resolver) throws IOException` | PayloadCodec.Payload — `payload` | readFile | `filename`, `encodeText`, `readFile`, `ensureRoom`, `PayloadCodec.Payload`, `Arrays.fill` |
| `private byte[] encodeText() throws IOException` | byte[] — `result` | read | `IOException`, `ensureRoom`, `StandardCharsets.UTF_8.newEncoder`, `encode`, `CharBuffer.wrap`, `encoded.remaining`, `encoded.get`, `encoded.hasArray`, `Arrays.fill`, `encoded.array` |
| `private byte[] readFile(ContentResolver resolver) throws IOException` | byte[] — `output.toByteArray()` | read | `resolver.openInputStream`, `WipingBuffer`, `IOException`, `input.read`, `output.size`, `ensureRoom`, `output.write`, `output.toByteArray`, `Arrays.fill` |
| `private String filename(ContentResolver resolver) throws IOException` | String — `name` | read | `resolver.query`, `cursor.moveToFirst`, `cursor.getString`, `name.isEmpty`, `name.replaceAll`, `name.equals`, `name.getBytes`, `IOException` |
| `static void ensureRoom(long bytes) throws IOException` | void — no value | read, encodeText, readFile | `Runtime.getRuntime`, `runtime.maxMemory`, `runtime.totalMemory`, `runtime.freeMemory`, `IOException` |
| `@Override public void close()` | void — no value | External owner / framework / callback in class atlas | `Arrays.fill` |
| `@Override public synchronized void write(byte[] bytes, int offset, int length)` | void — no value | readFile | `super.write`, `Arrays.fill` |
| `@Override public void close()` | void — no value | External owner / framework / callback in class atlas | `Arrays.fill`, `reset` |

### app/src/main/java/com/example/kineticvault/ui/hide/SecretStep.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `SecretStep(HideActivity host, HideWork work)` | New containing-class object | External owner / framework / callback in class atlas | `host.findViewById`, `text.setFilters`, `InputFilter.LengthFilter`, `passphrase.setFilters`, `host.registerForActivityResult`, `ActivityResultContracts.OpenDocument`, `setText`, `schedule`, `TextImport`, `importer.read`, `type.check`, `text.setText`, `setOnClickListener`, `textPicker.launch`, `host.message`, `bind` |
| `private void bind()` | void — no value | SecretStep | `text.addTextChangedListener`, `TextWatcher`, `beforeTextChanged`, `onTextChanged`, `afterTextChanged`, `schedule`, `type.setOnCheckedChangeListener`, `text.setVisibility`, `isFile`, `host.findViewById`, `setVisibility`, `text.setText`, `compression.setOnCheckedChangeListener`, `ghost.setOnCheckedChangeListener`, `usePassphrase.setOnCheckedChangeListener`, `passphrase.setVisibility`, `passphrase.setText`, `setOnClickListener`, `picker.launch`, `host.message`, `prepare` |
| `public void beforeTextChanged(CharSequence s, int start, int count, int after)` | void — no value | bind | No further method call in body |
| `public void onTextChanged(CharSequence s, int start, int before, int count)` | void — no value | bind | No further method call in body |
| `public void afterTextChanged(Editable value)` | void — no value | bind | `schedule` |
| `void activate()` | void — no value | External owner / framework / callback in class atlas | `setBusy`, `schedule` |
| `void deactivate()` | void — no value | External owner / framework / callback in class atlas | `main.removeCallbacks`, `setBusy` |
| `private boolean isFile()` | boolean — `type.getCheckedRadioButtonId() == R.id.hide_type_file` | bind, hasInput, snapshot | `type.getCheckedRadioButtonId` |
| `private boolean hasInput()` | boolean — `isFile() ? file != null : text.length() > 0` | schedule, measure, prepare | `isFile`, `text.length` |
| `private void schedule()` | void — no value | SecretStep, bind, afterTextChanged, activate | `host.findViewById`, `setEnabled`, `main.removeCallbacks`, `capacity.setText`, `hasInput`, `meter.setProgressCompat`, `main.postDelayed` |
| `private SecretInput snapshot()` | SecretInput — `new SecretInput(isFile() ? null : copy(text), isFile() ? file : null, compression.isChecked(), ghost.isChecked())` | measure, prepare | `SecretInput`, `isFile`, `copy`, `compression.isChecked`, `ghost.isChecked` |
| `private void measure()` | void — no value | prepare | `hasInput`, `work.measure`, `snapshot`, `success`, `showCapacity`, `failure`, `capacity.setText` |
| `public void success(HideStore.Capacity result)` | void — no value | measure, prepare | `showCapacity` |
| `public void failure(int message)` | void — no value | measure, prepare | `capacity.setText` |
| `private void showCapacity(HideStore.Capacity result)` | void — no value | measure, success, prepare | `result.fits`, `capacity.setText`, `host.getString`, `meter.setIndicatorColor`, `host.getColor`, `meter.setProgressCompat`, `Math.min`, `host.findViewById`, `setEnabled` |
| `private void prepare()` | void — no value | bind | `hasInput`, `validPassphrase`, `main.removeCallbacks`, `setBusy`, `capacity.setText`, `work.measure`, `snapshot`, `success`, `showCapacity`, `result.fits`, `host.showStep`, `failure`, `host.message` |
| `public void success(HideStore.Capacity result)` | void — no value | measure, prepare | `setBusy`, `showCapacity`, `result.fits`, `host.showStep` |
| `public void failure(int message)` | void — no value | measure, prepare | `setBusy`, `capacity.setText`, `host.message` |
| `boolean validPassphrase()` | boolean — `false`; `true` | prepare | `usePassphrase.isChecked`, `passphrase.length`, `passphrase.setError`, `host.getString`, `host.message` |
| `boolean hasPassphrase()` | boolean — `usePassphrase.isChecked() && passphrase.length() > 0` | External owner / framework / callback in class atlas | `usePassphrase.isChecked`, `passphrase.length` |
| `char[] takePassphrase()` | char[] — `result` | External owner / framework / callback in class atlas | `usePassphrase.isChecked`, `copy`, `passphrase.setText` |
| `void close()` | void — no value | External owner / framework / callback in class atlas | `importer.close` |
| `void clearSensitiveUi()` | void — no value | External owner / framework / callback in class atlas | `importer.cancel`, `text.setText`, `passphrase.setText` |
| `private static char[] copy(EditText view)` | char[] — `result` | snapshot, takePassphrase | `view.length`, `view.getText`, `getChars` |
| `private void setBusy(boolean value)` | void — no value | activate, deactivate, prepare, success, failure | `host.findViewById`, `setEnabled` |

### app/src/main/java/com/example/kineticvault/ui/hide/TextImport.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `TextImport(HideActivity host)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `void read(Uri uri, java.util.function.Consumer<char[]> done)` | void — no value | External owner / framework / callback in class atlas | `worker.execute`, `host.getContentResolver`, `openInputStream`, `java.io.IOException`, `InputStreamReader`, `StandardCharsets.UTF_8.newDecoder`, `onMalformedInput`, `onUnmappableCharacter`, `reader.read`, `Arrays.copyOf`, `Math.min`, `Arrays.fill`, `main.post`, `host.isDestroyed`, `done.accept`, `host.message` |
| `void cancel()` | void — no value | External owner / framework / callback in class atlas | No further method call in body |
| `@Override public void close()` | void — no value | External owner / framework / callback in class atlas | `worker.shutdown` |

### app/src/main/java/com/example/kineticvault/ui/HomeActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override protected void onCreate(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `EdgeToEdge.enable`, `setContentView`, `findViewById`, `getResources`, `getDimensionPixelSize`, `ViewCompat.setOnApplyWindowInsetsListener`, `insets.getInsets`, `WindowInsetsCompat.Type.systemBars`, `WindowInsetsCompat.Type.displayCutout`, `view.setPadding`, `ViewCompat.requestApplyInsets`, `setOnClickListener`, `startActivity`, `Intent`, `com.example.kineticvault.data.SyncManager.get`, `configurationChanged`, `tile.setAlpha`, `tile.setTranslationY`, `tile.animate`, `alpha`, `translationY`, `setStartDelay`, `setDuration`, `start`, `this::overflow` |
| `private void overflow(View anchor)` | void — no value | onCreate | `androidx.appcompat.widget.PopupMenu`, `menu.getMenu`, `add`, `menu.setOnMenuItemClickListener`, `item.getItemId`, `startActivity`, `Intent`, `menu.show` |
| `private void showLedger(com.example.kineticvault.data.SyncManager.Status status)` | void — no value | External owner / framework / callback in class atlas | `findViewById`, `chip.setText`, `counts.setText`, `getString`, `queue.setText`, `setVisibility`, `setEnabled` |
| `@Override protected void onStart()` | void — no value | External owner / framework / callback in class atlas | `super.onStart`, `com.example.kineticvault.data.SyncManager.get`, `addListener` |
| `@Override protected void onStop()` | void — no value | External owner / framework / callback in class atlas | `com.example.kineticvault.data.SyncManager.get`, `removeListener`, `super.onStop` |
| `@Override protected void onResume()` | void — no value | External owner / framework / callback in class atlas | `super.onResume`, `findViewById`, `android.animation.ValueAnimator.ofFloat`, `pulse.setDuration`, `pulse.setRepeatCount`, `pulse.setRepeatMode`, `pulse.addUpdateListener`, `a.getAnimatedValue`, `hero.setScaleX`, `hero.setScaleY`, `pulse.start` |
| `@Override protected void onPause()` | void — no value | External owner / framework / callback in class atlas | `pulse.cancel`, `super.onPause` |

### app/src/main/java/com/example/kineticvault/ui/InfoActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `protected void showPage(int layout)` | void — no value | External owner / framework / callback in class atlas | `EdgeToEdge.enable`, `setContentView`, `findViewById`, `getResources`, `getDimensionPixelSize`, `ViewCompat.setOnApplyWindowInsetsListener`, `insets.getInsets`, `WindowInsetsCompat.Type.systemBars`, `WindowInsetsCompat.Type.displayCutout`, `WindowInsetsCompat.Type.ime`, `view.setPadding`, `ViewCompat.requestApplyInsets`, `setOnClickListener`, `finish` |

### app/src/main/java/com/example/kineticvault/ui/KineticOrbView.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public KineticOrbView(Context context)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public KineticOrbView(Context context, AttributeSet attrs)` | New containing-class object | External owner / framework / callback in class atlas | `setContentDescription`, `context.getString`, `setImportantForAccessibility` |
| `public void setAngles(float pitch, float roll)` | void — no value | External owner / framework / callback in class atlas | `Math.max`, `Math.min` |
| `public void setTokenCount(int count, boolean haptic)` | void — no value | reset | `Math.max`, `Math.min`, `performHapticFeedback`, `setContentDescription`, `getContext`, `getString`, `invalidate` |
| `public void setAnimating(boolean running)` | void — no value | onDetachedFromWindow | `postInvalidateOnAnimation` |
| `public void reset()` | void — no value | onDetachedFromWindow | `Arrays.fill`, `setTokenCount` |
| `@Override protected void onDraw(Canvas canvas)` | void — no value | External owner / framework / callback in class atlas | `super.onDraw`, `Math.min`, `getWidth`, `getHeight`, `drawDial`, `drawOrb`, `isShown`, `getWindowVisibility`, `postInvalidateOnAnimation` |
| `private void drawDial(Canvas canvas, float cx, float cy, float radius)` | void — no value | onDraw | `dial.set`, `paint.setStyle`, `paint.setStrokeWidth`, `paint.setStrokeCap`, `paint.setColor`, `getContext`, `getColor`, `paint.setAlpha`, `canvas.drawArc` |
| `private void drawOrb(Canvas canvas, float cx, float cy, float radius)` | void — no value | onDraw | `paint.setColor`, `getContext`, `getColor`, `paint.setAlpha`, `canvas.drawCircle` |
| `@Override protected void onDetachedFromWindow()` | void — no value | External owner / framework / callback in class atlas | `setAnimating`, `reset`, `super.onDetachedFromWindow` |

### app/src/main/java/com/example/kineticvault/ui/LearnActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override protected void onCreate(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `showPage`, `findViewById`, `web.getSettings`, `settings.setAllowFileAccess`, `settings.setAllowContentAccess`, `settings.setMixedContentMode`, `web.setWebViewClient`, `WebViewClient`, `shouldOverrideUrlLoading`, `request.getUrl`, `request.isForMainFrame`, `equals`, `uri.getScheme`, `uri.getHost`, `setJavaScriptEnabled`, `onReceivedError`, `showError`, `onReceivedHttpError`, `setOnClickListener`, `load`, `state.getBoolean` |
| `@Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request)` | boolean — `false`; `true`; `false` | onCreate | `request.getUrl`, `request.isForMainFrame`, `equals`, `uri.getScheme`, `uri.getHost`, `web.getSettings`, `setJavaScriptEnabled` |
| `@Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error)` | void — no value | onCreate | `request.isForMainFrame`, `showError` |
| `@Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response)` | void — no value | onCreate | `request.isForMainFrame`, `showError` |
| `private void load(boolean online)` | void — no value | onCreate | `web.stopLoading`, `findViewById`, `setVisibility`, `web.setVisibility`, `web.getSettings`, `setJavaScriptEnabled`, `web.loadUrl`, `getString` |
| `private void showError()` | void — no value | onCreate, onReceivedError, onReceivedHttpError | `web.stopLoading`, `web.setVisibility`, `findViewById`, `setVisibility`, `UiPolish.failure` |
| `@Override protected void onSaveInstanceState(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onSaveInstanceState`, `state.putBoolean` |
| `@Override protected void onPause()` | void — no value | External owner / framework / callback in class atlas | `web.onPause`, `super.onPause` |
| `@Override protected void onResume()` | void — no value | External owner / framework / callback in class atlas | `super.onResume`, `web.onResume` |
| `@Override protected void onDestroy()` | void — no value | External owner / framework / callback in class atlas | `web.stopLoading`, `web.setWebViewClient`, `WebViewClient`, `web.getParent`, `removeView`, `web.destroy`, `super.onDestroy` |

### app/src/main/java/com/example/kineticvault/ui/MatrixRainView.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public MatrixRainView(Context context)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public MatrixRainView(Context context, AttributeSet attrs)` | New containing-class object | External owner / framework / callback in class atlas | `setImportantForAccessibility`, `random.nextFloat` |
| `public void setProgress(int completed, int total)` | void — no value | External owner / framework / callback in class atlas | `Math.max`, `Math.min`, `invalidate` |
| `public void setRunning(boolean value)` | void — no value | onDetachedFromWindow | `postInvalidateOnAnimation` |
| `@Override protected void onDraw(Canvas canvas)` | void — no value | External owner / framework / callback in class atlas | `super.onDraw`, `Math.max`, `getWidth`, `SystemClock.uptimeMillis`, `Math.min`, `paint.setTypeface`, `paint.setTextSize`, `getHeight`, `random.nextFloat`, `paint.setColor`, `getContext`, `getColor`, `paint.setAlpha`, `canvas.drawText`, `isShown`, `getWindowVisibility`, `postInvalidateOnAnimation` |
| `@Override protected void onDetachedFromWindow()` | void — no value | External owner / framework / callback in class atlas | `setRunning`, `super.onDetachedFromWindow` |

### app/src/main/java/com/example/kineticvault/ui/NerdOverlay.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public NerdOverlay(TextView view)` | New containing-class object | External owner / framework / callback in class atlas | `view.setSaveEnabled`, `clear` |
| `public void clear()` | void — no value | NerdOverlay, begin | `java.util.Arrays.fill`, `view.setVisibility`, `VaultPreferences.current`, `view.setText` |
| `public void begin(boolean pad)` | void — no value | External owner / framework / callback in class atlas | `clear`, `view.setText` |
| `public void token(byte token)` | void — no value | External owner / framework / callback in class atlas | `VaultPreferences.current`, `System.arraycopy`, `render` |
| `@Override public void onSample(float ax, float ay, float az, float gx, float gy, float gz, float pitch, float roll, SensorFusionManager.Mode mode)` | void — no value | External owner / framework / callback in class atlas | `VaultPreferences.current`, `SystemClock.uptimeMillis`, `view.getContext`, `getString`, `render` |
| `private void render()` | void — no value | token, onSample | `StringBuilder`, `names.append`, `view.getContext`, `getString`, `view.setText`, `sample.isEmpty`, `names.length` |

### app/src/main/java/com/example/kineticvault/ui/reveal/RevealActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override public void onReceive(Context context, Intent intent)` | void — no value | External owner / framework / callback in class atlas | `Intent.ACTION_SCREEN_OFF.equals`, `intent.getAction`, `com.example.kineticvault.data.VaultPreferences.current`, `lock`, `work.discardExport`, `finish` |
| `public void success(Boolean logged)` | void — no value | load, unlock, export, success | `message` |
| `public void failure(int message)` | void — no value | load, unlock, export, success | `RevealActivity.this.message` |
| `public void success(Boolean logged)` | void — no value | load, unlock, export, success | `message` |
| `public void failure(int message)` | void — no value | load, unlock, export, success | `RevealActivity.this.message` |
| `@Override protected void onCreate(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `getWindow`, `addFlags`, `EdgeToEdge.enable`, `setContentView`, `findViewById`, `setSaveFromParentEnabled`, `RevealWork`, `getApplicationContext`, `TextScrambler`, `RevealGesture`, `installInsets`, `bind`, `state.getString`, `Uri.parse`, `getIntent`, `getData`, `IntentCompat.getParcelableExtra`, `getClipData`, `getItemCount`, `getItemAt`, `getUri`, `load` |
| `private void bind()` | void — no value | onCreate | `findViewById`, `setOnClickListener`, `lock`, `picker.launch`, `message`, `finish`, `unlock`, `exportOptions`, `MaterialAlertDialogBuilder`, `setTitle`, `setMessage`, `setNegativeButton`, `setPositiveButton`, `export`, `show` |
| `private void installInsets()` | void — no value | onCreate | `findViewById`, `getResources`, `getDimensionPixelSize`, `ViewCompat.setOnApplyWindowInsetsListener`, `insets.getInsets`, `WindowInsetsCompat.Type.systemBars`, `WindowInsetsCompat.Type.displayCutout`, `WindowInsetsCompat.Type.ime`, `view.setPadding`, `ViewCompat.requestApplyInsets` |
| `private void load(Uri uri)` | void — no value | onCreate | `lock`, `findViewById`, `setText`, `getString`, `setBusy`, `message`, `work.load`, `success`, `setVisibility`, `password.setVisibility`, `value.usesPassphrase`, `failure`, `RevealActivity.this.message` |
| `public void success(VaultHeader value)` | void — no value | load, unlock, export, success | `setBusy`, `findViewById`, `setVisibility`, `password.setVisibility`, `value.usesPassphrase`, `message` |
| `public void failure(int message)` | void — no value | load, unlock, export, success | `setBusy`, `RevealActivity.this.message` |
| `private void unlock()` | void — no value | bind | `header.usesPassphrase`, `password.length`, `message`, `gesture.takeTokens`, `password.getText`, `getChars`, `clear`, `gesture.clear`, `setBusy`, `work.unlock`, `success`, `unlocked`, `failure`, `denied`, `this::progress` |
| `public void success(Boolean value)` | void — no value | load, unlock, export, success | `unlocked`, `message` |
| `public void failure(int message)` | void — no value | load, unlock, export, success | `denied` |
| `private void unlocked()` | void — no value | unlock, success | `setBusy`, `findViewById`, `setVisibility`, `message`, `door.performHapticFeedback`, `door.open`, `work.hasFile`, `setText`, `work.takeText`, `scrambler.show` |
| `private void denied(int message)` | void — no value | unlock, failure | `setBusy`, `findViewById`, `setText`, `getString`, `gesture.clear`, `message`, `door.deny`, `door.startAnimation`, `AnimationUtils.loadAnimation`, `door.performHapticFeedback` |
| `private void progress(WorkProgress.Stage stage, int done, int total)` | void — no value | unlock | `findViewById`, `bar.setIndeterminate`, `bar.setProgress`, `setText`, `getString` |
| `private void exportOptions()` | void — no value | bind | `work.hasPayload`, `sheet.dismiss`, `work.hasFile`, `export`, `findViewById`, `performClick`, `com.example.kineticvault.ui.ExportSheet.show` |
| `private void cancelledExport()` | void — no value | External owner / framework / callback in class atlas | `work.discardExport`, `message` |
| `private void export(boolean open)` | void — no value | bind, exportOptions | `work.hasPayload`, `setBusy`, `message`, `work.prepareExport`, `success`, `equals`, `textSaver.launch`, `saver.launch`, `Intent`, `setDataAndType`, `addFlags`, `intent.setClipData`, `ClipData.newRawUri`, `getString`, `viewer.launch`, `work.opened`, `failure`, `RevealActivity.this.message`, `work.discardExport` |
| `public void success(RevealExport value)` | void — no value | load, unlock, export | `setBusy`, `equals`, `textSaver.launch`, `saver.launch`, `Intent`, `setDataAndType`, `addFlags`, `intent.setClipData`, `ClipData.newRawUri`, `getString`, `viewer.launch`, `work.opened`, `success`, `failure`, `RevealActivity.this.message`, `work.discardExport`, `message` |
| `public void success(Boolean ignored)` | void — no value | load, unlock, export, success | No further method call in body |
| `public void failure(int message)` | void — no value | load, unlock, export, success | `RevealActivity.this.message` |
| `public void failure(int message)` | void — no value | load, unlock, export, success | `setBusy`, `RevealActivity.this.message` |
| `private void setBusy(boolean value)` | void — no value | load, success, failure, unlock, unlocked, denied, export, lock | `findViewById`, `setVisibility`, `setIndeterminate`, `setEnabled` |
| `void message(int resource)` | void — no value | success, failure, bind, load, unlock, unlocked, denied, cancelledExport, export, lock | `isFinishing`, `findViewById`, `notice.setText`, `com.example.kineticvault.ui.UiPolish.notice` |
| `private void lock()` | void — no value | onReceive, bind, load, onPause, onStop | `work.autoLock`, `gesture.clear`, `password.getText`, `clear`, `scrambler.clear`, `door.reset`, `work.clearSensitive`, `setBusy`, `work.discardExport`, `findViewById`, `setVisibility`, `message` |
| `@Override protected void onStart()` | void — no value | External owner / framework / callback in class atlas | `super.onStart`, `ContextCompat.registerReceiver`, `IntentFilter`, `work.protection`, `com.example.kineticvault.data.VaultPreferences.current`, `getWindow`, `addFlags`, `clearFlags` |
| `@Override protected void onPause()` | void — no value | External owner / framework / callback in class atlas | `sheet.dismiss`, `gesture.clear`, `password.getText`, `clear`, `lock`, `super.onPause` |
| `@Override protected void onStop()` | void — no value | External owner / framework / callback in class atlas | `com.example.kineticvault.data.VaultPreferences.current`, `lock`, `getSystemService`, `power.isInteractive`, `work.discardExport`, `finish`, `unregisterReceiver`, `super.onStop` |
| `@Override protected void onSaveInstanceState(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `state.putString`, `selected.toString`, `super.onSaveInstanceState` |
| `@Override protected void onDestroy()` | void — no value | External owner / framework / callback in class atlas | `gesture.clear`, `password.getText`, `clear`, `scrambler.clear`, `door.reset`, `work.close`, `super.onDestroy` |

### app/src/main/java/com/example/kineticvault/ui/reveal/RevealExpiry.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private RevealExpiry()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `static void requireAllowed(Context context, VaultEntry entry) throws IOException` | void — no value | External owner / framework / callback in class atlas | `IOException`, `VaultRepository`, `repository.findByHash`, `System.currentTimeMillis`, `Expired` |

### app/src/main/java/com/example/kineticvault/ui/reveal/RevealExport.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private RevealExport(File file, Uri uri, String name, VaultEntry vault)` | New containing-class object | create | `name.lastIndexOf`, `android.webkit.MimeTypeMap.getSingleton`, `getMimeTypeFromExtension`, `name.substring`, `toLowerCase` |
| `static RevealExport create(Context context, byte[] bytes, String name, VaultEntry vault) throws IOException` | RevealExport — `result` | External owner / framework / callback in class atlas | `VaultRepository.requireWorker`, `cleanupOrphans`, `File`, `context.getCacheDir`, `directory.isDirectory`, `directory.mkdirs`, `IOException`, `UUID.randomUUID`, `toString`, `FileOutputStream`, `output.write`, `FileProvider.getUriForFile`, `context.getPackageName`, `RevealExport`, `file.delete` |
| `static synchronized void cleanupOrphans(Context context) throws IOException` | void — no value | create | `VaultRepository.requireWorker`, `File`, `context.getCacheDir`, `directory.exists`, `directory.listFiles`, `IOException`, `file.isFile`, `file.delete` |
| `void save(Context context, Uri destination) throws IOException` | void — no value | External owner / framework / callback in class atlas | `VaultRepository.requireWorker`, `FileInputStream`, `context.getContentResolver`, `openOutputStream`, `IOException`, `input.read`, `output.write`, `output.flush`, `Arrays.fill` |

### app/src/main/java/com/example/kineticvault/ui/reveal/RevealGesture.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `RevealGesture(RevealActivity host)` | New containing-class object | External owner / framework / callback in class atlas | `host.findViewById`, `com.example.kineticvault.ui.NerdOverlay`, `SensorFusionManager`, `GestureRecognizer.Listener`, `onToken`, `nerd.token`, `session.addToken`, `onMotion`, `session.onMotion`, `sensors.setTelemetryListener`, `pad.setOnTokenListener`, `usePad.setOnCheckedChangeListener`, `clear`, `pad.setVisibility`, `setOnClickListener`, `begin`, `session.done`, `usePad.setChecked`, `com.example.kineticvault.data.VaultPreferences.current`, `usePad.isChecked` |
| `public void onToken(byte token, long time)` | void — no value | RevealGesture | `nerd.token`, `session.addToken` |
| `public void onMotion(float pitch, float roll, boolean still, long time)` | void — no value | RevealGesture | `session.onMotion` |
| `private void begin()` | void — no value | RevealGesture | `clear`, `nerd.begin`, `usePad.isChecked`, `GestureSession`, `GestureSession.Listener`, `onProgress`, `status.setText`, `host.getString`, `status.performHapticFeedback`, `onTooShort`, `host.message`, `onFinished`, `sensors.stop`, `nerd.clear`, `session.takeTokens`, `pad.setEnabled`, `host.findViewById`, `setEnabled`, `sensors.start`, `usePad.setChecked` |
| `public void onProgress(int count)` | void — no value | begin | `status.setText`, `host.getString`, `status.performHapticFeedback` |
| `public void onTooShort(int count)` | void — no value | begin | `host.message` |
| `public void onFinished()` | void — no value | begin | `sensors.stop`, `nerd.clear`, `session.takeTokens`, `pad.setEnabled`, `host.findViewById`, `setEnabled`, `status.setText` |
| `byte[] takeTokens()` | byte[] — `result` | begin, onFinished | No further method call in body |
| `void clear()` | void — no value | RevealGesture, begin, onFinished | `sensors.stop`, `nerd.clear`, `session.close`, `Arrays.fill`, `pad.setEnabled`, `host.findViewById`, `setEnabled`, `status.setText` |

### app/src/main/java/com/example/kineticvault/ui/reveal/RevealLedger.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `RevealLedger(Context context)` | New containing-class object | External owner / framework / callback in class atlas | `context.getApplicationContext` |
| `VaultEntry capture(Uri source, Bitmap image, VaultHeader header) throws IOException` | VaultEntry — `VaultLogger.capture(context, source, image, header.isGhost(), header.usesPassphrase())` | External owner / framework / callback in class atlas | `VaultLogger.capture`, `header.isGhost`, `header.usesPassphrase` |
| `synchronized void adopt(VaultEntry entry)` | void — no value | External owner / framework / callback in class atlas | No further method call in body |
| `synchronized VaultEntry current()` | VaultEntry — `current` | autoLock | No further method call in body |
| `boolean revealed(VaultEntry entry, boolean success, int type, long bytes)` | boolean — `true`; `false` | External owner / framework / callback in class atlas | `VaultLogger.logReveal` |
| `boolean exported(VaultEntry entry)` | boolean — `true`; `false` | External owner / framework / callback in class atlas | `VaultLogger.logExport` |
| `void autoLock(ExecutorService worker)` | void — no value | External owner / framework / callback in class atlas | `current`, `worker.execute`, `VaultLogger.logAutoLock` |

### app/src/main/java/com/example/kineticvault/ui/reveal/RevealWork.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `RevealWork(Context context)` | New containing-class object | External owner / framework / callback in class atlas | `context.getApplicationContext`, `RevealLedger`, `worker.execute`, `RevealExport.cleanupOrphans` |
| `void protection(java.util.function.Consumer<Boolean> callback)` | void — no value | External owner / framework / callback in class atlas | `worker.execute`, `context.getSharedPreferences`, `context.getPackageName`, `getBoolean`, `main.post`, `callback.accept` |
| `void load(Uri uri, Callback<VaultHeader> callback)` | void — no value | External owner / framework / callback in class atlas | `clearSensitive`, `ledger.adopt`, `worker.execute`, `ImageUtils.decodeExact`, `ImageUtils.source`, `context.getContentResolver`, `ImageUtils.pixels`, `StegoCore.readHeader`, `requireCurrent`, `ledger.capture`, `deliver`, `callback.success`, `callback.failure`, `equals`, `error.getMessage`, `bitmap.recycle` |
| `synchronized void unlock(byte[] moves, char[] password, WorkProgress progress, Callback<Boolean> callback)` | void — no value | External owner / framework / callback in class atlas | `ledger.current`, `worker.execute`, `decrypt` |
| `private void decrypt(int ticket, int[] source, byte[] moves, char[] password, WorkProgress progress, Callback<Boolean> callback, VaultEntry entry)` | void — no value | unlock | `requireCurrent`, `RevealExpiry.requireAllowed`, `VaultEngine.reveal`, `deliver`, `progress.onProgress`, `decodeText`, `Arrays.fill`, `CancellationException`, `ledger.revealed`, `callback.success`, `discardResult`, `callback.failure`, `result.close` |
| `private static char[] decodeText(byte[] bytes)` | char[] — `result` | decrypt | `StandardCharsets.UTF_8.decode`, `ByteBuffer.wrap`, `buffer.remaining`, `buffer.get`, `buffer.hasArray`, `Arrays.fill`, `buffer.array` |
| `synchronized char[] takeText()` | char[] — `result` | External owner / framework / callback in class atlas | No further method call in body |
| `synchronized boolean hasPayload()` | boolean — `payload != null` | prepareExport | No further method call in body |
| `synchronized boolean hasFile()` | boolean — `payload != null && payload.type == PayloadCodec.FILE` | prepareExport | No further method call in body |
| `void prepareExport(Callback<RevealExport> callback)` | void — no value | External owner / framework / callback in class atlas | `worker.execute`, `requireCurrent`, `hasPayload`, `IOException`, `payload.data.clone`, `hasFile`, `context.getString`, `RevealExport.create`, `ledger.current`, `deliver`, `callback.success`, `callback.failure`, `Arrays.fill`, `pending.file.delete` |
| `void save(Uri destination, Callback<Boolean> callback)` | void — no value | External owner / framework / callback in class atlas | `worker.execute`, `IOException`, `source.save`, `ledger.exported`, `source.file.delete`, `main.post`, `callback.success`, `callback.failure` |
| `void opened(RevealExport source, Callback<Boolean> callback)` | void — no value | External owner / framework / callback in class atlas | `worker.execute`, `ledger.exported`, `main.post`, `callback.failure` |
| `void autoLock()` | void — no value | External owner / framework / callback in class atlas | `ledger.autoLock` |
| `void discardExport()` | void — no value | close | `worker.execute`, `source.file.delete` |
| `synchronized void clearSensitive()` | void — no value | load, close | `Arrays.fill`, `discardResult` |
| `private synchronized void discardResult(int ticket)` | void — no value | decrypt, clearSensitive | `payload.close`, `Arrays.fill` |
| `private synchronized void requireCurrent(int ticket)` | void — no value | load, decrypt, prepareExport | `CancellationException` |
| `private void deliver(int ticket, Runnable action)` | void — no value | load, decrypt, prepareExport | `main.post`, `action.run` |
| `void close()` | void — no value | decrypt, discardResult | `clearSensitive`, `discardExport`, `worker.shutdown` |

### app/src/main/java/com/example/kineticvault/ui/ScannerActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override protected void onCreate(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `showPage`, `findViewById`, `setOnClickListener`, `open.launch`, `showStatus`, `capture`, `reveal`, `state.getString`, `Uri.parse`, `state.getBoolean`, `cameraControls`, `status.setText`, `scan`, `receive`, `getIntent` |
| `@Override protected void onNewIntent(Intent intent)` | void — no value | External owner / framework / callback in class atlas | `super.onNewIntent`, `setIntent`, `receive` |
| `private void receive(Intent intent)` | void — no value | onCreate, onNewIntent | `Intent.ACTION_VIEW.equals`, `intent.getAction`, `intent.getData`, `Intent.ACTION_SEND.equals`, `IntentCompat.getParcelableExtra`, `intent.getClipData`, `getItemCount`, `getItemAt`, `getUri`, `scan` |
| `private void capture()` | void — no value | onCreate | `cameraControls`, `clearPlane`, `discardCamera`, `findViewById`, `setVisibility`, `status.setText`, `worker.execute`, `File`, `getCacheDir`, `folder.isDirectory`, `folder.mkdirs`, `IOException`, `File.createTempFile`, `FileProvider.getUriForFile`, `getPackageName`, `main.post`, `launchCamera`, `showStatus` |
| `private void launchCamera(Uri uri)` | void — no value | capture, onResume | `deleteCapture`, `take.launch`, `cameraControls`, `discardCamera`, `showStatus` |
| `private void scan(Uri uri)` | void — no value | onCreate, receive | `clearPlane`, `findViewById`, `setVisibility`, `equals`, `uri.getScheme`, `showStatus`, `uri.equals`, `discardCamera`, `status.setText`, `worker.execute`, `decode` |
| `private void decode(Uri uri, int ticket)` | void — no value | scan | `ImageUtils.decodeExact`, `ImageUtils.source`, `getContentResolver`, `ImageUtils.pixels`, `StegoCore.readHeader`, `DiffMapData.buildXRay`, `image.getWidth`, `image.getHeight`, `main.post`, `showResult`, `preview.recycle`, `showStatus`, `image.recycle`, `Arrays.fill` |
| `private void showResult(int ticket, boolean found, Bitmap result)` | void — no value | decode | `result.recycle`, `xray.setPlane`, `xray.setVisibility`, `showStatus`, `UiPolish.enter`, `findViewById`, `setVisibility`, `status.performHapticFeedback` |
| `private void showStatus(int text, boolean error)` | void — no value | onCreate, capture, launchCamera, scan, decode, showResult | `status.setText`, `UiPolish.failure`, `UiPolish.notice` |
| `private void reveal()` | void — no value | onCreate | `Intent`, `setData`, `intent.addFlags`, `startActivity` |
| `private void clearPlane()` | void — no value | capture, scan, onDestroy | `xray.setPlane`, `xray.setVisibility`, `plane.recycle` |
| `private void cameraControls(boolean enabled)` | void — no value | onCreate, capture, launchCamera | `findViewById`, `setEnabled` |
| `private void discardCamera()` | void — no value | capture, launchCamera, scan, onDestroy | `deleteCapture` |
| `private void deleteCapture(Uri uri)` | void — no value | launchCamera, discardCamera | `getContentResolver`, `delete`, `worker.isShutdown`, `worker.execute`, `Executors.newSingleThreadExecutor`, `cleanup.execute`, `cleanup.shutdown` |
| `@Override protected void onResume()` | void — no value | External owner / framework / callback in class atlas | `super.onResume`, `launchCamera` |
| `@Override protected void onPause()` | void — no value | External owner / framework / callback in class atlas | `super.onPause` |
| `@Override protected void onSaveInstanceState(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onSaveInstanceState`, `state.putString`, `camera.toString`, `selected.toString`, `state.putBoolean` |
| `@Override protected void onDestroy()` | void — no value | External owner / framework / callback in class atlas | `clearPlane`, `isFinishing`, `discardCamera`, `worker.shutdown`, `super.onDestroy` |

### app/src/main/java/com/example/kineticvault/ui/SettingsActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override protected void onCreate(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `getWindow`, `addFlags`, `showPage`, `findViewById`, `ArrayAdapter.createFromResource`, `adapter.setDropDownViewResource`, `sensitivity.setAdapter`, `setEnabled`, `VaultPreferences.load`, `isDestroyed`, `populate`, `setOnClickListener`, `save`, `testConnection` |
| `private CheckBox toggle(int id)` | CheckBox — `findViewById(id)` | populate, save | `findViewById` |
| `private void populate()` | void — no value | onCreate | `VaultPreferences.current`, `sensitivity.setSelection`, `toggle`, `setChecked`, `url.setText`, `key.setText`, `findViewById`, `setEnabled` |
| `private void testConnection()` | void — no value | onCreate | `findViewById`, `setEnabled`, `Snackbar.make`, `show`, `com.example.kineticvault.data.SyncManager.get`, `testConnection`, `url.getText`, `toString`, `trim`, `key.getText`, `isDestroyed`, `performHapticFeedback` |
| `@Override protected void onPause()` | void — no value | External owner / framework / callback in class atlas | `code.getText`, `clear`, `super.onPause` |
| `@Override protected void onStop()` | void — no value | External owner / framework / callback in class atlas | `findViewById`, `setEnabled`, `super.onStop` |
| `private void save()` | void — no value | onCreate | `url.getText`, `toString`, `trim`, `android.net.Uri.parse`, `server.isEmpty`, `equals`, `parsed.getScheme`, `parsed.getHost`, `parsed.getUserInfo`, `url.setError`, `getString`, `code.length`, `code.getText`, `getChars`, `com.example.kineticvault.data.DisguiseCode.valid`, `toggle`, `isChecked`, `VaultPreferences.current`, `codeHash.isEmpty`, `java.util.Arrays.fill`, `code.setError`, `clear`, `VaultPreferences.Values`, `sensitivity.getSelectedItemPosition`, `key.getText`, `findViewById`, `setEnabled`, `VaultPreferences.save`, `isDestroyed`, `performHapticFeedback`, `com.example.kineticvault.data.SyncManager.get`, `configurationChanged`, `Snackbar.make`, `show` |

### app/src/main/java/com/example/kineticvault/ui/SplashActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override protected void onCreate(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `EdgeToEdge.enable`, `android.view.View`, `blank.setBackgroundColor`, `getColor`, `setContentView`, `VaultPreferences.load`, `isFinishing`, `isDestroyed`, `VaultPreferences.current`, `route`, `showLogo` |
| `private void showLogo()` | void — no value | onCreate | `SplashLogoView`, `logo.setBackgroundColor`, `getColor`, `setContentView`, `ValueAnimator.ofFloat`, `animator.setDuration`, `animator.addUpdateListener`, `logo.setProgress`, `value.getAnimatedValue`, `animator.start`, `main.postDelayed` |
| `private void route()` | void — no value | onCreate, onResume | `isFinishing`, `isDestroyed`, `startActivity`, `Intent`, `VaultPreferences.current`, `finish` |
| `@Override protected void onResume()` | void — no value | External owner / framework / callback in class atlas | `super.onResume`, `animator.start`, `route` |
| `@Override protected void onPause()` | void — no value | External owner / framework / callback in class atlas | `animator.cancel`, `super.onPause` |
| `@Override protected void onDestroy()` | void — no value | External owner / framework / callback in class atlas | `main.removeCallbacks`, `animator.cancel`, `super.onDestroy` |

### app/src/main/java/com/example/kineticvault/ui/SplashLogoView.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public SplashLogoView(Context context)` | New containing-class object | External owner / framework / callback in class atlas | `setContentDescription`, `context.getString` |
| `public void setProgress(float value)` | void — no value | External owner / framework / callback in class atlas | `invalidate` |
| `@Override protected void onSizeChanged(int w, int h, int oldW, int oldH)` | void — no value | External owner / framework / callback in class atlas | `Math.min`, `logo.reset`, `logo.moveTo`, `logo.lineTo`, `logo.close`, `measure.setPath` |
| `@Override protected void onDraw(Canvas canvas)` | void — no value | External owner / framework / callback in class atlas | `super.onDraw`, `drawn.reset`, `measure.getSegment`, `measure.getLength`, `paint.setColor`, `getContext`, `getColor`, `paint.setStyle`, `paint.setStrokeWidth`, `getResources`, `getDisplayMetrics`, `paint.setStrokeCap`, `canvas.drawPath`, `paint.setAlpha`, `getWidth`, `getHeight`, `canvas.drawLine` |

### app/src/main/java/com/example/kineticvault/ui/TextScrambler.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public TextScrambler(TextView target)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public void show(char[] text)` | void — no value | External owner / framework / callback in class atlas | `clear`, `Math.min`, `target.setImportantForAccessibility`, `advance` |
| `private void advance()` | void — no value | show | `Math.min`, `Character.isHighSurrogate`, `target.setText`, `main.postDelayed`, `target.setImportantForAccessibility`, `Arrays.fill` |
| `public void clear()` | void — no value | show | `main.removeCallbacks`, `Arrays.fill`, `target.setText` |

### app/src/main/java/com/example/kineticvault/ui/UiPolish.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private UiPolish()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public static void install(Activity activity)` | void — no value | External owner / framework / callback in class atlas | `activity.findViewById`, `content.getTag`, `content.setTag`, `buttons`, `enter` |
| `@SuppressLint("ClickableViewAccessibility") private static void buttons(View view)` | New containing-class object | External owner / framework / callback in class atlas | `view.setOnTouchListener`, `event.getActionMasked`, `button.isPressed`, `button.isEnabled`, `feedback`, `group.getChildCount`, `buttons`, `group.getChildAt` |
| `public static void enter(View view)` | void — no value | install | `ValueAnimator.areAnimatorsEnabled`, `view.startAnimation`, `AnimationUtils.loadAnimation`, `view.getContext` |
| `public static void feedback(View view)` | void — no value | t | `view.performHapticFeedback` |
| `public static void notice(View view)` | void — no value | failure | `view.setAccessibilityLiveRegion`, `ValueAnimator.areAnimatorsEnabled`, `view.startAnimation`, `AnimationUtils.loadAnimation`, `view.getContext` |
| `public static void failure(View view)` | void — no value | External owner / framework / callback in class atlas | `notice`, `view.performHapticFeedback` |
| `public static void pause(Activity activity)` | void — no value | External owner / framework / callback in class atlas | `activity.findViewById`, `content.clearAnimation` |

### app/src/main/java/com/example/kineticvault/ui/vault/VaultActivity.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override protected void onCreate(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `EdgeToEdge.enable`, `setContentView`, `findViewById`, `VaultRepository`, `getApplicationContext`, `VaultAdapter`, `state.containsKey`, `state.getLong`, `query.setText`, `state.getString`, `insets`, `bindList`, `bindFilters`, `rangeLabel`, `setOnClickListener`, `sheet.dismiss`, `com.example.kineticvault.ui.ExportSheet.show`, `csv.launch`, `getString`, `failure` |
| `private void insets()` | void — no value | onCreate | `getResources`, `getDimensionPixelSize`, `ViewCompat.setOnApplyWindowInsetsListener`, `insets.getInsets`, `WindowInsetsCompat.Type.systemBars`, `WindowInsetsCompat.Type.displayCutout`, `WindowInsetsCompat.Type.ime`, `view.setPadding`, `ViewCompat.requestApplyInsets` |
| `private void bindList()` | void — no value | onCreate | `findViewById`, `list.setAdapter`, `registerForContextMenu`, `list.setOnItemClickListener`, `open`, `adapter.getItem`, `list.setOnItemLongClickListener`, `view.performHapticFeedback`, `setOnClickListener`, `finish`, `filtered`, `clearFilters`, `startActivity`, `Intent`, `load` |
| `private void bindFilters()` | void — no value | onCreate | `findViewById`, `setOnClickListener`, `VaultDialogs.range`, `rangeLabel`, `load`, `clearFilters`, `query.addTextChangedListener`, `TextWatcher`, `beforeTextChanged`, `onTextChanged`, `main.removeCallbacks`, `main.postDelayed`, `afterTextChanged` |
| `public void beforeTextChanged(CharSequence text, int start, int count, int after)` | void — no value | bindFilters | No further method call in body |
| `public void onTextChanged(CharSequence text, int start, int before, int count)` | void — no value | bindFilters | `rangeLabel`, `main.removeCallbacks`, `main.postDelayed` |
| `public void afterTextChanged(Editable text)` | void — no value | bindFilters | No further method call in body |
| `private void load()` | void — no value | bindList, bindFilters, clearFilters, change, failure, onResume | `query.getText`, `toString`, `findViewById`, `setVisibility`, `worker.execute`, `repository.search`, `main.post`, `show`, `loadFailed` |
| `private void show(List<VaultEntry> entries)` | void — no value | onCreate, load, onContextItemSelected, change, failure | `findViewById`, `setVisibility`, `adapter.replace`, `entries.isEmpty`, `com.example.kineticvault.ui.UiPolish.enter`, `setText`, `filtered` |
| `private void loadFailed()` | void — no value | load | `findViewById`, `setVisibility`, `com.example.kineticvault.ui.UiPolish.failure` |
| `private boolean filtered()` | boolean — `query.length() > 0 \|\| from != null` | bindList, show, rangeLabel | `query.length` |
| `private void clearFilters()` | void — no value | bindList, bindFilters | `query.setText`, `main.removeCallbacks`, `rangeLabel`, `load` |
| `private void rangeLabel()` | void — no value | onCreate, bindFilters, onTextChanged, clearFilters | `findViewById`, `label.setText`, `getString`, `DateFormat.getMediumDateFormat`, `format`, `Date`, `setVisibility`, `filtered` |
| `private void open(VaultEntry entry)` | void — no value | bindList | `startActivity`, `Intent`, `setData`, `Uri.parse`, `failure` |
| `@Override public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info)` | void — no value | External owner / framework / callback in class atlas | `super.onCreateContextMenu`, `adapter.getItem`, `menu.setHeaderTitle`, `menu.add` |
| `@Override public boolean onContextItemSelected(MenuItem item)` | boolean — `super.onContextItemSelected(item)`; `super.onContextItemSelected(item)`; `true` | External owner / framework / callback in class atlas | `super.onContextItemSelected`, `item.getItemId`, `VaultDialogs.rename`, `change`, `repository.rename`, `VaultDialogs.expiry`, `repository.setExpiry`, `MaterialAlertDialogBuilder`, `setTitle`, `setMessage`, `setNegativeButton`, `setPositiveButton`, `repository.softDelete`, `show` |
| `private void change(Runnable mutation)` | void — no value | onContextItemSelected | `worker.execute`, `mutation.run`, `main.post`, `root.performHapticFeedback`, `load`, `Snackbar.make`, `show`, `failure` |
| `private void failure(int message)` | void — no value | onCreate, loadFailed, open, change | `com.example.kineticvault.ui.UiPolish.failure`, `Snackbar.make`, `setAction`, `load`, `show` |
| `@Override protected void onPause()` | void — no value | External owner / framework / callback in class atlas | `sheet.dismiss`, `super.onPause` |
| `@Override protected void onResume()` | void — no value | External owner / framework / callback in class atlas | `super.onResume`, `load` |
| `@Override protected void onSaveInstanceState(Bundle state)` | void — no value | External owner / framework / callback in class atlas | `state.putString`, `query.getText`, `toString`, `state.putLong`, `super.onSaveInstanceState` |
| `@Override protected void onDestroy()` | void — no value | External owner / framework / callback in class atlas | `main.removeCallbacks`, `adapter.close`, `worker.execute`, `worker.shutdown`, `super.onDestroy`, `repository::close` |

### app/src/main/java/com/example/kineticvault/ui/vault/VaultAdapter.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override protected int sizeOf(String key, Bitmap bitmap)` | int — `bitmap.getAllocationByteCount()` | External owner / framework / callback in class atlas | `bitmap.getAllocationByteCount` |
| `VaultAdapter(Context context)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `void replace(List<VaultEntry> rows)` | void — no value | External owner / framework / callback in class atlas | `entries.clear`, `entries.addAll`, `notifyDataSetChanged` |
| `@Override public int getCount()` | int — `entries.size()` | External owner / framework / callback in class atlas | `entries.size` |
| `@Override public VaultEntry getItem(int position)` | VaultEntry — `entries.get(position)` | getItemId, getView | `entries.get` |
| `@Override public long getItemId(int position)` | long — `getItem(position).id` | External owner / framework / callback in class atlas | `getItem` |
| `@Override public boolean hasStableIds()` | boolean — `true` | External owner / framework / callback in class atlas | No further method call in body |
| `@Override public View getView(int position, View recycled, ViewGroup parent)` | View — `row` | External owner / framework / callback in class atlas | `LayoutInflater.from`, `inflate`, `getItem`, `row.findViewById`, `setText`, `date`, `context.getString`, `Formatter.formatFileSize`, `expiry`, `thumbnail` |
| `private String date(long timestamp)` | String — `context.getString(R.string.vault_date_time, DateFormat.getMediumDateFormat(context).format(date), DateFormat.getTimeFormat(context).format(date))` | getView, expiry | `Date`, `context.getString`, `DateFormat.getMediumDateFormat`, `format`, `DateFormat.getTimeFormat` |
| `private void expiry(TextView view, VaultEntry entry)` | void — no value | getView | `view.setVisibility`, `view.setText`, `context.getString`, `System.currentTimeMillis`, `date` |
| `private void thumbnail(ImageView view, VaultEntry entry)` | void — no value | getView | `view.setTag`, `view.setImageResource`, `images.get`, `view.setImageBitmap`, `worker.execute`, `BitmapFactory.decodeByteArray`, `main.post`, `bitmap.recycle`, `images.put`, `entry.uuid.equals`, `view.getTag` |
| `@Override public void close()` | void — no value | External owner / framework / callback in class atlas | `worker.shutdown`, `images.evictAll` |

### app/src/main/java/com/example/kineticvault/ui/vault/VaultDialogs.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private VaultDialogs()` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `static void rename(VaultActivity host, VaultEntry entry, Consumer<String> save)` | void — no value | External owner / framework / callback in class atlas | `EditText`, `input.setHint`, `input.setText`, `input.setInputType`, `input.setFilters`, `InputFilter.LengthFilter`, `input.setMinHeight`, `host.getResources`, `getDisplayMetrics`, `input.setSaveEnabled`, `input.setImportantForAutofill`, `MaterialAlertDialogBuilder`, `setTitle`, `setMessage`, `setView`, `setNegativeButton`, `setPositiveButton`, `create`, `dialog.setOnShowListener`, `dialog.getButton`, `setOnClickListener`, `input.getText`, `toString`, `trim`, `title.isEmpty`, `input.setError`, `host.getString`, `save.accept`, `dialog.dismiss`, `dialog.show` |
| `static void expiry(VaultActivity host, VaultEntry entry, Consumer<Long> save)` | void — no value | External owner / framework / callback in class atlas | `MaterialAlertDialogBuilder`, `setTitle`, `setMessage`, `setNegativeButton`, `setNeutralButton`, `save.accept`, `setPositiveButton`, `expiryDate`, `show` |
| `private static void expiryDate(VaultActivity host, VaultEntry entry, Consumer<Long> save)` | void — no value | expiry | `Calendar.getInstance`, `date.setTimeInMillis`, `chooseDate`, `TimePickerDialog`, `selected.set`, `save.accept`, `selected.getTimeInMillis`, `date.get`, `DateFormat.is24HourFormat`, `show` |
| `static void range(VaultActivity host, Consumer<long[]> save)` | void — no value | External owner / framework / callback in class atlas | `Calendar.getInstance`, `chooseDate`, `dateDialog`, `finish.add`, `save.accept`, `start.getTimeInMillis`, `finish.getTimeInMillis`, `end.setTitle`, `end.getDatePicker`, `setMinDate`, `end.show` |
| `private static void chooseDate(VaultActivity host, Calendar date, Consumer<Calendar> selected)` | void — no value | expiryDate, range | `chooseDate` |
| `private static void chooseDate(VaultActivity host, Calendar date, Consumer<Calendar> selected, int title)` | void — no value | expiryDate, range, chooseDate | `dateDialog`, `dialog.setTitle`, `dialog.show` |
| `private static DatePickerDialog dateDialog(VaultActivity host, Calendar date, Consumer<Calendar> selected)` | DatePickerDialog — `new DatePickerDialog(host, (picker, year, month, day) -> { Calendar value = Calendar.getInstance()` | range, chooseDate | `DatePickerDialog`, `Calendar.getInstance`, `value.clear`, `value.set`, `selected.accept`, `date.get` |

### app/src/main/java/com/example/kineticvault/ui/VaultDoorView.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public VaultDoorView(Context context)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public VaultDoorView(Context context, AttributeSet attrs)` | New containing-class object | External owner / framework / callback in class atlas | `setContentDescription`, `context.getString` |
| `@Override protected void onDraw(Canvas canvas)` | void — no value | External owner / framework / callback in class atlas | `super.onDraw`, `getWidth`, `getHeight`, `Math.min`, `paint.setStyle`, `paint.setColor`, `ContextCompat.getColor`, `getContext`, `canvas.drawCircle`, `paint.setStrokeWidth`, `Math.max`, `paint.setStrokeCap`, `canvas.drawLine`, `Math.cos`, `Math.sin` |
| `public void open(Runnable completed)` | void — no value | External owner / framework / callback in class atlas | `reset`, `ValueAnimator.ofFloat`, `animation.setDuration`, `animation.addUpdateListener`, `value.getAnimatedValue`, `invalidate`, `animation.addListener`, `AnimatorListenerAdapter`, `onAnimationCancel`, `onAnimationEnd`, `setContentDescription`, `getContext`, `getString`, `completed.run`, `animation.start` |
| `@Override public void onAnimationCancel(Animator value)` | void — no value | open | No further method call in body |
| `@Override public void onAnimationEnd(Animator value)` | void — no value | open | `setContentDescription`, `getContext`, `getString`, `completed.run` |
| `public void deny()` | void — no value | External owner / framework / callback in class atlas | `reset`, `ValueAnimator.ofFloat`, `animation.setDuration`, `animation.addUpdateListener`, `value.getAnimatedValue`, `invalidate`, `animation.start` |
| `public void reset()` | void — no value | open, deny, onDetachedFromWindow | `animation.cancel`, `clearAnimation`, `invalidate`, `setContentDescription`, `getContext`, `getString` |
| `@Override protected void onDetachedFromWindow()` | void — no value | External owner / framework / callback in class atlas | `reset`, `super.onDetachedFromWindow` |

### app/src/main/java/com/example/kineticvault/ui/WipeComparisonView.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public WipeComparisonView(Context context)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public WipeComparisonView(Context context, AttributeSet attrs)` | New containing-class object | External owner / framework / callback in class atlas | `setContentDescription`, `context.getString` |
| `public void setImages(Bitmap original, Bitmap encoded)` | void — no value | External owner / framework / callback in class atlas | `invalidate` |
| `public void setFraction(float value)` | void — no value | External owner / framework / callback in class atlas | `Math.max`, `Math.min`, `invalidate` |
| `@Override protected void onDraw(Canvas canvas)` | void — no value | External owner / framework / callback in class atlas | `super.onDraw`, `Math.min`, `getWidth`, `before.getWidth`, `getHeight`, `before.getHeight`, `destination.set`, `canvas.drawBitmap`, `canvas.save`, `canvas.clipRect`, `canvas.restoreToCount`, `paint.setColor`, `getContext`, `getColor`, `paint.setStrokeWidth`, `getResources`, `getDimension`, `canvas.drawLine` |

### app/src/main/java/com/example/kineticvault/ui/WipeSliderView.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public WipeSliderView(Context context)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public WipeSliderView(Context context, AttributeSet attrs)` | New containing-class object | External owner / framework / callback in class atlas | `setOrientation`, `WipeComparisonView`, `addView`, `LayoutParams`, `SeekBar`, `slider.setMax`, `slider.setProgress`, `slider.setContentDescription`, `context.getString`, `getResources`, `getDimensionPixelSize`, `slider.setOnSeekBarChangeListener`, `SeekBar.OnSeekBarChangeListener`, `onProgressChanged`, `comparison.setFraction`, `onStartTrackingTouch`, `onStopTrackingTouch`, `bar.performHapticFeedback` |
| `public void onProgressChanged(SeekBar bar, int value, boolean user)` | void — no value | WipeSliderView | `comparison.setFraction` |
| `public void onStartTrackingTouch(SeekBar bar)` | void — no value | WipeSliderView | No further method call in body |
| `public void onStopTrackingTouch(SeekBar bar)` | void — no value | WipeSliderView | `bar.performHapticFeedback` |
| `public void setImages(Bitmap before, Bitmap after)` | void — no value | External owner / framework / callback in class atlas | `comparison.setImages` |

### app/src/main/java/com/example/kineticvault/ui/XRayView.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public XRayView(Context context)` | New containing-class object | External owner / framework / callback in class atlas | No further method call in body |
| `public XRayView(Context context, AttributeSet attrs)` | New containing-class object | External owner / framework / callback in class atlas | `setContentDescription`, `context.getString` |
| `public void setPlane(Bitmap value)` | void — no value | External owner / framework / callback in class atlas | `invalidate` |
| `@Override protected void onDraw(Canvas canvas)` | void — no value | External owner / framework / callback in class atlas | `super.onDraw`, `canvas.drawColor`, `Math.min`, `getWidth`, `plane.getWidth`, `getHeight`, `plane.getHeight`, `area.set`, `canvas.drawBitmap` |

### app/src/main/java/com/example/kineticvault/VaultApplication.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Override public void onCreate()` | void — no value | External owner / framework / callback in class atlas | `super.onCreate`, `com.example.kineticvault.data.VaultPreferences.load`, `com.example.kineticvault.data.SyncManager.get`, `configurationChanged`, `registerActivityLifecycleCallbacks`, `ActivityLifecycleCallbacks`, `onActivityStarted`, `start`, `onActivityStopped`, `stop`, `onActivityCreated`, `onActivityResumed`, `com.example.kineticvault.ui.UiPolish.install`, `onActivityPaused`, `com.example.kineticvault.ui.UiPolish.pause`, `onActivitySaveInstanceState`, `onActivityDestroyed`, `AppCompatDelegate.setDefaultNightMode` |
| `registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks()` | New containing-class object | onCreate | `onActivityStarted`, `com.example.kineticvault.data.SyncManager.get`, `start`, `onActivityStopped`, `stop`, `onActivityCreated`, `onActivityResumed`, `com.example.kineticvault.ui.UiPolish.install`, `onActivityPaused`, `com.example.kineticvault.ui.UiPolish.pause`, `onActivitySaveInstanceState`, `onActivityDestroyed` |
| `@Override public void onActivityStarted(android.app.Activity activity)` | void — no value | onCreate, registerActivityLifecycleCallbacks | `com.example.kineticvault.data.SyncManager.get`, `start` |
| `@Override public void onActivityStopped(android.app.Activity activity)` | void — no value | onCreate, registerActivityLifecycleCallbacks | `com.example.kineticvault.data.SyncManager.get`, `stop` |
| `@Override public void onActivityCreated(android.app.Activity a, android.os.Bundle b)` | void — no value | onCreate, registerActivityLifecycleCallbacks | No further method call in body |
| `@Override public void onActivityResumed(android.app.Activity a)` | void — no value | onCreate, registerActivityLifecycleCallbacks | `com.example.kineticvault.ui.UiPolish.install` |
| `@Override public void onActivityPaused(android.app.Activity a)` | void — no value | onCreate, registerActivityLifecycleCallbacks | `com.example.kineticvault.ui.UiPolish.pause` |
| `@Override public void onActivitySaveInstanceState(android.app.Activity a, android.os.Bundle b)` | void — no value | onCreate, registerActivityLifecycleCallbacks | No further method call in body |
| `@Override public void onActivityDestroyed(android.app.Activity a)` | void — no value | onCreate, registerActivityLifecycleCallbacks | No further method call in body |

### app/src/test/java/com/example/kineticvault/core/PayloadCodecTest.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Test public void emptyTextAndFileAreSupported() throws Exception` | void — no value | External owner / framework / callback in class atlas | `PayloadCodec.Payload`, `PayloadCodec.unpack`, `PayloadCodec.pack`, `assertEquals` |
| `@Test public void filenameTraversalAndControlCharactersAreRejected()` | void — no value | External owner / framework / callback in class atlas | `assertThrows`, `PayloadCodec.Payload` |
| `@Test public void unknownTypeAndLongNameAreRejected()` | void — no value | External owner / framework / callback in class atlas | `assertThrows`, `PayloadCodec.Payload`, `Arrays.fill`, `String` |
| `@Test public void packedLengthsMustMatchExactly() throws Exception` | void — no value | External owner / framework / callback in class atlas | `PayloadCodec.Payload`, `PayloadCodec.pack`, `Arrays.copyOf`, `assertThrows`, `PayloadCodec.unpack`, `ByteBuffer.wrap`, `putInt` |
| `@Test public void truncatedNameAndInvalidUtf8AreRejected()` | void — no value | External owner / framework / callback in class atlas | `assertThrows`, `PayloadCodec.unpack` |
| `@Test public void bothCompressionSettingsHaveGzipEnvelope() throws Exception` | void — no value | External owner / framework / callback in class atlas | `GzipCodec.compress`, `assertEquals`, `assertArrayEquals`, `GzipCodec.decompress`, `assertTrue` |
| `@Test public void corruptGzipIsRejected() throws Exception` | void — no value | External owner / framework / callback in class atlas | `GzipCodec.compress`, `assertThrows`, `GzipCodec.decompress` |
| `@Test public void gzipBombStopsAtExpansionLimit() throws Exception` | void — no value | External owner / framework / callback in class atlas | `ByteArrayOutputStream`, `GZIPOutputStream`, `Math.min`, `gzip.write`, `assertThrows`, `GzipCodec.decompress`, `buffer.toByteArray` |

### app/src/test/java/com/example/kineticvault/core/StegoCoreTest.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private int[] cover(int count)` | int[] — `result` | exactCapacityWorksForEveryPixelRemainder, standardAndGhostBothRoundTripWithDifferentMaps, ghostShuffleIsDeterministic, onlyRgbLeastSignificantBitsChangeAndCoverIsPreserved, absentSignatureAndTinyImagesAreRejected, transparencyIsRejected, maliciousLengthsAreRejectedBeforeAllocation | `Arrays.fill` |
| `private VaultHeader header(int flags, int length) throws IOException` | VaultHeader — `VaultHeader.create(new byte[16], new byte[12], flags, length)` | exactCapacityWorksForEveryPixelRemainder, standardAndGhostBothRoundTripWithDifferentMaps, ghostShuffleIsDeterministic, onlyRgbLeastSignificantBitsChangeAndCoverIsPreserved, transparencyIsRejected, corruptedMagicVersionAndFlagsAreRejected, maliciousLengthsAreRejectedBeforeAllocation, headerCopiesCannotMutateItsContents | `VaultHeader.create` |
| `@Test public void headerHasExactLayoutAndSize() throws Exception` | void — no value | External owner / framework / callback in class atlas | `Arrays.fill`, `VaultHeader.create`, `bytes`, `assertEquals`, `assertArrayEquals`, `Arrays.copyOf`, `Arrays.copyOfRange`, `ByteBuffer.wrap`, `getInt` |
| `@Test public void exactCapacityWorksForEveryPixelRemainder() throws Exception` | void — no value | External owner / framework / callback in class atlas | `StegoCore.capacityBytes`, `Arrays.fill`, `StegoCore.embed`, `cover`, `header`, `assertArrayEquals`, `StegoCore.extract`, `assertThrows` |
| `@Test public void capacityAccountsForBothUnusedHeaderBits()` | void — no value | External owner / framework / callback in class atlas | `assertEquals`, `StegoCore.capacityBytes` |
| `@Test public void standardAndGhostBothRoundTripWithDifferentMaps() throws Exception` | void — no value | External owner / framework / callback in class atlas | `Arrays.fill`, `cover`, `StegoCore.embed`, `header`, `assertArrayEquals`, `StegoCore.extract`, `assertFalse`, `Arrays.equals`, `assertEquals`, `assertTrue` |
| `@Test public void ghostShuffleIsDeterministic() throws Exception` | void — no value | External owner / framework / callback in class atlas | `Arrays.fill`, `header`, `assertArrayEquals`, `StegoCore.embed`, `cover` |
| `@Test public void onlyRgbLeastSignificantBitsChangeAndCoverIsPreserved() throws Exception` | void — no value | External owner / framework / callback in class atlas | `cover`, `original.clone`, `StegoCore.embed`, `header`, `assertArrayEquals`, `assertEquals` |
| `@Test public void absentSignatureAndTinyImagesAreRejected()` | void — no value | External owner / framework / callback in class atlas | `assertThrows`, `StegoCore.readHeader`, `cover` |
| `@Test public void transparencyIsRejected() throws Exception` | void — no value | External owner / framework / callback in class atlas | `cover`, `assertThrows`, `StegoCore.embed`, `header` |
| `@Test public void corruptedMagicVersionAndFlagsAreRejected() throws Exception` | void — no value | External owner / framework / callback in class atlas | `header`, `bytes`, `valid.clone`, `assertThrows`, `VaultHeader.parse` |
| `@Test public void maliciousLengthsAreRejectedBeforeAllocation() throws Exception` | void — no value | External owner / framework / callback in class atlas | `header`, `bytes`, `ByteBuffer.wrap`, `putInt`, `assertThrows`, `VaultHeader.parse`, `StegoCore.embed`, `cover`, `Arrays.copyOf`, `StegoCore.extract` |
| `@Test public void headerCopiesCannotMutateItsContents() throws Exception` | void — no value | External owner / framework / callback in class atlas | `header`, `header.bytes`, `header.salt`, `assertEquals` |

### app/src/test/java/com/example/kineticvault/core/VaultEngineTest.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `private byte[] gesture()` | byte[] — `new byte[]{0, 0, 4, 5}` | hide, textRoundTripsInBothModesAndCompressionLevels, arbitraryFileBytesRoundTrip, wrongPassphraseFailsAuthentication, passphraseWhitespaceIsSignificant, ciphertextBitFlipFailsAuthentication, changedPassphraseFlagIsAuthenticated, changedIvIsAuthenticated, changedSaltIsAuthenticated, oversizedSecretStopsBeforeKeyDerivation, progressFinishesAtActualBitCount, canonicalEncodingIsStableAndUnambiguous, invalidGestureTokensAreRejected, decryptsIndependentNodeCryptoFixture | No further method call in body |
| `private char[] phrase()` | char[] — `" demo \| café 🔐 ".toCharArray()` | hide, textRoundTripsInBothModesAndCompressionLevels, wrongGestureFailsAuthentication, ciphertextBitFlipFailsAuthentication, changedPassphraseFlagIsAuthenticated, changedIvIsAuthenticated, changedSaltIsAuthenticated, oversizedSecretStopsBeforeKeyDerivation | `toCharArray` |
| `private int[] cover()` | int[] — `pixels` | hide, arbitraryFileBytesRoundTrip, progressFinishesAtActualBitCount | `Arrays.fill` |
| `private int[] hide(boolean ghost, boolean compress) throws Exception` | int[] — `VaultEngine.hide(cover(), payload, gesture(), phrase(), compress, ghost, null)` | textRoundTripsInBothModesAndCompressionLevels, arbitraryFileBytesRoundTrip, wrongGestureFailsAuthentication, wrongPassphraseFailsAuthentication, passphraseWhitespaceIsSignificant, ciphertextBitFlipFailsAuthentication, changedPassphraseFlagIsAuthenticated, changedIvIsAuthenticated, changedSaltIsAuthenticated, repeatedEncryptionUsesFreshSaltAndIv, oversizedSecretStopsBeforeKeyDerivation, progressFinishesAtActualBitCount | `getBytes`, `PayloadCodec.Payload`, `VaultEngine.hide`, `cover`, `gesture`, `phrase` |
| `@Test public void textRoundTripsInBothModesAndCompressionLevels() throws Exception` | void — no value | External owner / framework / callback in class atlas | `hide`, `VaultEngine.reveal`, `gesture`, `phrase`, `assertEquals`, `String` |
| `@Test public void arbitraryFileBytesRoundTrip() throws Exception` | void — no value | External owner / framework / callback in class atlas | `PayloadCodec.Payload`, `VaultEngine.hide`, `cover`, `gesture`, `assertFalse`, `StegoCore.readHeader`, `usesPassphrase`, `VaultEngine.reveal`, `assertEquals`, `assertArrayEquals` |
| `@Test public void wrongGestureFailsAuthentication() throws Exception` | void — no value | External owner / framework / callback in class atlas | `hide`, `assertThrows`, `VaultEngine.reveal`, `phrase` |
| `@Test public void wrongPassphraseFailsAuthentication() throws Exception` | void — no value | External owner / framework / callback in class atlas | `hide`, `assertThrows`, `VaultEngine.reveal`, `gesture`, `toCharArray` |
| `@Test public void passphraseWhitespaceIsSignificant() throws Exception` | void — no value | External owner / framework / callback in class atlas | `hide`, `assertThrows`, `VaultEngine.reveal`, `gesture`, `toCharArray` |
| `@Test public void ciphertextBitFlipFailsAuthentication() throws Exception` | void — no value | External owner / framework / callback in class atlas | `hide`, `assertThrows`, `VaultEngine.reveal`, `gesture`, `phrase` |
| `@Test public void changedPassphraseFlagIsAuthenticated() throws Exception` | void — no value | External owner / framework / callback in class atlas | `hide`, `flipHeaderBit`, `assertFalse`, `StegoCore.readHeader`, `usesPassphrase`, `assertThrows`, `VaultEngine.reveal`, `gesture`, `phrase` |
| `@Test public void changedIvIsAuthenticated() throws Exception` | void — no value | External owner / framework / callback in class atlas | `hide`, `flipHeaderBit`, `assertThrows`, `VaultEngine.reveal`, `gesture`, `phrase` |
| `@Test public void changedSaltIsAuthenticated() throws Exception` | void — no value | External owner / framework / callback in class atlas | `hide`, `flipHeaderBit`, `assertThrows`, `VaultEngine.reveal`, `gesture`, `phrase` |
| `@Test public void repeatedEncryptionUsesFreshSaltAndIv() throws Exception` | void — no value | External owner / framework / callback in class atlas | `hide`, `assertFalse`, `Arrays.equals`, `StegoCore.readHeader`, `salt`, `iv` |
| `@Test public void oversizedSecretStopsBeforeKeyDerivation() throws Exception` | void — no value | External owner / framework / callback in class atlas | `Arrays.fill`, `PayloadCodec.Payload`, `assertThrows`, `VaultEngine.hide`, `gesture`, `phrase`, `assertFalse` |
| `@Test public void progressFinishesAtActualBitCount() throws Exception` | void — no value | External owner / framework / callback in class atlas | `PayloadCodec.Payload`, `VaultEngine.hide`, `cover`, `gesture`, `assertEquals`, `StegoCore.readHeader`, `ciphertextLength` |
| `@Test public void canonicalEncodingIsStableAndUnambiguous()` | void — no value | External owner / framework / callback in class atlas | `assertArrayEquals`, `toCharArray`, `GestureCredential.canonical`, `gesture`, `assertFalse`, `Arrays.equals`, `assertEquals`, `GestureCredential.entropyBits` |
| `@Test public void invalidGestureTokensAreRejected()` | void — no value | External owner / framework / callback in class atlas | `assertThrows`, `GestureCredential.canonical`, `gesture` |
| `@Test public void payloadCloseWipesOwnedPlaintext() throws Exception` | void — no value | External owner / framework / callback in class atlas | `PayloadCodec.Payload`, `payload.close`, `assertArrayEquals` |
| `@Test public void decryptsIndependentNodeCryptoFixture() throws Exception` | void — no value | External owner / framework / callback in class atlas | `hex`, `CryptoCore.decrypt`, `VaultHeader.parse`, `gesture`, `toCharArray`, `assertArrayEquals`, `getBytes`, `Arrays.fill` |
| `private byte[] hex(String text)` | byte[] — `result` | decryptsIndependentNodeCryptoFixture | `text.length`, `Integer.parseInt`, `text.substring` |
| `private void flipHeaderBit(int[] image, int bit)` | void — no value | changedPassphraseFlagIsAuthenticated, changedIvIsAuthenticated, changedSaltIsAuthenticated | No further method call in body |

### app/src/test/java/com/example/kineticvault/ExampleUnitTest.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `public void addition_isCorrect()` | void — no value | External owner / framework / callback in class atlas | `assertEquals` |

### app/src/test/java/com/example/kineticvault/image/ImagePolicyTest.java

| Exact declaration / parameters | Return value | Local callers / registration container | Direct calls and callback references |
| --- | --- | --- | --- |
| `@Test public void coverFitsBothPixelCountAndLongSide() throws Exception` | void — no value | External owner / framework / callback in class atlas | `ImageMemory.fitCover`, `assertTrue`, `Math.max`, `assertEquals` |
| `@Test public void smallerBudgetLimitsPixelsEvenBelowLongSideLimit() throws Exception` | void — no value | External owner / framework / callback in class atlas | `ImageMemory.fitCover`, `assertArrayEquals` |
| `@Test public void narrowAndExtremeDimensionsCannotOverflow() throws Exception` | void — no value | External owner / framework / callback in class atlas | `ImageMemory.fitCover`, `assertTrue`, `assertThrows`, `ImageMemory.pixelCount` |
| `@Test public void coreLimitIsExact() throws Exception` | void — no value | External owner / framework / callback in class atlas | `assertEquals`, `ImageMemory.pixelCount`, `assertThrows` |
| `@Test public void smallCoverIsNotUpscaled() throws Exception` | void — no value | External owner / framework / callback in class atlas | `assertArrayEquals`, `ImageMemory.fitCover` |
| `@Test public void ghostBudgetIncludesItsShuffleArray() throws Exception` | void — no value | External owner / framework / callback in class atlas | `assertTrue`, `ImageMemory.maximumPixels`, `assertEquals`, `ImageMemory.requiredBytes`, `assertThrows` |
| `@Test public void tinyInvalidPlansAreRejected()` | void — no value | External owner / framework / callback in class atlas | `assertThrows`, `ImageMemory.fitCover` |
| `@Test public void unchangedImageHasInfinitePsnr()` | void — no value | External owner / framework / callback in class atlas | `ImageStats.compare`, `assertEquals` |
| `@Test public void knownOneLevelChangeHasCorrectPsnr()` | void — no value | External owner / framework / callback in class atlas | `ImageStats.compare`, `assertEquals` |
| `@Test public void changedPixelsCountOnceAcrossThreeChannels()` | void — no value | External owner / framework / callback in class atlas | `ImageStats.compare`, `assertEquals` |
| `@Test public void squaredErrorAccumulatorDoesNotOverflow()` | void — no value | External owner / framework / callback in class atlas | `Arrays.fill`, `ImageStats.compare`, `assertEquals` |
| `@Test public void unequalPixelArraysAreRejected()` | void — no value | External owner / framework / callback in class atlas | `assertThrows`, `ImageStats.compare` |

## Likely viva questions

1. **What starts the app?** Android creates VaultApplication, then launches SplashActivity via MAIN/LAUNCHER; Splash routes using saved disguise settings.
2. **Can another app open every screen?** No. Splash and image-receiving Scanner are exported; ordinary feature Activities are internal.
3. **Why FileProvider?** It exposes selected private files as content URIs with temporary grants, rather than requiring broad storage access.
4. **Where are receivers declared?** They are registered in Java for screen-off/airplane events during bounded screen/foreground lifetimes, not in the manifest.
5. **Why do old documents mention another launcher?** IMPLEMENTATION contains historical Phase1B notes; the actual manifest and navigation classes describe the current app.

## Covers these files

- `.gitignore`
- `AGENTS.md`
- `app/.gitignore`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/example/kineticvault/ExampleInstrumentedTest.java`
- `app/src/androidTest/java/com/example/kineticvault/image/ImageLayerTest.java`
- `app/src/androidTest/java/com/example/kineticvault/image/PngFixtures.java`
- `app/src/debug/res/xml/network_security_config.xml`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/assets/how_it_works.html`
- `app/src/main/java/com/example/kineticvault/core/Constants.java`
- `app/src/main/java/com/example/kineticvault/core/CryptoCore.java`
- `app/src/main/java/com/example/kineticvault/core/GestureCredential.java`
- `app/src/main/java/com/example/kineticvault/core/GzipCodec.java`
- `app/src/main/java/com/example/kineticvault/core/PayloadCodec.java`
- `app/src/main/java/com/example/kineticvault/core/StegoCore.java`
- `app/src/main/java/com/example/kineticvault/core/VaultEngine.java`
- `app/src/main/java/com/example/kineticvault/core/VaultHeader.java`
- `app/src/main/java/com/example/kineticvault/core/WorkProgress.java`
- `app/src/main/java/com/example/kineticvault/data/DisguiseCode.java`
- `app/src/main/java/com/example/kineticvault/data/HistoryCsv.java`
- `app/src/main/java/com/example/kineticvault/data/SyncClient.java`
- `app/src/main/java/com/example/kineticvault/data/SyncManager.java`
- `app/src/main/java/com/example/kineticvault/data/SyncStore.java`
- `app/src/main/java/com/example/kineticvault/data/VaultDatabaseHelper.java`
- `app/src/main/java/com/example/kineticvault/data/VaultEntry.java`
- `app/src/main/java/com/example/kineticvault/data/VaultLogger.java`
- `app/src/main/java/com/example/kineticvault/data/VaultPreferences.java`
- `app/src/main/java/com/example/kineticvault/data/VaultRepository.java`
- `app/src/main/java/com/example/kineticvault/diagnostics/HarnessReport.java`
- `app/src/main/java/com/example/kineticvault/diagnostics/HarnessRunner.java`
- `app/src/main/java/com/example/kineticvault/diagnostics/TestHarnessActivity.java`
- `app/src/main/java/com/example/kineticvault/gesture/GesturePadView.java`
- `app/src/main/java/com/example/kineticvault/gesture/GestureRecognizer.java`
- `app/src/main/java/com/example/kineticvault/gesture/GestureSession.java`
- `app/src/main/java/com/example/kineticvault/gesture/StrengthMeter.java`
- `app/src/main/java/com/example/kineticvault/image/CoverGenerator.java`
- `app/src/main/java/com/example/kineticvault/image/ImageDecoder.java`
- `app/src/main/java/com/example/kineticvault/image/ImageFiles.java`
- `app/src/main/java/com/example/kineticvault/image/ImageMemory.java`
- `app/src/main/java/com/example/kineticvault/image/ImageStats.java`
- `app/src/main/java/com/example/kineticvault/image/ImageUtils.java`
- `app/src/main/java/com/example/kineticvault/image/PngIntegrity.java`
- `app/src/main/java/com/example/kineticvault/sensor/SensorFusionManager.java`
- `app/src/main/java/com/example/kineticvault/ui/AboutActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/CalculatorActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/DiffMapData.java`
- `app/src/main/java/com/example/kineticvault/ui/DiffMapView.java`
- `app/src/main/java/com/example/kineticvault/ui/DiffStats.java`
- `app/src/main/java/com/example/kineticvault/ui/ExportSheet.java`
- `app/src/main/java/com/example/kineticvault/ui/GhostComparisonActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/GhostComparisonWork.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/CoverStep.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/GestureReplay.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/GestureStep.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/HideActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/HideExporter.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/HideForge.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/HideStore.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/HideWork.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/ProofSheet.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/ResultStep.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/SecretInput.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/SecretStep.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/TextImport.java`
- `app/src/main/java/com/example/kineticvault/ui/HomeActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/InfoActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/KineticOrbView.java`
- `app/src/main/java/com/example/kineticvault/ui/LearnActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/MatrixRainView.java`
- `app/src/main/java/com/example/kineticvault/ui/NerdOverlay.java`
- `app/src/main/java/com/example/kineticvault/ui/reveal/RevealActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/reveal/RevealExpiry.java`
- `app/src/main/java/com/example/kineticvault/ui/reveal/RevealExport.java`
- `app/src/main/java/com/example/kineticvault/ui/reveal/RevealGesture.java`
- `app/src/main/java/com/example/kineticvault/ui/reveal/RevealLedger.java`
- `app/src/main/java/com/example/kineticvault/ui/reveal/RevealWork.java`
- `app/src/main/java/com/example/kineticvault/ui/ScannerActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/SettingsActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/SplashActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/SplashLogoView.java`
- `app/src/main/java/com/example/kineticvault/ui/TextScrambler.java`
- `app/src/main/java/com/example/kineticvault/ui/UiPolish.java`
- `app/src/main/java/com/example/kineticvault/ui/vault/VaultActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/vault/VaultAdapter.java`
- `app/src/main/java/com/example/kineticvault/ui/vault/VaultDialogs.java`
- `app/src/main/java/com/example/kineticvault/ui/VaultDoorView.java`
- `app/src/main/java/com/example/kineticvault/ui/WipeComparisonView.java`
- `app/src/main/java/com/example/kineticvault/ui/WipeSliderView.java`
- `app/src/main/java/com/example/kineticvault/ui/XRayView.java`
- `app/src/main/java/com/example/kineticvault/VaultApplication.java`
- `app/src/main/keepRules/rules.keep`
- `app/src/main/res/anim/fade_in.xml`
- `app/src/main/res/anim/pulse.xml`
- `app/src/main/res/anim/shake.xml`
- `app/src/main/res/anim/slide_up.xml`
- `app/src/main/res/drawable/bg_glow_card_cyan.xml`
- `app/src/main/res/drawable/bg_glow_card_violet.xml`
- `app/src/main/res/drawable/ic_arrow_down.xml`
- `app/src/main/res/drawable/ic_arrow_left.xml`
- `app/src/main/res/drawable/ic_arrow_right.xml`
- `app/src/main/res/drawable/ic_arrow_up.xml`
- `app/src/main/res/drawable/ic_history.xml`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_lock.xml`
- `app/src/main/res/drawable/ic_orb.xml`
- `app/src/main/res/drawable/ic_scan.xml`
- `app/src/main/res/drawable/ic_settings.xml`
- `app/src/main/res/drawable/ic_shield.xml`
- `app/src/main/res/layout/activity_about.xml`
- `app/src/main/res/layout/activity_calculator.xml`
- `app/src/main/res/layout/activity_ghost_comparison.xml`
- `app/src/main/res/layout/activity_hide.xml`
- `app/src/main/res/layout/activity_home.xml`
- `app/src/main/res/layout/activity_learn.xml`
- `app/src/main/res/layout/activity_reveal.xml`
- `app/src/main/res/layout/activity_scanner.xml`
- `app/src/main/res/layout/activity_settings.xml`
- `app/src/main/res/layout/activity_test_harness.xml`
- `app/src/main/res/layout/activity_vault.xml`
- `app/src/main/res/layout/hide_step_cover.xml`
- `app/src/main/res/layout/hide_step_forge.xml`
- `app/src/main/res/layout/hide_step_gesture.xml`
- `app/src/main/res/layout/hide_step_result.xml`
- `app/src/main/res/layout/hide_step_secret.xml`
- `app/src/main/res/layout/sheet_export.xml`
- `app/src/main/res/layout/vault_row.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/mipmap-hdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-mdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/diff_strings.xml`
- `app/src/main/res/values/dimens.xml`
- `app/src/main/res/values/gesture_ids.xml`
- `app/src/main/res/values/scanner_strings.xml`
- `app/src/main/res/values/settings_strings.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/main/res/values/sync_strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values/vault_strings.xml`
- `app/src/main/res/values-night/colors.xml`
- `app/src/main/res/values-night/themes.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/main/res/xml/network_security_config.xml`
- `app/src/test/java/com/example/kineticvault/core/PayloadCodecTest.java`
- `app/src/test/java/com/example/kineticvault/core/StegoCoreTest.java`
- `app/src/test/java/com/example/kineticvault/core/VaultEngineTest.java`
- `app/src/test/java/com/example/kineticvault/ExampleUnitTest.java`
- `app/src/test/java/com/example/kineticvault/image/ImagePolicyTest.java`
- `build.gradle.kts`
- `design.md`
- `docs/AUDIT.md`
- `docs/BUILD_HEALTH.md`
- `docs/IMPLEMENTATION.md`
- `docs/INVENTIONS.md`
- `docs/PHONE_CHECKLIST.md`
- `docs/STATE_SNAPSHOT.md`
- `gradle/gradle-daemon-jvm.properties`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle.properties`
- `gradlew`
- `gradlew.bat`
- `local.properties`
- `README.md`
- `server/.gitignore`
- `server/.htaccess`
- `server/api.php`
- `server/config.sample.php`
- `server/README_XAMPP.md`
- `server/schema.sql`
- `settings.gradle.kts`
- `User Manual.md`
