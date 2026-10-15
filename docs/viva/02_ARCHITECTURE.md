# 02 — Architecture

## Big picture in plain English

Kinetic Vault has a reusable engine wrapped by Android screens. The engine handles exact byte/pixel transformations synchronously: synchronous means a method finishes its operation before returning. Android helpers call it on a worker thread (a separate sequence of execution) so the UI thread (the main sequence that handles taps and drawing) remains responsive.

Encryption changes readable plaintext into ciphertext using a key. Authentication checks that ciphertext and associated metadata have not changed. Steganography hides those ciphertext bytes in image pixels. A PNG is a lossless image format; a pixel is one image cell; its RGB channels are red, green and blue amounts from0 to255. A bit is a binary digit; an LSB (least significant bit) is the lowest-valued digit in a channel. Changing that digit changes the channel by at most1. The phone movement produces named tokens (small integer identifiers), not a stored motion recording or biometric signature. An optional exact passphrase strengthens the credential.

Java paths throughout this document are relative to `app/src/main/java/com/example/kineticvault/`, unless a full project path is shown. Resource paths are under `app/src/main/res/`. The frozen core is `core/`. A facade is a small public front door to several helpers: ImageUtils is the image facade. A controller coordinates a screen step: CoverStep/SecretStep/GestureStep/ResultStep are controllers owned by HideActivity. The UI is Java + XML with findViewById; there is no separate ViewModel/MVVM layer or new runtime dependency to explain.

## Hide pipeline: real class-by-class flow

An Activity is an Android screen; a callback is a function called later when an action finishes. An Intent is an Android request to launch another screen/app. A URI (Android Uri) identifies a provider item/file; ContentResolver asks the owning provider for streams. A Bitmap is an Android image object; ARGB_8888 represents alpha (opacity), red, green and blue in8 bits each. Opaque means alpha255. sRGB is the colour space requested by the decoder. A thumbnail is a reduced display copy and is never the extraction source.

/
```text
1. HomeActivity click -> explicit Intent -> ui/hide/HideActivity.onCreate
   -> constructs HideWork + CoverStep + SecretStep + GestureStep + ResultStep

2. CoverStep gallery callback / camera result / Generate
   -> HideWork.run [single worker]
   -> HideStore.load / finishCapture / generate
   -> ImageUtils.decodeCover -> ImageDecoder.cover
      OR CoverGenerator.generate(1024,1024,System.nanoTime())
   -> HideStore.adopt -> full prepared opaque cover + display thumbnail
   -> Handler main callback -> CoverStep preview -> HideActivity.showStep(1)

3. SecretStep edits/radio/checkbox/import -> schedule (300ms debounce)
   -> snapshot -> SecretInput(char[], file Uri, compress, ghost)
   -> HideWork.measure -> HideStore.measure
   -> SecretInput.read -> strict UTF-8 text OR bounded file read
   -> PayloadCodec.Payload(type,basename,data)
   -> PayloadCodec.pack -> GzipCodec.compress
   -> needed = actual GZIP bytes +16-byte GCM tag
   -> available = StegoCore.capacityBytes(cover pixel count)
   -> main -> SecretStep.showCapacity

4. SecretStep.prepare -> same measurement with keep=true
   -> HideStore retains fitting Payload, compression and Ghost selection
   -> main -> HideActivity.showStep(2)
   -> GestureStep.begin twice
   -> SensorFusionManager -> GestureRecognizer -> GestureSession
      OR GesturePadView -> GestureSession
   -> GestureSession.matches -> matched enrolled token array
   -> optional private GestureReplay; Forge enabled

5. HideActivity.forge -> takeTokens + takePassphrase -> showStep(3)
   -> HideWork.forge [single worker; generation guards]
   -> HideStore.forge -> HideForge.run
   -> ImageUtils.pixels(cover)
   -> VaultEngine.hide
      a. PayloadCodec.pack
      b. GzipCodec.compress (GZIP exists even when compression off)
      c. capacity guard before key derivation
      d. CryptoCore.encrypt
         -> SecureRandom salt/IV -> VaultHeader.create
         -> GestureCredential.canonical -> PBKDF2 -> AES-GCM
      e. StegoCore.embed (sequential header, Standard/Ghost ciphertext pixels)
   -> int[] encoded -> ImageUtils.fromPixels

6. HideForge.finish
   -> ImageUtils.savePng -> ImageFiles.savePng -> cache/shared/kinetic-UUID.png
   -> ImageUtils.statistics + before/after thumbnail + DiffMapData.build
   -> VaultLogger.logHide -> capture -> private encrypted copy + SQLite event
   -> HideForge.Result -> Handler main callback -> ResultStep.show
   -> HideActivity.showStep(4): wipe + diff/loupe + X-ray + exact statistics

7. ResultStep Export/Share
   -> HideWork.run -> HideExporter.saveGallery / saveDocument / shareUri
   -> copy original PNG bytes; FileProvider URI for sharing
   -> EXPORT metadata event; main UI status / chooser
   OR -> HideStore.proofUri -> ProofSheet.create -> display-only PNG share
```

A debounce waits briefly after edits before doing costly repeated work. Prepared means the exact selected secret snapshot is retained in HideStore, not re-read after enrollment. Ownership means the component responsible for clearing/recycling an array/bitmap. Payload.close wipes its owned bytes. HideWork cleanup wipes transferred tokens/passphrase whether work succeeds, fails or becomes obsolete. clearSensitive invalidates the generation (a number attached to work) and queues prepared-payload clearing; stale callbacks are discarded. close drains queued tasks so cleanup is not skipped. A PBKDF2 call may complete before its next guard: cancellation is cooperative, not a guaranteed instant stop.

