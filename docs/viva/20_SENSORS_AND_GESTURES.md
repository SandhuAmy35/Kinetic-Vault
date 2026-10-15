# 20 — Sensors and gestures deep-dive

## What a gesture means in this app

Kinetic Vault turns phone motion into a short sequence of named moves, called **tokens**. A token is a small integer identifying a move, not a recording of the full motion. The **accelerometer** measures acceleration along the device's three axes, including the gravity contribution seen by Android's sensor. The **gyroscope** measures rotation rates about those axes. **Sensor fusion** combines those measurements to estimate orientation. The **recognizer** converts that orientation and gravity-removed acceleration into tokens. A **session** owns one temporary token recording. **Enrollment** means making two completed recordings and requiring the same token order and count.

The result is a repeatable discrete credential, not biometric authentication. Someone who knows the same named moves can enter them using the Gesture Pad. A strong passphrase is recommended because a short human-selected gesture offers little resistance to offline guessing (trying candidate credentials against a copied image without asking the app).

Java paths in this document are relative to `app/src/main/java/com/example/kineticvault/`; resource paths beginning `res/` are relative to `app/src/main/`. The complete target packages are one file in `sensor/` and four files in `gesture/`. Other files are read to explain the actual enrollment, lifecycle and key-derivation callers. The D2 documents describe encryption and image operations in depth; this document follows how motion reaches that key-derivation boundary.

## First-use Java and Android vocabulary

An **Activity** is an Android screen; a **View** is a screen control or drawing component. **Lifecycle methods** are Android's notifications that a screen is created, active, paused, stopped or destroyed. **UI** means user interface. The **main/UI thread** is the execution sequence handling taps/drawing; a **worker thread** handles expensive background tasks. **Handler** sends an operation to a thread's message queue; **Looper** is the loop servicing that queue. **Executor** is an interface for running a supplied task; it does not necessarily create a thread. **ExecutorService** additionally manages queued tasks and shutdown. An **interface** defines required methods; a **callback** is supplied by a caller and invoked later by its owner; a **lambda** (arrow syntax) implements a small callback. A **method reference** uses :: to pass an existing operation. An **enum** is a fixed set of named choices.

A Java **byte** is a signed8-bit integer; a **char** is an unsigned16-bit UTF-16 code unit; **int** is signed32-bit and **long** signed64-bit. A **bit** is a binary digit; a byte contains8bits. **float** and **double** represent floating-point numbers; finite means neither infinity nor NaN (not a number). **UTF-16** is Java's character representation; a supplementary character such as many emoji uses two code units. **ASCII** is the basic character set covering token digits and key-password prefix. **Hexadecimal** uses base16; one hex digit represents four bits. A **cast** explicitly converts a value to another type.

**Static** members belong to a class rather than an object; **final** prevents reassignment/subclassing in its declared context, but does not freeze array contents. A **constructor** initializes a new object. A **nested class** is declared in another class; a non-static **inner class** can use its outer instance. An **anonymous class** is an unnamed inline implementation. **Inheritance** (extends) reuses parent behavior; implements supplies interface behavior; @Override marks an inherited/interface implementation; super invokes the parent. **Generics**, such as Callback<T>, carry a type parameter, so a callback can return a particular result type safely. **null** means no object. **Ownership** identifies who must clear an array or stop/release a resource.

**Exceptions** report failures: IllegalArgumentException means invalid inputs; IllegalStateException means a wrong call state; NullPointerException can be thrown by Objects.requireNonNull. throws propagates a checked failure, catch handles it, finally performs cleanup on all exits. An **AutoCloseable** object has a close contract; **try-with-resources** calls close automatically. **synchronized** serializes access to shared state; **volatile** publishes updates between threads, and an **AtomicInteger** performs indivisible integer updates. None replaces the serialization contract of recognizer/session state.

A **Canvas** is Android's drawing surface; **Paint** carries colour/stroke/text style; **RectF** is a floating-point rectangle; **ValueAnimator** changes a value over time and calls an update listener. **Accessibility** means actions and descriptions available to assistive tools; a **Bundle** is Android's state/value container; a **resource ID** is the generated integer name used to retrieve a view/text/dimension (R.id, R.string, R.dimen). **dp** measures density-independent UI size; **sp** additionally follows text scaling. A **haptic** is system touch/vibration feedback.

## File/class map, ownership and important fields

| File / class or contract | One-sentence purpose | Created/called by | Important state |
| --- | --- | --- | --- |
| sensor/SensorFusionManager.java / SensorFusionManager | Combines accelerometer/gyro orientation and feeds gravity-removed samples to recognition. | GestureStep and RevealGesture construct it; Android calls its SensorEventListener methods after start registration. | SensorManager sensors; main Handler; recognizer; gravity/acceleration/gyro float[3]; pitch/roll, accelPitch/accelRoll, angularSpeed; mode/initialized/running; lastGyroNanos/lastAccelNanos; optional telemetry listener. |
| SensorFusionManager.Mode | Identifies gyro fusion, accelerometer fallback or unavailable sensing. | start returns it; caller shows recovery/status. | GYROSCOPE_FUSION, ACCELEROMETER_ONLY, UNAVAILABLE. |
| SensorFusionManager.TelemetryListener | Supplies transient raw sensor/angle samples to a display callback. | NerdOverlay implements it; SensorFusionManager.emit invokes it. | onSample(ax,ay,az,gx,gy,gz,pitch,roll,Mode). No stored history in interface. |
| gesture/GestureRecognizer.java / GestureRecognizer | Recognizes six named moves from serialized finite orientation/motion samples. | SensorFusionManager constructs it and calls update; controllers implement listener. | callbackExecutor/listener; enterDegrees/exitDegrees; volatile generation; lastSample/lastMove/flatSince/snapSince; tiltLatched/flatLatched/snapRejected. |
| GestureRecognizer.Listener | Separates recognized token events from ongoing motion/stillness events. | GestureStep/RevealGesture anonymous listeners; recognizer.deliver invokes them. | onToken(byte,long); onMotion(float,float,boolean,long). |
| gesture/GestureSession.java / GestureSession | Owns and validates one erasable4–8-token recording. | GestureStep creates first/current performances; RevealGesture creates one session. | tokens[8], listener, count; lastMove/lastMotion/stillSince; finished/closed. |
| GestureSession.Listener | Reports accepted counts, completion and too-short attempts. | Controllers supply anonymous callbacks; session invokes them. | onProgress(int), onFinished(), onTooShort(int). |
| gesture/StrengthMeter.java / StrengthMeter | Labels the honest upper bound on uniformly random gesture strength. | GestureStep uses label; bits delegates to GestureCredential. | Stateless static helpers; private constructor. |
| gesture/GesturePadView.java / GesturePadView | Draws six accessible touch/keyboard actions emitting the same tokens as motion. | Hide/Reveal XML inflates it; controllers set token callback. | TOKENS/LABELS/ACTIONS arrays; Paint; cells RectF[6], labels String[6]; gap/radius/stroke/textSize/touchMin; listener; selected/pressed/pointer/lastMove. |
| GesturePadView.OnTokenListener | Returns whether the session accepted a proposed pad token. | Controller lambda calls GestureSession.addToken; performClick invokes listener. | boolean onToken(byte,long). |
| GesturePadView.PadAccessibility | Exposes six named assistive actions through the same click path. | Main pad constructor creates the private inner AccessibilityDelegate. | Uses outer selected, ACTIONS/LABELS, performClick; no additional fields. |
| core/Constants.java / Constants | Centralizes fixed token IDs, gesture/fusion thresholds and format limits. | All motion/credential classes. | Public static final values and two sensitivity helpers; private constructor. |

Supporting enrollment controllers live in ui/hide/GestureStep.java (two sessions, matched enrolled byte[], sensors, orb/pad/status/buttons, replay), and ui/reveal/RevealGesture.java (one session, completed tokens, sensors/pad/status). They are created by their host Activities. Their complete motion-related operations are documented below. UI drawing helpers and crypto classes are context at this boundary, not extra implementations hidden inside sensor/gesture.

## Full feature chains, visible result, storage and recovery

```text
Hide motion:
HideActivity.onCreate -> GestureStep constructor -> SensorFusionManager constructor
 -> user Begin -> GestureStep.begin -> new GestureSession -> startSensors
 -> SensorFusionManager.start -> Android SensorEventListener.onSensorChanged
 -> accelerometer / gyroscope -> emit -> GestureRecognizer.update -> recognize
 -> callbackExecutor -> deliver -> GestureStep listener.onToken / onMotion
 -> GestureSession.addToken / onMotion -> Listener.onProgress/onFinished
 -> GestureStep.finishAttempt -> first complete -> repeat recording
 -> GestureSession.matches -> enrolled tokens -> Forge enabled

Hide pad:
GestureStep.bind -> GesturePadView.setOnTokenListener
 -> user touch / accessibility / keyboard activation -> GesturePadView.performClick
 -> controller listener -> GestureSession.addToken -> count/orb/strength
 -> Done -> GestureSession.done -> GestureStep.finishAttempt -> compare two runs

Reveal motion/pad:
RevealActivity creates RevealGesture -> user Record -> RevealGesture.begin
 -> sensors/recognizer OR pad -> GestureSession -> Done or continuous still finish
 -> onFinished -> sensors.stop -> session.takeTokens -> Unlock enabled
 -> RevealActivity.unlock -> RevealGesture.takeTokens -> worker decrypt/key derivation

Strength:
GestureSession.addToken -> GestureStep.Listener.onProgress
 -> StrengthMeter.label -> bits -> GestureCredential.entropyBits -> strength text

Private replay:
GestureStep replay click -> GestureReplay.show(enrolled) -> next
 -> named move + orb angle/count -> clear on dismissal/reset/host pause

Nerd Mode:
SensorFusionManager.emit -> NerdOverlay.onSample -> throttled display
GestureStep/RevealGesture token callback -> NerdOverlay.token -> render
 -> clear at recording completion/reset/pause

Key handoff:
HideActivity.forge -> GestureStep.takeTokens + SecretStep.takePassphrase
 -> HideWork.forge [worker] -> HideStore.forge -> HideForge.run -> VaultEngine.hide
 -> CryptoCore.encrypt -> transform -> derive -> GestureCredential.canonical -> PBKDF2
RevealActivity.unlock -> RevealGesture.takeTokens -> RevealWork.unlock/decrypt [worker]
 -> VaultEngine.reveal -> CryptoCore.decrypt -> transform -> derive -> same canonical
```

