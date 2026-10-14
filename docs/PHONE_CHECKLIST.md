# Phase 1B phone verification checklist

Build and install using BUILD_HEALTH.md. Use one Android 8+ phone or emulator.
These are expected results, not claims that device verification has already run.

- [ ] Local Gradle sync succeeds with bundled JBR 25 and Offline mode disabled.
- [ ] Unit tests, debug APK, unsigned release APK and lint complete successfully.
- [ ] Run connectedDebugAndroidTest: the ten new image tests pass.
- [ ] The adaptive Kinetic Vault icon appears and opens the integrity screen.
- [ ] A fresh process opens in dark mode with readable text and a READY status.
- [ ] No camera, storage, location or network permission prompt appears.
- [ ] Tap Run integrity test: TESTING appears and the button/theme switch disable.
- [ ] The spinner and stage label remain responsive; scrolling still works.
- [ ] The result is a large green PASS with a generated cover preview.
- [ ] Standard and Ghost each show Text PASS and Binary file PASS.
- [ ] Each case shows measured forge/save/reload/unlock timings, PSNR and a hash prefix.
- [ ] Run again three times: all cases pass without rising latency or a crash.
- [ ] Turn airplane mode on and rerun: all four cases still pass offline.
- [ ] Preview light theme: labels, cards, button and system bars remain readable.
- [ ] Run the test in light mode; it still passes. Force-stop/relaunch returns to dark.
- [ ] At 200% font size, content scrolls without clipping and controls remain usable.
- [ ] Check gesture navigation, three-button navigation and a display cutout: no overlap.
- [ ] Press Back during TESTING, reopen and run again: no stale result or file collision.
- [ ] Rotate/fold a large-screen device if supported: recreation returns to a safe idle screen.
- [ ] TalkBack announces the Run button, status, mode labels and generated cover meaningfully.

The device suite checks actual PNG round trips, transparency behavior, an oversized
cover, exact dimensions, EXIF non-rotation, thumbnail bounds, known SHA-256 bytes,
FileProvider path isolation, malformed input and rejection of main-thread image work.

Test PNGs are temporary and removed after each case; they are not Gallery exports.
No Motion, Hide, Reveal, Scanner or cloud-ledger screen is implemented in this phase.
Stop here. Phase 2 requires START PHASE 2.