Failure paths are part of the flow: Cover keeps Next disabled until ready; Secret disables Next when no input/measurement/fit; repeated enrollment mismatch clears both attempts; forge errors return to Secret; allocation errors show a smaller-image/secret recovery. onPause clears enrollment/prepared secrets and interrupts the sensitive flow; onStop clears text/passphrase UI. Recreation restarts Hide and does not restore secret-bearing child controls.

## Reveal pipeline: original bytes to authenticated content

```text
1. HomeActivity / VaultActivity.open / ScannerActivity.reveal
   -> explicit Intent -> ui/reveal/RevealActivity
   -> original Uri from Intent data, stream/ClipData fallback or source-only saved state
   OR -> OpenDocument picker callback -> RevealActivity.load

2. RevealActivity.load -> lock + reset header/attempt count
   -> RevealWork.load [single worker]
   -> ImageUtils.source(ContentResolver,Uri) -> ImageUtils.decodeExact(...,true)
   -> ImageDecoder.exact
      -> PngIntegrity.read (PNG framing/CRC if PNG signature)
      -> bounds + conservative Ghost memory budget
      -> mutable sRGB ARGB_8888 at original dimensions, opacity required
      -> no resize, sampling, crop, EXIF rotation or density scaling
   -> ImageUtils.pixels -> StegoCore.readHeader -> VaultHeader.parse
   -> RevealLedger.capture -> VaultLogger.capture -> private original-byte copy/hash/DB
   -> main -> header-aware passphrase field and gesture controls

3. RevealGesture.begin
   -> SensorFusionManager / GestureRecognizer -> GestureSession
      OR GesturePadView -> GestureSession
   -> one completed 4..8-token session -> takeTokens -> Unlock enabled
   -> RevealActivity.unlock copies exact passphrase and clears visible credentials

4. RevealWork.unlock -> decrypt [single worker]
   -> RevealExpiry.requireAllowed (fresh local SQLite record)
   -> VaultEngine.reveal
      a. StegoCore.readHeader + extract -> header + ciphertext
      b. CryptoCore.decrypt
         -> GestureCredential.canonical -> PBKDF2WithHmacSHA256
         -> AES/GCM/NoPadding + full header as authenticated data
         -> Cipher.doFinal (authenticates before returning bytes)
      c. GzipCodec.decompress with expansion bound
      d. PayloadCodec.unpack -> type + basename + exact content bytes
   -> RevealExpiry.requireAllowed again
   -> decodeText for TEXT; retain Payload and transient char[]
   -> RevealLedger.revealed -> REVEAL_OK / payload type/size metadata
   -> main -> RevealActivity.unlocked -> VaultDoorView.open callback
      -> TextScrambler.show for text OR file Save/Open controls
   -> finally wipe submitted gesture/passphrase arrays

5. Optional intentional plaintext export
   -> RevealActivity.export -> RevealWork.prepareExport
   -> clone Payload bytes -> RevealExport.create -> cache/reveal/random-UUID file
   -> FileProvider URI with display name/MIME in memory
   -> CreateDocument callback -> RevealWork.save -> RevealExport.save
      OR ACTION_VIEW -> another app -> return callback -> discardExport
   -> EXPORT event; wipe cloned/temp-copy buffers; remove temporary file

6. Lock / leave / screen off / destroy
   -> RevealActivity.lock -> RevealGesture.clear + password.clear
      + TextScrambler.clear + VaultDoorView.reset
   -> RevealWork.clearSensitive -> Payload.close + char[] wipe + generation increment
   -> optional AUTOLOCK event queued separately; intended external export retained as needed
   -> discardExport on return/cancel/close; orphan cleanup next Reveal after process death
```

EXIF is image orientation metadata; applying it would reorder embedded pixels. Exact decoding rejects unsupported size, transparency or memory demand instead of modifying the vault. ImageDecoder can decode supported opaque non-PNG input too; it does not enforce a PNG-only filename/MIME check. PNG framing checks run only when the signature matches; a non-PNG image goes to BitmapFactory. Save always explicitly encodes PNG.

Header recognition is structural detection, not authentication. Wrong credentials and changed/damaged authenticated data can cause the same failure; Reveal uses a neutral denial. attempts is an in-memory count, not a timed lockout/rate limiter. GCM authentication precedes decompression, and both encoded input and expansion are bounded.

An exported decrypted file is plaintext outside the vault's protection. A receiving app can retain it. cache/reveal cleanup deletes temporary files but does not promise secure erasure. If autoLock is disabled, previously revealed content may remain while the Activity survives; active credentials still clear on pause, and destruction clears all secret state. Local expiry is checked before/after new decryption, not continuously against already displayed content.

## Threading and lifecycle model

ExecutorService is a Java task queue with a managed worker thread. Executors.newSingleThreadExecutor means tasks execute one at a time. Handler posts work to a thread's message queue; Looper is the loop servicing that queue. Handler(Looper.getMainLooper()) sends UI results back to Android's main thread. A lambda is a short function expression; an interface is a named contract of methods implemented by a class or callback. These combine in worker progress delivery.

Real fragment from ui/hide/HideWork.java, run (excerpt):

```java
T result = action.call();
main.post(() -> { if (!closed) callback.success(result); });
```

