# 01 — Feature index

## Reading the index

A feature is an action or result a user can see. UI means user interface. An Activity is an Android screen; a method is a named operation inside a class. A callback is an operation Android or a helper calls later after an event. An ASCII call chain below reads left to right; slash separates alternative branches, and a semicolon starts a related chain rather than a second mandatory step.

All Java class paths below are under `app/src/main/java/com/example/kineticvault/`: named screen/helper classes in `ui/`, Hide helpers in `ui/hide/`, Reveal helpers in `ui/reveal/`, library helpers in `ui/vault/`, engine in `core/`, motion in `sensor/` and `gesture/`, images in `image/`, records/sync in `data/`, lab in `diagnostics/`. Each table names the real methods; 00_PROJECT_MAP.md gives the exact class/file catalogue and contracts. Layout paths are relative to `app/src/main/res/layout/` unless the row explicitly says values/assets/manifest. XML defines controls; several dialogs and the splash are built directly in Java.

Plaintext means readable secret content. Ciphertext means encrypted bytes. A PNG is a lossless image file; RGB are red/green/blue channel values; an LSB is a channel's lowest binary bit. A URI (Android Uri) is a provider/file address; ContentResolver opens its streams. FileProvider grants temporary access to selected private files. SQLite is the on-device relational database; SharedPreferences is Android's named key/value settings store; MySQL is the separate server database. GZIP is the reversible compression envelope; AES-GCM is encryption with a tamper-detection tag; PBKDF2 derives a key from password characters and a random salt. An IV is a fresh encryption initialization value. Ghost scatters ciphertext placement using public metadata. PSNR (peak signal-to-noise ratio) measures RGB image difference in decibels (dB), not security.

Intent means an Android action request: explicit intents name an app screen, implicit intents ask Android to choose a compatible app (SEND/VIEW/pickers/camera). The Activity Result API registers a contract and callback, then launches an external operation. FLAG_SECURE is the Android secure-window flag. A Bundle is Android's saved screen-state container. A generation counter identifies obsolete asynchronous work. BLOB means a database byte-array column. MIME means a media/content type such as image/png. CRUD means create/read/update/delete. A tombstone is a retained deletion marker that stops an older sync update from restoring a deleted record. See 02_ARCHITECTURE.md for small real code examples of these constructs, algorithms and their constants; the rows here explain observable behavior and disclosure boundaries.

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


## Feature locator table

