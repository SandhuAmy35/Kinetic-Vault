# Other screens, navigation and shared Activity behavior

## How to read the screen contracts

An **Activity** is an Android screen; a **View** is a control/drawing component. **XML** is the markup defining view trees. A **layout** is such a tree,installed by setContentView/inflation; a **view ID** is its generated R.id name used with findViewById. A **listener** is a registered callback for a tap/change/event; a **callback** is code invoked later or inline by its owner. **Lifecycle methods** are Android's created/started/resumed/paused/stopped/destroyed notifications. A **Bundle** stores selected screen state for recreation;it is not a place for secrets here. **State** includes flags,selected addresses,temporary arrays,bitmaps and dialog references. **Ownership** identifies who must wipe/release it.

**UI** means user interface. The **main/UI thread** handles controls/drawing. A **worker thread** runs expensive operations. **ExecutorService** queues work;Executors.newSingleThreadExecutor serializes it. **Handler** posts operations to a thread's message queue;**Looper** services that queue;Looper.getMainLooper targets UI. A **generation/ticket** counter discards obsolete asynchronous results. **Synchronous** calls finish before returning;**asynchronous** work later returns through a callback. A lambda (arrow syntax) is a short function;::passes an existing method. **Interfaces** specify required methods;**generics**,such as Callback<T>/List<VaultEntry>,attach result/element types. **Anonymous classes** supply inline implementations;**inner classes** can access an outer instance;**static nested classes** group data without capturing it. @Override marks a parent/interface method;super invokes parent behavior;void means no return value. The tables use parameters' actual source names;callback objects provide their own result rather than a direct method return.

An **Intent** requests an Android operation. **Explicit** intents name an Activity;**implicit** intents ask Android to find a compatible handler,for example SEND/VIEW. A **Uri** identifies provider content,often not a filesystem path. **ContentResolver** opens original streams;**FileProvider** exposes allowed private files through content URIs with temporary grants. **ClipData** also carries grantable references. **MIME** is content type,such as image/png. The **Activity Result API** registers a typed contract/callback before launching a picker/camera/viewer. **exported** means other apps may enter a declared component;**singleTop** routes a new request to an already-top Activity's onNewIntent. **finish()** removes this Activity from the back stack (screen history);it does not guarantee a new Home screen is launched.

A **BroadcastReceiver** handles Android event messages selected by IntentFilter. **Insets** are safe edge padding for status/navigation bars,cutouts and keyboard (IME). **Edge-to-edge** lets content extend under system bars,requiring such padding. A **Snackbar** is transient on-screen feedback;**AlertDialog** is a modal confirmation/input window;**bottom sheet** is a lower-edge action panel. **Haptics** are system touch/vibration feedback;**accessibility** provides actions/descriptions/live announcements for assistive tools. dp means density-independent size;sp also follows text scaling. **DayNight** is a theme family with light/dark palettes. FLAG_SECURE asks Android to protect a window from capture;it does not defeat a camera or compromised device.

A **Bitmap** is an image in memory;a **pixel** is a colour cell;**RGB** are red/green/blue values0..255;**LSB** means each channel's lowest bit. **PNG** losslessly encodes pixels;ordinary JPEG is lossy. **Plaintext** is readable secret content;**ciphertext** is encrypted bytes. **Payload** holds type,basename and content;basename excludes directories. **GZIP** is the always-present compression envelope. **AES-GCM** encrypts and authenticates;**PBKDF2** derives a key from credential and public random **salt**. **IV** is the fresh initialization value;**tag** is authentication result;**AAD** is public header metadata included in authentication. **Ghost** scatters ciphertext positions publicly/reproducibly. **PSNR** (peak signal-to-noise ratio) measures RGB error in decibels(dB),not security. **SHA-256** is a file fingerprint;**UUID** is a random record identifier. A preview/thumbnail is a separate display copy,never extraction input.

**SQLite** is the local relational database;SQLiteOpenHelper manages schema,SQLiteDatabase runs queries/transactions,Cursor iterates rows and ContentValues supplies typed updates. A **transaction** groups committed changes. **SharedPreferences** is named private key/value settings;MySQL is the optional server database. **Metadata** describes operations/images without secret content. **HTTP** is web request transport,HTTPS encrypts it;HttpURLConnection sends requests;**JSON** is named-field structured text. **WebView** embeds web content. A **ListView adapter** supplies row views;LRU cache discards least recently used entries at its size limit. **Soft deletion** sets a hidden/deleted flag while retaining files/history;**expiry** is a local clock-based new-unlock policy. **Canvas** draws;**Paint** styles drawing;**ValueAnimator** varies a numeric value over time and invokes updates. **Exceptions** report failure;try/catch handles it,finally cleans up;try-with-resources automatically closes AutoCloseable resources. synchronized serializes state;volatile publishes fields between threads;AtomicInteger safely increments a shared counter. Java byte/char/int/long are8/16/32/64-bit values;char represents UTF-16 code units,UTF-8 encodes text bytes. Bitwise &,OR,^,~,<<,>>> select/combine/compare/invert/shift bits;casts explicitly convert types. Small real source examples appear at the end of each document.

All Java paths are relative to `app/src/main/java/com/example/kineticvault/`; all layout names expand to `app/src/main/res/layout/<name>.xml`. Lifecycle rows describe overrides actually present;other lifecycle methods retain inherited behavior. View tables enumerate all explicit android:id declarations,including included layouts and shared sheet/row files. Controls created in Java may have no resource ID and are listed separately. Each action table is a left-to-right ASCII call chain;slashes name alternative branches. No screen behavior is inferred from an old phase's launcher description.

## Complete Activity inventory and entry graph

There are twelve concrete Activities and one abstract Activity base in the real source. An abstract class cannot be launched itself; InfoActivity supplies reusable screen setup to About, Settings, Learn and Scanner. Hide and Reveal are documented in [30_HIDE_AND_REVEAL.md](30_HIDE_AND_REVEAL.md). Every concrete screen is accounted for across these two documents.

