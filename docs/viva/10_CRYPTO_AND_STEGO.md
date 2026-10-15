# 10 — Cryptography and steganography deep-dive

## What this engine does

Kinetic Vault converts text or one file into encrypted bytes and carries those bytes in a PNG image. **Plaintext** means readable secret content; **ciphertext** means its encrypted form. **Cryptography** protects content using a key; **steganography** places it inside another medium. A **pixel** is an image cell; **RGB** are its red, green and blue amounts (0–255). A **bit** is a binary digit; a **byte** is eight bits. A channel's **least significant bit (LSB)** is its lowest bit: replacing it changes that amount by at most one. **PNG** is a lossless image encoding; **JPEG** normally uses lossy compression that changes pixels. Format details and why this matters appear below.

A **payload** is the secret's type, original basename and content. A **basename** is a filename without directories. **Packing** serializes those fields into an unambiguous byte sequence. **GZIP** is a compression envelope containing compressed data and an error-detecting trailer. **Authentication** means cryptographic detection of a changed message/key/associated metadata; it is stronger than merely recognizing the public vault signature. **Ghost** is the optional, publicly reproducible shuffle of payload pixels.

All source paths below start at `app/src/main/java/com/example/kineticvault/`. Thus `core/CryptoCore.java — derive` identifies an exact file/method without line numbers. This guide completely covers the nine classes in `core/`, including their nested classes/interfaces and Constants fields. `11_IMAGE_HANDLING.md` explains the seven `image/` classes that bridge original image bytes and these pixel arrays.

## Source files and class ownership

Most classes are stateless utilities: their private constructor prevents ordinary object creation, and callers use static methods. A static method belongs to the class. An immutable value object exposes no way to change its owned data after construction; arrays still need defensive copies to make that true.

| File / class | One-sentence purpose | Who calls/creates it | Key fields and nested types |
| --- | --- | --- | --- |
| core/Constants.java / Constants | Centralizes frozen format limits, token IDs and motion thresholds. | All engine/image/motion callers read its fields; motion uses the two sensitivity helpers. | Public static final fields; no instances or retained state. |
| core/GestureCredential.java / GestureCredential | Canonically encodes gesture tokens and exact passphrase, and calculates the token entropy upper bound. | CryptoCore.derive calls canonical; the gesture strength feature calls entropyBits through StrengthMeter. | No retained fields. |
| core/CryptoCore.java / CryptoCore | Derives keys and encrypts/authenticates or decrypts the GZIP bytes. | VaultEngine.hide/reveal call encrypt/decrypt. | Nested Sealed contains header and ciphertext; all key material is local. |
| core/CryptoCore.java / Sealed | Returns the public header and encrypted bytes together. | CryptoCore.encrypt calls its private constructor; VaultEngine.hide reads fields. | final header, final ciphertext reference; ciphertext array itself is not immutable. |
| core/PayloadCodec.java / PayloadCodec | Packs and validates payload metadata/content and unpacks recovered bytes. | VaultEngine and HideStore use pack; VaultEngine.reveal uses unpack. | TEXT=1, FILE=2; nested Payload. |
| core/PayloadCodec.java / Payload | Owns type, filename and a mutable plaintext byte array that close wipes. | Secret input preparation and unpack construct it; engine reads it; owning controller closes it. | final type, filename String, data byte[]; constructor transfers data ownership without cloning. |
| core/GzipCodec.java / GzipCodec | Writes the always-present GZIP envelope and bounds decompression. | VaultEngine and HideStore.measure call it. | Static nested WipingBuffer and ConfiguredGzip. |
| core/GzipCodec.java / WipingBuffer | Clears current/replaced output buffers rather than retaining unnecessary plaintext copies. | compress/decompress construct it in try-with-resources. | Inherited protected buf and count from ByteArrayOutputStream. |
| core/GzipCodec.java / ConfiguredGzip | Selects normal/no compression while retaining the GZIP format. | GzipCodec.compress constructs it. | Inherited def compression engine; no extra fields. |
| core/VaultHeader.java / VaultHeader | Owns a defensive copy of the public 38-byte version-1 header. | CryptoCore.encrypt creates it; StegoCore.readHeader parses it. | private final encoded; copied inputs/outputs. |
| core/StegoCore.java / StegoCore | Writes/reads header and ciphertext bits in opaque pixel arrays. | VaultEngine; header discovery also uses readHeader in Reveal/Scanner. | Temporary Ghost order array; no retained state. |
| core/VaultEngine.java / VaultEngine | Orchestrates the reversible packing–compression–crypto–pixel pipeline. | HideForge.run, RevealWork.decrypt; public demonstrations use the same engine (see D1 map). | Local packed/compressed buffers, wiped in finally. |
| core/WorkProgress.java / WorkProgress and Stage | Define synchronous progress callbacks and a fixed set of stage names. | Core methods call report; Android worker wrappers implement the callback. | Stage enum; no owned listener thread or queue. |

An **interface** defines methods an implementation must supply; a **callback** is a supplied operation invoked by its owner. An **enum** is a fixed list of names. From core/WorkProgress.java:

```java
public interface WorkProgress {
    enum Stage { PACK, COMPRESS, KEY, ENCRYPT, EMBED, EXTRACT, DECRYPT, UNPACK }
    void onProgress(Stage stage, int completed, int total);
```

## Full user-visible call chains and storage boundary

An Android **Activity** is a screen. The **UI thread** handles drawing/taps; a **worker thread** handles expensive operations. An **ExecutorService** queues background tasks; a **Handler** posts result operations to a **Looper**, the thread's message-processing loop. These packages do not own Activities, database connections or a background executor. The engine is synchronous: its methods run on the calling thread until completion. HideWork and RevealWork supply the workers and return results to main.