| User-visible feature | Screen | Main classes/methods: full chain | Layout files / asset |
| --- | --- | --- | --- |
| Normal launch and animated logo | Splash | `SplashActivity.onCreate -> VaultPreferences.load -> SplashActivity.showLogo -> SplashLogoView.setProgress -> SplashActivity.route -> HomeActivity` | Java-built blank/logo views; no splash XML |
| Calculator disguise and launch code | Calculator / Settings | `SettingsActivity.save -> VaultPreferences.save -> DisguiseCode.create; SplashActivity.route -> CalculatorActivity.equalsPressed -> VaultPreferences.unlockDisguise -> DisguiseCode.matches -> HomeActivity` | activity_calculator.xml; activity_settings.xml |
| Four-operation calculator | Calculator | `CalculatorActivity.press -> operator / calculate -> apply -> showNumber -> render` | activity_calculator.xml |
| Home navigation and More options | Home | `HomeActivity.onCreate / overflow -> startActivity(new Intent(...)) -> selected Activity` | activity_home.xml |
| Select a cover from device | Hide / Cover | `CoverStep.openGallery -> OpenDocument result -> CoverStep.load -> HideWork.run -> HideStore.load -> ImageUtils.decodeCover -> ImageDecoder.cover` | activity_hide.xml; hide_step_cover.xml |
| Take a cover photo | Hide / Cover | `CoverStep.openCamera -> HideWork.run -> HideStore.prepareCapture -> CoverStep.resume -> CameraContract.createIntent -> TakePicture result -> HideStore.finishCapture -> ImageUtils.decodeCover` | hide_step_cover.xml |
| Generate offline cover | Hide / Cover | `CoverStep.load -> HideWork.run -> HideStore.generate -> CoverGenerator.generate -> HideStore.adopt` | hide_step_cover.xml |
| Text or arbitrary file secret | Hide / Secret | `SecretStep.bind -> snapshot -> SecretInput.read -> encodeText / readFile -> PayloadCodec.Payload` | hide_step_secret.xml |
| Import UTF-8 .txt | Hide / Secret | `SecretStep textPicker callback -> TextImport.read -> main consumer -> EditText.setText -> SecretStep.schedule` | hide_step_secret.xml |
| Compression toggle | Hide / Secret | `SecretStep.schedule -> HideWork.measure -> HideStore.measure -> PayloadCodec.pack -> GzipCodec.compress` | hide_step_secret.xml |
| Ghost mode toggle | Hide / Secret and engine | `SecretStep.snapshot -> HideStore.measure -> HideStore.forge -> HideForge.run -> VaultEngine.hide -> CryptoCore.encrypt -> StegoCore.embed -> pixelOrder` | hide_step_secret.xml |
| Optional exact passphrase | Hide / Secret; Reveal | `SecretStep.validPassphrase / takePassphrase -> HideWork.forge -> CryptoCore.derive -> GestureCredential.canonical; RevealActivity.unlock -> RevealWork.unlock -> same derive` | hide_step_secret.xml; activity_reveal.xml |
| Live measured capacity | Hide / Secret | `SecretStep.schedule -> measure -> HideWork.measure -> HideStore.measure -> PayloadCodec.pack -> GzipCodec.compress -> StegoCore.capacityBytes -> SecretStep.showCapacity` | hide_step_secret.xml |
| Motion gesture recording and fallback | Hide enrollment / Reveal | `GestureStep.begin / RevealGesture.begin -> SensorFusionManager.start -> onSensorChanged -> accelerometer / gyroscope -> emit -> GestureRecognizer.update -> Listener.onToken/onMotion -> GestureSession.addToken/onMotion` | hide_step_gesture.xml; activity_reveal.xml |
| Six-button Gesture Pad | Hide / Reveal | `GesturePadView.performClick -> OnTokenListener.onToken -> GestureSession.addToken -> enrollment / unlock callback` | hide_step_gesture.xml; activity_reveal.xml; values/gesture_ids.xml |
| Enroll twice and clear recordings | Hide / Gesture | `GestureStep.begin -> GestureSession.done -> GestureStep.finishAttempt -> GestureSession.matches -> takeTokens -> forge enabled; reset -> session.close` | hide_step_gesture.xml |
| Honest gesture strength | Hide / Gesture | `GestureSession.Listener.onProgress -> StrengthMeter.label -> bits -> GestureCredential.entropyBits` | hide_step_gesture.xml |
| Motion orb, dial and trail | Hide / Gesture; Home; Replay | `GestureRecognizer.Listener.onMotion -> KineticOrbView.setAngles; session.onProgress -> setTokenCount; onDraw -> drawDial / drawOrb` | hide_step_gesture.xml; activity_home.xml; Java replay dialog |
| Private enrolled-gesture replay | Hide / Gesture | `GestureStep replay click -> GestureReplay.show -> next -> KineticOrbView.setAngles/setTokenCount -> clear` | hide_step_gesture.xml; Java-built AlertDialog |
| Nerd Mode live telemetry | Hide / Reveal / Settings | `SettingsActivity.save -> VaultPreferences.save; GestureStep / RevealGesture -> NerdOverlay.begin -> SensorFusionManager.emit -> NerdOverlay.onSample/token -> render` | hide_step_gesture.xml; activity_reveal.xml; activity_settings.xml |
| Forge encrypted PNG | Hide / Forge | `HideActivity.forge -> HideWork.forge -> HideStore.forge -> HideForge.run -> VaultEngine.hide -> PayloadCodec.pack -> GzipCodec.compress -> CryptoCore.encrypt -> StegoCore.embed -> ImageUtils.fromPixels/savePng -> HideForge.finish -> VaultLogger.logHide` | hide_step_forge.xml; hide_step_result.xml |
| Forge stages, matrix rain and cancellation | Hide / Forge | `HideWork stage/progress callback -> HideActivity.progress -> MatrixRainView.setProgress; HideActivity.back -> confirmation -> HideWork.clearSensitive` | hide_step_forge.xml |
| Before/after wipe | Hide / Result | `ResultStep.show -> WipeSliderView.setImages -> WipeComparisonView.setImages; SeekBar.onProgressChanged -> setFraction -> onDraw` | hide_step_result.xml |
| Diff map, touch loupe and sweep replay | Hide / Result; Ghost comparison | `HideForge.finish / GhostComparisonWork.encode -> DiffMapData.build -> DiffMapView.setData/bindLoupe/startSweep -> touch -> inspect -> DiffMapData.original/encoded` | hide_step_result.xml; activity_ghost_comparison.xml |
| X-ray LSB visualization | Hide / Result; Scanner absent header | `DiffMapData.buildXRay -> XRayView.setPlane -> onDraw` | hide_step_result.xml; activity_scanner.xml |
| Changed pixels, max channel change and PSNR | Hide / Result; comparison; lab | `ImageUtils.statistics -> ImageStats.compare -> DiffStats.show; HarnessRunner.roundTrip -> report` | hide_step_result.xml; activity_ghost_comparison.xml; activity_test_harness.xml |
| Standard versus Ghost public demo | Ghost comparison | `ResultStep compare click -> GhostComparisonActivity.generate -> GhostComparisonWork.create -> encode -> VaultEngine.hide -> DiffMapData.build -> GhostComparisonActivity.show` | activity_ghost_comparison.xml |
| Save original encrypted PNG | Hide / Result | `ResultStep.exportOptions -> ExportSheet.show -> savePng / saveDocument -> HideWork.run -> HideExporter.saveGallery / saveDocument -> copy -> VaultLogger.logExport` | hide_step_result.xml; sheet_export.xml |
| Share encrypted PNG as file | Hide / Result | `ResultStep.warnShare -> prepareShare -> HideWork.run -> HideExporter.shareUri -> FileProvider -> ACTION_SEND chooser -> logShare` | hide_step_result.xml |
| Share comparison proof sheet | Hide / Result | `ResultStep.warnProof -> prepareShare(true) -> HideStore.proofUri -> ProofSheet.create -> ImageUtils.savePng -> HideExporter.shareUri -> ACTION_SEND` | hide_step_result.xml; sheet_export.xml; worker Canvas image |
| Choose original vault and inspect header | Reveal | `RevealActivity.load -> RevealWork.load -> ImageUtils.decodeExact -> ImageDecoder.exact -> PngIntegrity.read -> ImageUtils.pixels -> StegoCore.readHeader -> RevealLedger.capture -> VaultLogger.capture` | activity_reveal.xml |
| Authenticate and reveal text/file | Reveal | `RevealActivity.unlock -> RevealWork.unlock -> decrypt -> RevealExpiry.requireAllowed -> VaultEngine.reveal -> StegoCore.extract -> CryptoCore.decrypt -> GzipCodec.decompress -> PayloadCodec.unpack -> expiry recheck -> RevealLedger.revealed -> RevealActivity.unlocked` | activity_reveal.xml |
| Vault-door and text-scramble reveal | Reveal | `RevealActivity.unlocked -> VaultDoorView.open -> completed callback -> TextScrambler.show -> advance; denied -> VaultDoorView.deny` | activity_reveal.xml |
| Auto-lock and screenshot preference | Reveal / Hide / Settings | `SettingsActivity.save -> VaultPreferences.save; RevealActivity.screenOff.onReceive / onStop -> lock -> RevealWork.autoLock / clearSensitive / discardExport; HideActivity.onPause -> gesture.reset / work.clearSensitive` | activity_settings.xml; activity_reveal.xml; activity_hide.xml |
| Save decrypted file or text .txt | Reveal / Export sheet | `RevealActivity.exportOptions -> export(false) -> RevealWork.prepareExport -> RevealExport.create -> CreateDocument callback -> RevealWork.save -> RevealExport.save -> RevealLedger.exported` | activity_reveal.xml; sheet_export.xml |
| Open decrypted file with another app | Reveal | `RevealActivity open warning -> export(true) -> RevealWork.prepareExport -> RevealExport.create -> ACTION_VIEW via StartActivityForResult -> opened -> result discardExport` | activity_reveal.xml |
| Vault library with image/text rows | Vault | `VaultActivity.onResume -> load -> VaultRepository.search -> VaultEntry.read -> VaultActivity.show -> VaultAdapter.replace/getView -> thumbnail` | activity_vault.xml; vault_row.xml |
| Search title and filter creation dates | Vault | `VaultActivity.bindFilters -> VaultDialogs.range -> DatePickerDialog callbacks -> VaultActivity.load -> VaultRepository.search -> show` | activity_vault.xml |
| Open original from Vault list | Vault | `VaultActivity.bindList item click -> open -> Intent.setData(entry.imageUri) -> RevealActivity.load` | activity_vault.xml; vault_row.xml |
| Rename neutral local metadata | Vault / context menu | `VaultActivity.onCreateContextMenu -> onContextItemSelected -> VaultDialogs.rename -> VaultActivity.change -> VaultRepository.rename -> update` | activity_vault.xml; Java dialog |
| Set/clear expiry using date and time | Vault / context menu; Reveal | `VaultDialogs.expiry -> expiryDate -> DatePickerDialog -> TimePickerDialog -> VaultActivity.change -> VaultRepository.setExpiry; RevealWork.decrypt -> RevealExpiry.requireAllowed` | activity_vault.xml; Java dialogs; activity_reveal.xml |
| Delete library entry | Vault / context menu | `VaultActivity.onContextItemSelected -> confirmation -> change -> VaultRepository.softDelete -> update -> SyncManager.request` | activity_vault.xml; Java AlertDialog |
| Export history .csv | Vault / export sheet | `VaultActivity CSV launcher -> worker -> HistoryCsv.export -> VaultDatabaseHelper.getReadableDatabase/rawQuery -> ContentResolver.openOutputStream` | activity_vault.xml; sheet_export.xml |
| Scanner device/camera input | Scanner | `ScannerActivity OpenDocument / TakePicture callbacks -> scan -> decode -> ImageUtils.decodeExact -> StegoCore.readHeader -> showResult` | activity_scanner.xml |
| Receive shared/opened image and forward to Reveal | Scanner exported entry | `Android SEND/VIEW filter -> ScannerActivity.onCreate / onNewIntent -> receive -> scan -> decode -> showResult -> reveal -> RevealActivity` | activity_scanner.xml; AndroidManifest.xml |
| Offline interactive Learn and online reading | Learn | `LearnActivity.load(false) -> WebView.loadUrl(asset) -> HTML toggle listener; load(true) -> HTTPS Wikipedia WebView; WebViewClient callbacks -> showError` | activity_learn.xml; assets/how_it_works.html |
| About and team workstreams | About | `HomeActivity.overflow -> AboutActivity.onCreate -> InfoActivity.showPage` | activity_about.xml; values/settings_strings.xml |
| Sensitivity, pad and privacy settings | Settings | `SettingsActivity.populate/save -> VaultPreferences.load/save -> persist -> SensorFusionManager.start / gesture constructors / activity privacy checks` | activity_settings.xml; values/settings_strings.xml |
| Test server connection with entered values | Settings | `SettingsActivity.testConnection -> SyncManager.testConnection -> SyncClient.post(health) -> server/api.php health -> SELECT 1 -> main callback` | activity_settings.xml; values/sync_strings.xml |
| Cloud Ledger totals, queued states and manual retry | Home | `VaultApplication lifecycle -> SyncManager.start/request -> sync -> SyncStore.vaults/events -> SyncClient.post -> api.php -> MySQL -> finish/publish -> HomeActivity.showLedger; retry -> configurationChanged` | activity_home.xml; values/sync_strings.xml |
| Airplane banner and upload pause | Home / foreground app | `SyncManager.receiver.onReceive -> publish -> worker SyncStore.airplane -> request -> sync -> finish -> HomeActivity.showLedger` | activity_home.xml |
| Integrity Lab and temporary light preview | Integrity Lab | `TestHarnessActivity.startTest -> executeTest -> HarnessRunner.run -> runCase -> VaultEngine.hide -> save/reload -> VaultEngine.reveal -> report -> showReport; light switch -> AppCompatDelegate.setDefaultNightMode` | activity_test_harness.xml |
| Haptics, accessible feedback and safe screen edges | Across app | `VaultApplication onActivityResumed -> UiPolish.install -> buttons / enter; screen callbacks -> UiPolish.notice/failure; ViewCompat inset listener -> setPadding` | All activity layouts; styles.xml; dimens.xml; anim resources |

