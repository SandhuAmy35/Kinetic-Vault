# Kinetic Vault Design System

Source notation: native resources live under `app/src/main/res/`; Java views under `app/src/main/java/com/example/kineticvault/`. Native dimensions retain Android `dp` and `sp`; they are not converted to CSS pixels. Unspecified Material 3 / Android defaults are identified rather than estimated.

## Brand & Voice

- **Product:** Kinetic Vault (`app_name`). Android app that encrypts text or files and hides them inside PNG images; phone-motion gestures and an optional passphrase derive the key.
- **Audience — inferred:** people protecting private text/files, with educational tools for exploring image steganography.
- **Visual tone — inferred from resources:** cyber-vault, luminous cyan, violet halos, shield/lock imagery, animated motion dial and binary rain. Both light and dark native themes use `Theme.Material3.DayNight.NoActionBar` (`Base.Theme.KineticVault`).
- **Copy:** direct, conversational, instructional. Short action labels; complete sentences for guidance; longer recovery messages specify the next action. Feature names use Title Case, e.g. “Integrity Lab”; action labels use sentence case, e.g. “Hide a secret” (`home_hide`). Eyebrows/status accents use literal ALL CAPS, e.g. “PRIVATE BY DESIGN” (`home_eyebrow`); there is no automatic uppercase transform in the shared button style (`android:textAllCaps=false`).
- **Characters:** middot separators `·`, ellipsis `…`, arrows `→`, mathematical symbols and calculator glyphs `⌫ ÷ × −`; sampled copy uses no emoji.
- **Microcopy:** “Your hand is the key.” (`tagline`); “Your vault starts here.” (`vault_empty_title`).

## Colors

Native semantic tokens: `values/colors.xml` and `values-night/colors.xml`. Android eight-digit hex is **#AARRGGBB**; CSS eight-digit hex in the explainer is **#RRGGBBAA**.

| Role / token | Light | Dark |
|---|---|---|
| Page background (`kv_background`) | #F3F6FC | #0A0E1A |
| Card/surface (`kv_surface`) | #FFFFFF | #121A2B |
| Higher surface; pressed Gesture Pad fill (`kv_surface_high`) | #E5ECF7 | #1C2940 |
| Dominant primary accent (`kv_primary`, `colorPrimary`) | #006775 | #00E5FF |
| Text on primary fill (`kv_on_primary`, `colorOnPrimary`) | #FFFFFF | #06212A |
| Secondary violet (`kv_violet`, `colorSecondary`) | #6035CF | #B39AFF |
| Tertiary magenta; Hide notice text (`kv_magenta`, `colorTertiary`) | #BB1969 | #FF6ABB |
| Primary text (`kv_text`, `colorOnSurface`) | #142237 | #F4F8FF |
| Secondary/body text (`kv_text_secondary`, `colorOnSurfaceVariant`) | #465673 | #A5B5CB |
| Outline (`kv_outline`, `colorOutline`) | #71819C | #5F7493 |
| Success (`kv_success`) | #087044 | #39FF88 |
| Error (`kv_danger`, `colorError`) | #B4233D | #FF7188 |
| Outer cyan-card halo (`kv_glow_outer`) | #12006775 | #0D00E5FF |
| Inner cyan halo/border (`kv_glow_inner`) | #28006775 | #2600E5FF |
| Violet halo/border (`kv_violet_glow`) | #226035CF | #337C4DFF |

- Persistent launcher branding: #0A0E1A (`kv_brand_background`), #00E5FF (`kv_brand_cyan`), shield fill #121A2B (`drawable/ic_launcher_foreground.xml`). Brand tokens inherit unchanged into night resources.
- Cyan dominates controls, kickers, line icons, gesture focus and motion decoration. Violet differentiates secondary navigation/information cards and the Ghost comparison panel. Magenta highlights Hide notices. No dedicated warning or info color token is declared in these theme resources.
- Card borders use translucent glow tokens, rather than the general outline token. Vault list separators use `@android:color/transparent` (`activity_vault.xml`).

