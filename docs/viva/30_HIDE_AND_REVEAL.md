# Hide and Reveal screens

## How to read the screen contracts

An **Activity** is an Android screen; a **View** is a control/drawing component. **XML** is the markup defining view trees. A **layout** is such a tree,installed by setContentView/inflation; a **view ID** is its generated R.id name used with findViewById. A **listener** is a registered callback for a tap/change/event; a **callback** is code invoked later or inline by its owner. **Lifecycle methods** are Android's created/started/resumed/paused/stopped/destroyed notifications. A **Bundle** stores selected screen state for recreation;it is not a place for secrets here. **State** includes flags,selected addresses,temporary arrays,bitmaps and dialog references. **Ownership** identifies who must wipe/release it.

**UI** means user interface. The **main/UI thread** handles controls/drawing. A **worker thread** runs expensive operations. **ExecutorService** queues work;Executors.newSingleThreadExecutor serializes it. **Handler** posts operations to a thread's message queue;**Looper** services that queue;Looper.getMainLooper targets UI. A **generation/ticket** counter discards obsolete asynchronous results. **Synchronous** calls finish before returning;**asynchronous** work later returns through a callback. A lambda (arrow syntax) is a short function;::passes an existing method. **Interfaces** specify required methods;**generics**,such as Callback<T>/List<VaultEntry>,attach result/element types. **Anonymous classes** supply inline implementations;**inner classes** can access an outer instance;**static nested classes** group data without capturing it. @Override marks a parent/interface method;super invokes parent behavior;void means no return value. The tables use parameters' actual source names;callback objects provide their own result rather than a direct method return.

An **Intent** requests an Android operation. **Explicit** intents name an Activity;**implicit** intents ask Android to find a compatible handler,for example SEND/VIEW. A **Uri** identifies provider content,often not a filesystem path. **ContentResolver** opens original streams;**FileProvider** exposes allowed private files through content URIs with temporary grants. **ClipData** also carries grantable references. **MIME** is content type,such as image/png. The **Activity Result API** registers a typed contract/callback before launching a picker/camera/viewer. **exported** means other apps may enter a declared component;**singleTop** routes a new request to an already-top Activity's onNewIntent. **finish()** removes this Activity from the back stack (screen history);it does not guarantee a new Home screen is launched.

A **BroadcastReceiver** handles Android event messages selected by IntentFilter. **Insets** are safe edge padding for status/navigation bars,cutouts and keyboard (IME). **Edge-to-edge** lets content extend under system bars,requiring such padding. A **Snackbar** is transient on-screen feedback;**AlertDialog** is a modal confirmation/input window;**bottom sheet** is a lower-edge action panel. **Haptics** are system touch/vibration feedback;**accessibility** provides actions/descriptions/live announcements for assistive tools. dp means density-independent size;sp also follows text scaling. **DayNight** is a theme family with light/dark palettes. FLAG_SECURE asks Android to protect a window from capture;it does not defeat a camera or compromised device.

A **Bitmap** is an image in memory;a **pixel** is a colour cell;**RGB** are red/green/blue values0..255;**LSB** means each channel's lowest bit. **PNG** losslessly encodes pixels;ordinary JPEG is lossy. **Plaintext** is readable secret content;**ciphertext** is encrypted bytes. **Payload** holds type,basename and content;basename excludes directories. **GZIP** is the always-present compression envelope. **AES-GCM** encrypts and authenticates;**PBKDF2** derives a key from credential and public random **salt**. **IV** is the fresh initialization value;**tag** is authentication result;**AAD** is public header metadata included in authentication. **Ghost** scatters ciphertext positions publicly/reproducibly. **PSNR** (peak signal-to-noise ratio) measures RGB error in decibels(dB),not security. **SHA-256** is a file fingerprint;**UUID** is a random record identifier. A preview/thumbnail is a separate display copy,never extraction input.

**SQLite** is the local relational database;SQLiteOpenHelper manages schema,SQLiteDatabase runs queries/transactions,Cursor iterates rows and ContentValues supplies typed updates. A **transaction** groups committed changes. **SharedPreferences** is named private key/value settings;MySQL is the optional server database. **Metadata** describes operations/images without secret content. **HTTP** is web request transport,HTTPS encrypts it;HttpURLConnection sends requests;**JSON** is named-field structured text. **WebView** embeds web content. A **ListView adapter** supplies row views;LRU cache discards least recently used entries at its size limit. **Soft deletion** sets a hidden/deleted flag while retaining files/history;**expiry** is a local clock-based new-unlock policy. **Canvas** draws;**Paint** styles drawing;**ValueAnimator** varies a numeric value over time and invokes updates. **Exceptions** report failure;try/catch handles it,finally cleans up;try-with-resources automatically closes AutoCloseable resources. synchronized serializes state;volatile publishes fields between threads;AtomicInteger safely increments a shared counter. Java byte/char/int/long are8/16/32/64-bit values;char represents UTF-16 code units,UTF-8 encodes text bytes. Bitwise &,OR,^,~,<<,>>> select/combine/compare/invert/shift bits;casts explicitly convert types. Small real source examples appear at the end of each document.

All Java paths are relative to `app/src/main/java/com/example/kineticvault/`; all layout names expand to `app/src/main/res/layout/<name>.xml`. Lifecycle rows describe overrides actually present;other lifecycle methods retain inherited behavior. View tables enumerate all explicit android:id declarations,including included layouts and shared sheet/row files. Controls created in Java may have no resource ID and are listed separately. Each action table is a left-to-right ASCII call chain;slashes name alternative branches. No screen behavior is inferred from an old phase's launcher description.

## Scope and navigation

This guide covers the real Hide and Reveal screens and the controllers that make their actions work. The other Activities, including Scanner and Vault entry routes, are in [31_OTHER_SCREENS.md](31_OTHER_SCREENS.md). The engine and motion algorithms have their full byte/threshold walkthroughs in [10_CRYPTO_AND_STEGO.md](10_CRYPTO_AND_STEGO.md), [11_IMAGE_HANDLING.md](11_IMAGE_HANDLING.md) and [20_SENSORS_AND_GESTURES.md](20_SENSORS_AND_GESTURES.md).

Both Activities are private manifest components. Home opens them using explicit Intents. Vault opens Reveal with an image Uri in Intent.data; Scanner does the same after inspecting a header. Hide does not parse incoming payload extras. Reveal accepts several Uri shapes defensively, but that does not make it an exported share target. Scanner is the exported receiver for image SEND/VIEW requests. Both gesture screens are portrait-only and handle keyboard resize. Back generally finishes to the existing caller; it does not always launch Home.

## HideActivity: five panels in one Activity

**Purpose and caller:** Home or Vault's empty-state action creates ui/hide/HideActivity.java to turn a cover and secret into an encrypted PNG. Its panels are not separate Activities: a ViewFlipper chooses the cover, secret, gesture, forging or result child. Fields retain the step number, resumed flag, HideWork, CoverStep, SecretStep, GestureStep, ResultStep and MatrixRainView references.

### Layout and every declared ID


### View IDs — app/src/main/res/layout/activity_hide.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `hide_root` | LinearLayout | Hide screen root; safe system/cutout/keyboard padding; child saved-state disabled. | Default visible/enabled unless parent or controller changes it. |
| `hide_back` | MaterialButton | Back through steps or finish; forge step requests cancellation confirmation. | Default visible/enabled unless parent or controller changes it. |
| `hide_step_title` | TextView | Current STEP n OF 5 label. | Default visible/enabled unless parent or controller changes it. |
| `hide_step_progress` | LinearProgressIndicator | Step position,0/25/50/75/100; not engine work progress. | Default visible/enabled unless parent or controller changes it. |
| `hide_notice` | TextView | Recoverable status/error also echoed as Snackbar; hidden on step change. | visibility=gone |
| `hide_steps` | ViewFlipper | ViewFlipper selecting Cover,Secret,Gesture,Forge,Result children. | saveEnabled=false |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

### View IDs — app/src/main/res/layout/hide_step_cover.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `hide_cover_preview` | ImageView | Display thumbnail of prepared cover,not encoded input for extraction. | Default visible/enabled unless parent or controller changes it. |
| `hide_cover_loading` | LinearProgressIndicator | Indeterminate cover loading/preparation indicator. | visibility=gone |
| `hide_cover_info` | TextView | Idle/loading/retry or dimensions and encoded capacity. | Default visible/enabled unless parent or controller changes it. |
| `hide_gallery` | MaterialButton | Launch image OpenDocument picker. | Default visible/enabled unless parent or controller changes it. |
| `hide_camera` | MaterialButton | Prepare private camera output then launch TakePicture. | Default visible/enabled unless parent or controller changes it. |
| `hide_generate` | MaterialButton | Queue offline1024x1024generated cover. | Default visible/enabled unless parent or controller changes it. |
| `hide_cover_next` | MaterialButton | Continue to Secret only when cover ready. | enabled=false |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

### View IDs — app/src/main/res/layout/hide_step_secret.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `hide_secret_type` | RadioGroup | Exclusive Text/File radio group and visibility controller. | Default visible/enabled unless parent or controller changes it. |
| `hide_type_text` | MaterialRadioButton | Select typed/imported text payload. | Default visible/enabled unless parent or controller changes it. |
| `hide_type_file` | MaterialRadioButton | Select one arbitrary file payload; clears typed text. | Default visible/enabled unless parent or controller changes it. |
| `hide_import_text` | MaterialButton | Launch UTF-8 .txt import picker. | Default visible/enabled unless parent or controller changes it. |
| `hide_secret_text` | EditText | Transient multiline plaintext input,not saved/autofilled. | saveEnabled=false |
| `hide_file_controls` | LinearLayout | Container shown only for File choice. | visibility=gone |
| `hide_choose_file` | MaterialButton | Launch arbitrary-file OpenDocument picker. | Default visible/enabled unless parent or controller changes it. |
| `hide_file_status` | TextView | Generic file-selected/no-file status; no filename disclosure in label. | Default visible/enabled unless parent or controller changes it. |
| `hide_compress` | MaterialCheckBox | Compression toggle; remeasure actual GZIP bytes. | checked=true |
| `hide_ghost` | MaterialCheckBox | Public scattering-mode choice; remeasure current prepared input. | Default visible/enabled unless parent or controller changes it. |
| `hide_use_passphrase` | MaterialCheckBox | Show/hide optional phrase field; switching off clears phrase. | Default visible/enabled unless parent or controller changes it. |
| `hide_passphrase` | EditText | Transient exact-case/space password input; no saved/autofill state. | visibility=gone; saveEnabled=false |
| `hide_capacity_text` | TextView | Measured used/available bytes,fit/over-size or preparation errors. | Default visible/enabled unless parent or controller changes it. |
| `hide_capacity_meter` | LinearProgressIndicator | Scaled0..1000capacity usage; colour reflects fit. | Default visible/enabled unless parent or controller changes it. |
| `hide_secret_next` | MaterialButton | Reprepare exact payload and advance only if fits/phrase valid. | enabled=false |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