## Feature → source, storage and recovery

### 1. Normal launch and animated logo

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Splash — Normal launch and animated logo |
| Main classes/methods and full call chain | `SplashActivity.onCreate -> VaultPreferences.load -> SplashActivity.showLogo -> SplashLogoView.setProgress -> SplashActivity.route -> HomeActivity` |
| Layout / asset | Java-built blank/logo views; no splash XML |
| Data storage | Private preferences; animation state only in memory |
| Failure / recovery | Route waits until settings loaded and screen resumed. Closing removes delay/animation. |
| Security / limits | No vault credentials enter splash. Fresh process selects dark theme. |

### 2. Calculator disguise and launch code

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Calculator / Settings — Calculator disguise and launch code |
| Main classes/methods and full call chain | `SettingsActivity.save -> VaultPreferences.save -> DisguiseCode.create; SplashActivity.route -> CalculatorActivity.equalsPressed -> VaultPreferences.unlockDisguise -> DisguiseCode.matches -> HomeActivity` |
| Layout / asset | activity_calculator.xml; activity_settings.xml |
| Data storage | Salt:derived-hash in private SharedPreferences; calculator input only in memory |
| Failure / recovery | Invalid code format gives input error; wrong match calculates normally; new code must be re-entered after failed save. |
| Security / limits | 6..12 ASCII digits; separate from vault key; only launch routing is disguised; Scanner/icon remain visible. |

### 3. Four-operation calculator

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Calculator — Four-operation calculator |
| Main classes/methods and full call chain | `CalculatorActivity.press -> operator / calculate -> apply -> showNumber -> render` |
| Layout / asset | activity_calculator.xml |
| Data storage | In-memory StringBuilder and BigDecimal accumulator |
| Failure / recovery | Arithmetic/format errors -> fail -> clear/retry message. |
| Security / limits | Immediate execution with DECIMAL64, 16-character entry cap; pause clears input and invalidates code callback. |

### 4. Home navigation and More options

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Home — Home navigation and More options |
| Main classes/methods and full call chain | `HomeActivity.onCreate / overflow -> startActivity(new Intent(...)) -> selected Activity` |
| Layout / asset | activity_home.xml |
| Data storage | Back stack only |
| Failure / recovery | Standard Back returns; individual destinations show operation-specific recovery. |
| Security / limits | Explicit internal destinations; About available in popup; menu is built in Java. |

### 5. Select a cover from device

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Cover — Select a cover from device |
| Main classes/methods and full call chain | `CoverStep.openGallery -> OpenDocument result -> CoverStep.load -> HideWork.run -> HideStore.load -> ImageUtils.decodeCover -> ImageDecoder.cover` |
| Layout / asset | activity_hide.xml; hide_step_cover.xml |
| Data storage | Owned cover bitmap and <=512px display preview in HideStore |
| Failure / recovery | Picker/load failure offers another image or generated cover; Next stays disabled. |
| Security / limits | Cover may be sampled/flattened before embedding; this route is different from Reveal exact decoding. |

### 6. Take a cover photo

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Cover — Take a cover photo |
| Main classes/methods and full call chain | `CoverStep.openCamera -> HideWork.run -> HideStore.prepareCapture -> CoverStep.resume -> CameraContract.createIntent -> TakePicture result -> HideStore.finishCapture -> ImageUtils.decodeCover` |
| Layout / asset | hide_step_cover.xml |
| Data storage | Temporary JPEG cache/capture/, then owned cover bitmap |
| Failure / recovery | Camera cancellation/unavailable -> deleteCapture and choose/generate recovery. |
| Security / limits | External camera app; FileProvider read/write grant and ClipData; no CAMERA permission. This is an input cover, not vault output. |