```text
Hide screen
 -> HideActivity.forge -> HideWork.forge [single worker]
 -> HideStore.forge -> HideForge.run -> ImageUtils.pixels
 -> VaultEngine.hide
 -> PayloadCodec.pack -> GzipCodec.compress
 -> capacity check -> CryptoCore.encrypt
 -> GestureCredential.canonical -> CryptoCore.derive -> CryptoCore.transform
 -> StegoCore.embed -> header write + ciphertext write
 -> HideForge.finish -> ImageUtils.fromPixels/savePng
 -> original encrypted PNG + result statistics/previews -> main callback

Reveal screen
 -> RevealActivity.load -> RevealWork.load [single worker]
 -> ImageUtils.decodeExact -> ImageUtils.pixels -> StegoCore.readHeader
 -> RevealActivity.unlock -> RevealWork.unlock -> RevealWork.decrypt [worker]
 -> local expiry policy -> VaultEngine.reveal
 -> StegoCore.readHeader -> StegoCore.extract (also reads header)
 -> CryptoCore.decrypt -> derive/transform -> authentication succeeds
 -> GzipCodec.decompress -> PayloadCodec.unpack
 -> expiry recheck -> returned Payload -> text/file UI through main callback

Scanner screen
 -> ScannerActivity.scan -> decode [worker]
 -> ImageUtils.decodeExact -> ImageUtils.pixels -> StegoCore.readHeader
 -> found/absent status; only a found structural header enables Reveal navigation
```

| User-visible feature | What persists / transient data | Failure and recovery | Security boundary |
| --- | --- | --- | --- |
| Hide text/file with optional compression/passphrase/Ghost | Core keeps only caller-owned/transient arrays. HideForge saves encrypted PNG in cache/shared/; its logging hook keeps a private encrypted copy and local metadata. | Over-capacity/read/crypto/storage failure surfaces an operation error; choose smaller secret/larger supported cover or re-enter secret and enroll again. | Keys, token sequences and passphrases are not part of the header/database payload; contents and filename are encrypted. |
| Measured capacity | HideStore.measure temporarily packs/GZIPs actual input, then retains the fitting Payload only when requested. | Too large disables progression; enable compression or change input/cover. | Capacity includes tag and all envelope overhead; no raw-file-size shortcut. |
| Reveal text/file | Pixels are encrypted data in memory; recovered Payload/text remain in live UI/work state unless the user deliberately exports plaintext. | Neutral denial for wrong credentials/changed data; retry exact original credentials and untouched PNG. Image/record failures have separate pre-unlock recovery. | Successful GCM authentication precedes decompression/unpacking; header discovery alone is not trust. |
| Scanner discovery | Original input URI/pixels and an exploratory display image; core itself writes no files. | Missing/damaged header yields absent status; decoding errors request an accessible original image. | Public signature makes vault images detectable; Scanner does not decrypt or authenticate. |
| Stage display | Transient progress only. | Callback may throw cancellation to stop at a boundary; wrapper ignores obsolete results. | KEY/ENCRYPT/DECRYPT have total=0; no invented percentage for expensive crypto. |

A **URI** is an Android content address, not necessarily a filesystem path. **Metadata** means operational descriptions, not secret contents. SQLite is the on-device record database; none of these core classes uses it directly. Local expiry is an application policy outside VaultEngine, not an expiring cryptographic key.

## Constants: exact values and their purpose

**Versioning** means recording which fixed byte-format rules a reader must use. Version 1 has no stored configurable key-derivation settings, so changing them would break old images. A **MiB** is 1,048,576 bytes. Hexadecimal writes one digit per four bits; 0xff is 255.

| Constant(s) | Exact value | Why used |
| --- | --- | --- |
| VERSION | 1 | Reject other interpretations of the frozen format. |
| HEADER_BYTES / HEADER_PIXELS | 38 / 102 | 304 header bits occupy the first 102 RGB pixels; payload begins at the next whole pixel. |
| SALT_BYTES / IV_BYTES / TAG_BYTES | 16 / 12 / 16 | Public derivation randomness; GCM initialization value; 128-bit authentication tag. |
| KEY_BITS / PBKDF2_ITERATIONS | 256 / 120,000 | Fixed derived AES key size and password-guessing work factor. |
| FLAG_PASSPHRASE / FLAG_GHOST | 1 / 2 | Header bit0 and bit1; other bits rejected. |
| MAX_PIXELS | 9,000,000 | Upper supported pixel allocation/format capacity. |
| MAX_PAYLOAD_BYTES | 16*1024*1024 = 16,777,216 | Raw content/expansion bound, not guaranteed image capacity. |
| MAX_FILENAME_BYTES | 240 | UTF-8 basename bound. |
| MAX_PACKED_BYTES | MAX_PAYLOAD_BYTES+247 = 16,777,463 | Raw max + 240 filename bytes + 7 structural bytes. |
| MAX_CIPHER_BYTES | (9,000,000−102)*3/8 = 3,374,961 | Absolute RGB ciphertext capacity, including tag. |
| MAX_PASSPHRASE_CHARS | 1024 | Bounds canonical password allocation/derivation input. |
| COVER_LONG_SIDE | 3000 | Cover-preparation limit; not an extra long-side limit on exact Reveal decoding. |
| LEFT, RIGHT, FORWARD, BACK, SNAP, FLAT | byte values 0,1,2,3,4,5 | Permanent token IDs; motion/pad yield identical credentials. |
| MIN_TOKENS / MAX_TOKENS | 4 / 8 | Credential length bounds; entropy method also permits counts0..8 for live meter. |

Constants.java also contains the complete motion settings below. They are recognition parameters, not encryption parameters. The complementary filter (orientation blending) and movement detector are implemented in sensor/ and gesture/, outside D2; this table describes their constants without starting that documentation step.