**Offline explainer palette** — `app/src/main/assets/how_it_works.html`, fixed dark via `:root{color-scheme:dark}`:

| Role / selector | Exact value |
|---|---|
| Page / lowest-bit text (`body`, `.last`) | #0a0e1a |
| Main text (`body`) | #edf5ff |
| Paragraphs (`p`) | #c6d1e5 |
| Heading/tag/accent/bit fill/button border (`h2`, `.tag`, `.last`, `button`) | #78eee8 |
| Card border (`.card`) | #33475b |
| Gradient stops (`.card`) | #142133, #151329 |
| Button fill / text (`button`) | #15343e / #efffff |
| Keyboard-focus outline (`button:focus-visible`) | #d7baff |
| Flow-item border (`.flow span`) | #4d4566 |
| Note left border (`.note`) | #b39aff |
| Card glow (`.card`) | #40e0d010 |
| Swatch glow (`.swatch`) | #78eee830 |

Illustrative pixel swatch uses `rgb(100,150,200)` (`.swatch`); its red channel changes to 101 when toggled. Preserve RGB syntax from source rather than substituting a converted hex value.

## Typography

Native family: `sans` (`Base.Theme.KineticVault/android:fontFamily`), an Android system alias; no exact named typeface is declared. Nerd overlays explicitly use `monospace`; binary rain uses `android.graphics.Typeface.MONOSPACE`. These are system fonts, not downloaded web fonts.

| Native role | Size | Weight/style | Color | Line height / tracking |
|---|---|---|---|---|
| Result/display (`TextAppearance.KineticVault.Result`) | 48sp (`type_result`) | `bold` | `kv_text` | Inherited from `TextAppearance.Material3.DisplayMedium`; no local override |
| Screen title (`TextAppearance.KineticVault.Title`) | 28sp (`type_title`) | `bold` | `kv_text` | Inherited from `TextAppearance.Material3.HeadlineMedium`; no local override |
| Body (`TextAppearance.KineticVault.Body`) | 16sp (`type_body`) | Inherited | `kv_text_secondary` | Inherited from `TextAppearance.Material3.BodyLarge`; no local override |
| Kicker (`TextAppearance.KineticVault.Kicker`) | 12sp (`type_kicker`) | `bold` | `kv_primary` | `android:letterSpacing=0.12`; line height inherited from `TextAppearance.Material3.LabelMedium` |
| Small stepper (`activity_hide.xml`) | 14sp (`type_small`) | Body inheritance | Body inheritance | Body inheritance |
| Nerd telemetry (`hide_step_gesture.xml`, `activity_reveal.xml`) | 12sp | Unspecified; `monospace` | `?android:attr/textColorPrimary` | Unspecified |
| Calculator display (`activity_calculator.xml`) | 32sp | Unspecified | `?android:attr/textColorPrimary` | Unspecified |

Native roles do not define a heading h1/h2/h3 hierarchy. Do not fabricate intermediate sizes, numerical weights or line heights. Vault row titles override Body to `bold`. Kicker uppercase comes from literal strings, not a text-transform rule.

**Offline explainer:** `system-ui,sans-serif`; bits use `monospace`. No web-font import or @font-face declaration.

| CSS role | Exact sizing | Weight | Tracking |
|---|---|---|---|
| Body / buttons | `font:17px/1.65 system-ui,sans-serif`; buttons `font:inherit` | Browser default | Unspecified |
| h1 | `font-size:32px;line-height:1.2` | Browser h1 default | Unspecified |
| h2 | `font-size:22px`; inherited line height `1.65` | Browser h2 default | Unspecified |
| Tag (`.tag`) | `font-size:12px`; inherited line height `1.65` | Inherited | `.16em` |
| Bits (`.bits`) | `font:clamp(19px,5vw,30px)/2 monospace` | Font shorthand default | `.07em` |

## Spacing & Layout