### 7. Generate offline cover

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Cover — Generate offline cover |
| Main classes/methods and full call chain | `CoverStep.load -> HideWork.run -> HideStore.generate -> CoverGenerator.generate -> HideStore.adopt` |
| Layout / asset | hide_step_cover.xml |
| Data storage | 1024x1024 bitmap in memory, preview in HideStore |
| Failure / recovery | Memory/operation error offers smaller/readable input or retry. |
| Security / limits | Offline seeded procedural noise is cover artwork; it is not encryption randomness. |

### 8. Text or arbitrary file secret

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Secret — Text or arbitrary file secret |
| Main classes/methods and full call chain | `SecretStep.bind -> snapshot -> SecretInput.read -> encodeText / readFile -> PayloadCodec.Payload` |
| Layout / asset | hide_step_secret.xml |
| Data storage | Erasable character/byte buffers and retained prepared payload only in memory |
| Failure / recovery | Missing/oversize/unreadable content or long filename -> capacity/preparation notice; select smaller secret. |
| Security / limits | Radio buttons choose exactly one type; file basename/content are encrypted; no plaintext persistence during preparation. |

### 9. Import UTF-8 .txt

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Secret — Import UTF-8 .txt |
| Main classes/methods and full call chain | `SecretStep textPicker callback -> TextImport.read -> main consumer -> EditText.setText -> SecretStep.schedule` |
| Layout / asset | hide_step_secret.xml |
| Data storage | Bounded erasable worker character arrays and visible EditText |
| Failure / recovery | Malformed UTF-8/oversize/read failure -> txt_import_error; select smaller UTF-8 file. |
| Security / limits | No immutable imported plaintext String in importer; arrays cleared on delivery/cancel; source file already belongs to provider. |

### 10. Compression toggle

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Secret — Compression toggle |
| Main classes/methods and full call chain | `SecretStep.schedule -> HideWork.measure -> HideStore.measure -> PayloadCodec.pack -> GzipCodec.compress` |
| Layout / asset | hide_step_secret.xml |
| Data storage | Option in SecretInput/HideStore; GZIP bytes transient |
| Failure / recovery | Too large -> enable compression/reduce payload/change cover. |
| Security / limits | Both toggle states use GZIP; off selects Deflater.NO_COMPRESSION. Compression is not encryption. |

### 11. Ghost mode toggle

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Secret and engine — Ghost mode toggle |
| Main classes/methods and full call chain | `SecretStep.snapshot -> HideStore.measure -> HideStore.forge -> HideForge.run -> VaultEngine.hide -> CryptoCore.encrypt -> StegoCore.embed -> pixelOrder` |
| Layout / asset | hide_step_secret.xml |
| Data storage | Public Ghost flag/salt in PNG header; temporary shuffle array |
| Failure / recovery | Image memory/capacity failures show operation recovery. |
| Security / limits | Salt-seeded scattering is public and reproducible; AES-GCM provides confidentiality/authentication. Header remains sequential. |

### 12. Optional exact passphrase

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Secret; Reveal — Optional exact passphrase |
| Main classes/methods and full call chain | `SecretStep.validPassphrase / takePassphrase -> HideWork.forge -> CryptoCore.derive -> GestureCredential.canonical; RevealActivity.unlock -> RevealWork.unlock -> same derive` |
| Layout / asset | hide_step_secret.xml; activity_reveal.xml |
| Data storage | Erasable char arrays during operation; only passphrase-present flag in image/metadata |
| Failure / recovery | Checked-but-empty option gives field error; wrong credentials or damaged data give neutral denial. |
| Security / limits | Case/spaces/UTF-16 units preserved exactly; no trimming/normalization; recommend strong unique phrase. |

### 13. Live measured capacity

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Secret — Live measured capacity |
| Main classes/methods and full call chain | `SecretStep.schedule -> measure -> HideWork.measure -> HideStore.measure -> PayloadCodec.pack -> GzipCodec.compress -> StegoCore.capacityBytes -> SecretStep.showCapacity` |
| Layout / asset | hide_step_secret.xml |
| Data storage | Transient compressed/packed data, then prepared Payload if keep=true and fits |
| Failure / recovery | Too large disables Next and suggests smaller secret/compression/larger cover; async stale results ignored. |
| Security / limits | Counts actual GZIP bytes +16-byte tag; does not claim raw file size equals image capacity. |

### 14. Motion gesture recording and fallback

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide enrollment / Reveal — Motion gesture recording and fallback |
| Main classes/methods and full call chain | `GestureStep.begin / RevealGesture.begin -> SensorFusionManager.start -> onSensorChanged -> accelerometer / gyroscope -> emit -> GestureRecognizer.update -> Listener.onToken/onMotion -> GestureSession.addToken/onMotion` |
| Layout / asset | hide_step_gesture.xml; activity_reveal.xml |
| Data storage | Transient motion arrays and 8-token session |
| Failure / recovery | Missing gyro -> accelerometer-only notice; no accelerometer -> select pad and begin again; short gesture stays editable. |
| Security / limits | Only named token sequence derives key, not a biometric motion trace. Sensors stop and sessions clear on pause/reset. |

### 15. Six-button Gesture Pad

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Reveal — Six-button Gesture Pad |
| Main classes/methods and full call chain | `GesturePadView.performClick -> OnTokenListener.onToken -> GestureSession.addToken -> enrollment / unlock callback` |
| Layout / asset | hide_step_gesture.xml; activity_reveal.xml; values/gesture_ids.xml |
| Data storage | Same transient token representation as motion |
| Failure / recovery | Rejects rapid <300ms presses and undersized hit cells; requires active recording and Done after >=4 moves. |
| Security / limits | Keyboard/accessibility actions share token semantics and accepted-move haptics; no stored gesture. |

### 16. Enroll twice and clear recordings

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Gesture — Enroll twice and clear recordings |
| Main classes/methods and full call chain | `GestureStep.begin -> GestureSession.done -> GestureStep.finishAttempt -> GestureSession.matches -> takeTokens -> forge enabled; reset -> session.close` |
| Layout / asset | hide_step_gesture.xml |
| Data storage | Two transient sessions, then enrolled byte[] |
| Failure / recovery | Mismatch clears both and asks exact repeat; <=3 moves asks for more; max8 accepted. |
| Security / limits | Requires distinct completed performances; no global gesture account or stored recovery key. |

### 17. Honest gesture strength

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Gesture — Honest gesture strength |
| Main classes/methods and full call chain | `GestureSession.Listener.onProgress -> StrengthMeter.label -> bits -> GestureCredential.entropyBits` |
| Layout / asset | hide_step_gesture.xml |
| Data storage | Display only |
| Failure / recovery | Incomplete label gives remaining-move count. |
| Security / limits | Upper bound tokenCount*log2(6); human choice may be weaker; passphrase entropy is not estimated. |