| Field/helper | Value | Meaning from source comments |
| --- | --- | --- |
| FILTER_ALPHA | .98f | Trust gyro longer vs gravity correction. |
| TILT_ENTER_DEGREES / TILT_EXIT_DEGREES | 25f / 12f | Tilt acceptance vs smaller rearm angle. |
| tiltEnterDegrees(int sensitivity) | 0→20f; 2→30f; otherwise25f | Relaxed/strict/normal entry; returns float; called by recognizer sensitivity configuration. No exceptions or IO. |
| tiltExitDegrees(int sensitivity) | 0→10f; 2→14f; otherwise12f | Matching rearm threshold; returns float and has no side effects. |
| FLAT_DEGREES / MOVE_GAP_MS / FLAT_HOLD_MS / STILL_FINISH_MS | 8f / 300 / 500 / 1500 | Level pose, minimum move gap, flat hold and motion-session still completion. |
| SNAP_DOWN_METERS_PER_SECOND_SQUARED | −7f | Required downward linear acceleration. |
| SNAP_MAX_MS / SNAP_RECOVERY_METERS_PER_SECOND_SQUARED / SNAP_MAX_TILT_DEGREES | 220 / −1.5f / 20f | Pulse duration, recovery threshold and level tolerance. |
| STILL_ACCEL_TOLERANCE / STILL_GYRO_RADIANS_PER_SECOND | .8f / .15f | Tremor tolerance for stillness. |
| SENSOR_PERIOD_US / GRAVITY_FILTER_ALPHA | 20,000 / .9f | Requested sample period and gravity smoothing. |
| SENSOR_MAX_DT_SECONDS / SENSOR_MIN_GRAVITY_MAGNITUDE | .25f / 1f | Restart stale integration; ignore near-zero orientation initialization. |
| FUSION_MIN_ROLL_COSINE / GRAVITY_METERS_PER_SECOND_SQUARED | .15f / 9.80665f | Avoid unstable angle singularity; subtract gravity in m/s². |

UNCLEAR: the source fixes the security/performance choice but does not document a measured study selecting the iteration count: `public static final int PBKDF2_ITERATIONS = 120_000;`. Explain its actual value/purpose, without inventing a benchmark or claiming weak credentials become strong.

## Credential encoding and PBKDF2 key derivation

### Idea and worked example

**PBKDF2** is Password-Based Key Derivation Function 2: repeated keyed hashing derives fixed-length key bytes from a password and salt. **HMAC-SHA256** is its keyed-hash building block here. A **salt** is a fresh public random byte sequence; it makes repeated uses of the same credentials derive different keys. **Entropy** measures unpredictability in bits; PBKDF2 increases the cost per guess, not the number of possible gestures.

Tokens LEFT LEFT SNAP FLAT are [0,0,4,5]. For the illustrative passphrase A|é, UTF-16 character values are 0x0041,0x007c,0x00e9. UTF-16 is Java's representation using 16-bit code units; an emoji may use two units. **ASCII** is the basic character encoding covering the password's output digits/letters. Canonical means one exact representation:

```text
KVLT1|4|0045|0041007c00e9
prefix |count|tokens|each passphrase code unit as four lowercase hex digits
length = 9 + 4 + 4*3 = 25 characters
```

For 'A'=65, shifts12,8,4,0 select nibbles0,0,4,1; nibble means four bits. Empty phrase yields KVLT1|4|0045| (13 chars). Spaces/case/Unicode remain exact: no trimming, normalization or case conversion. In the sample, the literal '|' in the phrase becomes 007c, so it cannot collide with separators.

Uniformly random four-token choices: 6^4=1296 possibilities, 4*log2(6)≈10.34 bits; eight: 6^8=1,679,616, ≈20.68 bits. Human choices may be less random. A strong phrase is recommended; a 256-bit derived key does not imply 256 bits of user-secret entropy.

### Code walkthrough

1. core/GestureCredential.java — canonical calls validate, requiring non-null tokens of length4..8, token values0..5 and non-null phrase of length<=1024.
2. Allocate9+tokens.length+4*phrase.length chars. Copy KVLT1|, write count digit at6, separator7, token digits from8, then separator and phrase encoding.
3. writePassphrase emits four nibbles per UTF-16 unit using unsigned shifts and mask15; returns through the supplied output array.
4. core/CryptoCore.java — derive gets canonical char[], constructs PBEKeySpec(password,salt,120000,256), asks SecretKeyFactory for PBKDF2WithHmacSHA256 and gets encoded derived bytes (256/8=32 bytes).
5. finally clears canonical chars and PBEKeySpec's password copy, then attempts to destroy the generated SecretKey. Transform later clears derived bytes and its AES wrapper.

| Method / parameters | Return value | Called by → calls | Edge/failure behavior |
| --- | --- | --- | --- |
| GestureCredential.canonical(byte[] tokens, char[] passphrase) | New canonical char[]. | CryptoCore.derive → validate, System.arraycopy, writePassphrase. | Throws IllegalArgumentException for invalid length/token/null inputs; caller must wipe returned array. |
| validate(byte[] tokens, char[] passphrase) | void. | canonical → bounds/token loop. | Count4..8; phrase<=1024; token0..5. Does not alter inputs. |
| writePassphrase(char[] out, int offset, char[] passphrase) | void; fills out. | canonical → String.toCharArray for public hex alphabet. | Private caller supplies correctly sized output/offset; preserves each unit. |
| entropyBits(int tokenCount) | double = tokenCount*ln6/ln2. | StrengthMeter via app → Math.log. | Allows0..8; throws outside that range; reports only uniform upper bound. |
| CryptoCore.derive(byte[] tokens, char[] passphrase, byte[] salt) | 32 derived key bytes. | transform → canonical, PBEKeySpec, SecretKeyFactory.getInstance/generateSecret, getEncoded, destroy. | Security/provider failures propagate GeneralSecurityException; finally wipes password/spec and generated key where possible. |

## AES-GCM: salt, IV, tag and AAD are different

**AES** is Advanced Encryption Standard. **GCM** is Galois/Counter Mode, which combines encryption and an authentication tag. **IV** is initialization vector: a fresh per-encryption value used to initialize GCM. A **tag** is the cryptographic authentication result appended to ciphertext; a changed key/message/AAD fails authentication. **AAD**, additional authenticated data, is readable metadata included in the tag calculation. A **provider** is Java's implementation of the requested cryptographic algorithm.

| Item | Exactly how this code uses it | Where it lives |
| --- | --- | --- |
| Password characters | canonical token/phrase chars feed PBEKeySpec. | Temporary array, cleared by derive. Never header. |
| Salt | SecureRandom fills16 bytes; PBKDF2 uses it; Ghost also folds these public bytes into a shuffle seed. | Header offsets6..21; not a secret key. |
| Derived AES key | 32 bytes wrapped by SecretKeySpec(...,"AES"). | Temporary memory; not PNG header/storage metadata. |
| IV | SecureRandom fills12 bytes; GCMParameterSpec(128,header.iv()) passes it to cipher.init. | Header offsets22..33. It must not be reused with the same key; this code generates fresh randomness. |
| GCM tag | doFinal encryption appends16 bytes; decryption doFinal authenticates them. | Final16 bytes of ciphertext, hidden with the rest; no separate header tag field. |
| AAD | cipher.updateAAD(header.bytes()) supplies all38 header bytes before doFinal. | Header stays public; tag protects its contents including flags and declared length. |