| Visible feature | What the student can demonstrate | Storage | Failure/recovery and security |
| --- | --- | --- | --- |
| Motion recording | Tilt phone, return near level, watch accepted count/orb; Done after4–8moves. | Samples/angles/tokens only in live arrays/fields. | Missing gyro gives accelerometer-only status; unavailable accelerometer switches pad checkbox, clears attempt and asks user to record again with pad. No stored biometric trace. |
| Double enrollment | First complete prompts exact repeat; matching order/count enables Forge. | Two session arrays, then enrolled byte[] temporarily. | Too short stays editable; mismatch resets both. Match proves token equality only, not user identity or motion-quality similarity. |
| Pad fallback | Two-column, three-row Left/Right/Forward/Back/Snap/Flat controls; touch, keyboard arrows and named accessibility actions. | Same live token values; default-pad preference is separate settings metadata. | <300ms repeat or session rejection produces no accepted-click feedback; no sensor angles inferred from pad. Known tokens can be replayed by either route. |
| Strength label | Incomplete-move count then at most10.34–20.68gesture bits and phrase advice. | Display only; no phrase-strength score. | Human choices can be weaker than uniform bound; phrase presence does not add fabricated bits. |
| Private replay | Opt-in matched moves shown one at a time with names/orb. | Temporary cloned token array; never a saved gesture. | Protected window/dialog; dismiss/pause/reset clears clone; cannot recover credentials after this enrollment ends. |
| Nerd Mode | Actual raw samples, gyro/fallback status, fused angles/formula and detected names. | Transient strings/8token display array; preference boolean only persists. | Pad explicitly says sensors inactive; tokens visible to anyone viewing screen. Nerd forces Hide/Reveal secure-window protection. |
| Pause/leave | Recording clears and sensors stop; Hide returns to secret preparation when needed. | No gesture/passphrase restored in saved state. | Re-enter secret/phrase and enroll again after Hide pause; Reveal re-records. Auto-lock setting controls previously revealed content, not retention of active recording credentials. |

An Android **secure window** (FLAG_SECURE) asks Android to restrict screenshots/capture; it does not stop someone photographing the display or a compromised system. Recording contents are not sent to the database/network by sensor/gesture. **SharedPreferences** is Android's private key/value settings store: VaultPreferences holds sensitivity/pad/nerd/privacy choices, not enrolled gestures. **Metadata** means operational settings/records rather than plaintext secrets.

## What the sensors measure and which axes the code uses

| Measurement | Meaning, units and limitation | Real source use |
| --- | --- | --- |
| Accelerometer values[0..2] | Device-axis acceleration in metres/second² (m/s²), including gravity contribution. A level face-up stationary phone is approximately[0,0,+9.80665]. Shaking/translation makes gravity-only orientation less reliable. | accelerometer stores raw acceleration, smooths gravity and derives angles; emit removes estimated gravity for SNAP/stillness. |
| Gyroscope values[0..2] | Angular velocity (rotation rate) around device X/Y/Z in radians/second. A radian is an angular unit;180degrees=πradians. Integration accumulates rate*time into angle, but small bias can drift over time. | gyroscope transforms body-axis rates into current pitch/roll rates before blending with gravity estimate. |
| Pitch | Angle in degrees; positive raises top=BACK, negative lowers top=FORWARD. | atan2(gravityY,gravityZ); fused orientation and sign-to-token mapping. |
| Roll | Angle in degrees; positive lowers right edge=RIGHT, negative lowers left edge=LEFT. | atan2(−gravityX,hypot(gravityY,gravityZ)). |
| Linear acceleration | Estimated device-axis acceleration after removing gravity, not the raw sample. | emit computes ax/ay/az passed to recognizer; SNAP uses az. |
| angularSpeed | Rotation-rate magnitude in rad/s, or estimated angle-speed fallback. | Stillness needs abs(angularSpeed)<=.15. |

Android device X runs across the screen, Y toward the top and Z out of the display; the app's portrait convention is explicitly documented in SensorFusionManager. It does not infer a gesture from screen auto-rotation. The app declares gesture screens portrait in its manifest (D1 map); the manager itself does not remap coordinates using display rotation. It estimates pitch/roll, not a full yaw/heading orientation. **atan2** obtains a signed angle from two components; **hypot** computes sqrt(a²+b²); **magnitude** is sqrt(x²+y²+z²).

## Complementary filter: idea, numbers and complete walkthrough

### Why combine two sensors?

A **complementary filter** combines short-term gyro prediction with correction toward the gravity-derived angle. The gyro follows quick rotation; accelerometer correction reduces long-term drift. Before angle estimation, a separate **low-pass gravity filter** smooths acceleration: it retains mostly the previous value and a little of each new sample. These two filters use different alphas and must not be confused.

**dt** (delta time) is elapsed seconds between two same-sensor timestamps. **Monotonic time** advances with elapsed device time rather than wall-clock dates; sensor timestamps are nanoseconds, converted to milliseconds for recognition. One second=1,000,000,000nanoseconds; one millisecond=1,000,000nanoseconds. Data missing for too long is not treated as continuously measured stillness.

### Numeric examples

Gravity smoothing with alpha_g=.9: previous gravity component10, raw new8 -> .9*10+.1*8=9.8. On initial/restart sample the code assigns raw value directly instead of smoothing from zero.

Angle blend with alpha=.98: previous theta10degrees; angular rate20degrees/s; dt.02s -> predicted10.4. Gravity angle8 -> difference−2.4; correction.02*(−2.4)=−.048; result10.352degrees. With alpha.90 the result would10.16 (stronger immediate gravity correction); alpha1 would keep10.4 and never correct drift; alpha0 would follow8. These alpha variations explain the effect, not available user settings: source FILTER_ALPHA is fixed.98.

Angles **wrap** around at ±180degrees; wrap here maps into[−180,180). If predicted179 and gravity−179, direct subtraction gives−358 but wrapped error is+2. Blend gives179+.02*2=179.04, taking the short direction. wrap(180)=−180; wrap(190)=−170.

Gravity-only example: raw stable x≈−4.9033,y0,z≈8.4928 (30degree right tilt under gravity9.80665) -> pitch0, roll≈+30degrees. At level emit subtracts g from rawZ≈9.80665 and obtains az≈0; a downward snap sample≈1.80665 gives linear az≈−8 while orientation remains approximately level.

### Exact formulas in sensor/SensorFusionManager.java

```text
gravity_i = restart ? raw_i : .9*gravity_i + .1*raw_i
accelPitch = degrees(atan2(gravity_y, gravity_z))
accelRoll  = degrees(atan2(-gravity_x, hypot(gravity_y, gravity_z)))

p = radians(current pitch); r = radians(current roll)
pitchRate = gyroX + sin(p)*tan(r)*gyroY + cos(p)*tan(r)*gyroZ
rollRate  = cos(p)*gyroY - sin(p)*gyroZ

predicted = wrap(theta + degrees(rate)*dt)
newTheta = wrap(predicted + (1-.98)*wrap(accelAngle-predicted))
wrap(a) = a+180 - floor((a+180)/360)*360 -180
```

These pitchRate/rollRate equations transform body-axis rotation into the current angle coordinate system. An **Euler singularity** is an orientation where this coordinate representation becomes unstable; tan(roll) grows near±90degrees. The code switches to accelerometer angles when abs(cos(roll))<.15 instead of continuing unstable rate conversion. It is not a generic quaternion/rotation-vector filter; neither such data type is present here.

### Actual control flow

1. start must run on main. It calls stop to reset old registration/state, reads in-memory sensitivity, registers accelerometer at requested20,000µs with the main Handler; if registration fails, returns UNAVAILABLE. If gyro registration succeeds mode=GYROSCOPE_FUSION, otherwise ACCELEROMETER_ONLY. running=true only after accelerometer succeeds.
2. onSensorChanged ignores stopped state, fewer than3values, negative timestamp or any non-finite first3values. It dispatches by sensor type; gyro raw vector is copied before gyroscope's chronological/integration checks.
3. accelerometer ignores timestamp<=lastAccelNanos, computes dt and restart when !initialized or dt>.25. Store raw vector and smoothed/raw gravity. If gravity magnitude<1m/s², return without angle initialization/emission. Otherwise compute accelerometer angles.
4. Gyro is considered fresh only in fusion mode, with lastGyroNanos>=0 and elapsed-from-last-gyro<=.25s. If restarting or not fresh, useAccelerometer estimates angular speed from wrapped angle change/dt (zero on restart/nonpositive dt), then replaces pitch/roll with gravity angles.
5. Set initialized=true and emit at the accelerometer timestamp. Gyro callbacks change the retained fused angles but do not directly emit recognizer events.
6. gyroscope ignores uninitialized or non-increasing timestamp. Compute dt, store timestamp and raw-rate magnitude. First gyro interval uses seconds(...,−1)=0; invalid/nonpositive/>.25 interval or near-singularity roll resets fused angles to accelerometer estimates and returns.
7. Otherwise transform gyro rates and blend pitch/roll with gravity angles using the wrapped formula. FILTER_ALPHA applies once per processed gyro update; it is not recalculated from dt. Faster/slower actual sensor delivery changes effective correction rate;20,000µs is a request, not guaranteed frequency.
8. emit converts fused angles to radians and subtracts predicted gravity using g9.80665:

```text
linearX = rawAccelX + g*sin(roll)
linearY = rawAccelY - g*sin(pitch)*cos(roll)
linearZ = rawAccelZ - g*cos(pitch)*cos(roll)
```

9. If telemetry callback exists, send raw acceleration vector, latest stored gyro vector, fused angles and mode; then recognizer.update(timestampNanos/1_000_000, pitch,roll,linearX,Y,Z,angularSpeed). Nerd samples are raw acceleration, not the linear vector sent to recognition.
10. stop requires main, sets running false, unregisters listener, invalidates recognizer and zeroes all motion arrays/angles/timestamps/initialization/mode. No samples are persisted. resetGesture resets recognition alone, not fusion history/registration.

**Staleness detail:** dt is maintained separately per sensor. Freshness does not mean full cross-sensor timestamp synchronization; the source uses latest retained values. A raw gyro vector can be copied even if its integration is ignored, so telemetry is educational, not a calibrated sensor recorder. onAccuracyChanged is intentionally empty. No dedicated calibration/bias-estimation algorithm is implemented.

### Every SensorFusionManager method and callback