### View IDs — app/src/main/res/layout/hide_step_gesture.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `hide_orb` | KineticOrbView | Custom orientation orb and accepted-token count dial. | Default visible/enabled unless parent or controller changes it. |
| `hide_nerd` | TextView | Optional transient raw sensor/formula/token overlay. | saveEnabled=false; freezesText=false; visibility=gone |
| `hide_gesture_status` | TextView | First/second recording,repeat,match/mismatch prompt. | Default visible/enabled unless parent or controller changes it. |
| `hide_strength` | TextView | Uniform-token upper-bound bits and phrase advice. | Default visible/enabled unless parent or controller changes it. |
| `hide_use_pad` | MaterialCheckBox | Choose six-control pad instead of sensors; resets enrollment. | Default visible/enabled unless parent or controller changes it. |
| `hide_gesture_pad` | GesturePadView | Accessible custom six-token control,enabled during recording only. | visibility=gone; saveEnabled=false |
| `hide_record` | MaterialButton | Begin first/second performance. | Default visible/enabled unless parent or controller changes it. |
| `hide_gesture_done` | MaterialButton | Finish current recording explicitly. | enabled=false |
| `hide_gesture_reset` | MaterialButton | Clear both enrollment recordings and any enrolled array. | Default visible/enabled unless parent or controller changes it. |
| `hide_gesture_replay` | MaterialButton | Opt-in protected replay of currently matched tokens. | enabled=false |
| `hide_forge` | MaterialButton | Transfer credentials to worker and forge PNG after match. | enabled=false |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

### View IDs — app/src/main/res/layout/hide_step_forge.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `hide_matrix` | MatrixRainView | Decorative binary rain responding to actual progress. | Default visible/enabled unless parent or controller changes it. |
| `hide_forge_status` | TextView | Current actual engine/render/save stage and percentage where known. | Default visible/enabled unless parent or controller changes it. |
| `hide_forge_progress` | LinearProgressIndicator | Engine progress0..1000; indeterminate when total<=0. | Default visible/enabled unless parent or controller changes it. |
| `hide_cancel_forge` | MaterialButton | Show keep/cancel confirmation for active forge. | Default visible/enabled unless parent or controller changes it. |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

### View IDs — app/src/main/res/layout/hide_step_result.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `hide_comparison` | WipeSliderView | Before/after thumbnails plus Java-created accessible SeekBar. | Default visible/enabled unless parent or controller changes it. |
| `hide_diff` | DiffMapView | Channel-change preview with animated sweep and exact-pixel inspector. | Default visible/enabled unless parent or controller changes it. |
| `hide_loupe` | TextView | Exact source pixel coordinates,RGB and LSB values from diff touch/click. | Default visible/enabled unless parent or controller changes it. |
| `hide_diff_replay` | MaterialButton | Restart diff sweep animation. | Default visible/enabled unless parent or controller changes it. |
| `hide_xray` | XRayView | Display amplified encoded low-bit plane. | Default visible/enabled unless parent or controller changes it. |
| `hide_ghost_compare` | MaterialButton | Open independent public-data GhostComparisonActivity. | Default visible/enabled unless parent or controller changes it. |
| `hide_statistics` | TextView | Exact changed count/percentage,max channel delta and PSNR. | Default visible/enabled unless parent or controller changes it. |
| `hide_save` | MaterialButton | Open API-specific PNG/proof export bottom sheet. | Default visible/enabled unless parent or controller changes it. |
| `hide_share` | MaterialButton | Show Document/File warning then share original PNG URI. | Default visible/enabled unless parent or controller changes it. |
| `hide_finish` | MaterialButton | finish() current Hide Activity; label says Return home but does not start Home. | Default visible/enabled unless parent or controller changes it. |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

### Lifecycle and Activity method contracts

All methods below return void unless stated otherwise. Android supplies the Bundle/ lifecycle calls; controls/controllers supply the other calls.

| Method / parameters | Caller -> work and return/update | Failure, lifetime or edge case |
|---|---|---|
| onCreate(Bundle state) | Android -> secure-window preference, edge-to-edge, activity_hide, disable child state saving, installInsets(), construct worker/controllers, register Back/cancel listeners, showStep(0) | Recreated screen starts at cover and announces that sensitive work was cleared; does not restore secrets from Bundle. |
| isForeground() | CoverStep camera-preparation callback -> returns boolean resumed | Prevents launching camera while paused. |
| installInsets() | onCreate -> root WindowInsets listener | Applies system-bar, cutout and keyboard padding with 16dp spacing; returns consumed layout result from listener. |
| showStep(int next) | cover/secret controllers, forging result, Back -> deactivate secret, reset gesture if entering/leaving gesture, choose child/title/progress, animate | Only activates secret measurement when panel 1 and resumed. Rain runs only on resumed forge panel. Keyboard hidden outside secret panel. |
| forge() | Forge click -> validate optional phrase, transfer enrolled tokens/password, showStep(3), clear editable text, work.forge(...) | Missing required phrase returns to secret; missing tokens aborts. Success only affects still-current forge panel, shows Result and haptic; failure returns to secret with message. |
| progress(int label, int done, int total) | HideWork main-thread progress -> label, rain and progress bar | Ignores unless step 3; unknown total means indeterminate; known ratio maps to bar scale 1000. |
| message(int message) | controllers -> string resource in notice and Snackbar, UiPolish.notice | Does nothing after finish/destruction. It is a resource ID, not exception/plaintext text. |
| back() | toolbar Back, forge cancel, OnBackPressedCallback | Cover/result finish. Forge shows confirmation: negative keeps forging, positive invalidates work, clears UI and returns secret. Other panels wipe sensitive work before previous step. |
| hideKeyboard() | showStep -> InputMethodManager/window token | UI convenience; clearing fields is a separate operation. |
| onResume() | Android -> mark resumed, cover.resume(), update capture protection, activate secret if current | Pending camera only launches in foreground. |
| onPause() | Android -> mark paused, result.pause(), reset gesture, stop rain, deactivate secret, invalidate work | If on gesture/forge, return to secret and show paused notice. Sensitive UI cleared. |
| onStop() | Android -> clear sensitive UI | Plaintext/passphrase edit fields are not restored. |
| onDestroy() | Android -> detach cover/result views, close importer and worker | Worker drains queued owners so their finally blocks wipe arrays; does not abandon them with shutdownNow. |

### User actions and callbacks