### Worked size example and code walkthrough

Take a hypothetical100-byte already-GZIP input. CryptoCore creates a header declaring100+16=116 ciphertext bytes, length hex00 00 00 74. Phrase present and Ghost on gives flags1 OR2=3. Key length256 bits=32 bytes; IV12 bytes; tag128 bits=16 bytes. Exact ciphertext values depend on key, salt, IV and contents; this is a format/size illustration, not an invented encryption result.

1. encrypt rejects null/oversized compressed input (greater than MAX_CIPHER_BYTES−16) and null phrase.
2. SecureRandom.nextBytes independently fills salt and IV; nonempty phrase sets bit0 and ghost sets bit1.
3. VaultHeader.create writes the public header including the exact ciphertext+tag length.
4. transform reports KEY, derives key bytes and constructs SecretKeySpec.
5. Cipher.getInstance("AES/GCM/NoPadding") selects the algorithm. NoPadding means this mode does not add block padding; the tag still adds16 bytes.
6. cipher.init(mode,key,new GCMParameterSpec(128,header.iv())); cipher.updateAAD(header.bytes()); report ENCRYPT or DECRYPT.
7. doFinal returns ciphertext+tag for encryption, or authenticated compressed plaintext for decryption. A failed tag throws rather than returning accepted plaintext.
8. finally fills key bytes with zero and tries destroy(key). Key providers may keep inaccessible copies: clearing is best effort, not guaranteed system-wide erasure.

| Method / parameters | Return value | Called by → calls | Edge/failure behavior |
| --- | --- | --- | --- |
| encrypt(byte[] compressed, byte[] tokens, char[] passphrase, boolean ghost, WorkProgress progress) | Sealed(header,ciphertext). | VaultEngine.hide → SecureRandom, VaultHeader.create, transform, Sealed constructor. | IOException for unsupported input size; IllegalArgumentException for null phrase/invalid credentials through canonical; security failures propagate. Empty array is the no-phrase representation. |
| decrypt(VaultHeader header, byte[] ciphertext, byte[] tokens, char[] passphrase, WorkProgress progress) | Authenticated compressed byte[]. | VaultEngine.reveal → header.ciphertextLength, transform. | Header/data must be non-null and exact declared size; no plaintext accepted on failed authentication. |
| transform(int mode, byte[] input, VaultHeader header, byte[] tokens, char[] passphrase, WorkProgress progress) | Cipher.doFinal bytes for requested mode. | encrypt/decrypt → WorkProgress.report, derive, header accessors, SecretKeySpec, Cipher, GCMParameterSpec, destroy. | finally clears key bytes and attempts destroy; callback/security errors propagate. No UI/thread switching. |
| destroy(SecretKey key) | void. | derive/transform → key.destroy. | Null is harmless; catches DestroyFailedException because some providers cannot erase internal key copies; does not log secrets. |
| Sealed(VaultHeader header, byte[] ciphertext) | New nested result object. | encrypt only; assigns references. | Private constructor; no extra validation/copy/wipe. Header already validated, ciphertext is encrypted data. |

**GeneralSecurityException** is the checked superclass for cryptographic errors. **AEADBadTagException** is the authentication-failure subtype; AEAD means authenticated encryption with associated data. It is referenced in a core comment and propagated by the cipher, not explicitly caught in these classes. Wrong credentials and damaged authenticated bytes can both produce it; RevealWork's neutral denial does not diagnose which. Some tampering instead fails earlier structural/capacity checks. The public flags are authenticated only after decryption succeeds.

## Byte-by-byte version-1 header

Offsets are zero-based byte offsets in the extracted header, not offsets in the PNG file's chunk structure. **Big endian** stores the most significant byte first. Example fields below use public illustrative salt01..10 hex, IV15..20 hex, flags03 and ciphertext length116 decimal. Real encrypt calls generate random salt/IV, not these fixed bytes.

| Offset decimal (hex) | Field byte | Example hex | Meaning |
| --- | --- | --- | --- |
| 0 (00) | Magic[0] | 4b | ASCII K. |
| 1 (01) | Magic[1] | 56 | ASCII V. |
| 2 (02) | Magic[2] | 4c | ASCII L. |
| 3 (03) | Magic[3] | 54 | ASCII T. |
| 4 (04) | Version | 01 | Version1 only. |
| 5 (05) | Flags | 03 | bit0 phrase, bit1 Ghost; bits2..7 zero. |
| 6 (06) | Salt[0] | 01 | First of16 salt bytes. |
| 7 (07) | Salt[1] | 02 | Public salt byte. |
| 8 (08) | Salt[2] | 03 | Public salt byte. |
| 9 (09) | Salt[3] | 04 | Public salt byte. |
| 10 (0a) | Salt[4] | 05 | Public salt byte. |
| 11 (0b) | Salt[5] | 06 | Public salt byte. |
| 12 (0c) | Salt[6] | 07 | Public salt byte. |
| 13 (0d) | Salt[7] | 08 | Public salt byte. |
| 14 (0e) | Salt[8] | 09 | Public salt byte. |
| 15 (0f) | Salt[9] | 0a | Public salt byte. |
| 16 (10) | Salt[10] | 0b | Public salt byte. |
| 17 (11) | Salt[11] | 0c | Public salt byte. |
| 18 (12) | Salt[12] | 0d | Public salt byte. |
| 19 (13) | Salt[13] | 0e | Public salt byte. |
| 20 (14) | Salt[14] | 0f | Public salt byte. |
| 21 (15) | Salt[15] | 10 | Last salt byte. |
| 22 (16) | IV[0] | 15 | First of12 IV bytes. |
| 23 (17) | IV[1] | 16 | Public IV byte. |
| 24 (18) | IV[2] | 17 | Public IV byte. |
| 25 (19) | IV[3] | 18 | Public IV byte. |
| 26 (1a) | IV[4] | 19 | Public IV byte. |
| 27 (1b) | IV[5] | 1a | Public IV byte. |
| 28 (1c) | IV[6] | 1b | Public IV byte. |
| 29 (1d) | IV[7] | 1c | Public IV byte. |
| 30 (1e) | IV[8] | 1d | Public IV byte. |
| 31 (1f) | IV[9] | 1e | Public IV byte. |
| 32 (20) | IV[10] | 1f | Public IV byte. |
| 33 (21) | IV[11] | 20 | Last IV byte. |
| 34 (22) | Cipher length[0] | 00 | Most significant byte, bits31..24. |
| 35 (23) | Cipher length[1] | 00 | bits23..16. |
| 36 (24) | Cipher length[2] | 00 | bits15..8. |
| 37 (25) | Cipher length[3] | 74 | Least significant byte; entire length116. |

