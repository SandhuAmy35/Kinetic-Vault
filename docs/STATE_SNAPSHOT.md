# STATE SNAPSHOT

Phase 1B COMPLETE — device-verified, four PASS, confirmed by the user:
Standard text, Standard binary file, Ghost text and Ghost binary file.
Core crypto and wire format remain frozen. Phase 2A and the 2B Hide flow are implemented.

Authoritative phases: 1 foundation (including 1B); 2 Motion + Hide + Reveal;
3 Data + Server; 4 Wow layer + Scanner; 5 Harden + Ship.

New files: app/src/main/java/com/example/kineticvault/VaultApplication.java; diagnostics/{HarnessReport,HarnessRunner,TestHarnessActivity}.java and image/{ImageMemory,ImageDecoder,ImageStats,ImageFiles,ImageUtils,CoverGenerator}.java under that Java package; app/src/main/res/xml/{file_paths,network_security_config}.xml; res/values/{dimens,styles}.xml; res/values-night/colors.xml; res/drawable/ic_{lock,shield,scan,history,settings,arrow_left,arrow_right,arrow_up,arrow_down,orb}.xml and bg_glow_card_{cyan,violet}.xml; res/anim/{fade_in,slide_up,shake,pulse}.xml; res/layout/activity_test_harness.xml (all res paths are under app/src/main); app/src/test/java/com/example/kineticvault/image/ImagePolicyTest.java; app/src/androidTest/java/com/example/kineticvault/image/{PngFixtures,ImageLayerTest}.java; docs/BUILD_HEALTH.md and docs/PHONE_CHECKLIST.md.

Public APIs: ImageUtils.decodeCover(Source, boolean), decodeExact(Source, boolean),
pixels(Bitmap), fromPixels(int[], int, int), savePng(Bitmap, File), thumbnail(Bitmap, int),
sha256(File), psnr(int[], int[]); CoverGenerator.generate(int, int, long);
HarnessRunner.run(Context, Listener). Image, crypto and disk work run off-thread.

Decisions: mutable sRGB ARGB_8888; covers flattened and limited by pixel count,
long side and memory; exact decode preserves pixel order; PNG-only output;
Ghost memory allowance includes shuffle storage. Dark-default Material 3;
private FileProvider; backups disabled; no CAMERA permission.

Earlier evidence: 47 JVM tests passed; resource compile/link passed. Agent Gradle
verification was environment-blocked. Separate instrumentation and release/R8
verification remain unconfirmed. No builds, tests or web searches ran in this update.

STOPPED after 2B. Await the next explicit START step.

2A files: sensor/SensorFusionManager.java; gesture/{GestureRecognizer,GestureSession,StrengthMeter,GesturePadView}.java; res/values/gesture_ids.xml. Java paths are under the existing app package.
SensorFusionManager(Context, GestureRecognizer.Listener): Mode start(), void stop(), void resetGesture(); main-thread lifecycle calls for onResume/onPause.
GestureRecognizer(Executor, Listener): void update(long,float,float,float,float,float,float), void reset(); injected main executor delivers onToken(byte,long) and onMotion(float,float,boolean,long).
GestureSession(Listener): boolean addToken(byte,long), void onMotion(long,boolean), boolean done(), int size(), boolean isFinished(), static boolean matches(GestureSession,GestureSession), byte[] takeTokens(), void reset(), void close().
StrengthMeter: static double bits(int), static String label(Context,int,boolean); GesturePadView: constructors (Context), (Context,AttributeSet), (Context,AttributeSet,int), void setOnTokenListener(OnTokenListener), boolean performClick(); listener boolean onToken(byte,long).
2A decisions: pure Java recognizer/session; pitch BACK-positive and roll RIGHT-positive; monotonic milliseconds; main sensor callbacks; accelerometer fallback; stop sensors and close sessions on pause; wipe transferred tokens after engine use.
Constants holds all motion tuning; strings/dimens and pad accessibility actions added. No screens added. Enrollment compares two distinct completed sessions before taking tokens.

2B Java files: ui/{HomeActivity,KineticOrbView,MatrixRainView,WipeComparisonView}.java; ui/hide/{HideActivity,HideWork,HideStore,HideForge,HideExporter,CoverStep,SecretStep,GestureStep,ResultStep,SecretInput}.java; data/VaultLogger.java.
2B resources: layout/activity_{home,hide}.xml and hide_step_{cover,secret,gesture,forge,result}.xml; strings.xml additions; Home is the launcher, Hide is portrait, Integrity Lab opens the existing harness; FileProvider also exposes cache/capture/ for TakePicture.
KineticOrbView(Context), KineticOrbView(Context,AttributeSet): void setAngles(float,float), setTokenCount(int,boolean), setAnimating(boolean), reset().
MatrixRainView(Context), MatrixRainView(Context,AttributeSet): void setProgress(int,int), setRunning(boolean); WipeComparisonView(Context), WipeComparisonView(Context,AttributeSet): void setImages(Bitmap,Bitmap), setFraction(float).
SecretInput(char[],Uri,boolean,boolean): PayloadCodec.Payload read(ContentResolver), void close(); VaultLogger: static void logHide(), logReveal(), logExport() (no-op).
2B decisions: exact packed/GZIP capacity on a single worker; prepared payload stays in memory; matching enrollment required; engine stage progress with indeterminate KDF; worker PNG/statistics/export; MediaStore 29+, CreateDocument below; FileProvider sharing warns Document/File.
Credentials and payload buffers are cleared after use/cancel; pausing discards enrollment and interrupts forging at safe engine boundaries; sensitive fields are not saved/autofilled, FLAG_SECURE is enabled. Reveal and other future tiles only show coming-soon explanations.