| Method / real parameter names | Return / plain-English job | Called by → calls | Edge/failure handling |
| --- | --- | --- | --- |
| SensorFusionManager(Context context,GestureRecognizer.Listener listener) | New manager connected to application SensorManager and recognizer. | GestureStep/RevealGesture constructors → getApplicationContext/getSystemService, new GestureRecognizer with executor lambda. | Null SensorManager handled later by start; listener/executor require non-null through recognizer. Lambda runs command immediately on main, otherwise main.post. |
| setTelemetryListener(TelemetryListener listener) | void; replace optional telemetry callback. | Controller constructor → field assignment. | null disables telemetry; no disk/thread scheduling. |
| start() | Mode describing registered capability. | GestureStep.startSensors / RevealGesture.begin → requireMainThread,stop,setSensitivity,getDefaultSensor,registerListener. | No accelerometer/service/failed registration -> UNAVAILABLE; no gyro/failed registration -> fallback. Registration exceptions are not caught inside this method; do not claim every possible platform exception gets a pad recovery. |
| stop() | void; unregister/reset all state. | start, controllers on completion/reset/pause → requireMainThread,unregisterListener,recognizer.reset,Arrays.fill. | Safe when no sensor service or already stopped; throws if called off main. |
| resetGesture() | void; clear recognizer history only. | Public between-performance helper → requireMainThread,recognizer.reset. | Current controllers complete by stop/start rather than relying on this helper. |
| onSensorChanged(SensorEvent event) | void; validate and dispatch accelerometer/gyro. | Android → accelerometer/gyroscope,System.arraycopy. | Invalid/nonfinite/negative samples ignored; event assumed non-null Android callback. |
| accelerometer(SensorEvent event) | void; gravity/angles/fallback, then recognizer emission. | onSensorChanged → seconds,magnitude,degrees,useAccelerometer,emit. | Non-increasing time/low gravity ignored; long gap restarts. |
| useAccelerometer(float dt,boolean restart) | void; set angles to gravity estimate and estimate angularSpeed. | accelerometer → wrap,Math.hypot/toRadians. | restart or dt<=0 means speed0 rather than dividing by zero. |
| gyroscope(SensorEvent event) | void; update transformed gyro fusion. | onSensorChanged → seconds,magnitude,degrees,blend,Math trig. | No initialization/stale timestamp ignored; invalid interval/singularity resets to accel angles; no emit here. |
| blend(float theta,float omega,float dt,float accelAngle) | float new angle in degrees. | gyroscope twice → wrap. | omega already degrees/s; wrapped correction avoids±180 seam. Private callers guard dt. |
| emit(long timestampNanos) | void; remove gravity and send telemetry/recognition. | accelerometer → telemetry.onSample if present,recognizer.update. | Uses latest retained values; converts ns to integer ms by division. Callback exceptions are not swallowed. |
| magnitude(float[] values) | float vector length. | accelerometer,gyroscope → Math.sqrt. | Private callers supply>=3valid values; no own bounds check. |
| seconds(long now,long before) | float seconds;0if before<0. | accelerometer,gyro/freshness → arithmetic. | Difference widened by long timestamps then divided by1e9f. Does not itself reject negative difference. |
| degrees(double radians) | float degrees. | accelerometer/gyro → Math.toDegrees. | Unit conversion only. |
| wrap(float degrees) | float angle in[−180,180). | blend/fallback → Math.floor. | Internal finite angle assumptions; no stored state. |
| requireMainThread() | void; assert lifecycle owner. | start/stop/resetGesture → Looper.myLooper/getMainLooper. | IllegalStateException if not main; does not post automatically. |
| onAccuracyChanged(Sensor sensor,int accuracy) | void; intentionally no action. | Android. | Accuracy changes do not alter sensitivity/filter/calibration. |
| TelemetryListener.onSample(float ax,float ay,float az,float gx,float gy,float gz,float pitch,float roll,Mode mode) | void callback with raw samples/fused orientation. | emit → supplied NerdOverlay implementation. | No persistence or lifetime ownership in interface. |

## From angles to tokens: thresholds, latches and timing

**Hysteresis** uses a higher entry boundary and lower rearm boundary so noisy readings do not repeatedly trigger. A **latch** remembers that a pose already emitted its token; it must be released before that kind repeats. **Stillness** here is a thresholded estimate, not proof of no motion. A **generation** number invalidates already queued callbacks after reset. Token values are bytes0..5; −1is the private result meaning no token.

| Token | ID | Recognition condition in Normal mode |
| --- | --- | --- |
| LEFT | 0 | Selected roll is negative and abs(roll)>=25, tilt not latched. |
| RIGHT | 1 | Selected roll is positive and abs(roll)>=25, tilt not latched. |
| FORWARD | 2 | Selected pitch is negative and abs(pitch)>=25, tilt not latched. |
| BACK | 3 | Selected pitch is positive and abs(pitch)>=25, tilt not latched. |
| SNAP | 4 | Face-up-enough downward linear Z pulse<=−7 then recovery>=−1.5 within220ms, with tilt<=20. |
| FLAT | 5 | Both angles<=8in magnitude and still continuously>=500ms, not already flat-latched. |

For tilts, choose larger magnitude angle; when abs(roll)==abs(pitch), roll wins. Return **both** abs(pitch) and abs(roll)<=exit (Normal12) to release tilt latch; lowering only the previously selected axis is not enough if other axis remains outside exit. Gap since last emitted token must>=300ms; equality300is allowed. Entry is inclusive>=25, exit inclusive<=12, FLAT inclusive<=8, SNAP tilt rejects only>20.

### Exact recognizer sequence

1. update rejects time<0,time<=lastSample,or any nonfinite pitch/roll/linear acceleration/angularSpeed. Negative token sentinel does not represent an accepted gesture move.
2. If intersample gap>.25*1000=250ms, reset flatSince/snapSince and set snapRejected=true. A recovery acceleration sample is needed before a new SNAP pulse can arm. This gap does not clear tiltLatched/flatLatched or lastMove.
3. Compute still=(sqrt(ax²+ay²+az²)<=.8) AND(abs(angularSpeed)<=.15rad/s).
4. recognize computes neutral and clears tilt latch if both within exit. It calls detectSnap and detectFlat **before** the300ms gap check, so pose state can change even while token emission is inhibited.
5. If eligible gap: SNAP has first priority, then newly unlatched threshold tilt, then newly unlatched FLAT. Tilt latch is set only when tilt token emitted; flat latch when FLAT emitted. Any emitted token updates lastMove.
6. update captures generation stamp and asks callbackExecutor to deliver. deliver ignores obsolete stamp, sends onToken if>=0, then sends onMotion only if generation still unchanged. Token callback may complete/stop/reset recognition; the second generation check prevents a stale motion callback after that reset.

### SNAP walkthrough and numeric pulse

A **pulse** is a short departure and return in acceleration. Start near face-up with pitch/roll within20. At time600ms linearZ−8 starts pulse; at750linearZ−1 returns above−1.5, duration150<=220, so detectSnap true. It emits only if300ms elapsed since last token. The pulse is accepted on recovery, not at initial downward spike. This avoids treating a long drop/continued acceleration as repeated snaps.

At tilt>20: snapSince=−1,snapRejected=true. At recovery>=−1.5: pulse result uses current snapSince/rejection/duration, then clears snapSince and re-arms snapRejected=false. Below recovery: an unarmed/not rejected pulse starts when az<=−7; if duration>220, mark rejected. A failed/long/tilted pulse must recover before another valid start. Starting threshold equality−7, recovery equality−1.5and duration220are accepted. A valid pulse suppressed by the global move gap is consumed, not queued for later replay.

### FLAT walkthrough and the still-finish distinction

With abs(pitch),abs(roll)<=8and still, first sample sets flatSince; after500ms emits FLAT if gap permits and not flatLatched. Leaving level (>8on either axis) releases flatLatched and restarts hold. Acceleration noise that breaks stillness resets flatSince but does **not** release an already emitted flat latch while still level. This prevents repeated FLAT from a tiny tremor.

Example level/still begins800ms, continuous samples to1300 ->FLAT at1300. Remaining level/still does not emit repeated FLAT. This is separate from GestureSession's1500ms still completion after at least4moves. A flat pose can itself add a token before completion; auto-finish is not a secret alternative with no possible FLAT move. Pad never emits motion samples, so pad completion requires Done.

### Worked complete sequence: RIGHT, LEFT, SNAP, FLAT

Normal sensitivity, starting fresh. The table selects important samples; assume intermediate finite samples no more than200ms apart, so no missing-data restart occurs. Listed linear acceleration is gravity-removed; unspecified components and angularSpeed are0. Interpolating/transitional samples must not introduce other threshold events.

| Time ms | Relevant sample | Recognizer/session result |
| --- | --- | --- |
| 0 | pitch0,roll0,linearZ0,still | Starts level hold; no token yet. |
| 100 | pitch3,roll26 | RIGHT1 accepted; tilt latched, flat hold broken. |
| 200 | pitch3,roll28 | No duplicate RIGHT; latch still set (also gap<300). |
| 300 | pitch4,roll10 | Both within12 ->tilt rearmed; roll10not within8so no FLAT hold. |
| 350 | pitch2,roll−26 | Only250since token; no token, no new tilt latch set by this rejected tilt. |
| 400 | pitch2,roll−26 |300since100 ->LEFT0 accepted and latched. |
| 500 | pitch0,roll0,linearZ0 | Neutral rearms tilt; SNAP recovery clears prior tilt rejection; flat hold starts. |
| 600 | pitch0,roll0,linearZ−8 | SNAP pulse begins; not still, so flat hold resets. |
| 750 | pitch0,roll0,linearZ−1 | Recovery after150ms ->SNAP4;350since last token, accepted. Magnitude1>.8is not still. |
| 800 | angles0,linear acceleration0 | Still/flat hold begins. |
| 1000,1200 | Continued level/still | Hold200/400ms; no FLAT yet. |
| 1300 | Continued level/still |500ms hold ->FLAT5,550after SNAP; fourth token. Session still timer starts on same-time onMotion. |
| 1500..2800 | Continued level/still with gaps<=200ms | No duplicate FLAT. At2800,1500ms continuous stillness since1300 ->session.done ->completed. |

Final tokens[1,0,4,5]. Done could also explicitly complete once the count reaches4. Different timestamps/angles in a later performance are acceptable if they produce this exact order/count. A sample gap>250during the final still interval restarts the session still timer; silence is never counted as a measured hold.

### Every GestureRecognizer method and callback

| Method / parameter names | Return / job | Called by → calls | Edge/failure handling |
| --- | --- | --- | --- |
| GestureRecognizer(Executor mainThreadExecutor,Listener listener) | New pure recognizer with callback executor. | SensorFusionManager constructor → Objects.requireNonNull twice. | Null dependencies ->NullPointerException; pure API requires serialized update/reset and callback execution. |
| setSensitivity(int sensitivity) | void; updates entry/exit then resets recognition. | SensorFusionManager.start → Constants.tiltEnterDegrees/tiltExitDegrees,reset. |0relaxed,2strict,others normal. Does not change token IDs/key encoding. |
| update(long time,float pitch,float roll,float ax,float ay,float az,float angularSpeed) | void; process one sample and deliver callback. | SensorFusionManager.emit → finite,recognize,callbackExecutor.execute(lambda). | Time monotonic ms,angles degrees,linear acceleration m/s²; invalid samples ignored. Missing-data behavior above. |
| deliver(long stamp,byte token,long time,float pitch,float roll,boolean still) | void; current-generation callback dispatch. | Executor lambda from update → listener.onToken/onMotion. | Obsolete stamps discarded; generation rechecked between callbacks. Exceptions propagate from callback. |
| recognize(long time,float pitch,float roll,float az,boolean still) | byte0..5or−1. | update → detectSnap,detectFlat,Math abs/max. | Neutral rearm/state-before-gap/priority rules above; no own sample validation. |
| detectFlat(long time,float pitch,float roll,boolean still) | boolean valid elapsed level hold. | recognize. | Leaves level rearm; not still resets timer; already flat-latched status checked by recognize. |
| detectSnap(long time,float pitch,float roll,float az) | boolean accepted recovered pulse. | recognize. | Tilt,minimum spike,recovery,duration/rejection conditions above; not a velocity/height estimator. |
| finite(float... values) | boolean all Float.isFinite. | update → Float.isFinite. | Varargs means variable number of float arguments represented as an array. |
| reset() | void; increment generation and reset all times/latches. | SensorFusionManager.stop/resetGesture/setSensitivity. | Times−1; all latches false. queued delivery invalidated; no callback generated by reset itself. |
| Listener.onToken(byte token,long timestampMillis) | void event. | deliver → controller session.addToken (plus overlay). | Host may reject token/full session; recognizer's emitted lastMove is still advanced. |
| Listener.onMotion(float pitchDegrees,float rollDegrees,boolean still,long timestampMillis) | void event for orb/session finish. | deliver → host orb/session.onMotion. | May be skipped after token-triggered reset; no raw vector in this callback. |