Total4+1+1+16+12+4=38 bytes. There is no plaintext filename, raw payload length, compression flag, key, gesture, phrase, iteration-count field or separate tag field here. Flags accepted:0 neither option;1 phrase;2 Ghost;3 both. Compression mode need not be stored because both settings write a GZIP envelope.

### Header methods and defensive copies

| Method / parameters | What it does / returns | Called by → calls | Failure/edge handling |
| --- | --- | --- | --- |
| private VaultHeader(byte[] encoded) | Clones38-byte array into private final encoded; new object. | parse only → clone. | Constructor assumes parse validation. |
| create(byte[] salt, byte[] iv, int flags, int length) | Creates big-endian38 bytes, then returns parse result. | CryptoCore.encrypt → ByteBuffer.allocate/order/put/putInt, parse. | Salt must16, IV12; null/wrong sizes fail. Rejects flags outside0..3; parse validates length. |
| parse(byte[] bytes) | Checks structure, returns defensive-copy header. | StegoCore.readHeader/create → ByteBuffer.getInt(34), constructor. | Must exactly38; KVLT magic; version1 via &255; flags unknown bits via &252; ciphertext16..3,374,961. Does not authenticate contents. |
| bytes() | Full encoded clone. | CryptoCore.transform, StegoCore.embed. | Caller mutation cannot alter original header. |
| salt() / iv() | Copies ranges[6,22) / [22,34). | CryptoCore derivation/init; StegoCore.pixelOrder uses salt. | Upper bound exclusive; returned arrays are public metadata. |
| usesPassphrase() / isGhost() | boolean from encoded[5]&1 / &2. | UI/header callers; StegoCore order. | Values structurally validated but untrusted until GCM success. |
| ciphertextLength() | big-endian signed int from34, already positive/bounded by parse. | CryptoCore.decrypt and StegoCore read/extract/embed checks. | Returns total ciphertext including tag. |

readHeader additionally compares declared ciphertext to actual image capacity. VaultHeader.parse alone knows no dimensions; structural validity and fitting the current image are separate checks.

## Payload packing: exact layout, example and methods

**UTF-8** encodes text as bytes; filename byte count can differ from Java character count. Payload validation checks encoding explicitly. The codec treats content as bytes and does not decode text contents; text interpretation belongs to callers.

Let N=filename UTF-8 bytes and L=content bytes:

| Packed offset | Width | Field |
| --- | --- | --- |
| 0 | 1 | type1 TEXT or2 FILE. |
| 1..2 | 2 | Unsigned big-endian N. |
| 3..(2+N) | N | UTF-8 basename (empty for TEXT). |
| (3+N)..(6+N) | 4 | Big-endian content length L. |
| (7+N)..(6+N+L) | L | Exact content bytes. |

Worked example: FILE a.bin, bytes[10,20,30]. N=5,L=3,total=7+5+3=15. Packed hexadecimal bytes:

```text
02 | 00 05 | 61 2e 62 69 6e | 00 00 00 03 | 0a 14 1e
file|name5 | a  .  b  i  n | content3    | three bytes
```

Walkthrough: Payload constructor validates name/type/data before taking data ownership. pack revalidates, allocates exact length, writes type/short/name/int/content using big-endian ByteBuffer, and returns backing byte[]. unpack requires7..MAX_PACKED_BYTES; reads unsigned short by &65535, reserves4bytes for remaining length, strict-decodes filename, requires L equal to all remaining bytes, validates name, allocates exactly L and transfers new data into Payload. Trailing bytes and declared lengths beyond available input are rejected.

| Method / parameters | Return / work | Called by → calls | Failure/edge handling |
| --- | --- | --- | --- |
| Payload(byte type, String filename, byte[] data) | New owned payload, no data clone. | Input preparation/unpack → validate. | Empty TEXT/file content is permitted at codec level; TEXT name must empty, FILE name nonempty. Caller must not reuse owned bytes as unrelated storage. |
| Payload.close() | void; Arrays.fill(data,(byte)0). | Owner cleanup / try-with-resources. | Does not clear immutable filename String or null fields; repeated fill harmless. |
| pack(Payload payload) | New serialized byte[]. | VaultEngine.hide / HideStore.measure → validate, getBytes(UTF_8), ByteBuffer. | Null Payload fails IOException; mutable data rechecked. Caller owns returned plaintext array. |
| unpack(byte[] packed) | New Payload with separate content byte[]. | VaultEngine.reveal → ByteBuffer, decodeName, validateName, Payload constructor. | Exact bounds/remaining-length/type/name checks before content allocation; malformed metadata IOException. |
| validate(byte type,String filename,byte[] data) | void. | Constructor/pack → validateName. | Null or >16MiB data rejected; no lower positive bound on content length. |
| validateName(byte type,String filename) | void. | validate/unpack → UTF-8 encoder. | Type1/2; name non-null; filename UTF-8<=240; '.'/'..' rejected; controls<32,127, slash, backslash,colon rejected; strict encoder rejects invalid surrogate input. |
| decodeName(byte[] name) | Strict decoded String. | unpack → UTF-8 decoder, CodingErrorAction.REPORT, ByteBuffer.wrap. | CharacterCodingException propagates as IOException subtype; malformed/unmappable bytes rejected. |

