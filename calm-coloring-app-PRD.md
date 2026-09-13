# Product Requirements Document
## Calm Coloring — A Low-Stimulation, Tap-to-Fill Coloring App for Young Children

**Status:** Draft v1 — for build handoff
**Author:** Atul (compiled from stress-tested product discussion)
**Target platforms:** Android + iOS (phone & tablet), via Kotlin Multiplatform / Compose Multiplatform

---

## 1. Background & Why This Product Exists

An initial idea evaluation of "AI photo-to-coloring-page app for babies" scored **3.8/10** against the Minimalist Entrepreneur framework. Two disqualifying findings drove the pivot documented in this PRD:

- **The AI photo→art feature is already commoditized.** At least six live products (Fotor, Colorfly, icoloring.ai, Mimi Panda, Coloring.app, ColorifyAI, ColoringBook.ai) offer photo-to-coloring-page conversion today, several for free. It is not a differentiator.
- **Uploading a child's photo is a regulatory liability, not an asset.** The FTC's amended COPPA rule (enforceable April 22, 2026) classifies facial/biometric data as personal information requiring separate, verified parental consent. Building a kids' app around photo uploads invites exactly the scrutiny that produced Disney's $10M COPPA settlement.

**The validated wedge instead:** a genuinely calm, low-stimulation coloring experience. This is not a hunch — it's a documented product category:

- Independent evaluators score toddler apps on four calm-design axes: **sound, unprompted animation, reward-loop engineering, and brightness/saturation** (harmlessapp.com). Existing coloring apps score poorly here specifically because of bright, high-contrast, attention-grabbing design — visible in the happyclicks.net reference screenshot (saturated swatch grid, thick high-contrast chrome, generic repetitive templates).
- **Pok Pok**, a Montessori-inspired "calm screen time" app, is a proven commercial success in this exact positioning — press-covered, award-winning, sold as a $49.99–60 lifetime purchase.

This PRD scopes a product built around that wedge: calm, tap-to-fill, low-stimulation, ad-free, privacy-first, age-appropriate coloring — nothing more for v1.

---

## 2. Goals

- Ship a coloring experience that is measurably calmer than incumbents on the sound/animation/reward/brightness axes.
- Make the core interaction (tap a region, watch it fill) reliable and satisfying for a young child's actual motor and cognitive ability at each age band — not just "cute," but usable.
- Solve print as a first-class capability, not an afterthought (identified gap vs. incumbents).
- Ship with a minimal, defensible content pipeline (vector, not raster) that supports both crisp on-screen rendering and clean print output from the same source data.
- Avoid COPPA exposure entirely for v1 by collecting no personal data and requiring no accounts.

## 3. Non-Goals (v1)

- **AI photo-to-art conversion.** Explicitly deferred — commoditized feature, disproportionate regulatory risk (biometric data), do not build in v1.
- **Freehand drawing/brush tools.** Explicitly rejected in favor of tap-to-fill; do not build a general-purpose canvas/painter.
- **Confetti/particle reward bursts, sound effects, streaks, push notifications, or any other attention-engineered reward loop.** Directly contradicts the calm-design thesis this product is built on.
- **Accounts, profiles, cloud sync, social sharing.** Adds data-collection surface (and COPPA burden) without a validated need.
- **The 1–2 year old age tier**, as a committed v1 feature (see §5.3) — treated as a future research spike, not a launch requirement.

---

## 4. Target User

**Primary:** Parents of children roughly 2.5–5 years old who are fatigued by ad-heavy, overstimulating, data-hungry kids' apps and are willing to pay a small one-time or annual price to avoid that (validated pattern: Pok Pok's pricing and press reception).

**End user:** The child, interacting via tap only — no drag, no multi-step gestures, no reading required.

---

## 5. UX & Interaction Design

### 5.1 Core interaction: tap-to-fill vector regions (not freehand, not raster flood fill)

- Each template is authored as an SVG where **every fillable area is its own separate closed vector path** (e.g., a horse's body, mane, tail, and each leg are distinct paths) — not one continuous outline.
- On tap, the app performs a point-in-polygon hit test against each region's path to determine which region was tapped. This runs once per tap, not per frame, so it stays cheap even with 10–30 regions per scene.
- The tapped region's fill color animates from empty to the selected color using `animateColorAsState` with a **spring with high damping / low stiffness** — a slow, settling motion, not a bounce. This animation *is* the calm feedback moment; no additional animation or particle library is used.
- **Why vector over raster flood fill:** raster flood fill (the literal "MS Paint" approach) risks color bleeding past thin or anti-aliased outlines, and locks print quality to the source bitmap's resolution. Vector regions never bleed (they're closed shapes, not pixel boundaries) and stay crisp at any zoom or print size — the same source data serves both the on-screen canvas and the print/PDF export.