## GestureSession: one recording, completion and array ownership

The session is a small state machine (stored flags/times that determine which actions are valid). It accepts tokens from either recognizer or pad and owns an8-byte buffer initialized to zero. Zero is also LEFT, so count is essential: unused zeros must not be mistaken for extra LEFT moves. It is intended for use on one thread; it has no worker or synchronization around ordinary methods.

### Accepting moves and deciding when recording is done

1. addToken returns false if closed,finished or count==8. These guards occur before invalid-token/time checks.
2. Otherwise token must0..5and timestamp>=0; invalid event throws IllegalArgumentException. Refuse timestamp<lastMotion or less than300since last accepted token. Do not invent a wall-clock timestamp: pad uses elapsedRealtime and motion uses sensor elapsed timestamps.
3. Store token at tokens[count],increment count,set lastMove,reset stillSince,call listener.onProgress(count),return true.
4. onMotion ignores closed/finished/negative or non-increasing motion timestamp, or timestamp<lastMove. It allows equality with lastMove because recognizer sends token before same-sample motion.
5. Detect a gap>250ms; store lastMotion. If moving,gap or count<4,reset stillSince. If still and count>=4,set timer to current timestamp when missing; once elapsed>=1500call done.
6. done returns false if closed; true immediately if already finished. If count<4,call onTooShort(count) and leave recording editable. Otherwise set finished=true,clear still timer,call onFinished and return true.
7. count8does not automatically finish. No more tokens can be added, but Done or enough continuous motion stillness can finish. Pad must use Done.

Worked session example: attempts at100,400,700ms give3tokens; Done at that point calls onTooShort(3),not finish. A fourth valid token at1000allows Done immediately. Alternatively still samples1000,1200,1400,...2500with no gap>250complete1500ms later. If next sample after1000is1400,a400ms gap resets the timer to1400instead of counting the missing interval. Motion markednot-still similarly restarts it. The timer records actual sample continuity, not a Handler timeout.

### Every GestureSession method and callback

| Method / parameters | Return / plain-English job | Called by → calls | Edge/failure/cleanup |
| --- | --- | --- | --- |
| GestureSession(Listener listener) | New empty session. | GestureStep.begin/RevealGesture.begin → Objects.requireNonNull. | Null listener rejected; tokens[8] zero initialized; times−1. |
| addToken(byte token,long timestampMillis) | boolean accepted move. | Recognizer/pad controller callbacks → listener.onProgress. | Closed/finished/full/too-close/out-of-order false; invalid token/negative time throws after state guards. Only acceptance mutates count. |
| onMotion(long timestampMillis,boolean still) | void; continuity/auto-finish. | Controller recognizer listener → done when1500ms continuous still after>=4tokens. | Invalid/repeated/reversed time ignored; gap>250or movement resets still timer; missing data is not stillness. |
| done() | boolean completed now/already completed. | Done button or onMotion → listener.onTooShort/onFinished. | Too short false but editable; closed false; already finished true without repeating callback. |
| size() | int count. | Public status helper. | After clear/close count0. |
| isFinished() | boolean finished&&!closed. | matches/takeTokens and public status. | Closed sessions never considered finished. |
| matches(GestureSession first,GestureSession second) | boolean equal completed recordings. | GestureStep.finishAttempt → isFinished on both,XOR/OR across8slots. | Reject null,same object,incomplete/closed; compares count and exact tokens. Does not compare timing or orientation. |
| takeTokens() | New byte[count],then closes source. | GestureStep.finishAttempt,RevealGesture.onFinished → Arrays.copyOf,close. | Requires isFinished or IllegalStateException; source token buffer wiped; caller owns returned copy. |
| reset() | void; wipe/open session state and progress0. | Public active-session reset helper → clear,listener.onProgress(0). | Closed session cannot reopen; throws IllegalStateException. Current controllers normally close/recreate instead. |
| clear() | void; fill tokens0,zero count/finished,times−1. | reset/close → Arrays.fill. | Does not set closed false; cannot reopen closed session. |
| close() | void; clear and set closed=true. | Controller cleanup,takeTokens,AutoCloseable owner → clear. | Idempotent clear; no callback or disk persistence. |
| Listener.onProgress(int tokenCount) | void accepted-count callback. | addToken/reset → controller label/orb/strength. | May run inline before addToken returns. |
| Listener.onFinished() | void completion callback. | done → controller finishAttempt or Reveal token transfer. | May close/reset session/sensors inline; callers must tolerate changed ownership. |
| Listener.onTooShort(int tokenCount) | void failure callback. | done with count<4 → user notice. | Does not wipe or abandon editable attempt. |

## Enrollment: compare two distinct completed runs

### Idea and worked comparison

Enrollment confirms that the user can repeat the exact sequence of named tokens. It is not dynamic time warping, template matching of raw sensor traces, a trained classifier or a biometric signature. Two recordings may have different angles/timestamps while yielding the same discrete credential.

Example first[1,0,4,5],second[1,0,4,5],bothcount4: initial difference=4 XOR4=0. Compare all8slots, padding zeros in unused positions; every XOR0,so final0and match. Different second[1,1,4,5]: position1XOR is0 XOR1=1; OR accumulates1; final!=0 ->mismatch. A5move second ending LEFT0with otherwise identical first4must still fail: count4 XOR5=1,even though unused first padding is0. This is why comparing padded buffers alone would be wrong.

**XOR** (^) produces1where bits differ; bitwise **OR** combines any difference without cancelling it. The loop does not return early per differing token, but eligibility checks return early and Java execution is not promised constant-time by this code. Do not claim it is a timing-proof password comparison.

Real core comparison in gesture/GestureSession.java — matches:

```java
int difference = first.count ^ second.count;
for (int i = 0; i < Constants.MAX_TOKENS; i++) {
    difference |= first.tokens[i] ^ second.tokens[i];
}
return difference == 0;
```

### Exact controller state transition

GestureStep.begin refuses starting while a current attempt or enrolled sequence already exists. First session completion calls finishAttempt: stop sensors/overlay/orb; store first=current,current=null; ask for second run. On second completion, matches(first,current) must succeed. Mismatch calls reset,which closes both and clears enrolled data,then shows mismatch status/notice. Success first.takeTokens returns sized copy and closes first; current.close wipes second; both references cleared; enrolled holds only copy. Forge and private replay become enabled.

GestureStep.takeTokens clears replay, transfers enrolled **without copying again**, sets fieldnull and disables Forge. HideActivity.forge calls it before showStep(3), so reset while changing step does not wipe transferred result. HideWork.forge owns submitted tokens/phrase and fills both in finally after work/cancellation. No credential is stored for future recovery. RevealGesture instead captures one complete performance and enables Unlock; no second enrollment is needed to try decrypting an existing image.

Switching between pad/motion resets the current enrollment. The default pad preference only selects an initial input mode. Matching uses token semantics, not the input hardware source, so a known sequence can be reproduced on pad. Sensitivity affects which physical samples cross boundaries but does not change existing token encoding.

### GestureStep: supporting class and all motion methods

Purpose: orchestrate two-recording Hide enrollment and its visible state. HideActivity constructs it with SecretStep for phrase-presence advice. Fields include host/secret,orb/pad/checkbox/status/strength/buttons,sensors/nerd,replay,first/current sessions,enrolled[].

| Method / parameters | Return / work | Caller → calls | Edge/recovery/ownership |
| --- | --- | --- | --- |
| GestureStep(HideActivity host,SecretStep secret) | New enrollment controller. | HideActivity.onCreate → findViewById,GestureReplay,NerdOverlay,SensorFusionManager,bind,preference.pad. | Anonymous recognizer listener sends proposed token to overlay then session; onMotion updates orb/session. Default pad checkbox may reset state through listener. |
| bind() | void; attach pad/toggle/record/done/forge/replay/reset callbacks. | Constructor → setOnTokenListener,setOnCheckedChangeListener,setOnClickListener,reset. | No current session ->pad callback false. Toggle resets both attempts. Done only if session exists. |
| begin() | void; start new active recording and labels. | Record click → nerd.begin,orb.reset/setAnimating,StrengthMeter.label,new GestureSession,startSensors if motion. | current/enrolled non-null ->no-op; pads enabled only during capture; first retained while second created. |
| startSensors() | void; start sensing and explain mode. | begin → sensors.start,usePad.setChecked/host.message. | UNAVAILABLE changes checkboxtrue (reset handler clears attempt),so user begins again; accelerometer-only notice retains attempt. |
| finishAttempt() | void; finish first or compare second. | Session.onFinished → stop/clear visual state,GestureSession.matches,takeTokens/close,reset on mismatch. | Match enables Forge/replay; mismatch resets both; no secret persistence. |
| takeTokens() | byte[]enrolled ornull,ownership transfer. | HideActivity.forge → replay.clear. | Fieldnull after transfer; disable Forge; caller must wipe. |
| reset() | void; stop sensors/visuals,wipe sessions/enrolled,restore ready UI. | Constructor,toggle/reset click,step change,host.onPause → replay.clear,sensors.stop,nerd.clear,orb.stop/reset,sessions.close,Arrays.fill,StrengthMeter.label. | Wipes retained first/current/enrolled; controls reset; null-safe ownership checks. |

### RevealGesture: supporting class and all recording methods

Purpose: collect one in-memory credential attempt for Reveal. RevealActivity constructs it. Fields host/sensors/nerd,pad/checkbox/status,session,tokens[] own only current attempt.

| Method / parameters | Return / work | Caller → calls | Edge/recovery/ownership |
| --- | --- | --- | --- |
| RevealGesture(RevealActivity host) | New single-run controller. | RevealActivity.onCreate → findViewById,NerdOverlay,SensorFusionManager,gesture/pad/UI callbacks,clear,default pad preference. | Recognizer token callback checks session; motion callback only session.onMotion (Reveal has door instead of live orb). |
| begin() | void; clears old attempt,starts fresh session/optional sensors. | Record/restart click → clear,nerd.begin,new GestureSession,sensors.start. | Too-short notice; unavailable motion toggles pad/clears and asks retry; fallback warning. onFinished stops/clears telemetry,takeTokens/closes session,disables pad/Done,enables Unlock. |
| takeTokens() | Existing byte[]or null; no copy. | RevealActivity.unlock → field assignmentnull. | Ownership leaves controller and passes to decryption worker. |
| clear() | void; stop sensors and wipe session/completed tokens,disable unlock. | begin,toggle,host.lock/onPause/onDestroy,unlock cleanup → sensors.stop,nerd.clear,session.close,Arrays.fill. | Completed local copy wiped unless already transferred; default prompt restored. |

## Gesture Pad: six Canvas controls, one token contract

### Layout, numeric size example and drawing

GesturePadView is a custom View, not six ordinary Android Buttons. Layout order is two columns: LEFT/RIGHT, FORWARD/BACK, SNAP/FLAT. TOKENS maps cell index to permanent byte, LABELS loads human-readable strings, ACTIONS maps six accessibility IDs from res/values/gesture_ids.xml. UI IDs are unrelated integer resources, not byte values used for encryption.

