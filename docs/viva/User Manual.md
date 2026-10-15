# Kinetic Vault

**Your hand is the key.** Kinetic Vault is an Android app that encrypts text or one file, hides the encrypted bytes inside a PNG image, and unlocks them with a repeatable phone-motion gesture plus an optional passphrase.

This README is the user manual for the features implemented through Phase 4D. You need Android 8.0 or later and an installed copy of the app. Local hiding, revealing, image exploration and library management work without a server. Cloud Ledger and online reading are optional.

## Contents

- [First vault in a few minutes](#first-vault-in-a-few-minutes)
- [Screens and capabilities](#screens-and-capabilities)
- [Hide: covers, secrets and options](#hide-covers-secrets-and-options)Crea
- [Gesture guide](#gesture-guide)
- [Forge and result tools](#forge-and-result-tools)
- [Saving and sharing](#saving-and-sharing)
- [Reveal and decrypted exports](#reveal-and-decrypted-exports)
- [Vault library and history](#vault-library-and-history)
- [Scanner](#scanner)
- [Settings](#settings)
- [Calculator disguise](#calculator-disguise)
- [Cloud Ledger](#cloud-ledger)
- [Learn, About and Integrity Lab](#learn-about-and-integrity-lab)
- [Visual effects and feedback](#visual-effects-and-feedback)
- [Common scenarios](#common-scenarios)
- [Troubleshooting](#troubleshooting)
- [How protection works and its limits](#how-protection-works-and-its-limits)
- [Project reference](#project-reference)

## First vault in a few minutes

1. Open Kinetic Vault. Normally, a short animated logo leads to Home.
2. Tap **Hide a secret**, then **Generate a cover** for an offline starting image.
3. Tap **Next: add your secret**. Leave **Text** selected and type a message.
4. Leave **Compress the secret** enabled. Enable **Add a passphrase (recommended)** and enter a strong, unique passphrase. Remember spaces and capitalization exactly.
5. Wait for the capacity message to say **Fits**, then tap **Next: enroll your gesture**.
6. Choose motion or **Use the Gesture Pad instead of motion**. Tap **Begin first recording**, enter 4–8 moves, then **Done recording**. Record that same sequence a second time.
7. When both recordings match, tap **Forge encrypted PNG**. Keep this screen open until the result appears.
8. Tap **Save PNG** to open export options and save the encrypted image. Use **Share PNG file** only as a Document/File attachment.
9. From Home, tap **Reveal a secret**, select that saved original PNG, record the same moves once, enter the original passphrase and tap **Unlock vault**.

Each image has its own credentials. There is no universal enrolled gesture, password reset or secret-recovery service. Keep an unchanged PNG copy and remember its credentials.

## Screens and capabilities

| Screen or tool | What you can do |
| --- | --- |
| Hide a secret | Choose a device image, take a cover photo or generate a cover; encrypt typed/imported text or one file; choose compression, Ghost placement and a passphrase; enroll a gesture twice. |
| Result | Compare before/after images, inspect channel changes and LSBs, read measured statistics, compare Standard and Ghost demos, save/share the encrypted PNG, and share a display comparison sheet where available. |
| Reveal a secret | Open an original vault image, enter its gesture once, authenticate/decrypt, read text, save text or files, and open a decrypted file in another app. |
| Vault | Browse local encrypted images with thumbnails; search titles; filter creation dates; rename, set/clear expiry, soft-delete entries and export event history as CSV. |
| Scanner | Inspect an original image for a Kinetic Vault header, continue to Reveal, or explore an LSB X-ray when no header is found. Accept images shared/opened from other apps. |
| Settings | Select sensitivity and default input method; control screenshot protection and Reveal auto-lock; configure Cloud Ledger; enable calculator disguise and Nerd Mode. |
| Learn | Use an interactive offline explanation or read the linked steganography article online. |
| Integrity Lab | Run the built-in public-data demonstration of four PNG round trips and inspect its reported results. |
| About | Read the app purpose and the three project workstreams. |

Home includes these destinations and a **Cloud Ledger** status card. **More options** also opens Settings, Learn and About. Scroll longer screens to reach their controls.

## Hide: covers, secrets and options

### Step 1 — Cover

A cover is the visible image that carries the encrypted data. Choose one of three routes:

- **Choose from device:** use Android's document picker to select an accessible image.
- **Take a photo:** open an installed camera app and capture a new cover. Kinetic Vault delegates capture and does not declare the CAMERA permission.
- **Generate a cover:** create a fresh 1,024 × 1,024 plasma/gradient/noise image entirely offline. Repeat to create another cover.

After preparation, the screen shows a preview, dimensions and encoded capacity. Hide may sample oversized covers and flatten transparency onto an opaque background before embedding. Covers have a 3,000-pixel long-side limit, a 9-million-pixel limit and a device-dependent memory budget. The selected original image is not edited in place; the result is a new PNG.

Choosing a larger supported cover usually provides more room. The preview is smaller than the working image and is never the file used for decryption.

### Step 2 — Secret

**Text** lets you type a message or tap **Import text (.txt)** to load UTF-8 text into the text field. Imported text can be edited before continuing. Unicode text is supported. Switching to **File** clears the typed text.

**File** lets you select one accessible file through Android's picker. The app encrypts its exact content bytes and basename. A vault contains text or one file, not both. To carry several files, first create an archive with another app and select that archive, provided it fits.

| Option | What it changes | When to use it |
| --- | --- | --- |
| Compress the secret | Compresses the packed payload before encryption. Default: on. Turning it off still uses the format's GZIP envelope, without compression. | Usually useful for text. Already compressed media/archives may shrink little. Use the capacity meter to decide. |
| Ghost mode | Scatters encrypted payload bits across a repeatable shuffled pixel order. Off selects Standard sequential placement. | Use when you want dispersed placement or want to explore its visual difference. Encryption strength is the same in both modes. |
| Add a passphrase | Combines the exact passphrase with the gesture to derive the encryption key. The option starts unchecked. | Enable for meaningful resistance to offline guessing; gestures alone provide little entropy. |

These choices can be combined: Text/File × compression on/off × Standard/Ghost × with/without a passphrase. Motion and pad entry are further independent choices. Reveal reads the image header and envelope, so you do not manually reselect its Ghost or compression setting.

The live capacity meter measures the actual packed/compressed secret plus encryption overhead. **Fits** enables the next step; **Too large** means use a smaller secret, enable compression or return to choose a larger cover. Capacity is not a raw-file-size promise. The format limits raw payloads to 16 MiB and filenames to 240 UTF-8 bytes; most covers hold much less. A long filename also consumes space.

### Back, interruptions and cancellation

Back moves to the preceding Hide step, except at Cover/Result where it exits. Going back clears prepared secret state and enrollment; prepare/enroll again as needed. During Forge, Back or **Cancel forging** opens a confirmation: continue working or cancel and return to Secret.

Leaving during Gesture or Forge returns the flow to Secret and requires re-entering the secret/passphrase and enrolling again. Leaving other secret-entry screens can also clear sensitive fields. Activity recreation restarts Hide at Cover rather than restoring credentials. Keep the app foregrounded from enrollment through forging.

## Gesture guide

A gesture is an ordered sequence of **4 to 8 named moves**. The app derives credentials from these move tokens, not an exact recording of your hand's trajectory or biometric identity. Order and count must match; a faster/slower performance can work if it produces the same tokens.

### Motion input

Hold the phone face-up in portrait orientation, start recording near level, and move deliberately.

| Move | How to perform it |
| --- | --- |
| Left | Lower the left edge. |
| Right | Lower the right edge. |
| Forward | Lower the top edge. |
| Back | Raise the top edge. |
| Snap | Make a brief downward snap while face-up; keep a secure grip. |
| Flat | Hold the phone flat for about half a second. |

Return near level between tilts. At Normal sensitivity a tilt registers past about 25° and rearms inside about 12°. Leave at least 300 ms between moves. Settings offers Relaxed (20°/10°), Normal (25°/12°) and Strict (30°/14°) tilt thresholds.

Tap **Done recording** after your final move. In motion mode, stillness for about 1.5 seconds after at least four moves can finish recording automatically. A flat hold is itself a move, so watch the move count and use Done to avoid adding Flat unintentionally. Eight is the maximum; use Done when your sequence is complete.

The app uses accelerometer/gyroscope sensing where available, with an accelerometer-only fallback. If sensing is unavailable or hard to repeat, use the pad. Sensitivity changes recognition thresholds, not the credentials stored in an existing image.

### Gesture Pad input

Select the pad checkbox before beginning, start recording, then tap the six named controls in your chosen order. Pause at least 300 ms between taps and choose Done explicitly. You can also use the pad's accessibility actions, or keyboard arrow keys to select a control and Enter to activate it.

Motion and pad emit identical tokens. A motion-created vault can be unlocked with the pad, and a pad-created vault with motion, if the named sequence matches. Switching input method clears the current attempt; start recording again. If the app switches to the pad because sensors are unavailable, tap Record/Begin again.

### Enrollment, strength and private replay

Hide requires two completed matching recordings. After the first, tap **Record the same gesture again**. A mismatch clears both; repeat from the beginning. **Clear both recordings** deliberately resets enrollment.

The strength meter reports the gesture's maximum entropy assuming uniformly random moves: about 10.34 bits for four moves and 20.68 bits for eight. Human choices may be weaker. It does not estimate passphrase strength or treat successful enrollment as evidence of a strong secret.

After matching enrollment, **Privately replay enrolled gesture** shows each named move and an orb preview in a screenshot-protected dialog. Snap and Flat are identified by their labels. Replay is available only for the current enrolled sequence before forging; it is not later credential recovery. Its temporary move copy is cleared on completion, dismissal/reset or pause. Keep the display private from people nearby.

## Forge and result tools

### Step 4 — Forge and matrix rain

Forging packs the content, prepares its GZIP envelope, derives a key, encrypts, embeds bits, renders pixels and saves the encrypted PNG. Stage labels show the current work. A percentage appears only when the engine supplies measurable progress; key derivation and other unmeasured stages use an indeterminate indicator.

**Matrix rain** is the falling binary-column animation on the Forge screen. Its brightness/speed responds to reported progress. The digits are decorative, not the secret, key or ciphertext. It starts while forging is visible and stops when that step ends or pauses. There is no separate matrix-rain switch or interaction.

### Step 5 — Result

The saved output is the encrypted PNG. Result provides these tools:

- **Before/after wipe:** drag the comparison divider to expose the cover on the left and encoded image on the right. It compares display previews without changing either image.
- **Diff Map Pro:** red marks changes in R, green in G and blue in B; combined channel changes mix colors, while black means unchanged. Touch and drag for the loupe and exact source pixel coordinates, RGB values and channel LSBs. Accessibility activation inspects the center pixel.
- **Replay scan sweep:** replay the diff-map highlighting sweep. This is a visualization of already computed differences, not another encryption or a security scan.
- **X-ray:** amplify encoded red/green/blue LSBs to 0 or 255. It reveals the low-bit plane without decrypting content.
- **Statistics:** view changed-pixel count/percentage, maximum channel change and PSNR. Higher PSNR means closer RGB similarity; identical images have infinite PSNR. These are image measurements, not a password-strength or secrecy score.
- **Ghost vs Standard:** open an independent public demo comparing both placement methods; use **Generate again** for a new pair and Back to return.
- **Save PNG / Share PNG file:** export the actual encrypted image.
- **Return home:** finish Hide.

Diff previews combine changed channels within display bins; the loupe magnifies those bins while its numeric RGB/LSB readings use exact source pixels. X-ray is a sampled preview. Natural images also have low-bit patterns.

The Ghost comparison demo uses a generated 320 × 320 cover, the same 2,048-byte public payload and demo credentials, with compression off. Each encryption has fresh salt/IV, so the two ciphertexts and change counts can differ. Standard places payload changes sequentially; Ghost disperses them. Both retain a public header and use the same encryption. This demo does not reprocess your secret.

### Comparison proof sheet

On Android 10+, **Save PNG → Share comparison proof sheet** offers a separate PNG after a cover-disclosure warning. It contains LSB-stripped before/after previews, a coarse 16 × 16 change-density map, dimensions and measured source statistics.

The sheet excludes decrypted content and credentials, but discloses the cover and coarse change information. It is a display artifact, not an unlockable vault or cryptographic proof. The current Android 8–9 export menu does not expose this option.

## Saving and sharing

| Output | Where to find it | What it contains |
| --- | --- | --- |
| Encrypted PNG | Hide Result: Save PNG or Share PNG file | The vault; needs its original gesture/passphrase to decrypt. |
| Comparison sheet PNG | Result export menu on Android 10+ | A display comparison; cannot be unlocked. |
| Decrypted .txt | Reveal text: Export options → Save text (.txt) | Unencrypted revealed text. |
| Decrypted file | Reveal file: Save decrypted file / Open decrypted file | Unencrypted original file content. |
| History .csv | Vault: Export history (.csv) | Local event identifiers, types and timestamps, not secrets. |

**Save PNG** opens a bottom sheet. Android 10+ offers **Save PNG to gallery**, **Save PNG to a chosen folder**, **Share PNG file** and the comparison-sheet option. Android 8–9 offers chosen-folder saving and sharing. Folder saving uses Android's document picker; choose the provider/destination and confirm. You can cancel the picker without saving.

Sharing opens a **Send as Document/File** warning followed by Android's share chooser. In the receiving app, use its file/document attachment route. A photo-sharing route can resize, recompress or convert the image and destroy the vault. If an app cannot preserve the file, save it and attach it manually through another file-transfer method.

Keep the original PNG bytes. Avoid JPEG conversion, cropping, filters, resizing, screenshot copies and social-media image uploads. Simply renaming the file while preserving its bytes does not change the embedded vault. Do not rely on a gallery preview or photograph as your only copy.

## Reveal and decrypted exports

1. Open **Reveal a secret** and tap **Choose vault image**, or tap a Vault library entry. Scanner can also send a detected image to Reveal.
2. The app reads the original image and header without resizing or rotating it. Choose an opaque, accessible original within memory/pixel limits.
3. Choose motion or pad, tap **Record / restart gesture**, enter the original 4–8 moves once and tap Done.
4. If the image was created with a passphrase, its field appears. Enter it exactly, including spaces/case. If it had no passphrase, no passphrase is requested.
5. Tap **Unlock vault**. Successful authentication opens the animated vault door.

For **text**, a brief scramble animation settles into the full readable message. **Export options → Save text (.txt)** writes UTF-8 text through the document picker, normally named revealed-text.txt.

For a **file**, use **Export options → Save decrypted file** to select a destination, or **Open decrypted file** to launch a compatible external app. Open first warns that the receiving app can retain the plaintext. The app prepares a temporary decrypted copy and removes its temporary export when you return. A third-party app's own retained copies are outside Kinetic Vault's control. If no viewer supports the type, save it and use an appropriate app.

Saved decrypted exports are ordinary unencrypted files. Their protection does not follow them outside the vault.

Failed authentication shows a neutral message and an unsuccessful-attempt count. Wrong credentials and damaged authenticated data can produce the same failure; the app cannot tell which happened. Restart recording, re-enter the passphrase or select the untouched PNG. The displayed counter is feedback, not protection against offline guessing.

Auto-lock is on by default. Leaving Reveal or turning the screen off clears revealed content; recording credentials clear on pause regardless of that setting. Export actions prepare a temporary copy before opening an external picker/viewer, so an in-progress export can continue even if the Reveal display locks. Unlock again for another export if needed. Turning off auto-lock can retain the open content while the activity survives; closing/destroying Reveal still clears secret state.

## Vault library and history

The local Vault keeps encrypted image copies in app-private storage and shows image thumbnails, generic/renamed titles, creation dates, capacity/payload information and expiry when present. Forging and loading vaults through Reveal populate local records. Payload size may remain unknown until unlocking. Records are associated with the encrypted image's SHA-256 digest.

- **Open:** tap a row to Reveal its private encrypted image; normal credentials are still required.
- **Search:** type a local title. Search does not search decrypted content or secret filenames.
- **Date range:** choose first and last creation dates; the filter includes both selected days. Search and date filtering can be combined.
- **Clear filters:** restore the unfiltered list. An empty filtered view also offers this recovery.
- **Rename:** long-press a row → Rename → enter a nonempty neutral title → Save. Titles are local metadata, limited to 80 characters. Do not put secrets in them.
- **Set expiry:** long-press → Set expiry → Choose date → choose a date and time.
- **Clear expiry:** long-press → Set expiry → Clear expiry.
- **Delete:** long-press → Delete and confirm. This soft-deletes the list entry. Its local encrypted image and event history remain; exported copies also remain. There is no visible restore/secure-erasure control.
- **Export history (.csv):** open its export sheet, select CSV export, then choose a destination. The file contains event_id, vault_uuid, event and timestamp_ms. It exports local event history rather than a backup of the current filtered image list.

Local events include Hide, successful/failed Reveal, auto-lock and export; sync can also record observed airplane-mode changes. CSV contains operational metadata and may disclose when you used the app, even though it excludes plaintext and credentials.

### Expiry behavior

Expiry refuses new Reveal unlocks after the local timestamp, with checks before and after decryption. To regain access, change or clear expiry in Vault, then choose the image again.

Expiry uses the device clock and local metadata. It does not delete files, close a secret that is already open, modify the encrypted PNG or impose expiry on copies on other devices. It is a local access policy/reminder, not cryptographic self-destruction.

## Scanner

Open **Scanner → Open image** to choose an existing image, or **Take picture** to capture an image through a camera app. You can also share a single image to Kinetic Vault or select it in an Open with chooser from another app; these routes enter Scanner when offered by Android.

Scanner reads original pixels:

- **Signature found:** the image has a structurally recognized Kinetic Vault header. Tap **Open in Reveal** to enter its credentials and authenticate it.
- **No Kinetic Vault data found:** inspect the displayed LSB X-ray or choose another original image.
- **Cannot read unchanged:** choose an accessible opaque image within the supported pixel/memory budget.

A found header does not establish authenticity; successful Reveal authentication does. Scanner cannot decrypt other apps' formats and is not a universal hidden-data detector. X-ray patterns alone prove nothing about hidden content.

A camera capture is useful for exploring natural-image LSBs, not recovering a vault from a screen or printed picture. Photographing a vault does not preserve its original pixel bits. Use the actual original PNG file.

## Settings

Changes take effect after **Save settings**. Connection testing is a separate action.

| Setting | Initial default | Behavior |
| --- | --- | --- |
| Gesture sensitivity | Normal | Relaxed/Normal/Strict adjust tilt entry/rearm thresholds. Choose Relaxed if tilts are hard to register, Strict if small motions cause extra moves. |
| Use Gesture Pad by default | Off | Starts Hide/Reveal with pad selected. You can override it per recording screen. |
| Protect secret screens from screenshots | On | Applies Android secure-window protection to Hide/Reveal. Turn off only when you deliberately want those screens capturable. Nerd Mode still forces protection. |
| Auto-lock when leaving Reveal or turning the screen off | On | Clears revealed content when leaving/screen-off; it does not prevent all third-party plaintext copies. |
| Server URL / Server API key | Empty | Optional full api.php endpoint and its API key for metadata synchronization. |
| Open calculator on launch | Off | Routes normal launches to the calculator after a valid code has been configured. |
| Calculator code | Unset | 6–12 digits. A blank field preserves an existing code; a new valid code replaces it. |
| Nerd Mode | Off | Shows live motion information and detected tokens during recording; forces Hide/Reveal screenshot protection. |

**Test Connection** uses the entered URL/key without saving them. Save enables use of that configuration. Settings errors retain a recovery message; re-enter a new calculator code if saving fails because the code input is cleared.

### Nerd Mode

During motion recording the overlay shows accelerometer readings in m/s², gyroscope readings in rad/s when available, pitch/roll, the sensor-fusion filter formula and detected move names. Accelerometer-only fallback is identified. In pad mode raw sensors/fused angles are inactive and the panel identifies pad input.

The overlay is educational telemetry, not an export or secret-strength score. Tokens are visible to someone looking at your screen while recording. The panel clears when recording finishes/pauses; sensors are stopped when not needed.

## Calculator disguise

1. Open Settings, enable **Open calculator on launch**, enter a memorable 6–12 digit code and Save settings.
2. On the next normal launch, the calculator appears instead of Home.
3. Tap **C** to clear any calculation, type the code alone and press **=** to open Home. Preserve any leading zeros. Do not place the code inside an arithmetic expression.

The calculator supports addition, subtraction, multiplication, division, decimal input, backspace and clear. Calculations execute in entry order rather than scientific operator precedence. An invalid result, such as division by zero, asks you to clear and retry. Inputs clear when the screen pauses. A nonmatching code entry behaves as an ordinary calculation.

To change the code, reach Settings and save a new valid value. To disable disguise, turn its switch off and Save. Blank preserves the existing code. There is no built-in code recovery.

Disguise changes the launcher screen only. The Kinetic Vault name/icon and shared-image entry remain visible. It neither changes vault encryption nor substitutes for the image's gesture/passphrase. The calculator code is stored as a salted derived hash, separate from vault credentials.

## Cloud Ledger

Cloud Ledger optionally sends allowlisted metadata to a configured PHP/MySQL service. It is not image backup, cloud secret storage, an account system or multi-device image restoration.

### Use an existing service

1. Obtain its full api.php URL and API key from the service operator.
2. Enter both in Settings and tap **Test Connection**.
3. After a successful connection, **Save settings** to configure synchronization.
4. Return Home to inspect **Synced**, **Pending** or **Offline**, queued updates and server totals.

The card reports vault/event counts and payload-byte totals for this installation. When totals are cached, it labels them **Last received**. Pending operations retry while the app is visible; failed updates remain queued. Retry delay increases from about 5 seconds to a 5-minute cap. **Retry sync** or saving settings resets retry timing. Changing URL/key requeues metadata for the new destination.

Airplane mode pauses sync and displays a banner. Local features remain usable. Turn airplane mode off and keep the app open to resume. There is no continuous background sync service.

Uploaded fields include random installation/vault/event identifiers, encrypted-file SHA-256, sizes, Standard/Ghost and passphrase-presence flags, timestamps, expiry, revisions, event types and deletion markers. It does not upload image bytes, thumbnails, local titles, image paths, secret filenames, locations, plaintext, gestures, passphrases, encryption keys, salts or IVs. The API key is sent as server authentication, not used to unlock images. Metadata still reveals usage and image identifiers to the service operator.

### Host the optional development service

The supplied server uses PHP 8+, a 64-bit runtime, MySQL/MariaDB and PDO MySQL. See [server setup](../../server/README_XAMPP.md) for the complete configuration and API contract.

For a local XAMPP setup, start Apache/MySQL, copy the server directory to the web root, import schema.sql, and copy config.sample.php to config.php. Configure database access and a random API key of at least 24 characters; the placeholder is rejected. Protect the configuration/schema files using the supplied server rules.

An Android emulator reaches the PC at http://10.0.2.2/kinetic-vault/api.php. A phone needs the PC's LAN address with both devices on the same network; localhost on a phone refers to the phone. Debug HTTP access is restricted to explicitly configured development hosts, so a different LAN address requires a developer resource change. Release configuration requires HTTPS. Use HTTPS and a separate deployment API key for a deployed service.

Server operations cover health, metadata upsert/list/delete, event logging and totals. Android does not download images from the server. The shared API key authenticates the ledger; random device IDs partition records and are not separate user credentials.

## Learn, About and Integrity Lab

**Learn → Offline explainer** opens the bundled interactive page. Tap its example button to toggle a red-channel LSB, see the binary/color change, and read the RGB embedding, encryption and PNG-preservation explanation. This route works offline.

**Read more online** opens the linked Wikipedia steganography article inside the app. If unavailable, use **Retry page** or return to the offline explainer. Remote reading needs connectivity and is distinct from Cloud Ledger configuration. The app limits remote navigation and keeps JavaScript disabled for remote pages.

**About**, available from Home's More options menu, explains the app and lists workstreams A (sensors/gesture/cryptography), B (steganography/images/visuals) and C (UI/database/server/documentation).

**Integrity Lab → Run integrity test** performs four built-in public-data demonstrations: Standard text, Standard binary file, Ghost text and Ghost binary file. It creates a cover, encrypts/embeds, writes PNG, reloads original pixels and compares recovered data. It reports stage/timings, PSNR, changed percentage and a SHA-256 prefix; PASS appears only when its checks succeed. **Run again** repeats it. **Preview light theme** is an in-memory lab preview; fresh processes start dark. The lab has no real-secret enrollment and removes its temporary demonstration PNGs. It is not the screen for hiding personal data.

## Visual effects and feedback

| Effect | Where it appears | Meaning and interaction |
| --- | --- | --- |
| Animated logo | Normal launch | Draws the branding during the short splash; leads automatically to Home. Disguise skips this logo route. |
| Pulsing Home orb | Home | Ambient branding; no gesture capture or secret data is represented. |
| Motion orb and trail | Hide enrollment | Follows recognized pitch/roll; dial segments light as moves are accepted. Use the count to notice unintended moves. |
| Replay orb | Private gesture replay | Visualizes the current matched sequence; named labels explain every move. |
| Matrix rain | Forge | Decorative falling 0/1 columns responsive to measured progress; use the stage label/indicator for actual status. |
| Vault door | Reveal | Opens after successful authentication, denies/shakes after an unsuccessful attempt, resets when locked. It does not add a separate encryption layer. |
| Text scramble | Revealed text | Brief letter animation resolving into the message after decryption; no input needed. |
| Wipe divider | Result | Draggable before/after comparison. |
| Colored diff map and loupe | Result | Shows changed channels; touch to inspect pixel values. |
| Scan sweep | Result diff map | Animated exploration highlight; replay with its button. |
| X-ray | Result; Scanner without a found header | Amplified LSB visualization; neither plaintext nor proof of hidden data. |
| Ghost/Standard maps | Comparison demo | Show placement differences using fresh public demonstration data. |
| Glow cards, entrances and fades | App screens | Dark cyber-vault styling and navigation/status feedback. |
| Haptics and notices | Accepted moves, actions, success/errors | Confirm meaningful interactions; respect device haptic behavior. Snackbars/status panels explain outcomes and recovery. |
| Export bottom sheets and dialogs | Hide, Reveal, Vault | Present available actions and require confirmation for disclosures/deletion. Dismiss to cancel. |

There is no user-facing per-effect switch or general theme selector. Some newer transitions/replay behavior respect disabled system animations, and the offline page supports reduced motion; do not assume every custom effect is disabled by that setting. Core information is also given by labels/counts. Secret-screen screenshot protection can prevent screen captures or cause them to appear blank; this is expected when enabled.

## Common scenarios

### Private note for yourself

Generate a cover → type/import Text → enable compression and a strong passphrase → enroll → Forge → save PNG. Later tap its Vault row or choose the original in Reveal. Read in-app, or export .txt only if you need an unencrypted copy.

### Protect a document, photo or archive

Choose File rather than Text and select the original file. For already compressed formats, compare the capacity result with compression on/off. Choose a larger cover if needed. After unlocking, Save restores its content bytes or Open delegates to a compatible viewer. The visible cover can be unrelated to the file inside.

### Send to someone else or move to another phone

Save/share the encrypted PNG as a file/document. The recipient needs Kinetic Vault, that original file and its exact named move sequence/passphrase; the pad can reproduce your motion sequence. Communicate credentials privately using a separate channel appropriate to your situation. Recipient: download the original → share/open with Kinetic Vault Scanner or choose it in Reveal → authenticate. Cloud Ledger is unnecessary for transfer.

### Use without internet or in airplane mode

Generate/select a local cover, Hide, Reveal and browse Vault normally. Use offline Learn and the public lab/demo. Camera/file viewers depend on installed apps, and a cloud-backed document provider may require its own internet access. Sync stays pending/offline until connectivity returns.

### Motion is unreliable or inaccessible

Select pad before recording, or set it as the default in Settings. Enter the same token order and count; no original motion recording is needed. For motion, try another sensitivity and watch accepted moves rather than assuming every movement registers.

### Present the visual features

Use a nonsensitive message/cover, then demonstrate enrollment orb, matrix rain, wipe comparison, diff loupe, scan sweep, X-ray and Ghost comparison. On Android 10+, share the comparison sheet if cover disclosure is acceptable. Learn and Integrity Lab offer public-data explanations without exposing personal credentials. Nerd Mode displays actual tokens, so use demonstration credentials.

### Manage a collection

Give images neutral titles in Vault, combine title/date filters, set local expiry dates and export history CSV when needed. Keep separately saved encrypted PNGs for portability. Deleting a row hides it from the list rather than erasing all copies.

### Temporarily stop revealing a secret

Leave Reveal or turn the screen off with auto-lock on. When returning, record credentials again. Disable auto-lock only if you intentionally accept retention while the activity survives. Exported plaintext and copies retained by other apps are unaffected.

### Preserve access before changing devices or clearing app data

Keep original encrypted PNG files outside app-private storage and retain their credentials. The ledger cannot restore image bytes. Uninstalling/clearing app data can remove the private library, settings and local history; separately exported images remain wherever you saved them. There is no in-app bulk image-backup/restore workflow.

## Troubleshooting

| Situation | Recovery |
| --- | --- |
| Secret does not fit | Enable compression, reduce content/filename length, or choose a larger supported cover. Wait for the measured Fits result. |
| Unexpected gesture moves | Return near level between tilts, space moves, watch the count and finish explicitly. Try Strict sensitivity or the pad. |
| Gestures do not match | Clear/restart both recordings and repeat the same named moves/count. Avoid unintended Flat holds. |
| Too few moves | Add moves until there are at least four, then Done. |
| Sensors missing | Select pad and begin recording again; accelerometer-only fallback is also supported. |
| Session was paused/restarted | Re-enter cleared secret/passphrase and enroll again; after recreation choose a cover again. |
| No vault signature | Select the original encoded PNG rather than the original cover, screenshot, proof sheet or compressed copy. |
| Unlock denied | Restart the original gesture, check exact passphrase and obtain an untouched PNG. Damage and wrong credentials cannot be distinguished. |
| Expired vault | In Vault, long-press → Set expiry → change/clear it, then choose the image again. |
| Image/memory error | For Hide, use a smaller cover/secret and close other apps. For Reveal/Scanner, keep the original unchanged and try a device with enough memory; resizing it can destroy the vault. Transparent/oversized originals are rejected rather than transformed. |
| Save/storage error | Free storage, choose another writable destination/provider and retry. A local event-recording warning may mean the file saved successfully but history recording failed. |
| Picker unavailable | Enable/install a document provider and retry. Hide can still generate a cover. |
| Camera unavailable/cancelled | Choose an existing image or generate a Hide cover; retry capture if appropriate. |
| Share chooser unavailable | Save PNG first and attach it as a document/file from another app. |
| Decrypted file has no viewer | Save it, then open with an app supporting its type. Unlock again if auto-lock cleared the session. |
| Vault list fails/looks empty | Retry after freeing resources; clear filters if entries are hidden by search/date criteria. You can also choose an exported original through Reveal. |
| Cloud offline/pending | Turn airplane mode off; confirm endpoint/API key; Test Connection and Save; Retry sync while the app remains open. Check host/network policy with the server operator. |
| Online Learn fails | Retry with connectivity or use Offline explainer. |
| Calculator will not open Home | Clear, enter the saved 6–12 digit code alone including leading zeros, then =. No recovery code exists. |
| Forgotten gesture/passphrase | There is no recovery or reset that decrypts an existing image. Private replay is only available during current enrollment. |

## How protection works and its limits

The pipeline is **text/file → packed metadata → GZIP envelope → AES-GCM encryption → RGB least-significant-bit embedding → PNG**. Version 1 derives a 256-bit key using PBKDF2 with 120,000 iterations and authenticates encrypted data and header using AES-GCM. Each forge uses random salt and IV. The encrypted file's original filename is inside the encrypted payload.

Each pixel carries up to three embedded bits, one per red/green/blue channel; each altered channel changes by at most one. Standard uses sequential payload pixels and Ghost a repeatable shuffled order. A fixed public header identifies Kinetic Vault images. Ghost changes placement, not cryptographic strength, and does not make vaults undetectable.

Important boundaries:

- Human-chosen gestures are low-entropy; use a strong, unique passphrase. Key derivation slows guessing but does not create entropy.
- The app is not a biometric authenticator. Someone who knows the tokens and passphrase can use the pad to unlock.
- Recognizing a header is structural detection; only successful authenticated decryption establishes validity.
- PNG files must preserve original pixel values. Visual similarity does not establish that the hidden data survived.
- Screenshot protection does not stop someone photographing the display. A compromised device, keyboard or receiving app can undermine privacy.
- Credentials/transient plaintext are cleared where owned by the app, but Java/platform implementations can retain copies; guaranteed memory erasure is not promised.
- Local titles/history/expiry and uploaded ledger metadata are operational information, not encrypted payload content. Location tagging is not implemented and location is not collected.
- Deletion is soft deletion, expiry is local policy, disguise is a launch-screen choice and comparison sheets are display images. None provides remote revocation, secure destruction or secret recovery.

## Project reference

The app is Java/XML under package com.example.kineticvault, with minSdk 26. Its implementation is organized into three workstreams: motion/crypto, image/steganography/visuals, and UI/database/server/docs. Core classes and the version-1 wire format are frozen.

- [Current implementation state](../STATE_SNAPSHOT.md)
- [Wire format and foundational implementation handoff](../IMPLEMENTATION.md)
- [Local expiry, gesture replay and comparison-sheet concepts](../INVENTIONS.md)
- [Optional server setup and API](../../server/README_XAMPP.md)

STATE_SNAPSHOT describes the current feature set. IMPLEMENTATION and the Phase 1B build/phone documents also contain historical phase-specific instructions; references to a temporary launcher or future phases there do not describe today's complete navigation. This manual describes user behavior and does not supply an APK download, signing setup or deployment service.