- Native spacing: 4dp (`space_xs`), 8dp (`space_sm`), 16dp (`space_md`), 24dp (`space_lg`), 32dp (`space_xl`), from `values/dimens.xml`. Local 12dp padding/margins occur in telemetry and information panels; calculator key margins are 2dp.
- Radius ladder: 16dp controls (`control_radius`); 24dp card interior (`card_radius`); 26dp inner halo and 28dp outer halo (`bg_glow_card_cyan.xml`, `bg_glow_card_violet.xml`).
- Native stroke: 1dp (`stroke_width`) for cards and Gesture Pad. Standard vector strokes: `1.8` in a 24×24 viewport; launcher strokes `2` and `3` in a 108×108 viewport. Splash shield stroke is `density * 4` Canvas pixels (`SplashLogoView`).
- Native screens generally use full-width vertical LinearLayouts with ScrollViews; no fixed desktop container width. Home adds 24dp horizontal padding plus system-bar/display-cutout insets (`HomeActivity`), and 24dp vertical content padding (`activity_home.xml`). Titles and explicit Back/text controls replace a native action bar.
- Home action tiles have `minHeight=80dp`; violet tile vertical margins 8dp; card padding 24dp. Information panels commonly use 16dp padding and 12dp vertical margins.
- Gesture Pad: two columns × three rows; 8dp gaps (`space_sm`), preferred width 280dp (`gesture_pad_width`), minimum 48dp cells (`touch_min`). Layout heights differ: Hide 216dp; Reveal 280dp.
- Standard button minimum height 56dp (`button_height`), minimum width 48dp (`touch_min`). Many explicit secondary controls set minimum height 48dp; calculator keys 60dp.
- Vault rows: minimum height 96dp, padding 16dp, 64dp square center-cropped thumbnail, 16dp trailing thumbnail margin. List gaps are transparent dividers of 8dp.
- Comparison demo: equal-width two-column panels, each padding 8dp and containing 180dp-tall diff views (`activity_ghost_comparison.xml`). Cover preview 220dp; result comparison/diff views 240dp; X-ray result 200dp; forge animation 300dp.
- Additional declared dimensions: 24dp icons (`icon_size`), 64dp orb (`orb_size`), 160dp preview (`preview_height`); actual individual layouts may override these.
- Explainer CSS: universal `box-sizing:border-box`; body `margin:0;padding:22px`; cards `padding:20px;margin:22px 0`; flow `display:flex;flex-wrap:wrap;gap:8px`; flow items `padding:8px 12px`; buttons `padding:12px 18px;width:100%`; note `padding-left:16px`. Radii: bit highlight 5px, buttons/flow items 12px, swatch 16px, cards 20px. Swatch is 76px × 76px.

## Effects

- **Native card glow:** concentric filled layers, not a blurred shadow. Outer radius 28dp and `kv_glow_outer`; middle inset 2dp on all sides, radius 26dp, `kv_glow_inner` or `kv_violet_glow`; interior inset 4dp, radius 24dp, opaque `kv_surface`, 1dp stroke matching the middle layer. Sources: `drawable/bg_glow_card_cyan.xml`, `drawable/bg_glow_card_violet.xml`.
- **Motion orb:** five concentric cyan circles: alpha `15 + (5 - i) * 12`, radius `radius * (0.12f + i * 0.035f)`; solid core radius `radius * 0.115f`; 24-sample fading trail (`KineticOrbView`). Dial outline stroke `radius * 0.045f`, round caps; accepted segments cyan alpha 255, inactive outline alpha 70.
- **Explainer card:** `background:linear-gradient(145deg,#142133,#151329)`; `box-shadow:0 0 24px #40e0d010` (`.card`).
- **Explainer swatch:** `box-shadow:0 0 24px #78eee830`; `transition:background .5s` (`.swatch`).
- **Lowest-bit pulse:** `animation:glow 2s infinite`; at 50%, `box-shadow:0 0 18px #78eee8` (`.last`, `@keyframes glow`).
- **Resource motion:** `fade_in.xml`: alpha 0→1, 240 ms; `slide_up.xml`: Y 12%→0%, alpha 0→1, 280 ms; both use `@android:interpolator/fast_out_slow_in`. `pulse.xml`: alpha 0.65→1, 900 ms, reverse/infinite. `shake.xml`: X -2%→2%, 60 ms, reverse, repeatCount 5.
- **Home motion:** tiles translate Y from 32 Canvas/view pixels and fade to 1 over 350 ms, stagger `i * 75L`; hero scales .94f→1.04f over 1600 ms, reverse/infinite (`HomeActivity`).
- `UiPolish` uses reduced-motion-aware entrance and notice fades; meaningful button releases use `VIRTUAL_KEY` haptics, failures `LONG_PRESS`. Explainer disables animations/transitions under `prefers-reduced-motion:reduce`.
- Exact native card/modal/popover elevation tiers are not overridden in the inspected shared theme/components; their framework values are unspecified here.