~~~text
Android launcher -> SplashActivity -> HomeActivity OR CalculatorActivity
CalculatorActivity -- configured digits then '=' --> HomeActivity
HomeActivity -> HideActivity / RevealActivity / VaultActivity / ScannerActivity
HomeActivity -> SettingsActivity / LearnActivity / AboutActivity / TestHarnessActivity
VaultActivity -> RevealActivity(data=image Uri) / HideActivity(empty state)
ScannerActivity -> RevealActivity(data=original image Uri)
HideActivity result -> GhostComparisonActivity
External SEND(image/*) / VIEW(content,image/*) -> ScannerActivity
~~~

All internal routes above use explicit Intents. Only Splash (launcher) and Scanner (image SEND/VIEW) are exported. Scanner is singleTop, so a request when it is already at the top calls onNewIntent instead of necessarily creating another screen. No other Activity has a custom onNewIntent handler. Private screens cannot be directly invoked by arbitrary apps. The manifest declares INTERNET, not CAMERA; TakePicture delegates capture to a camera app via temporary provider permission. Manifest activities are portrait for Splash, Hide, Reveal and TestHarness; keyboard resize is specified for Splash, Hide, Reveal, TestHarness, Vault and Settings. Other orientations follow normal Android/theme behavior.

## Application and shared screen behavior

VaultApplication.java is created by Android before the first Activity. onCreate() loads VaultPreferences asynchronously, calls SyncManager.configurationChanged after load, forces initial dark mode, and registers an anonymous Application.ActivityLifecycleCallbacks. The integer visible count increments on activityStarted: transition 0->1 calls sync.start. activityStopped decrements: transition 1->0 calls sync.stop. activityResumed calls UiPolish.install(activity); activityPaused calls UiPolish.pause(activity). Created, save-instance-state and destroyed callbacks are intentionally empty. Sync network/database activity uses SyncManager's own worker; these callbacks only coordinate visibility. A quick transition between Activities can cause stop/start scheduling; it is not evidence of a new database or a secret upload.

ui/InfoActivity.java extends AppCompatActivity and is abstract. It has no screen of its own, fields containing secrets, or custom lifecycle override. showPage(int layout) returns void: subclasses call it from onCreate, it enables edge-to-edge, installs layout, adds a WindowInsets listener for bars/cutout/keyboard with 24dp content spacing, requests insets, and binds info_back to finish(). Its shared info_root/info_back IDs appear separately in each subclass's XML. Back returns to whichever screen opened it.

UiPolish.java has no secret store. install(Activity) runs once per content via the polish_installed tag, recursively attaches Button touch listeners and starts entry animation except Home/Splash. Its touch callback haptics on ACTION_UP and returns false, allowing the actual click listener to continue. notice(View) provides polite live-region feedback/fade, failure(View) gives failure haptic, pause(Activity) clears content animation. These are additional callbacks alongside each screen's listeners, not replacements for user actions. ExportSheet.show(host,note,labels,actions) constructs a secure-following BottomSheetDialog with 56dp Java Buttons, per-action haptic/dismiss/alive check and Cancel dismiss. It has no persistent state; callers dismiss on pause. Shared layout IDs are listed later.

## SplashActivity: real launcher

**Purpose/caller/fields:** Android MAIN/LAUNCHER starts ui/SplashActivity.java. It waits for preferences and a brief logo before routing. State is loaded, elapsed, resumed and routed flags, main Handler/delayed route, logo and animator references. There is no activity_splash.xml: a blank View, then SplashLogoView are created in Java, with no declared view IDs.

| Method/callback | Inputs, result and calls | Lifecycle/error/state |
|---|---|---|
| onCreate(Bundle state) | Android -> edge-to-edge/background -> VaultPreferences.load(callback) | Callback ignores dead Activity, marks loaded; disguise sets elapsed then route, normal mode showLogo. No secret Bundle state. |
| showLogo() | no args, void -> Java SplashLogoView, ValueAnimator 0..1, duration 1000ms; update listener logo.setProgress; post 1200ms delay | Only starts animation while resumed; delay marks elapsed and route. |
| route() | no args, void -> explicit Intent Calculator or Home, then finish | Requires loaded&&elapsed&&resumed&&!routed and alive; prevents duplicate launch. |
| onResume() | Android -> resumed=true, restart unfinished animation, route | Defers launches until foreground. |
| onPause() | Android -> resumed=false, animator.cancel | Timer may finish but route waits foreground. |
| onDestroy() | Android -> remove delayed callback, cancel animator | No key/gesture/plaintext to wipe. |

User chain: launch icon -> SplashActivity.onCreate -> preferences callback -> showLogo/delay -> route -> HomeActivity or CalculatorActivity. Preferences are private SharedPreferences loaded by a worker, with cached Values returned to main. The screen offers no explicit preference-load retry/error control: document the existing behavior, not an imagined recovery dialog. SplashLogoView.java holds drawing Paint/progress; onDraw draws the vault logo, setProgress(float) changes animation amount and invalidates. Animator callback is UI work, not encryption.

## HomeActivity: menu and sync summary

**Purpose/caller/fields:** Splash or successful calculator unlock creates ui/HomeActivity.java. It provides navigation and observes SyncManager status. It keeps a pulse ValueAnimator and a status-listener method reference, not plaintext or credential state.


### View IDs — app/src/main/res/layout/activity_home.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `home_root` | ScrollView | Home scroll root with system/cutout insets. | Default visible/enabled unless parent or controller changes it. |
| `home_more` | MaterialButton | Runtime PopupMenu for Settings,Learn,About. | Default visible/enabled unless parent or controller changes it. |
| `home_orb` | KineticOrbView | Decorative Home hero,not a recording surface. | Default visible/enabled unless parent or controller changes it. |
| `cloud_status` | Chip | Read-only Offline/Pending/Synced chip. | Default visible/enabled unless parent or controller changes it. |
| `cloud_airplane` | TextView | Airplane-mode pause banner,hidden otherwise. | visibility=gone |
| `cloud_stats` | TextView | Live or last-received server totals,or unavailable guidance. | Default visible/enabled unless parent or controller changes it. |
| `cloud_queue` | TextView | Count of metadata updates queued. | Default visible/enabled unless parent or controller changes it. |
| `cloud_retry` | MaterialButton | Reset sync backoff and request foreground retry;disabled in airplane mode. | Default visible/enabled unless parent or controller changes it. |
| `home_hide` | MaterialButton | Explicit navigation to HideActivity. | Default visible/enabled unless parent or controller changes it. |
| `home_reveal` | FrameLayout | Explicit navigation to RevealActivity. | Default visible/enabled unless parent or controller changes it. |
| `home_lab` | MaterialButton | Explicit navigation to TestHarnessActivity. | Default visible/enabled unless parent or controller changes it. |
| `home_scan` | FrameLayout | Explicit navigation to ScannerActivity. | Default visible/enabled unless parent or controller changes it. |
| `home_history` | FrameLayout | Explicit navigation to VaultActivity. | Default visible/enabled unless parent or controller changes it. |
| `home_settings` | FrameLayout | Explicit navigation to SettingsActivity. | Default visible/enabled unless parent or controller changes it. |
| `home_learn` | MaterialButton | Explicit navigation to LearnActivity. | Default visible/enabled unless parent or controller changes it. |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

| Method | Caller, parameters/result and work | State/failure |
|---|---|---|
| onCreate(Bundle state) | Android -> layout, edge-to-edge, 24dp horizontal safe insets, navigation listeners, cloud retry, tile animation | Tiles translate 32 pixels/fade over 350ms with 75ms staggering. No incoming secret extras parsed. |
| overflow(View anchor) | More -> PopupMenu; item-selected callback | Menu IDs 1 Settings, 2 Learn, 3 About; selection starts explicit Activity and returns handled. |
| showLedger(SyncManager.Status status) | registered listener -> cloud text/stats/queued count/airplane visibility/retry enabled | Offline takes precedence; otherwise pending>0 -> queued, else synced. Uses cached stats while offline/pending; retry disabled in airplane mode. |
| onStart()/onStop() | Android -> add/remove status listener | Immediate current status delivered on add; no dangling observer while stopped. |
| onResume()/onPause() | Android -> start/cancel pulse | Scale .94..1.04, 1600ms, reverse forever while resumed. No database work in pulse. |

| User action | Full call chain | Store/failure/security |
|---|---|---|
| Hide | Home tile click -> startActivity(new Intent(...HideActivity.class)) | No secret extra. |
| Reveal | Home tile click -> explicit RevealActivity | User selects image there. |
| Vault | Home tile click -> explicit VaultActivity | Local metadata queried on that screen. |
| Scan | Home tile click -> explicit ScannerActivity | Original-image header inspection. |
| Settings | Home tile / More item1 -> explicit SettingsActivity | Persistent settings edited explicitly. |
| Learn | Home tile / More item2 -> explicit LearnActivity | Local/remote education only. |
| About | More item3 -> explicit AboutActivity | Static team/workstream text. |
| Lab | Lab tile -> explicit TestHarnessActivity | Does not run until Lab's Run action. |
| Retry sync | cloud retry -> SyncManager.configurationChanged -> worker retry/status notifications -> showLedger | Local metadata remains when network fails; offline/queued feedback. No plaintext, gesture, phrase or key is synchronized. |

SyncManager uses its own executor and Handler to publish status. It checks airplane-mode state and has a runtime airplane-mode receiver; this is not a Home manifest receiver. Server storage is optional MySQL reached through the PHP/HTTP endpoint in Settings. The synchronized records/events are metadata; SQLite is the local ledger. Home itself does not write these values or maintain a separate list database. No onSaveInstanceState override is needed for secrets because this screen has none; pulse is recreated.

## CalculatorActivity: disguise and arithmetic

**Purpose/callers/fields:** Splash routes here only if disguise enabled. ui/CalculatorActivity.java implements an immediate-execution calculator and configured-code gate. Fields: mutable StringBuilder input, TextView display, BigDecimal accumulator, String operation, fresh/busy flags and generation. BigDecimal is decimal arithmetic rather than binary float; MathContext.DECIMAL64 sets precision/rounding. Disguise code is unrelated to the motion encryption key.


### View IDs — app/src/main/res/layout/activity_calculator.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `calc_root` | ScrollView | Calculator scroll root;secret input saving/autofill excluded. | saveEnabled=false |
| `calc_display` | TextView | Current numeric input/result/error;not saved/frozen. | saveEnabled=false; freezesText=false |
| `calc_keys` | LinearLayout | Container recursively traversed for buttons carrying action tags. | Default visible/enabled unless parent or controller changes it. |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

Buttons have no individual resource IDs. bind(ViewGroup group) recursively visits children; each non-group with tag gets one click listener -> press(tag). The actual tags are C, back, /, *, 7, 8, 9, -, 4, 5, 6, +, 1, 2, 3, =, 0 and .; the tag determines function, not label lookup. Buttons are 60dp high.

| Method | Parameters/result/caller and calls | Edges/failure |
|---|---|---|
| onCreate(Bundle state) | Android -> secure flag, edge/layout, root safe padding, bind(keys), reset | No input Bundle restoration; insets include bars/cutout. |
| bind(ViewGroup group) | void; recurse groups; tagged leaf click -> press(String) | Same handler for all number/operator buttons. |
| press(String key) | void; busy guard; C reset, = equalsPressed, operator operator, back delete/render; digits/decimal append/render | 16-character cap, duplicate decimal ignored, leading decimal gets 0. Fresh input wiped first. |
| equalsPressed() | no args, void -> normal calculate OR copy code char[] -> VaultPreferences.unlockDisguise callback | Requires enabled disguise, standalone fresh=false 6..12 digits, no decimal/pending operation. busy blocks keys. Callback only current generation/alive; match reset/haptic/explicit Home/finish, mismatch calculate. |
| operator(String next) | void -> number/apply/showNumber, set pending operator/fresh | Arithmetic/format errors -> fail. Sequential calculations, not algebraic precedence. |
| calculate() | no args, void -> number, apply if pending+nonfresh, showNumber; clear operation/accumulator | Divide by zero/malformed -> fail. |
| apply(BigDecimal left,BigDecimal right) | returns BigDecimal add/subtract/multiply/default divide using DECIMAL64 | Throws arithmetic errors to caller. |
| number() | returns zero for empty input else parsed BigDecimal | String conversion is for calculator values; disguise array owned/wiped by preference worker. |
| showNumber(BigDecimal value) | void -> wipeInput, append normalized decimal, render | Removes trailing zeros, avoids scientific display. |
| render() | void -> display input or resource zero | No persistence. |
| wipeInput(), reset() | void -> overwrite builder chars then clear; reset also clears accumulator/operator and fresh flag | Cannot guarantee erasure of immutable library/internal arithmetic copies. |
| fail() | void -> reset, error resource, long-press haptic | User can enter new calculation. |
| onPause() | Android -> generation++, busy=false, reset, super | Invalidates pending unlock result and clears digits. No other lifecycle override. |

Worked arithmetic: 2 + 3 * 4 = computes (2+3)*4 =20 because operator() applies the previous pending operation immediately. Worked gate: entering a configured six-digit code then '=' with no pending operation -> unlockDisguise -> DisguiseCode.matches on preference worker -> match callback opens Home. It never passes those digits to VaultEngine. No incoming Intent data parsed, no external implicit Intent sent. Code hash/salt live in SharedPreferences; UI/temporary copies are cleared and code char[] wiped by preferences work. FLAG_SECURE is always applied.

## SettingsActivity: explicit preference save and server test

**Purpose/caller/fields:** Home creates ui/SettingsActivity.java, a subclass of InfoActivity. It keeps sensitivity Spinner, URL/server-key/code EditTexts and connectionRequest ticket. FLAG_SECURE is unconditional because the form can show a server credential. URL/key are metadata-service credentials, not an encryption passphrase.


### View IDs — app/src/main/res/layout/activity_settings.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `info_root` | ScrollView | Shared information-screen root padded by InfoActivity.showPage. | Default visible/enabled unless parent or controller changes it. |
| `info_back` | MaterialButton | Inherited finish() Back control from InfoActivity.showPage. | Default visible/enabled unless parent or controller changes it. |
| `settings_sensitivity` | Spinner | Spinner populated from Relaxed/Normal/Strict array. | Default visible/enabled unless parent or controller changes it. |
| `settings_pad` | MaterialCheckBox | Saved default Gesture Pad choice. | Default visible/enabled unless parent or controller changes it. |
| `settings_protection` | MaterialCheckBox | Saved Hide/Reveal screenshot-protection choice. | Default visible/enabled unless parent or controller changes it. |
| `settings_autolock` | MaterialCheckBox | Saved Reveal auto-lock choice. | Default visible/enabled unless parent or controller changes it. |
| `settings_url` | EditText | Optional full HTTP(S) ledger endpoint;not automatically saved state. | saveEnabled=false |
| `settings_key` | EditText | Private API authentication input;masked,not saved/autofilled by view. | saveEnabled=false |
| `settings_test` | MaterialButton | Test entered endpoint/key without saving them. | enabled=false |
| `settings_disguise` | MaterialCheckBox | Saved calculator-on-normal-launch choice. | Default visible/enabled unless parent or controller changes it. |
| `settings_code` | EditText | Optional new6–12digit code;blank keeps existing hash;clears on pause/save. | saveEnabled=false |
| `settings_nerd` | MaterialCheckBox | Saved transient telemetry-display choice;forces secret-screen protection. | Default visible/enabled unless parent or controller changes it. |
| `settings_save` | MaterialButton | Validate/store current settings on worker;reenabled after result. | Default visible/enabled unless parent or controller changes it. |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

| Method/callback | Contract/caller and callees | Edges and state |
|---|---|---|
| onCreate(Bundle state) | Android -> showPage, secure flag, ArrayAdapter sensitivity choices/dropdown, bind fields/buttons, disable Save, preferences.load | Populate only after loaded/alive; Test/Save listeners registered. No incoming data. |
| populate() | load callback -> VaultPreferences.current() -> all checkboxes/sensitivity/URL/key; enable Save/Test | Code text is not populated from hash. Settings controls disable normal state/autofill for sensitive fields. |
| toggle(int id) | helper -> returns CheckBox from findViewById | Called by populate/save, which then call setChecked/isChecked. |
| testConnection() | Test -> ++connectionRequest, disable Test, connecting Snackbar -> SyncManager.testConnection(trim URL,key String,callback) | Uses unsaved form values. Current/alive callback re-enables Test, haptic and success/error Snackbar. Network on manager worker. |
| save() | Save -> parse/validate URL; copy code chars; validate disguise; clear code UI -> new Values -> preferences.save -> callback | A nonempty URL must use HTTP(S), have a host and no userInfo; empty URL is allowed to disable configuration. Enabled disguise needs valid new 6..12 digits or existing hash. Invalid code wiped and field error; invalid URL field error. Save disabled while worker commits. |
| save callback | preferences worker -> main saved boolean | Re-enable Save; if saved haptic and SyncManager.configurationChanged, then feedback. Failed commit offers retry via Save. |
| onPause() | Android -> code.setText(empty) | URL/key form intentionally remain during same Activity lifetime. |
| onStop() | Android -> ++connectionRequest, Test enabled | Obsolete connectivity callback ignored. No custom onDestroy or Bundle save logic. |

Action flows: toggle/spinner -> form state only -> Save -> VaultPreferences.save -> DisguiseCode hash if new code -> SharedPreferences.commit -> main callback -> SyncManager.configurationChanged. Blank code preserves existing hash. Default pad, sensitivity, screenshot protection, auto-lock, disguise and nerd options are settings; they do not modify the version-1 token IDs or wire format. Test -> testConnection -> SyncClient HTTP request -> result Snackbar; it does not implicitly save the fields. Back -> inherited finish, so unsaved edits may be lost. Server key is deliberately persisted in private preferences as a String; do not describe that as hardware-backed encrypted storage. HTTP is accepted by validation, so TLS confidentiality is only available when using HTTPS (platform network policy still applies).

## AboutActivity: static workstream description

ui/AboutActivity.java is created by Home's More menu. Its only custom method is onCreate(Bundle): super.onCreate then showPage(activity_about). It owns no custom fields, background jobs, sensitive state, saved state or outgoing Intent. Back -> InfoActivity listener -> finish. The remaining visible labels, description and Team A/B/C text are un-IDed static XML TextViews; they are not named-person credits or live server content. No other listeners/callbacks are registered by this class.


### View IDs — app/src/main/res/layout/activity_about.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `info_root` | ScrollView | Shared information-screen root padded by InfoActivity.showPage. | Default visible/enabled unless parent or controller changes it. |
| `info_back` | MaterialButton | Inherited finish() Back control from InfoActivity.showPage. | Default visible/enabled unless parent or controller changes it. |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

## LearnActivity: local interactive lesson and remote reference

**Purpose/caller/fields:** Home creates ui/LearnActivity.java. It uses InfoActivity setup, WebView web and boolean remote/failed state. WebView is an embedded web renderer; JavaScript is web scripting. A Content Security Policy (CSP) restricts allowed page resources. No Android JavaScript bridge is installed.


### View IDs — app/src/main/res/layout/activity_learn.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `info_root` | LinearLayout | Shared information-screen root padded by InfoActivity.showPage. | Default visible/enabled unless parent or controller changes it. |
| `info_back` | MaterialButton | Inherited finish() Back control from InfoActivity.showPage. | Default visible/enabled unless parent or controller changes it. |
| `learn_local` | MaterialButton | Switch WebView to bundled offline explainer. | Default visible/enabled unless parent or controller changes it. |
| `learn_remote` | MaterialButton | Switch to configured HTTPS Wikipedia article. | Default visible/enabled unless parent or controller changes it. |
| `learn_web` | WebView | Embedded local/remote web content. | Default visible/enabled unless parent or controller changes it. |
| `learn_error` | ScrollView | Designed unavailable-page panel replacing WebView on main-frame error. | visibility=gone |
| `learn_retry` | MaterialButton | Reload current local/remote choice. | Default visible/enabled unless parent or controller changes it. |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

| Method/callback | Inputs/result/caller and calls | Failure/security/lifetime |
|---|---|---|
| onCreate(Bundle state) | showPage; configure WebView file/content access off and mixed-content never, install anonymous WebViewClient, bind Local/Remote/Retry | Restore only remote boolean then load. Local page is app asset; network page not secret-bearing. |
| load(boolean online) | buttons/initialization -> remote flag, failed=false, stopLoading, hide error/show web, JS enabled iff local, loadUrl | Local file:///android_asset/how_it_works.html; remote resource URL is Wikipedia steganography article. |
| shouldOverrideUrlLoading(WebView,WebResourceRequest) | WebViewClient -> boolean block/allow navigation | For main frame only, allows HTTPS en.wikipedia.org; accepted navigation sets remote and disables JS. Other main hosts/schemes blocked; subframes return false. |
| onReceivedError / onReceivedHttpError | WebViewClient -> only main-frame showError | Small subresource errors do not replace whole lesson. |
| showError() | void -> once-only failed flag, stop loading, hide web/show error and failure feedback | Retry -> load(remote); Local -> offline asset. |
| onSaveInstanceState(Bundle out) | writes remote bool | Does not persist page form/content/history as secret state. |
| onPause()/onResume() | web.onPause/onResume then inherited behavior | Browser's own loader handles network asynchronously; Activity does not create an executor. |
| onDestroy() | stopLoading, install inert WebViewClient, remove WebView from parent, destroy | Releases renderer callbacks; no plaintext cache intentionally created. |

Action chains: Local -> load(false) -> asset page; Remote -> load(true) -> WebView.loadUrl -> main-frame success or error callback; Retry -> load(remote); Back -> finish. The local asset's HTML IDs are swatch, bit, value and toggle: coloured pixel example, current low bit, decimal/binary display and toggle button. JavaScript onclick toggles hidden 0/1, changes red 100/101, binary value and swatch/button text. Example: decimal100 binary01100100 -> set low bit1 ->101 binary01100101; only 1 intensity step changes. Inline CSS/JS are allowed by that local CSP, external resources are not; reduced-motion CSS respects display preference. This is educational public data, not an embed/decrypt call. No ACTION_VIEW leaves the app for remote lesson; it stays inside WebView.

## ScannerActivity: original-image header inspection

**Purpose/callers/fields:** Home or external image Intent creates ui/ScannerActivity.java. It extends InfoActivity. State: worker/main Handler, XRayView/status, plane Bitmap, selected/camera Uri, volatile generation/destroyed, capturing/launchPending/foreground. A header match reports structurally recognizable vault data; it does not prove authentication or reveal the secret.


### View IDs — app/src/main/res/layout/activity_scanner.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `info_root` | ScrollView | Shared information-screen root padded by InfoActivity.showPage. | Default visible/enabled unless parent or controller changes it. |
| `info_back` | MaterialButton | Inherited finish() Back control from InfoActivity.showPage. | Default visible/enabled unless parent or controller changes it. |
| `scan_choose` | MaterialButton | OpenDocument image input. | Default visible/enabled unless parent or controller changes it. |
| `scan_camera` | MaterialButton | Prepare capture file then external TakePicture. | Default visible/enabled unless parent or controller changes it. |
| `scan_status` | TextView | Working,header-found,absent,unreadable/camera error status. | Default visible/enabled unless parent or controller changes it. |
| `scan_reveal` | MaterialButton | Open original selected URI in Reveal;visible only after valid header detection. | visibility=gone |
| `scan_xray` | XRayView | Exploratory LSB plane only when structural header absent. | visibility=gone |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

| Method | Parameters/result and call chain | Edges/lifetime |
|---|---|---|
| onCreate(Bundle state) | Android -> showPage/bind pick,camera,reveal; restore camera/capturing/pending; controls/status; restore selected scan or receive(getIntent()) | Camera launch waits foreground. Saved image Uri is not a secret. |
| onNewIntent(Intent intent) | Android singleTop -> super,setIntent,receive | Handles new candidate without requiring a second scanner. |
| receive(Intent intent) | VIEW data OR SEND IntentCompat EXTRA_STREAM/fallback first ClipData -> scan | Recognized action with null Uri still calls scan, invalidating prior result. |
| capture() | Camera click -> guard capturing, busy flags/generation, clear selection/xray/old capture, worker create temp JPEG/provider Uri -> main launchCamera | Directory/file/provider IO/runtime failures reset controls and status. |
| launchCamera(Uri uri) | worker result/resume -> if destroyed delete, else store Uri/pending; if foreground TakePicture.launch | Launch runtime error restores controls and deletes capture. |
| scan(Uri uri) | pick/external -> ticket++, clear selected/xray/Reveal; require non-null content scheme; retain Uri, status -> worker decode | Invalid scheme rejected, previous success hidden. Original content source used; old nonactive camera cleaned. |
| decode(Uri uri,int ticket) | worker -> ImageUtils.decodeExact(...,true), pixels, StegoCore.readHeader; if absent -> buildXRay; post showResult | Guards before/after decode. IO/runtime/OOM -> current error. finally recycle original and zero pixels; obsolete preview recycled. |
| showResult(int ticket,boolean found,Bitmap result) | main -> accept current ticket, attach plane if absent, status/haptic, Reveal only found | False header does not expose credentials; no key derivation. |
| showStatus(int text,boolean error) | current scan/capture callbacks -> resource status text and feedback; void | Error flag chooses failure feedback; never show raw payload. |
| reveal() | Reveal click -> explicit RevealActivity with selected data+READ | Same original Uri, never xray bitmap. |
| clearPlane() | void -> detach XRayView and recycle plane | Called new candidate/destroy. |
| cameraControls(boolean enabled) | void -> choose/camera enabled state | Capturing prevents conflicting capture. |
| discardCamera() / deleteCapture(Uri uri) | null retained camera Uri; worker ContentResolver.delete, ignore RuntimeException | If main worker shut down, temporary cleanup worker is created and shut down. No secure-delete claim. |
| onResume()/onPause() | foreground true/false; resume launches pending camera | Pausing does not cancel a nonsecret scan; generation/destroy controls adoption. |
| onSaveInstanceState(Bundle out) | save camera, selected, capturing, launchPending | Restores handoff references, not decoded images/keys. |
| onDestroy() | destroyed=true, generation++, clearPlane, discard capture if finishing, worker.shutdown | Recreation preserves pending camera URI; queued decode cleans resources. |

Registered Activity Results: OpenDocument(image/*) -> non-null scan; null leaves current choice. TakePicture -> reset capturing/controls then scan camera if success/non-null, otherwise delete capture/cancelled status. Picker-launch error reports unavailable; camera cannot reliably recover a carrier after photographing it, because sensor/image processing changes low bits. The private JPEG is capture input, not a saved carrier. Header-absent xray is a generated low-bit plane display, not a decryption attempt. Data is temporary bitmaps/Uri only; scanner does not persist payload or gestures. Reveal later records encrypted-image metadata itself.

~~~text
Choose -> OpenDocument callback -> scan -> worker decodeExact -> StegoCore.readHeader
   found -> showResult -> Reveal button -> RevealActivity(data=original Uri)
   absent -> buildXRay -> showResult -> XRayView (no plaintext)
Camera -> capture -> worker temp/provider -> launchCamera -> TakePicture callback -> scan
External SEND/VIEW -> onCreate/onNewIntent -> receive -> scan -> same branches
~~~

## VaultActivity: searchable local ledger and metadata controls

**Purpose/caller/fields:** Home creates ui/vault/VaultActivity.java. It holds worker/main, search Runnable, VaultRepository, VaultAdapter, query/root views, nullable Long from/to bounds, selectedEntry, generation/closed, sheet and CreateDocument(text/csv) launcher. A nullable Long is an object integer that can also mean no date filter. It does not hold decrypted payloads.


### View IDs — app/src/main/res/layout/activity_vault.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `vault_root` | LinearLayout | Library root with safe/keyboard insets. | Default visible/enabled unless parent or controller changes it. |
| `vault_back` | MaterialButton | Finish library Activity. | Default visible/enabled unless parent or controller changes it. |
| `vault_search` | TextInputEditText | Debounced literal neutral-title search;query manually saved in Bundle. | saveEnabled=false |
| `vault_filter` | MaterialButton | Choose inclusive creation date range through two date pickers. | Default visible/enabled unless parent or controller changes it. |
| `vault_clear_filter` | MaterialButton | Clear title and date filters;only visible when filtering. | visibility=gone |
| `vault_export` | MaterialButton | History CSV bottom sheet then CreateDocument. | Default visible/enabled unless parent or controller changes it. |
| `vault_range` | TextView | All-dates/current inclusive visible date label. | Default visible/enabled unless parent or controller changes it. |
| `vault_progress` | ProgressBar | Indeterminate repository-search progress. | visibility=gone |
| `vault_list` | ListView | ListView using image-and-text VaultAdapter rows. | visibility=gone |
| `vault_empty` | ScrollView | Designed empty/no-match panel. | visibility=gone |
| `vault_empty_title` | TextView | Different title for no vaults vs filtered no matches. | Default visible/enabled unless parent or controller changes it. |
| `vault_empty_text` | TextView | Different recovery explanation for those empty states. | Default visible/enabled unless parent or controller changes it. |
| `vault_empty_action` | MaterialButton | Create first vault or clear filters depending current state. | Default visible/enabled unless parent or controller changes it. |
| `vault_error` | ScrollView | Designed load-failure panel. | visibility=gone |
| `vault_retry` | MaterialButton | Retry current repository search. | Default visible/enabled unless parent or controller changes it. |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

### View IDs — app/src/main/res/layout/vault_row.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `vault_row_thumbnail` | ImageView | Cached encrypted-image thumbnail or lock placeholder. | Default visible/enabled unless parent or controller changes it. |
| `vault_row_title` | TextView | Local neutral vault title. | Default visible/enabled unless parent or controller changes it. |
| `vault_row_date` | TextView | Creation date/time. | Default visible/enabled unless parent or controller changes it. |
| `vault_row_size` | TextView | Known payload size/capacity or unknown-before-unlock message. | Default visible/enabled unless parent or controller changes it. |
| `vault_row_expiry` | TextView | Expiry/expired date label;hidden if unset. | visibility=gone |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

| Method/callback | Contract/caller and calls | Failure/state |
|---|---|---|
| onCreate(Bundle state) | edge/layout/insets, repo/adapter, restore query/bounds before listeners, bindList/bindFilters/rangeLabel, CSV button sheet | Creates private metadata objects; DB opened/read on worker. |
| bindList() | attach adapter/register context menu; click opens entry; long-click haptic returns false; Back finish; empty action; Retry load | Returning false allows Android context-menu behavior after haptic. |
| bindFilters() | range button -> VaultDialogs.range; Clear -> clearFilters; TextWatcher.onTextChanged -> generation++,rangeLabel, cancel/post search250ms | beforeTextChanged/afterTextChanged empty. Debounce avoids a DB query per keystroke; generation rejects old results. |
| insets() | onCreate -> WindowInsets listener, bars/cutout/IME safe padding | void; handles keyboard around search without DB work. |
| load() | ++ticket, snapshot query/from/to, show progress/hide error -> worker repository.search -> main show | Runtime/OOM -> loadFailed only current/open screen. |
| show(List<VaultEntry> entries) | main result -> adapter.replace, progress gone, list/empty state, filtered label/action | Empty filtered result offers clear; unfiltered offers Hide. |
| loadFailed() | progress gone/error shown, list/empty hidden, failure feedback | Retry runs load with current filters. |
| filtered() | boolean query nonempty OR from !=null | Range state determines clear affordance. |
| clearFilters() | from/to null, query empty, remove delay, rangeLabel, load | Clears both query and dates. |
| rangeLabel() | no args void -> medium date display; end label uses to-1ms | to is exclusive next-midnight, not inclusive midnight that drops final day. |
| open(VaultEntry entry) | item click -> explicit Reveal with Uri.parse(entry.imageUri) | Runtime failure Snackbar with retry load; missing provider/read handled again in Reveal. |
| onCreateContextMenu(...) | Android -> remember selectedEntry, menu title/items | ID1 Rename,2 Expiry,3 Delete. |
| onContextItemSelected(MenuItem item) | boolean handled -> dialog -> change mutation | No selection delegates super; not arbitrary stored SQL. |
| change(Runnable mutation) | worker mutation -> main haptic/load/updated Snackbar | Runtime error -> failure; UI doesn't run SQLite mutation. |
| failure(int message) | failure feedback + Snackbar action load | Retry refreshes data; it does not silently repeat a failed destructive/mutation operation. |
| onResume() | Android -> load | Return from Reveal/settings refreshes records. |
| onPause() | dismiss sheet | Does not wipe/delete ledger. |
| onSaveInstanceState(Bundle out) | query/from/to only | No thumbnail/payload saved in Bundle. |
| onDestroy() | closed=true, remove search delay, adapter.close; queue repository.close; worker.shutdown | Prevent late UI adoption and release worker/database. |

| User action | Full ASCII call chain | Store, failure and security |
|---|---|---|
| Search | query change -> 250ms Runnable -> load -> VaultRepository.search(query,from,to) -> main show -> VaultAdapter.replace/getView | SQLite metadata only; no secret content indexing. Current tickets reject outdated result. |
| Date range | button -> VaultDialogs.range -> start DatePicker -> end DatePicker -> callback long[2] -> from/to -> rangeLabel/load | End constrained no earlier than start; normalized midnight, end+one day exclusive. Cancel causes no mutation. |
| Clear | clear / filtered empty action -> clearFilters -> load | Leaves database unchanged. |
| Empty unfiltered -> Hide | empty action -> explicit HideActivity | New forge feature, not a fake ledger entry. |
| Open row | ListView onItemClick -> open(entry) -> RevealActivity(data=imageUri) | Reveal independently authenticates original PNG; thumbnail is not used. |
| Rename | long press -> context item1 -> VaultDialogs.rename -> validated callback -> change -> repository.rename -> load | Display title metadata only, not encrypted payload filename; blank disallowed, 80-char cap. |
| Set expiry | context item2 -> VaultDialogs.expiry -> DatePicker -> TimePicker -> callback -> change -> repository.setExpiry -> load | Local current-clock policy; seconds/milliseconds set0. Neutral clears to null. No remotely enforced destruction. |
| Delete | context item3 -> confirmation positive -> change -> repository.softDelete -> load | Negative cancels. Soft deletion retains image/history; it is not a wipe/revoke operation. |
| Export CSV | Export -> ExportSheet -> CreateDocument.launch -> Uri callback -> disable Export -> worker HistoryCsv.export -> main enable/Snackbar | Cancellation no write. CSV contains metadata/events, not keys/plaintext. Provider/write failures offer error. |
| Retry | load error button / Snackbar action -> load | Keeps filters and refreshes. |

### VaultAdapter and VaultDialogs

VaultAdapter.java is created by VaultActivity. Fields own entries list, a 4MiB LruCache keyed by entry UUID for thumbnails, single worker/main and closed flag. replace(List<VaultEntry>) replaces list/notifies; getCount returns list length, getItem(position) entry, getItemId(position) row identity; getView(position,convertView,parent) inflates/reuses vault_row and binds title, formatted date, payload size/type (unknown when payloadType=0), expiry and thumbnail. Thumbnail helper sets view.tag to UUID and placeholder, uses cached Bitmap if present, else worker decodes thumbnail blob; exceptions/OOM keep placeholder. Main callback recycles if closed, otherwise caches and attaches only if tag still matches. This avoids a late image being attached to a recycled row for another record. close() marks closed, shuts worker and evicts cache; it does not indiscriminately recycle cached bitmaps still visible in a row. No payload decryption occurs.

VaultDialogs.java has static dialog helpers, no retained Activity state. rename(Activity,VaultEntry,callback) makes a Java EditText (no resource ID), min48dp, length80, state/autofill off; dialog positive listener installed onShow validates trimmed nonempty text, then callback/dismiss or field error. Negative cancels. expiry helper offers Clear (callback null) and Set -> chooseDate -> TimePicker -> millisecond timestamp callback. range helper chains two date dialogs, midnight normalization and exclusive upper bound callback long[2]. chooseDate overloads create DatePickerDialog with chosen initial date and optional minimum; callback supplies selected Calendar/date. Picker listeners are UI callbacks; actual SQLite writes happen through change on worker. Date/time/title are intentionally metadata, not secrets wiped on pause.

SQLiteOpenHelper/VaultDatabaseHelper owns schema; VaultRepository owns database queries and CRUD (create/read/update/delete) operations; rows include image identity/URI, title, timestamps, ghost/passphrase flags, sizes/thumbnail, expiry/deleted state and sync metadata. VaultLogger appends operation events. Exact schema/SQL belong to the data documentation stage; no D4 screen invents a second store. SharedPreferences holds app configuration and disguise hash; optional MySQL holds synchronized metadata/events. Deleting local metadata is not deleting a remote copy or exported image.

## GhostComparisonActivity: public Standard/Ghost visual lesson

**Purpose/caller/fields:** Hide result opens ui/GhostComparisonActivity.java with an explicit empty Intent. It owns single worker/main, Pair data, diff/status/stat/loupe views and busy/closed flags. No user cover, payload, tokens or Intent data is accepted.


### View IDs — app/src/main/res/layout/activity_ghost_comparison.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `diff_root` | ScrollView | Comparison scroll root with safe insets. | Default visible/enabled unless parent or controller changes it. |
| `diff_back` | MaterialButton | Finish comparison and return to previous screen. | Default visible/enabled unless parent or controller changes it. |
| `diff_status` | TextView | Building/ready/retry guidance. | Default visible/enabled unless parent or controller changes it. |
| `diff_progress` | LinearProgressIndicator | Indeterminate public-demo generation progress. | Default visible/enabled unless parent or controller changes it. |
| `diff_standard` | DiffMapView | Standard-placement diff map;touch/click pixel inspection. | Default visible/enabled unless parent or controller changes it. |
| `diff_standard_stats` | TextView | Exact Standard source statistics. | Default visible/enabled unless parent or controller changes it. |
| `diff_standard_pixel` | TextView | Standard exact pixel/LSB inspector label. | Default visible/enabled unless parent or controller changes it. |
| `diff_ghost` | DiffMapView | Ghost-placement diff map;touch/click pixel inspection. | Default visible/enabled unless parent or controller changes it. |
| `diff_ghost_stats` | TextView | Exact Ghost source statistics. | Default visible/enabled unless parent or controller changes it. |
| `diff_ghost_pixel` | TextView | Ghost exact pixel/LSB inspector label. | Default visible/enabled unless parent or controller changes it. |
| `diff_replay` | MaterialButton | Restart both sweep animations. | Default visible/enabled unless parent or controller changes it. |
| `diff_retry` | MaterialButton | Generate fresh public-demo pair again. | Default visible/enabled unless parent or controller changes it. |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

| Method | Caller, inputs/result and work | Error/cleanup |
|---|---|---|
| onCreate(Bundle state) | Android -> edge/layout/16dp insets, bind both loupes, Back/Retry/Replay, generate | Starts only this demonstration worker. |
| generate() | initial/Retry; guard busy/closed -> loading/public fixture label -> worker GhostComparisonWork.create -> main show | Exception/OOM -> current error status/failure feedback, retry enabled. |
| show(Pair result) | bind both maps/stats/instructions, ready/busy false; if resumed sweep+haptic | Swap then dispose previous; if result arrives after close, dispose instead. |
| setBusy(boolean value) | progress and Generate/Replay gating | Avoid simultaneous pairs; no plaintext view. |
| dispose(Pair old) | queue old.close on worker (fallback cleanup worker if shutdown) | UI data detached before recycle. |
| onPause() | stop both sweeps | No re-forge merely from replay. |
| onDestroy() | closed=true, detach maps, dispose Pair, worker.shutdown | Guard callbacks from destroyed screen. No state Bundle restoration. |

Action chains: Retry/initial -> generate -> worker create(public sample string) -> two VaultEngine.hide calls -> DiffMapData.build -> main show; Replay -> both DiffMapView.startSweep; touch maps -> bindLoupe source-coordinate callback; Back -> finish. Full DiffMapView touch and animation contracts are in the Hide/Reveal guide: outside down ignored, drag clamped, release performClick; keyboard/accessibility click selects centre.

GhostComparisonWork.java is a static fixture creator. create(String publicText) returns AutoCloseable Pair with standard/ghost maps and statistics. It creates a public 320x320 cover, a 2048-byte repeated-text payload, random eight-token gesture and 32-letter phrase, then calls its encode helper once with ghost=false and once true, compression=false. Each VaultEngine.hide uses independent random salt/IV; mode maps therefore are illustrative, not identical encrypted bits permuted. Each encode builds a difference map from original clone and stego. finally recycles cover, closes Payload and wipes fixture credentials/content; interruption guard cancels. Pair.close() closes both map owners. No ledger entries or secret export are required here.

Ghost idea/numbers/code: leave header pixels0..101 ordered; build order of all pixels102..end; salt bytes seed a long via seed=seed*31+(value&255); java.util.Random(seed) runs backwards swapping i with nextInt(i+1). Example a tail [102,103,104], draws j=0 at i=2 -> [104,103,102], then j=1 at i=1 -> unchanged. These are illustrative valid draws, not claimed output for a particular salt. Body bits visit RGB channels within each chosen pixel. Both hide/extract rebuild same order; no extra secret key is introduced. Standard mode uses sequential pixels, so partial payloads cluster near top; Ghost distributes changes. Header length38 bytes reserves102 pixels; RGB low-bit change maximum1; map colours/PSNR summarize differences, not stronger encryption.

## TestHarnessActivity: developer-facing Lab screen

**Purpose/caller/fields:** Home Lab opens diagnostics/TestHarnessActivity.java. It visibly exposes Run, theme switch, status, stage progress, per-mode results and preview. This guide describes its existing feature; no test execution is part of this documentation task. Fields hold one worker/main, cancellable Future task, view references, busy flag/currentStage/currentIndex.


### View IDs — app/src/main/res/layout/activity_test_harness.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `harness_root` | CoordinatorLayout | Integrity Lab outer root with system/cutout insets. | Default visible/enabled unless parent or controller changes it. |
| `cover_preview` | ImageView | Generated public-fixture display thumbnail. | Default visible/enabled unless parent or controller changes it. |
| `status` | TextView | Lab READY/TESTING/PASS/FAIL status. | Default visible/enabled unless parent or controller changes it. |
| `summary` | TextView | Lab idle/current stage/final timing/failure stage explanation. | Default visible/enabled unless parent or controller changes it. |
| `test_progress` | LinearProgressIndicator | Indeterminate built-in demonstration progress. | visibility=gone |
| `run_test` | MaterialButton | Run/repeat built-in public-data demonstration;disabled while busy. | Default visible/enabled unless parent or controller changes it. |
| `standard_result` | TextView | Standard text/file demonstration timings/statistics/hash prefix. | Default visible/enabled unless parent or controller changes it. |
| `ghost_result` | TextView | Ghost text/file demonstration timings/statistics/hash prefix. | Default visible/enabled unless parent or controller changes it. |
| `light_preview` | MaterialSwitch | Temporary light/night app theme preview;disabled during run. | saveEnabled=false |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

| Method/callback | Inputs/result and call chain | Failure/lifetime |
|---|---|---|
| onCreate(Bundle state) | Android -> edge/layout/safe24dp insets, bind views, theme initial state and checked listener, Run startTest | Theme listener changes AppCompatDelegate MODE_NIGHT_NO/YES. No auto-run on opening. |
| bindViews() | onCreate -> findViewById assigns eight control references | void; no expensive work. |
| installInsets() | onCreate -> root bar/cutout listener, horizontal space_lg and safe top/bottom | void; callback returns insets. |
| startTest() | Run -> guard busy, haptic, TESTING/status reset, busy -> task=worker.submit(executeTest) | Disabled controls stop duplicate run. |
| executeTest() | worker -> HarnessRunner.run(stage listener) -> main showStage; completed report -> main showReport | Cancellation ignored; OOM/memory, IO/storage, security/crypto and Runtime/unexpected map distinct failure message. |
| showStage(int index,int resource) | stage callback -> alive UI, remember stage/index, formatted progress | Late closed screen ignored. |
| showReport(HarnessReport report) | main -> PASS/timing/modeText, preview CENTER_CROP | If dead report.close. If accepted preview ownership transferred by report.preview=null. |
| modeText(HarnessReport report,boolean ghostMode) | returns formatted String for matching mode entries | Includes type/timing/PSNR/changed fraction and first12 SHA characters. |
| fail(int message) | posts main -> alive FAIL/status/stage explanation, busy off, failure colour | Run again offers recovery. |
| setBusy(boolean busy) | void -> disable Run/Lights while work, toggle indeterminate progress | Never blocks UI waiting on Future.get. |
| onDestroy() | task.cancel(true), worker.shutdownNow, detach preview | Unlike Hide cleanup queue, fixture worker uses interruption. No custom pause/stop cancellation; merely covering Activity does not imply destroyed/cancelled. |

Run chain: Run -> startTest -> executeTest -> HarnessRunner.run -> generate fixture -> hide -> PNG save -> exact reload -> reveal -> compare -> stats/hash -> HarnessReport -> main showReport. Theme chain: switch -> checked listener -> AppCompatDelegate.setDefaultNightMode; this is process theme state, not a persisted user preference; fresh app starts dark through VaultApplication. Back uses Android inherited navigation, no custom result Intent.

HarnessRunner.java owns public fixture values: 640x360 cover, seed 0x4b564c54, text/file for Standard and Ghost (four cases), tokens {0,0,4,5}, phrase phase1b-public-fixture, file4096 bytes with byte(i*31). Per case it saves PNG, reloads exact pixels, checks bytes/type/name round trip, compares pixel delta <=1 and changed count>0, calculates SHA-256 and timing/statistics, and builds report Entry. Temporary PNGs deleted and arrays wiped in finally; stage callbacks only carry public labels. HarnessReport.java groups timing/entries/preview; AutoCloseable.close disposes owned preview. MessageDigest is java.security's hash worker used for public SHA-256 diagnostics, not encryption. Lab does not write user Vault operation records or use the user's motion/passphrase. Large image, storage, crypto or assertion-like runtime failures become designed FAIL output rather than leaking payloads.

## Shared export-sheet layout


### View IDs — app/src/main/res/layout/sheet_export.xml

| View ID | Type | Purpose / owning behavior | XML initial state |
| --- | --- | --- | --- |
| `export_note` | TextView | Caller-supplied disclosure/storage note in shared bottom sheet. | Default visible/enabled unless parent or controller changes it. |
| `export_options` | LinearLayout | Container receiving Java-created labeled export buttons. | Default visible/enabled unless parent or controller changes it. |
| `export_cancel` | MaterialButton | Dismiss export sheet without running an option. | Default visible/enabled unless parent or controller changes it. |

Unidentified-by-ID headings,notes and layout containers remain static XML content; their text comes from named string resources. They are not missing Activity bindings.

Only callers create per-option Buttons in Java; no hidden XML IDs for each action. Vault's sheet launches CSV picker; Hide/Reveal sheet actions and plaintext/carrier warnings are in the other guide. Caller dismisses sheet on pause. The returned BottomSheetDialog reference is UI state, not database state.

## Real constructs and small source examples

The following source excerpts are at most ten lines each. They explain Java/Android constructs beyond plain fields/control flow, using this project's actual code. No new implementation or imagined method is introduced by an excerpt.

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

## Supporting callback and algorithm details

VaultAdapter.date(long timestamp) returns formatted date text for getView/expiry. expiry(TextView view,VaultEntry entry) sets label/visibility and expired warning based on nullable expiry/current clock; it does not decrypt or delete anything. thumbnail(ImageView view,VaultEntry entry) is the cache/tag/worker path described above. hasStableIds() returns true and getItemId(int position) returns entry.id (SQLite row identity, not UUID hash). UUID is the separate thumbnail cache/tag identity. Every getView call rebinds text/expiry/image to avoid recycled-row leftovers. VaultDialogs.expiryDate(VaultActivity,VaultEntry,Consumer<Long>) obtains date then TimePicker, and dateDialog(VaultActivity,Calendar,Consumer<Calendar>) returns the configured DatePickerDialog; chooseDate overloads show it with optional title. DatePicker/TimePicker listeners receive numeric fields, update Calendar and invoke the Consumer. These are transient Java dialog controls without layout resource IDs.

SyncClient.post(String address,String key,JSONObject body,BooleanSupplier paused) returns JSONObject or throws: require worker, parse URL, allow HTTP(S) only/host/no userInfo or fragment, reject blank/newline key, honor platform cleartext policy, check paused, open HttpURLConnection, 5000ms connect/read timeout, POST JSON with X-API-KEY, no redirects/caches, write bytes, require200/not paused, parse response, finally disconnect. response(InputStream,BooleanSupplier) returns JSONObject after bounded4096-byte reads, max65536 response bytes and ok=true. Exceptions become test/offline retry feedback through SyncManager; server key never becomes a motion key. BooleanSupplier is an interface returning a current boolean on demand. A method reference/listener can supply that pause check without copying stale state.

GhostComparisonWork.guard() returns void or CancellationException if thread interrupted; encode(int[] original,Payload payload,byte[] tokens,char[] phrase,boolean ghost,WorkProgress progress) calls guard, VaultEngine.hide then DiffMapData.build(original.clone(),encoded,320,320), returning DiffMapData. WorkProgress is an interface callback with onProgress(stage,done,total); fixture progress invokes guard only. Pair's constructor retains standard/ghost; close returns void and closes both maps. SecureRandom supplies fixture token ints0..5 and chars'a'..'z' before casts. These public fixtures are wiped for consistent ownership, not because they are the user's secret.

Image-difference arithmetic is actual RGB arithmetic, not comparing compressed PNG bytes. Example one changed channel100->101 among three channels gives MSE=1/3; PSNR=10log10(255*255/MSE)=52.90dB, unchanged image has infinite PSNR. ImageUtils.statistics iterates channels, accumulates squared errors/changed count/max absolute delta, then ImageStats stores/display formulas. In DiffMapData.build a red-channel XOR difference sets the map's red component; many source cells sharing a preview bin OR changes together. buildXRay samples each low bit then multiplies by255 (100&1=0,101&1=1). DiffMapView loupe still queries retained original coordinates, not a guessed enlarged thumbnail pixel.

Engine overview for examiner follow-up: pack type/name-length/name/content-length/content -> GZIP envelope -> GestureCredential.canonical -> PBKDF2WithHmacSHA256 (120000 iterations,256-bit key,16-byte salt) -> AES-GCM (12-byte IV,16-byte tag,AAD=38-byte header) -> RGB LSB embedding. Read performs header/length validation -> body extraction (same salt-seeded order for Ghost) -> GCM verification/decrypt -> bounded GZIP -> PayloadCodec unpack. For100x100 pixels, reserved header102 pixels leaves floor((10000-102)*3/8)=3711 ciphertext bytes; zipped1000+tag16=1016 fits. PNG preserves channel bytes; JPEG changes them. The Lab exercises this real pipeline but does not define a different format. Each step's byte offsets/crypto/zip class behavior is fully documented in D2. GCM authentication, not Scanner's header check or PSNR, decides whether recovered bytes can be trusted.

No screen in this guide records raw gestures, encryption passwords, keys or secret text in preferences, SQLite or MySQL. Intent image URIs and titles/history are metadata and can still reveal activity/cover information. Settings URL/key and the calculator gate have separate persistence/security implications already stated. Theme, public lesson, pulse, xray, sweep and public Lab fixture are not secret storage. No general secure-erasure promise follows from clearing a Java field or deleting a cache file.

## Likely viva questions

1. **What actually starts when the app icon is tapped?** SplashActivity, which waits for preferences and routes to Home or the calculator disguise.
2. **Which screen accepts another app's shared image?** Exported ScannerActivity handles image SEND and content VIEW, including singleTop onNewIntent; Reveal remains private.
3. **Does Vault Delete erase a carrier or expire its copies?** No. It soft-deletes ledger metadata; local expiry only gates later unlocks using this local record and clock.
4. **Why does the adapter compare a view tag before attaching a thumbnail?** A ListView reuses rows; a delayed result must still match the record currently shown in that row.
5. **Does a successful sync test save the form or upload a secret?** No. It tests the entered server configuration. Save commits settings separately, and sync carries operation metadata rather than encryption credentials/plaintext.

## Covers these files

- app/src/main/java/com/example/kineticvault/VaultApplication.java
- app/src/main/java/com/example/kineticvault/ui/InfoActivity.java
- app/src/main/java/com/example/kineticvault/ui/SplashActivity.java
- app/src/main/java/com/example/kineticvault/ui/SplashLogoView.java
- app/src/main/java/com/example/kineticvault/ui/HomeActivity.java
- app/src/main/java/com/example/kineticvault/ui/CalculatorActivity.java
- app/src/main/java/com/example/kineticvault/ui/SettingsActivity.java
- app/src/main/java/com/example/kineticvault/ui/AboutActivity.java
- app/src/main/java/com/example/kineticvault/ui/LearnActivity.java
- app/src/main/java/com/example/kineticvault/ui/ScannerActivity.java
- app/src/main/java/com/example/kineticvault/ui/vault/VaultActivity.java
- app/src/main/java/com/example/kineticvault/ui/vault/VaultAdapter.java
- app/src/main/java/com/example/kineticvault/ui/vault/VaultDialogs.java
- app/src/main/java/com/example/kineticvault/ui/GhostComparisonActivity.java
- app/src/main/java/com/example/kineticvault/ui/GhostComparisonWork.java
- app/src/main/java/com/example/kineticvault/diagnostics/TestHarnessActivity.java
- app/src/main/java/com/example/kineticvault/diagnostics/HarnessRunner.java
- app/src/main/java/com/example/kineticvault/diagnostics/HarnessReport.java
- app/src/main/java/com/example/kineticvault/ui/ExportSheet.java
- app/src/main/java/com/example/kineticvault/ui/UiPolish.java
- app/src/main/java/com/example/kineticvault/ui/DiffMapData.java
- app/src/main/java/com/example/kineticvault/ui/DiffMapView.java
- app/src/main/java/com/example/kineticvault/ui/DiffStats.java
- app/src/main/java/com/example/kineticvault/ui/WipeComparisonView.java
- app/src/main/java/com/example/kineticvault/data/VaultPreferences.java
- app/src/main/java/com/example/kineticvault/data/DisguiseCode.java
- app/src/main/java/com/example/kineticvault/data/SyncManager.java
- app/src/main/java/com/example/kineticvault/data/SyncClient.java
- app/src/main/java/com/example/kineticvault/data/SyncStore.java
- app/src/main/java/com/example/kineticvault/data/VaultRepository.java
- app/src/main/java/com/example/kineticvault/data/VaultDatabaseHelper.java
- app/src/main/java/com/example/kineticvault/data/VaultLogger.java
- app/src/main/java/com/example/kineticvault/data/HistoryCsv.java
- app/src/main/java/com/example/kineticvault/core/Constants.java
- app/src/main/java/com/example/kineticvault/core/StegoCore.java
- app/src/main/java/com/example/kineticvault/core/CryptoCore.java
- app/src/main/java/com/example/kineticvault/core/GzipCodec.java
- app/src/main/java/com/example/kineticvault/image/ImageUtils.java
- app/src/main/java/com/example/kineticvault/image/ImageStats.java
- app/src/main/AndroidManifest.xml
- app/src/main/assets/how_it_works.html
- app/src/main/res/layout/activity_home.xml
- app/src/main/res/layout/activity_calculator.xml
- app/src/main/res/layout/activity_settings.xml
- app/src/main/res/layout/activity_about.xml
- app/src/main/res/layout/activity_learn.xml
- app/src/main/res/layout/activity_scanner.xml
- app/src/main/res/layout/activity_vault.xml
- app/src/main/res/layout/vault_row.xml
- app/src/main/res/layout/activity_ghost_comparison.xml
- app/src/main/res/layout/activity_test_harness.xml
- app/src/main/res/layout/sheet_export.xml
- app/src/main/res/values/strings.xml
- app/src/main/res/values/settings_strings.xml
- app/src/main/res/values/scanner_strings.xml
- app/src/main/res/values/vault_strings.xml
- app/src/main/res/values/sync_strings.xml