Here action is a Callable<T>: a computation that returns a value and may throw an exception. T is a generic type parameter, meaning the same wrapper can return CoverInfo, Capacity, Uri or another typed result. Callback<T> supplies success(T) and failure(int message). Exceptions are error objects that unwind execution into catch; finally runs cleanup even when work fails. OutOfMemoryError is caught at top-level operations because allocation can fail despite conservative planning.

| Component | Off-main work | Main/UI work | Result/cancellation mechanism |
| --- | --- | --- | --- |
| HideWork / HideStore / HideForge | Input reads, packing/GZIP, crypto, pixels, PNG, stats, local logging, exports | Step navigation, input snapshots, labels/bitmaps, gestures | Handler posts; AtomicInteger secretGeneration invalidates progress/results; drain cleanup on close. |
| RevealWork | Exact decode/pixels, header read, DB identity, expiry, engine, text decode, plaintext export | Record controls, door/text visual state, visible text clearing | Handler posts and synchronized generation checks; ownership-wipe on lock/close. |
| ScannerActivity | Capture file preparation/deletion; exact decode/header/X-ray | Launch pickers/camera, status, install preview, navigation | Handler posts with generation/destroyed guards; recycle late bitmaps; drain cleanup. |
| VaultActivity / VaultAdapter | Repository queries/mutations/CSV; thumbnail BLOB decode | ListView bindings, filters/dialogs, cached bitmap attachment | Handler callbacks, search generation and UUID row tag. |
| VaultPreferences | Settings disk access, calculator-code derivation/comparison | Immutable Values snapshot read, form UI and callback | Static single worker/main Handler; supplied code arrays wiped in finally. |
| SyncManager / SyncClient / SyncStore | Preferences device/retry state, DB queue, HTTP/JSON | request scheduling, listener publication, banner/status, receiver event | Singleton worker, main Handler, active/airplane flags; timeouts/bounded IO. |
| TextImport | Strict UTF-8 stream reads/growing erasable char array | EditText result insertion | Worker/main + generation cancellation; consumer result wiped afterward. |
| GhostComparisonActivity / Work | Public demo cover/engine/maps and disposal | Draw results and sweeps | Worker/main with closed guard; explicit late-result disposal. |
| TestHarnessActivity / HarnessRunner | Public-data PNG round trips and measurements when user invokes them | Status, report, theme switch | Future task cancellation, interruption checks, Handler callbacks. |
| SensorFusionManager / GestureRecognizer / GestureSession | No dedicated sensor worker in this implementation | Sensor listener registered with main Handler; lightweight fusion/recognition/session/telemetry | Serialized callbacks; recognizer generation invalidates pending events; sensor stop unregisters. |
| Canvas views / ValueAnimator / TextScrambler | Large map/PNG preparation is done by workers | Drawing, small frame state, touch and animations | Stop/cancel on pause/detach/hidden/reset; TextScrambler removes pending Handler callbacks. |

Canvas is Android's drawing surface; Paint is its style (colour/stroke/text). ValueAnimator changes a value over time and calls an update listener to redraw. Pixel-array loops and output rendering are distinct from normal UI onDraw. ProofSheet deliberately uses a bitmap Canvas on a worker; screen onDraw uses the UI thread.

A lifecycle is Android's sequence of screen callbacks. onCreate builds screen state; onStart/onStop bracket started visibility; onResume/onPause bracket active interaction; onDestroy releases that screen instance. onSaveInstanceState stores selected non-secret data in a Bundle for recreation. `@Override` marks a method replacing a parent/interface contract; `super` invokes its parent implementation. These callbacks are not manually chained feature steps.

VaultApplication's anonymous ActivityLifecycleCallbacks counts started Activities. Transition between internal screens keeps sync active; when the count reaches0 it stops polling and unregisters its airplane receiver. Reveal registers/unregisters SCREEN_OFF in onStart/onStop. Gesture controllers stop SensorFusionManager and close GestureSession when paused/reset. Receivers are runtime Java objects, not manifest entries or background services.

## Data stores and boundaries

Persistence means surviving process closure. Metadata means descriptive/operational facts about the vault, not its secret content. A UUID is a randomly generated identifier used for record identity. SHA-256 is a cryptographic fingerprint used for encrypted-file identity/deduplication, not decryption. A BLOB is a SQL byte-array field. SQL is a database query language; SQLite is Android's local relational database; MySQL is a separate server database. A transaction groups operations so all commit together or roll back; a trigger is a database action automatically run after a matching insert/update. An index speeds matching/ordering without defining app UI.

### SQLite: kinetic_vault.db, schema version2

VaultDatabaseHelper extends SQLiteOpenHelper, Android's create/upgrade helper. VaultRepository offers synchronous operations with requireWorker checks, Cursor reads and ContentValues updates. A Cursor iterates query rows; ContentValues maps column names to typed values. Foreign keys link events to vault UUIDs; schema configuration enables these constraints.

| Table | Actual columns/purpose | Security/behavior |
| --- | --- | --- |
| vaults | id, uuid, title, image_uri, image_sha256; payload_type/bytes, capacity_bytes; ghost/uses_passphrase; created_at/expires_at; thumb; sync_state/deleted; lat/lng; added sync_revision/sync_updated_at | Local neutral labels, encrypted-image location and thumbnail, operational flags/sizes. No secret filename/key/gesture/passphrase/plaintext. lat/lng fields exist but no location collection is implemented. Unique hash deduplicates image captures. |
| events | id, vault_uuid foreign key, type, ts | HIDE, REVEAL_OK, REVEAL_FAIL, AUTOLOCK, EXPORT; metadata history only. |
| sync_events | event_id primary key, nullable vault_uuid, type, ts, sync_state | Durable outbox: a queue that survives process death; includes airplane events with no vault identity. |