## Components

| Primitive | Styling and states |
|---|---|
| Cyan/violet cards | Layered halos described above; 24dp opaque surface interior, 1dp translucent border; common padding 16dp or 24dp. Violet outermost halo still uses `kv_glow_outer`. |
| Primary buttons | `Widget.KineticVault.Button` inherits `Widget.Material3.Button`; minimum 56dp height / 48dp width, 16dp corner radius, no all-caps. Fill/text resolve through primary/on-primary theme tokens; exact ripple, content padding, disabled colors and elevation are inherited. |
| Outline buttons | Explicit `Widget.Material3.Button.OutlinedButton` on Lab/Learn, camera/generate, Reveal Done/Open and filters. Many set 48dp minimum height; Home Lab/Learn set 80dp. Explicit variant inherits its Material styling; shared 16dp radius is not restated locally. |
| Text/ghost-like buttons | Explicit `Widget.Material3.Button.TextButton` for Back, Cancel, reset, retry and Return Home; commonly 48dp minimum height. Radius, padding and state-list colors inherited. |
| Navigation cards | Violet FrameLayouts, clickable/focusable, minimum 80dp height; label uses Body, padding 24dp. No custom pressed/disabled color list declared in the Home XML. |
| Status chip | Material `Chip`, minimum 48dp height, non-clickable/non-checkable, polite live region; “Offline”, “Pending”, “Synced”. Shape/fill/state appearance inherited; no custom pill radius declared. |
| Native inputs | Plain EditText for secrets/passphrases/server/code; Vault search uses Material TextInputLayout/TextInputEditText. Secret textarea 4–8 lines, top/start gravity, explicit `kv_text`; password controls use masked input. Radius/fill/border/focus styling inherited; no shared custom input shape declared. |
| Selection controls | MaterialRadioButton text/file pair uses equal horizontal weights and 48dp minimum height; MaterialCheckBox options also minimum 48dp. Sensitivity Spinner height 56dp. Checked/unchecked colors inherited. |
| Gesture Pad | Canvas cells: 16dp radius, 1dp border, `kv_surface` fill; pressed fill `kv_surface_high`; focused selected border `kv_primary`, otherwise `kv_outline`; enabled labels `kv_text`, disabled labels `kv_text_secondary`. 16sp labels shrink only if necessary to fit. |
| Progress | Material LinearProgressIndicator in Hide; platform horizontal ProgressBar in Reveal/Vault. Vault progress height 4dp; most others wrap content. Colors/thickness otherwise inherited. |
| Export sheet | BottomSheetDialog content from `layout/sheet_export.xml`: ScrollView padding 24dp, Title heading, Body note with 16dp vertical margins, vertical option stack, text-style Cancel. Sheet corner/elevation/scrim values inherited. |
| Empty/error panels | Vault centers a 64dp lock icon, Title, Body and recovery CTA with 24dp padding; Learn error uses violet card, 24dp padding, Body with 16dp vertical padding and Retry button. Hide notices use `kv_magenta`. |
| Explainer cards/buttons | Card radius 20px, border `1px solid #33475b`, exact gradient/glow above. Buttons radius 12px, minimum height 48px, border `1px solid #78eee8`, fill #15343e, text #efffff. Focus: `outline:3px solid #d7baff;outline-offset:4px`. No hover/active/disabled override declared. |
| Explainer flow badges | Wrap flex row; radius 12px, border `1px solid #4d4566`, padding 8px 12px; no separate background fill. Note rule: `border-left:3px solid #b39aff`. |