### 18. Motion orb, dial and trail

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Gesture; Home; Replay — Motion orb, dial and trail |
| Main classes/methods and full call chain | `GestureRecognizer.Listener.onMotion -> KineticOrbView.setAngles; session.onProgress -> setTokenCount; onDraw -> drawDial / drawOrb` |
| Layout / asset | hide_step_gesture.xml; activity_home.xml; Java replay dialog |
| Data storage | 24-point transient trail; UI values |
| Failure / recovery | Reset/detach clears/stops; sensor fallback explains unavailable motion. |
| Security / limits | Decoration/count feedback; Home orb does not record input or represent a secret. |

### 19. Private enrolled-gesture replay

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Gesture — Private enrolled-gesture replay |
| Main classes/methods and full call chain | `GestureStep replay click -> GestureReplay.show -> next -> KineticOrbView.setAngles/setTokenCount -> clear` |
| Layout / asset | hide_step_gesture.xml; Java-built AlertDialog |
| Data storage | Temporary enrolled-token clone only |
| Failure / recovery | Unavailable until matched enrollment; dismissal/reset/pause cancels and wipes. |
| Security / limits | Opt-in sensitive disclosure; secure dialog/window; no recovery after leaving current enrollment. |

### 20. Nerd Mode live telemetry

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Reveal / Settings — Nerd Mode live telemetry |
| Main classes/methods and full call chain | `SettingsActivity.save -> VaultPreferences.save; GestureStep / RevealGesture -> NerdOverlay.begin -> SensorFusionManager.emit -> NerdOverlay.onSample/token -> render` |
| Layout / asset | hide_step_gesture.xml; activity_reveal.xml; activity_settings.xml |
| Data storage | Transient sample string/token buffer, preference boolean |
| Failure / recovery | Accelerometer fallback/pad state labelled; clear on stop/reset/finish. |
| Security / limits | Shows tokens to nearby viewers; forces Hide/Reveal screenshot protection; never exported to ledger. |

### 21. Forge encrypted PNG

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Forge — Forge encrypted PNG |
| Main classes/methods and full call chain | `HideActivity.forge -> HideWork.forge -> HideStore.forge -> HideForge.run -> VaultEngine.hide -> PayloadCodec.pack -> GzipCodec.compress -> CryptoCore.encrypt -> StegoCore.embed -> ImageUtils.fromPixels/savePng -> HideForge.finish -> VaultLogger.logHide` |
| Layout / asset | hide_step_forge.xml; hide_step_result.xml |
| Data storage | Original PNG cache/shared/, retained encrypted copy files/vaults/, SQLite metadata/event |
| Failure / recovery | Worker exceptions/OOM return to Secret with recovery; failed result assets/output cleaned; repeat enrollment/input to retry. |
| Security / limits | Credential ownership transfers to worker and cleanup wipes arrays; generated salt/IV fresh; only RGB LSBs change. |

### 22. Forge stages, matrix rain and cancellation

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Forge — Forge stages, matrix rain and cancellation |
| Main classes/methods and full call chain | `HideWork stage/progress callback -> HideActivity.progress -> MatrixRainView.setProgress; HideActivity.back -> confirmation -> HideWork.clearSensitive` |
| Layout / asset | hide_step_forge.xml |
| Data storage | UI progress and generation counter in memory |
| Failure / recovery | Cancellation invalidates result/progress, clears prepared secret and returns to Secret; pause similarly aborts sensitive session. |
| Security / limits | KDF/crypto indeterminate when total=0; rain is decoration and does not invent percentage or promise immediate PBKDF2 interruption. |

### 23. Before/after wipe

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Result — Before/after wipe |
| Main classes/methods and full call chain | `ResultStep.show -> WipeSliderView.setImages -> WipeComparisonView.setImages; SeekBar.onProgressChanged -> setFraction -> onDraw` |
| Layout / asset | hide_step_result.xml |
| Data storage | Display-only thumbnails owned by HideStore result |
| Failure / recovery | Null images render nothing; closing detaches references. |
| Security / limits | Not extraction inputs; saved/shared vault comes from original PNG file. |

### 24. Diff map, touch loupe and sweep replay

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Result; Ghost comparison — Diff map, touch loupe and sweep replay |
| Main classes/methods and full call chain | `HideForge.finish / GhostComparisonWork.encode -> DiffMapData.build -> DiffMapView.setData/bindLoupe/startSweep -> touch -> inspect -> DiffMapData.original/encoded` |
| Layout / asset | hide_step_result.xml; activity_ghost_comparison.xml |
| Data storage | Exact before/after arrays plus <=512px OR-binned diff bitmap |
| Failure / recovery | Host operation failures offer retry; stale demo assets disposed; sweeps stop when paused/hidden. |
| Security / limits | Label shows exact source RGB/LSB; magnifying glass enlarges preview bins; reveal locations/cover but no decrypted payload. |

### 25. X-ray LSB visualization

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Result; Scanner absent header — X-ray LSB visualization |
| Main classes/methods and full call chain | `DiffMapData.buildXRay -> XRayView.setPlane -> onDraw` |
| Layout / asset | hide_step_result.xml; activity_scanner.xml |
| Data storage | Sampled <=512px bitmap from original pixel LSBs |
| Failure / recovery | Image errors offer original-image retry; bitmap recycled on replacement/close. |
| Security / limits | Natural images have LSB noise; patterns do not prove hidden data. Scanner hides X-ray when valid header found. |

### 26. Changed pixels, max channel change and PSNR

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Result; comparison; lab — Changed pixels, max channel change and PSNR |
| Main classes/methods and full call chain | `ImageUtils.statistics -> ImageStats.compare -> DiffStats.show; HarnessRunner.roundTrip -> report` |
| Layout / asset | hide_step_result.xml; activity_ghost_comparison.xml; activity_test_harness.xml |
| Data storage | Numeric result only |
| Failure / recovery | Mismatched/empty arrays reject with IllegalArgumentException; identical pixels produce infinity dB. |
| Security / limits | Exact RGB image-quality measurements; neither authenticity nor encryption-strength proof. |

### 27. Standard versus Ghost public demo

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Ghost comparison — Standard versus Ghost public demo |
| Main classes/methods and full call chain | `ResultStep compare click -> GhostComparisonActivity.generate -> GhostComparisonWork.create -> encode -> VaultEngine.hide -> DiffMapData.build -> GhostComparisonActivity.show` |
| Layout / asset | activity_ghost_comparison.xml |
| Data storage | Ephemeral public demo only |
| Failure / recovery | Generate again recovery; late results disposed; arrays wiped/recycled. |
| Security / limits | Same cover/content/credentials but separate encryption calls have independent salt/IV; changes need not match counts. Does not reuse user secrets. |