The seven structural bytes plus240 max filename explain MAX_PACKED_BYTES=raw limit+247. No stored filesystem path is trusted. Name bytes/filename String are inside encryption on disk but not all filename copies are erasable in Java memory.

## GZIP: compression toggle does not change the envelope

### Idea and example

Both user settings make GZIP bytes. Enabled chooses Deflater.DEFAULT_COMPRESSION (−1); disabled selects Deflater.NO_COMPRESSION (0). GZIP includes its own framing and error-detecting checksum; it is not encryption/authentication against an attacker. A **decompression bomb** is small compressed input expanding into excessive memory.

For the15-byte packed example, GZIP wraps those15bytes even when compression is off; do not assume output length15. For a bounded-read example, if output.size() is MAX_PACKED_BYTES−3 and next read count is4, count>MAX_PACKED_BYTES−output.size() is4>3, so decompression throws before appending that chunk. Actual compressed output length depends on content and implementation, so HideStore measures it rather than using a guessed compression ratio.

Walkthrough: compress validates input size, creates WipingBuffer and ConfiguredGzip in nested try-with-resources, writes all packed bytes, finish writes trailer, then toByteArray copies completed output. Resources close after return expression; their internal storage is cleared. decompress validates compressed size<=MAX_CIPHER_BYTES, opens GZIPInputStream over ByteArrayInputStream and WipingBuffer, repeatedly reads8192-byte chunk, enforces expansion cap before write, copies output, finally zeroes chunk. GZIPInputStream detects invalid/truncated GZIP according to its implementation and propagates IOException.

| Method / parameters | Return / work | Called by → calls | Failure/ownership |
| --- | --- | --- | --- |
| compress(byte[] packed, boolean compress) | New GZIP byte[]. | VaultEngine.hide/HideStore.measure → WipingBuffer, ConfiguredGzip.write/finish, toByteArray. | Null/>MAX_PACKED_BYTES reject; does not wipe caller's packed input or returned copy. |
| decompress(byte[] compressed) | New expanded packed byte[]. | VaultEngine.reveal → GZIPInputStream.read, WipingBuffer.write/toByteArray. | Null/oversize input, bad GZIP or >MAX_PACKED_BYTES output fail; chunk wiped in finally. |
| ConfiguredGzip(ByteArrayOutputStream output, boolean compress) | New GZIP stream with requested level. | compress → super(output), def.setLevel. | IOException from stream setup propagates; uses same framing for both levels. |
| WipingBuffer.write(byte[] bytes,int offset,int length) | void; delegates append, clears previous buf if backing array changed. | GZIP and decompression output → super.write, Arrays.fill. | Parent validates ranges; synchronized uses Java monitor. Clears old expanded buffer, not caller input. |
| WipingBuffer.write(int value) | void; same replacement-buffer clearing for one-byte append. | Output stream byte writes → super.write, Arrays.fill. | Parent owns growth; low byte is appended. |
| WipingBuffer.close() | void; clears current buf and reset count. | try-with-resources cleanup → Arrays.fill, reset. | Does not clear toByteArray result; caller owns that copy. |

## Capacity mathematics: reserve whole header pixels

Idea: one LSB from each RGB channel gives3 bits/pixel. Header needs38*8=304bits, occupying ceil(304/3)=102pixels. Bit303 uses pixel101's R channel; G/B LSBs there remain unchanged. Ciphertext starts at pixel102, so those2 spare bits cannot be counted.

```text
P = number of image pixels
C(P) = 0                          if P < 102
C(P) = floor((P - 102) * 3 / 8)    otherwise
needed = actual_GZIP_length + 16   (GCM tag)
fits exactly when needed <= C(P)
```

Numeric examples: P=200 ->98payload pixels ->294bits ->36whole ciphertext bytes. If measured GZIP length20, needed36 fits exactly; if21, needed37 fails. P=1024*1024=1,048,576 ->1,048,474*3/8 ->393,177bytes. At9M pixels ->3,374,961bytes. P=104 ->0bytes; P=105 ->1byte, still insufficient for even the16-byte tag. Empty TEXT can be packed by codec but GZIP/tag still need space; no claim that raw secret capacity equals C(P).

Walkthrough: StegoCore.capacityBytes returns0 below102, widens pixelCount−102 to long, multiplies3, integer-divides8 and returns int. It does not itself reject counts above MAX_PIXELS; engine/pixel validation enforces the supported upper bound. VaultEngine.hide packs/GZIPs first, then rejects null/>9M cover or needed>capacity before expensive key derivation; embed validates again. readHeader rejects declared length beyond available image capacity before ciphertext allocation.

## Hiding one byte: exact pixel/bit worked example

This demonstrates the bit-writing operation, not a complete one-byte vault (a valid vault also needs header, GZIP and tag). Suppose one ciphertext byte is hexadecimal A5=decimal165=binary10100101. Standard payload begins at pixel102. Each original RGB is(100,150,200), whose least bits are(0,0,0); alpha remains255.

| Stream bit index | Value / significance | Pixel/channel | Shift into ARGB int | Old→new channel |
| --- | --- | --- | --- | --- |
| 0 | 1, byte bit7 | 102/R | 16 | 100→101 |
| 1 | 0, byte bit6 | 102/G | 8 | 150→150 |
| 2 | 1, byte bit5 | 102/B | 0 | 200→201 |
| 3 | 0, byte bit4 | 103/R | 16 | 100→100 |
| 4 | 0, byte bit3 | 103/G | 8 | 150→150 |
| 5 | 1, byte bit2 | 103/B | 0 | 200→201 |
| 6 | 0, byte bit1 | 104/R | 16 | 100→100 |
| 7 | 1, byte bit0 | 104/G | 8 | 150→151 |

Result pixels:102=(101,150,201),103=(100,150,201),104=(100,151,200). Pixel104/B is untouched in this eight-bit example. Re-read R,G,B then R,G,B then R,G ->10100101 ->A5; Java byte holding that bit pattern is−91, while &255 interprets it as165.

Real loop excerpt from core/StegoCore.java — writeBytes:

```java
int index = pixelIndex(bit, startPixel, order);
int shift = 16 - (bit % 3) * 8;
int value = (bytes[bit / 8] >>> (7 - bit % 8)) & 1;
pixels[index] = (pixels[index] & ~(1 << shift)) | (value << shift);
```