### 5.2 Calm-design constraints (apply to every screen, not just the canvas)

Derived directly from the four-axis calm framework (sound / animation / reward / brightness):

| Axis | Rule |
|---|---|
| Sound | No sound effects on fill, no background music by default, no sudden audio of any kind |
| Animation | No unprompted motion anywhere in the UI; the only animation is the deliberate, slow color-settle on tap |
| Reward | No confetti, no completion badges, no streaks, no "you did it!" popups engineered to prolong sessions |
| Brightness | A small, fixed, muted/low-saturation color palette used everywhere — chrome, UI, and fill colors — no neon or high-contrast swatch grids like the happyclicks.net reference |

**Palette:** define one small, fixed set of muted colors (target: 6–8 total swatches system-wide) rather than a large, saturated picker grid. Only the subset appropriate to the active age tier (§5.3) is shown to the child at once — this directly satisfies the requirement that the child never sees an overloading color selection.

### 5.3 Age-tiered complexity — two independent dials, not one

Earlier framing conflated "number of colors" with "template complexity" as a single age variable. These are two separate developmental axes and should be tuned independently:

- **Region dial** (motor/spatial precision): controlled by region count and minimum region size.
- **Palette dial** (color cognition): controlled by how many colors are offered at once.

Grounded against actual child-development and touch-UX research (NN/g; color-development literature):