Resource values from res/values/dimens.xml:gap8dp,radius16dp,stroke1dp,text16sp,touchMin48dp,preferred width280dp. Constructor minimum width2*48+3*8=120dp; minimum height3*48+4*8=176dp. Dimensions are resolved into display pixels by resources; do not mix raw dp numbers with pixel touch coordinates. The fixed XML pad heights are216dp Hide,280dp Reveal; both satisfy minimum under ordinary resource scale, while parent constraints can still force a smaller View.

For width280dp,height216dp,no padding,cell width(280−24)/2=128dp; height(216−32)/3≈61.33dp. hit only accepts rectangles with actual width/height>=48dp-equivalent. A forced total width100dp gives(100−24)/2=38dp cells; hit refuses these rather than counting an undersized target.

onMeasure chooses preferred/minimum width and minimum height plus padding,subject to parent measurement constraints. onSizeChanged constructs each cell from row/column,gaps/padding,clamps negative available width/height to0. onDraw loops drawCell: fill pressed/normal surface,stroke focused-selected/accent or outline,enabled/disabled text colour,centered label. Text scales down only if measured string exceeds available width. Paint.ANTI_ALIAS_FLAG smooths edges; round rectangles are visual styling,not recognition thresholds.

### Touch-to-token algorithm with worked presses

1. Disabled view rejects touch. ACTION_DOWN hit-tests; outside/no48dp cell returnsfalse; inside sets pressed index,tracks first pointer ID,selects/requestFocus.
2. ACTION_MOVE finds that pointer; moving outside original cell or missing pointer clears press. Adding another pointer or ACTION_CANCEL also cancels. These are touch events,not phone motion samples.
3. ACTION_UP accepts only same recorded pointer and same hit cell; clears press; calls performClick if valid. It requests redraw after event handling.
4. performClick rejects disabled state; gets SystemClock.elapsedRealtime (monotonic ms); requires>=300since last accepted pad click. Calls listener.onToken(TOKENS[selected],now); only if true updates lastMove,haptics,super.performClick and returnstrue.
5. Example RIGHT tapped at1000ms,session accepts ->token1and haptic. Another at1200 ->rejected,no session call from pad. At1300 ->gap300allowed; if session is full/finished,listener false,so no lastMove update/haptic. Key/accessibility activation uses this exact performClick path too.

A rejected tap does not become a delayed token. Pad has its own300ms guard and GestureSession separately enforces the same gap. The pad's lastMove survives session reset until view detach (only accepted clicks update it); immediately restarting within300ms can therefore suppress an early new click. Hosts disable pad outside recording. No onMotion events means no FLAT pose detection or auto-finish on pad: pressing Flat directly emits5,then user chooses Done after enough moves.

### Keyboard and assistive actions

onKeyDown handles DPAD_LEFT/RIGHT selecting column and UP/DOWN stepping two cells with boundary clamps. Other keys delegate to parent View behavior; the code does not explicitly special-case Enter inside onKeyDown. The content description instructs arrow keys then Enter; inherited clickable View activation reaches overridden performClick. Six accessibility actions are installed on one node,not six independently drawn virtual-child nodes. PadAccessibility.onInitializeAccessibilityNodeInfo adds named actions; performAccessibilityAction finds matching ACTIONS entry,sets selection,invalidates and calls performClick; unknown actions call parent.

### Every GesturePadView method and nested callback

| Method / parameters | Return / work | Caller → calls | Edge/failure/ownership |
| --- | --- | --- | --- |
| GesturePadView(Context context) | New view via(context,null). | Java caller → constructor overload. | Common setup ultimately3argument constructor. |
| GesturePadView(Context context,AttributeSet attrs) | New view via(context,attrs,0). | Android XML inflation / Java →3argument constructor. | AttributeSet is Android XML attributes,passed to View parent. |
| GesturePadView(Context context,AttributeSet attrs,int defStyleAttr) | Initialize all rectangles/labels/dimensions/accessibility. | Other constructors → super,dimension,getString,setMinimumWidth/Height,focus/click/content description,setAccessibilityDelegate. | Creates PadAccessibility inner object; no token session created here. |
| setOnTokenListener(OnTokenListener listener) | void; store callback. | Controllers → assignment. | null means clicks rejected; no caller ownership/wiping of token buffer because pad stores no sequence. |
| onMeasure(int widthSpec,int heightSpec) | void; choose measured dimensions. | Android layout → dimension,resolveSize,setMeasuredDimension. | Parent may constrain below preferred/minimum; hit guards actual target size. |
| onSizeChanged(int w,int h,int oldw,int oldh) | void; populate2x3cell geometry. | Android size change → padding accessors,RectF.set,Math.max. | Negative usable sizes become0; gaps/padding still applied. |
| onDraw(Canvas canvas) | void; render six cells. | Android draw → super.onDraw,drawCell. | No sensor/crypto/disk work. |
| drawCell(Canvas canvas,int index) | void; style background,focus,border,label. | onDraw → Paint,Canvas,getColor,getString-loaded label,measureText. | Private valid index; disabled labels muted; shrinking text avoids overflow. |
| onTouchEvent(MotionEvent event) | boolean touch consumed. | Android → hit,clearPress,requestFocus,performClick,invalidate. | Handles pointer cancel/move/outside behavior above; disabled/down outside false. |
| performClick() | boolean token accepted. | Touch,inherited click/keyboard,PadAccessibility → elapsedRealtime,OnTokenListener.onToken,haptic,super.performClick. | No listener/too fast/disabled/session rejection false; timestamp updated only on success. |
| onKeyDown(int keyCode,KeyEvent event) | boolean handled DPAD key,otherwise parent. | Android key dispatch → setContentDescription,invalidate,super.onKeyDown. | Clamps row selection; disabled delegates. |
| hit(float x,float y) | index0..5or−1. | onTouchEvent → RectF.width/height/contains. | Requires both actual dimensions>=touchMin. |
| clearPress() | void; pressed=pointer=−1. | Touch/detach. | Selection remains for keyboard/accessibility. |
| dimension(int resource) | float actual pixel dimension. | Constructors/onMeasure → getResources.getDimension. | Resource IDs from dimens; text dimension also resolved by resources. |
| onDetachedFromWindow() | void; clear press and lastMove=−1. | Android detach → clearPress,super. | Does not close controller session itself; host owns that lifecycle. |
| OnTokenListener.onToken(byte token,long timestampMillis) | boolean whether current session accepted. | performClick → host lambda→session.addToken. | Host may inspect/display candidate before session acceptance. |
| PadAccessibility.onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info) | void; append6named actions. | Android accessibility → super,new AccessibilityAction,info.addAction. | Uses resource action IDs and localized labels. |
| PadAccessibility.performAccessibilityAction(View host,int action,Bundle args) | boolean common activation result or parent result. | Android assistive dispatch → invalidate,performClick,super fallback. | Disabled/gap/session restrictions remain in force. |

## Strength meter: exact maths and honest interpretation

**Entropy** measures unpredictability in bits. If n independent moves are each uniformly chosen from6possible tokens,there are6^n sequences and log2(6^n)=n*log2(6)bits. **log2** means base2logarithm; source uses Math.log(6.0)/Math.log(2.0) because Math.log is natural logarithm. This is the contribution of tokens only,not a measured estimate of a human's choices or phrase.

| Token count n | Number of possible fixed-length uniform sequences6^n | Maximum bits (rounded2dp) | UI meaning |
| --- | --- | --- | --- |
| 0 | 1 | 0.00 |4more moves needed. |
| 1 | 6 | 2.58 |3more needed. |
| 2 | 36 | 5.17 |2more needed. |
| 3 | 216 | 7.75 |1more needed. |
| 4 | 1,296 | 10.34 |Minimum completed credential length. |
| 5 | 7,776 | 12.92 |Still low contribution without strong phrase. |
| 6 | 46,656 | 15.51 |Same uniform upper-bound qualification. |
| 7 | 279,936 | 18.09 |Same qualification. |
| 8 | 1,679,616 | 20.68 |Maximum supported token length. |

Worked n4: log2(1296)≈10.33985. n8doubles that to20.6797,not256. Performing enrollment twice does not double strength: the second run confirms the same sequence,not an independent second key. Timings/raw sensor noise do not add hidden bits because they are not part of canonical credentials. Adding a phrase changes the credential but the meter does not estimate or add its entropy.

Walkthrough: GestureSession.onProgress -> GestureStep updates StrengthMeter.label(host,count,secret.hasPassphrase()). label calls bits -> GestureCredential.entropyBits. count<4 uses gesture_strength_incomplete with upperBound and4−count; otherwise chooses with_passphrase or add_passphrase resource. Existing source strings explicitly qualify uniform upper bound/human weakness and recommend a phrase. A stored settings default does not constitute a phrase-strength estimator.

| Method / parameters | Return / work | Caller → calls | Edge/failure |
| --- | --- | --- | --- |
| StrengthMeter.bits(int tokenCount) | double upper bound. | label/public caller → GestureCredential.entropyBits. | Delegates range check0..8; throws outside. |
| StrengthMeter.label(Context context,int tokenCount,boolean hasPassphrase) | String appropriate status/advice. | GestureStep.begin/progress/reset → bits,context.getString. | Below4incomplete; otherwise phrase-present/advice; no phrase characters received or persisted. |
| GestureCredential.entropyBits(int tokenCount) | double n*ln6/ln2. | StrengthMeter.bits → Math.log. | Rejects<0/>8,allows0for live meter. |

## Every motion/gesture threshold in core/Constants.java

These values are exact source constants,not independently measured optima. Alpha is a smoothing/blending weight,not opacity here. A **microsecond** (µs) is10^-6seconds; milliseconds(ms)10^-3seconds. Float suffixf means32-bit floating literal; timing constants use long where declared. Entry/hold/gap inequalities are important at exact boundaries.