Icons use outline vectors, transparent fill, cyan `kv_primary`, round joins/caps, 24dp native size, 24×24 viewport, `strokeWidth=1.8`. Orb is 64dp with mixed translucent/solid circles and strokes 1 and 2. Launcher combines a filled shield and outlined lock. Recurring motifs: shield, lock, circular segmented dial, binary 0/1 rain, cyan/violet panel pairing. Binary rain uses 24 columns, nine-character tails, primary-colored tails and `kv_text` heads (`MatrixRainView`).

## Assets

- Icon-only launcher foreground: `app/src/main/res/drawable/ic_launcher_foreground.xml` (108dp shield/lock vector); background: `app/src/main/res/drawable/ic_launcher_background.xml`. Fixed dark brand artwork; no separate light launcher variant.
- Adaptive launchers: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`.
- Raster launchers: `app/src/main/res/mipmap-mdpi/`, `mipmap-hdpi/`, `mipmap-xhdpi/`, `mipmap-xxhdpi/`, `mipmap-xxxhdpi/`; each contains `ic_launcher.webp` and `ic_launcher_round.webp`.
- UI vectors under `app/src/main/res/drawable/`: `ic_lock.xml`, `ic_shield.xml`, `ic_scan.xml`, `ic_history.xml`, `ic_settings.xml`, `ic_arrow_left.xml`, `ic_arrow_right.xml`, `ic_arrow_up.xml`, `ic_arrow_down.xml`, `ic_orb.xml`. Semantic colors adapt to light/dark resources.
- Card backgrounds: `app/src/main/res/drawable/bg_glow_card_cyan.xml`, `app/src/main/res/drawable/bg_glow_card_violet.xml`.
- Animated logo/orb/binary artwork is generated by `ui/SplashLogoView.java`, `ui/KineticOrbView.java`, `ui/MatrixRainView.java` beneath the Java root; it is not a standalone image asset or wordmark.
- Offline illustrated explainer: `app/src/main/assets/how_it_works.html`; pixel swatch/bit diagrams use HTML/CSS rather than image files.
- No font resource directory appears in the native resource inventory; inspected typography uses system families. No standalone full wordmark or illustration image appears in the inspected resource/assets inventories.

## Sample content

| Authentic copy | Source token / location |
|---|---|
| Kinetic Vault | `values/strings.xml: app_name` |
| Your hand is the key. | `values/strings.xml: tagline` |
| PRIVATE BY DESIGN | `values/strings.xml: home_eyebrow` |
| Hide a secret | `values/strings.xml: home_hide` |
| Reveal a secret | `values/strings.xml: home_reveal` |
| Forge encrypted PNG | `values/strings.xml: hide_forge` |
| Next: enroll your gesture | `values/strings.xml: hide_next_gesture` |
| Open your vault. | `values/strings.xml: reveal_title` |
| Your vault starts here. | `values/vault_strings.xml: vault_empty_title` |
| Cloud Ledger | `values/sync_strings.xml: cloud_title` |
| Send as Document/File | `values/strings.xml: hide_share_title` |
| Privately replay enrolled gesture | `values/strings.xml: invention_replay` |
| Reading original pixels… | `values/scanner_strings.xml: scan_working` |
| Page unavailable | `values/scanner_strings.xml: learn_error_title` |
| A tiny colour change. / A hidden encrypted bit. | `assets/how_it_works.html: h1`, slash denotes source line break |
| Red: 100 · hidden bit: 0 | `assets/how_it_works.html: #value` |
| Hide a 1 in this pixel | `assets/how_it_works.html: #toggle` |