| What the student/user sees | Full action chain | Storage, failure and security |
|---|---|---|
| Pick a cover | Cover button -> CoverStep.openGallery -> OpenDocument.launch(image/*) -> result Uri -> CoverStep.load -> HideWork.run -> HideStore.load -> ImageUtils.decodeCover -> callback preview/capacity | Decoder may scale a cover; no secret embedded yet. Picker launch errors show message. Cancellation leaves current choice. |
| Take a cover photograph | Camera -> CoverStep.openCamera -> HideWork.run -> HideStore.prepareCapture -> main callback/pendingCamera -> CoverStep.resume -> TakePicture.launch -> camera callback -> HideStore.finishCapture -> ImageUtils.decodeCover | Private temporary JPEG is only an input cover, deleted after decode/cancel. The forged output remains PNG. FileProvider grants camera access; no CAMERA permission declared. |
| Generate a cover | Generate -> CoverStep.load -> HideWork.run -> HideStore.generate -> CoverGenerator.generate(1024,1024,System.nanoTime()) -> thumbnail -> callback | Owned cover plus at-most-512 preview in memory. Failure disables Next and offers retry. |
| Continue from cover | Next listener -> HideActivity.showStep(1) | Enabled only when a valid cover is ready; no extra Activity/Intent. |
| Type or choose a file | TextWatcher / RadioGroup / file result -> SecretStep.schedule -> delayed measure -> HideWork.measure -> HideStore.measure -> SecretInput.read -> PayloadCodec.pack -> GzipCodec.compress -> capacity callback | File picker uses */*; text mode takes a char[] copy. Typing is not persisted. Selecting file mode clears typed text. |
| Import a .txt document | Import -> OpenDocument(text/plain) -> TextImport.read -> strict UTF-8 worker -> char[] callback -> select text mode/set text/schedule | Invalid UTF-8, size/memory/read errors produce import feedback. Delivered temporary chars are wiped after callback. |
| Change compression/Ghost | CheckBox change -> SecretStep.schedule -> measured packed/GZIP length -> showCapacity | Both choices become prepared payload options. GZIP envelope exists even with compression disabled. Ghost changes placement, not capacity or encryption strength. |
| Enable phrase | CheckBox listener -> phrase visibility; disable -> clear field | It does not change ciphertext length: tag overhead stays fixed. Empty enabled phrase blocks progress/forge. |
| Continue from secret | Next -> SecretStep.prepare -> validate input/phrase -> HideWork.measure(keep=true) -> HideStore stores fitting Payload -> showStep(2) | Oversized/invalid input stays here with capacity/error and disabled Next. This pass retains payload ownership for forging. |
| Choose motion or pad | usePad change -> GestureStep.reset -> pad visibility | Both emit identical token values; changing mode erases both enrollment attempts. Preference provides initial choice. |
| Record enrollment | Begin -> GestureStep.begin -> GestureSession callbacks; sensors.start OR GesturePadView token listener -> current.addToken/onMotion -> progress orb/strength | Motion unavailable switches pad on and asks to begin again; gyro missing uses accelerometer warning. Nerd overlay is temporary telemetry, not stored key material. |
| End first/second recording | Done -> current.done -> GestureStep.finishAttempt -> keep first / first.matches(second) -> takeTokens -> Forge enabled | Too short keeps an explanatory error; mismatch resets both attempts. Completed duplicate attempts are closed/wiped. |
| Replay enrolled gesture | Replay -> GestureReplay.show -> timed next() -> orb angles/name -> clear | Secure dialog shows an owned token clone at 900ms intervals; replay clear/dismiss cancels ticks and wipes clone. Replay never stores gesture. |
| Reset gesture | Reset -> GestureStep.reset | Stops sensors/animations, closes sessions and zeros enrolled tokens; re-enroll to continue. |
| Forge | Forge -> HideActivity.forge -> HideWork.forge -> HideStore.forge -> HideForge.run -> VaultEngine.hide -> ImageUtils.fromPixels/savePng -> ImageUtils.statistics/DiffMapData -> VaultLogger.logHide -> main success -> ResultStep.show | Crypto/pixels/disk on single worker. Cancellation guards reject obsolete generations. No plaintext/gesture stored in history. Save/recording failures can prevent successful completion and show retry message. |
| Replay result reveal sweep | Result sweep button -> DiffMapView.startSweep | Animation only; does not re-encrypt or mutate PNG. |
| Drag comparison/loupe | WipeSliderView SeekBar listener -> WipeComparisonView.setFraction; DiffMapView touch listener -> source coordinates/LSB report | Uses display copies; no secret interpretation. Drag clamps to image and prevents parent interception; release supports click accessibility. |
| Open Ghost demonstration | Result Ghost button -> explicit Intent(GhostComparisonActivity.class) | Public synthetic demonstration, independent of user's payload. Documented in other-screens guide. |
| Finish | Done -> host.finish | Existing previous Activity resumes; result view references released during destruction. |
| Back/cancel forging | Back/cancel -> HideActivity.back -> AlertDialog -> clearSensitive/showStep or keep | Positive cancellation does not serialize credentials for recovery. Start secret preparation again. |

### CoverStep contract

ui/hide/CoverStep.java is constructed by HideActivity. It owns preview/info references, picker/camera launchers, ready and pendingCamera; image ownership belongs to HideStore. Constructor listeners invoke openGallery(), openCamera(), generated load and host.showStep(1).

| Method | Parameters/result and calls | Edges |
|---|---|---|
| openGallery() | no arguments, void; launches OpenDocument | Runtime picker error -> host.message. |
| openCamera() | no arguments, void; busy -> worker prepareCapture -> callback assigns pending Uri and resume() | Failure clears busy/message. |
| resume() | no arguments, void; pending Uri transferred to camera.launch | Delays launch until Activity resumed; launch failure deletes capture and reports error. |
| CameraContract.createIntent(Context context, Uri input) | returns Intent from TakePicture superclass, with READ/WRITE grants and ClipData | Camera can access exact temporary file through provider. This nested contract does not request runtime camera permission. |
| load(Callable<HideStore.CoverInfo> action) | void; clear ready/old preview, run action, callback attaches preview/dimensions/capacity | Failure leaves retry guidance; no Next until success. |
| setBusy(boolean busy) | void; gates acquisition and Next | ready and !busy required for Next. |
| deleteCapture() | void; queues store.deleteCapture | Failure callback only reports message; success ignored. |
| close() | void; clear pending URI and detach preview | Store later recycles owned images. |

### SecretStep, SecretInput and TextImport contracts

ui/hide/SecretStep.java is created by HideActivity and owns all secret-form controls, file Uri, active/busy/fits flags, version ticket, delayed measurement Runnable and a TextImport. No passphrase is deliberately converted to a permanent String by this controller. Text/file-content snapshots belong to SecretInput, then to Payload; editable Android widgets still contain text until explicitly cleared.

| Method / listener | Inputs, result, caller and callees | Failure / cleanup |
|---|---|---|
| bind() | constructor -> RadioGroup, text watcher, compression/Ghost/phrase, picker/import/Next listeners | Watcher's before/on callbacks do nothing; afterTextChanged calls schedule. File/type changes update visibility and schedule. |
| isFile(), hasInput() | no args; return boolean selected radio / file!=null or text length>0 | Empty selected file/text cannot proceed. |
| activate(), deactivate() | Activity panel/resume changes; void | Activate marks active and schedules; deactivate invalidates version/removes delay and busy state. |
| schedule() | form changes; void -> version++, fits=false, Next disabled, remove old delay; if active/not busy/input, post measure in 300ms | Debounce means measure only after a short pause in typing. Inactive stale callbacks cannot update panel. |
| snapshot() | no args -> SecretInput(text chars OR file Uri, compress, ghost) | Ownership transferred to worker; never both text and selected file read as one payload. |
| measure() | delay -> snapshot -> work.measure(keep=false) -> callback showCapacity | Only same version and active panel accepts result. Error updates measurement label. |
| showCapacity(HideStore.Capacity result) | void; fits(), needed/available, cyan/danger, 1000-scale ratio | available=0 uses full meter rather than divide by zero. Ratio clamped. |
| prepare() | Next -> guard fitting/not busy/input/phrase; cancel delay, ticket++, busy, measure(keep=true) | Current fitting result opens gesture; failure re-enables form and shows message. |
| validPassphrase() | no args -> boolean; Activity and prepare call it | Checked but empty sets field error and message. |
| hasPassphrase() | no args -> boolean checkbox checked AND non-empty phrase | Used for forge requirement. |
| takePassphrase() | no args -> owned char[] (or length zero), then clears field | HideWork must wipe transferred array. |
| copy(EditText view) | static -> new char[] copied from Editable | Avoids introducing another secret String. |
| clearSensitiveUi(), close() | Activity -> cancel import and clear text/phrase; close closes importer (field clearing is the separate clearSensitiveUi call) | Selected document Uri/options are not plaintext bytes, and need not be zeroed as credentials. |
| setBusy(boolean value) | void; gates editors, radio/options, file/import, Next | Busy blocks new preparation; Next also requires fits. |

SecretInput.java is an AutoCloseable owned snapshot. Public constructor receives char[] text, Uri file, boolean compress, boolean ghost. read(ContentResolver resolver) returns PayloadCodec.Payload: either strict UTF-8 text bytes, or file bytes plus sanitized display basename. encodeText() returns bytes using charset encoder; readFile(resolver) opens stream and enforces Constants.MAX_PAYLOAD_BYTES; filename(resolver) queries provider DISPLAY_NAME, defaults to secret.bin, replaces forbidden/control characters and limits name to 240 UTF-8 bytes. ensureRoom(long bytes) returns void or throws IOException when roughly bytes*6 + 24MiB exceeds remaining heap. close() wipes its text array. Its WipingBuffer replaces/grows byte storage and wipes old arrays; close wipes current backing storage. On an unsuccessful ownership transfer read() wipes temporary content. Untrusted provider filenames cannot become arbitrary output paths.

TextImport.java is created only by SecretStep: worker, main Handler, generation and closed control ownership. read(Uri uri, callback) launches strict UTF-8 decoding off UI, accumulating chars from a 4096 initial buffer up to MAX_PAYLOAD_BYTES/4 characters, growing and wiping superseded arrays. Cancellation checked during reading. Main callback runs only for the active generation/alive host and wipes delivered chars in finally; worker finally wipes its buffer. Exceptions and OutOfMemoryError produce txt_import_error. cancel() increments generation; close() marks closed, increments generation, and drains shutdown. There is no persistent imported-text cache. TextImport callback receives chars temporarily; SecretStep copies them into its editable UI.

### GestureStep and GestureReplay contracts

GestureStep.java is built by HideActivity. Fields hold first/current GestureSession, enrolled byte[], SensorFusionManager, pad/orb/NerdOverlay/strength/status controls and GestureReplay. The recognizer listener sends token/time to current.addToken, and pitch/roll/still/time to orb/session; telemetry listener feeds overlay. Pad returns whether the active session accepted its token. No active session means no token added.

| Method | Caller, inputs/result and calls | Edge handling |
|---|---|---|
| bind() | constructor -> Begin/Done/Forge/reset/replay/checkbox listeners | Done only calls current.done if a session exists. |
| begin() | record listener; no args, void -> new session listener, status/orb/strength, pad or startSensors | Guard current/enrolled state; callbacks update count, finishAttempt or too-short message. |
| startSensors() | begin -> sensors.start Mode | Unavailable triggers checkbox reset and pad guidance; accelerometer-only warns while retaining usable flow. |
| finishAttempt() | session finish -> stop sensors/overlay/orb, retain first or compare second | First completed run requests repeat. Mismatch resets. Match transfers first's tokens, closes duplicate, enables Forge/replay with haptic. |
| takeTokens() | host forge; no args -> byte[] or null | Transfers enrolled ownership, nulls reference/disables Forge and clears replay. |
| reset() | mode/reset/panel/pause -> stop/close/wipe session and enrollment state | Restores record controls, clears visuals and replay. No Bundle gesture recovery. |

GestureReplay.java is made by GestureStep. show(byte[] tokens) clones tokens, constructs an AlertDialog and Java label/KineticOrbView with no resource IDs, secures both host/dialog, then next() animates token names/angles. next() has no parameters/return; repeats every 900ms with tilt angles of 25 degrees and clears its token copy after final token. clear() cancels Handler work, wipes clone, dismisses dialog and restores host secure flag from protection/nerd preferences. Dismiss/reset/pause must be understood as wiping the replay data, not erasing Android's already rendered display from physical observation.

### HideWork, HideStore and HideForge: who owns expensive work

HideWork.java implements AutoCloseable (an object with a close cleanup contract). HideActivity creates it with Context; it uses application context in HideStore. Its Callback<T> interface has success(T value), failure(int message); Progress has update(int label,int done,int total). These return void. The same worker serializes cover loading, preparation, encryption, exporting and cleanup. main posts only callbacks/visual changes. Fields: store, worker, main, AtomicInteger secretGeneration, volatile closed.

| Method | Contract and call chain | Failure/cleanup |
|---|---|---|
| run(Callable<T> action, Callback<T> callback) / private overload with Runnable cleanup | Controllers submit work; void. Worker action.call -> main success iff !closed; always cleanup.run | CancellationException intentionally silent; OOM maps memory message, other Exception operation message. Already closed still runs cleanup. |
| measure(SecretInput input, boolean keep, Callback<Capacity> callback) | Guard ticket -> store.measure -> stale check | Finally input.close; stale result clears prepared payload and throws cancellation. UI controller additionally checks its version. |
| forge(byte[] tokens,char[] passphrase,Progress progress,Callback<Result> callback) | Guard -> store.forge; progress posts only current generation; wraps completion gate | Finally Arrays.fill credentials even on cancellation or errors. |
| clearSensitive() | no args; void -> generation increment immediately, queue store.clearPrepared | Obsolete work sees guard; queue follows serial ownership order. |
| guard(int generation) | void or CancellationException | closed or ticket mismatch cancels. |
| fail(Callback<T>,int message) | void -> main failure iff open | Message is safe resource, not raw secret exception. |
| stageLabel(WorkProgress.Stage stage) | static int resource; maps PACK/COMPRESS/KEY/ENCRYPT/EMBED/default | Shared by worker pipeline and displayed stage text. |
| close() | void; closed true, generation++, queue store.close, worker.shutdown | Drains instead of dropping tasks owning arrays. |

HideStore.java belongs entirely to that worker. Its application Context, cover/coverPreview, prepared Payload, compress/ghost flags, capture File, output File, rendered Result and optional proof File are not independently mutated on UI. CoverInfo nested immutable result holds preview,width,height,capacity; constructor computes StegoCore.capacityBytes(width*height). Capacity holds needed/available; fits() returns needed<=available.

| Method | Inputs/return and caller/callees | Edge/ownership |
|---|---|---|
| load(Uri uri) | CoverStep via work -> clearCover, ImageUtils.source/decodeCover, adopt; returns CoverInfo | Invalid/large image exception propagates to worker feedback. |
| generate() | returns CoverInfo for 1024-square generated image | Clears old cover first. |
| adopt(Bitmap bitmap) | returns CoverInfo with 512 thumbnail; retains originals | On thumbnail failure recycles incoming bitmap and rethrows. |
| prepareCapture() | returns FileProvider Uri for cache/capture/randomUUID.jpg | Deletes prior capture, creates directory/file, IOException if unavailable. |
| finishCapture() | returns decoded/adopted CoverInfo | Missing capture fails; finally deleteCapture even decode fails. |
| deleteCapture() | void, delete file and null | File.delete return is not a secure-erasure guarantee. |
| measure(SecretInput input,boolean keep) | returns Capacity(zipped.length+TAG_BYTES, image capacity) | pack and GZIP arrays wiped in finally; payload closed unless fitting keep transfers ownership to prepared. |
| forge(tokens,passphrase,progress,guard) | returns HideForge.Result via run | Requires cover and prepared; releases previous visuals; finally clearPrepared; stores output/rendered on success. |
| clearPrepared() | void -> Payload.close/null | Wipes owned plaintext bytes. |
| clearCover() | void -> clearPrepared, recycle cover and preview/null | Close or new acquisition; callers detach preview first. |
| proofUri() | returns Uri; lazily ProofSheet.create then HideExporter.shareUri | Reuses optional proof until rendered release. |
| releaseRendered() | void -> delete proof, recycle before/after, visuals.close, null | Does not delete forged output file. |
| close() | void -> clearCover, deleteCapture, releaseRendered | Original encrypted shared PNG is retained. |

HideForge.java is a static workflow, not an Activity. HideStore calls run with Context, cover Bitmap, Payload, credentials, compression/Ghost choices, progress and cancellation guard; it returns Result or throws. It checks image/memory room, gets pixels, calls VaultEngine.hide with progress/guard, reconstructs a Bitmap, and finishes the image. Its finish stage saves cache/shared/kinetic-UUID.png, computes ImageStats/ImageStats, creates 512 thumbnails and DiffMapData, records the hide through VaultLogger and returns Result. Result fields hold file,before,after,stats,visuals; ResultStep displays them. The reconstructed bitmap is recycled in run finally. Exact original/encoded arrays transfer to DiffMapData on successful finish and are wiped by its close; a failure before this transfer has no blanket array-wiping finally in HideForge itself; an incomplete finish deletes its newly created output and disposes visuals. Cancellation can invalidate a result before UI consumes it; no key is written to output metadata.

### ResultStep exports and shared bottom sheet

ResultStep.java is constructed by HideActivity and holds the current comparison reference, optional BottomSheetDialog and CreateDocument(image/png) launcher. show(HideForge.Result result) binds comparison/diff/xray/stats, starts sweep and clears busy state. pause() dismisses sheet and stops sweep. close() detaches comparison/diff/xray so store cleanup can recycle them. Constructor binds loupe, sweep replay, Ghost, save/share and finish controls.


### View IDs — app/src/main/res/layout/sheet_export.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `export_note` | TextView | Caller-supplied disclosure/storage note in shared bottom sheet. | Default visible/enabled unless parent or controller changes it. |
| `export_options` | LinearLayout | Container receiving Java-created labeled export buttons. | Default visible/enabled unless parent or controller changes it. |
| `export_cancel` | MaterialButton | Dismiss export sheet without running an option. | Default visible/enabled unless parent or controller changes it. |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

ExportSheet.java is a static shared utility. show(host,note,labels,actions) inflates this sheet and returns BottomSheetDialog. It creates one Button for each label, each at least 56dp high, without a declared XML ID. Each click haptics, dismisses, then runs the matching Runnable only while host alive; Cancel just dismisses. Dialog secure flags follow the host. Its inset callback adds 24dp spacing around bars/keyboard; dp converts dimensions. It has no retained secret state.

| User action / method | Full flow | Data/failure/security |
|---|---|---|
| Save options / exportOptions() | Result save -> ExportSheet.show -> savePng/saveDocument/warnShare/warnProof | Android 29+ offers gallery; older versions use document route. Proof is a separate report, not the carrier image. |
| savePng() | API29+ -> save(HideExporter.saveGallery); else saveDocument() | Gallery writes MediaStore Pictures/KineticVault with pending flag. No broad storage permission. |
| saveDocument() | CreateDocument.launch(KineticVault.png) -> Uri callback -> save(HideExporter.saveDocument) | Cancel clears busy. Launch/provider errors show export message. |
| save(Callable<Boolean> action) | busy(true) -> work.run -> callback busy(false)/saved or saved-event-warning | Boolean distinguishes actual save success from metadata event failure. Failure offers retry. |
| warnShare() | AlertDialog warning -> positive prepareShare(false); negative cancel | User told send as Document/File because messaging image modes may modify pixels. Cannot force recipient to preserve them. |
| warnProof() | AlertDialog disclosure -> positive prepareShare(true) | Proof exposes cover and aggregate visual statistics; separate explicit consent click. |
| prepareShare() / prepareShare(boolean proof) | worker store.proofUri OR HideExporter.shareUri -> main ACTION_SEND image/png EXTRA_STREAM + READ + ClipData -> chooser -> logShare if carrier | Runtime missing-handler/launch failures or worker errors show share/export error; busy cleared. Chooser launch is logged, not confirmed recipient delivery. |
| logShare() | work.run -> VaultLogger.logExport -> callback | Local event error shows record message; share launch may already have succeeded. |
| busy(boolean value) | export preparation -> gate Save/Share | Does not imply every result control is disabled. |

HideExporter.java has no instance fields. saveGallery(Context,File) returns Boolean event-record result after exact-byte copy to MediaStore; on incomplete write it deletes inserted row. saveDocument(Context,File,Uri) copies exact file bytes then records event; returns Boolean. recordExport(Context,File) catches IO/runtime and returns false, preserving successful save outcome. shareUri(Context,File) returns FileProvider Uri only for existing file or throws IOException. copy(ContentResolver,File,Uri) returns void using resolver output mode wt and 8192-byte stream buffer. It never recompresses/re-encodes the carrier. ProofSheet.create(Context,Result) returns a 1080x660 private PNG report, or throws; panel(Canvas,Paint,Bitmap,int,String) draws safe previews and density(Canvas,Paint,Bitmap,int) draws 16x16 aggregate density. Preview RGB low bits are cleared with 0x00fefefe; it is deliberately not a reusable carrier or cryptographic proof. Temporary drawing bitmaps/arrays are cleaned in finally. Store retains/deletes the proof file with rendered result lifetime.

## RevealActivity: inspect, authenticate, then display/export

**Purpose and callers:** ui/reveal/RevealActivity.java accepts a candidate original PNG, records the same gesture once and recovers authenticated text/file. Home supplies an empty Intent; Vault/Scanner supply data. Fields retain RevealWork, RevealGesture, sheet, door/scrambler/password controls, parsed header, selected Uri, busy/registered/revealed/external flags and local attempts count.

### Layout and every declared ID


### View IDs — app/src/main/res/layout/activity_reveal.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `reveal_root` | ScrollView | Reveal scroll root; safe system/cutout/keyboard padding; secret descendants not saved. | saveEnabled=false |
| `reveal_back` | MaterialButton | finish() current screen; no fresh Home intent. | Default visible/enabled unless parent or controller changes it. |
| `reveal_door` | VaultDoorView | Custom locked/open/denied door; opens only after worker success. | saveEnabled=false |
| `reveal_nerd` | TextView | Optional transient motion/recognized-token overlay. | saveEnabled=false; freezesText=false; visibility=gone |
| `reveal_pick` | MaterialButton | Lock current state then launch original-image OpenDocument. | Default visible/enabled unless parent or controller changes it. |
| `reveal_notice` | TextView | Status,stage,neutral denial and recovery message. | Default visible/enabled unless parent or controller changes it. |
| `reveal_progress` | ProgressBar | Horizontal work indicator,indeterminate for unknown total. | visibility=gone |
| `reveal_attempts` | TextView | In-memory unsuccessful-unlock count; no timed lockout. | Default visible/enabled unless parent or controller changes it. |
| `reveal_credentials` | LinearLayout | Recording controls/phrase/unlock container,shown after structural header load. | visibility=gone |
| `reveal_gesture_status` | TextView | Recording prompt,count and completed-gesture status. | Default visible/enabled unless parent or controller changes it. |
| `reveal_use_pad` | CheckBox | Switch input route and clear current attempt. | Default visible/enabled unless parent or controller changes it. |
| `reveal_pad` | GesturePadView | Same accessible six-token pad for one Reveal attempt. | visibility=gone; saveEnabled=false |
| `reveal_record` | MaterialButton | Record/restart one original gesture performance. | Default visible/enabled unless parent or controller changes it. |
| `reveal_done` | MaterialButton | Explicitly complete current session. | enabled=false |
| `reveal_passphrase` | EditText | Original exact phrase,shown only if header flag requests it. | saveEnabled=false; visibility=gone |
| `reveal_unlock` | MaterialButton | Submit completed token array and phrase to worker. | enabled=false |
| `reveal_text` | TextView | Nonselectable unsaved transient revealed text with scramble animation. | visibility=gone; saveEnabled=false; freezesText=false |
| `reveal_files` | LinearLayout | Post-authentication plaintext export notice/actions container. | visibility=gone |
| `reveal_save` | MaterialButton | Open decrypted-file/text export options sheet. | Default visible/enabled unless parent or controller changes it. |
| `reveal_open` | MaterialButton | Disclosure warning before plaintext ACTION_VIEW in another app. | Default visible/enabled unless parent or controller changes it. |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

### Lifecycle, incoming Intents and method contracts

| Method | Caller, parameters, result and callees | Failure / cleanup |
|---|---|---|
| onCreate(Bundle state) | Android -> FLAG_SECURE initially, edge-to-edge/layout/insets, disable child saving, create work/gesture/door/scrambler, bind | Candidate Uri priority: saved source, Intent.data, IntentCompat EXTRA_STREAM, first ClipData; if present load. No password/plaintext restoration. |
| bind() | onCreate -> picker, Back, Unlock, save options, open-warning listeners | Pick first lock()s current content; picker launch failure shown. Back finish. Open requires warning confirmation. |
| installInsets() | root insets listener -> bars/cutout/keyboard padding | Form remains usable with keyboard. |
| load(Uri uri) | picker/incoming -> selected=uri, lock, reset header/attempts, busy, work.load | Success installs header and required phrase visibility, credential prompt; failure clears busy and shows safe message. |
| unlock() | Unlock -> guard busy/header, validate required phrase, takeTokens, copy phrase chars, clear form/gesture, busy, work.unlock | Empty required phrase does not launch work; null tokens abort. Progress callback updates stage; success calls unlocked then optional event warning; failure denied. |
| unlocked() | decrypt callback -> revealed=true, busy=false, hide credentials, haptic, door.open(completion) | Door completion exposes file actions and text or file state. Text chars taken from worker and handed to scrambler. |
| denied(int message) | failure -> busy false, attempts++, gesture clear, notice, deny animation/shake/failure haptic | No detailed authentication oracle exposes whether password or gesture was wrong. Expiry/record/storage errors still have their own user-safe messages. |
| progress(WorkProgress.Stage stage,int done,int total) | main progress -> KEY/EXTRACT/UNPACK/default DECRYPT labels/bar | Unknown total indeterminate, otherwise 100-scale progress and percentage. |
| exportOptions() | save click -> sheet actions for current type | Guard not busy/hasPayload; text offers .txt; file offers save/open. |
| export(boolean open) | export action -> busy, work.prepareExport -> main result -> external=true then CreateDocument or ACTION_VIEW | Text MIME uses textSaver, other uses generic saver; viewer gets Uri+type+READ/ClipData. Missing handler runtime -> clear external, delete temp, show open error. |
| cancelledExport() | null picker Uri -> work.discardExport and retry guidance | Plaintext staging file is deleted, not kept awaiting another picker. |
| setBusy(boolean value) | load/unlock/export -> picker/actions/progress/credential visibility | Credentials only visible when !busy, !revealed and header exists. |
| message(int message) | safe string resource -> notice/UiPolish.notice | Ignores finishing Activity. |
| lock() | choose/lifecycle/receiver -> autoLock event if relevant, clear gesture/password/scrambler/door/work payload, reset visibility | Invalidates decrypt generation. Deletes export unless an intentional external operation is active. Encrypted pixels may remain for retry. |
| onStart() | Android -> register SCREEN_OFF receiver NOT_EXPORTED, work.protection callback | Async setting result enables FLAG_SECURE for protection or nerd, otherwise clears it. |
| onPause() | Android -> dismiss sheet, clear gesture/password; busy work causes lock | Non-busy visible plaintext uses stop/autolock policy. |
| onStop() | Android -> if autoLock lock; if also screen noninteractive discard export and finish; unregister receiver | Leaving to a picker/viewer preserves deliberate pending export until callback unless screen-off cleanup overrides. |
| onSaveInstanceState(Bundle out) | Android -> selected image source only | No gesture, phrase, plaintext, payload or unlocked status saved. |
| onDestroy() | Android -> clear gesture, phrase, scrambler, door, work.close | Close drains worker and removes temporary export. |

The dynamic screenOff anonymous BroadcastReceiver handles only Intent.ACTION_SCREEN_OFF. Its onReceive(Context,Intent) calls lock(), discardExport() and finish() when autoLock is enabled. Registration exists only while started; onStop unregisters. It is not a manifest receiver. A receiver is a callback, not an additional Activity or worker thread.

### Every Reveal action / result callback

| User/event | Full ASCII call chain | Storage, errors and recovery |
|---|---|---|
| Choose PNG | Choose -> lock -> OpenDocument(image/*) -> result if non-null -> load -> RevealWork.load -> ImageUtils.decodeExact(original,true) -> StegoCore.readHeader -> RevealLedger.capture/adopt -> main header | No resize/rotate; preview must never replace original bytes. No vault header/read/memory/ledger errors show notice; choose a different original PNG. |
| Start motion or pad | Record -> RevealGesture.begin -> GestureSession -> SensorFusionManager / GesturePadView -> session.addToken/onMotion | Only one run here, compared indirectly by authenticated decrypt. Sensor/pad callbacks update temporary count/overlay. |
| Done | Done -> session.done -> onFinished -> stop sensors -> session.takeTokens -> Unlock enabled | Too-short callback shows minimum guidance, no decrypt. |
| Change pad mode | checkbox -> RevealGesture.clear -> pad visibility | Clears any previous candidate; re-record. |
| Unlock | Unlock -> RevealActivity.unlock -> RevealWork.unlock/decrypt -> RevealExpiry.requireAllowed -> VaultEngine.reveal -> authenticate/decrypt/decompress/unpack -> requireAllowed -> logReveal -> main unlocked -> VaultDoorView.open -> TextScrambler.show / file actions | Credentials wiped in finally. Wrong credentials/corruption denies without plaintext. Local expiry checks occur before and after expensive work. |
| Save revealed text | Save -> ExportSheet -> export(false) -> RevealWork.prepareExport -> RevealExport.create -> textSaver.launch(name) -> Uri -> work.save -> RevealExport.save -> ledger export event -> callback | Plaintext private temp exists only for deliberate export; destination chosen by user persists outside app cleanup. Cancellation deletes temp. |
| Save revealed file | Save -> sheet -> export(false) -> prepareExport -> generic CreateDocument -> work.save | Same cleanup; stored basename is sanitized upstream, provider uses random private backing file. MIME may select textSaver for an actual text/plain file too. |
| Open file | Open -> warning dialog positive -> export(true) -> prepareExport -> ACTION_VIEW via StartActivityForResult -> work.opened -> viewer result -> discardExport | External app receives plaintext and may keep a copy. Returning from viewer deletes staging file regardless of result code. Event records launch, not external consumption. |
| Cancel save | document result null -> external=false -> cancelledExport -> discardExport | User can choose export again while payload remains unlocked/policy permits. |
| Finish/back | Back -> finish -> pause/stop/destroy cleanup | No implicit re-opening of Home. Saved exported plaintext is not recalled. |
| Screen off | screenOff.onReceive -> lock -> discardExport -> finish | Auto-lock preference gates this; no secret is put into receiver Intent. |

### RevealGesture, RevealWork and temporary plaintext

RevealGesture.java is constructed by the host with sensors, NerdOverlay, pad/usePad/status references; session and byte[] tokens are its only credential holders. Constructor registers sensor token/motion/telemetry listeners, pad acceptance callback, checkbox clear, Record and Done. begin() clears any prior state, starts an instrumented session, enables pad/Done, and starts sensors unless pad selected. Session onProgress updates count/haptic; onTooShort reports; onFinished stops sensors/overlay, takes tokens, disables Done/pad and enables Unlock. Unavailable sensors switch checkbox, whose callback clears the half-started session; begin again using pad. Accelerometer-only warns. takeTokens() returns ownership and nulls its reference. clear() stops sensors/overlay, closes session, zeros tokens, disables pad/Done/Unlock and resets prompt.

RevealWork.java is created by RevealActivity. It owns one ExecutorService, main Handler, synchronized generation/closed state, original encrypted pixel array, payload, credential arrays, decoded text char[], exported staging object, application Context and RevealLedger. Callback<T> has void success(T)/failure(int). There is no UI-thread pixel/crypto/file loop.

| Method | Inputs/result/caller and work | Failure / ownership |
|---|---|---|
| constructor(Context) | Activity -> queue best-effort orphan cleanup | Cleanup retried before export if necessary. |
| load(Uri,Callback<VaultHeader>) | clearSensitive/reset pixels+ledger; decode original -> pixels/header -> capture encrypted-image identity -> synchronized adopt -> deliver | Before/after generation guards; image Bitmap always recycled. OOM/image/no-vault/record errors mapped separately. |
| unlock(byte[] moves,char[] password,WorkProgress progress,Callback<Boolean>) | synchronized takes arrays; capture source/entry/ticket; queues decrypt | Transfers ownership; caller must no longer use arrays. Boolean success indicates event record, not secret bytes. |
| decrypt(...) | internal worker -> expiry -> VaultEngine.reveal -> decodeText for text -> expiry -> synchronized adopt -> ledger success -> deliver | Cancellation discards; expired, OOM and general failure differ; failure event best effort. Unadopted result.close and credential Arrays.fill in finally. |
| decodeText(byte[] bytes) | private static -> char[] decoded from UTF-8 | Revealed text array owned by work until transfer. |
| takeText() | synchronized no args -> char[] and nulls field | TextScrambler then owns/display-cleans it. |
| hasPayload(), hasFile() | synchronized no args -> boolean | UI uses to choose available actions. |
| prepareExport(Callback<RevealExport>) | worker clones payload bytes/name under lock -> RevealExport.create -> guard/adopt -> deliver | Temporary clone wiped in finally; unattached temp file deleted on failure/cancel. |
| save(Uri destination,Callback<Boolean>) | detach exported under lock; worker copies to destination then records export | Temp deleted in finally; successful save may return false event warning. Destination bytes remain if event recording fails. |
| opened(RevealExport source,Callback<Boolean>) | worker records open event | Only failure needs visible notice; viewer result drives temp disposal. |
| discardExport() | detach object then queue delete | Never delete an externally chosen destination. |
| protection(callback) | worker reads protection setting -> main callback | Android flag change runs on main. |
| autoLock() | queues metadata event through ledger | Does not store plaintext. |
| clearSensitive() | synchronized generation++, zero tokens/phrase, discardResult | Wipes payload/text; encrypted image pixels remain retry input. |
| discardResult(int ticket) | synchronized helper -> close/null payload, wipe/null text for applicable ticket | Prevents outdated cleanup removing newer owned data. |
| requireCurrent(int ticket) | synchronized void or CancellationException | Guard closed or changed generation. |
| deliver(int ticket,Runnable action) | main.post, synchronized current/alive guard -> action.run | Obsolete success/error/progress cannot reopen a locked screen. |
| close() | clearSensitive/discardExport, mark closed, pixels=null, worker.shutdown | Queued owners drain; private temp file deletion is best effort, not secure overwrite. |

RevealLedger.java is a small Context-backed metadata coordinator created by RevealWork. capture(Uri source,Bitmap image,VaultHeader header) identifies/records the encrypted image through VaultLogger; synchronized adopt(VaultEntry) and current() transfer/return current metadata. revealed(VaultEntry entry,boolean success,int type,long bytes) returns true after VaultLogger.logReveal or false on RuntimeException; exported(VaultEntry entry) similarly logs an export and returns Boolean success. autoLock(ExecutorService worker) snapshots current entry and queues logAutoLock, ignoring event failures so wiping is not delayed. Opening an external viewer is recorded through exported, not a separate open-event method. Failures are conveyed as Boolean warning or safe record error. There is no secret-bearing database column added by these screens.

RevealExpiry.requireAllowed(Context context,VaultEntry entry) returns void or IOException/Expired. It opens repository metadata, requires the matching record to exist, and rejects a stored expiry when currentTimeMillis >= expiry. It is invoked twice around decryption. This is a reversible local policy; it cannot revoke other copies of the image or enforce a server deadline.

RevealExport.java is immutable staging metadata: File file, Uri uri, String name/mime and VaultEntry vault. create(Context,byte[] bytes,String name,VaultEntry vault) returns the staging object after orphan cleanup, private cache/reveal/randomUUID file write and FileProvider Uri construction; MIME uses lowercase extension/registered type or application/octet-stream. The backing filename is random while provider display name is the payload's basename. save(Context context,Uri destination) streams to chosen Uri with an 8192-byte buffer and wipes that buffer. RevealWork deletes its file field directly; there is no delete() method; cleanupOrphans(Context) synchronizes a process-once removal of previous staging files, throwing on incomplete cleanup so later export retries it. Private export is still plaintext on disk until deleted: cache deletion is not cryptographic secure deletion, and a receiving app may retain it.

### Visual classes and callbacks used by these screens

These classes do not read secrets from storage. Their caller gives owned/copied display data. onDraw(Canvas) returns void and is invoked by Android; onSizeChanged recalculates bounds where overridden. Animations invalidate to request a redraw, not a crypto operation.

| File/class | Caller, fields and meaningful method contract | Edges/lifetime |
|---|---|---|
| ui/MatrixRainView.java | Hide XML/Activity; Paint, falling columns, progress/running/animator. setRunning(boolean) starts/stops ticks; setProgress(int completed,int total) adjusts visual progress; onDraw paints streams | Stopped on pause/detach; decorative progress is not encryption itself. |
| ui/KineticOrbView.java | Gesture XML/replay; Paint and angles/count/animation state. setAngles(float pitch,float roll) updates tilt; setTokenCount(int count,boolean haptic) updates count; setAnimating(boolean) starts/stops animator; reset() clears state; drawDial/drawOrb are onDraw helpers | GestureStep resets/stops on pause. Java replay uses same view without XML ID. |
| ui/NerdOverlay.java | GestureStep/RevealGesture create it over a TextView; telemetry fields temporary. begin(boolean pad), token(byte), sensor telemetry callbacks update diagnostic summary; clear() wipes/reset display | Preference gates visibility; never persist raw motion/token telemetry. |
| ui/DiffMapData.java | HideForge/Ghost worker; owns map Bitmap, source dimensions and changed-channel counts; build from original/stego pixels; close() recycles map | Data is low-bit change information, not extracted secret bytes. |
| ui/DiffStats.java | ResultStep/Ghost screen; static show(TextView view,ImageStats stats) returns void and formats PSNR, changed percentage and channel difference values computed by ImageUtils.statistics | Counts/PSNR describe distortion, not secrecy. |
| ui/DiffMapView.java | XML and Result/Ghost; map reference, Paint, image bounds, loupe TextView, sweep ValueAnimator. setData binds; bindLoupe attaches report; startSweep/stopSweep animate; onDraw draws map/cursor; onTouchEvent reports pixel/LSBs | DOWN outside image ignored; move clamped; UP performs click; accessibility click selects centre; detach/hidden stops animation. Caller detaches data before recycling. |
| ui/WipeComparisonView.java | WipeSliderView; before/after references, Paint, fraction. image setter binds; setFraction controls clipping; onDraw draws sides and split | Only preview bitmaps, no changed carrier bytes. |
| ui/WipeSliderView.java | XML custom container constructs comparison and SeekBar (no declared IDs). Listener progress -> fraction; start tracking empty; stop -> haptic | max 100, initial 50 means equal halves. Clear comparison before release. |
| ui/XRayView.java | Result/Scanner XML; plane/map reference and drawing Paint; setter binds, onDraw scales low-bit view | Scanner only shows absent-header xray. Detach before recycling. |
| ui/VaultDoorView.java | Reveal XML; Paint and animator state. open(Runnable completion) animates 700ms then callback; deny() animates 650ms; reset() cancels/clears | Animation listener cancellation guard prevents old completion revealing controls after lock. |
| ui/TextScrambler.java | Reveal constructs around plaintext TextView; main Handler, char[] and generation/tick state. show(char[]) animates up to 4096 chars for 18 ticks at 35ms, then shows full text; clear() removes callbacks, wipes array and clears view | Avoids cutting a surrogate pair (two UTF-16 chars for one symbol); full displayed text remains until lock, even after char[] wiped. |
| ui/UiPolish.java | App activity lifecycle install/pause plus screen feedback calls. install(Activity) tags once, recursively adds Button touch-up haptics while returning false, enters non-Home/non-Splash content; notice(View) polite live announcement/fade; failure(View) long-press haptic; pause(Activity) clears content animation | Touch listener preserves normal click processing. No secrets retained. |

## Screen-facing algorithm explanations

Capacity is the number of ciphertext bytes fitting after the public header: floor((N - HEADER_PIXELS)*3/8), returning zero when N<HEADER_PIXELS. The 38-byte header consumes 304 bits within 102 reserved pixels (306 channel bits); two spare header bits are not reused. For a 100x100 cover: floor((10000-102)*3/8)=3711 bytes. SecretStep measures packed payload -> GZIP envelope -> plus 16-byte GCM tag and compares with this capacity. Example zipped length 1000 gives needed 1016, so it fits; it does not simply compare source file size. All relevant constants originate in core/Constants.java, not the progress bar's display scale.

Embedding writes one bit into each RGB channel low bit; alpha is preserved. Worked byte 0xA5 = 10100101: source channels [100,100,100,100,100,100,100,100] become [101,100,101,100,100,101,100,101]. This spans two full RGB pixels plus the first two channels of a third; each changed channel differs by 1. The code's equivalent operation is channel&~1 combined with the next bit. Standard mode advances through channels after the header; Ghost uses the real shuffled channel order for body positions. Reading channels' &1 in the same order reconstructs 10100101. Header remains sequential so a scanner can find version/flags/salt/IV/length before knowing a key. Full offsets/shuffle walkthrough are in D2.

Credential flow: tokens are the bytes LEFT=0, RIGHT=1, FORWARD=2, BACK=3, SNAP=4, FLAT=5, not floating-point sensor traces. Enrollment [0,1,4,5] matches another [0,1,4,5]; [0,1,5,4] fails. Each session permits 4..8 moves, timing/angle gates live in Constants; recording does not compare approximate pictures of motion. GestureCredential builds key material from tokens plus optional phrase; CryptoCore derives AES key with PBKDF2 and a random salt, uses fresh IV, authenticates real header AAD and appends the GCM tag. Numeric facts: salt 16 bytes, IV 12, tag 16, key 256 bits, PBKDF2 iterations 120,000, fixed for format version 1. Public salt/IV in header are required for repeatable derivation/decryption; they are not secrets. Same password/tokens with different salt produce different derived keys. Never reuse or serialize a UI replay as a key store.

PSNR follows mean squared RGB error: changing one channel by 1 among 3 channels of one pixel gives MSE=1/3, then 10*log10(255^2/(1/3)) about 52.90dB. ImageStats compares all channels, unchanged image gives infinity. Visual change fraction counts changed channels versus available channels; it is not payload percentage. Compression attempts can reduce size for repeated text; encrypted/random bytes often cannot compress. GZIP still wraps uncompressed mode, so both reader paths use GzipCodec. PNG preserves these exact bits; a JPEG re-encode can change channel values and destroy authentication/extraction. A PNG filename alone cannot repair previously modified pixels.

## Small real source examples and constructs

Each block is at most ten lines and taken from the cached source read for this step; ellipses are avoided. Explanations above define the terms before their use. Engine-specific javax.crypto/java.security/java.util.zip class-by-class contracts belong to D2; these Activities invoke that engine rather than implementing a second cipher/zip algorithm.

### Interfaces and generics: callbacks specify a typed result and an error resource

Source: app/src/main/java/com/example/kineticvault/ui/hide/HideWork.java

```java
interface Callback<T> { void success(T value); void failure(int message); }
    interface Progress { void update(int label, int done, int total); }
```

### ExecutorService, Handler and Looper: worker queue and main-thread delivery

Source: app/src/main/java/com/example/kineticvault/ui/hide/HideWork.java

```java
private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicInteger secretGeneration = new AtomicInteger();
```

### Lambda callback: completed work posts its result to the UI

Source: app/src/main/java/com/example/kineticvault/ui/hide/HideWork.java

```java
T result = action.call();
                main.post(() -> { if (!closed) callback.success(result); });
```

### Anonymous BroadcastReceiver: screen-off dispatch uses an Intent action

Source: app/src/main/java/com/example/kineticvault/ui/reveal/RevealActivity.java

```java
private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                if (com.example.kineticvault.data.VaultPreferences.current().autoLock) {
                    lock(); work.discardExport(); finish();
                }
            }
        }
    };
```

### Lifecycle override: pause invalidates callbacks and clears calculator state

Source: app/src/main/java/com/example/kineticvault/ui/CalculatorActivity.java

```java
@Override protected void onPause() { generation++; busy = false; reset(); super.onPause(); }
```

### Explicit Intent routes use the same registered click-listener pattern; these listeners finish, generate and replay

Source: app/src/main/java/com/example/kineticvault/ui/GhostComparisonActivity.java

```java
findViewById(R.id.diff_back).setOnClickListener(v -> finish());
        findViewById(R.id.diff_retry).setOnClickListener(v -> generate());
        findViewById(R.id.diff_replay).setOnClickListener(v -> { standard.startSweep(); ghost.startSweep(); });
```

### Explicit Intent: successful disguise gate opens Home and removes calculator

Source: app/src/main/java/com/example/kineticvault/ui/CalculatorActivity.java

```java
startActivity(new Intent(this, HomeActivity.class)); finish();
```

### Activity Result API: registered document contract returns a Uri

Source: app/src/main/java/com/example/kineticvault/ui/hide/CoverStep.java

```java
new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) load(() -> work.store.load(uri));
        });
        camera = host.registerForActivityResult(new CameraContract(), success -> {
            if (success) load(work.store::finishCapture);
```

### Uri and ContentResolver plus try-with-resources: close both streams even after errors

Source: app/src/main/java/com/example/kineticvault/ui/reveal/RevealExport.java

```java
try (FileInputStream input = new FileInputStream(file);
                OutputStream output = context.getContentResolver().openOutputStream(destination, "wt")) {
            if (output == null) throw new IOException("Export destination unavailable");
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.flush();
        } finally { Arrays.fill(buffer, (byte) 0); }
```

### Bitwise shifts, masks and byte/int promotion: choose RGB low bit and write one value

Source: app/src/main/java/com/example/kineticvault/core/StegoCore.java

```java
int shift = 16 - (bit % 3) * 8;
            int value = (bytes[bit / 8] >>> (7 - bit % 8)) & 1;
            pixels[index] = (pixels[index] & ~(1 << shift)) | (value << shift);
```

### Conversions: random int becomes byte token or char letter

Source: app/src/main/java/com/example/kineticvault/ui/GhostComparisonWork.java

```java
for (int i = 0; i < tokens.length; i++) tokens[i] = (byte) random.nextInt(6);
            for (int i = 0; i < phrase.length; i++) phrase[i] = (char) ('a' + random.nextInt(26));
```

### Static nested class, AutoCloseable and ownership: close both maps

Source: app/src/main/java/com/example/kineticvault/ui/GhostComparisonWork.java

```java
static final class Pair implements AutoCloseable {
        final DiffMapData standard, ghost;
        Pair(DiffMapData standard, DiffMapData ghost) { this.standard = standard; this.ghost = ghost; }
        @Override public void close() { standard.close(); ghost.close(); }
    }
```

### Volatile field: publish closure to callbacks; busy gates UI work

Source: app/src/main/java/com/example/kineticvault/ui/GhostComparisonActivity.java

```java
private volatile boolean closed;
    private boolean busy;
```

### Synchronized state and cancellation: ignore stale work safely

Source: app/src/main/java/com/example/kineticvault/ui/reveal/RevealWork.java

```java
private synchronized void requireCurrent(int ticket) {
        if (closed || ticket != generation) throw new CancellationException();
    }
```

### AtomicInteger: shared generation increments invalidate sensitive work

Source: app/src/main/java/com/example/kineticvault/ui/hide/HideWork.java

```java
private final AtomicInteger secretGeneration = new AtomicInteger();
    private volatile boolean closed;
```

### Insets, bitwise OR and callback return: combine safe areas then pad the screen

Source: app/src/main/java/com/example/kineticvault/ui/GhostComparisonActivity.java

```java
Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left + padding, safe.top, safe.right + padding, safe.bottom);
            return insets;
```

### ValueAnimator: a changing value drives a custom Canvas view

Source: app/src/main/java/com/example/kineticvault/ui/SplashActivity.java

```java
ValueAnimator.ofFloat(0, 1); animator.setDuration(1000);
        animator.addUpdateListener(value -> logo.setProgress((float) value.getAnimatedValue()));
        if (resumed) animator.start();
        main.postDelayed(delay, 1200);
    }
```

### Canvas and Paint: draw original/encoded previews inside the custom view

Source: app/src/main/java/com/example/kineticvault/ui/WipeComparisonView.java

```java
@Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (before == null || after == null) return;
        float scale = Math.min(getWidth() / (float) before.getWidth(), getHeight() / (float) before.getHeight());
        float w = before.getWidth() * scale, h = before.getHeight() * scale;
        destination.set((getWidth() - w) / 2, (getHeight() - h) / 2,
                (getWidth() + w) / 2, (getHeight() + h) / 2);
        canvas.drawBitmap(before, null, destination, paint);
```

### SQLite Cursor and try-with-resources: rows become metadata objects

Source: app/src/main/java/com/example/kineticvault/data/VaultRepository.java

```java
try (Cursor cursor = helper.getReadableDatabase().query("vaults", null, selection.toString(),
                args.toArray(new String[0]), null, null, "created_at DESC, id DESC")) {
            while (cursor.moveToNext()) rows.add(VaultEntry.read(cursor));
        }
        return rows;
    }
```

### HttpURLConnection: POST, timeouts and JSON request metadata run on the worker

Source: app/src/main/java/com/example/kineticvault/data/SyncClient.java

```java
HttpURLConnection c = (HttpURLConnection) url.openConnection();
        try {
            c.setConnectTimeout(5000); c.setReadTimeout(5000); c.setInstanceFollowRedirects(false);
            c.setRequestMethod("POST"); c.setDoOutput(true); c.setUseCaches(false);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setRequestProperty("X-API-KEY", key);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(bytes.length);
```

### JSON: parse structured reply and require its ok flag

Source: app/src/main/java/com/example/kineticvault/data/SyncClient.java

```java
JSONObject result = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        if (!result.optBoolean("ok", false)) throw new IOException();
        return result;
```

### Crypto, hash and compression classes reached by screen workers

These imported engine/data classes have one job each. They are not additional Activities. GeneralSecurityException is the checked family for cryptographic-provider failures; AEADBadTagException is the authentication failure discussed in CryptoCore's comment and delivered by Cipher.doFinal. DestroyFailedException (javax.security.auth) means a provider cannot destroy an internal key copy, caught by CryptoCore.destroy; callers cannot promise that every provider memory copy is erased.

| Class | What it means and actual use |
|---|---|
| java.security.SecureRandom | Cryptographic random generator; CryptoCore.encrypt fills salt/IV, GhostComparisonWork fills public-demo credentials. |
| java.security.MessageDigest | Hash accumulator; data/diagnostics computes SHA-256 image/file identities, not an AES key. |
| java.security.GeneralSecurityException | Parent exception propagated from CryptoCore and handled by Harness/worker; does not mean plaintext was authenticated. |
| javax.crypto.Cipher | Configured AES/GCM/NoPadding; init, updateAAD(header.bytes), doFinal encrypt/authenticate or verify/decrypt. |
| javax.crypto.SecretKey | Interface for provider-derived key; getEncoded yields bytes, destroy attempts cleanup. |
| javax.crypto.SecretKeyFactory | PBKDF2WithHmacSHA256 provider; generateSecret(PBEKeySpec) derives key. |
| javax.crypto.spec.PBEKeySpec | Password/salt/iteration/key-size parameters; clearPassword called in finally. |
| javax.crypto.spec.SecretKeySpec | Wraps derived bytes as AES key; destroy attempted when transform ends. |
| javax.crypto.spec.GCMParameterSpec | 128-bit authentication tag and header IV supplied to Cipher.init. |
| java.util.zip.GZIPOutputStream | Writes GZIP envelope; ConfiguredGzip subclass sets protected def's compression level. |
| java.util.zip.Deflater | DEFAULT_COMPRESSION or NO_COMPRESSION determines inner compression, not envelope presence. |
| java.util.zip.GZIPInputStream | Validates/expands envelope in 8192-byte chunks; size cap rejects expansion beyond MAX_PACKED_BYTES. |

### AES-GCM: key wrapper, IV/tag parameters, header AAD and authenticated transform

Source: app/src/main/java/com/example/kineticvault/core/CryptoCore.java

```java
key = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, key, new GCMParameterSpec(128, header.iv()));
            // Authenticate public metadata, including the mode and the length.
            cipher.updateAAD(header.bytes());
            WorkProgress.Stage stage = mode == Cipher.ENCRYPT_MODE
                    ? WorkProgress.Stage.ENCRYPT : WorkProgress.Stage.DECRYPT;
            WorkProgress.report(progress, stage, 0, 0);
```

### PBKDF2 specification and factory: 120000 iterations and 256-bit key

Source: app/src/main/java/com/example/kineticvault/core/CryptoCore.java

```java
specification = new PBEKeySpec(password, salt,
                    Constants.PBKDF2_ITERATIONS, Constants.KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            generated = factory.generateSecret(specification);
            return generated.getEncoded();
```

### Inheritance and zip classes: configured GZIP still wraps uncompressed mode

Source: app/src/main/java/com/example/kineticvault/core/GzipCodec.java

```java
private static final class ConfiguredGzip extends GZIPOutputStream {
        ConfiguredGzip(ByteArrayOutputStream output, boolean compress) throws IOException {
            super(output);
            def.setLevel(compress ? Deflater.DEFAULT_COMPRESSION : Deflater.NO_COMPRESSION);
        }
    }
```

A method reference such as this::executeTest passes the existing instance method as a Runnable; a Runnable returns no value. Future<?> is the cancellable task handle with an unspecified result type; it is cancelled with interruption at Lab destruction. Consumer<T> accepts a typed value and returns void; Callable<T> can throw and returns a typed value. Arrays.fill overwrites an owned mutable array; it does not wipe copies held by Android widgets, providers, immutable Strings or a remote app. instanceof checks a view's actual type before a cast. @Override asks Java to enforce a parent/interface method signature; extending a class inherits behavior while implementing an interface supplies its contract. Static helpers do not require an instance. Enum values such as WorkProgress.Stage.KEY identify named stages rather than magic numbers. Charset ByteBuffer/CharBuffer wrap typed byte/character storage; UTF-8 encoding/decoding is different from casting a byte to char. Live-region/accessibility settings make notices/controls readable without changing encryption. All these examples are existing source behavior.

## Additional ownership and helper walkthroughs

DiffMapData.build(int[] before,int[] after,int width,int height) transfers both arrays, validates positive dimensions/lengths, scales display map to at most512, and visits every source pixel. It XORs before/after; any changed red/green/blue channel ORs that colour into the corresponding preview bin, preserving changes even when several source pixels share a bin. It builds diff Bitmap, then buildXRay and ImageUtils.statistics, returning DiffMapData; partial bitmaps are recycled on failure. buildXRay(int[] pixels,int width,int height) returns Bitmap and samples each channel's bit0 to 0 or255; e.g RGB(100,101,100) -> RGB(0,255,0). original(int x,int y) and encoded(int x,int y) return retained exact source pixel for loupe; caller supplies clamped coordinates. close() recycles diff/xray and zero-fills both arrays. The arrays are images, not decrypted payload bytes; still they are explicitly cleared once their visual owner closes.

DiffStats.show(TextView view,ImageStats stats) only formats already-computed statistics. WipeComparisonView.setImages(Bitmap original,Bitmap encoded) binds previews, setFraction(float value) bounds the divider; onDraw clips/draws two sides and split marker. WipeSliderView.setImages(Bitmap before,Bitmap after) forwards binding. XRayView.setPlane(Bitmap value) stores the plane reference and invalidates; it does not own/recycle it. All XML custom-view constructors take Context/AttributeSet (Android's XML attribute bundle); single-Context constructors delegate to them. Paint fields control colour/stroke/typeface. Animator update listeners vary state and invalidate. MatrixRainView.onDetachedFromWindow and KineticOrbView.onDetachedFromWindow stop running animations, avoiding detached drawing callbacks. KineticOrbView.drawDial(Canvas,float,float,float) and drawOrb(Canvas,float,float,float) are called only by onDraw to render dial/orb geometry. None changes tokens or crypto credentials.

NerdOverlay.onSample(float ax,float ay,float az,float gx,float gy,float gz,float pitch,float roll,Mode mode) returns void: sensor telemetry callback, gated by preference and at least120ms between rendered samples, formats acceleration/gyro/angles/alpha and render(). token(byte) rejects invalid tokens, retains at most8 and shifts when full, then render(); render() translates LABELS and sample into TextView. clear() wipes its eight-byte array, counters/sample and displays idle according to preference. begin(boolean pad) clears then displays pad mode. These transient Strings may exist until garbage collection; they are not written to SQLite/SharedPreferences/logs. TextScrambler.advance() is the Handler Runnable target described earlier; when tick reaches18 it displays whole secret and wipes both owned arrays. clear() must also clear TextView because wiping those arrays alone does not retract text copied into a widget.

Gesture strength measures an optimistic upper bound from six token choices: n*log2(6). Four tokens give4*2.5849625=10.34 bits; eight give20.68. It is not a claim of 256 bits of human entropy just because AES key size is256. StrengthMeterView displays current count/optional phrase advice; GestureStep progress updates it. The actual sensor pipeline/session thresholds, complementary filter and strength label selection are explained in D3, not reimplemented by HideActivity. Normal entry25deg/exit12deg gives hysteresis: a left tilt crossing25 emits LEFT; it must return inside12 before another tilt, plus300ms gap. These control reliable recognition, not PBKDF2 iteration count.

The export/open/save callbacks always return to main before changing Views. Hide uses one serial queue plus a separate text importer; Reveal has one serial queue. Disk writes run off UI even when initiated by a button. Dialog confirmations, picker results, sensor session notifications and animation completions are callbacks, not blocking waits. Only selected image Uri and nonsecret workflow configuration can survive recreation; credential/payload UI is deliberately restarted.

## Likely viva questions

1. **Are the five Hide steps five Activities?** No. HideActivity switches included panels with a ViewFlipper and delegates to controllers.
2. **How does the UI stay responsive during encryption?** HideWork/RevealWork serialize expensive work on an ExecutorService and post guarded callbacks through a main Handler.
3. **Why must Reveal use the original image?** Resize, rotation or lossy recompression can change low bits. decodeExact reads original pixels; previews are display-only.
4. **Where does revealed plaintext exist?** Owned in-memory Payload/char arrays and TextView; deliberate export creates a short-lived private plaintext file, and the chosen external destination can persist it.
5. **Does auto-lock revoke exported content or force a remote app to forget it?** No. It wipes this screen's owned sensitive state, cancels stale UI delivery and deletes its staging file; external copies remain outside that control.

## Covers these files

- app/src/main/java/com/example/kineticvault/ui/hide/HideActivity.java
- app/src/main/java/com/example/kineticvault/ui/hide/CoverStep.java
- app/src/main/java/com/example/kineticvault/ui/hide/SecretStep.java
- app/src/main/java/com/example/kineticvault/ui/hide/SecretInput.java
- app/src/main/java/com/example/kineticvault/ui/hide/TextImport.java
- app/src/main/java/com/example/kineticvault/ui/hide/GestureStep.java
- app/src/main/java/com/example/kineticvault/ui/hide/GestureReplay.java
- app/src/main/java/com/example/kineticvault/ui/hide/HideWork.java
- app/src/main/java/com/example/kineticvault/ui/hide/HideStore.java
- app/src/main/java/com/example/kineticvault/ui/hide/HideForge.java
- app/src/main/java/com/example/kineticvault/ui/hide/ResultStep.java
- app/src/main/java/com/example/kineticvault/ui/hide/HideExporter.java
- app/src/main/java/com/example/kineticvault/ui/hide/ProofSheet.java
- app/src/main/java/com/example/kineticvault/ui/reveal/RevealActivity.java
- app/src/main/java/com/example/kineticvault/ui/reveal/RevealGesture.java
- app/src/main/java/com/example/kineticvault/ui/reveal/RevealWork.java
- app/src/main/java/com/example/kineticvault/ui/reveal/RevealLedger.java
- app/src/main/java/com/example/kineticvault/ui/reveal/RevealExpiry.java
- app/src/main/java/com/example/kineticvault/ui/reveal/RevealExport.java
- app/src/main/java/com/example/kineticvault/ui/ExportSheet.java
- app/src/main/java/com/example/kineticvault/ui/UiPolish.java
- app/src/main/java/com/example/kineticvault/ui/MatrixRainView.java
- app/src/main/java/com/example/kineticvault/ui/KineticOrbView.java
- app/src/main/java/com/example/kineticvault/ui/NerdOverlay.java
- app/src/main/java/com/example/kineticvault/ui/DiffMapData.java
- app/src/main/java/com/example/kineticvault/ui/DiffMapView.java
- app/src/main/java/com/example/kineticvault/ui/DiffStats.java
- app/src/main/java/com/example/kineticvault/ui/WipeComparisonView.java
- app/src/main/java/com/example/kineticvault/ui/WipeSliderView.java
- app/src/main/java/com/example/kineticvault/ui/XRayView.java
- app/src/main/java/com/example/kineticvault/ui/VaultDoorView.java
- app/src/main/java/com/example/kineticvault/ui/TextScrambler.java
- app/src/main/java/com/example/kineticvault/core/Constants.java
- app/src/main/java/com/example/kineticvault/core/VaultEngine.java
- app/src/main/java/com/example/kineticvault/core/CryptoCore.java
- app/src/main/java/com/example/kineticvault/core/GestureCredential.java
- app/src/main/java/com/example/kineticvault/core/GzipCodec.java
- app/src/main/java/com/example/kineticvault/core/StegoCore.java
- app/src/main/java/com/example/kineticvault/image/ImageUtils.java
- app/src/main/java/com/example/kineticvault/image/ImageStats.java
- app/src/main/java/com/example/kineticvault/image/CoverGenerator.java
- app/src/main/java/com/example/kineticvault/data/VaultPreferences.java
- app/src/main/java/com/example/kineticvault/data/VaultRepository.java
- app/src/main/java/com/example/kineticvault/data/VaultDatabaseHelper.java
- app/src/main/java/com/example/kineticvault/data/VaultLogger.java
- app/src/main/java/com/example/kineticvault/data/SyncClient.java
- app/src/main/AndroidManifest.xml
- app/src/main/res/layout/activity_hide.xml
- app/src/main/res/layout/hide_step_cover.xml
- app/src/main/res/layout/hide_step_secret.xml
- app/src/main/res/layout/hide_step_gesture.xml
- app/src/main/res/layout/hide_step_forge.xml
- app/src/main/res/layout/hide_step_result.xml
- app/src/main/res/layout/activity_reveal.xml
- app/src/main/res/layout/sheet_export.xml
- app/src/main/res/values/strings.xml
- app/src/main/res/values/vault_strings.xml