| Exact name | Value | Used by / precise meaning and rationale |
| --- | --- | --- |
| FILTER_ALPHA | .98f | SensorFusionManager.blend keeps98%gyro prediction/2%wrapped accelerometer correction per update. Larger trusts gyro longer; smaller corrects drift more quickly but follows acceleration noise. |
| GRAVITY_FILTER_ALPHA | .9f | accelerometer smooths gravity90%previous/10%new except restart; reduces short spikes in orientation. Not the same alpha as .98. |
| TILT_ENTER_DEGREES |25f | Default entry; largest angle magnitude>=25emits unlatched tilt after gap. Lower entry easier,more accidental sensitivity. |
| TILT_EXIT_DEGREES |12f | Default rearm; both magnitudes<=12release tilt latch. Must remain below entry for hysteresis. |
| tiltEnterDegrees(sensitivity) |0→20f,2→30f,otherwise25f | Relaxed/strict/normal entry helper,used by GestureRecognizer.setSensitivity. Settings clamps saved value0..2; helper itself defaults any other int toNormal. |
| tiltExitDegrees(sensitivity) |0→10f,2→14f,otherwise12f | Matching relaxed/strict/normal rearm. Sensitivity does not modify other gesture thresholds or token byte IDs. |
| FLAT_DEGREES |8f | Both axes<=8and still for flat hold; tolerance defines face-up level region. |
| MOVE_GAP_MS |300 | GestureRecognizer emission,GestureSession acceptance and GesturePadView click guard; reject interval<300,accept equality. Avoid rapid accidental repeats. |
| FLAT_HOLD_MS |500 | detectFlat requires continuous level/still elapsed>=500; deliberate flat hold. |
| STILL_FINISH_MS |1500 | Session after>=4tokens finishes with continuous sampled stillness>=1500. Longer would allow more pause between deliberate moves. |
| SNAP_DOWN_METERS_PER_SECOND_SQUARED |−7f | detectSnap starts downward gravity-removed deviceZ pulse at az<=−7; more negative requires sharper pulse. |
| SNAP_MAX_MS |220 | Valid recovery elapsed<=220; longer pulse rejected; duration>220sets rejection. |
| SNAP_RECOVERY_METERS_PER_SECOND_SQUARED |−1.5f | az>=−1.5is recovery/rearm; moving threshold toward0requires fuller return. |
| SNAP_MAX_TILT_DEGREES |20f | Either angle magnitude>20rejects SNAP; equality20permitted. Keeps gesture near face-up. |
| STILL_ACCEL_TOLERANCE |.8f | Linear acceleration magnitude<=.8required for still; inclusive tolerance for tremor. |
| STILL_GYRO_RADIANS_PER_SECOND |.15f | abs(angularSpeed)<=.15required alongside linear condition. Fallback estimates angle speed without gyro. |
| SENSOR_PERIOD_US |20,000 | Accelerometer/gyro registration request20ms(~50Hz); real sensor delivery may differ. Smaller interval means more samples/battery work. |
| SENSOR_MAX_DT_SECONDS |.25f | Sensor integration/freshness restart if> .25s; recognizer/session continuity gap>250ms. Equality250does not count as a gap. |
| SENSOR_MIN_GRAVITY_MAGNITUDE |1f | gravity vector magnitude<1m/s²does not initialize/emit orientation; avoids near-zero angle estimate. Equality1passes. |
| FUSION_MIN_ROLL_COSINE |.15f | abs(cos(roll))<.15switches gyro step to accel angles; avoid near-singular Euler rate conversion. Equality.15does not trigger this guard. |
| GRAVITY_METERS_PER_SECOND_SQUARED |9.80665f | emit uses fixed gravity magnitude in m/s²for device-axis subtraction; not an adaptive local calibration. |
| MIN_TOKENS / MAX_TOKENS |4 / 8 | Credential/session length; max buffer; minimum completion and fixed-length compare loop. |
| LEFT / RIGHT / FORWARD / BACK / SNAP / FLAT | byte0 /1 /2 /3 /4 /5 | Permanent version1tokens shared by motion/pad/credential canonicalization. |
| MAX_PASSPHRASE_CHARS |1024 | Canonical credential input cap in UTF-16code units; bounds allocation,not estimated strength. |

Constants also contains non-motion format/image values; their deeper use is in D2. For completeness at the credential boundary: VERSION1,HEADER_BYTES38,HEADER_PIXELS102,SALT_BYTES16,IV_BYTES12,TAG_BYTES16,KEY_BITS256,PBKDF2_ITERATIONS120000,FLAG_PASSPHRASE1,FLAG_GHOST2,MAX_PIXELS9000000,MAX_PAYLOAD_BYTES16777216,MAX_FILENAME_BYTES240,MAX_PACKED_BYTES16777463,MAX_CIPHER_BYTES3374961,COVER_LONG_SIDE3000. These do not tune recognition. The flags are header bits; a tag is an encryption authentication result; names and definitions appear in the key-material section below.

| Constants helper method | Parameters / return / caller | Calls / edge behavior |
| --- | --- | --- |
| tiltEnterDegrees(int sensitivity) | returnsfloat20/25/30; GestureRecognizer.setSensitivity calls it. | Conditional expression only;no IO/exceptions/state changes. |
| tiltExitDegrees(int sensitivity) | returnsfloat10/12/14; same caller. | Conditional expression only;all other valuesNormal. |

Constants private constructor has no operation other than preventing ordinary instances. There is no gesture threshold secretly stored in pixel format or database per vault. Sensitivity preference is read at SensorFusionManager.start,so changing it affects later recording starts. KineticOrbView scales visual angles using the normal25constant even if recognizer sensitivity isRelaxed/Strict; replay also uses25for display. Visual orb extent is not the acceptance threshold in every sensitivity.

## How tokens become key material

**Cryptographic key material** is the password/key data used to protect the payload. **PBKDF2** (Password-Based Key Derivation Function2) repeatedly applies a keyed hash to derive a fixed-size key. **HMAC-SHA256** is the keyed-hash algorithm used here. A **salt** is fresh public random input that changes the derived key for the same credential. **AES-GCM** is encryption plus authentication: AES=Advanced Encryption Standard; GCM=Galois/Counter Mode. An **IV** (initialization vector) initializes each encryption; a **tag** is the cryptographic tamper-detection result; **AAD** (additional authenticated data) is readable header metadata also protected by that tag. **Plaintext** is secret readable content; **ciphertext** its encrypted form.

### Idea and worked credential

Token sequence from the worked motion run[1,0,4,5] plus illustrative phraseA|é becomes:

```text
KVLT1|4|1045|0041007c00e9
prefix |count|digits|four lowercase hex digits per UTF-16 passphrase unit
length =9+4+4*3=25chars
```

'A' char65hex0041,'|'124hex007c,'é'233hex00e9. For'A',shifts12,8,4,0and mask15yield0,0,4,1. Tokens are encoded by(char)('0'+token): token1becomes character'1',not rawbyte1in a textual password. No spaces/case/Unicode are normalized; empty phrase still has final separator. Phase of motion,timing,accelerometer vectors,degree magnitudes and sensitivity are not encoded. Therefore compatible pad input derives the same credential given identical phrase and salt.

### Step-by-step real derivation

1. Controllers takeTokens produce a sized byte[]only after completed session/enrollment. HideActivity.forge or RevealActivity.unlock passes it and char[]phrase to worker. An absent phrase is new char[0],not null.
2. CryptoCore.transform calls derive(tokens,passphrase,header.salt()). GestureCredential.canonical validates non-null4..8tokens0..5,non-null phrase<=1024code units.
3. canonical allocates9+tokenCount+phraseLength*4chars,copies KVLT1|,writes count/delimiters/token digits,then writePassphrase's four hex nibbles per char.
4. derive creates PBEKeySpec(password,salt,120000,256),SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"),generateSecret,then getEncoded ->256/8=32key bytes. Real key bytes depend on the random salt/credential; no fixed key is fabricated for this example.
5. transform wraps key bytes as SecretKeySpec(...,"AES"),initializes Cipher(AES/GCM/NoPadding) with GCMParameterSpec(128,header.iv()),supplies all38header bytes as AAD,then doFinal encrypts/decrypts. Example100GZIP bytes yields116ciphertext+tag bytes; tag128bits=16bytes,IV12bytes,salt16bytes.
6. derive finally wipes canonical chars/clears PBEKeySpec password and attempts generated SecretKey destruction. transform finally wipes key bytes and attempts AES-key destruction. HideWork and Reveal workers own/wipe supplied gesture/phrase arrays; GestureSession.close wiped original session array earlier.

These arrays can be cleared where owned; Java Strings,keyboard/UI/crypto-provider/native copies are not guaranteed erased. Storing a gesture enrollment twice does not persist a recovery credential. Wrong tokens or phrase cannot be distinguished reliably from changed/damaged authenticated ciphertext by tag failure; user must retry original credentials/untouched image. User-selected gestures alone are weak against offline guessing;120000iterations slows each guess but adds no entropy. Key size256bits does not imply256bits of user-secret strength.

### Credential bridge methods and crypto types

| Method / parameters | Return / purpose | Caller → calls | Failure/ownership |
| --- | --- | --- | --- |
| GestureCredential.canonical(byte[] tokens,char[] passphrase) | New char[]canonical credential. | CryptoCore.derive → validate,System.arraycopy,writePassphrase. | Invalid inputs throw IllegalArgumentException; caller owns returned secret chars. |
| validate(byte[] tokens,char[] passphrase) | void bounds/type validation. | canonical → token loop. | Null,too-short/long,bad0..5token,phrase>1024rejected. |
| writePassphrase(char[] out,int offset,char[] passphrase) | void fill supplied output. | canonical → public hex alphabet.toCharArray,shifts/mask. | Private caller guarantees output space; preserves UTF-16units. |
| CryptoCore.derive(byte[] tokens,char[] passphrase,byte[] salt) |32key bytes. | transform → canonical,PBEKeySpec,SecretKeyFactory,generateSecret/getEncoded,destroy. | GeneralSecurityException propagates; cleanup in finally. |
| CryptoCore.transform(int mode,byte[] input,VaultHeader header,byte[] tokens,char[] passphrase,WorkProgress progress) | encrypted or authenticated decrypted bytes. | encrypt/decrypt → derive,SecretKeySpec,Cipher,GCMParameterSpec,header accessors,progress. | On decryption authentication failure no accepted payload returned; cleanup owns key bytes,not caller credentials. |
| CryptoCore.destroy(SecretKey key) | void best-effort provider erasure. | derive/transform → key.destroy. | Null ignored; DestroyFailedException tolerated without logging secret. |

GestureCredential has no state/private constructor; its entropyBits contract is above. CryptoCore is the stateless cryptography utility; VaultEngine calls encrypt/decrypt and transform handles derivation. Its nested Sealed returns public header/ciphertext together; no gesture state. Full encrypt/decrypt,header,payload/GZIP/stego methods are in10_CRYPTO_AND_STEGO.md,not a second motion algorithm.

| Namespace type used at this boundary | Definition / real source example |
| --- | --- |
| java.security.SecureRandom | Cryptographic random generator; `SecureRandom random = new SecureRandom();` then `random.nextBytes(salt);` in CryptoCore.encrypt; also fills IV. |
| java.security.GeneralSecurityException | Checked family of key/algorithm/provider failures; declared by CryptoCore.derive/transform/encrypt/decrypt. |
| javax.crypto.SecretKeyFactory | PBKDF2 provider operation; `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");` in derive. |
| javax.crypto.SecretKey | Provider-generated key interface; `generated = factory.generateSecret(specification);`; getEncoded returns bytes. |
| javax.crypto.spec.PBEKeySpec | Password,salt,iteration,key-length request; `new PBEKeySpec(password, salt, Constants.PBKDF2_ITERATIONS, Constants.KEY_BITS)`; clearPassword clears owned copy. |
| javax.crypto.spec.SecretKeySpec | Raw AES key wrapper; `new SecretKeySpec(keyBytes, "AES")` in transform. |
| javax.crypto.Cipher | Encryption/authentication object; `Cipher.getInstance("AES/GCM/NoPadding")`,updateAAD/doFinal in transform. |
| javax.crypto.spec.GCMParameterSpec | GCM tag-bits andIV configuration; `new GCMParameterSpec(128, header.iv())`. |
| javax.security.auth.DestroyFailedException | Best-effort key-destruction failure; caught by destroy. It is outside javax.crypto but used in this cleanup. |
| javax.crypto.AEADBadTagException | Authenticated-encryption failure subtype mentioned in core comment,not directly imported/caught there. It does not uniquely diagnose bad gesture rather than damage. |