sync_state is0=pending,1=synced,2=failed. Failed records remain queued. vault_sync_dirty updates pending state, increments revision and sets update time after listed metadata fields change. event_sync_queue mirrors new events into outbox; migration inserts existing event IDs as 'vault-'+id. SyncStore uploads at most50 vault rows and50 events per pass. Acknowledgement matches uuid plus sent sync_revision, so a later local edit stays dirty. Rename updates local title (not uploaded); it still dirties metadata revision. Soft delete sets deleted=1; it does not remove the encrypted file or event history. findByHash can return deleted rows for stable image identity; normal search filters deleted=0.

VaultRepository.record inserts identity if absent, optionally updates learned payload type/size and inserts event in one transaction. Schema upgrades add missing metadata columns instead of dropping existing data. Local expiry uses fresh expires_at checked against the device clock before/after engine decryption; null disables it. It can be changed/cleared locally and is not encryption expiry.

### SharedPreferences

SharedPreferences is Android's private named key/value store. VaultPreferences reads/writes on its static worker and exposes a volatile immutable Values snapshot. Immutable means a constructed settings object has final fields and is replaced wholesale. volatile makes new snapshot references visible between threads. Settings are not encrypted by the vault payload key.

| Store | Keys and meaning |
| --- | --- |
| packageName + '_preferences' | sensitivity0/1/2; gesture_pad; screenshot_protection; auto_lock; server_url; api_key; disguise; nerd; disguise_hash. Defaults Normal, pad off, screenshot/auto-lock on, empty server/key/hash, disguise/nerd off. |
| metadata_sync | device_id random installation UUID; destination Base64 SHA-256(URL+'\n'+API key); next_retry and failures. Retry state/device identity persist; server totals are in-memory only. |

api_key is stored as a preference String and sent in X-API-KEY to authenticate the service. That is separate from the AES image key. disguise_hash stores Base64(salt)+':'+Base64(derived code hash), not raw calculator digits. The calculator code uses PBKDF2 but does not unlock a vault image. Caller-supplied char[] code is owned/wiped by save/unlockDisguise. Blank new code preserves an existing hash. commit returns success; current is replaced only if persistence succeeds.

### Files and external destinations

| Location | Contents / owner |
| --- | --- |
| files/vaults/UUID.png | Retained encrypted original-byte copy made by VaultLogger; FileProvider URI stored in SQLite; deduplicated by file SHA-256. |
| cache/shared/kinetic-UUID.png | Original forged encrypted PNG from HideForge. HideStore owns output reference; closing rendered assets does not claim a secure purge of all shared cache files. |
| cache/shared/proof-UUID.png | Display-only comparison sheet; HideStore caches/deletes its proof on release. |
| cache/capture/ | Camera JPEG covers/scans; prepared/deleted by worker helpers; original vault output is always PNG. |
| cache/reveal/random UUID | Explicit temporary decrypted export, real display filename/MIME in memory; removed on designated return/cancel/close and prior-process cleanup. |
| Gallery or document provider | Deliberately saved PNG, plaintext file/text or event CSV. The destination provider owns storage/retention. |

FileProvider maps only these dedicated subtrees, not arbitrary app files. Main manifest disables backup and supplies legacy/modern exclusion XML. This does not revoke or erase manually exported copies. Byte buffers are cleared where owned, but Java Strings, TextView/keyboard/platform/native providers can keep copies: do not promise guaranteed memory erasure.

### MySQL via PHP: optional metadata ledger

HTTP is the web request protocol; HTTPS adds encrypted transport. HttpURLConnection sends a request to the configured URL. JSON is a text object format with named fields. PHP server/api.php validates JSON then uses PDO (PHP Data Objects database API) and prepared statements (SQL with separately bound values) to query MySQL. MySQL is not embedded in the APK.

```text
local metadata/event mutation -> SQLite pending/failed outbox
 -> SyncManager.request -> worker sync -> SyncStore.vaults/events
 -> SyncClient.post(JSON, X-API-KEY, device_id)
 -> server/api.php authenticate/validate/action switch -> PDO prepared SQL
 -> MySQL vaults/events -> JSON ok -> SyncStore.acknowledge
 -> stats -> Handler main -> HomeActivity.showLedger
```

| Server operation | Real handler and effect |
| --- | --- |
| health | SELECT1 then service=kinetic-vault/version1. Settings tests entered URL/key without saving. |
| upsert_vault / delete_vault | upsert validates UUID/hash/numbers/booleans and inserts or applies revision>=stored; delete stores a tombstone (retained deletion flag). Revision assigned last so comparisons see previous revision. |
| list_vaults | listVaults validates offset, returns up to100 device-scoped metadata rows including tombstones; Android sync does not call it to restore images. |
| log_event | logEvent validates allowlisted type/IDs/time and INSERT IGNORE by device/event ID, making retries idempotent (repeating an acknowledged action does not duplicate it). |
| stats | Counts nondeleted device vaults, sums payload_bytes and counts device events; shown as live/last received totals. |

Server tables: vaults primary key(device_id,uuid), fields sha256/sizes/mode flags/created_at/expires_at/updated_at/revision/deleted; events primary key(device_id,event_id), optional vault_uuid/type/ts and device/time index. No image, thumbnail, title, URI, filename, location, salt, IV, encryption key, gesture, passphrase or plaintext column is sent/required. Metadata still exposes use/timing/mode/sizes/fingerprint.