The % operator gives remainder: bit%3 chooses channel, bit%8 chooses position in source byte. Clear selected channel bit with an inverted one-bit mask, then OR desired value at that same position. Other bits and alpha survive. Header and ciphertext each have their own bit index beginning0; header consumes only304 of the306 reserved channel positions.

### Embed/extract methods: complete contracts

| Method / parameters | Return / operation | Called by → calls | Failure/edge handling |
| --- | --- | --- | --- |
| capacityBytes(int pixelCount) | Integer whole ciphertext-byte capacity. | Engine/headers/capacity UI → arithmetic above. | <102 returns0; no own max-dimension rejection. |
| validatePixels(int[] pixels) | void; validates every alpha. | readHeader/embed → loop. | Null, fewer102, more9M or any alpha!=255 -> IOException. |
| readHeader(int[] pixels) | VaultHeader parsed from sequential header bits. | RevealWork, Scanner, VaultEngine.reveal and extract → validatePixels, readBytes(38,0,null,null), VaultHeader.parse, capacityBytes. | Rejects malformed header or declared ciphertext too long for image. Does not decrypt or authenticate. |
| embed(int[] cover,VaultHeader header,byte[] ciphertext,WorkProgress progress) | New encoded int[]; original cover unchanged. | VaultEngine.hide → validatePixels, length/capacity checks, pixelOrder, cover.clone, writeBytes twice. | Null/mismatched ciphertext/header or too little capacity -> IOException; alpha/dimensions rejected. Callback exceptions propagate. |
| extract(int[] pixels,WorkProgress progress) | New ciphertext byte[] including tag. | VaultEngine.reveal → readHeader, pixelOrder, readBytes with header length/start102. | Header/capacity validated before allocating declared output; no mutation of input pixels. |
| pixelOrder(int count,VaultHeader header) | null for Standard; shuffled absolute pixel indexes for Ghost. | embed/extract → header.isGhost/salt, Random.nextInt. | Count already validated by callers; allocates count−102 ints; memory failure handled by outer UI worker. |
| pixelIndex(int bitIndex,int startPixel,int[] order) | Pixel index; position=bitIndex/3. | writeBytes/readBytes → no other helper. | null order chooses startPixel+position; Ghost array already contains absolute indexes. Bounds assumed by checked caller. |
| writeBytes(int[] pixels,byte[] bytes,int startPixel,int[] order,WorkProgress progress) | void; writes bits MSB-first, R/G/B order. | embed → pixelIndex, WorkProgress.report. | Reports at bit0, each65536bits and final total; header call passes null progress. Only specified bits changed. |
| readBytes(int[] pixels,int count,int startPixel,int[] order,WorkProgress progress) | New count-byte array. | readHeader/extract → pixelIndex, WorkProgress.report. | Extracts same channel shifts, ORs into byte at7−bit%8; count validated by callers. |

**ARGB** is alpha/red/green/blue packed into a32-bit int. Alpha uses bits31..24; R bits23..16; G15..8; B7..0. The combined set of changeable bits is0x00010101. Cryptographic ciphertext bytes, not raw plaintext bytes, feed writeBytes.

## Ghost shuffle: public deterministic pixel placement

Idea: leave header fixed and shuffle all later pixels using the header salt. **Deterministic** means same inputs produce same order. **Fisher–Yates** is a descending swap shuffle; it permutes all indexes without duplicates. This is visual scattering, not extra key material or undetectability.

Worked seed fold: unsigned salt bytes1,2 give seed0→1→33 by seed=seed*31+byte. All16bytes take part and Java long arithmetic wraps on overflow. For an order-only illustration with count105 and16zero salt bytes, seed0 and initial[102,103,104], Random(0) nextInt(3)=0 swaps positions2/0 ->[104,103,102]; nextInt(2)=1 swaps1/1 unchanged. Consecutive RGB groups then use104,103,102. This tiny example cannot contain a real vault; it illustrates indexing only.

Walkthrough in core/StegoCore.java — pixelOrder:

1. If !header.isGhost(), return null so pixelIndex uses sequential start+position.
2. Allocate count−102 integers; fill absolute indexes102..count−1.
3. Fold header.salt() unsigned bytes with &255, initial seed0, multiplier31.
4. Construct java.util.Random(seed), a specified pseudorandom generator; pseudorandom means a repeatable sequence determined by its seed.
5. Descend i=order.length−1 while i>0, choose j=random.nextInt(i+1), swap order[i]/order[j].
6. write/read each selected pixel's R/G/B in place. The reader regenerates order from the visible salt without credentials.

Exact compatibility choices:102 header pixels,16salt bytes, signed64-bit overflow, unsigned-byte mask255, multiplier31, java.util.Random, descending loop, boundi+1. Replacing any of them changes version-1 traversal. Memory adds4bytes per order element. SecureRandom generates cryptographic salt/IV; Random merely reproduces placement. Public seed fold/Random can have collisions; Ghost is not the confidentiality mechanism.

## VaultEngine and WorkProgress: orchestration contracts

| Method / parameters | Return / operation | Who calls → what it calls | Failure/cleanup |
| --- | --- | --- | --- |
| VaultEngine.hide(int[] cover,PayloadCodec.Payload payload,byte[] tokens,char[] passphrase,boolean compress,boolean ghost,WorkProgress progress) | New encoded pixels. | HideForge.run → report PACK, pack, report COMPRESS, compress, capacity check, CryptoCore.encrypt, StegoCore.embed. | IOException/security/argument/callback errors propagate; finally wipe packed/compressed. Caller owns/wipes Payload/tokens/phrase. Cover cloning occurs in embed. |
| VaultEngine.reveal(int[] pixels,byte[] tokens,char[] passphrase,WorkProgress progress) | New owned Payload. | RevealWork.decrypt → readHeader, extract (reads header again), CryptoCore.decrypt, report UNPACK, decompress, unpack. | Finally wipe compressed/packed; returns independent Payload data requiring close. No partial accepted plaintext on GCM error. |
| VaultEngine.wipe(byte[] bytes) | void; null-safe zero fill. | hide/reveal finally → Arrays.fill. | Does not clear ciphertext/header/pixel input, nor caller credentials. |
| WorkProgress.onProgress(Stage stage,int completed,int total) | void callback contract. | report → supplied implementation. | Invoked synchronously on caller worker; total0 means indeterminate, not0% complete. |
| WorkProgress.report(WorkProgress listener,Stage stage,int done,int total) | void. | Engine/crypto/stego → listener.onProgress if non-null. | Null listener is permitted; does not validate progress numbers or catch listener failures. |

