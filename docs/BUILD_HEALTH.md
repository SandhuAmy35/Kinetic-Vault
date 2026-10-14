# Phase 1B build health

## Diagnosis

The generated-class failure observed in this session comes from a filesystem
access restriction in the agent environment. The deepest Gradle cause is:

```text
java.nio.file.AccessDeniedException: .../gradle-9.6.0/lib/gradle-classloaders-9.6.0.jar
at jdk.nio.zipfs.ZipFileSystemProvider.removeFileSystem
at jdk.nio.zipfs.ZipFileSystem.close
at SimpleGeneratedJavaClassCompiler.compile
```

A standalone Java probe, without Gradle or app code, can read all 84,174 bytes
of that JAR but fails at Path.toRealPath(). This reproduces with JBR 25 and JDK 21.
The earlier offline dependency-resolution failure was separate: the existing
read-only cache resolved those dependencies before this path-access failure.
Changing Java source level, app test source sets or the vault format would not
repair that environmental restriction. No permission or ACL changes were made.

Retained: AGP 9.4.1, Gradle 9.6.0, compile/target SDK 37, minSdk 26, Java 11
source/target compatibility, the template's Java 25 daemon criteria and its
existing dependencies. App code is Java; the template's Gradle scripts use Kotlin DSL.
Release optimization now covers the full app: the dependency-only packageScope
restriction was removed. Release execution still needs the local checks below.

## Run on your Windows machine

In Android Studio, open this project, turn OFF Gradle Offline mode, and choose
Android Studio's bundled JBR 25 as the Gradle JDK. The checked-in daemon criteria
request Java 25. Install the Android SDK 37 platform if Studio requests it.
Use File > Sync Project with Gradle Files.

Then open PowerShell and run exactly:

```powershell
Set-Location -LiteralPath 'C:\Users\sandh\Desktop\Coding\KineticVault'
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat --stop
.\gradlew.bat --version
.\gradlew.bat clean :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug --no-configuration-cache --stacktrace
```

Expected: BUILD SUCCESSFUL. Do not downgrade Gradle/AGP or remove dependencies
based solely on the sandbox failure. If a local build fails, its first actual
cause in the stack trace is the evidence to investigate.

Debug APK: app/build/outputs/apk/debug/app-debug.apk.
The release APK is unsigned until a signing configuration is supplied in Phase 5.
This phase does not supply a production signing key or claim a signed release.

For one connected, USB-debugging-authorized Android 8+ phone or a running emulator:

```powershell
.\gradlew.bat :app:installDebug :app:connectedDebugAndroidTest --no-configuration-cache --stacktrace
```

The device suite contains 10 new image/integration tests plus the template test.
JUnit's standard Gradle task also includes the existing template unit test.
Open Kinetic Vault from the launcher to use the temporary integrity screen.

## Verification actually performed here

| Check | Result |
| --- | --- |
| Core file SHA-256 before/after | All nine files unchanged |
| Targeted JVM suite | 47 passed: 35 existing core + 12 image policy/statistics |
| AAPT2 resource compilation | Passed |
| AAPT2 resource linking against cached template libraries | Passed |
| Android Java and instrumentation-source compilation | Bytecode emitted, no source or deprecation diagnostics |
| Java compiler archive close | Environment AccessDeniedException after compilation |
| Full Gradle debug/release/lint | Blocked before app compilation by the environment |
| Phone/emulator instrumentation | Not run; ADB initialization also blocked in this environment |
| Visual/device QA | Pending the phone checklist |

The direct compiler/linker checks are useful source validation, not a substitute
for an APK build, merged-manifest inspection, R8 execution or phone testing.
No APK build or device PASS is claimed from this session.
Diagnostic logs are under build/phase1b-verification/ and are not app source.

## References

- [AGP 9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes)
- [AGP 9.3+ release optimization DSL](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization)