### 28. Save original encrypted PNG

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Result — Save original encrypted PNG |
| Main classes/methods and full call chain | `ResultStep.exportOptions -> ExportSheet.show -> savePng / saveDocument -> HideWork.run -> HideExporter.saveGallery / saveDocument -> copy -> VaultLogger.logExport` |
| Layout / asset | hide_step_result.xml; sheet_export.xml |
| Data storage | Gallery API29+ or selected document destination; local EXPORT event |
| Failure / recovery | Storage/provider failure offers another destination; completed file with event failure has distinct warning. |
| Security / limits | Original file bytes copied without recompression; below API29 use document destination; MediaStore pending row removed if incomplete. |

### 29. Share encrypted PNG as file

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Result — Share encrypted PNG as file |
| Main classes/methods and full call chain | `ResultStep.warnShare -> prepareShare -> HideWork.run -> HideExporter.shareUri -> FileProvider -> ACTION_SEND chooser -> logShare` |
| Layout / asset | hide_step_result.xml |
| Data storage | Original PNG cache/shared/ plus EXPORT event |
| Failure / recovery | No receiver -> save first and attach as Document/File; URI/export failure offers retry. |
| Security / limits | Warning instructs Document/File; MIME image/png and read grant cannot force receiver to preserve pixels. |

### 30. Share comparison proof sheet

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Hide / Result — Share comparison proof sheet |
| Main classes/methods and full call chain | `ResultStep.warnProof -> prepareShare(true) -> HideStore.proofUri -> ProofSheet.create -> ImageUtils.savePng -> HideExporter.shareUri -> ACTION_SEND` |
| Layout / asset | hide_step_result.xml; sheet_export.xml; worker Canvas image |
| Data storage | Cached 1080x660 display PNG cache/shared/ |
| Failure / recovery | Failed rendering/output removed and export recovery shown. |
| Security / limits | Warns cover/statistics/coarse location disclosure; preview RGB LSBs cleared, density binned16x16; not unlockable vault or cryptographic proof. |

### 31. Choose original vault and inspect header

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Reveal — Choose original vault and inspect header |
| Main classes/methods and full call chain | `RevealActivity.load -> RevealWork.load -> ImageUtils.decodeExact -> ImageDecoder.exact -> PngIntegrity.read -> ImageUtils.pixels -> StegoCore.readHeader -> RevealLedger.capture -> VaultLogger.capture` |
| Layout / asset | activity_reveal.xml |
| Data storage | Original encrypted pixels in memory, private encrypted image copy and SQLite identity |
| Failure / recovery | No signature vs unreadable image vs metadata recording failure get separate recovery strings. |
| Security / limits | No resize/EXIF rotation/density transform; opacity/size/memory checks reject unsupported originals. Header discovery alone is unauthenticated. |

### 32. Authenticate and reveal text/file

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Reveal — Authenticate and reveal text/file |
| Main classes/methods and full call chain | `RevealActivity.unlock -> RevealWork.unlock -> decrypt -> RevealExpiry.requireAllowed -> VaultEngine.reveal -> StegoCore.extract -> CryptoCore.decrypt -> GzipCodec.decompress -> PayloadCodec.unpack -> expiry recheck -> RevealLedger.revealed -> RevealActivity.unlocked` |
| Layout / asset | activity_reveal.xml |
| Data storage | Payload/text only in live memory; metadata/type/size/event in SQLite |
| Failure / recovery | Denied count increments and recording resets; wrong credential and corrupted authentication data share neutral denial; no lockout timer is implemented. |
| Security / limits | AES-GCM authenticates header/ciphertext before unpacking; exact original tokens/passphrase required; no recovery master key. |

### 33. Vault-door and text-scramble reveal

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Reveal — Vault-door and text-scramble reveal |
| Main classes/methods and full call chain | `RevealActivity.unlocked -> VaultDoorView.open -> completed callback -> TextScrambler.show -> advance; denied -> VaultDoorView.deny` |
| Layout / asset | activity_reveal.xml |
| Data storage | Transient UI animation and erasable char arrays |
| Failure / recovery | Denial shake/haptics; lock/destroy cancels effects and clears text. |
| Security / limits | Animations follow successful authentication; they are not additional cryptography. TextScrambler clears owned arrays, platform TextView may retain copies. |

### 34. Auto-lock and screenshot preference

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Reveal / Hide / Settings — Auto-lock and screenshot preference |
| Main classes/methods and full call chain | `SettingsActivity.save -> VaultPreferences.save; RevealActivity.screenOff.onReceive / onStop -> lock -> RevealWork.autoLock / clearSensitive / discardExport; HideActivity.onPause -> gesture.reset / work.clearSensitive` |
| Layout / asset | activity_settings.xml; activity_reveal.xml; activity_hide.xml |
| Data storage | Preference flags; AUTOLOCK event if applicable |
| Failure / recovery | Re-record/re-enter credentials; source URI may remain selected; destruction always clears. |
| Security / limits | Auto-lock configurable; active recordings always clear. FLAG_SECURE is Android window protection, not guaranteed secrecy against camera/compromised OS. |

### 35. Save decrypted file or text .txt

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Reveal / Export sheet — Save decrypted file or text .txt |
| Main classes/methods and full call chain | `RevealActivity.exportOptions -> export(false) -> RevealWork.prepareExport -> RevealExport.create -> CreateDocument callback -> RevealWork.save -> RevealExport.save -> RevealLedger.exported` |
| Layout / asset | activity_reveal.xml; sheet_export.xml |
| Data storage | Temporary plaintext cache/reveal/ UUID file then chosen plaintext destination |
| Failure / recovery | Cancelled picker discards temporary copy and gives retry guidance; failed destination suggests another folder/unlock. |
| Security / limits | Explicit plaintext disclosure; text MIME text/plain, file MIME inferred; chosen saved file is outside vault protection. |

### 36. Open decrypted file with another app

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Reveal — Open decrypted file with another app |
| Main classes/methods and full call chain | `RevealActivity open warning -> export(true) -> RevealWork.prepareExport -> RevealExport.create -> ACTION_VIEW via StartActivityForResult -> opened -> result discardExport` |
| Layout / asset | activity_reveal.xml |
| Data storage | Temporary plaintext in cache/reveal/, receiver can retain its own copy |
| Failure / recovery | Missing viewer -> Save with compatible app; temp removed on return/cancel/close; old-process orphan cleanup runs on next Reveal. |
| Security / limits | Read grant/ClipData plus disclosure warning; deletion is not secure erasure and cannot retract third-party copies. |

### 37. Vault library with image/text rows

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Vault — Vault library with image/text rows |
| Main classes/methods and full call chain | `VaultActivity.onResume -> load -> VaultRepository.search -> VaultEntry.read -> VaultActivity.show -> VaultAdapter.replace/getView -> thumbnail` |
| Layout / asset | activity_vault.xml; vault_row.xml |
| Data storage | SQLite records and thumbnail BLOBs; encrypted files/vaults/; 4MiB thumbnail cache |
| Failure / recovery | Designed empty/error panels: Hide first vault, clear filters, or Retry/free resources. |
| Security / limits | Generic initial titles; stored metadata is not encrypted by payload key; no plaintext thumbnails of secret files. |