A **lambda** supplies a callback compactly; for example HideForge.run passes a WorkProgress implementation that calls guard.run then progress.update. A callback may throw CancellationException when a generation is obsolete. Generation means a number identifying the currently allowed sensitive operation. CryptoCore's PBKDF2/doFinal calls are not interruptible at every internal step; cancellation takes effect at the surrounding callbacks/guards. Core has no automatic main-thread guard; its callers must obey the worker contract. The image facade does enforce a worker guard on its major operations.

## Cryptography/compression library types and real uses

| Type | Meaning and real source example |
| --- | --- |
| java.security.SecureRandom | Cryptographic random source; `SecureRandom random = new SecureRandom();` then `random.nextBytes(salt);` in CryptoCore.encrypt. |
| java.security.GeneralSecurityException | Checked family of provider/algorithm/key failures; declared by CryptoCore encrypt/decrypt/transform/derive. |
| javax.crypto.SecretKeyFactory | Key-derivation provider; `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")` in derive. |
| javax.crypto.SecretKey | Generated key interface; `generated = factory.generateSecret(specification);` in derive; getEncoded obtains bytes. |
| javax.crypto.spec.PBEKeySpec | Password/salt/iteration/size specification; `new PBEKeySpec(password, salt, Constants.PBKDF2_ITERATIONS, Constants.KEY_BITS)` in derive; clearPassword erases its owned password copy. |
| javax.crypto.spec.SecretKeySpec | Wraps raw derived key bytes with AES identity; `new SecretKeySpec(keyBytes, "AES")` in transform. |
| javax.crypto.Cipher | Encryption/decryption object; `Cipher.getInstance("AES/GCM/NoPadding")`, updateAAD/doFinal in transform. |
| javax.crypto.spec.GCMParameterSpec | GCM tag-bits/IV configuration; `new GCMParameterSpec(128, header.iv())` in transform. |
| javax.crypto.AEADBadTagException | Cipher authentication-failure subtype referenced by core comment; propagated, not a distinct wrong-password diagnosis. It is not an imported class in core source. |
| javax.security.auth.DestroyFailedException | Related cleanup exception, outside javax.crypto; `catch (DestroyFailedException ignored)` in destroy tolerates unsupported key erasure. |
| java.util.zip.GZIPInputStream | GZIP reader; `new GZIPInputStream(new ByteArrayInputStream(compressed))` in decompress. |
| java.util.zip.GZIPOutputStream | GZIP writer superclass; ConfiguredGzip extends it. |
| java.util.zip.Deflater | Compression-level constants; `def.setLevel(compress ? Deflater.DEFAULT_COMPRESSION : Deflater.NO_COMPRESSION);` in ConfiguredGzip. |

No MessageDigest or CRC32 is used in core; image/ImageFiles.java uses MessageDigest for file SHA-256 and image/PngIntegrity.java uses CRC32 for PNG chunk checks, explained in11. Neither a fingerprint nor CRC is a substitute for the GCM tag. Stream classes, nested-class ownership, exceptions and Android integration constructs are explained in the reading aid below.

## Why PNG output, never JPEG

PNG losslessly encodes the integer channel values the engine produced. JPEG's usual quantization (rounding transformed image information to reduce file size) changes those values, including LSBs, even when the picture looks alike. Example: hidden red101 (LSB1) reconstructed as100 (LSB0) changes one ciphertext bit; if structure still parses, GCM authentication fails. ImageFiles.write selects Bitmap.CompressFormat.PNG explicitly; extension/quality100 alone would not choose PNG or make JPEG lossless.

A cover photo may start as JPEG because it is prepared before embedding. The encoded output must be preserved as the original PNG; rescaling, filtering, screenshots, photo re-encoding or photographing it can change bits. The PNG container's own header/chunks are different from VaultHeader hidden in decoded pixels. GZIP for the payload and PNG's internal image compression are two separate layers.

## Security and ownership facts to say accurately

- Random salt/IV and expensive derivation do not rescue a guessable gesture; add a strong passphrase. The pad can reproduce the same tokens, so motion is not biometrics.
- Header magic/flags/salt/IV/length and Ghost order are public; vaults are detectable. Only successful authentication makes their content/metadata trustworthy.
- Payload owns the supplied plaintext data; engine owns/wipes packed/compressed intermediates; derive/transform wipe their controlled password/key copies. Android wrappers own/wipe gesture/phrase arrays.
- Filename String, encoding-library/provider/native copies and UI text may remain outside these fills; do not claim guaranteed RAM erasure. PNG export is encrypted; intentional Reveal exports are unencrypted copies.
- Limits defend allocation and malformed inputs. A structurally valid header/tag-sized length can still fail decryption/GZIP/payload parsing. CRC/GZIP checks detect corruption but are not adversarial authenticity.

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

1. **Why reserve102pixels instead of only304bits?** Header needs304bits but occupies102whole RGB pixels; ciphertext deliberately starts at102, leaving2channel bits unused. Capacity must subtract those whole pixels.
2. **How do salt and IV differ?** The16-byte salt feeds PBKDF2 (and public Ghost order); the12-byte IV initializes AES-GCM. Both are public fresh random fields, neither is a password/key.
3. **What exactly is authenticated?** Ciphertext/tag plus all38header bytes supplied through updateAAD; the header stays readable. A failed tag cannot distinguish wrong credentials from damage.
4. **What happens when compression is off?** The code still writes GZIP, selecting NO_COMPRESSION; Reveal therefore always decompresses one envelope format and needs no compression flag.
5. **Does Ghost strengthen encryption or hide the signature?** It only scatters ciphertext pixels using public salt; header remains sequential and AES-GCM is the protection.

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
- `app/src/main/java/com/example/kineticvault/image/ImageFiles.java`
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
