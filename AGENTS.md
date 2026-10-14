KINETIC VAULT: working rules (read first, every session)
 
WHAT: Android app (Java + XML, package com.example.kineticvault, minSdk 26, template SDK) that hides encrypted text or files in PNG images, using a phone-motion gesture as the key. The core engine is DONE and FROZEN: the wire format is in docs/IMPLEMENTATION.md. Never change core classes or the format unless I say so.
 
HOW YOU WORK (saves my usage limit)
- Write files straight to disk. Never print file contents in chat.
- Do NOT run Gradle, tests, builds or web searches. I build in Android Studio and paste errors back.
- NO TESTING OR VERIFICATION of any kind: I do all of that myself. Do not check, re-verify or re-document finished work, and write no verification notes.
- Read only docs/STATE_SNAPSHOT.md plus the specific files you must call or edit. Do not re-read files you just wrote.
- Build ONLY the step named in my START line, then stop. Never start the next step.
- Final reply: 3 lines maximum. Append at most 8 lines to docs/STATE_SNAPSHOT.md (new files + public method signatures). No checklists, no verification sections. If the snapshot passes 60 lines, compress it under 40 (file list, public signatures, decisions).
- Never ask me questions. Decide, and note any assumption in one line.
 
CODE RULES
Java + XML only, findViewById, no new Gradle dependencies, Activity Result API, ExecutorService + Handler(Looper.getMainLooper()) for background work, no deprecated APIs (no AsyncTask). Crypto, pixel work, disk and network never run on the UI thread. Simple readable code: methods under ~30 lines, classes under ~250 lines, comments explain WHY. Unregister sensors and receivers in onPause/onStop. All text in strings.xml, content descriptions on icons, 48dp touch targets, edge-to-edge insets, DayNight themes already in res/values. Gesture screens are portrait only. Never log or persist keys, gestures, passphrases or plaintext. Reveal and Scan decode the ORIGINAL bytes with no resize or rotate. Always save PNG. Share PNG as a FILE through FileProvider with a 'send as Document/File' warning dialog. Do not declare the CAMERA permission. Reuse existing ImageUtils, CoverGenerator, VaultEngine and design resources; never duplicate them.
 
LOOK: dark cyber-vault, Material 3 tokens already in res/values, glow cards, animations in res/anim, vector icons in res/drawable. Haptics on meaningful events. Every error state is designed and offers a clear recovery step.
 
GESTURE: tokens LEFT=0, RIGHT=1, FORWARD=2, BACK=3, SNAP=4, FLAT=5, length 4 to 8. A tilt registers past ~25 degrees and returns inside ~12 degrees, at least 300 ms between moves. All thresholds live in Constants. Enrollment = perform twice, both must match. The Gesture Pad fallback emits the same tokens. The strength meter shows bits honestly and nudges toward a passphrase.
 
SYLLABUS (all must be visible by the end): checkboxes, radio buttons, spinner, options and context menus, date and time pickers, touch listener (diff-map loupe), custom Canvas views, toasts and snackbars, explicit intents, AlertDialog + bottom sheet, images from device, .txt import/export, .csv export, SQLite CRUD, MySQL via PHP, implicit intents (share, view, TakePicture, ACTION_SEND receive), ListView with image + text, WebView (asset + remote), broadcast receivers (SCREEN_OFF auto-lock, airplane mode).
 
RUBRIC: original problem + documented user needs, 3+ documented new concepts, polished Material app with graceful failure paths, modular code in 3 workstreams (A sensors/gesture/crypto, B stego/image/visuals, C UI/DB/server/docs), a flawless integrated demo.