| Tier | Region design | Palette size | Evidence basis | Build priority |
|---|---|---|---|---|
| **3–5 years** | Moderate region count; minimum touch target **2cm × 2cm** (NN/g's own minimum for this age, 4x adult recommendation) | 3–6 muted colors | This is the only age band with solid *both* motor-precision and color-naming research behind it — children reliably name multiple colors by 3–4, and NN/g's touch-target research is built on this age group | **Build and validate first** |
| **2–3 years** | Very few, very large regions (1–3 per scene); oversized targets, no precision demanded | ~2 colors | Color-matching emerges 24–36 months; correct color identification ~30 months. Targeting precision below 3 has no solid research backing it, so this tier compensates by removing precision demands entirely | Second phase, after 3–5 tier is validated with real users |
| **1–2 years** | Not a targeting task — one giant region covering nearly the whole canvas so any tap anywhere succeeds | 1 color | No reliable touchscreen-targeting research exists below age 3. This tier is closer to a cause-and-effect toy than a "coloring" task | **Deferred / research spike only** — do not commit to this tier in the v1 build plan |

**Recommendation:** build the 3–5 tier as the actual MVP, get it in front of real children and parents, and only then decide whether to invest in the 2–3 and 1–2 tiers, rather than building all three up front.

### 5.4 Print

- Print/export renders the filled composition from the same vector source that drives on-screen rendering, so output quality is not capped by a raster template's resolution.
- No cloud round-trip required — local PDF generation via platform-native APIs (see §6).

---

## 6. Technical Architecture

**Stack:** Kotlin Multiplatform + Compose Multiplatform, targeting Android and iOS (phone and tablet form factors), Skia-backed rendering on both platforms for consistent low-latency touch response.

**Rationale (vs. WebView / React Native):** the product's entire value proposition depends on smooth, non-jarring touch response. WebView-based rendering introduces input latency and inconsistent fill/animation performance, especially on older Android tablets — directly undermining the calm-design thesis. React Native offers no advantage here either: custom region-fill rendering still requires native/Skia-level work regardless of framework, and there's no existing investment in that stack to leverage. Compose Multiplatform lets the same touch-handling and canvas expertise already in-house (custom Android view systems) transfer directly, in one codebase, across both platforms.

**Content pipeline:**
1. Templates are authored as SVGs with each fillable area as a separate closed `<path>`.
2. SVGs are converted at build time to Compose `Path`/`ImageVector` objects using [`svg-to-compose`](https://github.com/DevSrSouza/svg-to-compose) (actively maintained, purpose-built for this).
3. Each region is rendered from its `Path`, with per-region `Color` state driving the fill.

**Interaction/animation:**
- Hit-testing: point-in-polygon against region paths, evaluated on tap-up only (not per-frame) to keep the interaction cheap and responsive.
- Fill feedback: `animateColorAsState` with a tuned `spring()` (high damping ratio, low stiffness) — Compose's built-in animation API is sufficient; no third-party animation library is needed (evaluated and rejected: `touchlab/compose-animations` is deprecated in favor of the now-built-in Compose animation APIs).
- Explicitly rejected: `CanvasPainter` (freehand-drawing library, wrong interaction model), any raster flood-fill implementation (bleeding/resolution risk), particle/confetti libraries like `ConfettiKit` (contradicts the calm-design thesis — may be revisited later only as a very restrained, optional micro-interaction, not a default).

**Print/export:** platform-native PDF generation via `expect`/`actual` (Android `PdfDocument`, iOS `PDFKit`/`UIGraphicsPDFRenderer`), rendering directly from the same vector path data used on-screen.

**Backend:** none required for v1. The app is fully offline — no server, no accounts, no analytics SDKs that collect device identifiers. This is a deliberate architectural choice that also substantially reduces COPPA compliance burden (see §8).

**Before committing:** do a final, current check of Compose Multiplatform's iOS production maturity immediately before build start — it is broadly reported production-ready through 2026, but verify against the latest JetBrains release notes rather than taking this PRD's snapshot as permanently current.

---

## 7. Content Requirements & Licensing

- Minimum region size scales with age tier (see §5.3); the 3–5 tier's 2cm × 2cm minimum is a hard floor grounded in NN/g's touch-target research, not a suggestion.
- **Licensing:** do not casually pull "free commercial use" stock SVG coloring art (Vecteezy, Freepik, etc.) — those licenses typically permit using the image as-is, not decomposing it into separately licensed, redistributable path data inside a paid app. Default to custom-illustrated templates. This is a lower bar than it sounds: the calm-design direction already wants sparse, minimal-detail art, not intricate illustration — the design constraint and the licensing-safety constraint point the same direction.
- Launch content target: a small, deliberately curated template library (e.g., 10–15 templates) for the 3–5 tier — depth of polish over breadth of content, consistent with a minimalist MVP.
- **Open decision, not yet resolved in this PRD:** who authors the templates (illustrator commission vs. in-house), and the per-template cost/timeline. Flag this explicitly to whoever picks up the build — it is a real content-production dependency, not a solved problem.

---

## 8. Privacy & Compliance

- **No accounts, no third-party analytics collecting device identifiers, no ads, no photo upload.** This removes the app from most of COPPA's 2026 compliance burden by construction rather than by consent-flow engineering.
- COPPA's amended rule (enforceable April 22, 2026) bans bundled consent, expands "personal information" to include biometric data, and requires published data-retention documentation for anything that *is* collected. Because v1 collects nothing, most of this is moot — but if any analytics or crash-reporting SDK is added later, it must be evaluated against these rules before integration, not after.
- No feature in this PRD requires parental consent flows for v1. If a future version reintroduces photo upload or any data collection, this section must be revisited before that feature ships.

---

## 9. MVP Scope Summary

**In scope for v1:**
- Tap-to-fill vector coloring canvas, 3–5 year age tier only
- Fixed, muted, small color palette (6–8 swatches system-wide, tier-appropriate subset shown)
- Calm-design constraints applied throughout (no sound, no unprompted animation, no reward loops)
- Local, vector-sourced PDF print/export
- Offline-first, no accounts, no ads, no data collection
- 10–15 launch templates, custom-illustrated or properly licensed

**Explicitly out of scope for v1:** AI photo-to-art, freehand drawing, confetti/reward effects, accounts/cloud sync/social sharing, 1–2 and 2–3 year tiers.

---

## 10. Validation Plan

Before full build investment, per the Minimalist Entrepreneur framework this product was evaluated against:
1. Low-fidelity test of the tap-to-fill mechanic and muted palette with real 3–5 year olds and their parents — confirm the core interaction and calm-design choices land before building the full template pipeline.
2. Mine actual 1–3 star App Store reviews of existing baby/toddler coloring apps to validate (or correct) the specific complaints this product is designed against, rather than relying on impression alone.
3. Post-launch: track organic download response to "ad-free / no data collection" positioning specifically, since that is the differentiated value claim the pricing model depends on.

---

## 11. Open Risks & Unresolved Questions

These are carried forward deliberately rather than smoothed over — they need explicit decisions before or during build, not after:

- **Content authoring resourcing is undecided** — no illustrator or pipeline commitment yet exists for the vector template library.
- **1–2 year tier has no validated interaction model** — do not build it speculatively; treat as a research spike after the 3–5 tier ships.
- **CMP-for-iOS production readiness** should get one final current check immediately before build start, not assumed from this document.
- **Print/PDF platform integration effort is not yet detailed at the implementation level** — scope it during technical planning, not assumed to be trivial.
- **Monetization model is directionally scoped, not finalized:** a one-time purchase or family annual plan (roughly $2.99–4.99 one-time, or ~$9.99/year, modeled on Pok Pok's positioning) is the working hypothesis but has not itself been validated with real willingness-to-pay data.