### 38. Search title and filter creation dates

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Vault — Search title and filter creation dates |
| Main classes/methods and full call chain | `VaultActivity.bindFilters -> VaultDialogs.range -> DatePickerDialog callbacks -> VaultActivity.load -> VaultRepository.search -> show` |
| Layout / asset | activity_vault.xml |
| Data storage | Filters only saved in Activity Bundle; query in SQLite |
| Failure / recovery | No matches -> change title/clear filters; DB error -> Retry. |
| Security / limits | Literal title LIKE escaped; selection parameters used; date start inclusive, end exclusive next midnight. |

### 39. Open original from Vault list

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Vault — Open original from Vault list |
| Main classes/methods and full call chain | `VaultActivity.bindList item click -> open -> Intent.setData(entry.imageUri) -> RevealActivity.load` |
| Layout / asset | activity_vault.xml; vault_row.xml |
| Data storage | Existing private encrypted file URI |
| Failure / recovery | Opening error offers Retry or choose separately exported original PNG. |
| Security / limits | Passes image address, never prior plaintext/credentials. |

### 40. Rename neutral local metadata

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Vault / context menu — Rename neutral local metadata |
| Main classes/methods and full call chain | `VaultActivity.onCreateContextMenu -> onContextItemSelected -> VaultDialogs.rename -> VaultActivity.change -> VaultRepository.rename -> update` |
| Layout / asset | activity_vault.xml; Java dialog |
| Data storage | Local SQLite title and dirty/revision state |
| Failure / recovery | Empty/over80 title rejected; failure asks retry and apply change again. |
| Security / limits | Title is plaintext metadata; warning asks neutral labels; title is excluded from cloud payload. |

### 41. Set/clear expiry using date and time

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Vault / context menu; Reveal — Set/clear expiry using date and time |
| Main classes/methods and full call chain | `VaultDialogs.expiry -> expiryDate -> DatePickerDialog -> TimePickerDialog -> VaultActivity.change -> VaultRepository.setExpiry; RevealWork.decrypt -> RevealExpiry.requireAllowed` |
| Layout / asset | activity_vault.xml; Java dialogs; activity_reveal.xml |
| Data storage | Nullable expires_at in SQLite and cloud metadata |
| Failure / recovery | Expired unlock asks Vault change/clear then choose image again. |
| Security / limits | Device-clock/local unlock policy only; does not erase PNG, expire remote copies, or close already open content. |

### 42. Delete library entry

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Vault / context menu — Delete library entry |
| Main classes/methods and full call chain | `VaultActivity.onContextItemSelected -> confirmation -> change -> VaultRepository.softDelete -> update -> SyncManager.request` |
| Layout / asset | activity_vault.xml; Java AlertDialog |
| Data storage | deleted flag and revision in SQLite; server tombstone |
| Failure / recovery | Mutation failure offers retry; cancel does nothing. |
| Security / limits | Soft deletion retains encrypted image/event history and exported copies; no secure deletion claim. |

### 43. Export history .csv

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Vault / export sheet — Export history .csv |
| Main classes/methods and full call chain | `VaultActivity CSV launcher -> worker -> HistoryCsv.export -> VaultDatabaseHelper.getReadableDatabase/rawQuery -> ContentResolver.openOutputStream` |
| Layout / asset | activity_vault.xml; sheet_export.xml |
| Data storage | Chosen UTF-8 CSV of local events, no new secret store |
| Failure / recovery | Picker/unwritable destination -> choose destination and retry. |
| Security / limits | Exports IDs/type/timestamps only, not credentials/names/content; quoting and formula prefix mitigate spreadsheet interpretation. |

### 44. Scanner device/camera input

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Scanner — Scanner device/camera input |
| Main classes/methods and full call chain | `ScannerActivity OpenDocument / TakePicture callbacks -> scan -> decode -> ImageUtils.decodeExact -> StegoCore.readHeader -> showResult` |
| Layout / asset | activity_scanner.xml |
| Data storage | Original URI; transient pixel/X-ray; temporary capture JPEG |
| Failure / recovery | Unsupported URI/image -> choose accessible opaque original within memory budget; camera unavailable -> device picker. |
| Security / limits | Capturing a screen/print does not preserve vault LSBs; exact decoding does not promise PNG-only input, supported opaque originals may be explored. |

### 45. Receive shared/opened image and forward to Reveal

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Scanner exported entry — Receive shared/opened image and forward to Reveal |
| Main classes/methods and full call chain | `Android SEND/VIEW filter -> ScannerActivity.onCreate / onNewIntent -> receive -> scan -> decode -> showResult -> reveal -> RevealActivity` |
| Layout / asset | activity_scanner.xml; AndroidManifest.xml |
| Data storage | Incoming content URI and transient image processing |
| Failure / recovery | Null/non-content URI invalidates old result; absent header asks original PNG and shows exploratory X-ray. |
| Security / limits | Valid structural header only; authentication requires Reveal. Disguise does not disable this entry. |

### 46. Offline interactive Learn and online reading

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Learn — Offline interactive Learn and online reading |
| Main classes/methods and full call chain | `LearnActivity.load(false) -> WebView.loadUrl(asset) -> HTML toggle listener; load(true) -> HTTPS Wikipedia WebView; WebViewClient callbacks -> showError` |
| Layout / asset | activity_learn.xml; assets/how_it_works.html |
| Data storage | Bundled asset and transient WebView state; only remote boolean saved |
| Failure / recovery | Remote/load errors -> designed Retry or Offline explainer. |
| Security / limits | JS enabled only for asset; file/content access off, mixed content blocked, main-frame navigation limited to HTTPS en.wikipedia.org. |

### 47. About and team workstreams

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | About — About and team workstreams |
| Main classes/methods and full call chain | `HomeActivity.overflow -> AboutActivity.onCreate -> InfoActivity.showPage` |
| Layout / asset | activity_about.xml; values/settings_strings.xml |
| Data storage | Bundled text resources |
| Failure / recovery | Back returns to previous Activity. |
| Security / limits | Static app/workstream explanation; no data collection. |

### 48. Sensitivity, pad and privacy settings

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Settings — Sensitivity, pad and privacy settings |
| Main classes/methods and full call chain | `SettingsActivity.populate/save -> VaultPreferences.load/save -> persist -> SensorFusionManager.start / gesture constructors / activity privacy checks` |
| Layout / asset | activity_settings.xml; values/settings_strings.xml |
| Data storage | Private named SharedPreferences and immutable Values snapshot |
| Failure / recovery | Invalid URL/code/save failure gives field/Snackbar recovery; new code input wiped regardless. |
| Security / limits | Server API key is stored as private preference String; calculator hash separate; vault keys/gestures/passphrases never settings. |