The API accepts POST only, key length>=24 and not placeholder, validates X-API-KEY with hash_equals, caps body65536 bytes and JSON depth32, uses generic errors and no-store responses. Health also needs database/config. Non-POST405, authentication401, oversized body413, malformed input400 and unavailable ledger503. A shared API key is the service credential; device_id partitions data but is not independent user authentication. config.sample.php is a template; real config.php is gitignored and Apache .htaccess denies configuration/schema/setup-file downloads. Supplied files do not establish any live deployment.

Client uses 5000ms connect/read timeouts, rejects redirects, restricts HTTP(S)/URL/key shape, limits response65536 bytes, requires HTTP200 and ok=true. Airplane/active checks stop new uploads and bound response progress; a running operation can take until its next check/timeout. Main release policy denies HTTP; debug allows only four named hosts. Sync runs foreground-only, with normal30s poll,1s continuation when pending, persisted exponential retry starting5s (failure count capped6) and min(300000,5000<<[failures-1]); maximum reached by that count is160s even though the clamp is300s. Backoff state can wait up to300s per scheduling branch, and README's generic '5-minute cap' describes the upper clamp, not a guaranteed sixth-failure300s delay. Changing destination requeues metadata; manual retry/settings change resets failures/next_retry. Cached stats are only process memory, explicitly marked last received when offline/pending.

## Three workstreams mapped to real packages

