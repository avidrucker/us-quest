# us-quest — Brainstorming

A divergent catalog of improvement ideas. Nothing here is committed-to; it's raw
material to pick from later. Each idea has a rough **effort** (S / M / L) and
**impact** note. ⭐ = standout / high-leverage given where the project is today.

_Last updated: 2026-06-24._

## Where the project is now

A ClojureScript (shadow-cljs + reagent + re-frame) branching CYOA engine with a
pure `adventure.domain` core, malli/guardrails contracts, localStorage
persistence, URL-hash share links, three built-in demos (a romance "love-quiz",
a cognitive-biases intro, and a **Japanese greetings lesson**), a node unit
suite + a Playwright e2e suite, and automated Pages deploys. The engine is
solid; the open question is **what to grow it into**. The project has a genuine
split identity — romantic gift vs. language tutor vs. general story engine — and
that's the most important thing to resolve before investing heavily.

---

## A. Language-learning tool ⭐ (strong fit: the Japanese lesson already exists; the user studies Spanish too, and has Forvo/Anki tooling)

- ⭐ **Native pronunciation audio** — a 🔊 button per phrase, sourced from Forvo
  (there's already a `forvo-audio` workflow), cached locally. _M · high impact for a language tool._
- **TTS fallback** — Web Speech API (`speechSynthesis`) when no recorded audio
  exists; zero assets, instant. _S · medium._
- ⭐ **Right/wrong scoring + end-of-lesson feedback** — track correct vs. wrong
  choices, show a score + which phrases were missed. Turns "no wrong answers"
  into real practice (the loop-back reactions are already the teaching moments). _M · high._
- **Furigana / romaji toggle** — show/hide reading aids per learner level. _M · medium._
- ⭐ **Spaced-repetition review mode** — resurface missed phrases over time; could
  export to Anki (the user has `anki-gen`). _L · high for retention._
- **More lessons & languages** — numbers, ordering food, directions; **Spanish**
  track (the user is actively studying it). The engine is language-agnostic. _M each · high._
- **Hint system** — reveal romaji/meaning when stuck. _S · medium._
- **"Story mode" vs "quiz mode"** — same content, stricter feedback in quiz mode. _S · medium._

## B. Personal / romantic gift

- ⭐ **Photo passages** — upload/inline images (not just emoji/URL); base64 into
  the adventure so it still shares via link. _M · high for the gift use-case (watch link size)._
- **Per-passage audio / background music** — a voice note or song. _M · medium._
- **Themes / skins** — color + font presets ("romantic", "playful", "minimal"). _S–M · medium._
- **Celebration ending** — confetti / hearts animation on the payoff. _S · medium._
- **Richer share** — QR code for the link, OpenGraph preview card, optional
  "open on <date>" reveal, optional password-lock. _M · medium._

## C. General CYOA authoring engine

- ⭐ **Branch graph view** — visualize passages + choices as a node graph in the
  editor (read-only first, then click-to-edit). Visually impressive and genuinely
  useful for authoring; great portfolio piece. _L · high._
- **Drag-to-reorder** passages and choices. _M · medium._
- ⭐ **Import / export** an adventure as an EDN/JSON file (round-trips the share
  format you already have). Unlocks backup, sharing big adventures, version
  control of content. _S · high (mostly built — share encode/decode exists)._
- **Templates / duplicate** — start from a copy. _S · medium._
- **State & conditions** — flags/inventory: "if visited X, show Y", "you already
  have the key". This is the big leap from CYOA to interactive fiction. _L · high but scope-heavy._
- **Endings map / completion** — "you've found 3 of 5 endings". _M · medium._
- **Undo / redo** in the editor. _M · medium._
- **Jump-to-problem** — click a validation problem to open the offending passage. _S · medium._
- **Markdown** in passage text (bold/italic/links). _S · low-medium._

## D. Technical craftsmanship / showcase

- ⭐ **Accessibility pass** — keyboard nav, focus management on route change, ARIA
  roles/labels, contrast check, `aria-live` for appended passages. The Playwright
  suite can assert a lot of it. _M · high (and the right thing to do)._
- ⭐ **PWA / offline** — manifest + service worker; the app is already
  localStorage-only, so "installable, works offline" is a small step with a big
  perceived-quality payoff. _M · high._
- **Reduced-motion already respected** — extend the animation polish (choice
  transitions, the append-and-scroll already shipped). _S · low._
- **Corrupt-state resilience** — guard against malformed localStorage (share
  decode already guards); add an error boundary + a "reset library" escape hatch. _S · medium._
- **Privacy-respecting analytics** — where players drop off, which endings hit;
  purely local or self-hosted. _M · medium._
- **Dark mode / theme toggle.** _S · medium._
- **Property-based tests** for the domain (e.g. validate ∘ build invariants); more
  e2e coverage; visual-regression snapshots. _M · medium._

## E. Data model / architecture deepening

- **Stored-data versioning + migration** — a schema version in localStorage so
  future model changes don't strand existing libraries (you just hit a near-miss
  of this with #14's stable ids). _S–M · medium-high (cheap insurance)._
- **Separate play-progress from the library** — track per-adventure progress
  (current trail, endings seen) distinct from the authored content. _M · medium._
- **Compress share links** — the `share` namespace already flags this; slot a
  compressor before base64 once links grow (photo passages will push this). _S · low until needed._
- **Optional cloud sync / accounts** — a real backend (Datomic? a tiny API?) for
  cross-device libraries. _L · high cost; only if the product direction demands it._

## F. Content & product

- **First-run onboarding** — a 20-second guided tour. _S · medium._
- **"Remix this" ** — fork a shared adventure into your library to edit. _S · medium (you already import on share)._
- **Sample gallery** — a curated set beyond the three seeds. _S · low-medium._

---

## Cross-cutting "if I had to pick" shortlist

If the goal is a **language tutor**: Forvo audio ⭐ + scoring ⭐ + a Spanish track.
If the goal is a **showcase**: branch graph view ⭐ + a11y ⭐ + PWA ⭐.
If the goal is a **gift**: photo passages ⭐ + themes + richer share.
Cheap-insurance regardless of direction: **stored-data versioning/migration**.

## Open questions to resolve before committing

1. Which identity leads — tutor, gift, or general engine? (Improvements diverge sharply.)
2. Single-player toy or something others author in? (Drives editor investment.)
3. Stay backend-free (localStorage + share links) or add accounts/sync?
4. One language or a multi-language learning platform (Japanese + Spanish)?