### 49. Test server connection with entered values

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Settings — Test server connection with entered values |
| Main classes/methods and full call chain | `SettingsActivity.testConnection -> SyncManager.testConnection -> SyncClient.post(health) -> server/api.php health -> SELECT 1 -> main callback` |
| Layout / asset | activity_settings.xml; values/sync_strings.xml |
| Data storage | No automatic save of entered config; existing installation device_id may be created |
| Failure / recovery | Timeout/auth/network/server failure -> check airplane/URL/API key; stale UI request ignored. |
| Security / limits | Requires service=kinetic-vault/version1; API key header is not an encryption key; HTTP only permitted debug hosts. |

### 50. Cloud Ledger totals, queued states and manual retry

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Home — Cloud Ledger totals, queued states and manual retry |
| Main classes/methods and full call chain | `VaultApplication lifecycle -> SyncManager.start/request -> sync -> SyncStore.vaults/events -> SyncClient.post -> api.php -> MySQL -> finish/publish -> HomeActivity.showLedger; retry -> configurationChanged` |
| Layout / asset | activity_home.xml; values/sync_strings.xml |
| Data storage | SQLite queue/state/revision; metadata_sync preferences; MySQL allowlisted metadata/events |
| Failure / recovery | Offline/pending with local features usable; persistent exponential retry; cached totals labelled; manual retry resets backoff. |
| Security / limits | Uploads no image bytes/title/URI/thumbnail/secret filename/keys/gesture/passphrase/location/salt/IV; metadata still reveals usage/hash/sizes. Not image backup. |

### 51. Airplane banner and upload pause

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Home / foreground app — Airplane banner and upload pause |
| Main classes/methods and full call chain | `SyncManager.receiver.onReceive -> publish -> worker SyncStore.airplane -> request -> sync -> finish -> HomeActivity.showLedger` |
| Layout / asset | activity_home.xml |
| Data storage | AIRPLANE_ON/OFF in sync_events outbox then server events |
| Failure / recovery | Airplane mode stops uploads/retry button; turn off and keep app open to resume. |
| Security / limits | Receiver registered only while an Activity started; in-flight request may run until current network check/timeout. No location or background service. |

### 52. Integrity Lab and temporary light preview

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Integrity Lab — Integrity Lab and temporary light preview |
| Main classes/methods and full call chain | `TestHarnessActivity.startTest -> executeTest -> HarnessRunner.run -> runCase -> VaultEngine.hide -> save/reload -> VaultEngine.reveal -> report -> showReport; light switch -> AppCompatDelegate.setDefaultNightMode` |
| Layout / asset | activity_test_harness.xml |
| Data storage | Public fixture buffers, temp PNGs deleted per case; UI report/preview only |
| Failure / recovery | Memory/IO/crypto/unexpected error identifies stage and asks Run again; destruction interrupts worker. |
| Security / limits | Uses public fixture credentials/text/binary content; no enrolled user secrets or cloud metadata. This index describes the feature; it does not report execution. |

### 53. Haptics, accessible feedback and safe screen edges

| Item | Source-backed explanation |
| --- | --- |
| Screen / what is visible | Across app — Haptics, accessible feedback and safe screen edges |
| Main classes/methods and full call chain | `VaultApplication onActivityResumed -> UiPolish.install -> buttons / enter; screen callbacks -> UiPolish.notice/failure; ViewCompat inset listener -> setPadding` |
| Layout / asset | All activity layouts; styles.xml; dimens.xml; anim resources |
| Data storage | View-only state |
| Failure / recovery | Recovery messages identify next action; click/touch feedback follows enabled controls. |
| Security / limits | Some shared transitions respect disabled system animations; not every custom effect has a reduced-motion guard. No implemented Toast call was found in the read Java sources; Snackbar/status text supply current notices. |

## Syllabus locations visible in current source

| Construct | Actual feature location |
| --- | --- |
| Checkboxes / radio buttons / spinner | Hide options and pad; text/file pair; Settings sensitivity. |
| Options / context menus | HomeActivity.overflow PopupMenu; Vault long-press context menu. |
| Date / time pickers | Vault date-range and expiry date/time dialogs. |
| Touch listener / custom Canvas | DiffMapView.bindLoupe/touch; GesturePadView; orb/door/maps/rain/wipe/logo. Canvas is Android's drawing surface; Paint carries drawing style. |
| Explicit / implicit intents / Activity Result API | Internal navigation; PNG SEND, plaintext VIEW, OpenDocument, CreateDocument, TakePicture, external SEND/VIEW receiver. |
| AlertDialog / bottom sheet | Forge/share/delete/disclosure confirmations; ExportSheet. An AlertDialog is a modal question; a bottom sheet is an action panel rising from the lower edge. |
| Device images / .txt / .csv / ListView image+text | Cover/Reveal/Scanner; TextImport/Reveal text export; HistoryCsv; VaultAdapter/vault_row. |
| SQLite / MySQL via PHP | VaultRepository/VaultDatabaseHelper; SyncManager/SyncClient and server/api.php. |
| WebView asset + remote / broadcast receivers | LearnActivity; Reveal SCREEN_OFF; foreground SyncManager AIRPLANE_MODE_CHANGED. A WebView embeds web content; a receiver responds to Android event messages. |
| Toasts and snackbars | Snackbar calls and designed status panels exist. No Toast invocation appears in the read Java sources; do not present it as implemented. |

## Small real callback examples

A lambda is a compact callback implementation. From ui/HomeActivity.java, onCreate:

```java
findViewById(R.id.home_scan).setOnClickListener(v -> startActivity(new Intent(this, ScannerActivity.class)));
```

A lifecycle method is called by Android when screen visibility changes. From ui/ScannerActivity.java:

```java
@Override protected void onPause() { foreground = false; super.onPause(); }
```

These examples explain the navigation and lifecycle terms used in the tables; detailed class state, threading and Android/Java construct explanations are in 00/02. `@Override` marks a replacement implementation of a parent/interface method; `super` calls the parent implementation.

## Likely viva questions

1. **Does Scanner unlock an image?** It only detects a structurally valid header; Reveal authenticates/decrypts using credentials.
2. **Where do I demonstrate SQLite CRUD?** Hide/Reveal create records, Vault reads them, rename/expiry update them and Delete sets a soft-deletion flag.
3. **Where do radio buttons and spinner occur?** Hide chooses Text/File with radios; Settings chooses gesture sensitivity with a spinner.
4. **Which exports contain plaintext?** Reveal Save/Open exports; Hide PNG is encrypted, while comparison sheets disclose cover/visual metadata and CSV discloses event metadata.
5. **Does Cloud Ledger restore my PNGs?** No. It syncs allowlisted metadata/events, not images or secret content.

## Covers these files

- `app/src/main/AndroidManifest.xml`
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
- `server/.gitignore`
- `server/.htaccess`
- `server/api.php`
- `server/config.sample.php`
- `server/README_XAMPP.md`
- `server/schema.sql`
- `app/src/main/assets/how_it_works.html`