No java.security/javax.crypto/java.util.zip class is used directly by sensor/ or gesture/. They pass tokens to core. GZIPInputStream/GZIPOutputStream/Deflater belong to GzipCodec payload compression,not motion; the context reading aid below defines them as required by the documentation rules without introducing a motion dependency.

## Lifecycle, callback ordering and cleanup ownership

SensorFusionManager's start comment says to call from onResume, but actual UI controllers start on **Begin/Record**, not automatically every Activity resume. The enforced condition is main-thread ownership. They stop immediately on recording completion/reset and host pause, so sensor registration lasts only the needed interaction. Sensor callbacks are registered using main Handler; the injected recognizer Executor runs commands inline if already on main, otherwise posts there. No separate sensor worker exists in this implementation.

Real executor callback from sensor/SensorFusionManager.java constructor:

```java
recognizer = new GestureRecognizer(command -> {
    if (Looper.myLooper() == Looper.getMainLooper()) command.run();
    else main.post(command);
}, listener);
```

SensorFusionManager requires main on start/stop/resetGesture. GestureRecognizer only documents serialized calls/executor; its volatile generation invalidates deliveries but does not make every float/flag update safe for arbitrary concurrent calls. GestureSession also requires single-thread use. Session callbacks can run inline and mutate/close current session or stop sensors before the invoking method returns; recognizer.deliver's second stamp check handles that lifecycle change. Hardware sample time is converted to integer milliseconds,so equal-ms update times can be ignored even if original nanoseconds differ.

| Host/boundary | Exact motion-related cleanup | Visible effect / limitation |
| --- | --- | --- |
| HideActivity.showStep(int next) | If leaving/entering gesture step,gesture.reset; secret controls deactivate first. | No completed enrollment carried backward/forward across steps unless tokens already transferred to Forge. |
| HideActivity.onPause() | gesture.reset,rain stop,secret deactivate,work.clearSensitive; if step2or3go to secret step and clear sensitive UI. | Pausing recording/forging requires secret/phrase/enrollment again. onStop clears text/phrase UI. |
| RevealActivity.onPause() | gesture.clear,password.clear;if busy lock. | Always clears active/completed local attempt even when autoLock off. |
| RevealActivity.onStop()/screenOff | Auto-lock if configured; unregister screenOff receiver in onStop; close if screen no longer interactive under policy. | Auto-lock setting changes revealed content policy,not recording cleanup. |
| RevealActivity.onDestroy() | gesture.clear,password/scrambler/door reset,work.close. | No secret restored from saved state; selected source URI alone can be retained. |
| HideWork.forge cleanup | Arrays.fill(tokens,0); Arrays.fill(passphrase,'\0') in cleanup. | Transferred arrays erased after worker exits including cancellation; expensive crypto may finish before cancellation boundary. |
| GestureSession.close()/takeTokens() | Wipe8token buffer,reset count/times,close. takeTokens returns separate sized copy before close. | Clearing old session cannot wipe returned copy; new owner must clear it. |
| GesturePadView.onDetachedFromWindow() | Cancel press,reset pad lastMove. | Does not own session; host must stop/clear that separately. |

**No motion credential storage:** SensorFusionManager,GestureRecognizer and GestureSession use only memory; no log,disk,network,SharedPreferences or SQLite write appears in them. Preferences store sensitivity/pad choices; they do not save gesture sequence or sensor trace. Private replay and Nerd overlay intentionally show sensitive named moves transiently. Array fills cannot guarantee erasure of platform/UI/provider/native copies; do not claim complete memory sanitization.

### Optional NerdOverlay integration

Purpose: educational display of current sensor samples/tokens; created by GestureStep/RevealGesture with a TextView. It implements SensorFusionManager.TelemetryListener. Fields are view,8-byte token history,count,last rendered time and current sample String. No history export or storage methods exist. Source: ui/NerdOverlay.java.

| Method / parameters | Return / work | Caller → calls | Edge/cleanup |
| --- | --- | --- | --- |
| NerdOverlay(TextView view) | New display helper; disables view saved state. | Controllers → view.setSaveEnabled(false),clear. | TextView is caller-provided; no sensor registration here. |
| clear() | void; zero token history/count/time/sample,set visibility from nerd preference,reset idle text. | Constructor,begin,controller reset/finish/pause → Arrays.fill,VaultPreferences.current. | Token buffer cleared; old immutable Strings cannot be securely overwritten. |
| begin(boolean pad) | void; clear then identify pad mode if selected. | Controller.begin → clear,setText. | Pad has no active raw sensors/fused angles. |
| token(byte token) | void; append valid token name to bounded display. | Controller recognizer/pad callbacks → System.arraycopy when full,render. | Disabled Nerd/invalid byte ignored. Called before session.addToken,so displayed proposals are not a proof of accepted/session count. |
| onSample(float ax,float ay,float az,float gx,float gy,float gz,float pitch,float roll,Mode mode) | void; format latest raw sensor values/angles/filter status. | SensorFusionManager.emit → SystemClock.uptimeMillis,getString,render. | Throttles output interval to>=120ms; fallback labels no gyro and displayedalpha0,not a different code FILTER_ALPHA setting. |
| render() | void; combine sample plus named token history. | token/onSample → StringBuilder,localized resources,view.setText. | Empty sample uses pad label; empty moves shows waiting. No credential strength calculation. |

### Optional private replay integration

Purpose: show only the current matched sequence before Forge; GestureStep creates GestureReplay. Fields main Handler,host,dialog,orb,label,moves clone,index,tick Runnable and token-name resources. Source: ui/hide/GestureReplay.java.

| Method / parameters | Return / work | Caller → calls | Edge/cleanup |
| --- | --- | --- | --- |
| GestureReplay(HideActivity host) | New helper storing host. | GestureStep constructor. | No gesture retained until show. |
| show(byte[] enrolled) | void; clear old replay,clone array,secure host/dialog,build names+orb panel,start next. | Replay click → clear,clone,MaterialAlertDialogBuilder,ValueAnimator.areAnimatorsEnabled,orb.setAnimating,next. | Null no-op after clear; view/label saved state disabled. Reveals tokens to person watching; requires opt-in. |
| next() | void; label one token and pose orb,count; schedule next900ms. | show/Handler tick → getString,orb.setAngles/setTokenCount,postDelayed. | Uses normal25degree display angle. SNAP/FLAT are identified by name,not recreated raw accelerations. Final clone wiped/null,but dialog can retain final visible label until clear. |
| clear() | void; cancel tick,wipe clone,clear label/orb,dismiss dialog and restore configured host protection. | Dismiss/reset/takeTokens/host pause path → removeCallbacks,Arrays.fill,orb.reset. | Detaches dismiss listener to avoid recursive clearing; secure flag retained if protection or nerd enabled. |

### Orb feedback integration

KineticOrbView is the custom visual indicator created by Hide XML and replay (also used decoratively on Home). It owns Paint,dial RectF,24point X/Y trail,target/current position,count/head/animation flag. It does not recognize moves or derive keys. Source: ui/KineticOrbView.java.

| Method / parameters | Return / work / caller | Calls and edge behavior |
| --- | --- | --- |
| KineticOrbView(Context context) / (Context context,AttributeSet attrs) | New view; XML/Java setup. | Constructor delegation,parent setup,accessible0of8description. |
| setAngles(float pitch,float roll) | void; recognizer/replay callback maps to target x=roll/25,y=−pitch/25,clamped−1..1. | Math.max/min; visual normal25scale is independent of sensitivity. |
| setTokenCount(int count,boolean haptic) | void; session count/replay feedback. | Clamp0..8; CLOCK_TICK only if increased and requested; update description/invalidate. |
| setAnimating(boolean running) | void; host recording/replay starts/stops frames. | postInvalidateOnAnimation only when running; not sensor registration. |
| reset() | void; clear positions/trails/count. | Controller/reset/detach → Arrays.fill,setTokenCount(0,false). |
| onDraw(Canvas canvas) | void; Android draw updates trail/smoothed display while animating. | drawDial/drawOrb; position smoothing.18per frame; records ring-buffer trail; requeue only animating+shown+visible. |
| drawDial(Canvas canvas,float cx,float cy,float radius) | void; eight count arcs. | onDraw → Paint/Canvas draws36degree arcs in45degree steps,cyan accepted/outline inactive. |
| drawOrb(Canvas canvas,float cx,float cy,float radius) | void; fading trail/glow/core. | onDraw → Canvas circles,Paint alpha/colour;24point history,not credential trace stored to disk. |
| onDetachedFromWindow() | void; stop/reset then parent. | Android detach → setAnimating(false),reset,super. |

Visual smoothing of.18is not sensor FILTER_ALPHA.98or GRAVITY_FILTER_ALPHA.9; conflating these three numbers would give the wrong algorithm. Replay's900ms and overlay's120ms are presentation intervals,not token timing constraints. Recognition thresholds all come from Constants.

## Java/Android constructs with tiny real examples

Each example below is a source excerpt of no more than ten lines. The terms were defined when first used above; this section gives concrete syntax. Context-only constructs required by _RULES.md are explicitly separated from sensor/gesture responsibilities.

### Interface, enum, lambda, anonymous class and method reference

Real sensor mode declaration in sensor/SensorFusionManager.java:

```java
public enum Mode { GYROSCOPE_FUSION, ACCELEROMETER_ONLY, UNAVAILABLE }
```

Real accepted-token contract in gesture/GesturePadView.java:

```java
public interface OnTokenListener {
    boolean onToken(byte token, long timestampMillis);
}
```

The actual file adds a comment documenting accepted-token return semantics; the method declaration above is unchanged. Real lambda in ui/hide/GestureStep.java — bind:

```java
pad.setOnTokenListener((token, time) -> {
    if (current == null) return false;
    nerd.token(token); return current.addToken(token, time);
});
```

An anonymous Listener implementation starts with `new GestureRecognizer.Listener() {` in the GestureStep constructor and supplies onToken/onMotion methods. Named inner class declaration from gesture/GesturePadView.java:

```java
private final class PadAccessibility extends AccessibilityDelegate {
```

It can access the outer view's selected/cells/listener. Named static nested types such as CryptoCore.Sealed do not capture an outer instance. Method reference in ui/hide/GestureReplay.java:

```java
private final Runnable tick = this::next;
```

Runnable means no returned value. The conditional expression condition?valueA:valueB selects one branch; token sign mapping and sensitivity helpers use it. Arrays.copyOf takes a sized copy; System.arraycopy copies a region (overlay sliding history); Arrays.fill clears owned arrays. Objects.requireNonNull explicitly rejects missing mandatory listener/executor.

### Generics, worker scheduling, Handler and Looper

Real declarations from ui/hide/HideWork.java:

```java
interface Callback<T> { void success(T value); void failure(int message); }
private final ExecutorService worker = Executors.newSingleThreadExecutor();
private final Handler main = new Handler(Looper.getMainLooper());
```

T is a generic type placeholder; Callable<T>.call returns that typed value and may throw. HideWork.run executes on worker,then posts a callback to main:

