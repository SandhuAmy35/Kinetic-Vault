# STATE SNAPSHOT

Scope: Phase 1 only. Wait for START PHASE 2 before implementing anything further.
No later-phase implementation files exist.

Phase 1 finished in C:/Users/sandh/Desktop/Coding/KineticVault.
Package: com.example.kineticvault. minSdk 26; template SDK 37, AGP 9.4.1,
Gradle 9.6.0 and Java 11 retained. No new dependencies.

Finished files: app/build.gradle.kts; core/Constants.java, WorkProgress.java,
GestureCredential.java, VaultHeader.java, CryptoCore.java, StegoCore.java,
PayloadCodec.java, GzipCodec.java, VaultEngine.java; tests/VaultEngineTest.java,
StegoCoreTest.java, PayloadCodecTest.java; docs/IMPLEMENTATION.md and this snapshot.
Core and tests live under app/src/main/java and app/src/test/java respectively,
inside com/example/kineticvault/core.

Main public APIs:
- VaultEngine.hide(int[], Payload, byte[], char[], boolean, boolean, WorkProgress): int[]
- VaultEngine.reveal(int[], byte[], char[], WorkProgress): Payload
- StegoCore.capacityBytes(int): int
- StegoCore.readHeader(int[]): VaultHeader
- StegoCore.embed(int[], VaultHeader, byte[], WorkProgress): int[]
- StegoCore.extract(int[], WorkProgress): byte[]
- GestureCredential.canonical(byte[], char[]): char[]
- GestureCredential.entropyBits(int): double
- Payload.close(): void

Decisions: 120,000 PBKDF2 iterations; full header authenticated as AAD;
capacity reserves 306 bits; compression-off still uses GZIP; fixed token IDs
and ASCII canonical credentials. See IMPLEMENTATION.md for exact wire format.
Call everything on a worker; callers wipe credentials and close payloads.

Verification: production core compiles cleanly; 35 JVM tests pass.
Android Gradle generated-class compilation fails locally; APK/R8/device unverified.
No launcher/UI yet. Remaining: images, sensors, UI, storage/server, animations,
exports, auto-lock, rubric documents and phone/release validation.

AFTER START PHASE 2, CONTINUE FROM: app/src/main/java/com/example/kineticvault/image/ImageUtils.java