| Workstream | Ownership in this source | Boundary |
| --- | --- | --- |
| A: motion + crypto | sensor/SensorFusionManager; gesture/GestureRecognizer, GestureSession, GesturePadView, StrengthMeter; core/GestureCredential, CryptoCore; motion and credential constants in core/Constants | Converts samples/pad actions to tokens and key material; sessions/crypto share token definitions. |
| B: image + visuals | core/StegoCore, VaultHeader; image/*; ui/DiffMapData, DiffMapView, DiffStats, KineticOrbView, MatrixRainView, VaultDoorView, SplashLogoView, WipeComparisonView, WipeSliderView, XRayView, TextScrambler; GhostComparisonWork; drawable/anim/design tokens | Preserves encoded pixels and explains/draws changes. core/VaultEngine, PayloadCodec/GzipCodec are shared engine integration rather than an artificial package-per-person split. |
| C: app + data + docs | VaultApplication; ui Activities/controllers under ui/hide, ui/reveal, ui/vault; data/*; diagnostics/* screen/report integration; server/*; layouts/resources/assets; docs/* | Navigation, lifecycle, async orchestration, storage, exports, synchronization, educational material and documentation. Uses A/B through real methods. |

The packages are technical boundaries; the workstream grouping is a responsibility map, not a claim about Git authorship. Constants and engine orchestration cross workstreams. Core/version1 format remain frozen.

## Algorithm walkthroughs with concrete examples

A byte is8 bits; Java byte is signed -128..127, so &255 reads its unsigned0..255 value. A char is one UTF-16 code unit; int is a32-bit signed integer used to hold ARGB pixels/lengths, and long is64-bit. Big endian stores a multibyte integer most significant byte first. Bitwise operators work on binary digits: & masks, | combines, ^ detects differing bits, ~ inverts, << shifts left and >>> shifts right with zero-fill. These are exact format operations, not ordinary arithmetic multiplication/division in every case.

### 1. Pack and GZIP

Idea: place an unambiguous type/name/length before content, then wrap it in GZIP so Reveal always has one decompression path. UTF-8 is the byte encoding used for names/text; UTF-16 is Java's character representation.

Example: FILE a.bin with bytes[10,20,30] has type2 +2-byte filename length5 +5 UTF-8 name bytes +4-byte content length3 +3 content bytes =15 packed bytes. Empty TEXT has type1, name length0 and content length0 =7 bytes. Actual compressed length depends on content; HideStore measures it instead of estimating.

Walkthrough: SecretInput.read obtains content and sanitized basename; Payload constructor validates type/name/data and owns data. PayloadCodec.pack allocates7+nameBytes+data.length, sets ByteOrder.BIG_ENDIAN and writes fields. GzipCodec.compress writes a ConfiguredGzip to WipingBuffer; default compression uses Deflater.DEFAULT_COMPRESSION(-1), off uses NO_COMPRESSION(0) but retains GZIP header/trailer. decompress streams8192-byte chunks, enforces MAX_PACKED_BYTES, and unpack checks exact remaining byte count before returning owned Payload.

Constants: TEXT1/FILE2, filename<=240 UTF-8 bytes, raw payload<=16*1024*1024=16777216, packed max=raw+247=16777463. Name cannot be empty for files, nonempty for text, '.'/'..', controls, slash/backslash/colon or malformed Unicode. Bounds stop unsupported allocation/path names and expansion bombs. WipingBuffer clears replaced buffers and closes by clearing backing bytes. GZIP checksum detects accidental compression-stream damage; GCM is the cryptographic authentication boundary.

### 2. Credential encoding and key derivation

Idea: turn the discrete move sequence plus exact phrase into one deterministic password, then use an expensive salted derivation so each guess costs work. PBKDF2 is Password-Based Key Derivation Function2; HMAC-SHA256 is its keyed-hash building block. Salt is a fresh public random input that makes the derived key different between images with the same credential. Derivation slows guessing; it does not add unpredictability to a weak gesture.

Example from actual source test data: tokens[0,0,4,5] and A|é become KVLT1|4|0045|0041007c00e9. 'A'=UTF-16 hex0041, '|'=007c, 'é'=00e9. The phrase separator cannot become ambiguous because phrase code units are always four hex characters. Four uniform tokens give6^4=1296 sequences, log2(1296)=10.33985bits; eight give6^8=1679616 and20.6797bits. Human patterns may be weaker.

Walkthrough: GestureCredential.canonical validates4..8 tokens in0..5 and passphrase<=1024chars; allocates9+tokenCount+4*phraseLength characters; writes KVLT1|count|digits| then writePassphrase takes shifts12,8,4,0 and masks15 for each hex nibble (4-bit digit). CryptoCore.derive creates PBEKeySpec with canonical chars,16-byte salt,120000 iterations and256 output bits; SecretKeyFactory.getInstance(PBKDF2WithHmacSHA256).generateSecret returns a SecretKey; getEncoded returns32 key bytes. finally wipes canonical chars, clears PBEKeySpec and attempts key destruction. entropyBits uses tokenCount*log(6)/log(2); StrengthMeter labels it as an upper bound and adds passphrase advice.

Constants:4..8 tokens, LEFT0 RIGHT1 FORWARD2 BACK3 SNAP4 FLAT5; phrase max1024 UTF-16 units; random salt16bytes; iterations120000; key256bits. Version1 header has no variable derivation settings, so these values are fixed by its format. Source does not give a performance study justifying120000: do not invent one. ASCII hex avoids provider-specific password-encoding differences. No trimming/case-folding/normalization is allowed.

DisguiseCode is separate: valid code6..12 ASCII digits, random salt16bytes, PBKDF2WithHmacSHA256/120000/256, Base64 salt:hash storage,32-byte expected hash and MessageDigest.isEqual comparison. It never participates in image derivation. Its input/salt/hash buffers clear where owned.

### 3. AES-GCM encryption and authentication

Idea: encrypt compressed bytes and attach an authentication tag; changing the key, public authenticated header or ciphertext prevents successful authentication. AES is Advanced Encryption Standard; GCM is Galois/Counter Mode. An IV (initialization vector) is a fresh per-encryption value. AAD (additional authenticated data) is readable metadata that is included in tamper detection.

Worked size example: compressed input100bytes produces ciphertext length116 including16-byte tag. VaultHeader stores116 as big-endian00 00 00 74. With passphrase+Ghost flags, flags=1|2=3. Header4+1+1+16+12+4=38bytes is all passed to updateAAD. This is a size/field example, not a fabricated encryption test vector. An existing independent fixture in core/VaultEngineTest.java decrypts8-byte 'portable' from24 ciphertext/tag bytes, demonstrating the same16-byte overhead in the supplied source.

Walkthrough: CryptoCore.encrypt bounds compressed length, requires a non-null char[], obtains fresh SecureRandom salt/IV, derives flags and creates header with compressed.length+16. transform reports KEY, calls derive, builds SecretKeySpec(AES), gets Cipher(AES/GCM/NoPadding), init with mode/key/GCMParameterSpec(128,iv), updateAAD(header.bytes), reports ENCRYPT/DECRYPT, then doFinal. decrypt first requires exact declared ciphertext length; engine finally wipes compressed/packed plaintext; transform wipes key bytes/attempts destroy in finally.

Constants: salt16, IV12, AES key256bits, GCM tag128bits=16bytes, version1. Header fields are magic KVLT,version,flags,salt,IV,cipher length. GeneralSecurityException covers algorithm/key/provider failures; AEADBadTagException is an authentication failure subtype that UI does not diagnose as uniquely wrong-password vs damage. Header validates magic/version/unknown flags/length before extraction; successful GCM is required for trust.

### 4. LSB embedding and capacity

Idea: visit RGB channels in a fixed order and replace only their last bits with the encrypted bitstream. Write each byte's most significant bit first. Alpha never carries data.

Example: byte0xA5=10100101. Starting RGB(100,150,200), first three bits1,0,1 make(101,150,201). Next RGB(100,150,200) gets0,0,1 ->(100,150,201). A third pixel gets0,1 in R/G ->(100,151,unchanged B). Every changed channel differs by1. Extraction masks the same bits and shifts them into the same byte positions.

Walkthrough: StegoCore.validatePixels enforces opaque ARGB and supported size. embed checks matching ciphertext length/capacity, builds optional order, clones cover, writes38-byte header at pixel0 and ciphertext starting102. writeBytes maps bit/3 to pixelIndex; shift=16-(bit%3)*8 selects R/G/B lowest bits at16/8/0; value=(bytes[bit/8] >>> (7-bit%8))&1; clear old bit with ~(1<<shift), then OR new bit. readBytes reverses it into byte[]. Reports at bit indices divisible65536 plus final count; header progress is not passed to UI.

Header304bits use102pixels (306channel positions); last two channel bits of pixel101 are untouched. Payload starts at102, so capacity=floor((pixelCount-102)*3/8), clamped0 for smaller images. Example200pixels ->98*3/8=36bytes, not36 raw secret bytes because tag/GZIP/packed metadata consume capacity. At9M pixels maximum cipher capacity3374961bytes. The length guard includes the tag and protects allocation before reading untrusted payload length.

Constants: header38bytes/102pixels, RGB3channels,8bits/byte, alpha255, max9000000pixels. No pixel bit outside mask0x00010101 is changed. All format mapping/order choices are version1 compatibility requirements, not tunable UI preferences.

### 5. Ghost shuffle

Idea: keep the public header in the same place, then visit remaining pixels in a reproducible shuffled order. It changes placement, not encryption strength. Fisher-Yates is the shuffle that swaps each descending position with a random position up to itself.

Small real-number example: count105 creates order[102,103,104]. Sixteen zero salt bytes produce seed0. Java Random(0)'s first nextInt(3)=0 swaps positions2/0 ->[104,103,102]; nextInt(2)=1 leaves positions1/1 ->same array. Payload bits0..2 use pixel104, bits3..5 use103, bits6..7 use102. This105pixel example demonstrates order math only: a complete authenticated payload/tag cannot fit its1-byte capacity. For seed folding, bytes1,2 give ((0*31+1)*31+2)=33 before later salt bytes.

Walkthrough: StegoCore.pixelOrder returns null for Standard (pixelIndex chooses startPixel+bit/3); for Ghost creates count-102 int entries102..last; folds all salt bytes using seed=seed*31+(value&255), with Java long overflow; constructs java.util.Random(seed); descends i=length-1 to1, j=nextInt(i+1), swaps order[i]/order[j]. Extract rebuilds the identical order from the public salt. A null order is a deliberate sequential-mode signal, not missing data.

Constants: start102, seed initial0, multiplier31,16unsigned salt bytes, java.util.Random's defined behavior and descending Fisher-Yates. Random here is deterministic placement randomness; SecureRandom supplies cryptographic salt/IV elsewhere. Ghost int[] adds4bytes per payload pixel and contributes to the higher memory budget.

### 6. Complementary orientation filter

Idea: gyro gives short-term rotation changes; accelerometer estimates long-term level from gravity. Blend the gyro prediction with a small correction toward gravity, wrapping angles at +/-180degrees. A complementary filter combines these strengths. A gyroscope measures radians/second; an accelerometer measures metres/second squared; pitch raises/lowers the top and roll lowers/raises an edge. Portrait axes make BACK positive pitch and RIGHT positive roll.

Worked example: theta10degrees, angular rate20degrees/s, dt0.02s -> prediction10.4degrees. Accelerometer estimate8degrees gives wrapped difference-2.4degrees; correction(1-.98)*-2.4=-.048, output10.352degrees. At seam predicted179degrees, accel-179degrees -> wrapped error2degrees, output179.04degrees, avoiding a spurious large turn.

Walkthrough: SensorFusionManager.start registers accelerometer and optional gyro at20000microseconds with main Handler. accelerometer rejects stale samples, smooths gravity=.9*old+.1*sample unless restarting, derives accelPitch=degrees(atan2(gy,gz)) and accelRoll=degrees(atan2(-gx,hypot(gy,gz))), uses fallback if no fresh gyro, then emit. gyroscope calculates dt, angular magnitude and transformed Euler rates using current pitch/roll; invalid/large dt or near-singularity orientation resets to accel estimate. blend wraps predicted theta+omega*dt and applies wrapped .02error. emit subtracts gravity using fused orientation and9.80665m/s², then telemetry and recognizer.update in monotonic milliseconds. Monotonic means elapsed-time based, unaffected by changing wall-clock dates.

Constants and purpose in core/Constants.java: FILTER_ALPHA.98 trusts short-term gyro; GRAVITY_FILTER_ALPHA.9 reduces spikes in gravity; SENSOR_PERIOD_US20000 (~50Hz requested, not guaranteed); SENSOR_MAX_DT_SECONDS.25 restarts stale integration; SENSOR_MIN_GRAVITY_MAGNITUDE1 rejects near-zero gravity initialization; FUSION_MIN_ROLL_COSINE.15 switches away from unstable Euler-rate division near singularity; GRAVITY_METERS_PER_SECOND_SQUARED9.80665 removes gravity. Accelerometer-only fallback computes angular speed from change/dt so a moving tilt is not called still. Nonfinite samples and reversed timestamps are ignored.

### 7. Gesture detection and session rules

Idea: accept a tilt once when it crosses entry threshold, then rearm only after returning inside a smaller exit threshold. This hysteresis (different enter/exit boundaries) prevents noisy repeats. SNAP is a bounded downward acceleration pulse plus recovery; FLAT is a level still hold. Session adds temporal/length constraints and finishes after enough deliberate moves.

Worked tilt example, Normal: roll26/pitch3 -> RIGHT1; roll28 -> no extra token because latched; roll10/pitch4 -> neutral rearms; after>=300ms, roll-26/pitch2 -> LEFT0. If only200ms since last token, no token accepted. For SNAP, face-up az=-8m/s² at1000ms starts pulse, az=-1m/s² at1150ms recovers within220ms and emits SNAP4 if gap allows. For FLAT, pitch/roll<=8degrees and still continuously500ms -> FLAT5 once until leaving level. Four accepted tokens followed by1500ms continuous stillness finish a motion session; pad users choose Done explicitly.

Walkthrough: GestureRecognizer.update validates finite/time-ordered input, resets hold/pulse on >250ms sample gap and computes stillness from linear acceleration magnitude and gyro angularSpeed. recognize clears tilt latch in neutral; evaluates pulse/flat state; rejects move gap<300; priority SNAP, then largest-axis tilt (roll wins equal magnitude), then unlatch FLAT. detectSnap rejects pulse if tilt>20degrees, accepts downward<=-7 with recovery>=-1.5 within220ms; rejected pulses must recover before rearm. detectFlat holds level/still and avoids noise repeating flat. deliver uses generation guard and calls token before motion; reset invalidates queued callbacks.

GestureSession.addToken rejects closed/finished/full sessions and too-close/out-of-order moves, validates token0..5 and reports count. onMotion requires sample continuity and at least4 tokens before measuring1500ms still finish. done keeps too-short attempt editable. matches requires two different completed sessions, compares counts and all8 padded slots using XOR/OR, returns true only difference0. takeTokens copies only count bytes then closes/wipes session. GestureStep retains first, records second and resets both if mismatch; Reveal requires one recording. Pad callback emits exact same token IDs and elapsed timestamps, with300ms acceptance gap and accessible actions.

Constants: MIN4/MAX8; Normal enter25/exit12degrees; Relaxed20/10, Strict30/14 via Constants helpers; FLAT8degrees/500ms; gap300ms; still finish1500ms; SNAP <=-7, recovery>=-1.5m/s², max220ms/max20degrees tilt; still acceleration<=.8m/s² and angular<=.15rad/s. These values reject tremor/rapid duplicate/long pulses and preserve repeatability. Sensitivity changes recognition only, not token IDs/credential format.

### 8. PSNR and visual summaries

Idea: measure RGB error numerically, then express signal-to-error ratio in decibels. Mean squared error (MSE) is the average squared RGB difference; PSNR=10*log10(255²/MSE). A larger value means smaller average image difference, not stronger encryption.

Example from ImagePolicyTest input: RGB(0,0,0) ->(1,1,1) gives squared error3, channel count3, MSE1, PSNR48.1308036dB, one pixel changed=100%, max delta1. Two pixels where only one changes like that give changed50% and MSE.5. Identical input gives zero error and positive infinity, not division failure.

Walkthrough: ImageStats.compare validates equal nonzero arrays; uses XOR masked0x00ffffff to count changed pixels once; loops shifts0/8/16 to extract unsigned channel values&255, accumulates long difference² and max absolute difference; constructor calculates percentage and PSNR. DiffStats.show formats infinity distinctly. Alpha is ignored by stats because engine requires opaque images separately.

DiffMapData.build bins all changed source channels with OR into <=512px map: red if R changed, green if G, blue if B; combined changes mix, black means none. It retains exact arrays for original/encoded inspection, not merely preview readings. buildXRay samples RGB last bits and multiplies by255, needing no original cover. DiffMapView loupe magnifies binned preview, but label gets exact source values. ProofSheet strips preview RGB LSBs using mask0x00fefefe, bins density16x16, renders1080x660 and uses exact source stats; it is a display image, not cryptographic proof.

Constants:255 channel peak,3 RGB samples per pixel, shifts0/8/16, long error accumulation prevents large-sum overflow. Diff/X-ray preview longest side512, sweep1400ms; before/after thumbnails512; logger thumbnails96. These preview sizes bound display work and do not alter the original encoded data.

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

## Current-source limits and historical notes

Source is the authority for current behavior. docs/IMPLEMENTATION.md defines the frozen format but includes historical Phase1B launcher/permission/scope statements. UNCLEAR if read as a current app description: “No later-phase implementation files have been created.” Current sources include full Hide/Reveal/data/server/visual/scanner/disguise flows, so this statement belongs to the earlier handoff period. No new format or source changes are implied by this architecture guide.

Every method/class is mapped in 00_PROJECT_MAP.md and every visible feature's storage/failure/disclosure path in 01_FEATURE_INDEX.md. This document explains their integration. Location columns exist but collection is omitted; cloud sync is optional metadata only; disguise is a launch-screen choice; local expiry and soft deletion are reversible policies; the public KVLT header makes vault images detectable.

## Likely viva questions

1. **Why compress before encryption?** Encryption makes bytes appear random and usually uncompressible; this engine always packs, writes GZIP, encrypts, then embeds. Both compression options keep the GZIP envelope.
2. **What protects data, Ghost or AES-GCM?** AES-GCM provides confidentiality/authentication. Ghost only changes public, reproducible pixel visitation order.
3. **Why not resize a Reveal image?** LSB extraction needs original pixel values/order. Only cover preparation and display previews may transform dimensions.
4. **Does all sensing run in a worker?** No. This implementation registers sensor delivery on main and performs lightweight fusion/recognition there. Crypto, large pixel loops, disk, DB and network operations run on workers.
5. **What survives closing the app?** Encrypted library files, SQLite metadata/outbox, preferences and optional server metadata. Credentials/live plaintext are not intentionally persisted except explicit decrypted exports; temporary plaintext has cleanup boundaries and cannot revoke receiver copies.

## Covers these files

- `app/src/androidTest/java/com/example/kineticvault/image/ImageLayerTest.java`
- `app/src/androidTest/java/com/example/kineticvault/image/PngFixtures.java`
- `app/src/debug/res/xml/network_security_config.xml`
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
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/main/res/xml/network_security_config.xml`
- `app/src/test/java/com/example/kineticvault/core/PayloadCodecTest.java`
- `app/src/test/java/com/example/kineticvault/core/StegoCoreTest.java`
- `app/src/test/java/com/example/kineticvault/core/VaultEngineTest.java`
- `app/src/test/java/com/example/kineticvault/image/ImagePolicyTest.java`
- `docs/IMPLEMENTATION.md`
- `server/.gitignore`
- `server/.htaccess`
- `server/api.php`
- `server/config.sample.php`
- `server/README_XAMPP.md`
- `server/schema.sql`
- `app/src/main/assets/how_it_works.html`
- `app/src/main/res/layout/activity_hide.xml`
- `app/src/main/res/layout/hide_step_secret.xml`
- `app/src/main/res/layout/hide_step_gesture.xml`
- `app/src/main/res/layout/hide_step_result.xml`
- `app/src/main/res/layout/activity_reveal.xml`
- `app/src/main/res/layout/activity_vault.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/settings_strings.xml`
- `app/src/main/res/values/sync_strings.xml`
- `app/src/main/res/values/vault_strings.xml`