```java
T result = action.call();
main.post(() -> { if (!closed) callback.success(result); });
```

SensorFusionManager's injected Executor instead executes immediately when already main,as shown earlier. This is why an Executor alone does not prove background processing. volatile generation suppresses obsolete callback delivery; AtomicInteger secretGeneration in HideWork controls cancelled sensitive work. Single-thread ownership is still required for sessions.

### Bitwise operators, conversions, varargs and resources

Bitwise ^ compares bits; OR combines differences; & masks; >>> shifts right with zeros; << shifts left. Byte/char arithmetic promotes to int. Real phrase-encoding lines in core/GestureCredential.java:

```java
for (int shift = 12; shift >= 0; shift -= 4) {
    out[offset++] = digits[(character >>> shift) & 15];
}
```

Real token digit conversion in canonical:

```java
result[8 + i] = (char) ('0' + tokens[i]);
```

Real sensor time conversion in sensor/SensorFusionManager.java — emit:

```java
recognizer.update(timestampNanos / 1_000_000L, pitch, roll, ax, ay, az, angularSpeed);
```

Long literal suffixL keeps divisor64-bit; integer division discards sub-ms fraction. A float...parameter in GestureRecognizer.finite is **varargs**,accepting multiple values as an array. A typecast to SensorManager converts getSystemService's general result to the expected class. **XML** is Android's layout/resource markup; getDimension converts dp/sp resources into actual pixel values for Canvas. Method signatures list parameter types/names; void means no returned value; boolean means true/false.

### Canvas, Paint, ValueAnimator and lifecycle

Real drawing in gesture/GesturePadView.java — drawCell:

```java
paint.setStyle(Paint.Style.FILL);
paint.setColor(getContext().getColor(pressed == index
        ? R.color.kv_surface_high : R.color.kv_surface));
canvas.drawRoundRect(cell, radius, radius, paint);
```

Paint stores current style; canvas draws using it. MotionEvent carries touch action/position/pointer ID,KeyEvent carries keyboard action; these are not hardware SensorEvents. invalidate requests redraw; postInvalidateOnAnimation schedules a display frame. ValueAnimator controls animation preference at replay's boundary in ui/hide/GestureReplay.java:

```java
orb.setAnimating(ValueAnimator.areAnimatorsEnabled());
```

Replay uses the setting rather than constructing a sensor-driven animator. Lifecycle snippet from ui/reveal/RevealActivity.java — onPause:

```java
gesture.clear(); password.getText().clear();
if (busy) lock();
super.onPause();
```

Android invokes lifecycle callbacks; the app invokes cleanup helpers. Bundle does not save gesture tokens: Reveal's saved state contains selected image address only; Hide discards child state. GestureSession implements AutoCloseable,so close can be explicit or invoked through try-with-resources; current recording controllers close explicitly.

### Context-only: Intents, Uri, ContentResolver, Activity Result API and BroadcastReceiver

An **Intent** requests an Android operation; **Uri** identifies provider content; **ContentResolver** opens its bytes. The **Activity Result API** registers an operation contract and callback before an external picker/viewer is launched. These get the original image into Reveal,not motion samples. Real picker declaration from ui/reveal/RevealActivity.java:

```java
private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) load(uri); });
```

Real provider source from image/ImageUtils.java — source(ContentResolver,Uri):

```java
return () -> resolver.openInputStream(uri);
```

Real implicit-view Intent from ui/reveal/RevealActivity.java — export:

```java
Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(value.uri, value.mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
```

**MIME** is the content type identifying compatible viewers. A **BroadcastReceiver** handles Android event messages,selected by an **IntentFilter**. Reveal's anonymous receiver responds to ACTION_SCREEN_OFF; onStart registration is:

```java
ContextCompat.registerReceiver(this, screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF),
        ContextCompat.RECEIVER_NOT_EXPORTED);
```

onStop unregisters; auto-lock preference controls clearing revealed content. Sensor listeners are separate registrations,stopped by controllers on pause; do not confuse broadcast and sensor callback types.

### Context-only: SQLite, HTTP/JSON and try-with-resources

**SQLite** is the local relational database. **SQLiteOpenHelper** creates/upgrades schema; **SQLiteDatabase** executes queries/updates; **Cursor** iterates rows; **ContentValues** supplies typed column values. None stores gesture trace/key material. Real examples from data/VaultRepository.java — record:

```java
SQLiteDatabase db = helper.getWritableDatabase();
db.beginTransaction();
```

A transaction commits a group of operations together. Class declaration in data/VaultDatabaseHelper.java:

```java
public final class VaultDatabaseHelper extends SQLiteOpenHelper {
```

Real automatic-close query from data/VaultRepository.java — findByHash:

```java
try (Cursor cursor = helper.getReadableDatabase().query("vaults", null,
        "image_sha256 = ?", new String[]{hash}, null, null, null, "1")) {
    return cursor.moveToFirst() ? VaultEntry.read(cursor) : null;
}
```

The ?placeholder binds a value separately from SQL text; SQL is the database query language. Another record fragment is `ContentValues values = new ContentValues();`. These are metadata operations outside motion packages.

**HTTP** is the web request protocol; HTTPS adds encrypted transport. **HttpURLConnection** sends requests; **JSON** is named-field structured text represented here by JSONObject. Real source from data/SyncClient.java — post/response:

```java
HttpURLConnection c = (HttpURLConnection) url.openConnection();
```

```java
JSONObject result = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
if (!result.optBoolean("ok", false)) throw new IOException();
```

UTF-8 encodes text bytes. No HTTP or JSON operation appears in sensor/gesture; metadata network service does not receive these token arrays through their API. IOException is an IO failure. Real try-with-resources from core/GzipCodec.java — compress:

```java
try (WipingBuffer output = new WipingBuffer()) {
    try (ConfiguredGzip gzip = new ConfiguredGzip(output, compress)) {
        gzip.write(packed);
        gzip.finish();
        return output.toByteArray();
    }
}
```

ByteArrayOutputStream is a growable in-memory byte output buffer; a static nested WipingBuffer overrides close to fill backing bytes. GZIPOutputStream writes a compression envelope,GZIPInputStream reads it,Deflater supplies DEFAULT_COMPRESSION/NO_COMPRESSION levels. Real line from GzipCodec.ConfiguredGzip:

```java
def.setLevel(compress ? Deflater.DEFAULT_COMPRESSION : Deflater.NO_COMPRESSION);
```

These are every java.util.zip type in this explicitly read compression context. None is used directly in motion packages. Compression/crypto/image algorithms not required by D3 remain explained in10/11 rather than being invented as sensor behavior.

## Practical limits and failure explanations for a viva

- Some hardware lacks gyro or accelerometer; fallback is deliberate. Pad entries use identical token meanings,so a missing sensor need not prevent access to a known credential. Automatic pad checkbox switching clears the attempt;begin again.
- Strong translation contaminates gravity estimation; drift and orientation singularities are handled by simple reset/blend rules,not a full calibrated inertial-navigation system. Fixed requested sensor period is not a measured guaranteed rate.
- A tilt only rearms when both axes return within exit; holding level long enough may add FLAT. If unexpected counts occur,clear/restart both Hide performances,use Done after intended moves,adjust sensitivity or choose pad.
- Raw token callback can precede session acceptance; use session count as accepted count. Gap checks exist at recognizer/session/pad boundaries and guard different sources/ownership points.
- Enrollment matches count/order only; replaying a known sequence reproduces credentials. Second performance adds repeatability confirmation,not independent password entropy. Timing noise is not cryptographic randomness.
- Sensor state,session arrays and replay/overlay buffers clear at defined lifecycle boundaries; captured UI/platform/native copies cannot be guaranteed erased. Do not promise screenshot protection stops shoulder-surfing or a camera.

UNCLEAR as a guaranteed physical-device sign convention beyond this portrait implementation: source states `/** Portrait device axes: +pitch raises the top (BACK); +roll lowers the right (RIGHT). */`. The equations and token mapping consistently implement that stated convention; the code contains no per-device mounting calibration or display-rotation remapping. Present it as the implementation's device-axis convention,not proof of calibrated behavior on every form factor.

## Likely viva questions

1. **Why use accelerometer and gyroscope together?** Gyro follows quick rotation but drifts; gravity-derived accelerometer angles correct it. This code uses wrapped.98prediction/.02correction,plus separate.9gravity smoothing.
2. **Why does holding the phone tilted not repeat the same move?** Entry sets a tilt latch;both angles must return inside the smaller exit threshold before another tilt. A300msminimum token gap also applies.
3. **How are SNAP,FLAT and recording completion different?** SNAP is a short downward linear-Z pulse with recovery;FLAT is a500mslevel/still pose emitting token5;session completion is1500mscontinuous sampled stillness after at least4tokens or explicit Done.
4. **What does enrollment actually compare?** Two distinct completed sessions' counts and exact0..5token arrays,including all8slots with XOR/OR;not raw sensor traces or timing. Matching tokens/phrase reproduce the same canonical credential.
5. **Why recommend a passphrase with a256-bit key?** Four/eight uniformly random tokens contribute at most10.34/20.68bits,and human choices may be weaker. PBKDF2 slows guesses but does not create unpredictability;the meter never claims phrase entropy.

## Covers these files

- `app/src/main/java/com/example/kineticvault/sensor/SensorFusionManager.java`
- `app/src/main/java/com/example/kineticvault/gesture/GestureRecognizer.java`
- `app/src/main/java/com/example/kineticvault/gesture/GestureSession.java`
- `app/src/main/java/com/example/kineticvault/gesture/StrengthMeter.java`
- `app/src/main/java/com/example/kineticvault/gesture/GesturePadView.java`
- `app/src/main/java/com/example/kineticvault/core/Constants.java`
- `app/src/main/java/com/example/kineticvault/core/GestureCredential.java`
- `app/src/main/java/com/example/kineticvault/core/CryptoCore.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/GestureStep.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/GestureReplay.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/HideActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/hide/HideWork.java`
- `app/src/main/java/com/example/kineticvault/ui/reveal/RevealGesture.java`
- `app/src/main/java/com/example/kineticvault/ui/reveal/RevealActivity.java`
- `app/src/main/java/com/example/kineticvault/ui/NerdOverlay.java`
- `app/src/main/java/com/example/kineticvault/ui/KineticOrbView.java`
- `app/src/main/java/com/example/kineticvault/data/VaultPreferences.java`
- `app/src/main/java/com/example/kineticvault/core/GzipCodec.java`
- `app/src/main/java/com/example/kineticvault/data/VaultRepository.java`
- `app/src/main/java/com/example/kineticvault/data/VaultDatabaseHelper.java`
- `app/src/main/java/com/example/kineticvault/data/SyncClient.java`
- `app/src/main/java/com/example/kineticvault/image/ImageUtils.java`
- `app/src/main/res/layout/hide_step_gesture.xml`
- `app/src/main/res/layout/activity_reveal.xml`
- `app/src/main/res/values/dimens.xml`
- `app/src/main/res/values/gesture_ids.xml`
- `app/src/main/res/values/strings.xml`
- `docs/viva/_RULES.md`
- `docs/viva/00_PROJECT_MAP.md`
